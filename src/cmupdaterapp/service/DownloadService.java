package cmupdaterapp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IDownloadService;
import cmupdaterapp.interfaces.IDownloadServiceCallback;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.ApplyUpdateActivity;
import cmupdaterapp.ui.DownloadActivity;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;
import cmupdaterapp.utils.MD5;
import cmupdaterapp.utils.Preferences;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DownloadService extends Service
{
	private static final String TAG = "DownloadService";

	private final RemoteCallbackList<IDownloadServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadServiceCallback>();
	
	private boolean prepareForDownloadCancel;
	private boolean mMirrorNameUpdated;
	private String mMirrorName;
	private boolean mDownloading = false;
	private UpdateInfo mCurrentUpdate;
	private WifiLock mWifiLock;
	private volatile int mtotalDownloaded;
	private int mcontentLength;
	private long mStartTime;
	private String minutesString;
	private String secondsString;
	private String fullUpdateFolderPath;
	private Context AppContext;
	private Resources res;
	private ConnectivityManager mConnectivityManager;
	private ConnectionChangeReceiver myConnectionChangeReceiver;
	private boolean connected;
	private boolean isPaused;
	private DownloadStatus status;
	private String fileName;
	private boolean resumeSupported = true;
	private long localFileSize = 0;
	
	private enum DownloadStatus
	{
	    WAITING_CONNECTION,
	    READY,
	    DOWNLOADING,
	    PAUSED,
	    FINISHED,
	    CANCELLED,
	    ERROR
	}
	
    @Override
    public IBinder onBind(Intent arg0)
    {
            return mBinder;
    }

    private final IDownloadService.Stub mBinder = new IDownloadService.Stub()
    {
		public void Download(UpdateInfo ui) throws RemoteException
		{
			mDownloading = true;
			boolean success = checkForConnectionAndUpdate(ui);
			notifyUser(ui, success);
			mDownloading = false;
		}
		public boolean DownloadRunning() throws RemoteException
		{
			return mDownloading;
		}
		public boolean PauseDownload() throws RemoteException
		{
			// TODO Auto-generated method stub
			return false;
		}
		public boolean ResumeDownload() throws RemoteException
		{
			// TODO Auto-generated method stub
			return false;
		}
		public boolean cancelDownload() throws RemoteException
		{
			// TODO Auto-generated method stub
			return cancelCurrentDownload();
		}
		public UpdateInfo getCurrentUpdate() throws RemoteException
		{
			return mCurrentUpdate;
		}
		public String getCurrentMirrorName() throws RemoteException
		{
			return mMirrorName;
		}
		public boolean isPaused() throws RemoteException
		{
			return isPaused;
		}
		public void registerCallback(IDownloadServiceCallback cb)
				throws RemoteException {
			if (cb != null) mCallbacks.register(cb);
			
		}
		public void unregisterCallback(IDownloadServiceCallback cb)
				throws RemoteException {
			if (cb != null) mCallbacks.unregister(cb);
			
		}
    };
    
    @Override
	public void onCreate()
    {
    	Log.d(TAG, "Download Service Created");

		mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock("CM Updater");

		fullUpdateFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Preferences.getPreferences(this).getUpdateFolder();

		AppContext = getApplicationContext();
		res = getResources();
		minutesString = res.getString(R.string.minutes);
		secondsString = res.getString(R.string.seconds);
		
		mConnectivityManager = (ConnectivityManager) AppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		myConnectionChangeReceiver = new ConnectionChangeReceiver();
		registerReceiver(myConnectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		android.net.NetworkInfo.State state = mConnectivityManager.getActiveNetworkInfo().getState();
		connected = (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.SUSPENDED);
		status = DownloadStatus.WAITING_CONNECTION;
    }
    
    @Override
    public void onDestroy()
    {
    	mCallbacks.kill();
    	unregisterReceiver(myConnectionChangeReceiver);
    	super.onDestroy();
    }
    
	
    
    private boolean checkForConnectionAndUpdate(UpdateInfo updateToDownload)
	{
    	mCurrentUpdate = updateToDownload;
		Log.d(TAG, "Called CheckForConnectionAndUpdate");
		boolean success;
		mWifiLock.acquire();
		
		//wait for a data connection
		while(!connected)
		{
			Log.d(TAG, "No data connection, waiting for a data connection");
			synchronized (mConnectivityManager)
			{
				try
				{
					mConnectivityManager.wait();
					break;
				}
				catch (InterruptedException e)
				{
					Log.e(TAG, "Error in TelephonyManager.wait", e);
				}
			}
		}

		try
		{
			Log.d(TAG, "Downloading update...");
			success = downloadFile(updateToDownload);
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "RuntimeEx while checking for updates", ex);
			notificateDownloadError();
			return false;
		} catch (IOException ex)
		{
			Log.e(TAG, "Exception while downloading update", ex);
			notificateDownloadError();
			return false;
		}
		finally
		{
			mWifiLock.release();
		}
		
		//Be sure to return false if the User canceled the Download
		if(prepareForDownloadCancel)
			return false;
		else
			return success;
	}

	private boolean downloadFile(UpdateInfo updateInfo) throws IOException
	{
		Log.d(TAG, "Called downloadFile");
		HttpClient httpClient = new DefaultHttpClient();
		HttpClient MD5httpClient = new DefaultHttpClient();

		HttpUriRequest req, md5req;
		HttpResponse response, md5response;
		
		List<URI> updateMirrors = updateInfo.updateFileUris;
		int size = updateMirrors.size();
		int start = new Random().nextInt(size);
		Log.d(TAG, "Mirrorcount: " + size);
		URI updateURI;
		File destinationFile = null;
		File partialDestinationFile = null;
		File destinationMD5File = null;
		String downloadedMD5 = null;
		
		//If directory not exists, create it
		File directory = new File(fullUpdateFolderPath);
		if (!directory.exists())
		{
			directory.mkdirs();
			Log.d(TAG, "UpdateFolder created");
		}

		fileName = updateInfo.getFileName(); 
		if (null == fileName || fileName.length() < 1)
		{
			fileName = "update.zip";
		}
		Log.d(TAG, "fileName: " + fileName);
		
		//Set the Filename to update.zip.partial
		partialDestinationFile = new File(fullUpdateFolderPath, fileName + ".partial");
		destinationFile = new File(fullUpdateFolderPath, fileName);
		if(partialDestinationFile.exists()) 
             localFileSize = partialDestinationFile.length(); 

		//For every Mirror
		for(int i = 0; i < size; i++)
		{
			resumeSupported = false;
			if (!prepareForDownloadCancel)
			{
				updateURI = updateMirrors.get((start + i)% size);
				mMirrorName = updateURI.getHost();
				Log.d(TAG, "Mirrorname: " + mMirrorName);
				
				boolean md5Available = true;
	
				mMirrorNameUpdated = false;
				try
				{
					req = new HttpGet(updateURI);
					md5req = new HttpGet(updateURI+".md5sum");
					
					// Add no-cache Header, so the File gets downloaded each time
					req.addHeader("Cache-Control", "no-cache");
					md5req.addHeader("Cache-Control", "no-cache");
	
					Log.d(TAG, "Trying to download md5sum file from " + md5req.getURI());
					md5response = MD5httpClient.execute(md5req);
					Log.d(TAG, "Trying to download update zip from " + req.getURI());
	
					if (localFileSize > 0)  
	                    req.addHeader("Range", "bytes=" + localFileSize + "-"); 
					response = httpClient.execute(req);
					
					int serverResponse = response.getStatusLine().getStatusCode();
					int md5serverResponse = md5response.getStatusLine().getStatusCode();
					
					if (serverResponse == HttpStatus.SC_NOT_FOUND)
					{
						Log.d(TAG, "File not found on Server. Trying next one.");
					}
					else if(serverResponse != HttpStatus.SC_OK && serverResponse != HttpStatus.SC_PARTIAL_CONTENT)
					{
						Log.d(TAG, "Server returned status code " + serverResponse + " for update zip trying next mirror");
					}
					else
					{
						// server must support partial content for resume
						if (localFileSize > 0 && serverResponse != HttpStatus.SC_PARTIAL_CONTENT)
						{
							//TODO: continue to next mirror if resume not supported
							Log.d(TAG, "Resume not supported");
							ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_not_supported, 0));
							resumeSupported = false;
						}
						else if (localFileSize > 0 && serverResponse == HttpStatus.SC_PARTIAL_CONTENT)
						{
							resumeSupported = true;
							Log.d(TAG, "Resume supported");
							ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_download, 0));
						}

						if (md5serverResponse != HttpStatus.SC_OK)
						{
							md5Available = false;
							Log.d(TAG, "Server returned status code " + md5serverResponse + " for update zip md5sum. Downloading without it");
						}
	
						if (md5Available)
						{
							destinationMD5File = new File(fullUpdateFolderPath, fileName + ".md5sum");
							if(destinationMD5File.exists()) destinationMD5File.delete();
	
							try
							{
								Log.d(TAG, "Trying to Read MD5 hash from response");
								HttpEntity temp = md5response.getEntity();
								InputStreamReader isr = new InputStreamReader(temp.getContent());
								BufferedReader br = new BufferedReader(isr);
								downloadedMD5 = br.readLine().split("  ")[0];
								Log.d(TAG, "MD5: " + downloadedMD5);
								br.close();
								isr.close();
	
								if (temp != null)
									temp.consumeContent();
	
								//Write the String in a .md5 File
								if (downloadedMD5 != null && !downloadedMD5.equals(""))
								{
									writeMD5(destinationMD5File, downloadedMD5);
								}
							}
							catch (IOException e)
							{
								Log.e(TAG, "Exception while reading MD5 response: ", e);
								throw new IOException("MD5 Response cannot be read");
							}
						}
	
						
						// Download Update ZIP if md5sum went ok
						HttpEntity entity = response.getEntity();
						dumpFile(entity, partialDestinationFile, destinationFile);
						//Was the download canceled?
						if(prepareForDownloadCancel)
						{
							Log.d(TAG, "Download was canceled. Break the for loop");
							break;
						}
						if (entity != null && !prepareForDownloadCancel)
						{
							Log.d(TAG, "Consuming entity....");
							entity.consumeContent();
							Log.d(TAG, "Entity consumed");
						}
						else
						{
							Log.d(TAG, "Entity resetted to NULL");
							entity = null;
						}
						Log.d(TAG, "Update download finished");
						
						if (md5Available)
						{
							Log.d(TAG, "Performing MD5 verification");
							if(!MD5.checkMD5(downloadedMD5, destinationFile))
							{
								throw new IOException(res.getString(R.string.md5_verification_failed));
							}
						}
	
						//If we reach here, download & MD5 check went fine :)
						return true;
					}
				}
				catch (IOException ex)
				{
					ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
					Log.e(TAG, "An error occured while downloading the update file. Trying next mirror", ex);
				}
				if(Thread.currentThread().isInterrupted() || !Thread.currentThread().isAlive())
					break;
			}
			else
			{
				Log.d(TAG, "Not trying any more mirrors, download canceled");
				break;
			}
		}
		ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
		Log.d(TAG, "Unable to download the update file from any mirror");

