package com.verity.tool;

import android.app.Application;

import com.verity.tool.util.Prefs;

/** 应用入口：初始化设置；主题与动态取色由 BaseActivity 按设置应用。 */
public class VerityApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.init(this);
    }
}
