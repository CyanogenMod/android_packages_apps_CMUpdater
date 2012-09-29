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
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.ApplyUpdate;
import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.UpdatesSettings;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.MD5;

//import com.cyanogenmod.updater.ui.ApplyUpdateActivity;
//import com.cyanogenmod.updater.ui.MainActivity;

import java.io.*;

public class MD5CheckerTask extends AsyncTask<File, Void, Boolean> {
    private static final String TAG = "MD5CheckerTask";
    private boolean DEBUG = false;

    private final ProgressDialog mDialog;
    private final String mFilename;
    private boolean mReturnvalue;
    private final Context mContext;

    public MD5CheckerTask(Context ctx, ProgressDialog dialog, String filename, Boolean _showDebugOutput) {
        mDialog = dialog;
        mFilename = filename;
        mContext = ctx;
    }

    @Override
    public Boolean doInBackground(File... params) {
        boolean MD5exists = false;
        try {
            File MD5file = new File(params[0] + ".md5sum");
            if (MD5file.exists() && MD5file.canRead())
                MD5exists = true;
            if (params[0].exists() && params[0].canRead()) {
                //If MD5 File exists, check it
                if (MD5exists) {
                    //Calculate MD5 of Existing Update
                    String calculatedMD5 = MD5.calculateMD5(params[0]);
                    //Read the existing MD5SUM
                    FileReader input = new FileReader(MD5file);
                    BufferedReader bufRead = new BufferedReader(input);
                    String firstLine = bufRead.readLine();
                    bufRead.close();
                    input.close();
                    //If the content of the File is not empty, compare it
                    if (firstLine != null) {
                        String[] SplittedString = firstLine.split("  ");
                        if (SplittedString[0].equalsIgnoreCase(calculatedMD5))
                            mReturnvalue = true;
                    } else
                        mReturnvalue = false;
                } else {
                    return true;
                }
            }
        }
        catch (IOException e) {
            Log.e(TAG, "IOEx while checking MD5 sum", e);
            mReturnvalue = false;
        }
        return mReturnvalue;
    }

    @Override
    public void onPostExecute(Boolean result) {
        UpdateInfo ui = new UpdateInfo();
        String[] temp = mFilename.split("\\\\");
        ui.setName(temp[temp.length - 1]);
        ui.setFileName(mFilename);
        if (result) {
            Intent i = new Intent(mContext, ApplyUpdate.class);
            i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) ui);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
        } else {
            Toast.makeText(mContext, R.string.apply_existing_update_md5error_message, Toast.LENGTH_SHORT).show();
        }

        //Is null when no MD5SUM is present
        if (mDialog != null)
            mDialog.dismiss();
    }

    @Override
    public void onCancelled() {
        Toast.makeText(mContext, R.string.md5_check_cancelled, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(mContext, UpdatesSettings.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }
}
