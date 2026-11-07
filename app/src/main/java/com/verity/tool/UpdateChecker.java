package com.verity.tool;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.verity.tool.util.GitHubUtil;
import com.verity.tool.util.NetUtil;
import com.verity.tool.util.Notify;

import java.io.File;

/** 检查 GitHub 最新 release，下载 APK 到缓存并拉起安装。 */
public class UpdateChecker {

    private static final int NOTIF_ID = 102;

    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());

    public UpdateChecker(Activity activity) {
        this.activity = activity;
    }

    /** 查询最新 tag；比当前版本新才提示下载。 */
    public void check() {
        Toast.makeText(activity, R.string.update_checking, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                GitHubUtil.Release r = GitHubUtil.latest();
                String current = currentVersion();
                if (r.tag == null || r.tag.isEmpty()) {
                    main.post(() -> Toast.makeText(activity, R.string.update_none, Toast.LENGTH_SHORT).show());
                    return;
                }
                // 资源包 tag（如 resource-package-v1）不是应用版本，跳过
                if (!r.tag.matches("(?i)v?[0-9]+([.][0-9]+)*")) {
                    main.post(() -> Toast.makeText(activity, R.string.update_none, Toast.LENGTH_SHORT).show());
                    return;
                }
                if (GitHubUtil.compareVersions(r.tag, current) <= 0) {
                    main.post(() -> Toast.makeText(activity, R.string.update_none, Toast.LENGTH_SHORT).show());
                    return;
                }
                if (r.apkUrl == null) {
                    main.post(() -> Toast.makeText(activity,
                            activity.getString(R.string.update_download_error, "release 中没有 apk"),
                            Toast.LENGTH_LONG).show());
                    return;
                }
                main.post(() -> promptDownload(r));
            } catch (Exception e) {
                main.post(() -> Toast.makeText(activity,
                        activity.getString(R.string.update_download_error, e.getMessage()),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void promptDownload(GitHubUtil.Release release) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.settings_update_title)
                .setMessage(activity.getString(R.string.update_found, release.tag))
                .setPositiveButton(R.string.update_start_download,
                        (d, w) -> download(release))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void download(GitHubUtil.Release release) {
        // 时间戳目录：便于用户整体清理，也避免覆盖正在安装的包
        File dir = new File(activity.getExternalCacheDir(),
                "Download/apk/" + System.currentTimeMillis());
        File apk = new File(dir, "VerityTool-" + release.tag + ".apk");

        Notify.createChannel(activity);
        Notify.updateProgress(activity, NOTIF_ID,
                activity.getString(R.string.settings_update_title), 0, 0,
                activity.getString(R.string.update_start_download));

        NetUtil.download(release.apkUrl, apk, new NetUtil.Progress() {
            @Override
            public void onProgress(int downloaded, int total) {
                int pct = total > 0 ? (int) (downloaded * 100.0 / total) : 0;
                Notify.updateProgress(activity, NOTIF_ID,
                        activity.getString(R.string.settings_update_title),
                        downloaded, total,
                        activity.getString(R.string.update_downloading, pct));
            }

            @Override
            public void onSuccess(File file) {
                Notify.cancel(activity, NOTIF_ID);
                main.post(() -> new AlertDialog.Builder(activity)
                        .setTitle(R.string.settings_update_title)
                        .setMessage(R.string.update_install_prompt)
                        .setPositiveButton(R.string.ok, (d, w) -> install(file))
                        .setNegativeButton(R.string.cancel, null)
                        .show());
            }

            @Override
            public void onError(String message) {
                Notify.cancel(activity, NOTIF_ID);
                main.post(() -> Toast.makeText(activity,
                        activity.getString(R.string.update_download_error, message),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    /** 缓存目录里的 apk 需要 FileProvider 才能被安装器读取。 */
    private void install(File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(i);
            return;
        }
        Uri uri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(i);
    }

    private String currentVersion() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0";
        }
    }
}