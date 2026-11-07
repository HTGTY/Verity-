package com.verity.tool.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.verity.tool.R;

/** 下载进度通知。 */
public final class Notify {

    public static final String CHANNEL = "verity_download";

    private Notify() {
    }

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL,
                    ctx.getString(R.string.dl_title),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            NotificationManagerCompat.from(ctx).createNotificationChannel(ch);
        }
    }

    public static NotificationCompat.Builder builder(Context ctx, CharSequence title, CharSequence text) {
        return new NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_settings)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true);
    }

    /** 点通知回到下载页。 */
    public static PendingIntent openApp(Context ctx) {
        Intent i = new Intent(ctx, com.verity.tool.DownloadActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(ctx, 0, i, flags);
    }

    public static void updateProgress(Context ctx, int id, CharSequence title,
                                      int done, int total, CharSequence text) {
        NotificationCompat.Builder b = builder(ctx, title, text)
                .setProgress(total, done, total <= 0)
                .setContentIntent(openApp(ctx));
        NotificationManagerCompat.from(ctx).notify(id, b.build());
    }

    public static void updateForeground(Context ctx, int id, CharSequence title,
                                        CharSequence text, int done, int total) {
        NotificationCompat.Builder b = builder(ctx, title, text)
                .setProgress(total, done, total <= 0)
                .setOngoing(true)
                .setContentIntent(openApp(ctx));
        NotificationManagerCompat.from(ctx).notify(id, b.build());
    }

    public static void cancel(Context ctx, int id) {
        NotificationManagerCompat.from(ctx).cancel(id);
    }
}