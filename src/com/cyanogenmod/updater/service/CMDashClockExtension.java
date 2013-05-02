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
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.utils.SysUtils;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CMDashClockExtension extends DashClockExtension {
    private static final String TAG = "CMDashClockExtension";

    public static final String ACTION_DATA_UPDATE = "com.cyanogenmod.updater.action.DASHCLOCK_DATA_UPDATE";

    private static final int MAX_BODY_ITEMS = 3;

    private boolean mInitialized = false;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        mInitialized = true;
    }

    @Override
    protected void onUpdateData(int reason) {
        ArrayList<UpdateInfo> updates = new ArrayList<UpdateInfo>();

        try {
            FullUpdateInfo updateInfo = State.loadState(this);
            if (updateInfo != null) {
                updates.addAll(updateInfo.roms);
                updates.addAll(updateInfo.incrementalRoms);
            }
        } catch (IOException e) {
            // ignored, updates is initialized correctly
        }

        Log.d(TAG, "Update dash clock for " + updates.size() + " updates");

        Intent intent = new Intent(this, UpdatesSettings.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Collections.sort(updates, new Comparator<UpdateInfo>() {
            @Override
            public int compare(UpdateInfo lhs, UpdateInfo rhs) {
                /* sort by date descending */
                return rhs.getDate().compareTo(lhs.getDate());
            }
        });

        String systemTypeMatch = SysUtils.getSystemProperty(Customization.BOARD);
        if (!TextUtils.isEmpty(systemTypeMatch)) {
            systemTypeMatch = "-" + systemTypeMatch + "$";
        }

        StringBuilder expandedBody = new StringBuilder();

        for (int i = 0; i < updates.size() && i < MAX_BODY_ITEMS; i++) {
            if (expandedBody.length() > 0) {
                expandedBody.append("\n");
            }
            String name = updates.get(i).getName();
            if (!TextUtils.isEmpty(systemTypeMatch)) {
                name = name.replaceAll(systemTypeMatch, "");
            }

            expandedBody.append(name);
        }

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(!updates.isEmpty())
                .icon(R.drawable.cid)
                .status(getString(R.string.extension_status, updates.size()))
                .expandedTitle(getString(R.string.extension_expandedTitle, updates.size()))
                .expandedBody(expandedBody.toString())
                .clickIntent(intent));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (TextUtils.equals(intent.getAction(), ACTION_DATA_UPDATE)) {
            if (mInitialized) {
                onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
            }
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
