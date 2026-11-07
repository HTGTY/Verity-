package com.verity.tool.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 解压 zip 到目录（保持内部目录结构）。 */
public final class ZipUtil {

    private ZipUtil() {
    }

    public static void unzip(File zip, File dest) throws IOException {
        dest.mkdirs();
        try (InputStream fis = new java.io.FileInputStream(zip);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File f = new File(dest, e.getName());
                if (e.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                f.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(f)) {
                    byte[] buf = new byte[8000];
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        os.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}