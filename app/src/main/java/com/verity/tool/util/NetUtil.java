package com.verity.tool.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** 阻塞式 HTTP GET + 流式下载（用于 GitHub release 与模组包）。 */
public final class NetUtil {

    private NetUtil() {
    }

    public interface Progress {
        void onProgress(int downloaded, int total);

        void onSuccess(File file);

        void onError(String message);
    }

    /** 同步返回响应文本。 */
    public static String get(String urlString) throws IOException {
        HttpURLConnection conn = newConnection(urlString);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod("GET");
        try {
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            int code = conn.getResponseCode();
            if (code >= 300) {
                throw new IOException("HTTP " + code);
            }
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8000];
            int n;
            while ((n = is.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            is.close();
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    /** 把 url 保存到 dest，不阻塞调用线程。 */
    public static void download(String urlString, File dest, Progress cb) {
        Thread t = new Thread(() -> {
            try {
                HttpURLConnection conn = newConnection(urlString);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code >= 300) {
                    conn.disconnect();
                    if (cb != null) cb.onError("下载失败 HTTP " + code);
                    return;
                }
                int total = conn.getContentLength();
                InputStream is = conn.getInputStream();
                dest.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(dest);
                byte[] buf = new byte[8000];
                long sum = 0;
                int n;
                while ((n = is.read(buf)) != -1) {
                    sum += n;
                    fos.write(buf, 0, n);
                    if (total > 0 && cb != null) {
                        cb.onProgress((int) sum, total);
                    }
                }
                fos.flush();
                fos.close();
                is.close();
                conn.disconnect();
                if (cb != null) cb.onSuccess(dest);
            } catch (IOException e) {
                if (cb != null) cb.onError(e.getMessage());
            }
        });
        t.start();
    }

    private static HttpURLConnection newConnection(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}