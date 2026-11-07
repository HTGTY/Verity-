package com.verity.tool.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/** 文件摘要计算，用于下载校验。 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha1(File file) {
        return digest(file, "SHA-1");
    }

    public static String sha512(File file) {
        return digest(file, "SHA-512");
    }

    /** 从输入流算摘要，调用方负责关闭流。 */
    public static String digest(InputStream in, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buf = new byte[64 * 1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
            return hex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private static String digest(File file, String algorithm) {
        if (file == null || !file.isFile()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return digest(in, algorithm);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
