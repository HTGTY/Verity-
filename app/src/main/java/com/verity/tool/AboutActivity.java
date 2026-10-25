package com.verity.tool;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/** 关于：作者信息与联系方式，点击可复制。 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.title_about);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.about_version_value)).setText(versionName());

        findViewById(R.id.row_email).setOnClickListener(v ->
                copy(getString(R.string.about_email_label), getString(R.string.about_email_value)));
        findViewById(R.id.row_qq).setOnClickListener(v ->
                copy(getString(R.string.about_qq_label), getString(R.string.about_qq_value)));
        findViewById(R.id.row_group).setOnClickListener(v ->
                copy(getString(R.string.about_group_label), getString(R.string.about_group_value)));
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "-";
        }
    }

    private void copy(String label, String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, getString(R.string.copied, label), Toast.LENGTH_SHORT).show();
    }
}
