/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.interfaces.IUpdateCheckService;
import com.cyanogenmod.updater.interfaces.IUpdateCheckServiceCallback;
import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.utils.StringUtils;
import com.cyanogenmod.updater.utils.SysUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;

public class UpdateCheckService extends Service {
    private static final String TAG = "UpdateCheckService";

    // Set this to true if the update service should check for smaller, test updates
    // This is for internal testing only
    private static final boolean TESTING_DOWNLOAD = false;

    private final RemoteCallbackList<IUpdateCheckServiceCallback> mCallbacks = new RemoteCallbackList<IUpdateCheckServiceCallback>();
    private String mSystemMod;
    private String mSystemRom;
    private Integer mCurrentBuildDate;
    private boolean mShowNightlyRomUpdates;
    private boolean mShowAllRomUpdates;
    private AutoCheckForUpdatesTask mTask;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        // Get the system MOD string
        mSystemMod = TESTING_DOWNLOAD ? "cmtestdevice" : SysUtils.getSystemProperty(Customization.BOARD);
        if (mSystemMod == null) {
                Log.i(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // See if we have an intent to parse
        if (intent != null) {
            boolean doCheck = intent.getBooleanExtra(Constants.CHECK_FOR_UPDATE, false);
            if (doCheck) {
                // If we should check for updates on start, do so in a seperate thread
                if (mTask == null || mTask.getStatus() == AsyncTask.Status.FINISHED) {
                    Log.i(TAG, "Checking for updates...");
                    mTask = new AutoCheckForUpdatesTask();
                    mTask.execute();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        mCallbacks.kill();
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        super.onDestroy();
    }

    //*********************************************************
    // Supporting methods and classes
    //*********************************************************
    private class AutoCheckForUpdatesTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            checkForNewUpdates();
            return null;
        }
    }

    private final IUpdateCheckService.Stub mBinder = new IUpdateCheckService.Stub() {

        public void registerCallback(IUpdateCheckServiceCallback cb) throws RemoteException {
            if (cb != null) mCallbacks.register(cb);
        }

        public void unregisterCallback(IUpdateCheckServiceCallback cb) throws RemoteException {
            if (cb != null) mCallbacks.unregister(cb);
        }

        public void checkForUpdates() throws RemoteException {
            checkForNewUpdates();
        }
    };

    private void displayExceptionToast(String ex) {
        mToastHandler.sendMessage(mToastHandler.obtainMessage(0, ex));
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private void checkForNewUpdates() {
        if (!isOnline()) {
            // Only check for updates if the phone is actually connected to a network
            Log.i(TAG, "Could not check for updates. Not connected to the network.");
            return;
        }

        // Start the update check
        FullUpdateInfo availableUpdates;
        while (true) {
            try {
                availableUpdates = getAvailableUpdates();
                break;
            } catch (IOException ex) {
                Log.e(TAG, "IOEx while checking for updates", ex);
                notifyCheckError(ex.getMessage());
                return;
            } catch (RuntimeException ex) {
                Log.e(TAG, "RuntimeEx while checking for updates", ex);
                notifyCheckError(ex.getMessage());
                return;
            }
        }

        // Store the last update check time and ensure boot check completed is true
        Date d = new Date();
        SharedPreferences prefs = getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        prefs.edit().putLong(Constants.LAST_UPDATE_CHECK_PREF, d.getTime()).apply();
        prefs.edit().putBoolean(Constants.BOOT_CHECK_COMPLETED, true).apply();

        int updateCountRoms = availableUpdates.getRomCount();
        int updateCount = availableUpdates.getUpdateCount();

        // Write to log
        Log.i(TAG, "The update check successfully completed at " + d.toString() + " and found "
                + updateCountRoms + " updates.");

        if (updateCountRoms == 0) {
            mToastHandler.sendMessage(mToastHandler.obtainMessage(0, R.string.no_updates_found, 0));
            finishUpdateCheck();
        } else {
            // There are updates available
            // The notification should launch the main app
            Intent i = new Intent(this, UpdatesSettings.class);
            i.putExtra(Constants.CHECK_FOR_UPDATE, true);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);

            Resources res = getResources();
            String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);

            // Get the notification ready
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.cm_updater);
            builder.setWhen(System.currentTimeMillis());
            builder.setTicker(res.getString(R.string.not_new_updates_found_ticker));

            // Set the rest of the notification content
            builder.setContentTitle(res.getString(R.string.not_new_updates_found_title));
            builder.setContentText(text);
            builder.setContentIntent(contentIntent);
            builder.setAutoCancel(true);
            Notification noti = builder.build();

            // Trigger the notification
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(R.string.not_new_updates_found_title, noti);

            // We are done
            finishUpdateCheck();
        }
    }

    private void notifyCheckError(String ExceptionText) {
        displayExceptionToast(ExceptionText);
        Log.e(TAG, "Update check error = " + ExceptionText);
        finishUpdateCheck();
    }

