package com.verity.tool.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/** Modrinth 版本接口，只用来补全缺失的 SHA1。 */
public final class ModrinthApi {

    private static final String BASE = "https://api.modrinth.com/v2/version/";

    private ModrinthApi() {
    }

    /** 取该版本首个文件的 SHA1，失败抛 IOException。 */
    public static String sha1(String versionId) throws IOException {
        String json = NetUtil.get(BASE + versionId);
        try {
            JSONObject root = new JSONObject(json);
            JSONArray files = root.getJSONArray("files");
            if (files.length() == 0) {
                throw new IOException("no files");
            }
            JSONObject file = files.getJSONObject(0);
            JSONObject hashes = file.getJSONObject("hashes");
            String sha1 = hashes.optString("sha1", "");
            if (sha1.isEmpty()) {
                throw new IOException("no sha1");
            }
            return sha1;
        } catch (Exception e) {
            throw new IOException("parse failed");
        }
    }
}
