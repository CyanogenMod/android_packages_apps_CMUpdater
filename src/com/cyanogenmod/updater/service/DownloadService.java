/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.cyanogenmod.updater.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.ApplyUpdate;
import com.cyanogenmod.updater.DownloadUpdate;
import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.customExceptions.NotEnoughSpaceException;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.interfaces.IDownloadService;
import com.cyanogenmod.updater.interfaces.IDownloadServiceCallback;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.MD5;
import com.cyanogenmod.updater.utils.SysUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    boolean DEBUG = false;

    private final RemoteCallbackList<IDownloadServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadServiceCallback>();

    private boolean mPrepForDownloadCancel;
    private boolean mDownloading = false;
    private UpdateInfo mCurrentUpdate;
    private WifiLock mWifiLock;
    private volatile long mTotalDownloaded;
    private int mContentLength;
    private long mStartTime;
    private String mMinString;
    private String mSecString;
    private String mFullUpdateFolderPath;
    private long mLocalFileSize = 0;

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
        mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock("CM Updater");

        mFullUpdateFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cmupdater";

        Resources res = getResources();
        mMinString = res.getString(R.string.minutes);
        mSecString = res.getString(R.string.seconds);
    }

    @Override
    public void onDestroy() {
        mCallbacks.kill();
        super.onDestroy();
    }

    private boolean checkForConnectionAndUpdate(UpdateInfo updateToDownload) {
        mCurrentUpdate = updateToDownload;

        boolean success;
        mWifiLock.acquire();

        try {
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

        //Be sure to return false if the User cancelled the Download
        return !mPrepForDownloadCancel && success;
    }

    private boolean downloadFile(UpdateInfo updateInfo) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpClient MD5httpClient = new DefaultHttpClient();

        HttpUriRequest req;
        HttpResponse response;

        File destinationFile;
        File partialDestinationFile;
        File destinationMD5File;
        String downloadedMD5 = null;

        //If directory does not exist, create it
        if (DEBUG)
            Log.i(TAG, "Checking if the " + mFullUpdateFolderPath + " directory exists");
        File directory = new File(mFullUpdateFolderPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                if (DEBUG)
                    Log.i(TAG, "Directory did not exist, we created it");
            } else {
                Log.e(TAG, "Error creating directory");
                return false;
            }
        }

        // Get the name of the file to download
        String fileName = updateInfo.getFileName();
        if (null == fileName || fileName.length() < 1) {
            fileName = "update.zip";
        }

        //Set the Filename to update.zip.partial
        partialDestinationFile = new File(mFullUpdateFolderPath, fileName + ".partial");
        destinationFile = new File(mFullUpdateFolderPath, fileName);
        if (DEBUG)
            Log.i(TAG, "The files we are creating are " + partialDestinationFile + " and " + destinationFile);

        if (partialDestinationFile.exists()) {
            mLocalFileSize = partialDestinationFile.length();
            if (DEBUG)
                Log.i(TAG, "The " + partialDestinationFile + " file already exists with a length of " + mLocalFileSize);
        }

        //For every Mirror
        if (!mPrepForDownloadCancel) {
            try {
                req = new HttpGet(URI.create(updateInfo.getDownloadUrl()));

                // Add no-cache Header, so the File gets downloaded each time
                req.addHeader("Cache-Control", "no-cache");

                if (DEBUG)
                    Log.i(TAG, "Trying to download update zip from " + req.getURI());

                if (mLocalFileSize > 0) {
                    if (DEBUG)
                        Log.d(TAG, "localFileSize for Resume: " + mLocalFileSize);

                    req.addHeader("Range", "bytes=" + mLocalFileSize + "-");
                }
                response = httpClient.execute(req);

                int serverResponse = response.getStatusLine().getStatusCode();
                if (serverResponse == HttpStatus.SC_NOT_FOUND) {
                    if (DEBUG)
                        Log.d(TAG, "File not found on Server. Trying next one.");
                } else if (serverResponse != HttpStatus.SC_OK && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {
                    if (DEBUG)
                        Log.d(TAG, "Server returned status code " + serverResponse + " for update zip trying next mirror");
                } else {
                    // server must support partial content for resume
                    if (mLocalFileSize > 0 && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {
                        if (DEBUG)
                            Log.d(TAG, "Resume not supported");

                        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_not_supported, 0));
                        //To get the UdpateProgressBar working correctly, when server does not support resume
                        mLocalFileSize = 0;
                    } else if (mLocalFileSize > 0 && serverResponse == HttpStatus.SC_PARTIAL_CONTENT) {
                        if (DEBUG)
                            Log.d(TAG, "Resume supported");

                        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_download, 0));
                    }

                    downloadedMD5 = updateInfo.getMD5();
                    if (DEBUG)
                        Log.d(TAG, "MD5: " + downloadedMD5);

                    // Download Update ZIP
                    HttpEntity entity = response.getEntity();
                    dumpFile(entity, partialDestinationFile, destinationFile);

                    // Was the download cancelled?
                    if (mPrepForDownloadCancel) {
                        if (DEBUG)
                            Log.d(TAG, "Download was canceled. Break the for loop");
                        entity = null;
                        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_canceled_toast_message, 0));
                        return true;
                    }

                    // Continue the process
                    if (entity != null && !mPrepForDownloadCancel) {
                        if (DEBUG)Log.d(TAG, "Consuming entity....");
                        entity.consumeContent();
                        if (DEBUG) Log.d(TAG, "Entity consumed");
                    } else {
                        if (DEBUG)
                            Log.d(TAG, "Entity reset to NULL");
                        entity = null;
                    }
                    if (DEBUG)
                        Log.d(TAG, "Update download finished");

                    if (DEBUG)
                        Log.d(TAG, "Performing MD5 verification");

                    if (!MD5.checkMD5(downloadedMD5, destinationFile)) {
                        throw new IOException(getResources().getString(R.string.md5_verification_failed));
                    }

                    //If we reach here, download & MD5 check went fine :)
                    return true;
                }

            } catch (IOException ex) {
                ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
                Log.e(TAG, "An error occured while downloading the update file.", ex);

            } catch (NotEnoughSpaceException ex) {
                ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
                Log.e(TAG, "Not enough Space on SDCard to download the Update");
                return false;
            }
        }

        // Houston, we have a problem!
        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
        if (DEBUG)
            Log.d(TAG, "Unable to download the update file from any mirror");

        return false;
    }

    private void dumpFile(HttpEntity entity, File partialDestinationFile, File destinationFile)
            throws IOException, NotEnoughSpaceException {
        if (DEBUG)
            Log.d(TAG, "DumpFile Called");

        if (!mPrepForDownloadCancel) {
            mContentLength = (int) entity.getContentLength();
            if (mContentLength <= 0) {
                if (DEBUG)
                    Log.d(TAG, "unable to determine the update file size, Set ContentLength to 1024");
                mContentLength = 1024;
            } else if (DEBUG) Log.d(TAG, "Update size: " + (mContentLength / 1024) + "KB");

            //Check if there is enough Space on SDCard for Downloading the Update
            if (!SysUtils.EnoughSpaceOnSdCard(mContentLength))
                throw new NotEnoughSpaceException(getResources().getString(R.string.download_not_enough_space));

            mStartTime = System.currentTimeMillis(); 

            byte[] buff = new byte[64 * 1024];
            int read;
            RandomAccessFile out = new RandomAccessFile(partialDestinationFile, "rw");
            out.seek(mLocalFileSize);
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
                mTotalDownloaded = mLocalFileSize;
                progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask, 100, 500);
                while ((read = is.read(buff)) > 0 && !mPrepForDownloadCancel) {
                    out.write(buff, 0, read);
                    mTotalDownloaded += read;
                }
                out.close();
                is.close();
                if (!mPrepForDownloadCancel) {
                    partialDestinationFile.renameTo(destinationFile);
                    if (DEBUG)
                        Log.d(TAG, "Download finished");
                } else {
                    if (DEBUG)
                        Log.d(TAG, "Download cancelled");
                }

            } catch (IOException e) {
                out.close();
                try {
                    destinationFile.delete();
                }
                catch (SecurityException ex) {
                    Log.e(TAG, "Unable to delete downloaded File. Continue anyway.", ex);
                }

            } finally {
                progressUpdateTimer.cancel();
                progressUpdateTimerTask.cancel();
                buff = null;
            }

        } else if (DEBUG)
            Log.d(TAG, "Download Cancel in Progress. Don't start Downloading");
    }

    private void onProgressUpdate() {
        //Only update the Notification and DownloadLayout, when no downloadcancel is in progress, so the notification will not pop up again
        if (!mPrepForDownloadCancel) {

            // Determine the progress made up to this point
            // localFileSize because the contentLength will only be the missing bytes and not the whole file
            long contentLengthOfFullDownload = mContentLength + mLocalFileSize;
            long speed = ((mTotalDownloaded - mLocalFileSize) / (System.currentTimeMillis() - mStartTime));
            speed = (speed > 0) ? speed : 1;
            long remainingTime = ((contentLengthOfFullDownload - mTotalDownloaded) / speed);
            String stringDownloaded = mTotalDownloaded / 1048576 + "/" + contentLengthOfFullDownload / 1048576 + " MB";
            String stringSpeed = speed + " kB/s";
            String stringRemainingTime = remainingTime / 60000 + " " + mMinString + " " + remainingTime % 60 + " " + mSecString;
            String stringComplete = stringDownloaded + " " + stringSpeed + " " + stringRemainingTime;

            // Get the intent ready
            Intent intent = new Intent(this, DownloadUpdate.class);
            intent.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) mCurrentUpdate);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the notification ready
            Resources res = getResources();
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download);
            builder.setWhen(System.currentTimeMillis());
            builder.setTicker(res.getString(R.string.notification_tickertext));

            // Set the rest of the content
            builder.setContentTitle(res.getString(R.string.app_name));  // TODO: change this
            builder.setContentText(stringComplete);
            builder.setContentIntent(contentIntent);
            builder.setProgress((int) contentLengthOfFullDownload, (int) mTotalDownloaded, false);
            builder.setOngoing(true);
            builder.setAutoCancel(false);
            Notification noti = builder.build();

            // Trigger the notification
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.NOTIFICATION_DOWNLOAD_STATUS, noti);

            // Update the DownloadProgress
            UpdateDownloadProgress(mTotalDownloaded, (int) contentLengthOfFullDownload, stringDownloaded, stringSpeed, stringRemainingTime);

        } else if (DEBUG)
            Log.d(TAG, "Downloadcancel in Progress. Not updating the Notification and DownloadLayout");

    }

    private void notifyUser(UpdateInfo ui, boolean success) {
        if (DEBUG)
            Log.d(TAG, "Called Notify User");

        Intent i;

        if (!success) {
            if (DEBUG)
                Log.d(TAG, "Downloaded Update was NULL");

            DeleteDownloadStatusNotification();
            DownloadError();
            stopSelf();
            return;
        }

        // Clear any existing notifications
        DeleteDownloadStatusNotification();

        // Get the apply update intent ready
        i = new Intent(this, ApplyUpdate.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) ui);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // Set the Notification to finished
        Resources res = getResources();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_tab_install);
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(res.getString(R.string.notification_finished));

        // Set the rest of the content
        builder.setContentTitle(res.getString(R.string.app_name));
        builder.setContentText(res.getString(R.string.notification_finished));
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);
        Notification noti = builder.build();

        // Trigger the notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(Constants.NOTIFICATION_DOWNLOAD_FINISHED, noti);

        DownloadFinished();
    }

    private void notificateDownloadError(String ExceptionText) {
        mDownloading = false;

        Intent i = new Intent(this, UpdatesSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);

        // Set the Notification to error
        Resources res = getResources();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.stat_notify_error);
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(res.getString(R.string.not_update_download_error_ticker));

        // Set the rest of the content
        builder.setContentTitle(res.getString(R.string.not_update_download_error_title));
        builder.setContentText(ExceptionText);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);
        Notification noti = builder.build();

        // Trigger the notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.string.not_update_download_error_title, noti);

        ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.exception_while_downloading, 0));
    }

    private void DeleteDownloadStatusNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.NOTIFICATION_DOWNLOAD_STATUS);
    }

    private void UpdateDownloadProgress(final long downloaded, final int total, final String downloadedText,
            final String speedText, final String remainingTimeText) {
        final int N = mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mCallbacks.getBroadcastItem(i).updateDownloadProgress(downloaded, total, downloadedText,
                        speedText, remainingTimeText);

            } catch (RemoteException e) {
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

            } catch (RemoteException e) {
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

            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void cancelCurrentDownload() {
        mPrepForDownloadCancel = true;
        if (DEBUG)
            Log.d(TAG, "Download Service CancelDownload was called");

        DeleteDownloadStatusNotification();
        File update = new File(mFullUpdateFolderPath + "/" + mCurrentUpdate.getFileName());
        if (update.exists()) {
            update.delete();
            if (DEBUG)
                Log.d(TAG, update.getAbsolutePath() + " deleted");
        }
        mDownloading = false;

        if (DEBUG)
            Log.d(TAG, "Download Cancel StopSelf was called");
        stopSelf();
    }

    private void stopDownload() {
        mPrepForDownloadCancel = true;
        mDownloading = false;
        stopSelf();
    }

    private final Handler ToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0)
                Toast.makeText(DownloadService.this, msg.arg1, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(DownloadService.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
        }
    };
}
