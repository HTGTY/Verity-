package com.verity.tool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.verity.tool.util.McPaths;
import com.verity.tool.util.NetUtil;
import com.verity.tool.util.Notify;
import com.verity.tool.util.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 前台服务下载整合包并解压。
 * 页面只订阅进度，离开页面（转后台）也不会中断。
 */
public class DownloadService extends Service {

    public static final String ACTION_START = "com.verity.tool.DOWNLOAD_START";
    public static final String EXTRA_VERSION = "version";
    /** 资源包直链，固定版本，不再走 API 查询。 */
    private static final String RESOURCE_URL =
            "https://github.com/HTGTY/Verity-/releases/download/resource-package-v1/resource.zip";
    private static final int NOTIF_ID = 201;
    private static final String CHANNEL = "verity_download";

    public interface Listener {
        void onLog(String line);

        void onProgress(int done, int total);

        void onDone(String dirName);

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

    private volatile boolean canceled;
    private volatile boolean running;
    private String version;
    private final List<String> logLines = new ArrayList<>();
    private int done;
    private int total;
    private String resultDir;
    private String failure;

    public static void start(Context ctx, String version) {
        Intent i = new Intent(ctx, DownloadService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_VERSION, version);
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
            if (v != null && !running) {
                version = v;
                beginForeground();
                work();
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
        // 补发当前状态，避免旋转屏幕或重新进入丢失日志
        for (String line : new ArrayList<>(logLines)) {
            l.onLog(line);
        }
        if (total > 0) {
            l.onProgress(done, total);
        }
        if (resultDir != null) {
            l.onDone(resultDir);
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

    /** 用户点「终止」：标记后由下载循环尽快退出并清理临时文件。 */
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
        resultDir = null;
        failure = null;
        File cache = null;
        try {
            File root = McPaths.rootDir();
            // version 已是目录名（如 1.20.1-Forge-Verity）
            File versionDir = new File(root, version);
            cache = new File(getCacheDir(),
                    "Download/resource-" + System.currentTimeMillis() + ".zip");

            log(getString(R.string.dl_url, RESOURCE_URL));

            final File target = cache;
            NetUtil.download(RESOURCE_URL, target, new NetUtil.Progress() {
                @Override
                public void onProgress(int downloaded, int t) {
                    if (canceled) {
                        return;
                    }
                    done = downloaded;
                    total = t;
                    int pct = t > 0 ? (int) (downloaded * 100.0 / t) : 0;
                    String text = t > 0
                            ? getString(R.string.dl_progress, fmt(downloaded), fmt(t), pct)
                            : getString(R.string.dl_progress_unknown, fmt(downloaded));
                    Notify.updateForeground(DownloadService.this, NOTIF_ID,
                            getString(R.string.dl_title), text, downloaded, t);
                    main.post(() -> {
                        for (Listener l : new ArrayList<>(listeners)) {
                            l.onProgress(downloaded, t);
                        }
                    });
                }

                @Override
                public void onSuccess(File file) {
                    if (canceled) {
                        file.delete();
                        finishCancel();
                        return;
                    }
                    log(getString(R.string.dl_unzip, versionDir.getName()));
                    main.post(() -> {
                        Notify.updateForeground(DownloadService.this, NOTIF_ID,
                                getString(R.string.dl_title), getString(R.string.dl_unzip, versionDir.getName()), 0, 0);
                        for (Listener l : new ArrayList<>(listeners)) {
                            l.onLog(getString(R.string.dl_unzip, versionDir.getName()));
                        }
                    });
                    new Thread(() -> {
                        try {
                            ZipUtil.unzip(file, versionDir);
                            file.delete();
                            finishOk(versionDir.getName());
                        } catch (Exception e) {
                            finishFail(String.valueOf(e.getMessage()));
                        }
                    }).start();
                }

                @Override
                public void onError(String message) {
                    finishFail(message);
                }
            });
        } catch (Exception e) {
            if (cache != null) {
                cache.delete();
            }
            finishFail(String.valueOf(e.getMessage()));
        }
    }

    private void finishOk(String dirName) {
        running = false;
        resultDir = dirName;
        log(getString(R.string.dl_done, dirName));
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onDone(dirName);
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
        failure = null;
        resultDir = null;
        log(getString(R.string.dl_stopped));
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onFailed(getString(R.string.dl_stopped));
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