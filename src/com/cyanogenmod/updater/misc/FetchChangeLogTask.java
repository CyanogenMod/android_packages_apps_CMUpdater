/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import com.android.volley.toolbox.RequestFuture;
import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.NotifyingWebView;
import com.cyanogenmod.updater.UpdateApplication;
import com.cyanogenmod.updater.requests.ChangeLogRequest;
import com.cyanogenmod.updater.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

public class FetchChangeLogTask extends AsyncTask<UpdateInfo, Void, Void>
        implements DialogInterface.OnDismissListener {
    private static final String TAG = "FetchChangeLogTask";

    private Context mContext;
    private UpdateInfo mInfo;
    private NotifyingWebView mChangeLogView;
    private AlertDialog mAlertDialog;

    public FetchChangeLogTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(UpdateInfo... infos) {
        mInfo = infos[0];

        if (mInfo != null) {
            File changeLog = mInfo.getChangeLogFile(mContext);
            if (!changeLog.exists()) {
                fetchChangeLog(mInfo);
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.change_log_dialog, null);
        final View progressContainer = view.findViewById(R.id.progress);
        mChangeLogView =
                (NotifyingWebView) view.findViewById(R.id.changelog);

        mChangeLogView.setOnInitialContentReadyListener(
                new NotifyingWebView.OnInitialContentReadyListener() {
                    @Override
                    public void onInitialContentReady(WebView webView) {
                        progressContainer.setVisibility(View.GONE);
                        mChangeLogView.setVisibility(View.VISIBLE);
                    }
                });

        mChangeLogView.getSettings().setTextZoom(80);
        mChangeLogView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.darker_gray));

        // Prepare the dialog box
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.changelog_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        mAlertDialog.setOnDismissListener(this);
        mAlertDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        File changeLog = mInfo.getChangeLogFile(mContext);

        if (changeLog.length() == 0) {
            // Change log is empty
            Toast.makeText(mContext, R.string.no_changelog_alert, Toast.LENGTH_SHORT).show();
        } else {
            // Load the url
            mChangeLogView.loadUrl(Uri.fromFile(changeLog).toString());
        }
    }

    private void fetchChangeLog(final UpdateInfo info) {
        Log.d(TAG, "Getting change log for " + info + ", url " + info.getChangelogUrl());

        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    Toast.makeText(mContext, R.string.no_changelog_alert,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        // We need to make a blocking request here
        RequestFuture<String> future = RequestFuture.newFuture();
        ChangeLogRequest request = new ChangeLogRequest(Request.Method.GET, info.getChangelogUrl(),
                Utils.getUserAgentString(mContext), future, errorListener);
        request.setTag(TAG);

        ((UpdateApplication) mContext.getApplicationContext()).getQueue().add(request);
        try {
            String response = future.get();
            parseChangeLogFromResponse(info, response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void parseChangeLogFromResponse(UpdateInfo info, String response) {
        boolean finished = false;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(
                    new FileWriter(info.getChangeLogFile(mContext)));
            ByteArrayInputStream bais = new ByteArrayInputStream(response.getBytes());
            reader = new BufferedReader(new InputStreamReader(bais), 2 * 1024);
            boolean categoryMatch = false, hasData = false;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("=")) {
                    categoryMatch = !categoryMatch;
                } else if (categoryMatch) {
                    if (hasData) {
                        writer.append("<br />");
                    }
                    writer.append("<b><u>");
                    writer.append(line);
                    writer.append("</u></b>");
                    writer.append("<br />");
                    hasData = true;
                } else if (line.startsWith("*")) {
                    writer.append("<br /><b>");
                    writer.append(line.replaceAll("\\*", ""));
                    writer.append("</b>");
                    writer.append("<br />");
                    hasData = true;
                } else {
                    writer.append("&#8226;&nbsp;");
                    writer.append(line);
                    writer.append("<br />");
                    hasData = true;
                }
            }
            finished = true;
        } catch (IOException e) {
            Log.e(TAG, "Downloading change log for " + info + " failed", e);
            // keeping finished at false will delete the partially written file below
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore, not much we can do anyway
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore, not much we can do anyway
                }
            }
        }

        if (!finished) {
            info.getChangeLogFile(mContext).delete();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        // Cancel all pending requests
        ((UpdateApplication) mContext.getApplicationContext()).getQueue().cancelAll(TAG);
        // Clean up
        mChangeLogView.destroy();
        mChangeLogView = null;
        mAlertDialog = null;
    }
}
