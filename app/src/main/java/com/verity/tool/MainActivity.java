package com.verity.tool;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.verity.tool.util.McPaths;

public class MainActivity extends BaseActivity {

    private static final int REQ_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView quick = findViewById(R.id.card_quick);
        MaterialCardView fix = findViewById(R.id.card_fix);

        quick.setOnClickListener(v -> {
            if (ensurePermission()) {
                startActivity(new Intent(this, QuickConfigActivity.class));
            }
        });
        fix.setOnClickListener(v -> {
            if (ensurePermission()) {
                startActivity(new Intent(this, MemoryFixActivity.class));
            }
        });

        animateEntrance(quick, 0);
        animateEntrance(fix, 70);

        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        TextView pathHint = findViewById(R.id.path_hint);
        pathHint.setText(getString(R.string.main_path_hint, McPaths.rootDir().getPath()));
    }

    private void animateEntrance(MaterialCardView card, long offset) {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        anim.setStartOffset(offset);
        card.startAnimation(anim);
    }

    private boolean ensurePermission() {
        if (hasStoragePermission()) {
            return true;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perm_title)
                .setMessage(R.string.perm_message)
                .setPositiveButton(R.string.perm_go, (dialog, which) -> openPermissionSettings())
                .setNegativeButton(R.string.cancel, null)
                .show();
        return false;
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void openPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE
                && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, R.string.perm_denied, Toast.LENGTH_SHORT).show();
        }
    }
}
