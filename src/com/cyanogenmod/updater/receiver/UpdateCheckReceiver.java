/*
* Copyright (C) 2012 The CyanogenMod Project
* Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.Utils;

public class UpdateCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "UpdateCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Load the required settings from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int updateFrequency = prefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);

        // Check if we are set to manual updates and don't do anything
        if (updateFrequency == Constants.UPDATE_FREQ_NONE) {
            return;
        }

        // Not set to manual updates, parse the received action
        final String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Connectivity has changed
            boolean hasConnection = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
            if (!hasConnection) {
                return;
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // We just booted. Store the boot check state
            prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, false).apply();
        }

        // Handle the actual update check based on the defined frequency
        if (updateFrequency > 0) {
            Log.i(TAG, "Scheduling future, repeating update checks.");
            Utils.scheduleUpdateService(context, updateFrequency * 1000);
        }
    }
}
