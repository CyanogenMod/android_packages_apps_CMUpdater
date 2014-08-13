package com.cyanogenmod.updater.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.receiver.DownloadReceiver;

import java.io.File;

public class DownloadNotifier {

    private DownloadNotifier() {
        // Don't instantiate me bro
    }

    public static void notifyDownloadComplete(Context context,
                                              Intent updateIntent, File updateFile) {
        Notification.Builder builder = createBaseContentBuilder(context, updateIntent);
        String updateUiName = UpdateInfo.extractUiName(updateFile.getName());
        builder.setSmallIcon(R.drawable.cm_updater);
        builder.setContentTitle(context.getString(R.string.not_download_success));
        builder.setContentText(updateUiName);
        builder.setTicker(context.getString(R.string.not_download_success));

        Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.setBigContentTitle(context.getString(R.string.not_download_success));
        style.bigText(context.getString(R.string.not_download_install_notice, updateUiName));
        builder.setStyle(style);
        builder.addAction(R.drawable.ic_tab_install,
                context.getString(R.string.not_action_install_update),
                createInstallPendingIntent(context, updateFile));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    public static void notifyDownloadError(Context context,
                                           Intent updateIntent, int failureMessageResId) {
        Notification.Builder builder = createBaseContentBuilder(context, updateIntent);
        builder.setSmallIcon(android.R.drawable.stat_notify_error);
        builder.setContentTitle(context.getString(R.string.not_download_failure));
        builder.setContentText(context.getString(failureMessageResId));
        builder.setTicker(context.getString(R.string.not_download_failure));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(R.string.not_download_success, builder.build());
    }

    private static Notification.Builder createBaseContentBuilder(Context context,
                                                                 Intent updateIntent) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 1,
                updateIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
    }


    private static PendingIntent createInstallPendingIntent(Context context, File updateFile) {
        Intent installIntent = new Intent(context, DownloadReceiver.class);
        installIntent.setAction(DownloadReceiver.ACTION_INSTALL_UPDATE);
        installIntent.putExtra(DownloadReceiver.EXTRA_FILENAME, updateFile.getName());

        return PendingIntent.getBroadcast(context, 0,
                installIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
