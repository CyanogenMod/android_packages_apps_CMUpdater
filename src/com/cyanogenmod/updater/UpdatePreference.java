/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import com.cyanogenmod.updater.customTypes.UpdateInfo;

public class UpdatePreference extends Preference implements OnClickListener, OnLongClickListener {
    private static final String TAG = "UpdatePreference";

    private static final float DISABLED_ALPHA = 0.4f;
    public static final int STYLE_NEW = 1;
    public static final int STYLE_DOWNLOADING = 2;
    public static final int STYLE_DOWNLOADED = 3;
    public static final int STYLE_INSTALLED = 4;

    private final UpdatesSettings mParent;
    private ImageView mUpdatesButton;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mUpdatesPref;
    private int mStyle;
    private String mTitle;
    private ProgressBar mProgressBar;
    private UpdateInfo mUpdateInfo = null;

    public UpdatePreference(UpdatesSettings parent, UpdateInfo ui, String title, int style) {
        super(parent, null, R.style.UpdatesPreferenceStyle);
        setLayoutResource(R.layout.preference_updates);
        mParent = parent;
        mTitle = title;
        mStyle = style;
        mUpdateInfo = ui;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Store the views from the layout
        mUpdatesPref = view.findViewById(R.id.updates_pref);
        mUpdatesPref.setOnClickListener(this);
        mUpdatesPref.setOnLongClickListener(this);

        mUpdatesButton = (ImageView)view.findViewById(R.id.updates_button);
        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);
        mProgressBar = (ProgressBar)view.findViewById(R.id.download_progress_bar);

        // Update the views
        updatePreferenceViews();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mStyle) {
            case STYLE_DOWNLOADED:
            case STYLE_INSTALLED:
                confirmDelete();
                break;

            case STYLE_DOWNLOADING:
            case STYLE_NEW:
            default:
                // Do nothing for now
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        String changeLog = ( TextUtils.isEmpty(mUpdateInfo.getChanges()) ? mParent.getResources().getString(R.string.no_changelog_alert) : mUpdateInfo.getChanges() );
        if (changeLog.equals(mParent.getResources().getString(R.string.no_changelog_alert))
                || changeLog.equals(mParent.getResources().getString(R.string.failed_to_load_changelog))) {
            // No changelog to show, display a toast
            Toast.makeText(mParent.getBaseContext(), changeLog, Toast.LENGTH_SHORT).show();
        } else {
            // Prepare the dialog box content
            WebView chngLog = new WebView(mParent);
            chngLog.getSettings().setTextZoom(80);
            chngLog.setBackgroundColor(mParent.getResources().getColor(android.R.color.darker_gray));
            chngLog.loadDataWithBaseURL(null, changeLog, "text/html", "utf-8", null);

            // Prepare the dialog box
            AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
            builder.setTitle(mParent.getResources().getString(R.string.changelog_dialog_title));
            builder.setView(chngLog);
            builder.setPositiveButton(R.string.dialog_close, null);
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
        builder.setTitle(R.string.confirm_delete_dialog_title);
        builder.setMessage(R.string.confirm_delete_dialog_message);
        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // We are OK to delete, trigger it
                mParent.deleteUpdate(getKey());
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            updatePreferenceViews();
        } else {
            disablePreferenceViews();
        }
    }

    public void setStyle(int style) {
        mStyle = style;
        showStyle();
    }

    public int getStyle() {
        return mStyle;
    }

    public void setProgress(int max, int progress) {
        if (mStyle != STYLE_DOWNLOADING) {
            return;
        }
        mProgressBar.setMax(max);
        mProgressBar.setProgress(progress);
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public ImageView getUpdatesButton() {
        return mUpdatesButton;
    }

    public UpdateInfo getUpdateInfo() {
        return mUpdateInfo;
    }

    public PreferenceActivity getParent() {
        return mParent;
    }

    private void disablePreferenceViews() {
        if (mUpdatesButton != null) {
            mUpdatesButton.setEnabled(false);
            mUpdatesButton.setAlpha(DISABLED_ALPHA);
        }
        if (mUpdatesPref != null) {
            mUpdatesPref.setEnabled(false);
            mUpdatesPref.setBackgroundColor(0);
        }
    }

    private void updatePreferenceViews() {
        if (mUpdatesPref != null) {
            mUpdatesPref.setEnabled(true);
            mUpdatesPref.setLongClickable(true);
            final boolean enabled = isEnabled();
            mUpdatesPref.setOnClickListener(enabled ? this : null);
            if (!enabled) {
                mUpdatesPref.setBackgroundColor(0);
            }

            // Set the title text
            mTitleText.setText(mTitle);
            mTitleText.setVisibility(View.VISIBLE);

            // Show the proper style view
            showStyle();
        }
    }

    private void showStyle() {
        if (mUpdatesButton == null || mSummaryText == null || mProgressBar == null) {
            Log.w(TAG, "Tried to set UI style, but UI elements were null (not yet bound).");
            return;
        }
        // Display the appropriate preference style
        switch (mStyle) {
            case STYLE_DOWNLOADED:
                // Show the install image and summary of 'Downloaded'
                mUpdatesButton.setImageResource(R.drawable.ic_tab_install);
                mUpdatesButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        mParent.startUpdate(mUpdateInfo);
                    }
                });

                mSummaryText.setText(R.string.downloaded_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);

                mProgressBar.setVisibility(View.GONE);
                break;

            case STYLE_DOWNLOADING:
                // Show the cancel button image and progress bar
                mUpdatesButton.setImageResource(R.drawable.ic_tab_cancel);
                mUpdatesButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        mParent.stopDownload();
                    }
                });
                mProgressBar.setVisibility(View.VISIBLE);
                mSummaryText.setVisibility(View.GONE);
                break;

            case STYLE_INSTALLED:
                // Show the installed button image and summary of 'Installed'
                mUpdatesButton.setImageResource(R.drawable.ic_tab_installed);
                mUpdatesButton.setEnabled(false);
                mSummaryText.setText(R.string.installed_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;

            case STYLE_NEW:
            default:
                // Show the download button image and summary of 'New'
                mUpdatesButton.setImageResource(R.drawable.ic_tab_download);
                mUpdatesButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        mParent.startDownload(getKey());
                    }
                });
                mSummaryText.setText(R.string.new_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;
        }
    }

}
