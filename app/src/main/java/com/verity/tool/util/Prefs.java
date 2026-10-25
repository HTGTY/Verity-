package com.verity.tool.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;

/** 应用设置：深色模式、莫奈取色、语言与启动器根目录。 */
public final class Prefs {

    public static final String THEME_FOLLOW = "follow";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    public static final String DEFAULT_ROOT = "/storage/emulated/0/FCL";

    private static final String FILE = "verity_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_MONET = "monet_enabled";
    private static final String KEY_LANG = "language";
    private static final String KEY_ROOT = "launcher_root";

    private static Context app;

    private Prefs() {
    }

    public static void init(Context context) {
        app = context.getApplicationContext();
    }

    private static SharedPreferences sp() {
        return app.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String themeMode() {
        return sp().getString(KEY_THEME, THEME_FOLLOW);
    }

    public static void setThemeMode(String mode) {
        sp().edit().putString(KEY_THEME, mode).apply();
    }

    /** 将保存的模式转换成 AppCompatDelegate 的夜间模式常量。 */
    public static int nightMode() {
        String mode = themeMode();
        if (THEME_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (THEME_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static boolean monetEnabled() {
        return sp().getBoolean(KEY_MONET, true);
    }

    public static void setMonetEnabled(boolean enabled) {
        sp().edit().putBoolean(KEY_MONET, enabled).apply();
    }

    public static String language() {
        return sp().getString(KEY_LANG, LANG_ZH);
    }

    public static void setLanguage(String language) {
        sp().edit().putString(KEY_LANG, language).apply();
    }

    public static String launcherRoot() {
        return sp().getString(KEY_ROOT, DEFAULT_ROOT);
    }

    public static void setLauncherRoot(String root) {
        sp().edit().putString(KEY_ROOT, root).apply();
    }

    /** 启动器根目录下的 .minecraft/versions。 */
    public static File versionsDir() {
        String root = launcherRoot();
        if (root.endsWith("/")) {
            return new File(root + ".minecraft/versions");
        }
        return new File(root + "/.minecraft/versions");
    }
}
