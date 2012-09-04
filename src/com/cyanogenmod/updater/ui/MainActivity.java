package com.cyanogenmod.updater.ui;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.listadapters.UpdateListAdapter;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.tasks.ChangelogTask;
import com.cyanogenmod.updater.tasks.MD5CheckerTask;
import com.cyanogenmod.updater.tasks.UpdateCheckTask;
import com.cyanogenmod.updater.tasks.ChangelogTask.ChangelogType;
import com.cyanogenmod.updater.utils.Preferences;
import com.cyanogenmod.updater.utils.StringUtils;
import com.cyanogenmod.updater.utils.SysUtils;
import com.cyanogenmod.updater.utils.UpdateFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    //Dialogs
    private static final int DIALOG_NO_SDCARD = 1;
    private static final int DIALOG_OVERWRITE_UPDATE = 2;
    private static final int DIALOG_DELETE_EXISTING = 3;
    private static final int DIALOG_NO_MD5 = 5;
    private static final int DIALOG_ABOUT = 6;

    private Boolean showDebugOutput = false;

    private Spinner mUpdatesSpinner;
    private Spinner mExistingUpdatesSpinner;
    private File mUpdateFolder;
    private ViewFlipper flipper;
    private Preferences prefs;
    private AsyncTask<File, Void, Boolean> md5CheckerTask;
    private File foo;
    private UpdateInfo updateForDownload;
    private String existingUpdateFilename;
    private UpdateListAdapter<UpdateInfo> spAdapterRoms;
    private ArrayAdapter<String> localUpdates;

    public void ListenerDownloadUpdateButton(View target) {
        if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())) {
            showDialog(DIALOG_NO_SDCARD);
            return;
        }
        if (showDebugOutput) Log.d(TAG, "Download Rom Button clicked");
        updateForDownload = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
        //Check if the File is present, so prompt the User to overwrite it
        foo = new File(mUpdateFolder + "/" + updateForDownload.getFileName());
        if (foo.isFile() && foo.exists()) {

            showDialog(DIALOG_OVERWRITE_UPDATE);
            return;
        }
        //Otherwise download it
        else {
            downloadRequestedUpdate(updateForDownload);
        }
    }

    public void ListenerUpdateChangelogButton(View target) {
        if (showDebugOutput) Log.d(TAG, "Rom Changelog Button clicked");
        getChangelog(ChangelogType.ROM);
    }

    public void ListenerDeleteUpdatesButton(View target) {
        if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())) {
            showDialog(DIALOG_NO_SDCARD);
            return;
        } else {
            showDialog(DIALOG_DELETE_EXISTING);
        }
    }

    public void ListenerApplyExistingButton(View target) {
        if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())) {
            showDialog(DIALOG_NO_SDCARD);
            return;
        }

        existingUpdateFilename = (String) mExistingUpdatesSpinner.getSelectedItem();
        if (showDebugOutput) Log.d(TAG, "Selected to Apply Existing update: " + existingUpdateFilename);
        File Update = new File(mUpdateFolder + "/" + existingUpdateFilename);
        File MD5 = new File(mUpdateFolder + "/" + existingUpdateFilename + ".md5sum");
        //IF no MD5 exists, ask the User what to do
        if (!MD5.exists() || !MD5.canRead()) {
            showDialog(DIALOG_NO_MD5);
        }
        //If MD5 exists, apply the update normally
        else {
            Resources res = getResources();
            ProgressDialog progressDialog = ProgressDialog.show(
                    MainActivity.this,
                    res.getString(R.string.verify_and_apply_dialog_title),
                    res.getString(R.string.verify_and_apply_dialog_message),
                    true,
                    true,
                    new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                    if (md5CheckerTask != null)
                    md5CheckerTask.cancel(true);
                    }
                    }
                    );

            md5CheckerTask = new MD5CheckerTask(this, progressDialog, existingUpdateFilename, showDebugOutput).execute(Update);
        }
    }

    public void ListenerFlipperButton(View target) {
        int id = target.getId();
        int child = -1;
        switch (id) {
            case R.id.button_available_updates:
                child = Constants.FLIPPER_AVAILABLE_UPDATES;
                break;
            case R.id.button_existing_updates:
                child = Constants.FLIPPER_EXISTING_UPDATES;
                break;
            default:
                return;
        }
        if (flipper.getDisplayedChild() != child)
            flipper.setDisplayedChild(child);
    }

    private final Spinner.OnItemSelectedListener mUpdateSpinnerChanged = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            Button updateChangelogButton = (Button) findViewById(R.id.show_changelog_button);
            List<String> changelog = ((UpdateInfo) mUpdatesSpinner.getSelectedItem()).getDescription();
            if (changelog == null || changelog.size() == 0) {
                updateChangelogButton.setVisibility(View.GONE);
            } else {
                updateChangelogButton.setVisibility(View.VISIBLE);
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {

        }
    };

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);			

            if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                Log.i(TAG, "SDcard Available");
            } else {
                Log.i(TAG, "SDcard Not Available");
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.nosdcardtoast,
                        (ViewGroup) findViewById(R.id.toast_layout_root));
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
                finish();
            }

            prefs = new Preferences(this);

            //Debug Output
            showDebugOutput = prefs.displayDebugOutput();

            if (showDebugOutput) Log.d(TAG, "onCreate called");

            //Sets the Title to Appname + Mod Version
            Resources res = getResources();
            setTitle(res.getString(R.string.app_name) + " " + res.getString(R.string.title_running) + " " + SysUtils.getModVersion());
            setContentView(R.layout.main);

            //Layout
            flipper = (ViewFlipper) findViewById(R.id.Flipper);
            mUpdatesSpinner = (Spinner) findViewById(R.id.available_updates_list);
            mUpdatesSpinner.setOnItemSelectedListener(mUpdateSpinnerChanged);
            spAdapterRoms = new UpdateListAdapter<UpdateInfo>(
                    this,
                    new ArrayList<UpdateInfo>());
            spAdapterRoms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAdapterRoms.setNotifyOnChange(false);
            mUpdatesSpinner.setAdapter(spAdapterRoms);

            mExistingUpdatesSpinner = (Spinner) findViewById(R.id.found_updates_list);
            localUpdates = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    new ArrayList<String>());
            localUpdates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            localUpdates.setNotifyOnChange(false);
            mExistingUpdatesSpinner.setAdapter(localUpdates);

            String mod = SysUtils.getModVersion();
        }

    @Override
        protected void onStart() {
            if (showDebugOutput) Log.d(TAG, "onStart called");
            super.onStart();
        }

    @Override
        protected void onResume() {
            if (showDebugOutput) Log.d(TAG, "onResume called");
            super.onResume();

            try {
                if (DownloadActivity.myService != null && DownloadActivity.myService.DownloadRunning()) {
                    UpdateInfo ui = DownloadActivity.myService.getCurrentUpdate();
                    Intent i = new Intent(MainActivity.this, DownloadActivity.class);
                    i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) ui);
                    startActivity(i);
                } else {
                    updateLayout();
                }
            }
            catch (RemoteException e) {
                Log.e(TAG, "Exception on calling DownloadService", e);
            }
        }

    @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            super.onCreateOptionsMenu(menu);
            menu.add(Menu.NONE, Constants.MENU_ID_UPDATE_NOW, Menu.NONE, R.string.menu_check_now)
                .setIcon(com.android.internal.R.drawable.ic_menu_rotate);
            menu.add(Menu.NONE, Constants.MENU_ID_CONFIG, Menu.NONE, R.string.menu_config)
                .setIcon(android.R.drawable.ic_menu_preferences);
            menu.add(Menu.NONE, Constants.MENU_ID_ABOUT, Menu.NONE, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
            return true;
        }

    @Override
        public boolean onPrepareOptionsMenu(Menu menu) {
            boolean superReturn = super.onPrepareOptionsMenu(menu);

            try {
                if (DownloadActivity.myService != null && DownloadActivity.myService.DownloadRunning()) {
                    //Download in progress
                    menu.findItem(Constants.MENU_ID_UPDATE_NOW).setEnabled(false);
                }
            }
            catch (RemoteException e) {
                Log.e(TAG, "Exception on calling DownloadService", e);
            }
            return superReturn;
        }

    @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            switch (item.getItemId()) {
                case Constants.MENU_ID_UPDATE_NOW:
                    checkForUpdates(null);
                    return true;
                case Constants.MENU_ID_CONFIG:
                    showConfigActivity();
                    return true;
                case Constants.MENU_ID_ABOUT:
                    showAboutDialog();
                    return true;
                default:
                    Log.e(TAG, "Unknown Menu ID:" + item.getItemId());
                    break;
            }
            return super.onMenuItemSelected(featureId, item);
        }

    public void updateLayout() {
        FullUpdateInfo mAvailableUpdates = null;
        try {
            mAvailableUpdates = State.loadState(this, showDebugOutput);
        }
        catch (IOException e) {
            Log.e(TAG, "Unable to restore activity status", e);
        }

        //Rom Layout
        View roms = findViewById(R.id.rom_layout);
        TextView nightlyBuildsRomtv = (TextView) roms.findViewById(R.id.nightly_rom_updates_textview);
        TextView showDowngradesRomtv = (TextView) roms.findViewById(R.id.show_rom_downgrades_textview);
        TextView lastRomUpdateChecktv = (TextView) roms.findViewById(R.id.last_rom_update_check);
        Button selectUploadButton = (Button) roms.findViewById(R.id.download_update_button);
        TextView DownloadText = (TextView) roms.findViewById(R.id.available_updates_text);
        LinearLayout stableNightlyInfoUpdates = (LinearLayout) roms.findViewById(R.id.stable_nightly_description_container_updates);
        Button changelogButton = (Button) roms.findViewById(R.id.show_changelog_button);
        //No ROM Updates Found Layout
        Button CheckNowUpdateChooserUpdates = (Button) roms.findViewById(R.id.check_now_button_update_chooser_updates);
        TextView CheckNowUpdateChooserTextUpdates = (TextView) roms.findViewById(R.id.check_now_update_chooser_text_updates);

        //Existing Updates Layout
        View existing = findViewById(R.id.existing_layout);
        TextView mdownloadedUpdateText = (TextView) existing.findViewById(R.id.downloaded_update_found);
        Button mdeleteOldUpdatesButton = (Button) existing.findViewById(R.id.delete_updates_button);
        Button mapplyUpdateButton = (Button) existing.findViewById(R.id.apply_update_button);
        TextView mNoExistingUpdatesFound = (TextView) existing.findViewById(R.id.no_existing_updates_found_textview);

        //Read existing Updates
        List<String> existingFilenames = null;
        mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + prefs.getUpdateFolder());
        FilenameFilter f = new UpdateFilter(".zip");
        File[] files = mUpdateFolder.listFiles(f);
        //If Folder Exists and Updates are present(with md5files)
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

        //Reset all Visibilities
        CheckNowUpdateChooserTextUpdates.setVisibility(View.GONE);
        CheckNowUpdateChooserUpdates.setVisibility(View.GONE);
        selectUploadButton.setVisibility(View.VISIBLE);
        mUpdatesSpinner.setVisibility(View.VISIBLE);
        DownloadText.setVisibility(View.VISIBLE);
        stableNightlyInfoUpdates.setVisibility(View.VISIBLE);
        changelogButton.setVisibility(View.VISIBLE);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);

        //Nightly and All
        String showNightlyRomUpdates;
        String showAllRomUpdates;

        Resources res = getResources();
        String trueString = res.getString(R.string.true_string);
        String falseString = res.getString(R.string.false_string);

        if (prefs.showNightlyRomUpdates())
            showNightlyRomUpdates = trueString;
        else
            showNightlyRomUpdates = falseString;

        if (prefs.showAllRomUpdates())
            showAllRomUpdates = trueString;
        else
            showAllRomUpdates = falseString;

        nightlyBuildsRomtv.setText(MessageFormat.format(res.getString(R.string.p_allow_nightly_rom_versions_title) + ": {0}", showNightlyRomUpdates));
        showDowngradesRomtv.setText(MessageFormat.format(res.getString(R.string.p_display_older_rom_versions_title) + ": {0}", showAllRomUpdates));
        lastRomUpdateChecktv.setText(res.getString(R.string.last_update_check_text) + ": " + prefs.getLastUpdateCheckString());

        //Sets the Rom Variables
        List<UpdateInfo> availableRoms = null;
        if (mAvailableUpdates != null) {
            if (mAvailableUpdates.roms != null)
                availableRoms = mAvailableUpdates.roms;
            //Add the incrementalUpdates
            if (mAvailableUpdates.incrementalRoms != null)
                availableRoms.addAll(mAvailableUpdates.incrementalRoms);
        }

        //Rom Layout
        if (availableRoms != null && availableRoms.size() > 0) {
            spAdapterRoms.clear();
            for (UpdateInfo rom:availableRoms) {
                spAdapterRoms.add(rom);
            }
            spAdapterRoms.notifyDataSetChanged();
        } else {
            selectUploadButton.setVisibility(View.GONE);
            mUpdatesSpinner.setVisibility(View.GONE);
            DownloadText.setVisibility(View.GONE);
            stableNightlyInfoUpdates.setVisibility(View.GONE);
            changelogButton.setVisibility(View.GONE);
            CheckNowUpdateChooserTextUpdates.setVisibility(View.VISIBLE);
            CheckNowUpdateChooserUpdates.setVisibility(View.VISIBLE);
        }

        //Existing Updates Layout
        if (existingFilenames != null && existingFilenames.size() > 0) {
            localUpdates.clear();
            for (String file:existingFilenames) {
                localUpdates.add(file);
            }
            localUpdates.notifyDataSetChanged();
        } else {
            mNoExistingUpdatesFound.setVisibility(View.VISIBLE);
            mExistingUpdatesSpinner.setVisibility(View.GONE);
            mapplyUpdateButton.setVisibility(View.GONE);
            mdownloadedUpdateText.setVisibility(View.GONE);
            mdeleteOldUpdatesButton.setVisibility(View.GONE);
        }
    }

    private void getChangelog(ChangelogType changelogType) {
        switch (changelogType) {
            case ROM:
                //Get the ROM Changelog and Display the Changelog
                UpdateInfo uiRom = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
                new ChangelogTask(this).execute(changelogType, uiRom);
                break;
            case APP:
                new ChangelogTask(this).execute(changelogType);
                break;
            default:
                return;
        }
    }

    private void showConfigActivity() {
        Intent i = new Intent(this, ConfigActivity.class);
        startActivity(i);
    }

    public void checkForUpdates(View target) {
        //Refresh the Layout when UpdateCheck finished
        UpdateCheckTask task = new UpdateCheckTask(this, showDebugOutput);
        task.execute((Void) null);
        updateLayout();
    }

    private void showAboutDialog() {
        showDialog(DIALOG_ABOUT);
    }

    private void downloadRequestedUpdate(UpdateInfo ui) {
        Intent i = new Intent(MainActivity.this, DownloadActivity.class);
        i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) ui);
        startActivity(i);
        Toast.makeText(this, R.string.downloading_update, Toast.LENGTH_LONG).show();
    }

    private boolean deleteOldUpdates() {
        boolean success;
        //updateFolder: Foldername
        //mUpdateFolder: Foldername with fullpath of SDCARD
        String updateFolder = prefs.getUpdateFolder();
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && !updateFolder.trim().equals("") && !updateFolder.trim().equals("/")) {
            deleteDir(mUpdateFolder);
            mUpdateFolder.mkdir();
            if (showDebugOutput) Log.d(TAG, "Updates deleted and UpdateFolder created again");
            success = true;
            Toast.makeText(this, R.string.delete_updates_success_message, Toast.LENGTH_LONG).show();
        } else if (!mUpdateFolder.exists()) {
            success = false;
            Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
        } else if (updateFolder.trim().equals("") || updateFolder.trim().equals("/")) {
            success = false;
            Toast.makeText(this, R.string.delete_updates_root_folder_message, Toast.LENGTH_LONG).show();
        } else {
            success = false;
            Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
        }
        return success;
    }

    private boolean deleteUpdate(String filename) {
        boolean success = false;
        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
            File ZIPfiletodelete = new File(mUpdateFolder + "/" + filename);
            File MD5filetodelete = new File(mUpdateFolder + "/" + filename + ".md5sum");
            if (ZIPfiletodelete.exists()) {
                ZIPfiletodelete.delete();
            } else {
                if (showDebugOutput) Log.d(TAG, "Update to delete not found");
                if (showDebugOutput) Log.d(TAG, "Zip File: " + ZIPfiletodelete.getAbsolutePath());
                return false;
            }
            if (MD5filetodelete.exists()) {
                MD5filetodelete.delete();
            } else {
                if (showDebugOutput) Log.d(TAG, "MD5 to delete not found. No Problem here.");
                if (showDebugOutput) Log.d(TAG, "MD5 File: " + MD5filetodelete.getAbsolutePath());
            }
            ZIPfiletodelete = null;
            MD5filetodelete = null;

            success = true;
            Toast.makeText(this, MessageFormat.format(getResources().getString(R.string.delete_single_update_success_message), filename), Toast.LENGTH_LONG).show();
        } else if (!mUpdateFolder.exists()) {
            Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
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

    @Override
        protected Dialog onCreateDialog(int id) {
            switch (id) {
                case DIALOG_NO_SDCARD:
                    return new AlertDialog.Builder(this)
                        .setTitle(R.string.sdcard_is_not_present_dialog_title)
                        .setMessage(R.string.sdcard_is_not_present_dialog_body)
                        .setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                }
                                }).create();
                case DIALOG_OVERWRITE_UPDATE:
                    return new AlertDialog.Builder(this)
                        .setTitle(R.string.overwrite_update_title)
                        .setMessage(R.string.overwrite_update_summary)
                        .setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                }
                                })
                    .setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            foo.delete();
                            if (showDebugOutput)
                            Log.d(TAG, "Start downlading update: " + updateForDownload.getFileName());
                            downloadRequestedUpdate(updateForDownload);
                            }
                            }).create();
                case DIALOG_DELETE_EXISTING:
                    return new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_updates_text)
                        .setMessage(R.string.confirm_delete_update_folder_dialog_message)
                        //Delete Only Selected Update
                        .setNeutralButton(R.string.confirm_delete_update_folder_dialog_neutral, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                //Delete Updates here
                                String f = (String) mExistingUpdatesSpinner.getSelectedItem();
                                if (showDebugOutput) Log.d(TAG, "Delete single Update selected: " + f);
                                deleteUpdate(f);
                                updateLayout();
                                }
                                })
                    //Delete All Updates
                    .setPositiveButton(R.string.confirm_delete_update_folder_dialog_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            if (showDebugOutput) Log.d(TAG, "Delete all Updates selected");
                            //Delete Updates here
                            //Set the Filenames to null, so the Spinner will be empty
                            deleteOldUpdates();
                            updateLayout();
                            }
                            })
                    //Delete no Update
                    .setNegativeButton(R.string.confirm_delete_update_folder_dialog_no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            if (showDebugOutput) Log.d(TAG, "Delete no updates selected");
                            dialog.dismiss();
                            }
                            }).create();
                case DIALOG_NO_MD5:
                    return new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.no_md5_found_title)
                        .setMessage(R.string.no_md5_found_summary)
                        .setPositiveButton(R.string.no_md5_found_positive, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                //Directly call on Postexecute, cause we need no md5check
                                new MD5CheckerTask(getApplicationContext(), null, existingUpdateFilename, showDebugOutput).onPostExecute(true);
                                dialog.dismiss();
                                }
                                })
                    .setNegativeButton(R.string.no_md5_found_negative, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            }
                            }).create();
                case DIALOG_ABOUT:
                    Dialog dialog = new Dialog(this);
                    dialog.setTitle(R.string.about_dialog_title);
                    dialog.setContentView(R.layout.about);
                    TextView mVersionName = (TextView) dialog.findViewById(R.id.version_name_about_text_view);
                    try {
                        PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                        mVersionName.setText("v " + pi.versionName);
                    }
                    catch (NameNotFoundException e) {
                        Log.e(TAG, "Can't find version name", e);
                        mVersionName.setText("v unknown");
                    }
                    return dialog;
                default:
                    return null;
            }
        }
}
