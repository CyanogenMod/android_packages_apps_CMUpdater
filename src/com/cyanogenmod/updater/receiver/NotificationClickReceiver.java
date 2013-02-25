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

import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;

public class NotificationClickReceiver extends BroadcastReceiver{
    private static String TAG = "NotificationClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	// Get the ID of the currently running CMUpdater download and the one just finished
        SharedPreferences prefs = context.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        long enqueue = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);

        // If we had an active download and the IDs match
        if (enqueue != -1 && id != -2 && id == enqueue) {
            Query query = new Query();
            query.setFilterById(id);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = dm.query(query);
            if (c.moveToFirst()) {

		        // Bring the main app to the foreground
		        Intent i = new Intent(context, UpdatesSettings.class);
		        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | 
		                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		        context.startActivity(i);
        
            }
        }
    }
}
