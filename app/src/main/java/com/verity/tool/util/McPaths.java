package com.verity.tool.util;

import java.io.File;

/** FCL 启动器中 Verity 模组相关路径。 */
public final class McPaths {

    public static final String[] VERSIONS = {"1.20.1", "1.21.1"};

    private McPaths() {
    }

    /** versions 根目录来自用户设置，可随时修改。 */
    public static File rootDir() {
        return Prefs.versionsDir();
    }

    public static File versionDir(String version) {
        return new File(rootDir(), version + "-Forge");
    }

    public static File configDir(String version) {
        return new File(versionDir(version), "config");
    }

    public static File commonToml(String version) {
        return new File(configDir(version), "verity-common.toml");
    }

    public static File savesDir(String version) {
        return new File(versionDir(version), "saves");
    }

    /** 版本目录存在，且其中包含游戏本体文件（jar / json）。 */
    public static boolean versionInstalled(String version) {
        File dir = versionDir(version);
        if (!dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isFile() && (name.endsWith(".jar") || name.endsWith(".json"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyVersionInstalled() {
        for (String version : VERSIONS) {
            if (versionInstalled(version)) {
                return true;
            }
        }
        return false;
    }
}
