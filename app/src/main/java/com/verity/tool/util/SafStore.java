package com.verity.tool.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 基于 SAF 的目录访问，用于 /Android/data 这类无法用 File 直接读取的位置。
 * 授权得到的 tree URI 指向 versions 目录本身。
 */
public final class SafStore {

    private static DocumentFile cached;
    private static String cachedUri;

    private SafStore() {
    }

    public static boolean granted() {
        return !Prefs.safTreeUri().isEmpty();
    }

    /** 清掉缓存，授权变化后必须调用。 */
    public static void invalidate() {
        cached = null;
        cachedUri = null;
    }

    public static DocumentFile root() {
        return root(Prefs.context());
    }

    /** 返回已定位的 versions 目录；定位失败返回 null。 */
    public static DocumentFile root(Context context) {
        String uri = Prefs.safTreeUri();
        if (uri.isEmpty() || context == null) {
            return null;
        }
        if (cached != null && uri.equals(cachedUri)) {
            return cached;
        }
        try {
            DocumentFile picked = DocumentFile.fromTreeUri(context, Uri.parse(uri));
            if (picked == null || !picked.exists() || !picked.isDirectory()) {
                return null;
            }
            DocumentFile versions = locateVersions(picked);
            if (versions == null) {
                return null;
            }
            cached = versions;
            cachedUri = uri;
            return versions;
        } catch (Exception e) {
            return null;
        }
    }

    /** 保存授权结果；返回是否拿到了读写权限。 */
    public static boolean persist(Context context, Uri treeUri, String path) {
        if (context == null || treeUri == null) {
            return false;
        }
        try {
            context.getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) {
            // 部分系统只授了读权限，仍然记录，读取照常可用
        }
        Prefs.setSafTree(treeUri.toString(), path);
        invalidate();
        return true;
    }

    /**
     * 从授权到的目录出发定位 versions：
     * 先按当前启动器路径推算相对层级，再退回按名字查找。
     */
    private static DocumentFile locateVersions(DocumentFile picked) {
        if (picked.getName() != null && "versions".equals(picked.getName())) {
            return picked;
        }
        String granted = Prefs.canonical(Prefs.safTreePath());
        String root = Prefs.launcherRoot();
        String[] candidates = {
                new File(Prefs.gameDir(root), "versions").getPath(),
                new File(root, "versions").getPath(),
                new File(root, ".minecraft/versions").getPath()
        };
        for (String candidate : candidates) {
            String path = Prefs.canonical(candidate);
            if (granted.isEmpty() || !path.startsWith(granted + "/")) {
                continue;
            }
            DocumentFile found = resolve(picked, path.substring(granted.length() + 1));
            if (found != null && found.isDirectory()) {
                return found;
            }
        }
        DocumentFile child = picked.findFile("versions");
        if (child != null && child.isDirectory()) {
            return child;
        }
        DocumentFile mc = picked.findFile(".minecraft");
        if (mc != null && mc.isDirectory()) {
            DocumentFile inner = mc.findFile("versions");
            if (inner != null && inner.isDirectory()) {
                return inner;
            }
        }
        return null;
    }

    public static List<DocumentFile> children(DocumentFile dir) {
        List<DocumentFile> list = new ArrayList<>();
        if (dir == null) {
            return list;
        }
        DocumentFile[] files;
        try {
            files = dir.listFiles();
        } catch (Exception e) {
            return list;
        }
        Collections.addAll(list, files);
        return list;
    }

    public static List<DocumentFile> childDirs(DocumentFile dir) {
        List<DocumentFile> list = new ArrayList<>();
        for (DocumentFile file : children(dir)) {
            if (file.isDirectory()) {
                list.add(file);
            }
        }
        list.sort(new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile a, DocumentFile b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return list;
    }

    public static DocumentFile child(DocumentFile dir, String name) {
        if (dir == null || name == null) {
            return null;
        }
        try {
            return dir.findFile(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** 按相对路径逐级查找，例如 "1.20.1-Forge/config/verity-common.toml"。 */
    public static DocumentFile resolve(DocumentFile base, String relative) {
        DocumentFile current = base;
        for (String part : relative.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            current = child(current, part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** 从授权根开始按相对路径查找。 */
    public static DocumentFile resolve(String relative) {
        return resolve(root(), relative);
    }

    public static String readText(DocumentFile file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        Context context = Prefs.context();
        if (context == null) {
            return null;
        }
        try {
            java.io.InputStream in = context.getContentResolver().openInputStream(file.getUri());
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** 覆盖写入；文件不存在时返回 false，避免误建。 */
    public static boolean writeText(DocumentFile file, String text) {
        if (file == null || !file.isFile()) {
            return false;
        }
        Context context = Prefs.context();
        if (context == null) {
            return false;
        }
        try {
            java.io.OutputStream out = context.getContentResolver()
                    .openOutputStream(file.getUri(), "wt");
            if (out == null) {
                return false;
            }
            out.write(text.getBytes("UTF-8"));
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean delete(DocumentFile file) {
        try {
            return file != null && file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /** 授权目录的可读描述，优先展示真实路径。 */
    public static String describe() {
        String path = Prefs.safTreePath();
        if (!path.isEmpty()) {
            return path;
        }
        DocumentFile root = root();
        return root == null ? "" : root.getName() == null ? "" : root.getName();
    }

    /** 从 tree URI 推断出真实路径，便于界面显示。 */
    public static String pathOf(Uri treeUri) {
        if (treeUri == null) {
            return "";
        }
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            int colon = docId.indexOf(':');
            if (colon >= 0) {
                String tail = docId.substring(colon + 1);
                return tail.isEmpty() ? "/storage/emulated/0" : "/storage/emulated/0/" + tail;
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
