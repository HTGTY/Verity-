package com.verity.tool.util;

import org.json.JSONArray;
import org.json.JSONObject;

/** GitHub release 接口的最小封装。 */
public final class GitHubUtil {

    public static final String REPO = "HTGTY/Verity-";

    private GitHubUtil() {
    }

    public static final class Release {
        public final String tag;
        public final String apkUrl;
        public final long id;
        public final String body;

        public Release(String tag, String apkUrl, long id, String body) {
            this.tag = tag;
            this.apkUrl = apkUrl;
            this.id = id;
            this.body = body;
        }
    }

    /** 获取最新发布（含 tag_name + apk 下载地址）。 */
    public static Release latest() throws Exception {
        String json = NetUtil.get("https://api.github.com/repos/" + REPO + "/releases/latest");
        return parseRelease(new JSONObject(json));
    }

    /** 根据 tag 获取指定名字的资产直链。 */
    public static String assetUrl(String tag, String assetName) throws Exception {
        String json = NetUtil.get("https://api.github.com/repos/" + REPO + "/releases/tags/" + tag);
        return findAsset(new JSONObject(json), assetName);
    }

    private static Release parseRelease(JSONObject r) throws Exception {
        String tag = r.optString("tag_name");
        long id = r.optLong("id");
        String body = r.optString("body", "");
        String apk = null;
        JSONArray assets = r.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.getString("name").endsWith(".apk")) {
                    apk = a.getString("browser_download_url");
                    break;
                }
            }
        }
        return new Release(tag, apk, id, body);
    }

    static String findAsset(JSONObject r, String name) throws Exception {
        JSONArray assets = r.optJSONArray("assets");
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.getString("name").equals(name)) {
                return a.getString("browser_download_url");
            }
        }
        return null;
    }

    /** 把版本号（如 v1.3 / 1.2）比较：相等=0，a 小于 b 为负。 */
    public static int compareVersions(String a, String b) {
        String[] xs = stripPrefix(a).split("[.]");
        String[] ys = stripPrefix(b).split("[.]");
        int len = Math.max(xs.length, ys.length);
        for (int i = 0; i < len; i++) {
            int x = i < xs.length ? parseInt(xs[i], 0) : 0;
            int y = i < ys.length ? parseInt(ys[i], 0) : 0;
            int c = Integer.compare(x, y);
            if (c != 0) return c;
        }
        return 0;
    }

    private static String stripPrefix(String v) {
        if (v == null) return "0";
        String s = v;
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        return s;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}