//		if (null != partialDestinationFile && partialDestinationFile.exists())
//		{
//			partialDestinationFile.delete();
//		}
//		if (null != destinationMD5File && destinationMD5File.exists())
//		{
//			destinationMD5File.delete();
//		}

		return false;
	}

	private void dumpFile(HttpEntity entity, File partialDestinationFile, File destinationFile) throws IOException
	{
		Log.d(TAG, "DumpFile Called");
		if(!prepareForDownloadCancel)
		{
			mcontentLength = (int) entity.getContentLength();
			if(mcontentLength <= 0)
			{
				Log.d(TAG, "unable to determine the update file size, Set ContentLength to 1024");
				mcontentLength = 1024;
			}
			else
				Log.d(TAG, "Update size: " + (mcontentLength/1024) + "KB" );
	
			mStartTime = System.currentTimeMillis(); 
	
			byte[] buff = new byte[1024];
			int read = 0;
			RandomAccessFile out = new RandomAccessFile(partialDestinationFile, "rw");
			out.seek(localFileSize);
			InputStream is = entity.getContent();
			TimerTask progressUpdateTimerTask = new TimerTask()
			{
				@Override
				public void run()
				{
					onProgressUpdate();
				}
			};
			Timer progressUpdateTimer = new Timer();
			status = DownloadStatus.DOWNLOADING;
			try
			{
				mtotalDownloaded = 0;
				progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask, 100, Preferences.getPreferences(this).getProgressUpdateFreq());
				while((read = is.read(buff)) > 0 && !prepareForDownloadCancel)
				{
					out.write(buff, 0, read);
					mtotalDownloaded += read;
				}

				out.close();
				is.close();
				if (!prepareForDownloadCancel)
				{
					partialDestinationFile.renameTo(destinationFile);
					status = DownloadStatus.FINISHED;
					Log.d(TAG, "Download finished");
				}
				else
				{
					status = DownloadStatus.CANCELLED;
					Log.d(TAG, "Download cancelled");
				}
			}
			catch(IOException e)
			{
				out.close();
				status = DownloadStatus.ERROR;
				try
				{
					destinationFile.delete();
				}
				catch (SecurityException ex)
				{
					Log.e(TAG, "Unable to delete downloaded File. Continue anyway.", ex);
				}
			}
			finally
			{
				progressUpdateTimer.cancel();
				buff = null;
			}
		}
		else
			Log.d(TAG, "Download Cancel in Progress. Don't start Downloading");
	}

	private void writeMD5(File md5File, String md5) throws IOException
	{
		Log.d(TAG, "Writing the calculated MD5 to disk");
		FileWriter fw = new FileWriter(md5File);
		try
		{
			fw.write(md5);
			fw.flush();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Exception while writing MD5 to disk", e);
		}
		finally
		{
			fw.close();
		}
	}

	private void onProgressUpdate()
	{
		//Only update the Notification and DownloadLayout, when no downloadcancel is in progress, so the notification will not pop up again
		if (!prepareForDownloadCancel)
		{
			// Shows Downloadstatus in Notificationbar. Initialize the Variables
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification mNotification = new Notification(R.drawable.icon_notification, res.getString(R.string.notification_tickertext), System.currentTimeMillis());
			mNotification.flags = Notification.FLAG_NO_CLEAR;
			mNotification.flags = Notification.FLAG_ONGOING_EVENT;
			RemoteViews mNotificationRemoteView = new RemoteViews(getPackageName(), R.layout.notification);
			Intent mNotificationIntent = new Intent(this, DownloadActivity.class);
			PendingIntent mNotificationContentIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, 0);
			mNotification.contentView = mNotificationRemoteView;
			mNotification.contentIntent = mNotificationContentIntent;
			int speed = (mtotalDownloaded/(int)(System.currentTimeMillis() - mStartTime));
			speed = (speed > 0) ? speed : 1;
			int remainingTime = ((mcontentLength - mtotalDownloaded)/speed);
			String stringDownloaded = mtotalDownloaded/1048576 + "/" + mcontentLength/1048576 + " MB";
			String stringSpeed = speed + " kB/s";
			String stringRemainingTime = remainingTime/60000 + " " + minutesString + " " + remainingTime%60 + " " + secondsString;

			String stringComplete = stringDownloaded + " " + stringSpeed + " " + stringRemainingTime;
			
			mNotificationRemoteView.setTextViewText(R.id.notificationTextDownloadInfos, stringComplete);
			mNotificationRemoteView.setProgressBar(R.id.notificationProgressBar, mcontentLength, mtotalDownloaded, false);
			mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_STATUS, mNotification);

			if(!mMirrorNameUpdated)
			{
				UpdateDownloadMirror(mMirrorName);
				mMirrorNameUpdated = true;
			}
			//Update the DownloadProgress
			UpdateDownloadProgress(mtotalDownloaded, mcontentLength, stringDownloaded, stringSpeed, stringRemainingTime);
		}
		else
			Log.d(TAG, "Downloadcancel in Progress. Not updating the Notification and DownloadLayout");
	}

	private void notifyUser(UpdateInfo ui, boolean success)
	{
		Log.d(TAG, "Called Notify User");
		Intent i;
		
		if(!success)
		{
			Log.d(TAG, "Downloaded Update was NULL");
			DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
			DownloadError();
			return;
		}
		
		i = new Intent(this, ApplyUpdateActivity.class);
		i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)ui);
		
		//Set the Notification to finished
		DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification mNotification = new Notification(R.drawable.icon_notification, res.getString(R.string.notification_tickertext), System.currentTimeMillis());
		Intent mNotificationIntent = new Intent(this, MainActivity.class);
		PendingIntent mNotificationContentIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, 0);
		mNotification = new Notification(R.drawable.icon, res.getString(R.string.notification_finished), System.currentTimeMillis());
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotificationContentIntent = PendingIntent.getActivity(AppContext, 0, i, 0);
		mNotification.setLatestEventInfo(AppContext, res.getString(R.string.app_name), res.getString(R.string.notification_finished), mNotificationContentIntent);
		Uri notificationRingtone = Preferences.getPreferences(AppContext).getConfiguredRingtone();
		if(Preferences.getPreferences(AppContext).getVibrate())
			mNotification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
		else
			mNotification.defaults = Notification.DEFAULT_LIGHTS;
		if(notificationRingtone == null)
		{
			mNotification.sound = null;
		}
		else
		{
			mNotification.sound = notificationRingtone;
		}
		mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINISHED, mNotification);
		
		DownloadFinished();
	}
	
    private void notificateDownloadError()
	{
    	mDownloading = false;
		Intent i = new Intent(this, MainActivity.class)
		.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_DOWNLOAD_FAILED);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
				PendingIntent.FLAG_ONE_SHOT);

		Notification notification = new Notification(android.R.drawable.stat_notify_error,
				res.getString(R.string.not_update_download_error_ticker),
				System.currentTimeMillis());

		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(
				this,
				res.getString(R.string.not_update_download_error_title),
				res.getString(R.string.not_update_download_error_body),
				contentIntent);

		Uri notificationRingtone = Preferences.getPreferences(this).getConfiguredRingtone();
		if(Preferences.getPreferences(this).getVibrate())
			notification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
		else
			notification.defaults = Notification.DEFAULT_LIGHTS;
		if(notificationRingtone == null)
		{
			notification.sound = null;
		}
		else
		{
			notification.sound = notificationRingtone;
		}

		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(R.string.not_update_download_error_title, notification);
	}
	
	private void DeleteDownloadStatusNotification(int id)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(id);
		Log.d(TAG, "Download Notification removed");
	}
	
	private void UpdateDownloadProgress(final int downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText)
	{
		final int N = mCallbacks.beginBroadcast();
		for (int i=0; i<N; i++)
		{
			try
			{
				mCallbacks.getBroadcastItem(i).updateDownloadProgress(downloaded, total, downloadedText, speedText, remainingTimeText);
			}
			catch (RemoteException e)
			{
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	private void UpdateDownloadMirror(String mirrorName)
	{
		final int M = mCallbacks.beginBroadcast();
		for (int i=0; i<M; i++)
		{
			try
			{
				mCallbacks.getBroadcastItem(i).UpdateDownloadMirror(mirrorName);
			}
			catch (RemoteException e)
			{
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	private void DownloadFinished()
	{
		final int M = mCallbacks.beginBroadcast();
		for (int i=0; i<M; i++)
		{
			try
			{
				mCallbacks.getBroadcastItem(i).DownloadFinished(mCurrentUpdate);
			}
			catch (RemoteException e)
			{
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	private void DownloadError()
	{
		final int M = mCallbacks.beginBroadcast();
		for (int i=0; i<M; i++)
		{
			try
			{
				mCallbacks.getBroadcastItem(i).DownloadError();
			}
			catch (RemoteException e)
			{
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	private boolean cancelCurrentDownload()
	{
		prepareForDownloadCancel = true;
		Log.d(TAG, "Download Service CancelDownload was called");
		DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
		File update = new File(fullUpdateFolderPath + "/" + mCurrentUpdate.getFileName());
		File md5sum = new File(fullUpdateFolderPath + "/" + mCurrentUpdate.getFileName() + ".md5sum");
		if(update.exists())
		{
			update.delete();
			Log.d(TAG, update.getAbsolutePath() + " deleted");
		}
		if(md5sum.exists())
		{
			md5sum.delete();
			Log.d(TAG, md5sum.getAbsolutePath() + " deleted");
		}
		mDownloading = false;
		Log.d(TAG, "Download Cancel StopSelf was called");
		stopSelf();
		return true;
	}
	
	private Handler ToastHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if (msg.arg1 != 0)
				Toast.makeText(DownloadService.this, msg.arg1, Toast.LENGTH_LONG).show();
			else
				Toast.makeText(DownloadService.this, (String)msg.obj, Toast.LENGTH_LONG).show();
		}
	};
	
	//Is called when Network Connection Changes
	class ConnectionChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			android.net.NetworkInfo.State state = mConnectivityManager.getActiveNetworkInfo().getState();
			connected = (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.SUSPENDED);
		}
	}
}