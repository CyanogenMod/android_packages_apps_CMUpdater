/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.cm.ScreenType;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.receiver.DownloadReceiver;
import com.cyanogenmod.updater.service.UpdateCheckService;
import com.cyanogenmod.updater.utils.UpdateFilter;
import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class UpdatesSettingsTv extends UpdatesSettings {
    private static String TAG = "UpdatesSettingsTv";

    private static final String KEY_SYSTEM_INFO = "system_info";
    private static final String KEY_REFESH = "refesh";
    private static final String KEY_DELETE_ALL = "delete_all";
    private Date lastCheck = new Date(PreferenceManager.getDefaultSharedPreferences(this).getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
    private final String date = DateFormat.getLongDateFormat(this).format(lastCheck);
    private final String time = DateFormat.getTimeFormat(this).format(lastCheck);
    private final String sysinfomessage = getString(R.string.sysinfo_device) + " "
            + Utils.getDeviceType() + getString(R.string.sysinfo_running) + " "
            + Utils.getInstalledVersion() + "\n\n" + getString(R.string.sysinfo_update_channel)
            + " " + Utils.getCmReleaseType() + getString(R.string.sysinfo_last_check) + " "
            + date + " " + time;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UpdatesSettings.mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        // Load the layouts
        addPreferencesFromResource(R.xml.main_tv);
        UpdatesSettings.mUpdatesList = (PreferenceCategory) findPreference(UpdatesSettings.UPDATES_CATEGORY);
        mUpdateCheck = (ListPreference) findPreference(Constants.UPDATE_CHECK_PREF);
        setStringSummary(KEY_SYSTEM_INFO, sysinfomessage);

        // Load the stored preference data
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mUpdateCheck != null) {
            int check = mPrefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);
            mUpdateCheck.setValue(String.valueOf(check));
            mUpdateCheck.setSummary(mapCheckValue(check));
            mUpdateCheck.setOnPreferenceChangeListener(this);
        }

        // Force a refresh if UPDATE_TYPE_PREF does not match release type
        int updateType = Utils.getUpdateType();
        int updateTypePref = mPrefs.getInt(Constants.UPDATE_TYPE_PREF,
                Constants.UPDATE_TYPE_SNAPSHOT);
        if (updateTypePref != updateType) {
            updateUpdatesType(updateType);
        }
    }

    protected void updateUpdatesType(int type) {
        mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, type).apply();
        checkForUpdates();
    }

    protected String mapCheckValue(Integer value) {
        Resources resources = getResources();
        String[] checkNames = resources.getStringArray(R.array.update_check_entries);
        String[] checkValues = resources.getStringArray(R.array.update_check_values);
        for (int i = 0; i < checkValues.length; i++) {
            if (Integer.decode(checkValues[i]).equals(value)) {
                return checkNames[i];
            }
        }
        return getString(R.string.unknown);
    }

    private void checkForUpdates() {
        if (mProgressDialog != null) {
            return;
        }

        // If there is no internet connection, display a message and return.
        if (!Utils.isOnline(this)) {
            Toast.makeText(this, R.string.data_connection_required, Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(R.string.checking_for_updates);
        mProgressDialog.setMessage(getString(R.string.checking_for_updates));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Intent cancelIntent = new Intent(UpdatesSettingsTv.this, UpdateCheckService.class);
                cancelIntent.setAction(UpdateCheckService.ACTION_CANCEL_CHECK);
                startService(cancelIntent);
                mProgressDialog = null;
            }
        });

        Intent checkIntent = new Intent(UpdatesSettingsTv.this, UpdateCheckService.class);
        checkIntent.setAction(UpdateCheckService.ACTION_CHECK);
        startService(checkIntent);

        mProgressDialog.show();
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
