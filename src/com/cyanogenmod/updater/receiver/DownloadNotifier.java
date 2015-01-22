/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *
 * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.UpdateInfo;
import java.io.File;

public class DownloadNotifier {
    private DownloadNotifier() {
        // Don't instantiate me bro
    }

    public static void notifyDownloadComplete(Context context,
            Intent updateIntent, File updateFile) {
        String updateUiName = UpdateInfo.extractUiName(updateFile.getName());

        Notification.BigTextStyle style = new Notification.BigTextStyle()
                .setBigContentTitle(context.getString(R.string.not_download_success))
                .bigText(context.getString(R.string.not_download_install_notice, updateUiName));

        Notification.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setSmallIcon(R.drawable.cm_updater)
                .setContentTitle(context.getString(R.string.not_download_success))
                .setContentText(updateUiName)
                .setTicker(context.getString(R.string.not_download_success))
                .setStyle(style)
                .addAction(R.drawable.ic_tab_install,
                context.getString(R.string.not_action_install_update),
                createInstallPendingIntent(context, updateFile));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    public static void notifyDownloadError(Context context,
            Intent updateIntent, int failureMessageResId) {
        Notification.Builder builder = createBaseContentBuilder(context, updateIntent)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.not_download_failure))
                .setContentText(context.getString(failureMessageResId))
                .setTicker(context.getString(R.string.not_download_failure));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    private static Notification.Builder createBaseContentBuilder(Context context,
            Intent updateIntent) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 1, updateIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
    }

    private static PendingIntent createInstallPendingIntent(Context context, File updateFile) {
        Intent installIntent = new Intent(context, DownloadReceiver.class);
        installIntent.setAction(DownloadReceiver.ACTION_INSTALL_UPDATE);
        installIntent.putExtra(DownloadReceiver.EXTRA_FILENAME, updateFile.getName());

        return PendingIntent.getBroadcast(context, 0, installIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
