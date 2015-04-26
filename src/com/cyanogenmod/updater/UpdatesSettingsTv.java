/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class UpdatesSettingsTv extends UpdatesSettings {
    private static String TAG = "UpdatesSettingsTv";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStringSummary(KEY_SYSTEM_INFO, createSysinfoMessage());
    }

    private String createSysinfoMessage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long lastCheck = prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);
        String date = DateFormat.getLongDateFormat(this).format(lastCheck);
        String time = DateFormat.getTimeFormat(this).format(lastCheck);
        String cmReleaseType = Constants.CM_RELEASETYPE_NIGHTLY;
        int updateType = Utils.getUpdateType();
        if (updateType == Constants.UPDATE_TYPE_SNAPSHOT) {
            cmReleaseType = Constants.CM_RELEASETYPE_SNAPSHOT;
        }
        String sysinfomessage = getString(R.string.sysinfo_device) + " " + Utils.getDeviceType() +
                "                            " + getString(R.string.sysinfo_running) + " "
                + Utils.getInstalledVersion() + "\n" + getString(R.string.sysinfo_update_channel)
                + " " + cmReleaseType + "    " + getString(R.string.sysinfo_last_check) + " "
                + date + " " + time;
        return sysinfomessage;
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.unknown));
        }
    }
}
