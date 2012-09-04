package com.cyanogenmod.updater.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.ui.R;

import java.io.*;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

public class Preferences {
    private static final String TAG = "Preferences";

    private Boolean showDebugOutput = false;

    private final SharedPreferences mPrefs;
    private final Resources mRes;
    private String temp;
    private boolean tempbool;
    private final Context context;

    public Preferences(Context ctx) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        mRes = ctx.getResources();
        context = ctx;
        showDebugOutput = displayDebugOutput();
        if (showDebugOutput) Log.d(TAG, "Preference Instance set.");
    }

    public int getUpdateFrequency() {
        try {
            return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.PREF_UPDATE_CHECK_FREQUENCY), ""));
        }
        catch (NumberFormatException ex) {
            return Constants.UPDATE_FREQ_AT_BOOT;
        }
    }

    public Date getLastUpdateCheck() {
        return new Date(mPrefs.getLong(mRes.getString(R.string.PREF_LAST_UPDATE_CHECK), 0));
    }

    public String getLastUpdateCheckString() {
        Date d = getLastUpdateCheck();
        if (d.getTime() == 0)
            return mRes.getString(R.string.no_updatecheck_executed);
        else
            return DateFormat.getDateTimeInstance().format(d);
    }

    public void setLastUpdateCheck(Date d) {
        Editor editor = mPrefs.edit();
        editor.putLong(mRes.getString(R.string.PREF_LAST_UPDATE_CHECK), d.getTime());
        if (!editor.commit()) Log.e(TAG, "Unable to write last update check");
    }

    public String getBoardString() {
        temp = SysUtils.getSystemProperty(Customization.BOARD);
        if (showDebugOutput) Log.d(TAG, "Board: " + temp);
        return temp;
    }

    public String getChangelogURL() {
        temp = mRes.getString(R.string.conf_changelog_url);
        if (showDebugOutput) Log.d(TAG, "ChangelogURL: " + temp);
        return temp;
    }

    //Roms
    public boolean showAllRomUpdates() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS_DEF_VALUE))); 
        if (showDebugOutput) Log.d(TAG, "Display All Rom Updates: " + tempbool);
        return tempbool;
    }

    public boolean showNightlyRomUpdates() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_NIGHTLY_ROM_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_NIGHTLY_ROM_VERSIONS_DEF_VALUE)));
        if (showDebugOutput) Log.d(TAG, "Display Nightly Rom Updates: " + tempbool);
        return tempbool;
    }

    public String getRomUpdateFileURL() {
        temp = mPrefs.getString(mRes.getString(R.string.PREF_ROM_UPDATE_FILE_URL),  mRes.getString(R.string.conf_update_server_url_def));
        if (showDebugOutput) Log.d(TAG, "Rom MetadataFile-Url: " + temp);
        return temp;
    }

    public void setRomUpdateFileURL(String updateFileURL) {
        Editor editor = mPrefs.edit();
        editor.putString(mRes.getString(R.string.PREF_ROM_UPDATE_FILE_URL), updateFileURL);
        if (!editor.commit()) Log.e(TAG, "Unable to write Rom Update File URL");
    }

    //Notifications
    public boolean notificationsEnabled() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_NOTIFICATION_ENABLED), Boolean.valueOf(mRes.getString(R.string.PREF_NOTIFICATION_ENABLED_DEF_VALUE)));
        if (showDebugOutput) Log.d(TAG, "Notifications Enabled: " + tempbool);
        return tempbool;
    }

    public boolean getVibrate() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_NOTIFICATION_VIBRATE), Boolean.valueOf(mRes.getString(R.string.PREF_NOTIFICATION_VIBRATE_DEF_VALUE)));
        if (showDebugOutput) Log.d(TAG, "Notification Vibrate: " + tempbool);
        return tempbool;
    }

    public Uri getConfiguredRingtone() {
        String uri = mPrefs.getString(mRes.getString(R.string.PREF_NOTIFICATION_RINGTONE), null);
        if (uri == null) return null;

        return Uri.parse(uri);
    }

    public void setNotificationRingtone(String RingTone) {
        if (showDebugOutput) Log.d(TAG, "Setting RingtoneURL to " + RingTone);
        Editor editor = mPrefs.edit();
        editor.putString(mRes.getString(R.string.PREF_NOTIFICATION_RINGTONE), RingTone);
        if (!editor.commit()) Log.e(TAG, "Unable to write Ringtone URI");
    }

    public boolean doNandroidBackup() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DO_NANDROID_BACKUP), Boolean.valueOf(mRes.getString(R.string.PREF_DO_NANDROID_BACKUP_DEF_VALUE)));
        Log.d(TAG, "Do Nandroid Backup: " + tempbool);
        return tempbool;
    }

    //Advanced Properties
    public String getUpdateFolder() {
        temp = mPrefs.getString(mRes.getString(R.string.PREF_UPDATE_FOLDER), Customization.DOWNLOAD_DIR);
        if (showDebugOutput) Log.d(TAG, "UpdateFolder: " + temp);
        return temp;
    }

    public boolean setUpdateFolder(String folder) {
        String folderTrimmed = folder.trim();
        File f = new File(Environment.getExternalStorageDirectory() + "/" + folderTrimmed);
        if (f.isFile())
            return false;
        if (!f.exists())
            f.mkdirs();
        if (f.exists() && f.isDirectory()) {
            Editor editor = mPrefs.edit();
            editor.putString(mRes.getString(R.string.PREF_UPDATE_FOLDER), folderTrimmed);
            if (!editor.commit()) {
                Log.e(TAG, "Unable to write Update Folder Path");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public int getProgressUpdateFreq() {
        temp = mPrefs.getString(mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY), mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF_VALUE));
        if (showDebugOutput) Log.d(TAG, "ProgressUpdateFrequency: " + temp);
        return Integer.parseInt(temp);
    }

    public void setProgressUpdateFreq(String freq) {
        if (showDebugOutput) Log.d(TAG, "Setting ProgressUpdate Frequency to " + freq);
        Editor editor = mPrefs.edit();
        editor.putString(mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY), freq);
        if (!editor.commit()) Log.e(TAG, "Unable to write Update Frequency");
    }

    public boolean displayDebugOutput() {
        tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DEBUG_OUTPUT), Boolean.valueOf(mRes.getString(R.string.PREF_DEBUG_OUTPUT_DEF_VALUE))); 
        if (showDebugOutput) Log.d(TAG, "Display Debug Output: " + tempbool);
        return tempbool;
    }

}
