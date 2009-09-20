package cmupdaterapp.ui;

import java.net.MalformedURLException;

import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IDownloadActivity;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.service.UpdateDownloaderService;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadActivity extends IDownloadActivity
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
	public static UpdateDownloaderService mUpdateDownloaderService;
	private Intent mUpdateDownloaderServiceIntent;
	
	private Resources res;
	
	//Indicates if a Service is bound 
	private boolean mbind = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "onCreate called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);
		mUpdateDownloaderServiceIntent = new Intent(this, UpdateDownloaderService.class);
		res = getResources();
		getWindow().setBackgroundDrawable(res.getDrawable(R.drawable.background));
	}
	
	@Override
	protected void onStart()
	{
		Log.d(TAG, "onStart called");
		super.onStart();
	}
	
	@Override
	protected void onResume()
	{
		Log.d(TAG, "onResume called");
		super.onResume();
		
		UpdateInfo ui = null;
		
		if (mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading())
		{
			Log.d(TAG, "Retrieved update from DownloadService");
			ui = mUpdateDownloaderService.getCurrentUpdate();
			mMirrorName = mUpdateDownloaderService.getCurrentMirrorName();
		}
		else
		{
			Log.d(TAG, "Not downloading");
			Intent i = getIntent();
			if (i!=null)
			{
				Bundle b = i.getExtras();
				if (b!=null)
					ui = (UpdateInfo) b.get(Constants.UPDATE_INFO);	
			}
	
			mUpdateDownloaderServiceIntent.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_DOWNLOAD_UPDATE);
			mUpdateDownloaderServiceIntent.putExtra(Constants.KEY_UPDATE_INFO, ui);
			startService(mUpdateDownloaderServiceIntent);
	
			bindService(mUpdateDownloaderServiceIntent, mUpdateDownloaderServiceConnection, Context.BIND_AUTO_CREATE);
			mbind = true;
		}

		UpdateDownloaderService.setUpdateProcessInfo(DownloadActivity.this);
		
		try
		{
			String[] temp = ui.updateFileUris.get(0).toURL().getFile().split("/");
			mFileName = temp[temp.length-1];
		}
		catch (MalformedURLException e)
		{
			mFileName = "Unable to get Filename";
			Log.e(TAG, "Unable to get Filename", e);
		}
		
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
		((Button)findViewById(R.id.cancel_download_buton)).setOnClickListener(mCancelDownloadListener);
	}
	
	@Override
	protected void onDestroy()
	{
		Log.d(TAG, "onDestroy called");
		if(mUpdateDownloaderService != null && !mUpdateDownloaderService.isDownloading())
		{
			try
			{
				if(mbind)
				{
					unbindService(mUpdateDownloaderServiceConnection);
					Log.d(TAG, "mUpdateDownloaderServiceConnection unbind finished");
					mbind = false;
				}
				else
					Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
			}
			catch (SecurityException ex)
			{
				Log.e(TAG, "Exit App: mUpdateDownloaderServiceConnection unbind failed", ex);
			}
			try
			{
				stopService(mUpdateDownloaderServiceIntent);
			}
			catch (SecurityException ex)
			{
				Log.e(TAG, "Exit App: mUpdateDownloaderServiceIntent could not be Stopped", ex);
			}
		}
		else
			Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
		super.onDestroy();
	}
	
	@Override
	public void updateDownloadProgress(final int downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText)
	{
		if(mProgressBar ==null)return;

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
				mProgressBar.setProgress(downloaded);

				mDownloadedBytesTextView.setText(downloadedText);
				mDownloadSpeedTextView.setText(speedText);
				mRemainingTimeTextView.setText(remainingTimeText);
			}
		});
	}
	
	@Override
	public void updateDownloadMirror(final String mirror)
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
					Log.d(TAG, "Positive Download Cancel Button pressed");
					if (mUpdateDownloaderService!=null)
					{
						mUpdateDownloaderService.cancelDownload();
						Log.d(TAG, "Cancel onClick Event: cancelDownload finished");
					}
					else
						Log.d(TAG, "Cancel Download: mUpdateDownloaderService was NULL");
					try
					{
						stopService(mUpdateDownloaderServiceIntent);
						Log.d(TAG, "stopService(mUpdateDownloaderServiceIntent) finished");
					}
					catch (SecurityException ex)
					{
						Log.e(TAG, "Cancel Download: mUpdateDownloaderServiceIntent could not be Stopped", ex);
					}
					try
					{
						if(mbind)
						{
							unbindService(mUpdateDownloaderServiceConnection);
							Log.d(TAG, "unbindService(mUpdateDownloaderServiceConnection) finished");
							mbind = false;
						}
						else
							Log.d(TAG, "mUpdateDownloaderServiceConnection not bound");
					}
					catch (SecurityException ex)
					{
						Log.e(TAG, "Cancel Download: mUpdateDownloaderServiceConnection unbind failed", ex);
					}
					Log.d(TAG, "Download Cancel Procedure Finished. Switching Layout");
					finish();
				}
			})
			.setNegativeButton(R.string.confirm_download_cancelation_dialog_no, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					Log.d(TAG, "Negative Download Cancel Button pressed");
					dialog.dismiss();
				}
			})
			.show();
		}

	};
	
	private final ServiceConnection mUpdateDownloaderServiceConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			mUpdateDownloaderService = ((UpdateDownloaderService.LocalBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className)
		{
			mUpdateDownloaderService = null;
		}
	};
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		getWindow().setBackgroundDrawable(res.getDrawable(R.drawable.background));
        super.onConfigurationChanged(newConfig); 
        Log.d(TAG, "Orientation Changed. New Orientation: "+newConfig.orientation);
    }
}