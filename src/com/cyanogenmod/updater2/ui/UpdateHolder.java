package com.cyanogenmod.updater2.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyanogenmod.updater2.ListActivity;
import com.cyanogenmod.updater2.R;
import com.cyanogenmod.updater2.misc.Constants;
import com.cyanogenmod.updater2.misc.UpdateInfo;
import com.cyanogenmod.updater2.utils.Utils;

import static com.cyanogenmod.updater2.UpdateApplication.getContext;

class UpdateHolder extends RecyclerView.ViewHolder {
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mSummary;
    private TextView mVersion;
    private ImageButton mExpandButton;
    private LinearLayout mExpandedLayout;
    private Button mButton;

    private boolean isExpanded = false;
    private int mStyle;

    /*
    private RelativeLayout mDownloadLayout;
    private ProgressBar mProgressBar;
    private ImageButton mCancelDownloadButton;
    */

    UpdateHolder(View mView) {
        super(mView);
        mIcon = (ImageView) mView.findViewById(R.id.update_icon);
        mTitle = (TextView) mView.findViewById(R.id.update_title);
        mSummary = (TextView) mView.findViewById(R.id.update_summary);
        mVersion = (TextView) mView.findViewById(R.id.update_version);
        mExpandButton = (ImageButton) mView.findViewById(R.id.expand_button);
        mExpandedLayout = (LinearLayout) mView.findViewById(R.id.update_expanded_view);
        mButton = (Button) mView.findViewById(R.id.update_button);
    }

    void init(final UpdateInfo mUpdate, final ListActivity mCallingActivity) {
        Context mContext = getContext();

        boolean isNightly = mUpdate.getType().equals(UpdateInfo.Type.NIGHTLY);
        String[] mName = mUpdate.getName().split("-");
        String mApi;
        switch (mUpdate.getApiLevel()) {
            case 23:
                mApi = mContext.getString(R.string.update_api_23);
                break;
            case 24:
                mApi = mContext.getString(R.string.update_api_24);
                break;
            default:
                mApi = String.format(
                        mContext.getString(R.string.update_api_unknown),
                        mName[1], mUpdate.getApiLevel());
        }
        mStyle = mUpdate.getStyle();

        int mButtonStatus;

        switch (mStyle) {
            case Constants.STYLE_DOWNLOADED:
                mButtonStatus = R.string.update_button_install;
                break;
            case Constants.STYLE_DOWNLOADING:
                mButtonStatus = R.string.update_button_downloading;
                mButton.setEnabled(false);
                break;
            case Constants.STYLE_INSTALLED:
                mButtonStatus = R.string.update_button_installed;
                mButton.setEnabled(false);
                break;
            default:
                mButtonStatus = R.string.update_button_download;
                break;
        }

        mIcon.setImageResource(isNightly ?
                R.drawable.ic_update_type_nightly : R.drawable.ic_update_type_snapshot);
        mTitle.setText(mName[3]);
        mSummary.setText(Utils.getBuildDate(mContext, mName[2]));
        mVersion.setText(mApi);
        mExpandedLayout.setVisibility(View.GONE);
        mExpandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setExpanded();
            }
        });

        Log.d("OHAI", String.valueOf(mStyle));

        mButton.setText(mButtonStatus);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mStyle) {
                    default:
                        mCallingActivity.startDownload(mUpdate);
                        break;
                }
            }
        });
    }

    void setExpanded() {
        mExpandedLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        isExpanded = !isExpanded;
        mExpandButton.setImageResource(isExpanded ?
                R.drawable.ic_detail_close : R.drawable.ic_detail_open);
    }
}
