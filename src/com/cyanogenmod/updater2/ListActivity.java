package com.cyanogenmod.updater2;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cyanogenmod.updater2.misc.Constants;
import com.cyanogenmod.updater2.misc.State;
import com.cyanogenmod.updater2.misc.UpdateInfo;
import com.cyanogenmod.updater2.receiver.DownloadReceiver;
import com.cyanogenmod.updater2.service.UpdateCheckService;
import com.cyanogenmod.updater2.ui.RecyclerClickListener;
import com.cyanogenmod.updater2.ui.UpdateAdapter;
import com.cyanogenmod.updater2.utils.UpdateFilter;
import com.cyanogenmod.updater2.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    // intent extras
    public static final String EXTRA_UPDATE_LIST_UPDATED = "update_list_updated";
    public static final String EXTRA_FINISHED_DOWNLOAD_ID = "download_id";
    public static final String EXTRA_FINISHED_DOWNLOAD_PATH = "download_path";
    public static final String EXTRA_FINISHED_DOWNLOAD_INCREMENTAL_FOR = "download_incremental_for";

    private Context mContext;

    // Download
    private DownloadManager mDownloadManager;
    private boolean isDownloading = false;
    private boolean isUpdating = false;
    private long mDownloadId;
    private String mDownloadFileName;

    // Check
    private Snackbar mProgressSnack;

    private File mUpdateFolder;

    private LinkedList<UpdateInfo> mUpdates;
    private SharedPreferences mPrefs;

    // UI
    private CoordinatorLayout mCoordinator;
    private RecyclerView mRecyclerView;
    private RelativeLayout mDownloadLayout;
    private ProgressBar mProgressBar;


    // Update service

    private Handler mUpdateHandler = new Handler();
    private Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            if (!isDownloading || mUpdates == null || mUpdates.isEmpty() || mDownloadId < 0) {
                return;
            }

            if (mProgressBar == null) {
                return;
            }

            Cursor mCursor =
                    mDownloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
            int mStatus;

            if (mCursor == null || !mCursor.moveToFirst()) {
                // DownloadReceiver has likely already removed the download
                // from the DB due to failure or MD5 mismatch
                mStatus = DownloadManager.STATUS_FAILED;
            } else {
                mStatus = mCursor.getInt(mCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }

            switch (mStatus) {
                case DownloadManager.STATUS_PENDING:
                    mProgressBar.setIndeterminate(true);
                    break;
                case DownloadManager.STATUS_PAUSED:
                case DownloadManager.STATUS_RUNNING:
                    int mDownloadedBytes = mCursor.getInt(mCursor.getColumnIndex(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int mTotalBytes = mCursor.getInt(mCursor.getColumnIndex(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (mTotalBytes < 0) {
                        mProgressBar.setIndeterminate(true);
                    } else {
                        mProgressBar.setIndeterminate(false);
                        mProgressBar.setMax(mTotalBytes);
                        mProgressBar.setProgress(mDownloadedBytes);
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    resetDownloadState();
                    break;
            }

            if (mCursor != null) {
                mCursor.close();
            }

            if (mStatus != DownloadManager.STATUS_FAILED) {
                mUpdateHandler.postDelayed(this, 1000);
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();

            if (DownloadReceiver.ACTION_DOWNLOAD_STARTED.equals(mAction)) {
                mDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                mUpdateHandler.post(mUpdateProgress);
            } else if (UpdateCheckService.ACTION_CHECK_FINISHED.equals(mAction)) {
                if (mProgressSnack != null) {
                    mProgressSnack.dismiss();
                    mProgressSnack = null;

                    if (intent.getIntExtra(UpdateCheckService.EXTRA_NEW_UPDATE_COUNT, -1) < 0) {
                        Snackbar.make(mCoordinator, R.string.update_check_failed,
                                Snackbar.LENGTH_LONG).show();
                    }

                    requestUpdateLayout();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_list);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mHeaderCm = (TextView) findViewById(R.id.header_cm_version);
        TextView mHeaderInfo = (TextView) findViewById(R.id.header_info);

        String[] mInstalled = Utils.getInstalledVersion().split("-");

        mHeaderCm.setText(String.format(getString(R.string.header_cm), mInstalled[0]));
        String mApi;
        switch (Utils.getInstalledApiLevel()) {
            case 23:
                mApi = "6.0.1";
                break;
            case 24:
                mApi = "7.0";
                break;
            default:
                mApi = "API " + String.valueOf(Utils.getInstalledApiLevel());
        }
        mHeaderInfo.setText(String.format(getString(R.string.header_info), mApi,
                mInstalled[2], Utils.getBuildDate(this, mInstalled[1])));


        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_cid_head);
        setSupportActionBar(mToolbar);


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mCoordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mDownloadLayout = (RelativeLayout) findViewById(R.id.update_download_status);
        mProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);

        mDownloadId = mPrefs.getLong(Constants.DOWNLOAD_ID, -1);
        if (mDownloadId >= 0) {
            Cursor mCursor = mDownloadManager.query(
                    new DownloadManager.Query().setFilterById(mDownloadId));
            if (mCursor == null || !mCursor.moveToFirst()) {
                Snackbar.make(mCoordinator, R.string.download_not_found,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                int mStatus = mCursor.getInt(mCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                Uri mUri = Uri.parse(
                        mCursor.getString(mCursor.getColumnIndex(DownloadManager.COLUMN_URI)));
                if (mStatus == DownloadManager.STATUS_PENDING ||
                        mStatus == DownloadManager.STATUS_RUNNING ||
                        mStatus == DownloadManager.STATUS_PAUSED) {
                    mDownloadFileName = mUri.getLastPathSegment();
                }
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }

        if (mDownloadId < 0 || mDownloadFileName == null) {
            resetDownloadState();
        }

        checkForUpdates();

        requestUpdateLayout();

        IntentFilter mFilter = new IntentFilter(UpdateCheckService.ACTION_CHECK_FINISHED);
        mFilter.addAction(DownloadReceiver.ACTION_DOWNLOAD_STARTED);
        mContext.registerReceiver(mReceiver, mFilter);

        checkForDownloadCompleted(getIntent());
        setIntent(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mUpdateHandler.removeCallbacks(mUpdateProgress);
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // Fail without making noise
        }
        if (mProgressSnack != null && mProgressSnack.isShown()) {
            mProgressSnack.dismiss();
        }
    }

    private void checkForUpdates() {
        if (!Utils.isOnline(mContext)) {
            Snackbar.make(mCoordinator, R.string.data_connection_required,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        mProgressSnack = Snackbar.make(mCoordinator, R.string.checking_for_updates,
                Snackbar.LENGTH_INDEFINITE);
        mProgressSnack.setAction(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mCancelIntent = new Intent(mContext, UpdateCheckService.class);
                mCancelIntent.setAction(UpdateCheckService.ACTION_CANCEL_CHECK);
                mContext.startService(mCancelIntent);
                mProgressSnack.dismiss();
            }
        });

        Intent mCheckIntent = new Intent(this, UpdateCheckService.class);
        mCheckIntent.setAction(UpdateCheckService.ACTION_CHECK);
        startService(mCheckIntent);

        mProgressSnack.show();
    }

    private void checkUpdateType() {
        int mUpdateType = mPrefs.getInt(Constants.UPDATE_TYPE_PREF,
                Constants.UPDATE_TYPE_SNAPSHOT);
        if (mUpdateType != Utils.getUpdateType()) {
            mPrefs.edit().putInt(Constants.UPDATE_TYPE_PREF, Utils.getUpdateType()).apply();
        }
    }

    private void resetDownloadState() {
        mDownloadId = -1;
        mDownloadFileName = null;
        isDownloading = false;
        mDownloadLayout.setVisibility(View.GONE);
    }

    private void requestUpdateLayout() {
        Utils.cancelNotification(mContext);
        updateLayout();
    }

    private void updateLayout() {
        mUpdates = new LinkedList<>();
        LinkedList<String> mExistingFiles = new LinkedList<>();

        mUpdateFolder = Utils.makeUpdateFolder(mContext);
        File[] mFiles = mUpdateFolder.listFiles(new UpdateFilter(".zip"));

        if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && mFiles != null) {
            for (File mFile : mFiles) {
                if (mFile.isFile()) {
                    mExistingFiles.add(mFile.getName());
                }
            }
        }

        LinkedList<UpdateInfo> mAvailableUpdates = State.loadState(mContext);

        for (UpdateInfo mAvailableUpdate : mAvailableUpdates.subList(0, 2)) {
            if (mExistingFiles.contains(mAvailableUpdate.getFileName())) {
                continue;
            }

            mUpdates.add(mAvailableUpdate);
        }

        for (String mFileName : mExistingFiles) {
            mUpdates.add(new UpdateInfo.Builder().setFileName(mFileName).build());
        }

        String mInstalled = String.format("cm-%1$s.zip", Utils.getInstalledVersion());
        String mIncremental = Utils.getIncremental();

        HashMap<String, UpdateInfo> mUpdatesMap = new HashMap<>();
        for (UpdateInfo mUpdate : mUpdates) {
            mUpdatesMap.put(mUpdate.getFileName(), mUpdate);
        }

        int mPosition = 0;
        for (UpdateInfo mUpdate : mUpdates) {
            if (mUpdate.isIncremental()) {
                continue;
            }

            boolean hasIncremental = false;
            String mIncrementalFile = String.format("incremental-%1$s-%2$s.zip", mIncremental,
                    mUpdate.getIncremental());
            if (mUpdatesMap.containsKey(mIncrementalFile)) {
                hasIncremental = true;
                mUpdate.setFileName(mIncrementalFile);
            }

            boolean isDownloadingUpdate = mUpdate.getFileName().equals(mDownloadFileName);
            int mStyle;

            if (isDownloadingUpdate) {
                mStyle = Constants.STYLE_DOWNLOADING;
            } else if (mExistingFiles.contains(mUpdate.getFileName())) {
                mStyle = Constants.STYLE_DOWNLOADED;
            } else if (mUpdate.getFileName().equals(mInstalled)) {
                mStyle = Constants.STYLE_INSTALLED;
            } else if (mUpdate.getDownloadUrl() != null) {
                mStyle = Constants.STYLE_NEW;
            } else {
                mStyle = Constants.STYLE_DOWNLOADED;
            }

            mUpdate.setStyle(mStyle);

            //mRecyclerView.getChildAt(mPosition);
            //TODO: setup child ui basing on installed state

            mPosition++;
        }

        if (mUpdates != null && !mUpdates.isEmpty()) {
            Collections.sort(mUpdates, new Comparator<UpdateInfo>() {
                @Override
                public int compare(UpdateInfo t1, UpdateInfo t2) {
                    return -t1.getName().compareTo(t2.getName());
                }
            });
        }

        UpdateAdapter mAdapter = new UpdateAdapter(mUpdates, this);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        mAdapter.notifyDataSetChanged();
    }

    private void checkForDownloadCompleted(Intent mIntent) {
        if (mIntent == null) {
            return;
        }

        long mId = mIntent.getLongExtra(EXTRA_FINISHED_DOWNLOAD_ID, -1);
        String mFullPath = mIntent.getStringExtra(EXTRA_FINISHED_DOWNLOAD_PATH);

        if (mId < 0 || mFullPath == null) {
            return;
        }

        updateLayout();

        resetDownloadState();

        String mIncremental = mIntent.getStringExtra(EXTRA_FINISHED_DOWNLOAD_INCREMENTAL_FOR);
        if (mIncremental != null) {
            onStartUpdate(mUpdates.get((int) mDownloadId));
        }
    }

    public void startDownload(UpdateInfo mUpdate) {
        if (!Utils.isOnline(mContext)) {
            Snackbar.make(mCoordinator, R.string.data_connection_required,
                    Snackbar.LENGTH_SHORT).show();
            return;
        } else if (isDownloading) {
            Snackbar.make(mCoordinator, R.string.download_already_running,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        int mPosition = 0;
        for (UpdateInfo mInfo : mUpdates) {
            if (mInfo.equals(mUpdate)) {
                break;
            }
            mPosition++;
        }

        mDownloadId = mPosition;

        isDownloading = true;
        mDownloadFileName = mUpdate.getFileName();

        Intent mIntent = new Intent(mContext, DownloadReceiver.class);
        mIntent.setAction(DownloadReceiver.ACTION_START_DOWNLOAD);
        mIntent.putExtra(DownloadReceiver.EXTRA_UPDATE_INFO, (Parcelable) mUpdate);
        mContext.sendBroadcast(mIntent);

        mUpdateHandler.post(mUpdateProgress);

    }

    public void stopDowload(UpdateInfo mUpdate) {
        if (!isDownloading || mDownloadFileName == null || mDownloadId < 0) {
            resetDownloadState();
            return;
        }

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.confirm_download_cancelation_dialog_title)
                .setMessage(R.string.confirm_download_cancelation_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mDownloadManager.remove(mDownloadId);
                        mUpdateHandler.removeCallbacks(mUpdateProgress);
                        resetDownloadState();

                        mPrefs.edit().remove(Constants.DOWNLOAD_ID)
                                .remove(Constants.DOWNLOAD_MD5).apply();

                        Snackbar.make(mCoordinator, R.string.download_cancelled,
                                Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onStartUpdate(final UpdateInfo mUpdate) {
        if (isUpdating) {
            return;
        }

        isUpdating = true;

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(String.format(getString(R.string.apply_update_dialog_text),
                        mUpdate.getName()))
                .setPositiveButton(R.string.update_button_install,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Utils.triggerUpdate(mContext, mUpdate.getFileName());
                        } catch (IOException e) {
                            Log.e("OHAI", "Unable to reboot into recovery mode. Error: " +
                                    e.getMessage());
                            Snackbar.make(mCoordinator, R.string.apply_unable_to_reboot_toast,
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        isUpdating = false;
                    }
                })
                .show();
    }
}
