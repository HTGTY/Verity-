package com.verity.tool.util;

/** v1.6 要下载的三个模组，版本与哈希都按 1.20.1 Forge 固定。 */
public final class ModSpec {

    public final String key;
    /** 展示名。 */
    public final String name;
    /** 展示用版本号。 */
    public final String version;
    /** 落到 mods 目录下的文件名。 */
    public final String fileName;
    public final String url;
    /** 预期字节数，0 表示未知。 */
    public final long size;
    /** 已知摘要，null 表示需要动态查询。 */
    public final String sha1;
    public final String sha512;
    /** Modrinth 版本 ID，用于动态获取 sha1。 */
    public final String hashVersionId;

    private ModSpec(String key, String name, String version, String fileName,
                    String url, long size, String sha1, String sha512, String hashVersionId) {
        this.key = key;
        this.name = name;
        this.version = version;
        this.fileName = fileName;
        this.url = url;
        this.size = size;
        this.sha1 = sha1;
        this.sha512 = sha512;
        this.hashVersionId = hashVersionId;
    }

    public static final ModSpec YACL = new ModSpec(
            "yacl",
            "Yet Another Config Lib (YACL)",
            "3.4.2+1.20.1-forge",
            "YetAnotherConfigLib-3.4.2+1.20.1-forge.jar",
            "https://cdn.modrinth.com/data/1eAoo2KR/versions/lALu7fvN/"
                    + "YetAnotherConfigLib-3.4.2+1.20.1-forge.jar",
            0, null, null, "lALu7fvN");

    public static final ModSpec GECKOLIB = new ModSpec(
            "geckolib",
            "GeckoLib",
            "4.7.1.2",
            "geckolib-forge-1.20.1-4.7.1.2.jar",
            "https://cdn.modrinth.com/data/8BmcQJ2H/versions/e72SjmmI/"
                    + "geckolib-forge-1.20.1-4.7.1.2.jar",
            0,
            "c2b5d7ccf77f505996701ba477741d7e5cd034e1",
            "fb41b2cd47b46bf84d101e4d1ba78f04151b85a4767f32bc84c9629312ed41ce"
                    + "558cd5660ff50f435839a23c4bea04db6710d3e9f804843ab52090bce1cf7fbb",
            null);

    public static final ModSpec VERITY = new ModSpec(
            "verity",
            "Verity JE 6",
            "6.0.0-beta.1",
            "verity-6.jar",
            "https://cdn.modrinth.com/data/on1Y0osD/versions/CXsEzVwJ/verity-6.jar",
            254898104L,
            "609c799d7350a657cf2193839224bd6c2f9971b2",
            "25771d7476639e5669bea28d0beb503813ca304cfd6ddb080be3eb4186fbea34"
                    + "024f3a0e93bcaeb4cef4ecfb925420956a5b0e92f4a957c7ff7b3a4a22127b6e",
            null);

    public static ModSpec[] all() {
        return new ModSpec[]{YACL, GECKOLIB, VERITY};
    }

    public static String[] keys() {
        ModSpec[] specs = all();
        String[] keys = new String[specs.length];
        for (int i = 0; i < specs.length; i++) {
            keys[i] = specs[i].key;
        }
        return keys;
    }

    /** 按 key 取模组，未知 key 返回 null。 */
    public static ModSpec of(String key) {
        for (ModSpec spec : all()) {
            if (spec.key.equals(key)) {
                return spec;
            }
        }
        return null;
    }
}
