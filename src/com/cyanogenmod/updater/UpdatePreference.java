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
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.updater.misc.FetchChangeLogTask;
import com.cyanogenmod.updater.misc.UpdateInfo;

import java.io.File;

public class UpdatePreference extends Preference implements OnClickListener, OnLongClickListener {
    private static final float DISABLED_ALPHA = 0.4f;
    public static final int STYLE_NEW = 1;
    public static final int STYLE_DOWNLOADING = 2;
    public static final int STYLE_DOWNLOADED = 3;
    public static final int STYLE_INSTALLED = 4;

    public interface OnActionListener {
        void onStartDownload(UpdatePreference pref);
        void onStopDownload(UpdatePreference pref);
        void onStartUpdate(UpdatePreference pref);
        void onDeleteUpdate(UpdatePreference pref);
    }

    public interface OnReadyListener {
        void onReady(UpdatePreference pref);
    }

    private OnActionListener mOnActionListener;
    private OnReadyListener mOnReadyListener;

    private UpdateInfo mUpdateInfo = null;
    private int mStyle;

    private ImageView mUpdatesButton;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mUpdatesPref;
    private ProgressBar mProgressBar;

    private OnClickListener mButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnActionListener == null) {
                return;
            }

            switch (mStyle) {
                case STYLE_DOWNLOADED:
                    mOnActionListener.onStartUpdate(UpdatePreference.this);
                    break;
                case STYLE_DOWNLOADING:
                    mOnActionListener.onStopDownload(UpdatePreference.this);
                    break;
                case STYLE_NEW:
                    mOnActionListener.onStartDownload(UpdatePreference.this);
                    break;
            }
        }
    };

    public UpdatePreference(Context context, UpdateInfo ui, int style) {
        super(context, null, R.style.UpdatesPreferenceStyle);
        setLayoutResource(R.layout.preference_updates);
        mStyle = style;
        mUpdateInfo = ui;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Store the views from the layout
        mUpdatesButton = (ImageView)view.findViewById(R.id.updates_button);
        mUpdatesButton.setOnClickListener(mButtonClickListener);

        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);
        mProgressBar = (ProgressBar)view.findViewById(R.id.download_progress_bar);

        mUpdatesPref = view.findViewById(R.id.updates_pref);
        mUpdatesPref.setOnClickListener(this);
        mUpdatesPref.setOnLongClickListener(this);

        // Update the views
        updatePreferenceViews();

        if (mOnReadyListener != null) {
            mOnReadyListener.onReady(this);
        }
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
        final Context context = getContext();
        new FetchChangeLogTask(context).execute(mUpdateInfo);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_dialog_message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // We are OK to delete, trigger it
                        if (mOnActionListener != null) {
                            mOnActionListener.onDeleteUpdate(UpdatePreference.this);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @Override
    public String toString() {
        return "UpdatePreference [mUpdateInfo=" + mUpdateInfo + ", mStyle=" + mStyle + "]";
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

    public void setOnActionListener(OnActionListener listener) {
        mOnActionListener = listener;
    }

    public void setOnReadyListener(OnReadyListener listener) {
        mOnReadyListener = listener;
        if (mUpdatesPref != null && listener != null) {
            listener.onReady(this);
        }
    }

    public void setStyle(int style) {
        mStyle = style;
        if (mUpdatesPref != null) {
            showStyle();
        }
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
            mTitleText.setText(mUpdateInfo.getName());
            mTitleText.setVisibility(View.VISIBLE);

            // Show the proper style view
            showStyle();
        }
    }

    private void showStyle() {
        // Display the appropriate preference style
        switch (mStyle) {
            case STYLE_DOWNLOADED:
                // Show the install image and summary of 'Downloaded'
                mUpdatesButton.setImageResource(R.drawable.ic_tab_install);
                mUpdatesButton.setEnabled(true);
                mSummaryText.setText(R.string.downloaded_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;

            case STYLE_DOWNLOADING:
                // Show the cancel button image and progress bar
                mUpdatesButton.setImageResource(R.drawable.ic_tab_cancel);
                mUpdatesButton.setEnabled(true);
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
                mUpdatesButton.setEnabled(true);
                mSummaryText.setText(R.string.new_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;
        }
    }
}
