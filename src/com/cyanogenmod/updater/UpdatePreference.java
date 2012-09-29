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

package com.cyanogenmod.updater;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.UpdatesSettings;

import java.io.Serializable;

public class UpdatePreference extends Preference implements OnClickListener {
    private static final String TAG = "UpdatePreference";

    private static final float DISABLED_ALPHA = 0.4f;
    public static final int STYLE_NEW = 1;
    public static final int STYLE_DOWNLOADING = 2;
    public static final int STYLE_DOWNLOADED = 3;

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

        mUpdatesButton = (ImageView)view.findViewById(R.id.updates_button);
        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);
        mProgressBar = (ProgressBar)view.findViewById(R.id.download_progress_bar);

        // Update the views
        updatePreferenceViews();
    }

    @Override
    public void onClick(View v) {
        switch (mStyle) {
            case STYLE_DOWNLOADED:
                // Show the delete confirmation dialog
                confirmDelete();
                break;

            case STYLE_DOWNLOADING:
            case STYLE_NEW:
            default:
                // Do nothing for now
                break;
        }
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
        builder.setTitle(R.string.confirm_delete_dialog_title);
        builder.setMessage(R.string.confirm_delete_dialog_message);
        builder.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // We are OK to delete, trigger it
                mParent.deleteUpdate(getKey());
            }
        });
        builder.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
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
        // Display the appropriate preference style
        switch (mStyle) {
            case STYLE_DOWNLOADED:
                // Show the install image
                mUpdatesButton.setImageResource(R.drawable.ic_tab_unselected_install);
                mUpdatesButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                // Check if an UpdateInfo object is available
                                if (mUpdateInfo == null) {
                                    // If not, create a skeleton with the name
                                    mUpdateInfo = new UpdateInfo();
                                    mUpdateInfo.setFileName(mTitle);
                                }

                                // Attempt to install the supplied update
                                Intent i = new Intent(mParent, ApplyUpdate.class);
                                i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) mUpdateInfo);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mParent.startActivity(i);
                            }
                        });

                // Display a summary of "Downloaded"
                mSummaryText.setText(R.string.downloaded_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;

            case STYLE_DOWNLOADING:
                // Show the cancel button image
                // The download service takes care of the button assignment
                mUpdatesButton.setImageResource(R.drawable.ic_tab_unselected_cancel);
                mProgressBar.setVisibility(View.VISIBLE);
                mSummaryText.setVisibility(View.GONE);
                break;

            case STYLE_NEW:
            default:
                // Show the download button image
                mUpdatesButton.setImageResource(R.drawable.ic_tab_unselected_download);
                mUpdatesButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        mParent.startDownload(getKey());
                    }
                });

                // Display a summary of "New"
                mSummaryText.setText(R.string.new_update_summary);
                mSummaryText.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;
        }
    }

}
