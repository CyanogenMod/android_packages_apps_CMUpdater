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

package com.cyanogenmod.updater.tasks;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.cyanogenmod.updater.interfaces.IUpdateCheckService;
import com.cyanogenmod.updater.interfaces.IUpdateCheckServiceCallback;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;

public class UpdateCheckTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdateCheckTask";

    private IUpdateCheckService mService;
    private boolean mBound;
    private Intent mServiceIntent;
    private final ProgressDialog mPgDialog;
    private final UpdatesSettings mParent;

    public UpdateCheckTask(UpdatesSettings parent) { 
        mParent = parent;
        mPgDialog = new ProgressDialog(mParent);
        mPgDialog.setTitle(R.string.checking_for_updates);
        mPgDialog.setMessage(mParent.getResources().getString(R.string.checking_for_updates));
        mPgDialog.setIndeterminate(true);
        mPgDialog.setCancelable(true);
        mPgDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (!isCancelled()) {
                    cancel(true);
                }
            }
        });
    }

    @Override
    protected void onPreExecute() {
        mPgDialog.show();
        mServiceIntent = new Intent(IUpdateCheckService.class.getName());
        ComponentName comp = mParent.startService(mServiceIntent);
        if (comp == null)
            Log.e(TAG, "startService failed");
        mBound = mParent.bindService(mServiceIntent, mConnection, 0);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            //Wait till the Service is bound
            while (mService == null) {
            }
            mService.checkForUpdates();
        }
        catch (RemoteException e) {
            Log.e(TAG, "Exception on calling UpdateCheckService", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mBound) {
            mParent.unbindService(mConnection);
            mBound = false;
        }

        boolean stopped = mParent.stopService(mServiceIntent);
        mParent.updateLayout();
    }

    @Override
    protected void onCancelled() {
        if (mBound) {
            mParent.unbindService(mConnection);
            mBound = false;
        }
        mParent.stopService(mServiceIntent);
        if (mPgDialog != null) {
            mPgDialog.dismiss();
        }

        mParent.updateLayout();
        super.onCancelled();

    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IUpdateCheckService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
        }
        }

        public void onServiceDisconnected(ComponentName name) {
            try {
                mService.unregisterCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
            mService = null;
        }
    };

    private final IUpdateCheckServiceCallback mCallback = new IUpdateCheckServiceCallback.Stub() {
        public void UpdateCheckFinished() throws RemoteException {
            if (mPgDialog != null) {
                mPgDialog.dismiss();
            }
        }
    };
}