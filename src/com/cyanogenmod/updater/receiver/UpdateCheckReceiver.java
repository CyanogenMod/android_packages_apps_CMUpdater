package com.cyanogenmod.updater.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.service.UpdateCheckService;

import java.util.Date;

public class UpdateCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Load the required settings from preferences
        SharedPreferences prefs = context.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        int updateFrequency = prefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);
        boolean bootCheckCompleted = prefs.getBoolean(Constants.BOOT_CHECK_COMPLETED, false);

        if (updateFrequency == Constants.UPDATE_FREQ_NONE) {
            // We are set to manual updates, don't do anything
            return;
        }

        // Parse the action
        final String action = intent.getAction();

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Connectivity has changed
            boolean hasConnection = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
            if (hasConnection) {
                if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT && !bootCheckCompleted) {
                    Log.i(TAG, "The on-boot check was not completed, retrying...");
                    Intent i = new Intent(context, UpdateCheckService.class);
                    i.putExtra(Constants.CHECK_FOR_UPDATE, true);
                    context.startService(i);
                } else if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT && bootCheckCompleted){
                    // Nothing to do
                    Log.i(TAG, "On-boot update check was already completed.");
                    return;
                } else if (updateFrequency > 0) {
                    Log.i(TAG, "Scheduling future, repeating update checks.");
                    scheduleUpdateService(context, updateFrequency * 1000);
                }
            }

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // We just booted
            if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT) {
                Log.i(TAG, "Triggering boot time update check");

                // Store the boot check state
                prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, false).apply();

                // Trigger the service
                Intent i = new Intent(context, UpdateCheckService.class);
                i.putExtra(Constants.CHECK_FOR_UPDATE, true);
                context.startService(i);
            } else  if (updateFrequency > 0) {
                Log.i(TAG, "Scheduling future, repeating update checks");
                scheduleUpdateService(context, updateFrequency + 1000);
            }
        }
    }

    private void scheduleUpdateService(Context context, int updateFrequency) {
        // Load the required settings from preferences
        SharedPreferences prefs = context.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        Date lastCheck = new Date(prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));

        // Get the intent ready
        Intent i = new Intent(context, UpdateCheckService.class);
        i.putExtra(Constants.CHECK_FOR_UPDATE, true);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Clear any old alarms and schedule the new alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck.getTime() + updateFrequency, updateFrequency, pi);
    }
}