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
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesActivity;
import com.cyanogenmod.updater.UpdateApplication;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.receiver.DownloadNotifier;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DownloadCompleteIntentService extends IntentService {

    private static final String TAG = "DownloadComplete";

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
        if (!intent.hasExtra(Constants.DOWNLOAD_ID) || !intent.hasExtra(Constants.DOWNLOAD_NAME)) {
            Log.e(TAG, "Missing intent extra data");
            return;
        }

        long id = intent.getLongExtra(Constants.DOWNLOAD_ID, -1);
        final String destName = intent.getStringExtra(Constants.DOWNLOAD_NAME);

        Intent updateIntent = new Intent(this, UpdatesActivity.class);
        updateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        int status = fetchDownloadStatus(id);
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            String destPath = Utils.makeUpdateFolder(getApplicationContext()).getPath() + "/"
                    + destName;
            File destFileTmp = new File(destPath + Constants.DOWNLOAD_TMP_EXT);

            try (
                FileOutputStream outStream = new FileOutputStream(destFileTmp);

                ParcelFileDescriptor file = mDm.openDownloadedFile(id);
                FileInputStream inStream = new FileInputStream(file.getFileDescriptor());

                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();
            ) {
                inChannel.transferTo(0, file.getStatSize(), outChannel);
            } catch (IOException e) {
                Log.e(TAG, "Copy of download failed", e);
                displayErrorResult(updateIntent, R.string.unable_to_download_file);
                if (destFileTmp.exists()) {
                    destFileTmp.delete();
                }
                return;
            } finally {
                mDm.remove(id);
            }

            if (!destFileTmp.exists()) {
                // The download was probably stopped. Exit silently
                Log.d(TAG, "File not found, can't verify it");
                return;
            }

            // Check the signature of the downloaded file
            try {
                android.os.RecoverySystem.verifyPackage(destFileTmp, null, null);
            } catch (Exception e) {
                Log.e(TAG, "Verification failed", e);
                if (destFileTmp.exists()) {
                    destFileTmp.delete();
                    displayErrorResult(updateIntent, R.string.verification_failed);
                } else {
                    // The download was probably stopped. Exit silently
                    Log.e(TAG, "Error while verifying the file", e);
                }
                return;
            }

            File destFile = new File(destPath);
            if (destFile.exists()) {
                destFile.delete();
            }
            if (!destFileTmp.exists()) {
                // The download was probably stopped. Exit silently
                Log.d(TAG, "File not found, can't rename it");
                return;
            }
            destFileTmp.renameTo(destFile);

            // We passed. Bring the main app to the foreground and trigger download completed
            updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_ID, id);
            updateIntent.putExtra(UpdatesSettings.EXTRA_FINISHED_DOWNLOAD_PATH,
                    destPath);
            displaySuccessResult(updateIntent, destFile);
        } else if (status == DownloadManager.STATUS_FAILED) {
            Log.e(TAG, "Download failed");
            // The download failed, reset
            mDm.remove(id);
            displayErrorResult(updateIntent, R.string.unable_to_download_file);
        }
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
