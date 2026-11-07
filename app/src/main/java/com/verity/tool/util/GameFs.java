package com.verity.tool.util;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏目录的统一入口：能用 File 直接读写就用 File，
 * 读不到（典型是 /Android/data）时自动改走 SAF 授权目录。
 */
public final class GameFs {

    /** 一个存档目录。 */
    public static final class Save {
        public final String name;
        public final long modified;
        public final boolean hasMemory;

        Save(String name, long modified, boolean hasMemory) {
            this.name = name;
            this.modified = modified;
            this.hasMemory = hasMemory;
        }
    }

    private GameFs() {
    }

    private static File directRoot() {
        return McPaths.rootDir();
    }

    /** versions 目录能否用 File 直接列出。 */
    public static boolean direct() {
        File root = directRoot();
        return root != null && root.isDirectory() && root.listFiles() != null;
    }

    /** 直连不可用但拿到了匹配当前启动器的 SAF 授权；FCL 不走 SAF。 */
    public static boolean saf() {
        if (direct()) {
            return false;
        }
        if (Prefs.LAUNCHER_FCL.equals(Prefs.launcherType())) {
            return false;
        }
        return SafStore.root() != null;
    }

    /** 当前启动器是否需要 SAF 兜底（FCL 不需要）。 */
    public static boolean safSupported() {
        return !Prefs.LAUNCHER_FCL.equals(Prefs.launcherType());
    }

    public static boolean usable() {
        return direct() || saf();
    }

    /** 界面上展示当前数据来源。 */
    public static String sourceDesc() {
        if (direct()) {
            File root = directRoot();
            return root == null ? "" : root.getPath();
        }
        if (saf()) {
            return SafStore.describe();
        }
        return "";
    }

    /** 扫描 versions 下所有含 .jar 的目录，按修改时间倒序。 */
    public static List<McPaths.Entry> scan() {
        if (direct()) {
            return scanDirect();
        }
        if (saf()) {
            return scanSaf();
        }
        return new ArrayList<>();
    }

    private static List<McPaths.Entry> scanDirect() {
        List<McPaths.Entry> list = new ArrayList<>();
        File[] dirs = directRoot().listFiles();
        if (dirs == null) {
            return list;
        }
        for (File dir : dirs) {
            if (!dir.isDirectory() || !McPaths.hasJar(dir)) {
                continue;
            }
            boolean configurable = new File(new File(dir, "config"), "verity-common.toml").isFile();
            list.add(new McPaths.Entry(dir.getName(), dir, dir.lastModified(), configurable));
        }
        list.sort((a, b) -> Long.compare(b.modified, a.modified));
        return list;
    }

    private static List<McPaths.Entry> scanSaf() {
        List<McPaths.Entry> list = new ArrayList<>();
        for (DocumentFile dir : SafStore.childDirs(SafStore.root())) {
            if (!hasJar(dir)) {
                continue;
            }
            DocumentFile config = SafStore.child(dir, "config");
            DocumentFile toml = SafStore.child(config, "verity-common.toml");
            boolean configurable = toml != null && toml.isFile();
            list.add(new McPaths.Entry(dir.getName() == null ? "" : dir.getName(),
                    null, dir.lastModified(), configurable));
        }
        return list;
    }

    private static boolean hasJar(DocumentFile dir) {
        for (DocumentFile file : SafStore.children(dir)) {
            String name = file.getName();
            if (file.isFile() && name != null && name.endsWith(".jar")) {
                return true;
            }
        }
        return false;
    }

    /** 读取版本的 verity-common.toml，失败返回 null。 */
    public static String readToml(String version) {
        if (direct()) {
            File toml = McPaths.commonToml(version);
            return toml.isFile() ? TomlEditor.readText(toml) : null;
        }
        return SafStore.readText(SafStore.resolve(version + "/config/verity-common.toml"));
    }

    /** 写回 verity-common.toml，成功返回 true。 */
    public static boolean writeToml(String version, String text) {
        if (direct()) {
            File toml = McPaths.commonToml(version);
            return toml.isFile() && TomlEditor.writeText(toml, text);
        }
        return SafStore.writeText(SafStore.resolve(version + "/config/verity-common.toml"), text);
    }

    /** 列出存档目录，带修改时间。 */
    public static List<Save> saves(String version) {
        List<Save> list = new ArrayList<>();
        if (direct()) {
            File savesDir = McPaths.savesDir(version);
            File[] files = savesDir.listFiles();
            if (files == null) {
                return list;
            }
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }
                boolean hasMemory = new File(file, "verity_memory.json").isFile();
                list.add(new Save(file.getName(), file.lastModified(), hasMemory));
            }
        } else if (saf()) {
            DocumentFile savesDir = SafStore.resolve(version + "/saves");
            for (DocumentFile dir : SafStore.childDirs(savesDir)) {
                DocumentFile memory = SafStore.child(dir, "verity_memory.json");
                list.add(new Save(dir.getName() == null ? "" : dir.getName(),
                        dir.lastModified(), memory != null && memory.isFile()));
            }
        }
        list.sort((a, b) -> Long.compare(b.modified, a.modified));
        return list;
    }

    /** 删除存档里的 verity_memory.json。 */
    public static boolean deleteMemory(String version, String saveName) {
        if (direct()) {
            File memory = new File(new File(McPaths.savesDir(version), saveName),
                    "verity_memory.json");
            return memory.isFile() && memory.delete();
        }
        DocumentFile memory = SafStore.resolve(version + "/saves/" + saveName
                + "/verity_memory.json");
        return SafStore.delete(memory);
    }

    /** versions 目录是否存在，用于区分「路径错误」和「没有版本」。 */
    public static boolean rootReadable() {
        if (direct()) {
            return true;
        }
        return saf();
    }
}
