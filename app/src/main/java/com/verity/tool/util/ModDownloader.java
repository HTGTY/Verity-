package com.verity.tool.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 单个模组的下载：支持 HTTP Range 断点续传、超时重试与摘要校验。
 * 下载内容先落在 .part 文件，校验通过后再由调用方重命名。
 */
public final class ModDownloader {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BUFFER = 64 * 1024;

    public interface Progress {
        void onProgress(long done, long total);
    }

    public interface Cancel {
        boolean canceled();
    }

    public static final class Result {
        public boolean ok;
        public boolean canceled;
        public String error;
        public String message;

        static Result ok(String message) {
            Result r = new Result();
            r.ok = true;
            r.message = message;
            return r;
        }

        static Result fail(String error) {
            Result r = new Result();
            r.error = error;
            return r;
        }

        static Result cancel() {
            Result r = new Result();
            r.canceled = true;
            return r;
        }
    }

    private ModDownloader() {
    }

    /**
     * 下载到 part 文件并校验。
     *
     * @param expectedSha1   期望 SHA1，null 表示不校验
     * @param expectedSha512 期望 SHA512，null 表示不校验
     */
    public static Result fetch(ModSpec spec, File part, String expectedSha1,
                               String expectedSha512, Progress progress, Cancel cancel) {
        String lastError = "";
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (cancel.canceled()) {
                return Result.cancel();
            }
            Result result = attempt(spec, part, expectedSha1, expectedSha512, progress, cancel);
            if (result.ok || result.canceled) {
                return result;
            }
            lastError = result.error;
            if (attempt < MAX_ATTEMPTS) {
                sleep(1200L * attempt);
            }
        }
        return Result.fail(lastError);
    }

    private static Result attempt(ModSpec spec, File part, String expectedSha1,
                                  String expectedSha512, Progress progress, Cancel cancel) {
        long existing = part.isFile() ? part.length() : 0L;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(spec.url).openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", NetUtil.USER_AGENT);
            conn.setRequestProperty("Accept-Encoding", "identity");
            if (existing > 0) {
                conn.setRequestProperty("Range", "bytes=" + existing + "-");
            }
            int code = conn.getResponseCode();
            if (code == 416 && existing > 0) {
                // 断点位置已经不合法，从头再来
                part.delete();
                return Result.fail("HTTP 416");
            }
            if (code >= 300 && code != 304) {
                return Result.fail("HTTP " + code);
            }

            boolean append = code == 206 && existing > 0;
            long total = conn.getContentLength() > 0
                    ? (append ? existing + conn.getContentLength() : conn.getContentLength())
                    : (spec.size > 0 ? spec.size : 0);

            part.getParentFile().mkdirs();
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(part, append);
            byte[] buf = new byte[BUFFER];
            long done = append ? existing : 0L;
            int read;
            try {
                while ((read = in.read(buf)) != -1) {
                    if (cancel.canceled()) {
                        return Result.cancel();
                    }
                    out.write(buf, 0, read);
                    done += read;
                    if (progress != null) {
                        progress.onProgress(done, total);
                    }
                }
            } finally {
                try {
                    out.flush();
                    out.close();
                } catch (Exception ignored) {
                }
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }

            if (total > 0 && done < total) {
                return Result.fail("下载不完整");
            }

            if (expectedSha512 != null && !expectedSha512.isEmpty()) {
                if (!expectedSha512.equalsIgnoreCase(HashUtil.sha512(part))) {
                    part.delete();
                    return Result.fail("SHA512 校验失败");
                }
            } else if (expectedSha1 != null && !expectedSha1.isEmpty()) {
                if (!expectedSha1.equalsIgnoreCase(HashUtil.sha1(part))) {
                    part.delete();
                    return Result.fail("SHA1 校验失败");
                }
            }
            return Result.ok("校验通过");
        } catch (IOException e) {
            return Result.fail(String.valueOf(e.getMessage()));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
