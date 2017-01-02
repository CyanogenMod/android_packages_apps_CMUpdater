/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cyanogenmod.updater.misc.UpdateInfo;
import com.cyanogenmod.updater.utils.Utils;

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

    private Context mContext;
    private UpdateInfo mUpdateInfo = null;
    private int mStyle;

    private ImageView mStopDownloadButton;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mUpdatesPref;
    private ProgressBar mProgressBar;
    private Button mButton;

    private String mBuildName[];

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

    private OnClickListener mStopDownloadClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnActionListener != null && mStyle == STYLE_DOWNLOADING) {
                mOnActionListener.onStopDownload(UpdatePreference.this);
            }
        }
    };

    public UpdatePreference(Context context, UpdateInfo ui, int style) {
        super(context, null, R.style.UpdatesPreferenceStyle);
        setLayoutResource(R.layout.preference_updates);
        mStyle = style;
        mUpdateInfo = ui;
        mContext = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mBuildName = mUpdateInfo.getFileName().split("-");
        final String mBuildDate = Utils.getInstalledBuildDateLocalized(mContext, mBuildName[2]);
        String mApi;
        switch (mBuildName[1]) {
            case "13.0":
                mApi = "6.0.1";
                break;
            case "14.1":
                mApi = "7.1.1";
                break;
            default:
                mApi = "???";
                break;
        }

        // Store the views from the layout
        mTitleText = (TextView)view.findViewById(R.id.title);
        mSummaryText = (TextView)view.findViewById(R.id.summary);
        mProgressBar = (ProgressBar)view.findViewById(R.id.download_progress_bar);
        mStopDownloadButton = (ImageView)view.findViewById(R.id.updates_button);
        mButton = (Button) view.findViewById(R.id.button);
        mStopDownloadButton.setOnClickListener(mButtonClickListener);
        mButton.setOnClickListener(mButtonClickListener);

        mUpdatesPref = view.findViewById(R.id.updates_pref);
        mUpdatesPref.setOnClickListener(this);
        mUpdatesPref.setOnLongClickListener(this);

        // Update the views
        updatePreferenceViews();

        mSummaryText.setText(String.format(mContext.getString(R.string.summary), mBuildDate,
                mBuildName[1], mApi));

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
        // TODO: implement export on sdcard
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
        return mStopDownloadButton;
    }

    public UpdateInfo getUpdateInfo() {
        return mUpdateInfo;
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
            mTitleText.setText(mBuildName[0]);

            // Show the proper style view
            showStyle();
        }
    }

    private void showStyle() {
        // Display the appropriate preference style
        switch (mStyle) {
            case STYLE_DOWNLOADED:
                mStopDownloadButton.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mButton.setVisibility(View.VISIBLE);
                mTitleText.setText(String.format("%1$s %2$s",
                        mBuildName[3], mContext.getString(R.string.type_downloaded)));
                mButton.setText(mContext.getString(R.string.install_button));
                break;

            case STYLE_DOWNLOADING:
                mButton.setVisibility(View.GONE);
                mStopDownloadButton.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                mTitleText.setText(String.format("%1$s %2$s",
                        mBuildName[3], mContext.getString(R.string.type_downloading)));
                break;

            case STYLE_INSTALLED:
                mStopDownloadButton.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mButton.setVisibility(View.GONE);
                mTitleText.setText(String.format("%1$s %2$s",
                        mBuildName[3], mContext.getString(R.string.type_installed)));
                break;

            case STYLE_NEW:
            default:
                mStopDownloadButton.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mButton.setVisibility(View.VISIBLE);
                mTitleText.setText(mBuildName[3]);
                mButton.setText(mContext.getString(R.string.download_button));
                break;
        }
    }
}
