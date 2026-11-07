package com.verity.tool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.verity.tool.util.GameFs;
import com.verity.tool.util.McPaths;
import com.verity.tool.util.Prefs;
import com.verity.tool.util.SafPicker;
import com.verity.tool.util.SafStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 配置设置：在 FCL 与 Zalith Launcher 2 之间切换，也可手动指定启动器根目录。 */
public class PathSettingsActivity extends BaseActivity {

    private static final int REQUEST_TREE = 1001;

    private TextInputEditText etPath;
    private TextView pathPreview;
    private TextView pathStatus;
    private TextView launcherStatus;
    private TextView safStatus;
    private MaterialButtonToggleGroup launcherGroup;
    /** 同步选中状态时避免触发切换逻辑。 */
    private boolean syncing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_settings);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.title_path);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etPath = findViewById(R.id.et_path);
        pathPreview = findViewById(R.id.path_preview);
        pathStatus = findViewById(R.id.path_status);
        launcherStatus = findViewById(R.id.launcher_status);
        safStatus = findViewById(R.id.saf_status);
        launcherGroup = findViewById(R.id.launcher_group);

        etPath.setText(Prefs.launcherRoot());
        updatePreview();
        syncLauncherSelection();
        updateSafStatus();

        etPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview();
                syncLauncherSelection();
            }
        });

        launcherGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || syncing) {
                return;
            }
            if (checkedId == R.id.btn_zl2) {
                applyLauncher(Prefs.ZL2_ROOT);
            } else if (checkedId == R.id.btn_fcl) {
                applyLauncher(Prefs.DEFAULT_ROOT);
            }
        });

        MaterialButton btnDefault = findViewById(R.id.btn_default);
        btnDefault.setOnClickListener(v -> {
            etPath.setText(Prefs.DEFAULT_ROOT);
            save();
        });

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> save());

        findViewById(R.id.btn_saf).setOnClickListener(v -> requestSaf());
    }

    /** 点击启动器按钮：填入预设目录并立即保存。 */
    private void applyLauncher(String root) {
        etPath.setText(root);
        if (etPath.getText() != null) {
            etPath.setSelection(etPath.getText().length());
        }
        save();
    }

    /** 根据当前输入框内容反推启动器，并同步按钮选中状态。 */
    private void syncLauncherSelection() {
        String type = Prefs.launcherType(text());
        syncing = true;
        if (Prefs.LAUNCHER_ZL2.equals(type)) {
            launcherGroup.check(R.id.btn_zl2);
        } else if (Prefs.LAUNCHER_FCL.equals(type)) {
            launcherGroup.check(R.id.btn_fcl);
        } else {
            launcherGroup.clearChecked();
        }
        syncing = false;
        updateLauncherStatus(type);
    }

    private void updateLauncherStatus(String type) {
        launcherStatus.setText(getString(R.string.settings_path_current,
                getString(Prefs.launcherNameRes(type))));
    }

    private void updateSafStatus() {
        // FCL 的目录在公共存储，直连即可，不需要 SAF 兜底
        boolean supported = GameFs.safSupported();
        findViewById(R.id.btn_saf).setVisibility(supported ? View.VISIBLE : View.GONE);
        safStatus.setVisibility(supported ? View.VISIBLE : View.GONE);
        if (!supported) {
            return;
        }
        if (SafStore.granted() && SafStore.root() != null) {
            safStatus.setText(getString(R.string.saf_granted, SafStore.describe()));
        } else if (SafStore.granted()) {
            safStatus.setText(R.string.saf_invalid);
        } else {
            safStatus.setText(R.string.saf_none);
        }
    }

    private void updatePreview() {
        String root = Prefs.canonical(text());
        pathPreview.setText(getString(R.string.path_preview,
                root.isEmpty() ? "-" : new File(Prefs.gameDir(root), "versions").getPath()));
    }

    private void save() {
        String root = Prefs.canonical(text());
        if (root.isEmpty()) {
            pathStatus.setText(R.string.path_empty);
            return;
        }

        Prefs.setLauncherRoot(root);
        updatePreview();
        syncLauncherSelection();
        reportStatus(root);
        Toast.makeText(this, R.string.path_saved, Toast.LENGTH_SHORT).show();
    }

    /** 报告目录是否存在，以及其中检测到的游戏版本。 */
    private void reportStatus(String root) {
        if (GameFs.rootReadable()) {
            List<String> found = new ArrayList<>();
            for (McPaths.Entry entry : McPaths.scan()) {
                if (entry.configurable) {
                    found.add(entry.name);
                }
            }
            if (found.isEmpty()) {
                pathStatus.setText(R.string.path_none);
            } else {
                pathStatus.setText(getString(R.string.path_detected, join(found)));
            }
            return;
        }

        File base = new File(root);
        pathStatus.setText(base.isDirectory() ? R.string.path_none : R.string.path_denied);
    }

    private String join(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    private String text() {
        return etPath.getText() == null ? "" : etPath.getText().toString();
    }

    /** 读不到目录时引导用户用系统文件选择器授权。 */
    private void requestSaf() {
        String root = Prefs.canonical(text());
        File versions = new File(Prefs.gameDir(root), "versions");

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.saf_title)
                .setMessage(getString(R.string.saf_message, versions.getPath()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.saf_go, (dialog, which) -> openTreePicker())
                .show();
    }

    private void openTreePicker() {
        try {
            SafPicker.open(this, REQUEST_TREE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.saf_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TREE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri treeUri = data.getData();
        if (treeUri == null) {
            return;
        }

        String path = SafStore.pathOf(treeUri);
        SafStore.persist(this, treeUri, path);

        DocumentFile versions = SafStore.root();
        if (versions == null) {
            SafStore.invalidate();
            safStatus.setText(R.string.saf_unreadable);
            return;
        }

        updateSafStatus();
        List<McPaths.Entry> entries = McPaths.scan();
        if (entries.isEmpty()) {
            safStatus.setText(getString(R.string.saf_empty, path));
        } else {
            safFound(entries);
        }
    }

    private void safFound(List<McPaths.Entry> entries) {
        List<String> names = new ArrayList<>();
        for (McPaths.Entry entry : entries) {
            names.add(entry.name);
        }
        pathStatus.setText(getString(R.string.path_detected, join(names)));
        Toast.makeText(this, R.string.saf_ready, Toast.LENGTH_SHORT).show();
    }
}
