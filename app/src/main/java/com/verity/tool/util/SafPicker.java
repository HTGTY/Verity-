package com.verity.tool.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

/** 打开系统目录选择器，默认定位到内部存储的 Android/data。 */
public final class SafPicker {

    /** 外部存储的 DocumentsProvider。 */
    private static final String AUTHORITY = "com.android.externalstorage.documents";
    /** 起始目录：/sdcard/Android/data。 */
    private static final String START_DOC_ID = "primary:Android/data";

    private SafPicker() {
    }

    public static void open(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            Uri initial = DocumentsContract.buildDocumentUri(AUTHORITY, START_DOC_ID);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
        } catch (Exception ignored) {
            // 低版本系统不支持初始目录，让选择器从默认位置打开
        }
        activity.startActivityForResult(intent, requestCode);
    }
}
