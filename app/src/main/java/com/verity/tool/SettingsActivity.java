package com.verity.tool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.verity.tool.util.Prefs;

/** 设置主页：显示、配置、关于三个入口。 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.title_settings);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView pathDesc = findViewById(R.id.settings_path_desc);
        pathDesc.setText(getString(R.string.settings_path_current,
                getString(Prefs.launcherNameRes(Prefs.launcherType()))));

        findViewById(R.id.card_display).setOnClickListener(v ->
                startActivity(new Intent(this, DisplaySettingsActivity.class)));
        findViewById(R.id.card_path).setOnClickListener(v ->
                startActivity(new Intent(this, PathSettingsActivity.class)));
        findViewById(R.id.card_update).setOnClickListener(v -> new UpdateChecker(this).check());
        findViewById(R.id.card_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }
}
