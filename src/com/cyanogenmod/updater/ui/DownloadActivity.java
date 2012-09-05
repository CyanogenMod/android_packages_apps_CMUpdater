package com.cyanogenmod.updater.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.cyanogenmod.updater.customTypes.DownloadProgress;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.interfaces.IDownloadService;
import com.cyanogenmod.updater.interfaces.IDownloadServiceCallback;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.utils.Preferences;

import java.io.Serializable;

public class DownloadActivity extends Activity {
    private static final String TAG = "DownloadActivity";

    private Boolean showDebugOutput = false;

    private ProgressBar mProgressBar;
    private TextView mDownloadedBytesTextView;
    private TextView mDownloadSpeedTextView;
    private TextView mRemainingTimeTextView;
    private UpdateInfo ui;
    //Indicates if a Service is bound 
    private boolean mbound = false;
    private Intent serviceIntent;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);		
            showDebugOutput = new Preferences(this).displayDebugOutput();

            if (showDebugOutput) Log.d(TAG, "onCreate called");

            Intent i = getIntent();
            Bundle b = i.getExtras();
            if (b != null) {
                ui = (UpdateInfo) b.get(Constants.KEY_UPDATE_INFO);
                if (showDebugOutput) Log.d(TAG, "Got UpdateInfo from Intent");
            }
            //If no Intent, ui will be null so get the UpdateInfo from State
            if (savedInstanceState != null) {
                ui = savedInstanceState.getParcelable(Constants.KEY_UPDATE_INFO);
                if (showDebugOutput) Log.d(TAG, "Restored UpdateInfo from State");
            } else if (showDebugOutput) Log.d(TAG, "savedInstanceState was null");
            setContentView(R.layout.download);
        }

    @Override
        protected void onResume() {
            if (showDebugOutput) Log.d(TAG, "onResume called");
            super.onResume();

            try {
                if (myService != null && myService.DownloadRunning()) {
                    ui = myService.getCurrentUpdate();
                    myService.registerCallback(mCallback);
                    if (showDebugOutput) Log.d(TAG, "Retrieved update from DownloadService");
                    mbound = true;
                } else {
                    if (showDebugOutput) Log.d(TAG, "Not downloading");
                    serviceIntent = new Intent(IDownloadService.class.getName());
                    ComponentName comp = startService(serviceIntent);
                    if (comp == null)
                        Log.e(TAG, "startService failed");
                    mbound = bindService(serviceIntent, mConnection, 0);
                }
            }
            catch (RemoteException ex) {
                Log.e(TAG, "Error on DownloadService call", ex);
                //Set myService to null, otherwise Mainactivity.onResume will crash,
                //cause the service is not null
                myService = null;
                finish();
            }

            String mFileName = ui.getFileName();

            mProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
            mDownloadedBytesTextView = (TextView) findViewById(R.id.bytes_downloaded_text_view);

            TextView mDownloadFilenameTextView = (TextView) findViewById(R.id.filename_text_view);

            mDownloadSpeedTextView = (TextView) findViewById(R.id.speed_text_view);
            mRemainingTimeTextView = (TextView) findViewById(R.id.remaining_time_text_view);

            if (mFileName != null)
                mDownloadFilenameTextView.setText(mFileName);
            findViewById(R.id.cancel_download_button).setOnClickListener(mCancelDownloadListener);
        }

    @Override
        protected void onDestroy() {
            if (showDebugOutput) Log.d(TAG, "onDestroy called");
            try {
                if (myService != null && !myService.DownloadRunning()) {
                    MyUnbindService(mConnection);
                } else if (showDebugOutput) Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
            }
            catch (RemoteException e) {
                Log.e(TAG, "Exception on calling DownloadService", e);
            }
            boolean stopped = stopService(serviceIntent);
            if (showDebugOutput) Log.d(TAG, "DownloadService stopped: " + stopped);
            super.onDestroy();
        }

    private void updateDownloadProgress(final long downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText) {
        if (mProgressBar == null) return;

        mProgressBar.post(new Runnable() {
                public void run() {
                if (total < 0) {
                mProgressBar.setIndeterminate(true);
                } else {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setMax(total);
                }
                mProgressBar.setProgress((int) downloaded);

                mDownloadedBytesTextView.setText(downloadedText);
                mDownloadSpeedTextView.setText(speedText);
                mRemainingTimeTextView.setText(remainingTimeText);
                }
                });
    }

    private final View.OnClickListener mCancelDownloadListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            new AlertDialog.Builder(DownloadActivity.this)
                .setMessage(R.string.confirm_download_cancelation_dialog_message)
                .setPositiveButton(R.string.confirm_download_cancelation_dialog_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        if (showDebugOutput) Log.d(TAG, "Positive Download Cancel Button pressed");
                        if (myService != null) {
                        try {
                        myService.cancelDownload();
                        }
                        catch (RemoteException e) {
                        Log.e(TAG, "Exception on calling DownloadService", e);
                        }
                        if (showDebugOutput) Log.d(TAG, "Cancel onClick Event: cancelDownload finished");
                        } else if (showDebugOutput)
                        Log.d(TAG, "Cancel Download: mUpdateDownloaderService was NULL");

                        MyUnbindService(mConnection);

                        if (showDebugOutput) Log.d(TAG, "Download Cancel Procedure Finished. Switching Layout");
                        myService = null;
                        finish();
                        }
                        })
            .setNegativeButton(R.string.confirm_download_cancelation_dialog_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    if (showDebugOutput) Log.d(TAG, "Negative Download Cancel Button pressed");
                    dialog.dismiss();
                    }
                    })
            .show();
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
                        if (myService.DownloadRunning())
                            ui = myService.getCurrentUpdate();
                        else
                            myService.Download(ui);
                    }
                    catch (RemoteException e) {
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
                    updateDownloadProgress(dp.getDownloaded(), dp.getTotal(), dp.getDownloadedText(), dp.getSpeedText(), dp.getRemainingTimeText());
                    break;
                case DOWNLOAD_FINISHED:
                    UpdateInfo u = (UpdateInfo) msg.obj;
                    Intent i = new Intent(DownloadActivity.this, ApplyUpdateActivity.class);
                    i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable) u);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                    MyUnbindService(mConnection); 
                    stopService(serviceIntent);
                    break;
                case DOWNLOAD_ERROR:
                    Toast.makeText(DownloadActivity.this, R.string.exception_while_downloading, Toast.LENGTH_LONG).show();
                    Intent i2 = new Intent(DownloadActivity.this, MainActivity.class);
                    i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i2);
                    finish();
                    MyUnbindService(mConnection);
                    stopService(serviceIntent);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                try {
                    //Disable the Back Key when Download is running
                    return !(myService != null && myService.DownloadRunning());
                }
                catch (RemoteException e) {
                    Log.e(TAG, "Exception on calling DownloadService", e);
                }
            }
            return super.dispatchKeyEvent(event);
        }

    //When the Activity is killed, save the UpdateInfo to state so we can restore it

    @Override
        protected void onRestoreInstanceState(Bundle outState) {
            if (myService != null) {
                serviceIntent = new Intent(IDownloadService.class.getName());
                ComponentName comp = startService(serviceIntent);
                if (comp == null)
                    Log.e(TAG, "startService failed");
                mbound = bindService(serviceIntent, mConnection, 0);
            }
        }
    @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            Log.d(TAG, "Called onSaveInstanceState");
            if (myService != null) {
                MyUnbindService(mConnection);
            } else if (showDebugOutput) Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
            outState.putParcelable(Constants.KEY_UPDATE_INFO, ui);
        }

    private void MyUnbindService(ServiceConnection con) {
        if (mbound) {
            unbindService(con);
            mbound = false;
            if (showDebugOutput) Log.d(TAG, "mUpdateDownloaderServiceConnection unbind finished");
        } else if (showDebugOutput) Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
    }
}
