/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdateApplication;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.receiver.DownloadReceiver;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class UpdateCheckService extends IntentService {
    private static final String TAG = "UpdateCheckService";

    // Set this to true if the update service should check for smaller, test updates
    // This is for internal testing only
    private static final boolean TESTING_DOWNLOAD = false;

    // request actions
    public static final String ACTION_CHECK = "com.cyanogenmod.cmupdater.action.CHECK";
    public static final String ACTION_CANCEL_CHECK = "com.cyanogenmod.cmupdater.action.CANCEL_CHECK";

    // broadcast actions
    public static final String ACTION_CHECK_FINISHED = "com.cyanogenmod.cmupdater.action.UPDATE_CHECK_FINISHED";
    // extra for ACTION_CHECK_FINISHED: total amount of found updates
    public static final String EXTRA_UPDATE_COUNT = "update_count";
    // extra for ACTION_CHECK_FINISHED: amount of updates that are newer than what is installed
    public static final String EXTRA_REAL_UPDATE_COUNT = "real_update_count";
    // extra for ACTION_CHECK_FINISHED: amount of updates that were found for the first time
    public static final String EXTRA_NEW_UPDATE_COUNT = "new_update_count";

    // max. number of updates listed in the expanded notification
    private static final int EXPANDED_NOTIF_UPDATE_COUNT = 4;

    private HttpRequestExecutor mHttpExecutor;

    public UpdateCheckService() {
        super("UpdateCheckService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_CHECK)) {
            synchronized (this) {
                mHttpExecutor.abort();
            }

            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        synchronized (this) {
            mHttpExecutor = new HttpRequestExecutor();
        }

        if (!Utils.isOnline(this)) {
            // Only check for updates if the device is actually connected to a network
            Log.i(TAG, "Could not check for updates. Not connected to the network.");
            return;
        }

        // Start the update check
        Intent finishedIntent = new Intent(ACTION_CHECK_FINISHED);
        LinkedList<UpdateInfo> availableUpdates;
        try {
            availableUpdates = getAvailableUpdatesAndFillIntent(finishedIntent);
        } catch (IOException e) {
            Log.e(TAG, "Could not check for updates", e);
            availableUpdates = null;
        }

        if (availableUpdates == null || mHttpExecutor.isAborted()) {
            return;
        }

        // Store the last update check time and ensure boot check completed is true
        Date d = new Date();
        PreferenceManager.getDefaultSharedPreferences(UpdateCheckService.this).edit()
                .putLong(Constants.LAST_UPDATE_CHECK_PREF, d.getTime())
                .putBoolean(Constants.BOOT_CHECK_COMPLETED, true)
                .apply();

        int realUpdateCount = finishedIntent.getIntExtra(EXTRA_REAL_UPDATE_COUNT, 0);
        UpdateApplication app = (UpdateApplication) getApplicationContext();

        // Write to log
        Log.i(TAG, "The update check successfully completed at " + d + " and found "
                + availableUpdates.size() + " updates ("
                + realUpdateCount + " newer than installed)");

        if (realUpdateCount != 0 && !app.isMainActivityActive()) {
            // There are updates available
            // The notification should launch the main app
            Intent i = new Intent(this, UpdatesSettings.class);
            i.putExtra(UpdatesSettings.EXTRA_UPDATE_LIST_UPDATED, true);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_ONE_SHOT);

            Resources res = getResources();
            String text = res.getQuantityString(R.plurals.not_new_updates_found_body,
                    realUpdateCount, realUpdateCount);

            // Get the notification ready
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.cm_updater)
                    .setWhen(System.currentTimeMillis())
                    .setTicker(res.getString(R.string.not_new_updates_found_ticker))
                    .setContentTitle(res.getString(R.string.not_new_updates_found_title))
                    .setContentText(text)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true);

            LinkedList<UpdateInfo> realUpdates = new LinkedList<UpdateInfo>();
            for (UpdateInfo ui : availableUpdates) {
                if (ui.isNewerThanInstalled()) {
                    realUpdates.add(ui);
                }
            }

            Collections.sort(realUpdates, new Comparator<UpdateInfo>() {
                @Override
                public int compare(UpdateInfo lhs, UpdateInfo rhs) {
                    /* sort by date descending */
                    long lhsDate = lhs.getDate();
                    long rhsDate = rhs.getDate();
                    if (lhsDate == rhsDate) {
                        return 0;
                    }
                    return lhsDate < rhsDate ? 1 : -1;
                }
            });

            Notification.InboxStyle inbox = new Notification.InboxStyle(builder)
                    .setBigContentTitle(text);
            int added = 0, count = realUpdates.size();

            for (UpdateInfo ui : realUpdates) {
                if (added < EXPANDED_NOTIF_UPDATE_COUNT) {
                    inbox.addLine(ui.getName());
                    added++;
                }
            }
            if (added != count) {
                inbox.setSummaryText(res.getString(R.string.not_additional_count, count - added));
            }
            builder.setStyle(inbox);
            builder.setNumber(availableUpdates.size());

            if (count == 1) {
                i = new Intent(this, DownloadReceiver.class);
                i.setAction(DownloadReceiver.ACTION_START_DOWNLOAD);
                i.putExtra(DownloadReceiver.EXTRA_UPDATE_INFO, (Parcelable) realUpdates.getFirst());
                PendingIntent downloadIntent = PendingIntent.getBroadcast(this, 0, i,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

                builder.addAction(R.drawable.ic_tab_download,
                        res.getString(R.string.not_action_download), downloadIntent);
            }

            // Trigger the notification
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(R.string.not_new_updates_found_title, builder.build());
        }

        sendBroadcast(finishedIntent);
    }

    private void addRequestHeaders(HttpRequestBase request) {
        String userAgent = Utils.getUserAgentString(this);
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        request.addHeader("Cache-Control", "no-cache");
    }

    private LinkedList<UpdateInfo> getAvailableUpdatesAndFillIntent(Intent intent) throws IOException {
        // Get the type of update we should check for
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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

        HttpEntity entity = mHttpExecutor.execute(request);
        if (entity == null || mHttpExecutor.isAborted()) {
            return null;
        }

        LinkedList<UpdateInfo> lastUpdates = State.loadState(this);
        HashMap<String, UpdateInfo> knownUpdates = new HashMap<String, UpdateInfo>();
        for (UpdateInfo ui : lastUpdates) {
            knownUpdates.put(ui.getFileName(), ui);
        }

        // Read the ROM Infos
        String json = EntityUtils.toString(entity, "UTF-8");
        LinkedList<UpdateInfo> updates = parseJSON(json, updateType, knownUpdates);

        if (mHttpExecutor.isAborted()) {
            return null;
        }

        int newUpdates = 0, realUpdates = 0;
        for (UpdateInfo ui : updates) {
            if (!lastUpdates.contains(ui)) {
                newUpdates++;
            }
            if (ui.isNewerThanInstalled()) {
                realUpdates++;
            }
        }

        intent.putExtra(EXTRA_UPDATE_COUNT, updates.size());
        intent.putExtra(EXTRA_REAL_UPDATE_COUNT, realUpdates);
        intent.putExtra(EXTRA_NEW_UPDATE_COUNT, newUpdates);

        State.saveState(this, updates);

        return updates;
    }

    private JSONObject buildUpdateRequest(int updateType) throws JSONException {
        JSONArray channels = new JSONArray();
        channels.put("stable");
        channels.put("snapshot");
        channels.put("RC");
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
                if (mHttpExecutor.isAborted()) {
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
        int apiLevel = obj.getInt("api_level");
        long timestamp = obj.getLong("timestamp");
        String typeString = obj.getString("channel");
        UpdateInfo.Type type;

        if (TextUtils.equals(typeString, "stable")) {
            type = UpdateInfo.Type.STABLE;
        } else if (TextUtils.equals(typeString, "RC")) {
            type = UpdateInfo.Type.RC;
        } else if (TextUtils.equals(typeString, "snapshot")) {
            type = UpdateInfo.Type.SNAPSHOT;
        } else if (TextUtils.equals(typeString, "nightly")) {
            type = UpdateInfo.Type.NIGHTLY;
        } else {
            type = UpdateInfo.Type.UNKNOWN;
        }

        UpdateInfo ui = new UpdateInfo(fileName, timestamp, apiLevel, url, md5, type, null);
        boolean includeAll = updateType == Constants.UPDATE_TYPE_ALL_STABLE
            || updateType == Constants.UPDATE_TYPE_ALL_NIGHTLY;

        if (!includeAll && !ui.isNewerThanInstalled()) {
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

        BufferedReader reader = null;

        try {
            HttpGet request = new HttpGet(URI.create(url));
            addRequestHeaders(request);

            HttpEntity entity = mHttpExecutor.execute(request);
            if (entity != null) {
                reader = new BufferedReader(new InputStreamReader(entity.getContent()), 2 * 1024);
                StringBuilder changeLogBuilder = new StringBuilder();
                String line;
                boolean categoryMatch = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (mHttpExecutor.isAborted()) {
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
            // leave the change log in info untouched in this case
            // (either it had a change log already, in which case it makes no sense to kill
            //  it; or - more likely - it was already null before)
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

    private static class HttpRequestExecutor {
        private HttpClient mHttpClient;
        private HttpRequestBase mRequest;
        private boolean mAborted;

        public HttpRequestExecutor() {
            mHttpClient = new DefaultHttpClient();
            mAborted = false;
        }

        public HttpEntity execute(HttpRequestBase request) throws IOException {
            synchronized (this) {
                mAborted = false;
                mRequest = request;
            }

            HttpResponse response = mHttpClient.execute(request);
            HttpEntity entity = null;

            if (!mAborted && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
            }

            synchronized (this) {
                mRequest = null;
            }

            return entity;
        }

        public synchronized void abort() {
            if (mRequest != null) {
                mRequest.abort();
            }
            mAborted = true;
        }

        public synchronized boolean isAborted() {
            return mAborted;
        }
    }
}
