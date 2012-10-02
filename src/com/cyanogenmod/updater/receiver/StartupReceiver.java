package com.cyanogenmod.updater.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.service.UpdateCheckService;

import java.util.Date;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context ctx, Intent intent) {

        // Load the following from preferences
        SharedPreferences prefs = ctx.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        int updateFreq = prefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);

        if (updateFreq == Constants.UPDATE_FREQ_NONE) {
            // We are set to manual updates, don't do anything
            return;
        }

        // We should check for updates
        if (updateFreq == Constants.UPDATE_FREQ_AT_BOOT) {
            // Asking UpdateService to check for updates on boot
            Intent i = new Intent(ctx, UpdateCheckService.class);
            i.putExtra(Constants.CHECK_FOR_UPDATE, true);
            ctx.startService(i);

        } else {
            // Scheduling future UpdateService checks for updates
            scheduleUpdateService(ctx, updateFreq * 1000);
        }
    }

    private void scheduleUpdateService(Context ctx, int updateFrequency) {

        // Get the intent ready
        Intent i = new Intent(ctx, UpdateCheckService.class);
        i.putExtra(Constants.CHECK_FOR_UPDATE, true);
        PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);

        // Clear any old alarms before we start
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        // Check if we need to schedule a new alarm
        if (updateFrequency > 0) {
            // Get the last time we checked for an update
            SharedPreferences prefs = ctx.getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
            Date lastCheck = new Date(prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));

            // Set the new alarm
            am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck.getTime() + updateFrequency, updateFrequency, pi);
        }
    }
}