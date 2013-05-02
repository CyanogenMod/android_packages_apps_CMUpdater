/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.io.IOException;

public class CMDashClockExtension extends DashClockExtension {
    public static final String ACTION_DATA_UPDATE = "com.cyanogenmod.updater.action.DASHCLOCK_DATA_UPDATE";
    private static final String TAG = "CMDashClockExtension";

    @Override
    protected void onUpdateData(int reason) {
        int updateCount = 0;

        try {
            FullUpdateInfo updateInfo = State.loadState(this);
            if (updateInfo != null) {
                updateCount = updateInfo.getUpdateCount();
            }
        } catch (IOException e) {
            // ignored, updateCount is initialized correctly
        }

        Log.d(TAG, "Update dash clock for " + updateCount + " updates");

        Intent intent = new Intent(this, UpdatesSettings.class);
        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(updateCount > 0)
                .icon(R.drawable.cid)
                .status(getString(R.string.extension_status, updateCount))
                .expandedTitle(getString(R.string.extension_expandedTitle, updateCount))
                .expandedBody(getString(R.string.extension_expandedBody))
                .clickIntent(intent));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (TextUtils.equals(intent.getAction(), ACTION_DATA_UPDATE)) {
            onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
