package com.cyanogenmod.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cyanogenmod.updater.customTypes.DownloadProgress;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.interfaces.IDownloadService;
import com.cyanogenmod.updater.interfaces.IDownloadServiceCallback;
import com.cyanogenmod.updater.misc.Constants;

import java.io.Serializable;

public class DownloadUpdate extends Activity {
    private static final String TAG = "DownloadUpdate";

    private Boolean DEBUG = false;

    private UpdatePreference mCallingPreference = null;
    private ProgressBar mProgressBar;
    private ImageView mCancelButton;
    private UpdateInfo mUpdateInfo;
    private PreferenceActivity mParent;

    //Indicates if a Service is bound 
    private boolean mbound = false;
    private Intent serviceIntent;

    public DownloadUpdate(UpdatePreference pref) {
        mCallingPreference = pref;
        mUpdateInfo = pref.getUpdateInfo();
        mProgressBar = pref.getProgressBar();
        mCancelButton = pref.getUpdatesButton();
        mParent = pref.getParent();
    }

    public void startDownload() {
        startService();
    }

    protected void startService() {
        try {
            if (myService != null && myService.DownloadRunning()) {
                mUpdateInfo = myService.getCurrentUpdate();
                myService.registerCallback(mCallback);

                if (DEBUG)
                    Log.d(TAG, "Retrieved update from DownloadService");

                mbound = true;
            } else {
                if (DEBUG)
                    Log.d(TAG, "Not downloading");

                serviceIntent = new Intent(IDownloadService.class.getName());
                ComponentName comp = mParent.startService(serviceIntent);
                if (comp == null) {
                    Log.e(TAG, "startService failed");
                }
                mbound = mParent.bindService(serviceIntent, mConnection, 0);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Error on DownloadService call", ex);
            //Set myService to null, otherwise Mainactivity.onResume will crash,
            //cause the service is not null
            myService = null;
            stopService();
        }

        mCancelButton.setOnClickListener(mCancelDownloadListener);
    }

    protected void stopService() {
        if (DEBUG)
            Log.d(TAG, "onDestroy called");

        try {
            if (myService != null && !myService.DownloadRunning()) {
                MyUnbindService(mConnection);
            } else if (DEBUG)
                Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on calling DownloadService", e);
        }

        boolean stopped = mParent.stopService(serviceIntent);
        if (DEBUG)
            Log.d(TAG, "DownloadService stopped: " + stopped);
    }

    private void updateDownloadProgress(final long downloaded, final int total, final String downloadedText,
            final String speedText, final String remainingTimeText) {
        if (mProgressBar == null) {
            return;
        }

        mProgressBar.post(new Runnable() {
            public void run() {
                if (total < 0) {
                    mProgressBar.setIndeterminate(true);
                } else {
                    mProgressBar.setIndeterminate(false);
                    mProgressBar.setMax(total);
                }
                mProgressBar.setProgress((int) downloaded);
            }
        });
    }

    private final View.OnClickListener mCancelDownloadListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
            builder.setTitle(R.string.confirm_download_cancelation_dialog_title);
            builder.setMessage(R.string.confirm_download_cancelation_dialog_message);
            builder.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (myService != null) {
                        try {
                            myService.cancelDownload();
                        } catch (RemoteException e) {
                            Log.e(TAG, "Exception on calling DownloadService", e);
                        }
                    }

                    MyUnbindService(mConnection);
                    myService = null;
                    stopService();
                    // Set the preference to the proper style if supplied
                    if (mCallingPreference != null) {
                        mCallingPreference.setStyle(UpdatePreference.STYLE_NEW);
                    }
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
    };

    public static IDownloadService myService;

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            myService = IDownloadService.Stub.asInterface(service);
            try {
                myService.registerCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
            //Start Downloading
            Thread t = new Thread() {
                public void run() {
                    try {
                        if (myService.DownloadRunning()) {
                            mUpdateInfo = myService.getCurrentUpdate();
                        } else {
                            myService.Download(mUpdateInfo);
                        }

                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception on calling DownloadService", e);
                    }
                }
            };
            t.start();
        }

        public void onServiceDisconnected(ComponentName name) {
            try {
                myService.unregisterCallback(mCallback);
            }
            catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
            myService = null;
        }
    };

    private final IDownloadServiceCallback mCallback = new IDownloadServiceCallback.Stub() {
        public void updateDownloadProgress(final long downloaded, final int total,
                final String downloadedText, final String speedText,
                final String remainingTimeText) throws RemoteException {
            mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD_PROGRESS, new DownloadProgress(
                            downloaded, total, downloadedText, speedText, remainingTimeText)));
        }

        public void DownloadFinished(UpdateInfo u) throws RemoteException {
            mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_FINISHED, u));
        }

        public void DownloadError() throws RemoteException {
            mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_ERROR));
        }
    };

    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
    private static final int DOWNLOAD_FINISHED = 2;
    private static final int DOWNLOAD_ERROR = 3;

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_DOWNLOAD_PROGRESS:
                    DownloadProgress dp = (DownloadProgress) msg.obj;
                    updateDownloadProgress(dp.getDownloaded(), dp.getTotal(), dp.getDownloadedText(),
                            dp.getSpeedText(), dp.getRemainingTimeText());
                    break;

                case DOWNLOAD_FINISHED:
                    // Clean up the download service
                    stopService();
                    MyUnbindService(mConnection); 
                    mParent.stopService(serviceIntent);

                    // Set the calling preference to the proper style
                    mCallingPreference.setStyle(UpdatePreference.STYLE_DOWNLOADED);

                    // Trigger the apply update activity
                    Intent i = new Intent(mParent, ApplyUpdate.class);
                    i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) mUpdateInfo);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mParent.startActivity(i);
                    break;

                case DOWNLOAD_ERROR:
                    // Clean up the download service
                    stopService();
                    MyUnbindService(mConnection);
                    mParent.stopService(serviceIntent);

                    // Display the error message to the user
                    Toast.makeText(mParent, R.string.exception_while_downloading,
                            Toast.LENGTH_SHORT).show();

                    // Set the calling preference back to the 'New' style
                    mCallingPreference.setStyle(UpdatePreference.STYLE_NEW);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    };

    private void MyUnbindService(ServiceConnection con) {
        if (mbound) {
            mParent.unbindService(con);
            mbound = false;
            if (DEBUG) Log.d(TAG, "mUpdateDownloaderServiceConnection unbind finished");
        } else if (DEBUG) Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
    }
}
