package com.verity.tool;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.verity.tool.util.McPaths;
import com.verity.tool.util.Prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 配置设置：修改启动器根目录，应用内部拼接 .minecraft/versions。 */
public class PathSettingsActivity extends BaseActivity {

    private TextInputEditText etPath;
    private TextView pathPreview;
    private TextView pathStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_settings);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.title_path);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etPath = findViewById(R.id.et_path);
        pathPreview = findViewById(R.id.path_preview);
        pathStatus = findViewById(R.id.path_status);

        etPath.setText(Prefs.launcherRoot());
        updatePreview();

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
            }
        });

        MaterialButton btnDefault = findViewById(R.id.btn_default);
        btnDefault.setOnClickListener(v -> {
            etPath.setText(Prefs.DEFAULT_ROOT);
            save();
        });

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> save());
    }

    private void updatePreview() {
        String root = normalize(text());
        pathPreview.setText(getString(R.string.path_preview,
                root.isEmpty() ? "-" : new File(root, ".minecraft/versions").getPath()));
    }

    private void save() {
        String root = normalize(text());
        if (root.isEmpty()) {
            pathStatus.setText(R.string.path_empty);
            return;
        }
        File dir = new File(root);
        if (!dir.isDirectory()) {
            pathStatus.setText(R.string.path_invalid);
            return;
        }

        Prefs.setLauncherRoot(root);
        List<String> found = new ArrayList<>();
        for (String version : McPaths.VERSIONS) {
            if (McPaths.versionInstalled(version)) {
                found.add(version + "-Forge");
            }
        }
        if (found.isEmpty()) {
            pathStatus.setText(R.string.path_none);
        } else {
            pathStatus.setText(getString(R.string.path_detected, join(found)));
        }
        Toast.makeText(this, R.string.path_saved, Toast.LENGTH_SHORT).show();
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

    private static String normalize(String path) {
        String trimmed = path.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String text() {
        return etPath.getText() == null ? "" : etPath.getText().toString();
    }
}
