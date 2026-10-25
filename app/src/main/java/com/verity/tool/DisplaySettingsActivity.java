package com.verity.tool;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.verity.tool.util.Prefs;

/** 显示设置：深色模式、莫奈取色与语言。 */
public class DisplaySettingsActivity extends BaseActivity {

    private RadioGroup groupTheme;
    private RadioGroup groupLanguage;
    private MaterialSwitch switchMonet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_settings);

        ((TextView) findViewById(R.id.title_bar)).setText(R.string.title_display);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        groupTheme = findViewById(R.id.group_theme);
        groupLanguage = findViewById(R.id.group_language);
        switchMonet = findViewById(R.id.switch_monet);

        switchMonet.setChecked(Prefs.monetEnabled());
        groupTheme.check(checkedThemeButton());
        groupLanguage.check(checkedLanguageButton());

        groupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            Prefs.setThemeMode(themeModeOf(checkedId));
            AppCompatDelegate.setDefaultNightMode(Prefs.nightMode());
        });
        switchMonet.setOnCheckedChangeListener((button, checked) -> {
            Prefs.setMonetEnabled(checked);
            recreate();
        });
        groupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            Prefs.setLanguage(checkedId == R.id.rb_lang_en ? Prefs.LANG_EN : Prefs.LANG_ZH);
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(Prefs.language()));
        });
    }

    private int checkedThemeButton() {
        String mode = Prefs.themeMode();
        if (Prefs.THEME_LIGHT.equals(mode)) {
            return R.id.rb_theme_light;
        }
        if (Prefs.THEME_DARK.equals(mode)) {
            return R.id.rb_theme_dark;
        }
        return R.id.rb_theme_follow;
    }

    private int checkedLanguageButton() {
        return Prefs.LANG_EN.equals(Prefs.language()) ? R.id.rb_lang_en : R.id.rb_lang_zh;
    }

    private String themeModeOf(int checkedId) {
        if (checkedId == R.id.rb_theme_light) {
            return Prefs.THEME_LIGHT;
        }
        if (checkedId == R.id.rb_theme_dark) {
            return Prefs.THEME_DARK;
        }
        return Prefs.THEME_FOLLOW;
    }
}
