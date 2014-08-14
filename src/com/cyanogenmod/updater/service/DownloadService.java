/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.receiver.DownloadReceiver;
import com.cyanogenmod.updater.utils.HttpRequestExecutor;
import com.cyanogenmod.updater.utils.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

public class DownloadService extends IntentService {
    private static final String TAG = DownloadService.class.getSimpleName();

    private static final String EXTRA_UPDATE_INFO = "update_info";

    private HttpRequestExecutor mHttpExecutor;
    private SharedPreferences mPrefs;

    public static void start(Context context, UpdateInfo ui) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(EXTRA_UPDATE_INFO, (Parcelable) ui);
        context.startService(intent);
    }

    public DownloadService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mHttpExecutor = new HttpRequestExecutor();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        UpdateInfo ui = intent.getParcelableExtra(EXTRA_UPDATE_INFO);

        try {
            getIncremental(ui);
        } catch (IOException e) {
            downloadFullZip(ui);
        }
    }

    private void getIncremental(UpdateInfo ui) throws IOException {
        String sourceIncremental = Utils.getIncremental();
        Log.d(TAG, "Looking for incremental ota for source=" + sourceIncremental + ", target="
                + ui.getIncremental());

        HttpPost request = buildRequest(sourceIncremental, ui);
        HttpEntity entity = mHttpExecutor.execute(request);
        if (entity == null || mHttpExecutor.isAborted()) {
            downloadFullZip(ui);
            return;
        }

        String json = EntityUtils.toString(entity, "UTF-8");
        UpdateInfo incrementalUpdateInfo = parseJSON(json, ui);

        if (incrementalUpdateInfo == null) {
            downloadFullZip(ui);
            return;
        } else {
            downloadIncremental(incrementalUpdateInfo, ui.getFileName());
            return;
        }
    }

    private String getServerUri() {
        String propertyUri = SystemProperties.get("cm.updater.uri");
        if (!TextUtils.isEmpty(propertyUri)) {
            return propertyUri;
        }

        return getString(R.string.conf_update_server_url_def);
    }

    private HttpPost buildRequest(String sourceIncremental, UpdateInfo ui) {
        URI requestUri = URI.create(getServerUri() + "/v1/build/get_delta");
        HttpPost request = new HttpPost(requestUri);

        // Add request headers
        String userAgent = Utils.getUserAgentString(this);
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        request.addHeader("Content-Type", "application/json");

        // Set request body
        try {
            JSONObject requestBody = buildRequestBody(sourceIncremental, ui);
            request.setEntity(new StringEntity(requestBody.toString()));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
            return null;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException", e);
            return null;
        }

        return request;
    }

    private JSONObject buildRequestBody(String sourceIncremental, UpdateInfo ui) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("source_incremental", sourceIncremental);
        body.put("target_incremental", ui.getIncremental());
        return body;
    }

    private UpdateInfo parseJSON(String json, UpdateInfo ui) {
        try {
            JSONObject obj = new JSONObject(json);

            if (obj.has("errors")) {
                return null;
            }

            UpdateInfo.Builder builder = new UpdateInfo.Builder();
            builder.setFileName(obj.getString("filename"));
            builder.setDownloadUrl(obj.getString("download_url"));
            builder.setMD5Sum(obj.getString("md5sum"));
            builder.setApiLevel(ui.getApiLevel());
            builder.setBuildDate(obj.getLong("date_created_unix"));
            builder.setType(UpdateInfo.Type.INCREMENTAL);
            builder.setIncremental(obj.getString("incremental"));
            return builder.build();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
            return null;
        }
    }

    private long enqueueDownload(String downloadUrl, String fullFilePath) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        String userAgent = Utils.getUserAgentString(this);
        if (userAgent != null) {
            request.addRequestHeader("User-Agent", userAgent);
        }
        request.setTitle(getString(R.string.app_name));
        request.setDestinationUri(Uri.parse(fullFilePath));
        request.setAllowedOverRoaming(false);
        request.setVisibleInDownloadsUi(false);

        // TODO: this could/should be made configurable
        request.setAllowedOverMetered(true);

        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        return dm.enqueue(request);
    }

    private void downloadIncremental(UpdateInfo ui, String originalName) {
        Log.v(TAG, "Downloading incremental zip: " + ui.getDownloadUrl());
        // If directory doesn't exist, create it
        File directory = Utils.makeUpdateFolder();
        if (!directory.exists()) {
            directory.mkdirs();
            Log.d(TAG, "UpdateFolder created");
        }

        // Build the name of the file to download, adding .partial at the end.  It will get
        // stripped off when the download completes
        String sourceIncremental = Utils.getIncremental();
        String targetIncremental = ui.getIncremental();
        String fileName = "incremental-" + sourceIncremental + "-" + targetIncremental + ".zip";
        String fullFilePath = "file://" + directory.getAbsolutePath() + "/" + fileName + ".partial";

        long downloadId = enqueueDownload(ui.getDownloadUrl(), fullFilePath);

        // Store in shared preferences
        mPrefs.edit()
                .putLong(Constants.DOWNLOAD_ID, downloadId)
                .putString(Constants.DOWNLOAD_MD5, ui.getMD5Sum())
                .putString(Constants.DOWNLOAD_INCREMENTAL_FOR, originalName)
                .apply();

        Utils.cancelNotification(this);

        Intent intent = new Intent(DownloadReceiver.ACTION_DOWNLOAD_STARTED);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
        sendBroadcast(intent);
    }

    private void downloadFullZip(UpdateInfo ui) {
        Log.v(TAG, "Downloading full zip");

        // If directory doesn't exist, create it
        File directory = Utils.makeUpdateFolder();
        if (!directory.exists()) {
            directory.mkdirs();
            Log.d(TAG, "UpdateFolder created");
        }

        // Build the name of the file to download, adding .partial at the end.  It will get
        // stripped off when the download completes
        String fullFilePath = "file://" + directory.getAbsolutePath() + "/" + ui.getFileName() + ".partial";

        long downloadId = enqueueDownload(ui.getDownloadUrl(), fullFilePath);

        // Store in shared preferences
        mPrefs.edit()
                .putLong(Constants.DOWNLOAD_ID, downloadId)
                .putString(Constants.DOWNLOAD_MD5, ui.getMD5Sum())
                .apply();

        Utils.cancelNotification(this);

        Intent intent = new Intent(DownloadReceiver.ACTION_DOWNLOAD_STARTED);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
        sendBroadcast(intent);
    }
}
