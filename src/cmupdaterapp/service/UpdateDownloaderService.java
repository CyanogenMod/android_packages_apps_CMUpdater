package cmupdaterapp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

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
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IDownloadActivity;
import cmupdaterapp.ui.ApplyUpdateActivity;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;
import cmupdaterapp.utils.MD5;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.misc.Log;

public class UpdateDownloaderService extends Service
{
	//public static UpdateDownloaderService INSTANCE;

	private static final String TAG = "UpdateDownloaderService";
	
	private int progressBarUpdateInterval;
	
	private boolean prepareForDownloadCancel;

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private HandlerThread mHandlerThread;
	private NotificationManager mNM;
	private boolean mWaitingForDataConnection = false;
	private File mDestinationFile;
	private File mDestinationMD5File;
	private DefaultHttpClient mHttpClient;
	private DefaultHttpClient mMD5HttpClient;
	private Random mRandom;
	private boolean mMirrorNameUpdated;
	private String mMirrorName;
	private String mFileName;

	private boolean mDownloading = false;
	private UpdateInfo mCurrentUpdate;
	private ConnectivityManager mConnectivityManager;
	private IntentFilter mConectivityManagerIntentFilter;
	
	private final IBinder mBinder = new LocalBinder();

	private static IDownloadActivity DOWNLOAD_ACTIVITY;

	private WifiLock mWifiLock;
	private WifiManager mWifiManager;

	private String mUpdateFolder;
	private String mDownloadedMD5;

	private volatile int mtotalDownloaded;
	private int mSpeed;
	private long mRemainingTime;
	private String mstringDownloaded;
	private String mstringSpeed;
	private String mstringRemainingTime;
	private String mstringComplete;
	
	private int mcontentLength;
	private long mStartTime;
	
	private String minutesString;
	private String secondsString;
	
	private Message mMsg;
	
	private String fullUpdateFolderPath;
	
	private Context AppContext;
	private Resources res;
	
