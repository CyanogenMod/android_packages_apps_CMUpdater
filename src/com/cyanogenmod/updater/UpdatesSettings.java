/*
 * Copyright (C) 2012 The CyanogenMod Project (DvTonder)
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.NotificationManager;
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
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.service.UpdateCheckService;
import com.cyanogenmod.updater.utils.UpdateFilter;
import com.cyanogenmod.updater.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

public class UpdatesSettings extends PreferenceActivity implements
        OnPreferenceChangeListener, UpdatePreference.OnReadyListener, UpdatePreference.OnActionListener {
    private static String TAG = "UpdatesSettings";

    // intent extras
    public static final String EXTRA_UPDATE_LIST_UPDATED = "update_list_updated";
    public static final String EXTRA_FINISHED_DOWNLOAD_ID = "download_id";
    public static final String EXTRA_FINISHED_DOWNLOAD_PATH = "download_path";

    private static final String UPDATES_CATEGORY = "updates_category";

    private static final int MENU_REFRESH = 0;
    private static final int MENU_DELETE_ALL = 1;
    private static final int MENU_SYSTEM_INFO = 2;

    private SharedPreferences mPrefs;
    private CheckBoxPreference mBackupRom;
    private ListPreference mUpdateCheck;
    private ListPreference mUpdateType;

    private PreferenceCategory mUpdatesList;
    private UpdatePreference mDownloadingPreference;

    private File mUpdateFolder;

    private boolean mStartUpdateVisible = false;
    private ProgressDialog mProgressDialog;

    private DownloadManager mDownloadManager;
    private boolean mDownloading = false;
    private long mDownloadId;
    private String mFileName;

    private Handler mUpdateHandler = new Handler();

    private BroadcastReceiver mUpdateCheckFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            updateLayout();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        // Load the layouts
        addPreferencesFromResource(R.xml.main);
        mUpdatesList = (PreferenceCategory) findPreference(UPDATES_CATEGORY);
        mUpdateCheck = (ListPreference) findPreference(Constants.UPDATE_CHECK_PREF);
        mUpdateType = (ListPreference) findPreference(Constants.UPDATE_TYPE_PREF);

        // Load the stored preference data
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mUpdateCheck != null) {
            int check = mPrefs.getInt(Constants.UPDATE_CHECK_PREF, Constants.UPDATE_FREQ_WEEKLY);
            mUpdateCheck.setValue(String.valueOf(check));
            mUpdateCheck.setSummary(mapCheckValue(check));
            mUpdateCheck.setOnPreferenceChangeListener(this);
        }

        if (mUpdateType != null) {
            int type = mPrefs.getInt(Constants.UPDATE_TYPE_PREF, 0);
            mUpdateType.setValue(String.valueOf(type));
            mUpdateType.setSummary(mUpdateType.getEntries()[type]);
            mUpdateType.setOnPreferenceChangeListener(this);
        }

        /* TODO: add this back once we have a way of doing backups that is not recovery specific
        mBackupRom = (CheckBoxPreference) findPreference(Constants.BACKUP_PREF);
        mBackupRom.setChecked(mPrefs.getBoolean(Constants.BACKUP_PREF, true));
        */

        // Set 'HomeAsUp' feature of the actionbar to fit better into Settings
        final ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Turn on the Options Menu
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh)
                .setIcon(R.drawable.ic_menu_refresh)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS
                        | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all)
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
                return true;

            case MENU_SYSTEM_INFO:
                showSysInfo();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if we need to refresh the screen to show new updates
        if (intent.getBooleanExtra(EXTRA_UPDATE_LIST_UPDATED, false)) {
            updateLayout();
        }

        // Check if we have been asked to start the 'download completed' functionality
        long id = intent.getLongExtra(EXTRA_FINISHED_DOWNLOAD_ID, -1);
        String fullPath = intent.getStringExtra(EXTRA_FINISHED_DOWNLOAD_PATH);
        if (id != -1 && fullPath != null) {
            downloadCompleted(id, fullPath);
        }
    }

    @Override
    public void onReady(UpdatePreference pref) {
        pref.setOnReadyListener(null);
        mUpdateHandler.post(mUpdateProgress);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUpdateCheck) {
            int value = Integer.valueOf((String) newValue);
            mPrefs.edit().putInt(Constants.UPDATE_CHECK_PREF, value).apply();
            mUpdateCheck.setSummary(mapCheckValue(value));
            Utils.scheduleUpdateService(this, value * 1000);
            return true;
        } else if (preference == mUpdateType) {
            int value = Integer.valueOf((String) newValue);
            mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, value).apply();
            mUpdateType.setSummary(mUpdateType.getEntries()[value]);
            checkForUpdates();
            return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Determine if there are any in-progress downloads
        mDownloadId = mPrefs.getLong(Constants.DOWNLOAD_ID, -1);
        if (mDownloadId >= 0) {
            Cursor c = mDownloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
            if (c == null || !c.moveToFirst()) {
                Toast.makeText(this, R.string.download_not_found, Toast.LENGTH_LONG).show();
            } else {
                String fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (fileName != null) {
                    if (status == DownloadManager.STATUS_PENDING
                            || status == DownloadManager.STATUS_RUNNING
                            || status == DownloadManager.STATUS_PAUSED) {
                        File file = new File(fileName);
                        mFileName = file.getName().replace(".partial", "");
                    }
                }
            }
            if (c != null) {
                c.close();
            }
        }

        updateLayout();
        registerReceiver(mUpdateCheckFinishedReceiver,
                new IntentFilter(UpdateCheckService.ACTION_CHECK_FINISHED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        unregisterReceiver(mUpdateCheckFinishedReceiver);
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    @Override
    public void onStartDownload(UpdatePreference pref) {
        // If there is no internet connection, display a message and return.
        if (!Utils.isOnline(this)) {
            Toast.makeText(this, R.string.data_connection_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mDownloading) {
            Toast.makeText(this, R.string.download_already_running, Toast.LENGTH_LONG).show();
            return;
        }

        // We have a match, get ready to trigger the download
        mDownloadingPreference = pref;

        UpdateInfo ui = mDownloadingPreference.getUpdateInfo();
        if (ui == null) {
            return;
        }

        mDownloadingPreference.setStyle(UpdatePreference.STYLE_DOWNLOADING);

        // Create the download request and set some basic parameters
        String fullFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + Constants.UPDATES_FOLDER;

        // If directory doesn't exist, create it
        File directory = new File(fullFolderPath);
        if (!directory.exists()) {
            directory.mkdirs();
            Log.d(TAG, "UpdateFolder created");
        }

        // Save the Changelog content to the sdcard for later use
        writeLogFile(ui.getFileName(), ui.getChangeLog());

        // Build the name of the file to download, adding .partial at the end.  It will get
        // stripped off when the download completes
        String fullFilePath = "file://" + fullFolderPath + "/" + ui.getFileName() + ".partial";

        Request request = new Request(Uri.parse(ui.getDownloadUrl()));
        String userAgent = Utils.getUserAgentString(this);
        if (userAgent != null) {
            request.addRequestHeader("User-Agent", userAgent);
        }
        request.addRequestHeader("Cache-Control", "no-cache");

        request.setTitle(getString(R.string.app_name));
        request.setDestinationUri(Uri.parse(fullFilePath));
        request.setAllowedOverRoaming(false);
        request.setVisibleInDownloadsUi(false);

        // TODO: this could/should be made configurable
        request.setAllowedOverMetered(true);

        // Start the download
        mDownloadId = mDownloadManager.enqueue(request);
        mFileName = ui.getFileName();
        mDownloading = true;

        // Store in shared preferences
        mPrefs.edit()
                .putLong(Constants.DOWNLOAD_ID, mDownloadId)
                .putString(Constants.DOWNLOAD_MD5, ui.getMD5Sum())
                .apply();
        mUpdateHandler.post(mUpdateProgress);
    }

    private Runnable mUpdateProgress = new Runnable() {
        public void run() {
            if (!mDownloading || mDownloadingPreference == null) {
                return;
            }

            ProgressBar progressBar = mDownloadingPreference.getProgressBar();
            if (progressBar == null) {
                return;
            }

            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(mDownloadId);

            Cursor cursor = mDownloadManager.query(q);
            int status;

            if (cursor == null || !cursor.moveToFirst()) {
                // DownloadCompletedReceiver has likely already removed the download
                // from the DB due to failure or MD5 mismatch
                status = DownloadManager.STATUS_FAILED;
            } else {
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }

            switch (status) {
                case DownloadManager.STATUS_PENDING:
                    progressBar.setIndeterminate(true);
                    break;
                case DownloadManager.STATUS_PAUSED:
                case DownloadManager.STATUS_RUNNING:
                    int downloadedBytes = cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int totalBytes = cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    progressBar.setIndeterminate(totalBytes < 0);
                    progressBar.setMax(totalBytes);
                    progressBar.setProgress(downloadedBytes);
                    break;
                case DownloadManager.STATUS_FAILED:
                    mDownloadingPreference.setStyle(UpdatePreference.STYLE_NEW);
                    mDownloadId = -1;
                    mFileName = null;
                    mDownloading = false;
                    break;
            }

            if (cursor != null) {
                cursor.close();
            }
            if (status != DownloadManager.STATUS_FAILED) {
                mUpdateHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onStopDownload(final UpdatePreference pref) {
        if (!mDownloading || mFileName == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_download_cancelation_dialog_title)
                .setMessage(R.string.confirm_download_cancelation_dialog_message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Set the preference back to new style
                        pref.setStyle(UpdatePreference.STYLE_NEW);

                        // We are OK to stop download, trigger it
                        mDownloadManager.remove(mDownloadId);
                        mUpdateHandler.removeCallbacks(mUpdateProgress);
                        mDownloadId = -1;
                        mFileName = null;
                        mDownloading = false;

                        // Clear the stored data from shared preferences
                        mPrefs.edit()
                                .putLong(Constants.DOWNLOAD_ID, mDownloadId)
                                .putString(Constants.DOWNLOAD_MD5, "")
                                .apply();

                        Toast.makeText(UpdatesSettings.this,
                                R.string.download_cancelled, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void downloadCompleted(long downloadId, String fullPathName) {
        mDownloading = false;

        String fileName = new File(fullPathName).getName();

        // Find the matching preference so we can retrieve the UpdateInfo
        UpdatePreference pref = (UpdatePreference) mUpdatesList.findPreference(fileName);
        if (pref != null) {
            pref.setStyle(UpdatePreference.STYLE_DOWNLOADED);
            onStartUpdate(pref);
        }
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
                Intent cancelIntent = new Intent(UpdatesSettings.this, UpdateCheckService.class);
                cancelIntent.setAction(UpdateCheckService.ACTION_CANCEL_CHECK);
                startService(cancelIntent);
            }
        });

        Intent checkIntent = new Intent(UpdatesSettings.this, UpdateCheckService.class);
        checkIntent.setAction(UpdateCheckService.ACTION_CHECK_MANUAL);
        startService(checkIntent);

        mProgressDialog.show();
    }

    private void updateLayout() {
        // Read existing Updates
        ArrayList<String> existingFiles = new ArrayList<String>();

        mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/cmupdater");
        File[] files = mUpdateFolder.listFiles(new UpdateFilter(".zip"));

        // If Folder Exists and Updates are present(with md5files)
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    existingFiles.add(file.getName());
                }
            }
        }

        // Clear the notification if one exists
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);

        // Build list of updates
        LinkedList<UpdateInfo> availableUpdates = State.loadState(this);
        LinkedList<UpdateInfo> updates = new LinkedList<UpdateInfo>();

        for (String fileName : existingFiles) {
            updates.add(new UpdateInfo(fileName, readLogFile(fileName)));
        }
        for (UpdateInfo update : availableUpdates) {
            // Only add updates to the list that are not already downloaded
            if (existingFiles.contains(update.getFileName())) {
                continue;
            }
            updates.add(update);
        }

        Collections.sort(updates, new Comparator<UpdateInfo>() {
            @Override
            public int compare(UpdateInfo lhs, UpdateInfo rhs) {
                // sort in descending 'UI name' order (newest first)
                return -lhs.getName().compareTo(rhs.getName());
            }
        });

        // Update the preference list
        refreshPreferences(updates);
    }

    private String readLogFile(String fileName) {
        StringBuilder text = new StringBuilder();

        File logFile = new File(mUpdateFolder, fileName + ".changelog");
        try {
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }

    private void writeLogFile(String fileName, String log) {
        File logFile = new File(mUpdateFolder, fileName + ".changelog");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
            bw.write(log);
            bw.close();
        } catch (IOException e) {
            Log.e(TAG, "File write failed", e);
        }
    }

    private void refreshPreferences(LinkedList<UpdateInfo> updates) {
        if (mUpdatesList == null) {
            return;
        }

        // Clear the list
        mUpdatesList.removeAll();

        // Convert the installed version name to the associated filename
        String installedZip = "cm-" + Utils.getInstalledVersion(true) + ".zip";

        // Add the updates
        for (UpdateInfo ui : updates) {
            // Determine the preference style and create the preference
            boolean isDownloading = ui.getFileName().equals(mFileName);
            int style;

            if (isDownloading) {
                // In progress download
                style = UpdatePreference.STYLE_DOWNLOADING;
            } else if (ui.getFileName().equals(installedZip)) {
                // This is the currently installed version
                style = UpdatePreference.STYLE_INSTALLED;
            } else if (ui.getDownloadUrl() != null) {
                style = UpdatePreference.STYLE_NEW;
            } else {
                style = UpdatePreference.STYLE_DOWNLOADED;
            }

            UpdatePreference up = new UpdatePreference(this, ui, style);
            up.setOnActionListener(this);
            up.setKey(ui.getFileName());

            // If we have an in progress download, link the preference
            if (isDownloading) {
                mDownloadingPreference = up;
                up.setOnReadyListener(this);
                mDownloading = true;
            }

            // Add to the list
            mUpdatesList.addPreference(up);
        }

        // If no updates are in the list, show the default message
        if (mUpdatesList.getPreferenceCount() == 0) {
            Preference pref = new Preference(this);
            pref.setLayoutResource(R.layout.preference_empty_list);
            pref.setTitle(R.string.no_available_updates_intro);
            pref.setEnabled(false);
            mUpdatesList.addPreference(pref);
        }
    }

    @Override
    public void onDeleteUpdate(UpdatePreference pref) {
        final String fileName = pref.getKey();

        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            File zipFileToDelete = new File(mUpdateFolder, fileName);
            File logFileToDelete = new File(mUpdateFolder, fileName + ".changelog");

            if (zipFileToDelete.exists()) {
                zipFileToDelete.delete();
            } else {
                Log.d(TAG, "Update to delete not found");
                return;
            }

            if (logFileToDelete.exists()) {
                logFileToDelete.delete();
            }

            String message = getString(R.string.delete_single_update_success_message, fileName);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else if (!mUpdateFolder.exists()) {
            Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_SHORT).show();
        }

        // Update the list
        updateLayout();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_all_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // We are OK to delete, trigger it
                        deleteOldUpdates();
                        updateLayout();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private boolean deleteOldUpdates() {
        boolean success;
        //mUpdateFolder: Foldername with fullpath of SDCARD
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            deleteDir(mUpdateFolder);
            mUpdateFolder.mkdir();
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
        Date lastCheck = new Date(mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
        String date = DateFormat.getLongDateFormat(this).format(lastCheck);
        String time = DateFormat.getTimeFormat(this).format(lastCheck);

        String message = getString(R.string.sysinfo_device) + " " + Utils.getDeviceType() + "\n\n"
                + getString(R.string.sysinfo_running) + " " + Utils.getInstalledVersion(true) + "\n\n"
                + getString(R.string.sysinfo_last_check) + " " + date + " " + time;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.menu_system_info)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        messageView.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Small);
    }

    @Override
    public void onStartUpdate(UpdatePreference pref) {
        final UpdateInfo updateInfo = pref.getUpdateInfo();

        // Prevent the dialog from being triggered more than once
        if (mStartUpdateVisible) {
            return;
        }

        mStartUpdateVisible = true;

        // Get the message body right
        String dialogBody = getString(R.string.apply_update_dialog_text, updateInfo.getFileName());

        // Display the dialog
        new AlertDialog.Builder(this)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(dialogBody)
                .setPositiveButton(R.string.dialog_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        triggerUpdate(updateInfo);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mStartUpdateVisible = false;
                    }
                })
                .show();
    }

    private void triggerUpdate(UpdateInfo updateInfo) {
        /*
         * Should perform the following steps.
         * 0.- Ask the user for a confirmation (already done when we reach here)
         * 1.- mkdir -p /cache/recovery
         * 2.- echo 'boot-recovery' > /cache/recovery/command
         * 3.- if(mBackup) echo '--nandroid'  >> /cache/recovery/command
         * 4.- echo '--update_package=SDCARD:update.zip' >> /cache/recovery/command
         * 5.- reboot recovery
         */
        try {
            // Set the 'boot recovery' command
            Process p = Runtime.getRuntime().exec("sh");
            OutputStream os = p.getOutputStream();
            os.write("mkdir -p /cache/recovery/\n".getBytes());
            os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());

            // See if backups are enabled and add the nandroid flag
            /* TODO: add this back once we have a way of doing backups that is not recovery specific
               if (mPrefs.getBoolean(Constants.BACKUP_PREF, true)) {
               os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
               }
               */

            // Add the update folder/file name
            // Emulated external storage moved to user-specific paths in 4.2
            String userPath = Environment.isExternalStorageEmulated() ? ("/" + UserHandle.myUserId()) : "";

            String cmd = "echo '--update_package=" + getStorageMountpoint() + userPath
                + "/" + Constants.UPDATES_FOLDER + "/" + updateInfo.getFileName()
                + "' >> /cache/recovery/command\n";
            os.write(cmd.getBytes());
            os.flush();

            // Trigger the reboot
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            powerManager.reboot("recovery");
        } catch (IOException e) {
            Log.e(TAG, "Unable to reboot into recovery mode", e);
            Toast.makeText(UpdatesSettings.this, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private String getStorageMountpoint() {
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = sm.getVolumeList();
        String primaryStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean alternateIsInternal = getResources().getBoolean(R.bool.alternateIsInternal);

        if (volumes.length <= 1) {
            // single storage, assume only /sdcard exists
            return "/sdcard";
        }

        for (int i = 0; i < volumes.length; i++) {
            StorageVolume v = volumes[i];
            if (v.getPath().equals(primaryStoragePath)) {
                /* This is the primary storage, where we stored the update file
                 *
                 * For CM10, a non-removable storage (partition or FUSE)
                 * will always be primary. But we have older recoveries out there 
                 * in which /sdcard is the microSD, and the internal partition is 
                 * mounted at /emmc.
                 *
                 * At buildtime, we try to automagically guess from recovery.fstab
                 * what's the recovery configuration for this device. If "/emmc"
                 * exists, and the primary isn't removable, we assume it will be 
                 * mounted there.
                 */ 
                if (!v.isRemovable() && alternateIsInternal) {
                    return "/emmc";
                }
            };
        }
        // Not found, assume non-alternate
        return "/sdcard";
    }
}
