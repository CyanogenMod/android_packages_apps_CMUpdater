/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.tasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;

public class UpdatePreparationTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdatePreparationTask";

    private final ProgressDialog mProgressDialog;
    private final UpdatesSettings mParent;
    private final List<String> mCommands;
    private boolean mPreparationFailed;

    public UpdatePreparationTask(UpdatesSettings parent, List<String> commands, boolean showProgressDialog) {
        mParent = parent;
        mCommands = commands;
        mPreparationFailed = true;
        if (showProgressDialog) {
            mProgressDialog = new ProgressDialog(mParent);
            mProgressDialog.setTitle(R.string.apply_preparing_the_update);
            mProgressDialog.setMessage(mParent.getResources().getString(R.string.apply_preparing_the_update));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        } else {
            mProgressDialog = null;
        }
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            // Run the commands using sh
            Process p = Runtime.getRuntime().exec("sh");
            OutputStream os = p.getOutputStream();
            for(String command : mCommands) {
                command = command + "\n";
                os.write(command.getBytes());
            }
            os.close();

            // Wait for the sh process to finish; this could take awhile if there
            // are any long running commands like 'cp'
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                Log.e(TAG, "Update preparation failed:", e);
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to reboot into recovery mode:", e);
            return null;
        }

        // Everything finsihed without error
        mPreparationFailed = false;
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mProgressDialog != null) {
            // Dismiss the progress dialog
            mProgressDialog.dismiss();
        }

        if (mPreparationFailed) {
            // Something went wrong
            mParent.updatePreparationFailed();
        } else {
            // Everything is ready, so reboot into recovery
            mParent.triggerReboot();
        }
    }
}
