package com.verity.tool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.verity.tool.util.McPaths;
import com.verity.tool.util.ThemeUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryFixActivity extends BaseActivity {

    private String version;

    private MaterialCardView card1201;
    private MaterialCardView card1211;
    private LinearLayout saveList;
    private TextView emptyHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_fix);

        card1201 = findViewById(R.id.card_v1201);
        card1211 = findViewById(R.id.card_v1211);
        saveList = findViewById(R.id.save_list);
        emptyHint = findViewById(R.id.empty_hint);

        card1201.setOnClickListener(v -> selectVersion("1.20.1"));
        card1211.setOnClickListener(v -> selectVersion("1.21.1"));

        refreshVersions();
    }

    private void refreshVersions() {
        boolean has1201 = McPaths.versionInstalled("1.20.1");
        boolean has1211 = McPaths.versionInstalled("1.21.1");

        TextView status1201 = findViewById(R.id.status_v1201);
        TextView status1211 = findViewById(R.id.status_v1211);
        status1201.setText(has1201 ? R.string.version_available : R.string.version_missing);
        status1211.setText(has1211 ? R.string.version_available : R.string.version_missing);

        if (!has1201 && !has1211) {
            emptyHint.setText(R.string.version_missing_all);
        } else {
            selectVersion(has1201 ? "1.20.1" : "1.21.1");
        }
    }

    private void selectVersion(String target) {
        if (!McPaths.versionInstalled(target)) {
            Toast.makeText(this, getString(R.string.toast_version_not_installed, target),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        version = target;
        updateCard(card1201, target.equals("1.20.1"));
        updateCard(card1211, target.equals("1.21.1"));
        loadSaves();
    }

    private void updateCard(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? dp(2) : dp(1));
        card.setStrokeColor(selected
                ? ThemeUtils.color(this, com.verity.tool.R.attr.colorPrimary)
                : ThemeUtils.color(this, com.verity.tool.R.attr.colorOutline));
        card.setCardBackgroundColor(selected
                ? ThemeUtils.color(this, com.verity.tool.R.attr.colorPrimaryContainer)
                : ThemeUtils.color(this, com.verity.tool.R.attr.colorSurface));
    }

    private void loadSaves() {
        saveList.removeAllViews();
        File savesDir = McPaths.savesDir(version);
        if (!savesDir.isDirectory()) {
            emptyHint.setText(R.string.no_saves_dir);
            return;
        }

        List<File> saves = new ArrayList<>();
        File[] files = savesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    saves.add(file);
                }
            }
        }
        if (saves.isEmpty()) {
            emptyHint.setText(R.string.no_saves);
            return;
        }

        saves.sort(new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        emptyHint.setText("");
        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (final File save : saves) {
            View item = inflater.inflate(R.layout.item_save, saveList, false);
            TextView name = item.findViewById(R.id.save_name);
            TextView time = item.findViewById(R.id.save_time);
            name.setText(save.getName());
            time.setText(getString(R.string.save_modified,
                    format.format(new Date(save.lastModified()))));
            item.setOnClickListener(v -> confirmDelete(save));
            saveList.addView(item);
        }
    }

    private void confirmDelete(final File saveDir) {
        final File memory = new File(saveDir, "verity_memory.json");
        if (!memory.isFile()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.title_no_fix)
                    .setMessage(getString(R.string.no_memory_in_save, saveDir.getName()))
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_warning)
                .setMessage(R.string.confirm_fix)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    boolean deleted = memory.delete();
                    if (deleted) {
                        new MaterialAlertDialogBuilder(MemoryFixActivity.this)
                                .setTitle(R.string.title_cleared)
                                .setMessage(R.string.cleared_message)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        Toast.makeText(MemoryFixActivity.this, R.string.delete_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
