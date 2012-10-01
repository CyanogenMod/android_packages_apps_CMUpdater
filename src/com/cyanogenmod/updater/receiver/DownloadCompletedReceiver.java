package com.cyanogenmod.updater.receiver;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;

public class DownloadCompletedReceiver extends BroadcastReceiver{
    private static String TAG = "DownloadCompletedReceiver";
    private static boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the ID of the currently running CMUpdater download and the one just finished
        SharedPreferences prefs = context.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        long enqueue = prefs.getLong(Constants.DOWNLOAD_ID, -1);
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);

        if (DEBUG)
            Log.d(TAG, "enqueue = " + enqueue + ", downloaded id = " + id);

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
                    // Clear the active download ID
                    prefs.edit().putLong(Constants.DOWNLOAD_ID, -1).apply();

                    // Get the full path name of the downloaded file
                    int filenameIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                    String fullPathName = c.getString(filenameIndex);

                    // Bring the main app to the foreground and trigger download completed
                    Intent i = new Intent(context, UpdatesSettings.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(Constants.DOWNLOAD_COMPLETED, true);
                    i.putExtra(Constants.DOWNLOAD_ID, id);
                    i.putExtra(Constants.DOWNLOAD_FULLPATH, fullPathName);
                    context.startActivity(i);

                } else if (status == DownloadManager.STATUS_FAILED) {
                    // The download failed
                    dm.remove(id);
                    prefs.edit().putLong(Constants.DOWNLOAD_ID, -1).apply();
                    Toast.makeText(context, R.string.unable_to_download_file, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
