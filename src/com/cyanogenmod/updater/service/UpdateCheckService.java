package com.cyanogenmod.updater.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import com.cyanogenmod.updater.customTypes.FullUpdateInfo;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.interfaces.IUpdateCheckService;
import com.cyanogenmod.updater.interfaces.IUpdateCheckServiceCallback;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.misc.State;
import com.cyanogenmod.updater.ui.MainActivity;
import com.cyanogenmod.updater.ui.R;
import com.cyanogenmod.updater.utils.Preferences;
import com.cyanogenmod.updater.utils.StringUtils;
import com.cyanogenmod.updater.utils.SysUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;

public class UpdateCheckService extends Service {
    private static final String TAG = "UpdateCheckService";

    private static Boolean showDebugOutput = false;

    private final RemoteCallbackList<IUpdateCheckServiceCallback> mCallbacks = new RemoteCallbackList<IUpdateCheckServiceCallback>();
    private Preferences mPreferences;
    private String systemMod;
    private String systemRom;
    private Integer currentBuildDate;
    private boolean showNightlyRomUpdates;
    private boolean showAllRomUpdates;
    private boolean WildcardUsed = false;

    @Override
        public IBinder onBind(Intent intent) {
            return mBinder;
        }

    @Override
        public void onCreate() {
            mPreferences = new Preferences(this);
            showDebugOutput = mPreferences.displayDebugOutput();
            systemMod = mPreferences.getBoardString();
            if (systemMod == null) {
                if (showDebugOutput)
                    Log.d(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
            } else {
                if (showDebugOutput) Log.d(TAG, "System's Mod version:" + systemMod);
            }
        }

    @Override
        public void onDestroy() {
            mCallbacks.kill();
            super.onDestroy();
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

    private void DisplayExceptionToast(String ex) {
        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex));
    }

