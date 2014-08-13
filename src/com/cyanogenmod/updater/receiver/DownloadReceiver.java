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
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdateApplication;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.service.DownloadService;
import com.cyanogenmod.updater.utils.MD5;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.IOException;

public class DownloadReceiver extends BroadcastReceiver{
    private static final String TAG = "DownloadReceiver";

    public static final String ACTION_START_DOWNLOAD = "com.cyanogenmod.cmupdater.action.START_DOWNLOAD";
    public static final String EXTRA_UPDATE_INFO = "update_info";

    public static final String ACTION_DOWNLOAD_STARTED = "com.cyanogenmod.cmupdater.action.DOWNLOAD_STARTED";

    static final String ACTION_INSTALL_UPDATE = "com.cyanogenmod.cmupdater.action.INSTALL_UPDATE";
    static final String EXTRA_FILENAME = "filename";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (ACTION_START_DOWNLOAD.equals(action)) {
            UpdateInfo ui = (UpdateInfo) intent.getParcelableExtra(EXTRA_UPDATE_INFO);
            handleStartDownload(context, ui);
        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            handleDownloadComplete(context, prefs, id);
        } else if (ACTION_INSTALL_UPDATE.equals(action)) {
            StatusBarManager sb = (StatusBarManager) context.getSystemService(Context.STATUS_BAR_SERVICE);
            sb.collapsePanels();
            String fileName = intent.getStringExtra(EXTRA_FILENAME);
            try {
                Utils.triggerUpdate(context, fileName);
            } catch (IOException e) {
                Log.e(TAG, "Unable to reboot into recovery mode", e);
                Toast.makeText(context, R.string.apply_unable_to_reboot_toast,
                            Toast.LENGTH_SHORT).show();
                Utils.cancelNotification(context);
            }
        }
    }

    private void handleStartDownload(Context context, UpdateInfo ui) {
        DownloadService.start(context, ui);
    }

    private void handleDownloadComplete(Context context, SharedPreferences prefs, long id) {
        long enqueued = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        String incrementalFor = prefs.getString(Constants.DOWNLOAD_INCREMENTAL_FOR, null);

        if (enqueued < 0 || id < 0 || id != enqueued) {
            return;
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        Intent updateIntent = new Intent(context, UpdatesSettings.class);
        updateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        int status = fetchDownloadStatus(context, id);
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            // Get the full path name of the downloaded file and the MD5

            // Strip off the .partial at the end to get the completed file
            String partialFileFullPath = fetchDownloadPartialPath(context, id);

            if (partialFileFullPath == null) {
                displayErrorResult(context, updateIntent, R.string.unable_to_download_file);
            }

            String completedFileFullPath = partialFileFullPath.replace(".partial", "");

            File partialFile = new File(partialFileFullPath);
            File updateFile = new File(completedFileFullPath);
            partialFile.renameTo(updateFile);

            String downloadedMD5 = prefs.getString(Constants.DOWNLOAD_MD5, "");

            // Start the MD5 check of the downloaded file
            if (MD5.checkMD5(downloadedMD5, updateFile)) {
                // We passed. Bring the main app to the foreground and trigger download completed
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_ID, id);
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_PATH, completedFileFullPath);
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_INCREMENTAL_FOR,
                        incrementalFor);
                displaySuccessResult(context, updateIntent, updateFile);
            } else {
                // We failed. Clear the file and reset everything
                dm.remove(id);

                if (updateFile.exists()) {
                    updateFile.delete();
                }
                displayErrorResult(context, updateIntent, R.string.md5_verification_failed);
            }
        } else if (status == DownloadManager.STATUS_FAILED) {
            // The download failed, reset
            dm.remove(id);
            displayErrorResult(context, updateIntent, R.string.unable_to_download_file);
        }

        // Clear the shared prefs
        prefs.edit()
                .remove(Constants.DOWNLOAD_MD5)
                .remove(Constants.DOWNLOAD_ID)
                .remove(Constants.DOWNLOAD_INCREMENTAL_FOR)
                .apply();
    }

    private String fetchDownloadPartialPath(DownloadManager dm, long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor c = dm.query(query);
        try {
            if (c.moveToFirst()) {
                return c.getString(
                        c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            }
        } finally {
            c.close();
        }
        return null;
    }

    private int fetchDownloadStatus(DownloadManager dm, long id) {
        Query query = new Query();
        query.setFilterById(id);
        Cursor c = dm.query(query);
        try {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            c.close();
        }
        return DownloadManager.STATUS_FAILED;
    }

    private void displayErrorResult(Context context, Intent updateIntent, int failureMessageResId) {
        final UpdateApplication app = (UpdateApplication) context.getApplicationContext();
        if (app.isMainActivityActive()) {
            Toast.makeText(context, failureMessageResId, Toast.LENGTH_LONG).show();
        } else {
            DownloadNotifier.notifyDownloadError(context, updateIntent, failureMessageResId);
        }
    }

    private void displaySuccessResult(Context context, Intent updateIntent, File updateFile) {
        final UpdateApplication app = (UpdateApplication) context.getApplicationContext();
        if (app.isMainActivityActive()) {
            context.startActivity(updateIntent);
        } else {
            DownloadNotifier.notifyDownloadComplete(context, updateIntent, updateFile);
        }
    }
}
