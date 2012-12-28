/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
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
    private final ProgressDialog mProgressDialog;
    private final UpdatesSettings mParent;
    private ComponentName mComp;

    public UpdateCheckTask(UpdatesSettings parent) {
        mParent = parent;
        mProgressDialog = new ProgressDialog(mParent);
        mProgressDialog.setTitle(R.string.checking_for_updates);
        mProgressDialog.setMessage(mParent.getResources().getString(R.string.checking_for_updates));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (!isCancelled()) {
                    cancel(true);
                    unbindAndStop();
                }
            }
        });
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog.show();
        mServiceIntent = new Intent(IUpdateCheckService.class.getName());
        mComp = mParent.startService(mServiceIntent);
        if (mComp == null) {
            Log.e(TAG, "startService failed");
            mBound = false;
        } else {
            mBound = mParent.bindService(mServiceIntent, mConnection, 0);
        }
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        if (mBound) {
            try {
                while (mService == null) {
                    // Wait till the Service is bound 
                }
                if (isCancelled() == false) {
                	mService.checkForUpdates();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on calling UpdateCheckService", e);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
    	unbindAndStop();
        mParent.updateLayout();
    }

    @Override
    protected void onCancelled() {
    	unbindAndStop();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mParent.updateLayout();
        super.onCancelled();
    }

    /**
     * Unbinds and stops the UpdateCheckService.
     */
    private void unbindAndStop() {
        if (mBound) {
            mParent.unbindService(mConnection);
            mBound = false;
        }
        if (mComp != null) {
            mParent.stopService(mServiceIntent);
            mComp = null;
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IUpdateCheckService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            try {
                mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
            mService = null;
        }
    };

    private final IUpdateCheckServiceCallback mCallback = new IUpdateCheckServiceCallback.Stub() {
        public void updateCheckFinished() throws RemoteException {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
    };
}
