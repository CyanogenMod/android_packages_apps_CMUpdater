/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.receiver;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.MD5;

import java.io.File;

public class DownloadCompletedReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the ID of the currently running CMUpdater download and the one just finished
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long enqueued = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        if (enqueued < 0 || id < 0 || id != enqueued) {
            return;
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Query query = new Query();
        query.setFilterById(id);

        Cursor c = dm.query(query);
        if (c == null) {
            return;
        }

        if (!c.moveToFirst()) {
            c.close();
            return;
        }

        final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
        int failureToastResId = -1;

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            // Get the full path name of the downloaded file and the MD5

            // Strip off the .partial at the end to get the completed file
            String partialFileFullPath = c.getString(
                    c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            String completedFileFullPath = partialFileFullPath.replace(".partial", "");

            File partialFile = new File(partialFileFullPath);
            File completedFile = new File(completedFileFullPath);
            partialFile.renameTo(completedFile);

            String downloadedMD5 = prefs.getString(Constants.DOWNLOAD_MD5, "");

            // Start the MD5 check of the downloaded file
            if (MD5.checkMD5(downloadedMD5, completedFile)) {
                // We passed. Bring the main app to the foreground and trigger download completed
                Intent i = new Intent(context, UpdatesSettings.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                i.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_ID, id);
                i.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_PATH, completedFileFullPath);
                context.startActivity(i);
            } else {
                // We failed. Clear the file and reset everything
                dm.remove(id);

                if (completedFile.exists()) {
                    completedFile.delete();
                }

                // Remove the log file if it exists
                File logFileToDelete = new File(completedFileFullPath + ".changelog");
                if (logFileToDelete.exists()) {
                    logFileToDelete.delete();
                }

                failureToastResId = R.string.md5_verification_failed;
            }
        } else if (status == DownloadManager.STATUS_FAILED) {
            // The download failed, reset
            dm.remove(id);

            failureToastResId = R.string.unable_to_download_file;
        }

        // Clear the shared prefs
        prefs.edit()
                .putString(Constants.DOWNLOAD_MD5, "")
                .putLong(Constants.DOWNLOAD_ID, -1)
                .apply();

        if (failureToastResId >= 0) {
            Toast.makeText(context, failureToastResId, Toast.LENGTH_LONG).show();
        }
    }
}
