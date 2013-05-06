/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.utils.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class UpdateCheckService extends Service {
    private static final String TAG = "UpdateCheckService";

    // Set this to true if the update service should check for smaller, test updates
    // This is for internal testing only
    private static final boolean TESTING_DOWNLOAD = false;

    public static final String ACTION_UPDATE_DATA_UPDATED = "com.cyanogenmod.cmupdater.action.UPDATE_DATA_UPDATED";
    public static final String ACTION_CHECK_FINISHED = "com.cyanogenmod.cmupdater.action.UPDATE_CHECK_FINISHED";

    private CheckForUpdatesTask mTask;

    private final Handler mToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0) {
                Toast.makeText(UpdateCheckService.this, msg.arg1, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UpdateCheckService.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                    mTask = new CheckForUpdatesTask();
                    mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(true);
        }
        super.onDestroy();
    }

    private void finishUpdateCheck() {
        sendBroadcast(new Intent(ACTION_CHECK_FINISHED));
        stopSelf();
    }

    private class CheckForUpdatesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (!isCancelled()) {
                checkForNewUpdates();
            } else {
                Log.i(TAG, "Update check cancelled by the user.");
            }
            finishUpdateCheck();
            return null;
        }

        private void checkForNewUpdates() {
            final Context context = UpdateCheckService.this;

            if (!Utils.isOnline(context)) {
                // Only check for updates if the device is actually connected to a network
                Log.i(TAG, "Could not check for updates. Not connected to the network.");
                return;
            }

            // Start the update check
            LinkedList<UpdateInfo> availableUpdates;
            try {
                availableUpdates = getAvailableUpdates();
            } catch (IOException e) {
                Log.e(TAG, "Could not check for updates", e);
                availableUpdates = null;
            }

            if (availableUpdates == null || isCancelled()) {
                return;
            }

            // Store the last update check time and ensure boot check completed is true
            Date d = new Date();
            PreferenceManager.getDefaultSharedPreferences(UpdateCheckService.this).edit()
                    .putLong(Constants.LAST_UPDATE_CHECK_PREF, d.getTime())
                    .putBoolean(Constants.BOOT_CHECK_COMPLETED, true)
                    .apply();

            // Write to log
            Log.i(TAG, "The update check successfully completed at " + d + " and found "
                    + availableUpdates.size() + " updates.");

            if (availableUpdates.isEmpty()) {
                mToastHandler.sendMessage(mToastHandler.obtainMessage(0, R.string.no_updates_found, 0));
            } else {
                // There are updates available
                // The notification should launch the main app
                Intent i = new Intent(context, UpdatesSettings.class);
                i.putExtra(Constants.CHECK_FOR_UPDATE, true);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT);

                Resources res = getResources();
                String text = res.getQuantityString(R.plurals.not_new_updates_found_body,
                        availableUpdates.size(), availableUpdates.size());

                // Get the notification ready
                Notification.Builder builder = new Notification.Builder(context)
                        .setSmallIcon(R.drawable.cm_updater)
                        .setWhen(System.currentTimeMillis())
                        .setTicker(res.getString(R.string.not_new_updates_found_ticker))
                        .setContentTitle(res.getString(R.string.not_new_updates_found_title))
                        .setContentText(text)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true);

                // Trigger the notification
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(R.string.not_new_updates_found_title, builder.build());
            }
        }

        private void addRequestHeaders(HttpRequestBase request) {
            String userAgent = Utils.getUserAgentString(UpdateCheckService.this);
            if (userAgent != null) {
                request.addHeader("User-Agent", userAgent);
            }
            request.addHeader("Cache-Control", "no-cache");
        }

        private LinkedList<UpdateInfo> getAvailableUpdates() throws IOException {
            final Context context = UpdateCheckService.this;
            // Get the type of update we should check for
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int updateType = prefs.getInt(Constants.UPDATE_TYPE_PREF, 0);

            // Get the actual ROM Update Server URL
            URI updateServerUri = URI.create(getString(R.string.conf_update_server_url_def));
            HttpPost request = new HttpPost(updateServerUri);

            try {
                JSONObject requestJson = buildUpdateRequest(updateType);
                request.setEntity(new StringEntity(requestJson.toString()));
            } catch (JSONException e) {
                Log.e(TAG, "Could not build request", e);
                return null;
            }
            addRequestHeaders(request);

            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);

            if (isCancelled() || response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return null;
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }

            LinkedList<UpdateInfo> lastUpdates = State.loadState(context);
            HashMap<String, UpdateInfo> knownUpdates = new HashMap<String, UpdateInfo>();
            for (UpdateInfo ui : lastUpdates) {
                knownUpdates.put(ui.getFileName(), ui);
            }

            // Read the ROM Infos
            String json = EntityUtils.toString(entity, "UTF-8");
            LinkedList<UpdateInfo> updates = parseJSON(json, updateType, knownUpdates);

            if (isCancelled()) {
                return null;
            }

            State.saveState(context, updates);

            Intent updateIntent = new Intent(ACTION_UPDATE_DATA_UPDATED);
            updateIntent.putExtra(Constants.UPDATE_COUNT, updates.size());
            sendBroadcast(updateIntent);

            return updates;
        }

        private JSONObject buildUpdateRequest(int updateType) throws JSONException {
            JSONArray channels = new JSONArray();
            channels.put("stable");
            channels.put("snapshot");
            if (updateType == Constants.UPDATE_TYPE_NEW_NIGHTLY
                    || updateType == Constants.UPDATE_TYPE_ALL_NIGHTLY) {
                channels.put("nightly");
            }

            JSONObject params = new JSONObject();
            params.put("device", TESTING_DOWNLOAD ? "cmtestdevice" : Utils.getDeviceType());
            params.put("channels", channels);

            JSONObject request = new JSONObject();
            request.put("method", "get_all_builds");
            request.put("params", params);

            return request;
        }

        private LinkedList<UpdateInfo> parseJSON(String jsonString, int updateType,
                HashMap<String, UpdateInfo> knownUpdates) {
            LinkedList<UpdateInfo> updates = new LinkedList<UpdateInfo>();
            try {
                JSONObject result = new JSONObject(jsonString);
                JSONArray updateList = result.getJSONArray("result");
                int length = updateList.length();

                Log.d(TAG, "Got update JSON data with " + length + " entries");

                for (int i = 0; i < length; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    if (updateList.isNull(i)) {
                        continue;
                    }
                    JSONObject item = updateList.getJSONObject(i);
                    UpdateInfo info = parseUpdateJSONObject(item, updateType, knownUpdates);
                    if (info != null) {
                        updates.add(info);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error in JSON result", e);
            }
            return updates;
        }

        private UpdateInfo parseUpdateJSONObject(JSONObject obj, int updateType,
                HashMap<String, UpdateInfo> knownUpdates) throws JSONException {
            String fileName = obj.getString("filename");
            String url = obj.getString("url");
            String md5 = obj.getString("md5sum");
            String typeString = obj.getString("channel");
            UpdateInfo.Type type;
            long timestamp;

            try {
                String timestampString = obj.getString("timestamp");
                timestamp = Long.parseLong(timestampString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse build timestamp", e);
                throw new JSONException("Invalid build timestamp");
            }

            if (TextUtils.equals(typeString, "stable")) {
                type = UpdateInfo.Type.STABLE;
            } else if (TextUtils.equals(typeString, "snapshot")) {
                type = UpdateInfo.Type.SNAPSHOT;
            } else if (TextUtils.equals(typeString, "nightly")) {
                type = UpdateInfo.Type.NIGHTLY;
            } else {
                type = UpdateInfo.Type.UNKNOWN;
            }

            UpdateInfo ui = new UpdateInfo(fileName, timestamp, url, md5, type, null);
            boolean includeAll = updateType == Constants.UPDATE_TYPE_ALL_STABLE
                    || updateType == Constants.UPDATE_TYPE_ALL_NIGHTLY;

            if (!includeAll && !isNewerThanInstalled(ui.getVersion(), ui.getDate())) {
                Log.d(TAG, "Build " + fileName + " is older than the installed build");
                return null;
            }

            // fetch change log after checking whether to include this build to
            // avoid useless network traffic

            UpdateInfo known = knownUpdates.get(fileName);
            if (known != null && known.getChangeLog() != null && TextUtils.equals(md5, known.getMD5Sum())) {
                // no need to re-fetch change log if we know the build
                ui.setChangeLog(known.getChangeLog());
            } else {
                fetchChangeLog(ui, obj.getString("changes"));
            }

            return ui;
        }

        private void fetchChangeLog(UpdateInfo info, String url) {
            Log.d(TAG, "Getting change log for " + info + ", url " + url);

            HttpClient httpClient = new DefaultHttpClient();
            BufferedReader reader = null;

            try {
                HttpGet request = new HttpGet(URI.create(url));
                addRequestHeaders(request);

                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = null;

                if (!isCancelled() && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    entity = response.getEntity();
                }

                if (entity != null) {
                    reader = new BufferedReader(new InputStreamReader(entity.getContent()), 2 * 1024);
                    StringBuilder changeLogBuilder = new StringBuilder();
                    String line;
                    boolean categoryMatch = false;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (isCancelled()) {
                            break;
                        }
                        if (line.isEmpty()) {
                            continue;
                        }

                        if (line.startsWith("=")) {
                            categoryMatch = !categoryMatch;
                        } else if (categoryMatch) {
                            if (changeLogBuilder.length() != 0) {
                                changeLogBuilder.append("<br />");
                            }
                            changeLogBuilder.append("<b><u>");
                            changeLogBuilder.append(line);
                            changeLogBuilder.append("</u></b>").append("<br />");
                        } else if (line.startsWith("*")) {
                            changeLogBuilder.append("<br /><b>");
                            changeLogBuilder.append(line.replaceAll("\\*", ""));
                            changeLogBuilder.append("</b>").append("<br />");
                        } else {
                            changeLogBuilder.append("&#8226;&nbsp;").append(line).append("<br />");
                        }
                    }
                    info.setChangeLog(changeLogBuilder.toString());
                } else {
                    info.setChangeLog("");
                }
            } catch (IOException e) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // ignore, not much we can do anyway
                    }
                }
            }
        }

        private int[] canonicalizeVersion(String versionString) {
            String[] parts = versionString.split("\\.");
            int[] version = new int[parts.length];

            for (int i = 0; i < parts.length; i++) {
                try {
                    version[i] = Integer.valueOf(parts[i]);
                } catch (NumberFormatException e) {
                    version[i] = 0;
                }
            }

            return version;
        }

        private boolean isNewerThanInstalled(String version, long date) {
            int[] currentVersion = canonicalizeVersion(Utils.getInstalledVersion(false));
            int[] newVersion = canonicalizeVersion(version);

            if (currentVersion.length < newVersion.length) {
                currentVersion = Arrays.copyOf(currentVersion, newVersion.length);
            } else if (currentVersion.length > newVersion.length) {
                newVersion = Arrays.copyOf(newVersion, currentVersion.length);
            }

            for (int i = 0; i < newVersion.length; i++) {
                if (newVersion[i] > currentVersion[i]) {
                    return true;
                }
            }

            // The jenkins timestamp is for build completion, not the actual build.date prop
            return date > Utils.getInstalledBuildDate() + 3600;
        }
    }
}
