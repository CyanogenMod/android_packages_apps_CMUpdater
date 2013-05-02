/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
 
package com.cyanogenmod.updater.service;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class CMDashClockExtension extends DashClockExtension {
    SharedPreferences sp;
    @Override
    public void onCreate() {
        super.onCreate();
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    protected void onUpdateData(int reason) {
        // Get preference value.
        Log.d("CMUpdater","updating");
        int numUpdates = sp.getInt("numUpdates", 0);
        Log.d("numUpdates from CMExtension", "" + numUpdates);
        Intent intent = new Intent(this, UpdatesSettings.class);
        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(numUpdates != 0)
                .icon(R.drawable.cid)
                .status(numUpdates + getString(R.string.extension_status))
                .expandedTitle(numUpdates + getString(R.string.extension_expandedTitle))
                .expandedBody(getString(R.string.extension_expandedBody))
                .clickIntent(intent));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sp.unregisterOnSharedPreferenceChangeListener(mListener);
    }

    public OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key=="numUpdates"){
                Log.d("debug_CMExtension", "The preference has been changed");
                        onUpdateData(DashClockExtension.UPDATE_REASON_CONTENT_CHANGED);
                }
        }
    };
}
