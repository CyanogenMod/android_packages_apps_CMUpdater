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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.service.UpdateCheckService;

public class UpdateCheckTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdateCheckTask";

    private final ProgressDialog mProgressDialog;
    private final UpdatesSettings mParent;

    private Object mWaitToken = new Object();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mWaitToken) {
                mWaitToken.notify();
            }
        }
    };

    public UpdateCheckTask(UpdatesSettings parent) {
        mParent = parent;
        mProgressDialog = new ProgressDialog(mParent);
        mProgressDialog.setTitle(R.string.checking_for_updates);
        mProgressDialog.setMessage(mParent.getResources().getString(R.string.checking_for_updates));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                cancel(true);
            }
        });
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... args) {
        synchronized (mWaitToken) {
            Intent intent = new Intent(mParent, UpdateCheckService.class);
            intent.putExtra(Constants.CHECK_FOR_UPDATE, true);
            if (mParent.startService(intent) == null) {
                Log.e(TAG, "startService failed");
            } else {
                IntentFilter filter = new IntentFilter(UpdateCheckService.ACTION_CHECK_FINISHED);
                mParent.registerReceiver(mReceiver, filter);
                try {
                    mWaitToken.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
                mParent.unregisterReceiver(mReceiver);
                mParent.stopService(intent);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mProgressDialog.dismiss();
        mParent.updateLayout();
    }

    @Override
    protected void onCancelled() {
        synchronized (mWaitToken) {
            mWaitToken.notify();
        }
        mProgressDialog.dismiss();
        mParent.updateLayout();
        super.onCancelled();
    }
}