    private FullUpdateInfo getAvailableUpdates() throws IOException {
        FullUpdateInfo retValue = new FullUpdateInfo();
        boolean romException = false;
        HttpClient romHttpClient = new DefaultHttpClient();
        HttpEntity romResponseEntity = null;
        mSystemRom = SysUtils.getModVersion();
        mCurrentBuildDate = Integer.valueOf(SysUtils.getSystemProperty(Customization.BUILD_DATE));

        // Get the type of update we should check for
        SharedPreferences prefs = getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
        int updateType = prefs.getInt(Constants.UPDATE_TYPE_PREF, 0);
        if (updateType == 0) {
            mShowAllRomUpdates = false;
            mShowNightlyRomUpdates = false;
        } else if (updateType == 1) {
            mShowAllRomUpdates = false;
            mShowNightlyRomUpdates = true;
        } else if (updateType == 2) {
            mShowAllRomUpdates = true;
            mShowNightlyRomUpdates = false;
        } else if (updateType == 3) {
            mShowAllRomUpdates = true;
            mShowNightlyRomUpdates = true;
        }

        //Get the actual Rom Updateserver URL
        try {
            PackageManager manager = this.getPackageManager();
            URI RomUpdateServerUri = URI.create(getResources().getString(R.string.conf_update_server_url_def));
            HttpPost romReq = new HttpPost(RomUpdateServerUri);
            String getcmRequest = "{\"method\": \"get_all_builds\", \"params\":{\"device\":\""+mSystemMod+"\", \"channels\": [\"nightly\",\"stable\",\"snapshot\"]}}";
            romReq.setEntity(new ByteArrayEntity(getcmRequest.getBytes()));

            // Set the request headers
            romReq.addHeader("Cache-Control", "no-cache");
            try {
                PackageInfo pinfo = manager.getPackageInfo(this.getPackageName(), 0);
                romReq.addHeader("User-Agent", pinfo.packageName+"/"+pinfo.versionName);
            } catch (android.content.pm.PackageManager.NameNotFoundException nnfe) {
                // Do nothing
            }

            HttpResponse romResponse = romHttpClient.execute(romReq);
            int romServerResponse = romResponse.getStatusLine().getStatusCode();
            if (romServerResponse != HttpStatus.SC_OK) {
                Log.e(TAG, "Server returned status code for ROM " + romServerResponse);
                romException = true;
            }

            if (!romException) {
                romResponseEntity = romResponse.getEntity();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Rom Update request failed: " + e);
            romException = true;
        }

        try {
            if (!romException) {
                //Read the Rom Infos
                BufferedReader romLineReader = new BufferedReader(new InputStreamReader(romResponseEntity.getContent()), 2 * 1024);
                StringBuffer romBuf = new StringBuffer();
                String romLine;
                while ((romLine = romLineReader.readLine()) != null) {
                    romBuf.append(romLine);
                }
                romLineReader.close();

                LinkedList<UpdateInfo> romUpdateInfos = parseJSON(romBuf);
                retValue.roms = getRomUpdates(romUpdateInfos);
            } else {
                Log.e(TAG, "There was an Exception on Downloading the Rom JSON File");
            }
        } finally {
            if (romResponseEntity != null)
                romResponseEntity.consumeContent();
        }

        FullUpdateInfo ful = filterUpdates(retValue, State.loadState(this));
        if (!romException) {
            State.saveState(this, retValue);
        }
        return ful;
    }

    private LinkedList<UpdateInfo> parseJSON(StringBuffer buf) {
        LinkedList<UpdateInfo> uis = new LinkedList<UpdateInfo>();
        JSONObject mainJSONObject;
        try {
            mainJSONObject = new JSONObject(buf.toString());
            JSONArray updateList = mainJSONObject.getJSONArray(Constants.JSON_UPDATE_LIST);
            for (int i = 0, max = updateList.length(); i < max; i++) {
                if (!updateList.isNull(i)) {
                    uis.add(parseUpdateJSONObject(updateList.getJSONObject(i)));
                } else {
                    Log.e(TAG, "There is an error in your JSON File(update part). Maybe a , after the last update");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error in JSON File: ", e);
        }
        return uis;
    }

    private UpdateInfo parseUpdateJSONObject(JSONObject obj) {
        UpdateInfo ui = new UpdateInfo();
        try {
            ui.setName(obj.getString(Constants.JSON_FILENAME).trim());
            ui.setVersion(obj.getString(Constants.JSON_FILENAME).trim());
            ui.setDate(obj.getString(Constants.JSON_TIMESTAMP).trim());
            ui.setDownloadUrl(obj.getString(Constants.JSON_URL).trim());
            ui.setMD5(obj.getString(Constants.JSON_MD5SUM).trim());
            ui.setBranchCode(obj.getString(Constants.JSON_BRANCH).trim());
            ui.setFileName(obj.getString(Constants.JSON_FILENAME).trim());
            ui.setChanges(returnFullChangeLog(obj.getString(Constants.JSON_CHANGES)));
            ui.setChangelogUrl(obj.getString(Constants.JSON_CHANGES));

        } catch (JSONException e) {
            Log.e(TAG, "Error in JSON File: ", e);
        }
        return ui;
    }

    private String returnFullChangeLog(String changeLogPath) {
        String fullChangeLog = getResources().getString(R.string.no_changelog_alert);

        HttpEntity changeLogResponseEntity = null;
        HttpClient changeLogHttpClient = new DefaultHttpClient();

        try {
            URI ChangeLogUpdateServerUri = URI.create(changeLogPath);
            HttpGet changeLogReq = new HttpGet(ChangeLogUpdateServerUri);
            changeLogReq.addHeader("Cache-Control", "no-cache");
            HttpResponse changeLogResponse = changeLogHttpClient.execute(changeLogReq);
            int changeLogServerResponse = changeLogResponse.getStatusLine().getStatusCode();

            if (changeLogServerResponse == HttpStatus.SC_OK) {
                changeLogResponseEntity = changeLogResponse.getEntity();
                BufferedReader changeLogLineReader;
                changeLogLineReader = new BufferedReader(new InputStreamReader(changeLogResponseEntity.getContent()), 2 * 1024);
                try {
                    StringBuilder changeLogBuf = new StringBuilder();
                    String changeLogLine;
                    boolean categoryMatch = false;
                    while ((changeLogLine = changeLogLineReader.readLine()) != null) {
                        changeLogLine = changeLogLine.trim();
                        if (!changeLogLine.isEmpty()) {
                            if (changeLogLine.startsWith("=")) {
                                categoryMatch = !categoryMatch;
                            } else if (categoryMatch) {
                                if (changeLogBuf.length() != 0) {
                                    changeLogBuf.append("<br />");
                                }
                                changeLogBuf.append("<b><u>").append(changeLogLine).append("</u></b>").append("<br />");
                            } else if (changeLogLine.startsWith("*")) {
                                changeLogBuf.append("<br /><b>").append(changeLogLine.replaceAll("\\*", "")).append("</b>").append("<br />");
                            } else {
                                changeLogBuf.append("&#8226;&nbsp;").append(changeLogLine).append("<br />");
                            }
                        }
                    }
                    fullChangeLog = changeLogBuf.toString();
                } catch (IOException e) {
                    fullChangeLog = getResources().getString(R.string.failed_to_load_changelog);
                } catch (IllegalStateException e) {
                    fullChangeLog = getResources().getString(R.string.failed_to_load_changelog);
                } finally {
                    if (changeLogResponseEntity != null) {
                        changeLogResponseEntity.consumeContent();
                    }

                    if (changeLogLineReader != null) {
                        changeLogLineReader.close();
                    }
                }
            }
        } catch (IOException e) {
            fullChangeLog = getResources().getString(R.string.failed_to_load_changelog);
        } catch (IllegalArgumentException e) {
            fullChangeLog = getResources().getString(R.string.failed_to_load_changelog);
        }

        return fullChangeLog;
    }

    private boolean branchMatches(UpdateInfo ui, boolean nightlyAllowed) {
        if (ui == null) {
            return false;
        }

        boolean allow = false;
        if (ui.getBranchCode().equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_NIGHTLY)) {
            if (nightlyAllowed) {
                allow = true;
            }
        } else {
            allow = true;
        }
        return allow;
    }

    private LinkedList<UpdateInfo> getRomUpdates(LinkedList<UpdateInfo> updateInfos) {
        LinkedList<UpdateInfo> ret = new LinkedList<UpdateInfo>();
        for (int i = 0, max = updateInfos.size(); i < max; i++) {
            UpdateInfo ui = updateInfos.poll();
            if (mShowAllRomUpdates || StringUtils.compareVersions(ui.getVersion(), mSystemRom,ui.getDate(),mCurrentBuildDate)) {
                if (branchMatches(ui, mShowNightlyRomUpdates)) {
                    ret.add(ui);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static FullUpdateInfo filterUpdates(FullUpdateInfo newList, FullUpdateInfo oldList) {
        FullUpdateInfo ful = new FullUpdateInfo();
        ful.roms = (LinkedList<UpdateInfo>) newList.roms.clone();
        ful.roms.removeAll(oldList.roms);
        return ful;
    }

    private final Handler mToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0) {
                Toast.makeText(UpdateCheckService.this, msg.arg1, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UpdateCheckService.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void finishUpdateCheck() {
        final int M = mCallbacks.beginBroadcast();
        for (int i = 0; i < M; i++) {
            try {
                mCallbacks.getBroadcastItem(i).updateCheckFinished();
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }
}
