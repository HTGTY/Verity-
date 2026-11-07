package com.verity.tool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.verity.tool.util.GameFs;
import com.verity.tool.util.HashUtil;
import com.verity.tool.util.McPaths;
import com.verity.tool.util.ModDownloader;
import com.verity.tool.util.ModSpec;
import com.verity.tool.util.ModrinthApi;
import com.verity.tool.util.Notify;
import com.verity.tool.util.SafStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 前台服务依次下载选中的模组 jar。
 * 直连模式把 .part 放在 mods 目录里直接续传；
 * SAF 模式先下到应用缓存，校验通过后再写入授权目录。
 */
public class DownloadService extends Service {

    public static final String ACTION_START = "com.verity.tool.DOWNLOAD_START";
    public static final String EXTRA_VERSION = "version";
    public static final String EXTRA_MODS = "mods";

    private static final int NOTIF_ID = 202;

    public interface Listener {
        void onLog(String line);

        void onProgress(int percent, String text);

        void onDone(String message);

        void onFailed(String message);
    }

    public class LocalBinder extends Binder {
        public DownloadService service() {
            return DownloadService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private final List<String> logLines = new ArrayList<>();

    private volatile boolean canceled;
    private volatile boolean running;

    private String version;
    private String[] modKeys;
    private int percent;
    private String progressText = "";
    private String result;
    private String failure;

    public static void start(Context ctx, String version, String[] modKeys) {
        Intent i = new Intent(ctx, DownloadService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_VERSION, version)
                .putExtra(EXTRA_MODS, modKeys);
        ContextCompat.startForegroundService(ctx, i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            String v = intent.getStringExtra(EXTRA_VERSION);
            String[] keys = intent.getStringArrayExtra(EXTRA_MODS);
            if (v != null && keys != null && keys.length > 0 && !running) {
                version = v;
                modKeys = keys;
                beginForeground();
                new Thread(this::work).start();
            }
        }
        return START_NOT_STICKY;
    }

    private void beginForeground() {
        Notify.createChannel(this);
        NotificationCompat.Builder b = Notify.builder(this,
                getString(R.string.dl_title), getString(R.string.dl_prepare))
                .setContentIntent(Notify.openApp(this))
                .setOngoing(true)
                .setProgress(0, 0, true);
        startForeground(NOTIF_ID, b.build());
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
        for (String line : new ArrayList<>(logLines)) {
            l.onLog(line);
        }
        if (!progressText.isEmpty()) {
            l.onProgress(percent, progressText);
        }
        if (result != null) {
            l.onDone(result);
        } else if (failure != null) {
            l.onFailed(failure);
        }
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public boolean isRunning() {
        return running;
    }

    /** 用户点「终止」：保留 .part，下次可继续。 */
    public void cancel() {
        if (!running) {
            return;
        }
        canceled = true;
        log(getString(R.string.dl_stopping));
    }

    private void work() {
        running = true;
        canceled = false;
        result = null;
        failure = null;
        percent = 0;
        progressText = "";

        List<ModSpec> specs = new ArrayList<>();
        for (String key : modKeys) {
            ModSpec spec = ModSpec.of(key);
            if (spec != null) {
                specs.add(spec);
            }
        }
        if (specs.isEmpty()) {
            finishFail(getString(R.string.dl_choose_mod));
            return;
        }

        boolean saf = !GameFs.direct();
        File directMods = null;
        DocumentFile safMods = null;
        if (saf) {
            safMods = SafStore.ensureDir(version + "/mods");
            if (safMods == null) {
                finishFail(getString(R.string.dl_saf_failed));
                return;
            }
        } else {
            directMods = new File(McPaths.versionDir(version), "mods");
            if (!directMods.isDirectory() && !directMods.mkdirs()) {
                finishFail(getString(R.string.dl_mods_failed));
                return;
            }
        }

        log(getString(R.string.dl_target, GameFs.modsPath(version)));
        int installed = 0;
        int skipped = 0;
        try {
            for (int i = 0; i < specs.size(); i++) {
                if (canceled) {
                    finishCancel();
                    return;
                }
                ModSpec spec = specs.get(i);
                publish(i, specs.size(), spec, getString(R.string.dl_prepare_mod, spec.name));

                String sha1 = spec.sha1;
                if (sha1 == null && spec.hashVersionId != null) {
                    try {
                        sha1 = ModrinthApi.sha1(spec.hashVersionId);
                    } catch (IOException e) {
                        log(getString(R.string.dl_hash_unavailable, spec.name));
                    }
                }
                String sha512 = spec.sha512;

                File directTarget = directMods == null ? null : new File(directMods, spec.fileName);
                DocumentFile safTarget = safMods == null ? null : SafStore.child(safMods, spec.fileName);

                if (alreadyOk(directTarget, safTarget, sha1)) {
                    log(getString(R.string.dl_skip_same, spec.fileName));
                    skipped++;
                    publish(i + 1, specs.size(), spec, getString(R.string.dl_skip_same, spec.fileName));
                    continue;
                }

                // 校验不一致的旧文件先删掉，避免占位
                if (directTarget != null && directTarget.isFile()) {
                    directTarget.delete();
                }
                if (safTarget != null && safTarget.isFile()) {
                    SafStore.delete(safTarget);
                }

                File part = directMods != null
                        ? new File(directMods, spec.fileName + ".part")
                        : new File(new File(getCacheDir(), "Download/mods"), spec.fileName + ".part");

                log(getString(R.string.dl_start_mod, spec.name, spec.version));
                final int index = i;
                final int count = specs.size();
                ModDownloader.Result res = ModDownloader.fetch(spec, part, sha1, sha512,
                        (done, total) -> publishBytes(index, count, spec, done, total),
                        () -> canceled);

                if (res.canceled) {
                    finishCancel();
                    return;
                }
                if (!res.ok) {
                    part.delete();
                    finishFail(getString(R.string.dl_mod_failed, spec.name, res.error));
                    return;
                }

                if (directTarget != null) {
                    if (!part.renameTo(directTarget)) {
                        part.delete();
                        finishFail(getString(R.string.dl_write_failed, spec.name));
                        return;
                    }
                } else {
                    DocumentFile doc = SafStore.ensureFile(safMods, spec.fileName);
                    if (doc == null || !SafStore.writeFile(doc, part)) {
                        part.delete();
                        finishFail(getString(R.string.dl_write_failed, spec.name));
                        return;
                    }
                    part.delete();
                }

                installed++;
                log(getString(R.string.dl_hash_ok, spec.fileName));
                publish(i + 1, count, spec, getString(R.string.dl_hash_ok, spec.fileName));
            }
        } catch (Exception e) {
            finishFail(String.valueOf(e.getMessage()));
            return;
        }

        finishOk(getString(R.string.dl_done_msg, installed, skipped));
    }

    /** 目标已存在且摘要一致才算可跳过。 */
    private boolean alreadyOk(File directTarget, DocumentFile safTarget, String sha1) {
        if (sha1 == null || sha1.isEmpty()) {
            return false;
        }
        String local;
        if (directTarget != null) {
            if (!directTarget.isFile()) {
                return false;
            }
            local = HashUtil.sha1(directTarget);
        } else {
            if (safTarget == null || !safTarget.isFile()) {
                return false;
            }
            local = SafStore.sha1(safTarget);
        }
        return sha1.equalsIgnoreCase(local);
    }

    private void publish(int finished, int count, ModSpec spec, String text) {
        int value = count <= 0 ? 0 : (int) (finished * 100L / count);
        publishProgress(value, getString(R.string.dl_overall, finished, count, text));
    }

    private void publishBytes(int index, int count, ModSpec spec, long done, long total) {
        int filePct = total > 0 ? (int) (done * 100L / total) : 0;
        int value = count <= 0 ? 0
                : (int) ((index * 100L + filePct) / count);
        String text = total > 0
                ? getString(R.string.dl_mod_progress, spec.name, filePct, fmt(done), fmt(total))
                : getString(R.string.dl_mod_progress_unknown, spec.name, fmt(done));
        publishProgress(value, text);
    }

    private void publishProgress(int value, String text) {
        percent = Math.max(0, Math.min(100, value));
        progressText = text;
        Notify.updateForeground(this, NOTIF_ID, getString(R.string.dl_title), text, percent, 100);
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onProgress(percent, text);
            }
        });
    }

    private void finishOk(String message) {
        running = false;
        result = message;
        percent = 100;
        progressText = getString(R.string.dl_done_title);
        log(getString(R.string.dl_done_log, message));
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onDone(message);
            }
        });
        Notify.cancel(this, NOTIF_ID);
        stopForeground(true);
        stopSelf();
    }

    private void finishFail(String message) {
        running = false;
        failure = message;
        log(getString(R.string.dl_failed, message));
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onFailed(message);
            }
        });
        Notify.cancel(this, NOTIF_ID);
        stopForeground(true);
        stopSelf();
    }

    private void finishCancel() {
        running = false;
        failure = getString(R.string.dl_canceled_hint);
        log(getString(R.string.dl_canceled_hint));
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onFailed(getString(R.string.dl_canceled_hint));
            }
        });
        Notify.cancel(this, NOTIF_ID);
        stopForeground(true);
        stopSelf();
    }

    private void log(String line) {
        logLines.add(line);
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onLog(line);
            }
        });
    }

    private static String fmt(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
