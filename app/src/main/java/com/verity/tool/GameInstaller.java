package com.verity.tool;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.verity.tool.util.GameFs;
import com.verity.tool.util.McPaths;
import com.verity.tool.util.StoragePermission;

import java.io.File;

/**
 * 一键安装入口：先让用户选版本，再跳下载页。
 * 已安装的目录也允许重装，不再二次确认。
 */
public class GameInstaller {

    private static final String DIR_SUFFIX = "-Forge-Verity";

    private final Activity activity;

    public GameInstaller(Activity activity) {
        this.activity = activity;
    }

    public void start() {
        if (!StoragePermission.has(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.perm_title)
                    .setMessage(R.string.perm_message)
                    .setPositiveButton(R.string.perm_go, (d, w) -> StoragePermission.request(activity))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        if (GameFs.saf()) {
            Toast.makeText(activity, R.string.install_saf_unsupported,
                    Toast.LENGTH_LONG).show();
            return;
        }
        File root = McPaths.rootDir();
        if (root == null || !root.isDirectory()) {
            Toast.makeText(activity, R.string.path_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        chooseVersion(root);
    }

    private void chooseVersion(File root) {
        final String[] versions = McPaths.VERSIONS;
        String[] labels = new String[versions.length];
        for (int i = 0; i < versions.length; i++) {
            File dir = new File(root, versions[i] + DIR_SUFFIX);
            String base = activity.getString(R.string.install_version_label, versions[i]);
            labels[i] = dir.exists()
                    ? base + " · " + activity.getString(R.string.version_available)
                    : base;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.install_choose)
                .setItems(labels, (DialogInterface d, int which) -> openDownloadPage(versions[which]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openDownloadPage(String version) {
        activity.startActivity(new Intent(activity, DownloadActivity.class)
                .putExtra(DownloadActivity.EXTRA_VERSION, version + DIR_SUFFIX));
    }
}