package cmupdaterapp.ui;

import java.io.Serializable;

import cmupdaterapp.customTypes.DownloadProgress;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IDownloadService;
import cmupdaterapp.interfaces.IDownloadServiceCallback;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadActivity extends Activity
{
	private static final String TAG = "DownloadActivity";

	private ProgressBar mProgressBar;
	private TextView mDownloadedBytesTextView;
	private TextView mDownloadMirrorTextView;
	private TextView mDownloadFilenameTextView;
	private TextView mDownloadSpeedTextView;
	private TextView mRemainingTimeTextView;
	private String mMirrorName;
	private String mFileName;
	private Button mStopButton;
	private Button mResumeButton;
	private UpdateInfo ui;
	//Indicates if a Service is bound 
	private boolean mbound = false;
	private Intent serviceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if (MainActivity.showDebugOutput) Log.d(TAG, "onCreate called");
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		if (b!=null)
		{
			ui = (UpdateInfo) b.get(Constants.KEY_UPDATE_INFO);
			if (MainActivity.showDebugOutput) Log.d(TAG, "Got UpdateInfo from Intent");
		}
		//If no Intent, ui will be null so get the UpdateInfo from State
		if (savedInstanceState != null)
		{
			ui = savedInstanceState.getParcelable(Constants.KEY_UPDATE_INFO);
			if (MainActivity.showDebugOutput) Log.d(TAG, "Restored UpdateInfo from State");
		}
		else
			if (MainActivity.showDebugOutput) Log.d(TAG, "savedInstanceState was null");
		setContentView(R.layout.download);
	}

	@Override
	protected void onResume()
	{
		if (MainActivity.showDebugOutput) Log.d(TAG, "onResume called");
		super.onResume();

		try
		{
			if (myService != null && myService.DownloadRunning())
			{
				ui = myService.getCurrentUpdate();
				myService.registerCallback(mCallback);
				if (MainActivity.showDebugOutput) Log.d(TAG, "Retrieved update from DownloadService");
				mMirrorName = myService.getCurrentMirrorName();
				mbound = true;
			}
			else
			{
				if (MainActivity.showDebugOutput) Log.d(TAG, "Not downloading");
				serviceIntent = new Intent(IDownloadService.class.getName());
				ComponentName comp = startService(serviceIntent);
				if (comp == null)
					Log.e(TAG, "startService failed");
				mbound = bindService(serviceIntent, mConnection, 0);
			}
		}
		catch (RemoteException ex)
		{
			Log.e(TAG, "Error on DownloadService call", ex);
		}

		mFileName = ui.getFileName();

		mProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
		mDownloadedBytesTextView = (TextView) findViewById(R.id.bytes_downloaded_text_view);

		mDownloadMirrorTextView = (TextView) findViewById(R.id.mirror_text_view);

		mDownloadFilenameTextView = (TextView) findViewById(R.id.filename_text_view);

		mDownloadSpeedTextView = (TextView) findViewById(R.id.speed_text_view);
		mRemainingTimeTextView = (TextView) findViewById(R.id.remaining_time_text_view);

		if(mMirrorName != null)
			mDownloadMirrorTextView.setText(mMirrorName);
		if(mFileName != null)
			mDownloadFilenameTextView.setText(mFileName);
		((Button)findViewById(R.id.cancel_download_button)).setOnClickListener(mCancelDownloadListener);
		mStopButton = (Button)findViewById(R.id.stop_download_button);
		mResumeButton = (Button)findViewById(R.id.resume_download_button);
		mStopButton.setOnClickListener(mStopDownloadListener);
		mResumeButton.setOnClickListener(mResumeDownloadListener);
	}

	@Override
	protected void onDestroy()
	{
		if (MainActivity.showDebugOutput) Log.d(TAG, "onDestroy called");
		try
		{
			if(myService != null && !myService.DownloadRunning())
			{
				if(mbound)
				{
					unbindService(mConnection);
					if (MainActivity.showDebugOutput) Log.d(TAG, "mUpdateDownloaderServiceConnection unbind finished");
					mbound = false;
				}
				else
					if (MainActivity.showDebugOutput) Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
			}
			else
				if (MainActivity.showDebugOutput) Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
		}
		catch (RemoteException e)
		{
			Log.e(TAG, "Exception on calling DownloadService", e);
		}
		boolean stopped = stopService(serviceIntent);
		if (MainActivity.showDebugOutput) Log.d(TAG, "DownloadService stopped: " + stopped);
		super.onDestroy();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	private void updateDownloadProgress(final long downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText)
	{
		if(mProgressBar == null) return;

		mProgressBar.post(new Runnable()
		{
			public void run()
			{
				if(total < 0)
				{
					mProgressBar.setIndeterminate(true);
				}
				else
				{
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

	private void updateDownloadMirror(final String mirror)
	{
		if(mDownloadMirrorTextView == null) return;

		mDownloadMirrorTextView.post(new Runnable()
		{
			public void run()
			{
				mDownloadMirrorTextView.setText(mirror);
				mMirrorName = mirror;
			}
		});
	}

	private final View.OnClickListener mCancelDownloadListener = new View.OnClickListener()
	{
		public void onClick(View arg0)
		{
			new AlertDialog.Builder(DownloadActivity.this)
			.setMessage(R.string.confirm_download_cancelation_dialog_message)
			.setPositiveButton(R.string.confirm_download_cancelation_dialog_yes, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					if (MainActivity.showDebugOutput) Log.d(TAG, "Positive Download Cancel Button pressed");
					if (myService!=null)
					{
						try
						{
							myService.cancelDownload();
						}
						catch (RemoteException e)
						{
							Log.e(TAG, "Exception on calling DownloadService", e);
						}
						if (MainActivity.showDebugOutput) Log.d(TAG, "Cancel onClick Event: cancelDownload finished");
					}
					else
						if (MainActivity.showDebugOutput) Log.d(TAG, "Cancel Download: mUpdateDownloaderService was NULL");
					if(mbound)
					{
						unbindService(mConnection);
						if (MainActivity.showDebugOutput) Log.d(TAG, "unbindService(mUpdateDownloaderServiceConnection) finished");
						mbound = false;
					}
					else
						if (MainActivity.showDebugOutput) Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
					if (MainActivity.showDebugOutput) Log.d(TAG, "Download Cancel Procedure Finished. Switching Layout");
					finish();
				}
			})
			.setNegativeButton(R.string.confirm_download_cancelation_dialog_no, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					if (MainActivity.showDebugOutput) Log.d(TAG, "Negative Download Cancel Button pressed");
					dialog.dismiss();
				}
			})
			.show();
		}
	};
	
	private final View.OnClickListener mStopDownloadListener = new View.OnClickListener()
	{
		public void onClick(View arg0)
		{
			//TODO: Pause Download Button Listener
			mResumeButton.setVisibility(View.VISIBLE);
			mStopButton.setVisibility(View.GONE);
		}
	};
	
	private final View.OnClickListener mResumeDownloadListener = new View.OnClickListener()
	{
		public void onClick(View arg0)
		{
			//TODO: Resume Download Button Listener
			mStopButton.setVisibility(View.VISIBLE);
			mResumeButton.setVisibility(View.GONE);
		}
	};

	public static IDownloadService myService;
	
	/**
	 * Class for interacting with the main interface of the service.
	 */
    private ServiceConnection mConnection = new ServiceConnection()
    {
    	public void onServiceConnected(ComponentName name, IBinder service)
    	{
    		myService = IDownloadService.Stub.asInterface(service);
    		try
    		{
    			myService.registerCallback(mCallback);
    		}
    		catch (RemoteException e)
    		{ }
    		//Start Downloading
    		Thread t = new Thread()
            {
    			public void run()
    			{
	    			try
	    			{
	    				if (myService.DownloadRunning())
	    					ui = myService.getCurrentUpdate();
	    				else
	    					myService.Download(ui);
	    			}
	    			catch (RemoteException e)
	    			{
	    				Log.e(TAG, "Exception on calling DownloadService", e);
	    			}
    			}
            };
            t.start();
    	}
    	public void onServiceDisconnected(ComponentName name)
    	{
    		try
    		{
    			myService.unregisterCallback(mCallback);
    		}
    		catch (RemoteException e)
    		{ }
    		myService = null;
    	}
    };

	private IDownloadServiceCallback mCallback = new IDownloadServiceCallback.Stub()
	{
		public void updateDownloadProgress(final long downloaded, final int total,
				final String downloadedText, final String speedText,
				final String remainingTimeText) throws RemoteException
				{
			mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD_PROGRESS, new DownloadProgress(
					downloaded, total, downloadedText, speedText, remainingTimeText)));
		}

		public void UpdateDownloadMirror(String mirror) throws RemoteException
		{
			mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD_MIRROR, mirror));
		}

		public void DownloadFinished(UpdateInfo u) throws RemoteException
		{
			mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_FINISHED, u));
		}

		public void DownloadError() throws RemoteException
		{
			mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_ERROR));
		}
	};

	private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
	private static final int UPDATE_DOWNLOAD_MIRROR = 2;
	private static final int DOWNLOAD_FINISHED = 3;
	private static final int DOWNLOAD_ERROR = 4;

	private Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case UPDATE_DOWNLOAD_PROGRESS:
					DownloadProgress dp = (DownloadProgress) msg.obj;
					updateDownloadProgress(dp.getDownloaded(), dp.getTotal(), dp.getDownloadedText(), dp.getSpeedText(), dp.getRemainingTimeText());
					break;
				case UPDATE_DOWNLOAD_MIRROR:
					updateDownloadMirror((String)msg.obj);
					break;
				case DOWNLOAD_FINISHED:
					UpdateInfo u = (UpdateInfo) msg.obj;
					Intent i = new Intent(DownloadActivity.this, ApplyUpdateActivity.class);
					i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)u);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(i);
					finish();
					break;
				case DOWNLOAD_ERROR:
					Toast.makeText(DownloadActivity.this, R.string.exception_while_downloading, Toast.LENGTH_LONG).show();
					Intent i2 = new Intent(DownloadActivity.this, MainActivity.class);
					i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(i2);
					finish();
					break;
				default:
					super.handleMessage(msg);
			}
		}
	};

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		int keyCode = event.getKeyCode();
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
	    {
	    	try
	    	{
	    		//Disable the Back Key when Download is running
				if (myService != null && myService.DownloadRunning())
					return false;
				else
					return true;
			}
	    	catch (RemoteException e)
	    	{
	    		Log.e(TAG, "Exception on calling DownloadService", e);
			}
	    }
	    return super.dispatchKeyEvent(event);
	}

	//When the Activity is killed, save the UpdateInfo to state so we can restore it
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (MainActivity.showDebugOutput) Log.d(TAG, "Called onSaveInstanceState");
		outState.putParcelable(Constants.KEY_UPDATE_INFO, ui);
	}
}