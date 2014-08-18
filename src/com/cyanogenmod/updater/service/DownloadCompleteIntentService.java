/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdateApplication;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.receiver.DownloadNotifier;
import com.cyanogenmod.updater.utils.MD5;

import java.io.File;

public class DownloadCompleteIntentService extends IntentService {
    private DownloadManager mDm;

    @Override
    public void onCreate() {
        super.onCreate();
        mDm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public DownloadCompleteIntentService() {
        super(DownloadCompleteIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!intent.hasExtra(Constants.DOWNLOAD_ID) ||
                !intent.hasExtra(Constants.DOWNLOAD_MD5)) {
            return;
        }

        long id = intent.getLongExtra(Constants.DOWNLOAD_ID, -1);
        String downloadedMD5 = intent.getStringExtra(Constants.DOWNLOAD_MD5);
        String incrementalFor = intent.getStringExtra(Constants.DOWNLOAD_INCREMENTAL_FOR);

        Intent updateIntent = new Intent(this, UpdatesSettings.class);
        updateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        int status = fetchDownloadStatus(id);
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            // Get the full path name of the downloaded file and the MD5

            // Strip off the .partial at the end to get the completed file
            String partialFileFullPath = fetchDownloadPartialPath(id);

            if (partialFileFullPath == null) {
                displayErrorResult(updateIntent, R.string.unable_to_download_file);
            }

            String completedFileFullPath = partialFileFullPath.replace(".partial", "");

            File partialFile = new File(partialFileFullPath);
            File updateFile = new File(completedFileFullPath);
            partialFile.renameTo(updateFile);

            // Start the MD5 check of the downloaded file
            if (MD5.checkMD5(downloadedMD5, updateFile)) {
                // We passed. Bring the main app to the foreground and trigger download completed
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_ID, id);
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_PATH,
                        completedFileFullPath);
                updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_INCREMENTAL_FOR,
                        incrementalFor);
                displaySuccessResult(updateIntent, updateFile);
            } else {
                // We failed. Clear the file and reset everything
                mDm.remove(id);

                if (updateFile.exists()) {
                    updateFile.delete();
                }
                displayErrorResult(updateIntent, R.string.md5_verification_failed);
            }
        } else if (status == DownloadManager.STATUS_FAILED) {
            // The download failed, reset
            mDm.remove(id);
            displayErrorResult(updateIntent, R.string.unable_to_download_file);
        }
    }

    private String fetchDownloadPartialPath(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor c = mDm.query(query);
        try {
            if (c.moveToFirst()) {
                return c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
            }
        } finally {
            c.close();
        }
        return null;
    }

    private int fetchDownloadStatus(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor c = mDm.query(query);
        try {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            c.close();
        }
        return DownloadManager.STATUS_FAILED;
    }

    private void displayErrorResult(Intent updateIntent, int failureMessageResId) {
        DownloadNotifier.notifyDownloadError(this, updateIntent, failureMessageResId);
    }

    private void displaySuccessResult(Intent updateIntent, File updateFile) {
        final UpdateApplication app = (UpdateApplication) getApplicationContext();
        if (app.isMainActivityActive()) {
            startActivity(updateIntent);
        } else {
            DownloadNotifier.notifyDownloadComplete(this, updateIntent, updateFile);
        }
    }
}
