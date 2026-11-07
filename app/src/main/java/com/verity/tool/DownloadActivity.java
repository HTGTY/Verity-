package com.verity.tool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.verity.tool.util.GameFs;
import com.verity.tool.util.McPaths;
import com.verity.tool.util.ModSpec;
import com.verity.tool.util.SafPicker;
import com.verity.tool.util.SafStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 下载模组：选 1.20.1 版本 → 勾选模组 → 下载。 */
public class DownloadActivity extends BaseActivity implements DownloadService.Listener {

    private static final int REQUEST_SAF = 2002;
    private static final String BILI_URL = "https://www.bilibili.com/video/BV1Nx3t6JEmf";

    private View panelPick;
    private View panelProgress;
    private TextView dirHint;
    private TextView stage;
    private TextView percent;
    private TextView logText;
    private ScrollView logScroll;
    private LinearLayout versionList;
    private LinearLayout modList;
    private LinearProgressIndicator progress;
    private MaterialButton btnStart;
    private MaterialButton btnStop;
    private MaterialButton btnSaf;

    private DownloadService service;
    private boolean bound;
    private boolean downloading;

    private String selectedVersion;
    private final List<VersionHolder> holders = new ArrayList<>();
    private final Map<String, MaterialCheckBox> checks = new LinkedHashMap<>();

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DownloadService.LocalBinder) binder).service();
            bound = true;
            service.addListener(DownloadActivity.this);
            if (service.isRunning()) {
                downloading = true;
                showProgressPanel();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.dl_title);
        findViewById(R.id.btn_back).setOnClickListener(v -> background());

        panelPick = findViewById(R.id.panel_pick);
        panelProgress = findViewById(R.id.panel_progress);
        dirHint = findViewById(R.id.dl_dir_hint);
        stage = findViewById(R.id.dl_stage);
        percent = findViewById(R.id.dl_percent);
        logText = findViewById(R.id.dl_log_text);
        logScroll = findViewById(R.id.dl_log_scroll);
        versionList = findViewById(R.id.dl_versions);
        modList = findViewById(R.id.dl_mods);
        progress = findViewById(R.id.dl_progress);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnSaf = findViewById(R.id.btn_saf);

        btnStart.setOnClickListener(v -> startDownload());
        btnStop.setOnClickListener(v -> confirmStop());
        findViewById(R.id.btn_background).setOnClickListener(v -> background());
        btnSaf.setOnClickListener(v -> SafPicker.open(this, REQUEST_SAF));
        findViewById(R.id.dl_bili_hint).setOnClickListener(v -> openBili());

        buildMods();
        refreshVersions();
        bindService(new Intent(this, DownloadService.class), connection, Context.BIND_AUTO_CREATE);
    }

    private void openBili() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BILI_URL)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.dl_bili_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** 三个模组各一张卡片，默认全选。 */
    private void buildMods() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final ModSpec spec : ModSpec.all()) {
            View view = inflater.inflate(R.layout.item_mod, modList, false);
            ((TextView) view.findViewById(R.id.mod_name)).setText(spec.name);
            TextView meta = view.findViewById(R.id.mod_meta);
            meta.setText(spec.size > 0
                    ? getString(R.string.dl_mod_meta_size, spec.version, mb(spec.size))
                    : getString(R.string.dl_mod_meta, spec.version));
            final MaterialCheckBox check = view.findViewById(R.id.mod_check);
            check.setChecked(true);
            view.setOnClickListener(v -> check.setChecked(!check.isChecked()));
            checks.put(spec.key, check);
            modList.addView(view);
        }
    }

    /** 只列 1.20.1 的版本目录。 */
    private void refreshVersions() {
        List<McPaths.Entry> entries = new ArrayList<>();
        for (McPaths.Entry entry : McPaths.scan()) {
            if (entry.name.contains("1.20.1")) {
                entries.add(entry);
            }
        }

        versionList.removeAllViews();
        holders.clear();
        if (entries.isEmpty()) {
            dirHint.setText(McPaths.rootReadable() ? R.string.dl_no_version : R.string.dl_denied);
        } else {
            dirHint.setText(getString(R.string.dl_version_hint, entries.size()));
            LayoutInflater inflater = LayoutInflater.from(this);
            for (McPaths.Entry entry : entries) {
                addVersionCard(inflater, entry);
            }
        }
        dirHint.setVisibility(View.VISIBLE);

        boolean needSaf = !McPaths.rootReadable() && GameFs.safSupported();
        btnSaf.setVisibility(needSaf ? View.VISIBLE : View.GONE);

        if (selectedVersion != null && !containsVersion(entries, selectedVersion)) {
            selectedVersion = null;
        }
        syncSelection();
    }

    private boolean containsVersion(List<McPaths.Entry> entries, String name) {
        for (McPaths.Entry entry : entries) {
            if (entry.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void addVersionCard(LayoutInflater inflater, McPaths.Entry entry) {
        View view = inflater.inflate(R.layout.item_game_dir, versionList, false);
        MaterialCardView card = (MaterialCardView) view;
        ((TextView) view.findViewById(R.id.dir_name)).setText(entry.name);
        TextView desc = view.findViewById(R.id.dir_desc);
        desc.setText(GameFs.modsExists(entry.name) ? R.string.dl_mods_exist : R.string.dl_mods_new);
        card.setOnClickListener(v -> {
            selectedVersion = entry.name;
            syncSelection();
        });
        versionList.addView(card);
        holders.add(new VersionHolder(entry.name, card, desc));
    }

    private void syncSelection() {
        int color = themeColor(android.R.attr.colorAccent);
        for (VersionHolder holder : holders) {
            boolean selected = holder.name.equals(selectedVersion);
            holder.card.setStrokeWidth(dp(selected ? 2 : 1));
            holder.card.setStrokeColor(selected ? color : themeColor(android.R.attr.colorControlNormal));
            if (selected) {
                holder.desc.setText(R.string.dl_selected);
            } else {
                holder.desc.setText(GameFs.modsExists(holder.name)
                        ? R.string.dl_mods_exist : R.string.dl_mods_new);
            }
        }
    }

    private void startDownload() {
        if (selectedVersion == null) {
            Toast.makeText(this, R.string.dl_choose_version, Toast.LENGTH_SHORT).show();
            return;
        }
        final List<String> keys = new ArrayList<>();
        for (Map.Entry<String, MaterialCheckBox> entry : checks.entrySet()) {
            if (entry.getValue().isChecked()) {
                keys.add(entry.getKey());
            }
        }
        if (keys.isEmpty()) {
            Toast.makeText(this, R.string.dl_choose_mod, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> olds = oldVerityJars();
        if (!olds.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dl_conflict_title)
                    .setMessage(getString(R.string.dl_conflict_msg, join(olds)))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dl_conflict_continue,
                            (d, w) -> beginDownload(keys))
                    .show();
            return;
        }
        beginDownload(keys);
    }

    /** mods 里其他版本的 verity jar，避免新旧版本同时加载。 */
    private List<String> oldVerityJars() {
        List<String> olds = new ArrayList<>();
        for (String name : GameFs.modFileNames(selectedVersion)) {
            String lower = name.toLowerCase(Locale.US);
            if (lower.startsWith("verity") && lower.endsWith(".jar")
                    && !name.equals(ModSpec.VERITY.fileName)) {
                olds.add(name);
            }
        }
        return olds;
    }

    private void beginDownload(List<String> keys) {
        downloading = true;
        logText.setText("");
        stage.setText(R.string.dl_prepare);
        percent.setText("");
        progress.setIndeterminate(true);
        progress.setProgress(0);
        btnStop.setEnabled(true);
        showProgressPanel();
        DownloadService.start(this, selectedVersion, keys.toArray(new String[0]));
    }

    private void showProgressPanel() {
        panelPick.setVisibility(View.GONE);
        panelProgress.setVisibility(View.VISIBLE);
    }

    private void showPickPanel() {
        panelProgress.setVisibility(View.GONE);
        panelPick.setVisibility(View.VISIBLE);
    }

    private void confirmStop() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dl_stop_title)
                .setMessage(R.string.dl_stop)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.dl_stop, (d, w) -> {
                    if (service != null) {
                        service.cancel();
                    } else {
                        downloading = false;
                        showPickPanel();
                    }
                })
                .show();
    }

    /** 返回键 / 后台按钮：页面关掉，服务继续跑。 */
    private void background() {
        if (downloading && service != null && service.isRunning()) {
            Toast.makeText(this, R.string.dl_background, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        background();
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            if (service != null) {
                service.removeListener(this);
            }
            unbindService(connection);
            bound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SAF || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri treeUri = data.getData();
        if (treeUri == null) {
            return;
        }
        SafStore.persist(this, treeUri, SafStore.pathOf(treeUri));
        if (SafStore.root() == null) {
            Toast.makeText(this, R.string.saf_unreadable, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.saf_ready, Toast.LENGTH_SHORT).show();
        refreshVersions();
    }

    // ---- DownloadService.Listener ----

    @Override
    public void onLog(String line) {
        runOnUiThread(() -> {
            if (logText.getText().length() > 0) {
                logText.append("\n");
            }
            logText.append(line);
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onProgress(int value, String text) {
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            progress.setProgress(value);
            stage.setText(text);
            percent.setText(getString(R.string.dl_percent, value));
        });
    }

    @Override
    public void onDone(String message) {
        if (!downloading) {
            return;
        }
        downloading = false;
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            progress.setProgress(100);
            btnStop.setEnabled(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dl_done_title)
                    .setMessage(message + "\n\n" + getString(R.string.dl_done_restart))
                    .setPositiveButton(R.string.ok, (d, w) -> finish())
                    .setOnDismissListener(d -> finish())
                    .show();
        });
    }

    @Override
    public void onFailed(String message) {
        if (!downloading) {
            return;
        }
        downloading = false;
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            btnStop.setEnabled(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dl_failed_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (d, w) -> showPickPanel())
                    .show();
        });
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String mb(long bytes) {
        return String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append("· ").append(items.get(i));
        }
        return sb.toString();
    }

    private static final class VersionHolder {
        final String name;
        final MaterialCardView card;
        final TextView desc;

        VersionHolder(String name, MaterialCardView card, TextView desc) {
            this.name = name;
            this.card = card;
            this.desc = desc;
        }
    }
}
