/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.receiver;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.Toast;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.res.Resources;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.MD5;

import java.io.File;

public class DownloadCompletedReceiver extends BroadcastReceiver{
    private static String TAG = "DownloadCompletedReceiver";
    private String mCompletedFileFullPath;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the ID of the currently running CMUpdater download and the one just finished
        SharedPreferences prefs = context.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        long enqueue = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);

        // If we had an active download and the id's match
        if (enqueue != -1 && id != -2 && id == enqueue) {
            Query query = new Query();
            query.setFilterById(id);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = dm.query(query);
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = c.getInt(columnIndex);
                if (status == DownloadManager.STATUS_SUCCESSFUL) {

                    // Get the full path name of the downloaded file and the MD5
                    int filenameIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);

                    // Strip off the .partial at the end to get the completed file
                    String partialFileFullPath = c.getString(filenameIndex);
                    mCompletedFileFullPath = partialFileFullPath.replace(".partial", "");
                    File partialFile = new File(partialFileFullPath);
                    File completedFile = new File(mCompletedFileFullPath);
                    partialFile.renameTo(completedFile);

                    String downloadedMD5 = prefs.getString(Constants.DOWNLOAD_MD5, "");

                    // Clear the shared prefs
                    prefs.edit().putString(Constants.DOWNLOAD_MD5, "").apply();
                    prefs.edit().putLong(Constants.DOWNLOAD_ID, -1).apply();

                    // Start the MD5 check of the downloaded file
                    if (MD5.checkMD5(downloadedMD5, completedFile)) {
                        // Fire Download Complete Notification
                        Intent i = new Intent(context, UpdatesSettings.class);

                        Resources res = context.getResources();
                        String text = res.getString(R.string.download_complete_notification);;

                        // Get the notification ready
                        Notification.Builder builder = new Notification.Builder(context);
                        builder.setSmallIcon(R.drawable.cm_updater);
                        builder.setWhen(System.currentTimeMillis());
                        builder.setTicker(res.getString(R.string.download_complete_notification_ticker));

                        // Set the rest of the notification content
                        builder.setContentTitle(res.getString(R.string.download_complete_notification_ticker));
                        builder.setContentText(text);
                        builder.setAutoCancel(true);
                        Notification noti = builder.build();

                        // Trigger the notification
                        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        nm.notify(R.string.not_new_updates_found_title, noti);

                    } else {
                        // We failed. Clear the file and reset everything
                        dm.remove(id);

                        // Remove the log file if it exists
                        File logFileToDelete = new File(mCompletedFileFullPath + ".changelog");
                        if (logFileToDelete.exists()) {
                            logFileToDelete.delete();
                        }

                        Toast.makeText(context, R.string.md5_verification_failed, Toast.LENGTH_LONG).show();
                    }

                } else if (status == DownloadManager.STATUS_FAILED) {
                    // The download failed, reset
                    dm.remove(id);

                    // Remove the log file if it exists
                    File logFileToDelete = new File(mCompletedFileFullPath + ".changelog");
                    if (logFileToDelete.exists()) {
                        logFileToDelete.delete();
                    }

                    // Cleannup the shared preferences
                    prefs.edit().putLong(Constants.DOWNLOAD_ID, -1).apply();
                    prefs.edit().putString(Constants.DOWNLOAD_MD5, "").apply();
                    Toast.makeText(context, R.string.unable_to_download_file, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
