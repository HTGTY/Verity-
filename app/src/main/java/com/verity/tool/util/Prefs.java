package com.verity.tool.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.verity.tool.R;

import java.io.File;

/** 应用设置：深色模式、莫奈取色、语言与启动器根目录。 */
public final class Prefs {

    /** 启动器类型。自定义指用户手动填写的路径。 */
    public static final String LAUNCHER_FCL = "fcl";
    public static final String LAUNCHER_ZL2 = "zl2";
    public static final String LAUNCHER_CUSTOM = "custom";

    public static final String THEME_FOLLOW = "follow";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    public static final String DEFAULT_ROOT = "/storage/emulated/0/FCL";

    /** Zalith Launcher 2 的游戏数据目录，Minecraft 直接放在 files/ 下。 */
    public static final String ZL2_ROOT =
            "/storage/emulated/0/Android/data/com.movtery.zalithlauncher.v2/files";

    private static final String FILE = "verity_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_MONET = "monet_enabled";
    private static final String KEY_LANG = "language";
    private static final String KEY_ROOT = "launcher_root";
    private static final String KEY_SAF = "saf_versions_uri";
    private static final String KEY_SAF_PATH = "saf_versions_path";

    private static Context app;

    private Prefs() {
    }

    public static void init(Context context) {
        app = context.getApplicationContext();
    }

    public static Context context() {
        return app;
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
        return canonical(sp().getString(KEY_ROOT, DEFAULT_ROOT));
    }

    public static void setLauncherRoot(String root) {
        sp().edit().putString(KEY_ROOT, canonical(root)).apply();
    }

    /** 由根目录判断当前使用的启动器。 */
    public static String launcherType() {
        return launcherType(launcherRoot());
    }

    public static String launcherType(String root) {
        String path = canonical(root);
        if (ZL2_ROOT.equals(path)) {
            return LAUNCHER_ZL2;
        }
        if (DEFAULT_ROOT.equals(path)) {
            return LAUNCHER_FCL;
        }
        return LAUNCHER_CUSTOM;
    }

    /** 启动器的名称，用于界面展示。 */
    public static int launcherNameRes(String type) {
        if (LAUNCHER_ZL2.equals(type)) {
            return R.string.launcher_name_zl2;
        }
        if (LAUNCHER_FCL.equals(type)) {
            return R.string.launcher_name_fcl;
        }
        return R.string.launcher_name_custom;
    }

    /** 启动器根目录对应的 .minecraft 目录，没有 .minecraft 时回退到根目录本身。 */
    public static File gameDir(String root) {
        File base = new File(canonical(root));
        File mc = new File(base, ".minecraft");
        if (mc.isDirectory()) {
            return mc;
        }
        if (new File(base, "versions").isDirectory()) {
            return base;
        }
        return mc;
    }

    /** 当前启动器的 versions 目录。 */
    public static File versionsDir() {
        return new File(gameDir(launcherRoot()), "versions");
    }

    /** 手动授权的 SAF 目录（通常是 versions 本身），为空表示未授权。 */
    public static String safTreeUri() {
        String uri = sp().getString(KEY_SAF, "");
        return uri == null ? "" : uri;
    }

    public static void setSafTree(String uri, String path) {
        sp().edit().putString(KEY_SAF, uri == null ? "" : uri)
                .putString(KEY_SAF_PATH, path == null ? "" : path).apply();
    }

    /** 授权时用户看到的目录，仅用于界面展示。 */
    public static String safTreePath() {
        String path = sp().getString(KEY_SAF_PATH, "");
        return path == null ? "" : path;
    }

    public static void clearSafTree() {
        sp().edit().remove(KEY_SAF).remove(KEY_SAF_PATH).apply();
    }

    /** 统一 /sdcard 前缀并去掉结尾多余的斜杠。 */
    public static String canonical(String path) {
        if (path == null) {
            return "";
        }
        String value = path.trim();
        if (value.equals("/sdcard")) {
            value = "/storage/emulated/0";
        } else if (value.startsWith("/sdcard/")) {
            value = "/storage/emulated/0/" + value.substring("/sdcard/".length());
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