	private final BroadcastReceiver mConnectivityChangesReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			NetworkInfo netInfo = (NetworkInfo) intent.getSerializableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if(netInfo != null && netInfo.isConnected())
			{
				synchronized (mConnectivityManager)
				{
					mConnectivityManager.notifyAll();
					mWaitingForDataConnection = false;
					unregisterReceiver(this);
				}
			}
		}
	};
	
	public class LocalBinder extends Binder
	{
		public UpdateDownloaderService getService()
		{
			return UpdateDownloaderService.this;
		}
	}

	private final class ServiceHandler extends Handler
	{
		public ServiceHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			mMsg = msg;
			
			Bundle arguments = (Bundle)msg.obj;

			int request = arguments.getInt(Constants.KEY_REQUEST); 
			switch(request)
			{
				case Constants.REQUEST_DOWNLOAD_UPDATE:
					Log.d(TAG, "Request Download Update Message was recieved");
					mDownloading = true;
					try
					{
						UpdateInfo ui = mCurrentUpdate = (UpdateInfo) arguments.getSerializable(Constants.KEY_UPDATE_INFO);
						File downloadedUpdate = checkForConnectionAndUpdate(ui);
						notifyUser(ui, downloadedUpdate);
					}
					finally
					{
						mDownloading = false;
					}
					break;
				default:
					Log.d(TAG, "Unknown request ID:" + request);
			}

			Log.d(TAG, "Done with #" + msg.arg1);
			stopSelf(msg.arg1);
		}
	}

	public static void setUpdateProcessInfo(IDownloadActivity iupi)
	{
		DOWNLOAD_ACTIVITY = iupi;
	}

	@Override
	public void onCreate()
	{
		Log.d(TAG, "Download Service Created");
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		//mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mConectivityManagerIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

		mHandlerThread = new HandlerThread(TAG);
		mHandlerThread.start();
		mServiceLooper = mHandlerThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		mHttpClient = new DefaultHttpClient();
		mMD5HttpClient = new DefaultHttpClient();
		mRandom = new Random();

		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock("CM Updater");

		mUpdateFolder = Preferences.getPreferences(this).getUpdateFolder();
		fullUpdateFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + mUpdateFolder; 
		progressBarUpdateInterval = Preferences.getPreferences(this).getProgressUpdateFreq();
		Log.d(TAG, "ProgressBarIntervall: " + progressBarUpdateInterval);
		//df = new SimpleDateFormat("HH 'hours' mm 'mins' ss 'seconds'");
		//df.setTimeZone(TimeZone.getDefault());
		AppContext = getApplicationContext();
		res = getResources();
		minutesString = res.getString(R.string.minutes);
		secondsString = res.getString(R.string.seconds);
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		prepareForDownloadCancel = false;
		Log.d(TAG, "Download Service started");
		synchronized (mConnectivityManager)
		{
			if(mWaitingForDataConnection)
			{
				Log.e(TAG, "Another update process is waiting for data connection. This should not happen");
				return;
			}
		}
		Log.d(TAG, "Starting #" + startId + ": " + intent.getExtras());
		
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent.getExtras();
		Log.d(TAG, "Sending: " + msg);
		mServiceHandler.sendMessage(msg);
	}

	private boolean isDataConnected()
	{
		if (mConnectivityManager.getActiveNetworkInfo() == null)
		{
			return false;
		}
		else
		{
			return mConnectivityManager.getActiveNetworkInfo().isConnected();
		}
	}

	/**
	private boolean isWifiNetwork()
	{
		return mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
	}
	 */

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "Download Service onDestroy Called");
		mHandlerThread.getLooper().quit();
		Thread.currentThread().interrupt();
		mHandlerThread.interrupt();
		mDownloading = false;
		DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
		Log.d(TAG, "Download Service Destroyed");
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.d(TAG, "Download Service onBind was called");
		return mBinder;
	}

	/**
	 * @return the downloading
	 */
	public boolean isDownloading()
	{
		return mDownloading;
	}

	/**
	 * @return the mCurrentUpdate
	 */
	public UpdateInfo getCurrentUpdate()
	{
		return mCurrentUpdate;
	}

	private File checkForConnectionAndUpdate(UpdateInfo updateToDownload)
	{
		Log.d(TAG, "Called CheckForConnectionAndUpdate");
		File downloadedFile;

		//wait for a data connection
		while(!isDataConnected())
		{
			Log.d(TAG, "No data connection, waiting for a data connection");
			registerDataListener();
			synchronized (mConnectivityManager)
			{
				try
				{
					Log.d(TAG, "No Data Connection. Waiting...");
					mConnectivityManager.wait();
					break;
				}
				catch (InterruptedException e)
				{
					Log.e(TAG, "Exception in ConnectivityManager.wait", e);
				}
			}
		}

		mWifiLock.acquire();

		try
		{
			Log.d(TAG, "Downloading update...");
			downloadedFile = downloadFile(updateToDownload);
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "RuntimeEx while checking for updates", ex);
			notificateDownloadError();
			return null;
		}
		finally
		{
			mWifiLock.release();
		}
		
		//Be sure to return null if the User canceled the Download
		if(prepareForDownloadCancel)
			return null;
		else
			return downloadedFile;
	}

	private void registerDataListener()
	{
		synchronized (mConnectivityManager)
		{
			//mTelephonyManager.listen(mDataStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

			registerReceiver(mConnectivityChangesReceiver, mConectivityManagerIntentFilter);
			mWaitingForDataConnection = true;
		}
	}

	private void notificateDownloadError()
	{
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

		//Use a resourceId as an unique identifier
		mNM.notify(R.string.not_update_download_error_title, notification);
	}

	private File downloadFile(UpdateInfo updateInfo)
	{
		Log.d(TAG, "Called downloadFile");
		HttpClient httpClient = mHttpClient;
		HttpClient MD5httpClient = mMD5HttpClient;

		HttpUriRequest req, md5req;
		HttpResponse response, md5response;

		List<URI> updateMirrors = updateInfo.updateFileUris;
		int size = updateMirrors.size();
		int start = mRandom.nextInt(size);
		Log.d(TAG, "Mirrorcount: " + size);
		URI updateURI;
		//For every Mirror
		for(int i = 0; i < size; i++)
		{
			if (!prepareForDownloadCancel)
			{
				updateURI = updateMirrors.get((start + i)% size);
				mMirrorName = updateURI.getHost();
				Log.d(TAG, "Mirrorname: " + mMirrorName);
				
				mFileName = updateInfo.fileName; 
				if (null == mFileName || mFileName.length() < 1)
				{
					mFileName = "update.zip";
				}
				Log.d(TAG, "mFileName: " + mFileName);
				
				boolean md5Available = true;
	
				mMirrorNameUpdated = false;
				//mUpdateProcessInfo.updateDownloadMirror(updateURI.getHost());
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
					response = httpClient.execute(req);
	
					int serverResponse = response.getStatusLine().getStatusCode();
					int md5serverResponse = md5response.getStatusLine().getStatusCode();
					
	
					if (serverResponse == 404)
					{
						Log.d(TAG, "File not found on Server. Trying next one.");
					}
					else if(serverResponse != 200)
					{
						Log.d(TAG, "Server returned status code " + serverResponse + " for update zip trying next mirror");
	
					}
					else
					{
						if (md5serverResponse != 200)
						{
							md5Available = false;
							Log.d(TAG, "Server returned status code " + md5serverResponse + " for update zip md5sum trying next mirror");
						}
						//If directory not exists, create it
						File directory = new File(fullUpdateFolderPath);
						if (!directory.exists())
						{
							directory.mkdirs();
							Log.d(TAG, "UpdateFolder created");
						}
	
						mDestinationFile = new File(fullUpdateFolderPath, mFileName);
						if(mDestinationFile.exists()) mDestinationFile.delete();
	
						if (md5Available)
						{
							mDestinationMD5File = new File(fullUpdateFolderPath, mFileName + ".md5sum");
							if(mDestinationMD5File.exists()) mDestinationMD5File.delete();
	
							try
							{
								Log.d(TAG, "Trying to Read MD5 hash from response");
								HttpEntity temp = md5response.getEntity();
								InputStreamReader isr = new InputStreamReader(temp.getContent());
								BufferedReader br = new BufferedReader(isr);
								mDownloadedMD5 = br.readLine().split("  ")[0];
								Log.d(TAG, "MD5: " + mDownloadedMD5);
								br.close();
								isr.close();
	
								if (temp != null)
									temp.consumeContent();
	
								//Write the String in a .md5 File
								if (mDownloadedMD5 != null || !mDownloadedMD5.equals(""))
								{
									writeMD5(mDestinationMD5File, mDownloadedMD5);
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
						dumpFile(entity, mDestinationFile);
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
							if(!MD5.checkMD5(mDownloadedMD5, mDestinationFile))
							{
								throw new IOException("MD5 verification failed");
							}
						}
	
						//If we reach here, download & MD5 check went fine :)
						return mDestinationFile;
					}
				}
				catch (IOException ex)
				{
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
		Log.d(TAG, "Unable to download the update file from any mirror");

		if (null != mDestinationFile && mDestinationFile.exists())
		{
			mDestinationFile.delete();
		}
		if (null != mDestinationMD5File && mDestinationMD5File.exists())
		{
			mDestinationMD5File.delete();
		}

		return null;
	}

	private void dumpFile(HttpEntity entity, File destinationFile) throws IOException
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
	
			byte[] buff = new byte[64 * 1024];
			int read = 0;
			FileOutputStream fos = new FileOutputStream(destinationFile);
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
			try
			{
				mtotalDownloaded = 0;
				progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask, 100, progressBarUpdateInterval);
				while(!Thread.currentThread().isInterrupted() && (read = is.read(buff)) > 0 && !prepareForDownloadCancel)
				{
					fos.write(buff, 0, read);
					mtotalDownloaded += read;
				}

				if(read > 0)
				{
					throw new IOException("Download Canceled");
				}
	
				fos.flush();
				fos.close();
				is.close();
				Log.d(TAG, "Download finished");
			}
			catch(IOException e)
			{
				fos.close();
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
			Intent mNotificationIntent = new Intent(this, MainActivity.class);
			PendingIntent mNotificationContentIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, 0);
			mNotification.contentView = mNotificationRemoteView;
			mNotification.contentIntent = mNotificationContentIntent;
			mSpeed = (mtotalDownloaded/(int)(System.currentTimeMillis() - mStartTime));
			mSpeed = (mSpeed > 0) ? mSpeed : 1;
			mRemainingTime = ((mcontentLength - mtotalDownloaded)/mSpeed);
			mstringDownloaded = mtotalDownloaded/1048576 + "/" + mcontentLength/1048576 + " MB";
			mstringSpeed = mSpeed + " kB/s";
			//mstringRemainingTime = df.format(new Date(mRemainingTime));
			mstringRemainingTime = mRemainingTime/60000 + " " + minutesString + " " + mRemainingTime%60 + " " + secondsString;

			mstringComplete = mstringDownloaded + " " + mstringSpeed + " " + mstringRemainingTime;
			
			mNotificationRemoteView.setTextViewText(R.id.notificationTextDownloadInfos, mstringComplete);
			mNotificationRemoteView.setProgressBar(R.id.notificationProgressBar, mcontentLength, mtotalDownloaded, false);
			mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_STATUS, mNotification);
			
			if(DOWNLOAD_ACTIVITY == null) return;
			
			if(!mMirrorNameUpdated)
			{
				DOWNLOAD_ACTIVITY.updateDownloadMirror(mMirrorName);
				mMirrorNameUpdated = true;
			}
			DOWNLOAD_ACTIVITY.updateDownloadProgress(mtotalDownloaded, mcontentLength, mstringDownloaded, mstringSpeed, mstringRemainingTime);
		}
		else
			Log.d(TAG, "Downloadcancel in Progress. Not updating the Notification and DownloadLayout");
	}

	private void notifyUser(UpdateInfo ui, File downloadedUpdate)
	{
		Log.d(TAG, "Called Notify User");
		Intent i;
		
		if(downloadedUpdate == null)
		{
			Log.d(TAG, "Downloaded Update was NULL");
			DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
			mHandlerThread.interrupt();
			//Go to the App with a download error
			i = new Intent(this, MainActivity.class);
			i.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_DOWNLOAD_FAILED);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			return;
		}
		
		i = new Intent(this, ApplyUpdateActivity.class);
		i.putExtra(Constants.KEY_UPDATE_INFO, ui);
		
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
		
		if(DOWNLOAD_ACTIVITY != null)
		{
			//app is active, switching layout
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
	}

	public void cancelDownload()
	{
		//Thread.currentThread().interrupt();
		prepareForDownloadCancel = true;
		Log.d(TAG, "Download Service CancelDownload was called");
		DeleteDownloadStatusNotification(Constants.NOTIFICATION_DOWNLOAD_STATUS);
		File update = new File(fullUpdateFolderPath + "/" + mCurrentUpdate.fileName);
		File md5sum = new File(fullUpdateFolderPath + "/" + mCurrentUpdate.fileName + ".md5sum");
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
		Log.d(TAG, "Done with #" + mMsg.arg1);
		stopSelf(mMsg.arg1);
		Log.d(TAG, "Download Cancel StopSelf was called");
		stopSelf();
	}
	
	private void DeleteDownloadStatusNotification(int id)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(id);
		Log.d(TAG, "Download Notification removed");
	}
}