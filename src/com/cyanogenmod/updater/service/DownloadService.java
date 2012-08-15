package com.cyanogenmod.updater.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.*;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.cyanogenmod.updater.customExceptions.NotEnoughSpaceException;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.interfaces.IDownloadService;
import com.cyanogenmod.updater.interfaces.IDownloadServiceCallback;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.ui.ApplyUpdateActivity;
import com.cyanogenmod.updater.ui.DownloadActivity;
import com.cyanogenmod.updater.ui.MainActivity;
import com.cyanogenmod.updater.ui.R;
import com.cyanogenmod.updater.utils.MD5;
import com.cyanogenmod.updater.utils.Preferences;
import com.cyanogenmod.updater.utils.SysUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    private Boolean showDebugOutput = false;

    private final RemoteCallbackList<IDownloadServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadServiceCallback>();

    private boolean prepareForDownloadCancel;
    private boolean mMirrorNameUpdated;
    private String mMirrorName;
    private boolean mDownloading = false;
    private UpdateInfo mCurrentUpdate;
    private WifiLock mWifiLock;
    private volatile long mtotalDownloaded;
    private int mcontentLength;
    private long mStartTime;
    private String minutesString;
    private String secondsString;
    private String fullUpdateFolderPath;
    private Resources res;
    private long localFileSize = 0;
    private Preferences prefs;

    @Override
        public IBinder onBind(Intent arg0) {
            return mBinder;
        }

    private final IDownloadService.Stub mBinder = new IDownloadService.Stub() {
        public void Download(UpdateInfo ui) throws RemoteException {
            mDownloading = true;
            boolean success = checkForConnectionAndUpdate(ui);
            notifyUser(ui, success);
            mDownloading = false;
        }

        public boolean DownloadRunning() throws RemoteException {
            return mDownloading;
        }

        public void PauseDownload() throws RemoteException {
            //TODO: Pause Download
            stopDownload();
        }

        public void cancelDownload() throws RemoteException {
            cancelCurrentDownload();
        }

        public UpdateInfo getCurrentUpdate() throws RemoteException {
            return mCurrentUpdate;
        }

        public String getCurrentMirrorName() throws RemoteException {
            return mMirrorName;
        }

        public void registerCallback(IDownloadServiceCallback cb)
            throws RemoteException {
                if (cb != null) mCallbacks.register(cb);

            }
        public void unregisterCallback(IDownloadServiceCallback cb)
            throws RemoteException {
                if (cb != null) mCallbacks.unregister(cb);

            }
    };

    @Override
        public void onCreate() {
            if (showDebugOutput) Log.d(TAG, "Download Service Created");

            prefs = new Preferences(this);

            showDebugOutput = prefs.displayDebugOutput();

            mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock("CM Updater");

            fullUpdateFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefs.getUpdateFolder();

            res = getResources();
            minutesString = res.getString(R.string.minutes);
            secondsString = res.getString(R.string.seconds);
        }

    @Override
        public void onDestroy() {
            mCallbacks.kill();
            super.onDestroy();
        }

    private boolean checkForConnectionAndUpdate(UpdateInfo updateToDownload) {
        if (showDebugOutput) Log.d(TAG, "Called CheckForConnectionAndUpdate");
        mCurrentUpdate = updateToDownload;

        boolean success;
        mWifiLock.acquire();

        try {
            if (showDebugOutput) Log.d(TAG, "Downloading update...");
            success = downloadFile(updateToDownload);
        }
        catch (RuntimeException ex) {
            Log.e(TAG, "RuntimeEx while checking for updates", ex);
            notificateDownloadError(ex.getMessage());
            return false;
        }
        finally {
            mWifiLock.release();
        }
        //Be sure to return false if the User canceled the Download
        return !prepareForDownloadCancel && success;
    }

    private boolean downloadFile(UpdateInfo updateInfo) {
        if (showDebugOutput) Log.d(TAG, "Called downloadFile");
        HttpClient httpClient = new DefaultHttpClient();
        HttpClient MD5httpClient = new DefaultHttpClient();

        HttpUriRequest req;
        HttpResponse response;

        File destinationFile;
        File partialDestinationFile;
        File destinationMD5File;
        String downloadedMD5 = null;

        //If directory not exists, create it
        File directory = new File(fullUpdateFolderPath);
        if (!directory.exists()) {
            directory.mkdirs();
            if (showDebugOutput) Log.d(TAG, "UpdateFolder created");
        }

        String fileName = updateInfo.getFileName();
        if (null == fileName || fileName.length() < 1) {
            fileName = "update.zip";
        }
        if (showDebugOutput) Log.d(TAG, "fileName: " + fileName);

        //Set the Filename to update.zip.partial
        partialDestinationFile = new File(fullUpdateFolderPath, fileName + ".partial");
        destinationFile = new File(fullUpdateFolderPath, fileName);
        if (partialDestinationFile.exists())
            localFileSize = partialDestinationFile.length(); 

        //For every Mirror
        if (!prepareForDownloadCancel) {
            try {
                req = new HttpGet(URI.create(updateInfo.getDownloadUrl()));

                // Add no-cache Header, so the File gets downloaded each time
                req.addHeader("Cache-Control", "no-cache");

                if (showDebugOutput) Log.d(TAG, "Trying to download update zip from " + req.getURI());

                if (localFileSize > 0) {
                    if (showDebugOutput) Log.d(TAG, "localFileSize for Resume: " + localFileSize);
                    req.addHeader("Range", "bytes=" + localFileSize + "-");
                }
                response = httpClient.execute(req);

                int serverResponse = response.getStatusLine().getStatusCode();

                if (serverResponse == HttpStatus.SC_NOT_FOUND) {
                    if (showDebugOutput) Log.d(TAG, "File not found on Server. Trying next one.");
                } else if (serverResponse != HttpStatus.SC_OK && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {
                    if (showDebugOutput)
                        Log.d(TAG, "Server returned status code " + serverResponse + " for update zip trying next mirror");
                } else {
                    // server must support partial content for resume
                    if (localFileSize > 0 && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {
                        if (showDebugOutput) Log.d(TAG, "Resume not supported");
                        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_not_supported, 0));
                        //To get the UdpateProgressBar working correctly, when server does not support resume
                        localFileSize = 0;
                    } else if (localFileSize > 0 && serverResponse == HttpStatus.SC_PARTIAL_CONTENT) {
                        if (showDebugOutput) Log.d(TAG, "Resume supported");
                        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_download, 0));
                    }

                    downloadedMD5 = updateInfo.getMD5();
                    if (showDebugOutput) Log.d(TAG, "MD5: " + downloadedMD5);

                    // Download Update ZIP
                    HttpEntity entity = response.getEntity();
                    dumpFile(entity, partialDestinationFile, destinationFile);
                    //Was the download canceled?
                    if (prepareForDownloadCancel) {
                        if (showDebugOutput) Log.d(TAG, "Download was canceled. Break the for loop");
                    }
                    if (entity != null && !prepareForDownloadCancel) {
                        if (showDebugOutput) Log.d(TAG, "Consuming entity....");
                        entity.consumeContent();
                        if (showDebugOutput) Log.d(TAG, "Entity consumed");
                    } else {
                        if (showDebugOutput) Log.d(TAG, "Entity resetted to NULL");
                        entity = null;
                    }
                    if (showDebugOutput) Log.d(TAG, "Update download finished");

                    if (showDebugOutput) Log.d(TAG, "Performing MD5 verification");
                    if (!MD5.checkMD5(downloadedMD5, destinationFile)) {
                        throw new IOException(res.getString(R.string.md5_verification_failed));
                    }

                    //If we reach here, download & MD5 check went fine :)
                    return true;
                }
            }
            catch (IOException ex) {
                ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
                Log.e(TAG, "An error occured while downloading the update file.", ex);
            }
            catch (NotEnoughSpaceException ex) {
                ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
                Log.e(TAG, "Not enough Space on SDCard to download the Update");
                return false;
            }
        }
        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
        if (showDebugOutput) Log.d(TAG, "Unable to download the update file from any mirror");
        return false;
    }

    private void dumpFile(HttpEntity entity, File partialDestinationFile, File destinationFile) throws IOException, NotEnoughSpaceException {
        if (showDebugOutput) Log.d(TAG, "DumpFile Called");
        if (!prepareForDownloadCancel) {
            mcontentLength = (int) entity.getContentLength();
            if (mcontentLength <= 0) {
                if (showDebugOutput) Log.d(TAG, "unable to determine the update file size, Set ContentLength to 1024");
                mcontentLength = 1024;
            } else if (showDebugOutput) Log.d(TAG, "Update size: " + (mcontentLength / 1024) + "KB");

            //Check if there is enough Space on SDCard for Downloading the Update
            if (!SysUtils.EnoughSpaceOnSdCard(mcontentLength))
                throw new NotEnoughSpaceException(res.getString(R.string.download_not_enough_space));

            mStartTime = System.currentTimeMillis(); 

            byte[] buff = new byte[64 * 1024];
            int read;
            RandomAccessFile out = new RandomAccessFile(partialDestinationFile, "rw");
            out.seek(localFileSize);
            InputStream is = entity.getContent();
            TimerTask progressUpdateTimerTask = new TimerTask() {
                @Override
                    public void run() {
                        onProgressUpdate();
                    }
            };
            Timer progressUpdateTimer = new Timer();
            try {
                //If File exists, set the Progress to it. Otherwise it will be initial 0
                mtotalDownloaded = localFileSize;
                progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask, 100, prefs.getProgressUpdateFreq());
                while ((read = is.read(buff)) > 0 && !prepareForDownloadCancel) {
                    out.write(buff, 0, read);
                    mtotalDownloaded += read;
                }
                out.close();
                is.close();
                if (!prepareForDownloadCancel) {
                    partialDestinationFile.renameTo(destinationFile);
                    if (showDebugOutput) Log.d(TAG, "Download finished");
                } else {
                    if (showDebugOutput) Log.d(TAG, "Download cancelled");
                }
            }
            catch (IOException e) {
                out.close();
                try {
                    destinationFile.delete();
                }
                catch (SecurityException ex) {
                    Log.e(TAG, "Unable to delete downloaded File. Continue anyway.", ex);
                }
            }
            finally {
                progressUpdateTimer.cancel();
                buff = null;
            }
        } else if (showDebugOutput) Log.d(TAG, "Download Cancel in Progress. Don't start Downloading");
    }

    private void onProgressUpdate() {
        //Only update the Notification and DownloadLayout, when no downloadcancel is in progress, so the notification will not pop up again
        if (!prepareForDownloadCancel) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification mNotification = new Notification(android.R.drawable.ic_popup_sync, res.getString(R.string.notification_tickertext), System.currentTimeMillis());
            mNotification.flags = Notification.FLAG_NO_CLEAR;
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;
            RemoteViews mNotificationRemoteView = new RemoteViews(getPackageName(), R.layout.notification);
            Intent mNotificationIntent = new Intent(this, DownloadActivity.class);
            mNotificationIntent.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) mCurrentUpdate);
            PendingIntent mNotificationContentIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotification.contentView = mNotificationRemoteView;
            mNotification.contentIntent = mNotificationContentIntent;

            //lcoalFileSize because the contentLength will only be the missing bytes and not the whole file
            long contentLengthOfFullDownload = mcontentLength + localFileSize;
            long speed = ((mtotalDownloaded - localFileSize) / (System.currentTimeMillis() - mStartTime));
            speed = (speed > 0) ? speed : 1;
            long remainingTime = ((contentLengthOfFullDownload - mtotalDownloaded) / speed);
            String stringDownloaded = mtotalDownloaded / 1048576 + "/" + contentLengthOfFullDownload / 1048576 + " MB";
            String stringSpeed = speed + " kB/s";
            String stringRemainingTime = remainingTime / 60000 + " " + minutesString + " " + remainingTime % 60 + " " + secondsString;
            String stringComplete = stringDownloaded + " " + stringSpeed + " " + stringRemainingTime;
            mNotificationRemoteView.setTextViewText(R.id.notificationTextDownloadInfos, stringComplete);
            mNotificationRemoteView.setProgressBar(R.id.notificationProgressBar, (int) contentLengthOfFullDownload, (int) mtotalDownloaded, false);
            mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_STATUS, mNotification);

            if (!mMirrorNameUpdated) {
                UpdateDownloadMirror(mMirrorName);
                mMirrorNameUpdated = true;
            }
            //Update the DownloadProgress
            UpdateDownloadProgress(mtotalDownloaded, (int) contentLengthOfFullDownload, stringDownloaded, stringSpeed, stringRemainingTime);
        } else if (showDebugOutput)
            Log.d(TAG, "Downloadcancel in Progress. Not updating the Notification and DownloadLayout");
    }

    private void notifyUser(UpdateInfo ui, boolean success) {
        if (showDebugOutput) Log.d(TAG, "Called Notify User");
        Intent i;

        if (!success) {
            if (showDebugOutput) Log.d(TAG, "Downloaded Update was NULL");
            DeleteDownloadStatusNotification();
            DownloadError();
            stopSelf();
            return;
        }

        i = new Intent(this, ApplyUpdateActivity.class);
        i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) ui);

        //Set the Notification to finished
        DeleteDownloadStatusNotification();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification mNotification = new Notification(R.drawable.icon, res.getString(R.string.notification_finished), System.currentTimeMillis());
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
        PendingIntent mNotificationContentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mNotification.setLatestEventInfo(this, res.getString(R.string.app_name), res.getString(R.string.notification_finished), mNotificationContentIntent);
        Uri notificationRingtone = prefs.getConfiguredRingtone();
        if (prefs.getVibrate())
            mNotification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
        else
            mNotification.defaults = Notification.DEFAULT_LIGHTS;
        if (notificationRingtone == null) {
            mNotification.sound = null;
        } else {
            mNotification.sound = notificationRingtone;
        }
        mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINISHED, mNotification);

        DownloadFinished();
    }

    private void notificateDownloadError(String ExceptionText) {
        mDownloading = false;
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_ONE_SHOT);
        Notification notification = new Notification(android.R.drawable.stat_notify_error,
                res.getString(R.string.not_update_download_error_ticker),
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(
                this,
                res.getString(R.string.not_update_download_error_title),
                ExceptionText,
                contentIntent);
        Uri notificationRingtone = prefs.getConfiguredRingtone();
        if (prefs.getVibrate())
            notification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
        else
            notification.defaults = Notification.DEFAULT_LIGHTS;
        if (notificationRingtone == null) {
            notification.sound = null;
        } else {
            notification.sound = notificationRingtone;
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(R.string.not_update_download_error_title, notification);
        if (showDebugOutput) Log.d(TAG, "Download Error");
        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.exception_while_downloading, 0));
    }

    private void DeleteDownloadStatusNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD_STATUS);
        if (showDebugOutput) Log.d(TAG, "Download Notification removed");
    }

    private void UpdateDownloadProgress(final long downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText) {
        final int N = mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mCallbacks.getBroadcastItem(i).updateDownloadProgress(downloaded, total, downloadedText, speedText, remainingTimeText);
            }
            catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void UpdateDownloadMirror(String mirrorName) {
        final int M = mCallbacks.beginBroadcast();
        for (int i = 0; i < M; i++) {
            try {
                mCallbacks.getBroadcastItem(i).UpdateDownloadMirror(mirrorName);
            }
            catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void DownloadFinished() {
        final int M = mCallbacks.beginBroadcast();
        for (int i = 0; i < M; i++) {
            try {
                mCallbacks.getBroadcastItem(i).DownloadFinished(mCurrentUpdate);
            }
            catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void DownloadError() {
        final int M = mCallbacks.beginBroadcast();
        for (int i = 0; i < M; i++) {
            try {
                mCallbacks.getBroadcastItem(i).DownloadError();
            }
            catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void cancelCurrentDownload() {
        prepareForDownloadCancel = true;
        if (showDebugOutput) Log.d(TAG, "Download Service CancelDownload was called");
        DeleteDownloadStatusNotification();
        File update = new File(fullUpdateFolderPath + "/" + mCurrentUpdate.getFileName());
        if (update.exists()) {
            update.delete();
            if (showDebugOutput) Log.d(TAG, update.getAbsolutePath() + " deleted");
        }
        mDownloading = false;
        if (showDebugOutput) Log.d(TAG, "Download Cancel StopSelf was called");
        stopSelf();
    }

    private void stopDownload() {
        //TODO: Pause download
        prepareForDownloadCancel = true;
        mDownloading = false;
        stopSelf();
    }

    private final Handler ToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0)
                Toast.makeText(DownloadService.this, msg.arg1, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(DownloadService.this, (String) msg.obj, Toast.LENGTH_LONG).show();
        }
    };
}
