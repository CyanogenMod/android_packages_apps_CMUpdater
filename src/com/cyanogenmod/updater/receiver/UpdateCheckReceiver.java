/*
 * Copyright (C) 2012-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.cyanogenmod.updater.service.UpdateCheckService;
import com.cyanogenmod.updater.utils.Utils;

public class UpdateCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "UpdateCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Load the required settings from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int updateFrequency = prefs.getInt(Constants.UPDATE_CHECK_PREF,
                Constants.UPDATE_FREQ_WEEKLY);

        // Check if we are set to manual updates and don't do anything
        if (updateFrequency == Constants.UPDATE_FREQ_NONE) {
            return;
        }

        // Not set to manual updates, parse the received action
        final String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Connectivity has changed
            boolean hasConnection = !intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.i(TAG, "Got connectivity change, has connection: " + hasConnection);
            if (!hasConnection) {
                return;
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // We just booted. Store the boot check state
            prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, false).apply();
        }

        // Handle the actual update check based on the defined frequency
        if (updateFrequency == Constants.UPDATE_FREQ_AT_BOOT) {
            boolean bootCheckCompleted = prefs.getBoolean(Constants.BOOT_CHECK_COMPLETED, false);
            if (!bootCheckCompleted) {
                Log.i(TAG, "Start an on-boot check");
                Intent i = new Intent(context, UpdateCheckService.class);
                i.setAction(UpdateCheckService.ACTION_CHECK);
                context.startService(i);
            } else {
                // Nothing to do
                Log.i(TAG, "On-boot update check was already completed.");
                return;
            }
        } else if (updateFrequency > 0) {
            Log.i(TAG, "Scheduling future, repeating update checks.");
            Utils.scheduleUpdateService(context, updateFrequency * 1000);
        }
    }
}
