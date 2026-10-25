package com.verity.tool;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.color.DynamicColors;
import com.verity.tool.util.Prefs;

/** 所有界面的基类：在 onCreate 之前统一应用深色模式、莫奈取色与语言。 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode());
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(Prefs.language()));
        if (!Prefs.monetEnabled()) {
            setTheme(R.style.Theme_VerityTool_Sky);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
    }
}
