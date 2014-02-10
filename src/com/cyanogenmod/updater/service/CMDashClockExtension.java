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
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

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
        LinkedList<UpdateInfo> updates = State.loadState(this);

        Log.d(TAG, "Update dash clock for " + updates.size() + " updates");

        Intent intent = new Intent(this, UpdatesSettings.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Collections.sort(updates, new Comparator<UpdateInfo>() {
            @Override
            public int compare(UpdateInfo lhs, UpdateInfo rhs) {
                /* sort by date descending */
                long lhsDate = lhs.getDate();
                long rhsDate = rhs.getDate();
                if (lhsDate < rhsDate) {
                    return 1;
                }
                if (lhsDate > rhsDate) {
                    return -1;
                }
                return 0;
            }
        });

        final int count = updates.size();
        final Resources res = getResources();
        StringBuilder expandedBody = new StringBuilder();

        for (int i = 0; i < count && i < MAX_BODY_ITEMS; i++) {
            if (expandedBody.length() > 0) {
                expandedBody.append("\n");
            }
            expandedBody.append(updates.get(i).getName());
        }

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(!updates.isEmpty())
                .icon(R.drawable.ic_extension_cid)
                .status(res.getQuantityString(R.plurals.extension_status, count, count))
                .expandedTitle(res.getQuantityString(R.plurals.extension_expandedTitle, count, count))
                .expandedBody(expandedBody.toString())
                .clickIntent(intent));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null &&
                TextUtils.equals(intent.getAction(), ACTION_DATA_UPDATE)) {
            if (mInitialized) {
                onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
            }
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
