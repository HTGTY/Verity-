package com.verity.tool.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 游戏目录与版本扫描。 */
public final class McPaths {

    public static final String[] VERSIONS = {"1.20.1", "1.21.1"};

    private McPaths() {
    }

    /** versions 根目录来自用户设置，可随时修改。 */
    public static File rootDir() {
        return Prefs.versionsDir();
    }

    public static File versionDir(String version) {
        return new File(rootDir(), version);
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

    /** 目录下直接存在任意 .jar 文件。 */
    public static boolean hasJar(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                return true;
            }
        }
        return false;
    }

    /** 版本目录存在，且其中包含游戏本体 jar。 */
    public static boolean versionInstalled(String version) {
        File dir = versionDir(version);
        return dir.isDirectory() && hasJar(dir);
    }

    /** versions 下的一个游戏版本目录。 */
    public static final class Entry {
        public final String name;
        public final File dir;
        public final long modified;
        public final boolean configurable;

        Entry(String name, File dir, long modified, boolean configurable) {
            this.name = name;
            this.dir = dir;
            this.modified = modified;
            this.configurable = configurable;
        }

        public boolean hasSaves() {
            File saves = new File(dir, "saves");
            return saves.isDirectory();
        }
    }

    /** versions 目录是否可读，用于区分「没有版本」与「没有权限」。 */
    public static boolean rootReadable() {
        return GameFs.rootReadable();
    }

    /**
     * 扫描 versions 下所有含 .jar 的目录，按修改时间倒序。
     * configurable 表示存在 config/verity-common.toml。
     * 直连读不到时自动改走 SAF 授权目录。
     */
    public static List<Entry> scan() {
        return GameFs.scan();
    }
}