    private void checkForNewUpdates() {
        FullUpdateInfo availableUpdates;
        while (true) {
            try {
                if (showDebugOutput) Log.d(TAG, "Checking for updates...");
                availableUpdates = getAvailableUpdates();
                break;
            }
            catch (IOException ex) {
                Log.e(TAG, "IOEx while checking for updates", ex);
                notificateCheckError(ex.getMessage());
                return;
            }
            catch (RuntimeException ex) {
                Log.e(TAG, "RuntimeEx while checking for updates", ex);
                notificateCheckError(ex.getMessage());
                return;
            }
        }

        mPreferences.setLastUpdateCheck(new Date());

        int updateCountRoms = availableUpdates.getRomCount();
        int updateCount = availableUpdates.getUpdateCount();
        if (showDebugOutput) Log.d(TAG, updateCountRoms + " ROM update(s) found; ");

        if (updateCountRoms == 0) {
            if (showDebugOutput) Log.d(TAG, "No updates found");
            ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.no_updates_found, 0));
            FinishUpdateCheck();
        } else {
            if (mPreferences.notificationsEnabled()) {
                Intent i = new Intent(this, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);

                Resources res = getResources();
                Notification notification = new Notification(android.R.drawable.ic_popup_sync,
                        res.getString(R.string.not_new_updates_found_ticker),
                        System.currentTimeMillis());

                //To remove the Notification, when the User clicks on it
                notification.flags = Notification.FLAG_AUTO_CANCEL;

                String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);
                notification.setLatestEventInfo(this, res.getString(R.string.not_new_updates_found_title), text, contentIntent);

                Uri notificationRingtone = mPreferences.getConfiguredRingtone();
                if (mPreferences.getVibrate())
                    notification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
                else
                    notification.defaults = Notification.DEFAULT_LIGHTS;
                notification.sound = notificationRingtone;

                //Use a resourceId as an unique identifier
                ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(R.string.not_new_updates_found_title, notification);
            }
            FinishUpdateCheck();
        }
    }

    private void notificateCheckError(String ExceptionText) {
        DisplayExceptionToast(ExceptionText);
        if (showDebugOutput) Log.d(TAG, "Update check error");
        FinishUpdateCheck();
    }

    private FullUpdateInfo getAvailableUpdates() throws IOException {
        FullUpdateInfo retValue = new FullUpdateInfo();
        boolean romException = false;
        HttpClient romHttpClient = new DefaultHttpClient();
        HttpEntity romResponseEntity = null;
        systemRom = SysUtils.getModVersion();
        currentBuildDate = new Integer(SysUtils.getSystemProperty(Customization.BUILD_DATE));
        showNightlyRomUpdates = mPreferences.showNightlyRomUpdates();
        showAllRomUpdates = mPreferences.showAllRomUpdates();
        //Get the actual Rom Updateserver URL
        try {
            URI RomUpdateServerUri = URI.create(getResources().getString(R.string.conf_update_server_url_def));
            HttpPost romReq = new HttpPost(RomUpdateServerUri);
            String getcmRequest = "{\"method\": \"get_builds\", \"params\":{\"device\":\""+systemMod+"\", \"channels\": [\"nightly\",\"stable\"]}}";
            romReq.setEntity(new ByteArrayEntity(getcmRequest.getBytes()));
            romReq.addHeader("Cache-Control", "no-cache");
            HttpResponse romResponse = romHttpClient.execute(romReq);
            int romServerResponse = romResponse.getStatusLine().getStatusCode();
            if (romServerResponse != HttpStatus.SC_OK) {
                if (showDebugOutput) Log.d(TAG, "Server returned status code for ROM " + romServerResponse);
                romException = true;
            }
            if (!romException)
                romResponseEntity = romResponse.getEntity();
        }
        catch (IllegalArgumentException e) {
            if (showDebugOutput) Log.d(TAG, "Rom Update request failed: " + e);
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
            } else if (showDebugOutput) Log.d(TAG, "There was an Exception on Downloading the Rom JSON File");
        }
        finally {
            if (romResponseEntity != null)
                romResponseEntity.consumeContent();
        }

        FullUpdateInfo ful = FilterUpdates(retValue, State.loadState(this, showDebugOutput));
        if (!romException)
            State.saveState(this, retValue, showDebugOutput);
        return ful;
    }

    private LinkedList<UpdateInfo> parseJSON(StringBuffer buf) {
        LinkedList<UpdateInfo> uis = new LinkedList<UpdateInfo>();

        JSONObject mainJSONObject;

        try {
            mainJSONObject = new JSONObject(buf.toString());
            JSONArray updateList = mainJSONObject.getJSONArray(Constants.JSON_UPDATE_LIST);
            if (showDebugOutput) Log.d(TAG, "Found " + updateList.length() + " updates in the JSON");
            for (int i = 0, max = updateList.length(); i < max; i++) {
                if (!updateList.isNull(i))
                    uis.add(parseUpdateJSONObject(updateList.getJSONObject(i)));
                else
                    Log.e(TAG, "Theres an error in your JSON File(update part). Maybe a , after the last update");
            }
        }
        catch (JSONException e) {
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

        }
        catch (JSONException e) {
            Log.e(TAG, "Error in JSON File: ", e);
        }
        return ui;
    }

    private boolean branchMatches(UpdateInfo ui, boolean nightlyAllowed) {
        if (ui == null) return false;

        boolean allow = false;

        if (ui.getBranchCode().equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_NIGHTLY)) {
            if (nightlyAllowed)
                allow = true;
        } else {
            allow = true;
        }
        return allow;
    }

    private LinkedList<UpdateInfo> getRomUpdates(LinkedList<UpdateInfo> updateInfos) {
        LinkedList<UpdateInfo> ret = new LinkedList<UpdateInfo>();
        for (int i = 0, max = updateInfos.size(); i < max; i++) {
            UpdateInfo ui = updateInfos.poll();
            if (showAllRomUpdates || StringUtils.compareVersions(ui.getVersion(), systemRom, ui.getDate(), currentBuildDate)) {
                if (branchMatches(ui, showNightlyRomUpdates)) {
                    if (showDebugOutput)
                        Log.d(TAG, "Adding Rom: " + ui.getName() + " Version: " + ui.getVersion() + " Filename: " + ui.getFileName());
                    ret.add(ui);
                } else {
                    if (showDebugOutput)
                        Log.d(TAG, "Discarding Rom " + ui.getName() + " (Branch mismatch - stable/nightly)");
                }
            } else {
                if (showDebugOutput) Log.d(TAG, "Discarding Rom " + ui.getName() + " (older version)");
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
        private static FullUpdateInfo FilterUpdates(FullUpdateInfo newList, FullUpdateInfo oldList) {
            if (showDebugOutput) Log.d(TAG, "Called FilterUpdates");
            if (showDebugOutput) Log.d(TAG, "newList Length: " + newList.getUpdateCount());
            if (showDebugOutput) Log.d(TAG, "oldList Length: " + oldList.getUpdateCount());
            FullUpdateInfo ful = new FullUpdateInfo();
            ful.roms = (LinkedList<UpdateInfo>) newList.roms.clone();
            ful.roms.removeAll(oldList.roms);
            if (showDebugOutput) Log.d(TAG, "fulList Length: " + ful.getUpdateCount());
            return ful;
        }

    private final Handler ToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0)
                Toast.makeText(UpdateCheckService.this, msg.arg1, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(UpdateCheckService.this, (String) msg.obj, Toast.LENGTH_LONG).show();
        }
    };

    private void FinishUpdateCheck() {
        final int M = mCallbacks.beginBroadcast();
        for (int i = 0; i < M; i++) {
            try {
                mCallbacks.getBroadcastItem(i).UpdateCheckFinished();
            }
            catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }
}
