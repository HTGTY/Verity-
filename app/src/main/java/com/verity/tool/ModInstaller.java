package com.verity.tool;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.verity.tool.util.StoragePermission;

/** 下载模组入口：确认存储权限后进入下载页，选版本与模组都在页面内完成。 */
public class ModInstaller {

    private final Activity activity;

    public ModInstaller(Activity activity) {
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
        activity.startActivity(new Intent(activity, DownloadActivity.class));
    }
}
