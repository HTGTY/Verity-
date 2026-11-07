package com.verity.tool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.verity.tool.util.GameFs;
import com.verity.tool.util.McPaths;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryFixActivity extends BaseActivity {

    private String version;

    private TextView dirHint;
    private LinearLayout dirList;
    private LinearLayout saveList;
    private TextView emptyHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_fix);

        dirHint = findViewById(R.id.dir_hint);
        dirList = findViewById(R.id.dir_list);
        saveList = findViewById(R.id.save_list);
        emptyHint = findViewById(R.id.empty_hint);

        refreshVersions();
    }

    private void refreshVersions() {
        DirPicker.render(this, dirList, dirHint, McPaths.scan(), entry -> {
            version = entry.name;
            loadSaves();
        }, DirPicker.COLLAPSE_LIMIT);
    }

    private void loadSaves() {
        saveList.removeAllViews();
        emptyHint.setText("");
        List<GameFs.Save> saves = GameFs.saves(version);
        if (saves.isEmpty()) {
            emptyHint.setText(R.string.no_saves);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (final GameFs.Save save : saves) {
            View item = inflater.inflate(R.layout.item_save, saveList, false);
            TextView name = item.findViewById(R.id.save_name);
            TextView time = item.findViewById(R.id.save_time);
            name.setText(save.name);
            time.setText(getString(R.string.save_modified,
                    format.format(new Date(save.modified))));
            item.setOnClickListener(v -> confirmDelete(save));
            saveList.addView(item);
        }
    }

    private void confirmDelete(final GameFs.Save save) {
        if (!save.hasMemory) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.title_no_fix)
                    .setMessage(getString(R.string.no_memory_in_save, save.name))
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_warning)
                .setMessage(R.string.confirm_fix)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    boolean deleted = GameFs.deleteMemory(version, save.name);
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
}
