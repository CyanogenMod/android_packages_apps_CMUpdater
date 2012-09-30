/*
 * Copyright (C) 2012 The CyanogenMod Project (DvTonder)
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

package com.cyanogenmod.updater;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.service.UpdateCheckService;
import com.cyanogenmod.updater.tasks.UpdateCheckTask;
import com.cyanogenmod.updater.utils.SysUtils;
import com.cyanogenmod.updater.utils.UpdateFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UpdatesSettings extends PreferenceActivity implements OnPreferenceChangeListener {

    private static String TAG = "UpdatesSettings";
    private static final boolean DEBUG = true;

    private static String UPDATES_CATEGORY = "updates_category";

    private static final int MENU_REFRESH = 0;
    private static final int MENU_DELETE_ALL = 1;
    private static final int MENU_SYSTEM_INFO = 2;

    private SharedPreferences mPrefs;
    private CheckBoxPreference mBackupRom;
    private ListPreference mUpdateCheck;
    private ListPreference mUpdateType;

    private PreferenceCategory mUpdatesList;

    private File mUpdateFolder;
    private ArrayList<UpdateInfo> mServerUpdates;
    private ArrayList<String> mLocalUpdates;

    private boolean mStartUpdateVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the layouts
        addPreferencesFromResource(R.xml.main);
        PreferenceScreen prefSet = getPreferenceScreen();
        mBackupRom = (CheckBoxPreference) prefSet.findPreference(Constants.BACKUP_PREF);
        mUpdatesList = (PreferenceCategory) prefSet.findPreference(UPDATES_CATEGORY);

        // Load the stored preference data
        mPrefs = getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        mBackupRom.setChecked(mPrefs.getBoolean(Constants.BACKUP_PREF, true));
        mUpdateCheck = (ListPreference) findPreference(Constants.UPDATE_CHECK_PREF);
        if (mUpdateCheck != null) {
            int check = mPrefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);
            mUpdateCheck.setValue(String.valueOf(check));
            mUpdateCheck.setSummary(mapCheckValue(check));
            mUpdateCheck.setOnPreferenceChangeListener(this);
        }

        mUpdateType = (ListPreference) findPreference(Constants.UPDATE_TYPE_PREF);
        if (mUpdateType != null) {
            int type = mPrefs.getInt(Constants.UPDATE_TYPE_PREF, 0);
            mUpdateType.setValue(String.valueOf(type));
            mUpdateType.setSummary(mUpdateType.getEntries()[type]);
            mUpdateType.setOnPreferenceChangeListener(this);
        }

        // Initialize the arrays
        mServerUpdates = new ArrayList<UpdateInfo>();
        mLocalUpdates = new ArrayList<String>();

        // Turn on the Options Menu and update the layout
        invalidateOptionsMenu();
        updateLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh)
                .setIcon(R.drawable.ic_menu_refresh)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS
                        | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all)
            //.setIcon(android.R.drawable.ic_menu_delete)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        menu.add(0, MENU_SYSTEM_INFO, 0, R.string.menu_system_info)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                checkForUpdates();
                return true;

            case MENU_DELETE_ALL:
                confirmDeleteAll();
                updateLayout();
                return true;

            case MENU_SYSTEM_INFO:
                showSysInfo();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mBackupRom) {
            mPrefs.edit().putBoolean(Constants.BACKUP_PREF, mBackupRom.isChecked()).apply();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        boolean doCheck = intent.getBooleanExtra(Constants.CHECK_FOR_UPDATE, false);
        if (doCheck) {
            // We have been asked to refresh the screen to show new updates
            updateLayout();
        }

        boolean startUpdate = intent.getBooleanExtra(Constants.START_UPDATE, false);
        if (startUpdate) {
            // We have been asked to refresh the screen to show new updates
            UpdateInfo ui = (UpdateInfo) intent.getSerializableExtra(Constants.KEY_UPDATE_INFO);
            if (ui != null) {
                startUpdate(ui);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUpdateCheck) {
            int value = Integer.valueOf((String) newValue);
            mPrefs.edit().putInt(Constants.UPDATE_CHECK_PREF, value).apply();
            mUpdateCheck.setSummary(mapCheckValue(value));
            scheduleUpdateService(value * 1000);
            return true;

        } else if (preference == mUpdateType) {
            int value = Integer.valueOf((String) newValue);
            mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, value).apply();
            mUpdateType.setSummary(mUpdateType.getEntries()[value]);
            // Trigger a new update check
            checkForUpdates();
            return true;
        }

        return false;
    }

    // TODO: figure out how to decouple the download from the app when a download
    // is running to prevent the FC on download finished, allowing for the download to continue
    // even after the user has backed out
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop any running downloads in a nice way
        try {
            if (DownloadUpdate.mDownloadService != null && DownloadUpdate.mDownloadService.isDownloadRunning()) {
                DownloadUpdate.mDownloadService.pauseDownload();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on calling DownloadService", e);
        }
    }

/* Remove for now
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (resetConfiguration(newConfig)) {
            // If a download is running, we have to re-attach the preference to the
            // downloader so the progressbar works after the config change
            try {
                if (DownloadUpdate.mDownloadService != null && DownloadUpdate.mDownloadService.isDownloadRunning()) {
                    UpdateInfo ui = DownloadUpdate.mDownloadService.getCurrentUpdate();

                    // Locate the preference that matches the key of the currently downloading file
                    // and attach the download to the new prefernce if found
                    UpdatePreference pref = findMatchingPreference(ui.getFileName());
                    if (pref != null) {
                        // TODO: figure out how to do the attach, for now, just pause and start
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on calling DownloadService", e);
            }
        }
    }

    // returns true if landscape/portrait configuration has changed
    private boolean resetConfiguration(Configuration config) {
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (mIsLandscape != isLandscape) {
            mIsLandscape = isLandscape;
            return true;
        }
        return false;
    }

*/
    //*********************************************************
    // Supporting methods
    //*********************************************************

    protected void startDownload(String key) {
        UpdatePreference pref = findMatchingPreference(key);
        if (pref != null) {
            // We have a match, trigger the download
            DownloadUpdate du = new DownloadUpdate(pref);
            du.startDownload();
            pref.setStyle(UpdatePreference.STYLE_DOWNLOADING);
        }
    }

    private UpdatePreference findMatchingPreference(String key) {
        if (mUpdatesList != null) {
            // Find the matching preference
            for (int i = 0; i < mUpdatesList.getPreferenceCount(); i++) {
                UpdatePreference pref = (UpdatePreference) mUpdatesList.getPreference(i);
                if (pref.getKey().equals(key)) {
                    // We have a match
                    return pref;
                }
            }
        }
        return null;
    }

    private String mapCheckValue(Integer value) {
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
    public void checkForUpdates() {
        //Refresh the Layout when UpdateCheck finished
        UpdateCheckTask task = new UpdateCheckTask(this);
        task.execute((Void) null);
        updateLayout();
    }

    public void updateLayout() {
        FullUpdateInfo availableUpdates = null;
        try {
            availableUpdates = State.loadState(this);
        } catch (IOException e) {
            Log.e(TAG, "Unable to restore activity status", e);
        }

        // Read existing Updates
        List<String> existingFilenames = null;
        mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/cmupdater");
        FilenameFilter f = new UpdateFilter(".zip");
        File[] files = mUpdateFolder.listFiles(f);

        // If Folder Exists and Updates are present(with md5files)
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null && files.length > 0) {
            //To show only the Filename. Otherwise the whole Path with /sdcard/cm-updates will be shown
            existingFilenames = new ArrayList<String>();
            for (File file : files) {
                existingFilenames.add(file.getName());
            }
            //For sorting the Filenames, have to find a way to do natural sorting
            existingFilenames = Collections.synchronizedList(existingFilenames);
            Collections.sort(existingFilenames, Collections.reverseOrder());
        }
        files = null;

        // Clear the notification if one exists
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);

        // Sets the Rom Variables
        List<UpdateInfo> availableRoms = null;
        if (availableUpdates != null) {
            if (availableUpdates.roms != null)
                availableRoms = availableUpdates.roms;
            //Add the incrementalUpdates
            if (availableUpdates.incrementalRoms != null)
                availableRoms.addAll(availableUpdates.incrementalRoms);
        }

        // Existing Updates Layout
        mLocalUpdates.clear();
        if (existingFilenames != null && existingFilenames.size() > 0) {
            for (String file:existingFilenames) {
                mLocalUpdates.add(file);
            }
        }

        // Available Roms Layout
        mServerUpdates.clear();
        if (availableRoms != null && availableRoms.size() > 0) {
            for (UpdateInfo rom:availableRoms) {

                // See if we have matching updates already downloaded
                boolean matched = false;
                for (String name : mLocalUpdates) {
                    if (name.equals(rom.getFileName())) {
                        matched = true;
                    }
                }

                // Only add updates to the server list that are not already downloaded
                if (!matched) {
                    mServerUpdates.add(rom);
                }
            }
        }

        // Update the preference list
        refreshPreferences();
    }

    private void refreshPreferences() {
        if (mUpdatesList != null) {
            // Clear the list
            mUpdatesList.removeAll();

            // Add the locally saved update files first
            if (!mLocalUpdates.isEmpty()) {
                // We have local updates to display
                for (String name : mLocalUpdates) {
                    UpdatePreference up = new UpdatePreference(this, null, name, UpdatePreference.STYLE_DOWNLOADED);
                    up.setKey(name);
                    mUpdatesList.addPreference(up);
                }
            }

            // Add the server based updates
            if (!mServerUpdates.isEmpty()) {
                // We have updates to display
                for (UpdateInfo ui : mServerUpdates) {
                    UpdatePreference up = new UpdatePreference(this, ui, ui.getName(), UpdatePreference.STYLE_NEW);
                    up.setKey(ui.getName());
                    mUpdatesList.addPreference(up);
                }
            }

            // If no updates are in the list, show the default message
            if (mUpdatesList.getPreferenceCount() == 0) {
                Preference npref = new Preference(this);
                npref.setLayoutResource(R.layout.preference_empty_list);
                npref.setTitle(R.string.no_available_updates_intro);
                npref.setEnabled(false);
                mUpdatesList.addPreference(npref);
            }
        }
    }

    public boolean deleteUpdate(String filename) {
        boolean success = false;
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            File ZIPfiletodelete = new File(mUpdateFolder + "/" + filename);
            File MD5filetodelete = new File(mUpdateFolder + "/" + filename + ".md5sum");
            if (ZIPfiletodelete.exists()) {
                ZIPfiletodelete.delete();
            } else {
                if (DEBUG) Log.d(TAG, "Update to delete not found");
                if (DEBUG) Log.d(TAG, "Zip File: " + ZIPfiletodelete.getAbsolutePath());
                return false;
            }
            if (MD5filetodelete.exists()) {
                MD5filetodelete.delete();
            } else {
                if (DEBUG) Log.d(TAG, "MD5 to delete not found. No Problem here.");
                if (DEBUG) Log.d(TAG, "MD5 File: " + MD5filetodelete.getAbsolutePath());
            }
            ZIPfiletodelete = null;
            MD5filetodelete = null;

            success = true;
            Toast.makeText(this, MessageFormat.format(getResources().getString(R.string.delete_single_update_success_message),
                    filename), Toast.LENGTH_SHORT).show();

        } else if (!mUpdateFolder.exists()) {
            Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_SHORT).show();
        }

        // Update the list
        updateLayout();
        return success;
    }

    private void scheduleUpdateService(int updateFrequency) {

        // Get the intent ready
        Intent i = new Intent(this, UpdateCheckService.class);
        i.putExtra(Constants.CHECK_FOR_UPDATE, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

        // Clear any old alarms before we start
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        // Check if we need to schedule a new alarm
        if (updateFrequency > 0) {
            Log.d(TAG, "Update frequency is " + updateFrequency);

            // Get the last time we checked for an update
            Date lastCheck = new Date(mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
            Log.d(TAG, "Last check was " + lastCheck.toString());

            // Set the new alarm
            am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck.getTime() + updateFrequency, updateFrequency, pi);
        }
    }

    private void confirmDeleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_delete_dialog_title);
        builder.setMessage(R.string.confirm_delete_all_dialog_message);
        builder.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // We are OK to delete, trigger it
                deleteOldUpdates();
            }
        });
        builder.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean deleteOldUpdates() {
        boolean success;
        //mUpdateFolder: Foldername with fullpath of SDCARD
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            deleteDir(mUpdateFolder);
            mUpdateFolder.mkdir();
            if (DEBUG)
                Log.d(TAG, "Updates deleted and UpdateFolder created again");
            success = true;
            Toast.makeText(this, R.string.delete_updates_success_message, Toast.LENGTH_SHORT).show();
        } else if (!mUpdateFolder.exists()) {
            success = false;
            Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_SHORT).show();
        } else {
            success = false;
            Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_SHORT).show();
        }
        return success;
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    private void showSysInfo() {
        // Build the message
        String systemMod = SysUtils.getSystemProperty(Customization.BOARD);
        String systemRom = SysUtils.getSystemProperty(Customization.SYS_PROP_MOD_VERSION);
        Date lastCheck = new Date(mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
        String message = getString(R.string.sysinfo_device) + " " + systemMod + "\n\n"
                + getString(R.string.sysinfo_running)+ " "+ systemRom + "\n\n"
                + getString(R.string.sysinfo_last_check) + " " + lastCheck.toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_system_info);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setTextAppearance(this,
                android.R.style.TextAppearance_DeviceDefault_Small);
    }

    protected void startUpdate(final UpdateInfo updateInfo) {
        if (DEBUG)
            Log.d(TAG, "Filename selected to flash: " + updateInfo.getFileName());

        // Prevent the dialog from being triggered more than once
        if (mStartUpdateVisible) {
            return;
        } else {
            mStartUpdateVisible = true;
        }

        // Get the message body right
        String dialogBody = MessageFormat.format(
                getResources().getString(R.string.apply_update_dialog_text),
                updateInfo.getFileName());

        // Display the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.apply_update_dialog_title);
            builder.setMessage(dialogBody);
            builder.setPositiveButton(R.string.dialog_update, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    /*
                     * Should perform the following steps.
                     * 0.- Ask the user for a confirmation (already done when we reach here)
                     * 1.- su
                     * 2.- mkdir -p /cache/recovery
                     * 3.- echo 'boot-recovery' > /cache/recovery/command
                     * 4.- if(mBackup) echo '--nandroid'  >> /cache/recovery/command
                     * 5.- echo '--update_package=SDCARD:update.zip' >> /cache/recovery/command
                     * 6.- reboot recovery 
                     */
                    try {
                        // Set the 'boot recovery' command
                        Process p = Runtime.getRuntime().exec("sh");
                        OutputStream os = p.getOutputStream();
                        os.write("mkdir -p /cache/recovery/\n".getBytes());
                        os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());

                        // See if backups are enabled and add the nandroid flag
                        SharedPreferences prefs = getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
                        if (prefs.getBoolean(Constants.BACKUP_PREF, true)) {
                            os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
                        }

                        // Add the update folder/file name
                        // TODO: this is where it handled the external storage command, now assume /sdcard/cmupdater
                        String cmd = "echo '--update_package=/sdcard/cmupdater/" + updateInfo.getFileName()
                                + "' >> /cache/recovery/command\n";
                        os.write(cmd.getBytes());
                        os.flush();
                        Toast.makeText(UpdatesSettings.this, R.string.apply_trying_to_get_root_access, Toast.LENGTH_SHORT).show();
                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        powerManager.reboot("recovery");

                    } catch (IOException e) {
                        Log.e(TAG, "Unable to reboot into recovery mode:", e);
                        Toast.makeText(UpdatesSettings.this, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mStartUpdateVisible = false;
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
