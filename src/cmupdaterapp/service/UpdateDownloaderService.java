package cmupdaterapp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Random;

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
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import cmupdaterapp.ui.ApplyUploadActivity;
import cmupdaterapp.ui.IUpdateProcessInfo;
import cmupdaterapp.ui.R;
import cmupdaterapp.ui.UpdateProcessInfo;
import cmupdaterapp.utils.IOUtils;
import cmupdaterapp.utils.Preferences;


public class UpdateDownloaderService extends Service
{
	//public static UpdateDownloaderService INSTANCE;

	private static final String TAG = "<CM-Updater> UpdateDownloader";

	public static final String KEY_REQUEST = "cmupdaterapp.request";
	public static final String KEY_UPDATE_INFO = "cmupdaterapp.updateInfo";

	public static final int REQUEST_DOWNLOAD_UPDATE = 1;

	public static final int NOTIFICATION_DOWNLOAD_STATUS = 5464;
	
	private static NotificationManager mNotificationManager;
	private Notification mNotification;
	private RemoteViews mNotificationRemoteView;
	private Intent mNotificationIntent;
	private PendingIntent mNotificationContentIntent;

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
	/*
	private final PhoneStateListener mDataStateListener = new PhoneStateListener(){

		@Override
		public void onDataConnectionStateChanged(int state) {
			if(state == TelephonyManager.DATA_CONNECTED) {
				synchronized (mTelephonyManager) {
					mTelephonyManager.notifyAll();
					mTelephonyManager.listen(mDataStateListener, PhoneStateListener.LISTEN_NONE);
					mWaitingForDataConnection = false;
				}
			}
		}
	};*/


	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private HandlerThread mHandlerThread;
	private NotificationManager mNM;
	private boolean mWaitingForDataConnection = false;
	private File mDestinationFile;
	private File mDestinationMD5File;
	//private TelephonyManager mTelephonyManager;
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

	private static IUpdateProcessInfo UPDATE_PROCESS_INFO;

	private WifiLock mWifiLock;
	private WifiManager mWifiManager;

	private String mUpdateFolder;
	private String mDownlaodedMD5;

	private int mSpeed;
	private long mRemainingTime;
	String mstringDownloaded;
	String mstringSpeed;
	String mstringRemainingTime;
	
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
			Bundle arguments = (Bundle)msg.obj;

			int request = arguments.getInt(KEY_REQUEST); 
			switch(request)
			{
			case REQUEST_DOWNLOAD_UPDATE:
				mDownloading = true;
				try 
				{
					UpdateInfo ui = mCurrentUpdate = (UpdateInfo) arguments.getSerializable(KEY_UPDATE_INFO);
					File downloadedUpdate = checkForConnectionAndUpdate(ui);
					notifyUser(ui, downloadedUpdate);
				}
				finally
				{
					mDownloading = false;
				}
				break;
			default:
				Log.e(TAG, "Unknown request ID:" + request);
			}

			Log.i("ServiceStartArguments", "Done with #" + msg.arg1);
			stopSelf(msg.arg1);
		}
	}

	public static void setUpdateProcessInfo(IUpdateProcessInfo iupi)
	{
		UPDATE_PROCESS_INFO = iupi;
	}


	@Override
	public void onCreate()
	{
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		//mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mConectivityManagerIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

		mHandlerThread = new HandlerThread(TAG);
		mHandlerThread.start();
		mServiceLooper = mHandlerThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		//Resources res = getResources();

		//String destFileName = res.getString(R.string.conf_update_file_name);
		//mDestinationFile = new File(Environment.getExternalStorageDirectory(), destFileName);
		//mDestinationMD5File = new File(Environment.getExternalStorageDirectory(), destFileName + ".md5sum");
		mHttpClient = new DefaultHttpClient();
		mMD5HttpClient = new DefaultHttpClient();
		mRandom = new Random();

		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock("CM Updater");

		mUpdateFolder = Preferences.getPreferences(this).getUpdateFolder();
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		synchronized (mConnectivityManager)
		{
			if(mWaitingForDataConnection)
			{
				Log.w(TAG, "Another update process is waiting for data connection. This should not happen");
				return;
			}
		}
		Log.i(TAG, "Starting #" + startId + ": " + intent.getExtras());

		// Shows Downloadstatus in Notificationbar. Initialize the Variables
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotification = new Notification(R.drawable.icon_notification, getResources().getString(R.string.notification_tickertext), System.currentTimeMillis());
		mNotification.flags = Notification.FLAG_NO_CLEAR;
		mNotificationRemoteView = new RemoteViews(getPackageName(), R.layout.notification);
		mNotificationIntent = new Intent(this, UpdateProcessInfo.class);
		mNotificationContentIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, 0);
		mNotification.contentView = mNotificationRemoteView;
		mNotification.contentIntent = mNotificationContentIntent;
		
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent.getExtras();
		mServiceHandler.sendMessage(msg);
		Log.d("ServiceStartArguments", "Sending: " + msg);
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
		mServiceLooper.quit();
		//INSTANCE = null;
		mDownloading = false;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
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
		File downloadedFile;

		//wait for a data connection
		while(!isDataConnected()) {
			Log.d(TAG, "No data connection, waiting for a data connection");
			registerDataListener();
			synchronized (mConnectivityManager)
			{
				try
				{
					mConnectivityManager.wait();
					break;
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}

		mWifiLock.acquire();

		try
		{
			Log.i(TAG, "Downloading update...");
			downloadedFile = downloadFile(updateToDownload);
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "RuntimeEx while checking for updates", ex);
			ex.printStackTrace();
			notificateDownloadError();
			return null;
		}
		finally
		{
			mWifiLock.release();
		}
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
		Resources res = getResources();
		Intent i = new Intent(this, UpdateProcessInfo.class)
		.putExtra(UpdateProcessInfo.KEY_REQUEST, UpdateProcessInfo.REQUEST_DOWNLOAD_FAILED);

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

		HttpClient httpClient = mHttpClient;
		HttpClient MD5httpClient = mMD5HttpClient;

		HttpUriRequest req, md5req;
		HttpResponse response, md5response;

		List<URI> updateMirrors = updateInfo.updateFileUris;
		int size = updateMirrors.size();
		int start = mRandom.nextInt(size);
		URI updateURI;

		for(int i = 0; i < size; i++)
		{
			updateURI = updateMirrors.get((start + i)% size);
			mMirrorName = updateURI.getHost();

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

				Log.i(TAG, "Trying to download md5sum file from " + md5req.getURI());
				md5response = MD5httpClient.execute(md5req);
				Log.i(TAG, "Trying to download update zip from " + req.getURI());
				response = httpClient.execute(req);

				int serverResponse = response.getStatusLine().getStatusCode();
				int md5serverResponse = md5response.getStatusLine().getStatusCode();
				

				if (serverResponse == 404)
				{
					Log.e(TAG, "File not found on Server. Trying next one.");
				}
				else if(serverResponse != 200)
				{
					Log.e(TAG, "Server returned status code " + serverResponse + " for update zip trying next mirror");

				}
				else
				{
					if (md5serverResponse != 200)
					{
						md5Available = false;
						Log.e(TAG, "Server returned status code " + md5serverResponse + " for update zip md5sum trying next mirror");
					}
					//If directory not exists, create it
					File directory = new File(Environment.getExternalStorageDirectory()+"/"+mUpdateFolder);
					if (!directory.exists())
					{
						directory.mkdirs();
						Log.d(TAG, "UpdateFolder created");
					}

					mDestinationFile = new File(Environment.getExternalStorageDirectory()+"/"+mUpdateFolder, mFileName);
					if(mDestinationFile.exists()) mDestinationFile.delete();

					if (md5Available)
					{
						mDestinationMD5File = new File(Environment.getExternalStorageDirectory()+"/"+mUpdateFolder, mFileName + ".md5sum");
						if(mDestinationMD5File.exists()) mDestinationMD5File.delete();

						try
						{
							Log.i(TAG, "Trying to Read MD5 hash from response");
							HttpEntity temp = md5response.getEntity();
							InputStreamReader isr = new InputStreamReader(temp.getContent());
							BufferedReader br = new BufferedReader(isr);
							mDownlaodedMD5 = br.readLine().split("  ")[0];
							Log.d(TAG,"MD5: " + mDownlaodedMD5);
							br.close();
							isr.close();

							if (temp != null)
								temp.consumeContent();

							//Write the String in a .md5 File
							if (mDownlaodedMD5 != null || !mDownlaodedMD5.equals(""))
							{
								writeMD5(mDestinationMD5File, mDownlaodedMD5);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Exception while reading MD5 response: "+e.getMessage());
							e.printStackTrace();
							throw new IOException("MD5 Response cannot be read");
						}
					}

					// Download Update ZIP if md5sum went ok
					HttpEntity entity = response.getEntity();
					dumpFile(entity, mDestinationFile);
					if (entity != null)
						entity.consumeContent();
					Log.i(TAG, "Update download finished");
					
					if (md5Available)
					{
						Log.i(TAG, "Performing MD5 verification");
						if(!IOUtils.checkMD5(mDownlaodedMD5, mDestinationFile))
						{
							throw new IOException("MD5 verification failed");
						}
					}

					//If we reach here, download & MD5 check went fine :)
					return mDestinationFile;
				}
			}
			catch (Exception ex)
			{
				Log.w(TAG, "An error occured while downloading the update file. Trying next mirror", ex);
				ex.printStackTrace();
			}
			if(Thread.currentThread().isInterrupted()) break;
		}

		Log.e(TAG, "Unable to download the update file from any mirror");

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
		long contentLength = entity.getContentLength();
		if(contentLength <= 0)
		{
			Log.w(TAG, "unable to determine the update file size, Set ContentLength to 1024");
			contentLength = 1024;
		}
		else Log.i(TAG, "Update size: " + (contentLength/1024) + "KB" );

		long StartTime = System.currentTimeMillis(); 

		byte[] buff = new byte[64 * 1024];
		int read = 0;
		int totalDownloaded = 0;
		FileOutputStream fos = new FileOutputStream(destinationFile);
		InputStream is = entity.getContent();
		try
		{
			while(!Thread.currentThread().isInterrupted() && (read = is.read(buff)) > 0)
			{
				fos.write(buff, 0, read);
				totalDownloaded += read;
				onProgressUpdate(totalDownloaded, (int)contentLength, StartTime);
			}

			if(read > 0)
			{
				throw new IOException("Download Canceled");
			}

			fos.flush();
			fos.close();
		}
		catch(Exception e)
		{
			fos.close();
			try
			{
				destinationFile.delete();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Unable to delete downlaoded File. Continue anyway. Message: "+ ex.getMessage());
			}
			e.printStackTrace();
		}
		finally
		{
			//is.close();
			buff = null;
		}
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
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			fw.close();
		}
	}

	private void onProgressUpdate(int downloaded, int total, long StartTime)
	{
		if(UPDATE_PROCESS_INFO == null) return;

		mSpeed = (downloaded/(int)(System.currentTimeMillis() - StartTime));
		mSpeed = (mSpeed > 0) ? mSpeed : 1;
		mRemainingTime = ((total - downloaded)/mSpeed)/1000;
		mstringDownloaded = (downloaded/(1024*1024)) + "/" + (total/(1024*1024)) + " MB";
		mstringSpeed = Integer.toString(mSpeed) + " kB/s";
		mstringRemainingTime = Long.toString(mRemainingTime) + " seconds";
		if(!mMirrorNameUpdated)
		{
			UPDATE_PROCESS_INFO.updateDownloadMirror(mMirrorName);
			mMirrorNameUpdated = true;
		}
		mNotificationRemoteView.setTextViewText(R.id.notificationTextDownloading, mstringDownloaded);
		mNotificationRemoteView.setTextViewText(R.id.notificationTextSpeed, mstringSpeed);
		mNotificationRemoteView.setTextViewText(R.id.notificationTextRemainingTime, mstringRemainingTime);
		mNotificationRemoteView.setProgressBar(R.id.notificationProgressBar, total, downloaded, false);
		mNotificationManager.notify(NOTIFICATION_DOWNLOAD_STATUS, mNotification);
		UPDATE_PROCESS_INFO.updateDownloadProgress(downloaded, total, StartTime);
	}

	private void notifyUser(UpdateInfo ui, File downloadedUpdate)
	{
		if(downloadedUpdate == null)
		{
			Toast.makeText(this, R.string.exception_while_downloading, Toast.LENGTH_LONG).show();
//			mHandlerThread.interrupt();
//			UpdateProcessInfo upi = new UpdateProcessInfo();
//			upi.switchToUpdateChooserLayout(null);
			return;
		}

		if(UPDATE_PROCESS_INFO == null)
		{
			//app is closed, launching a notification
			Resources res = getResources();
			Intent i = new Intent(this, ApplyUploadActivity.class)
			.putExtra(ApplyUploadActivity.KEY_UPDATE_INFO, ui);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
					PendingIntent.FLAG_CANCEL_CURRENT);

			Notification notification = new Notification(R.drawable.icon_notification,
					res.getString(R.string.not_update_downloaded_ticker),
					System.currentTimeMillis());
			String notificationBody = MessageFormat.format(res.getString(R.string.not_update_downloaded_body),
					ui.name);
			notification.setLatestEventInfo(this, res.getString(R.string.not_update_downloaded_title), notificationBody, contentIntent);
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

			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(R.string.not_update_downloaded_title, notification);
		}
		else
		{
			//app is active, switching layout
			Intent i = new Intent(this, ApplyUploadActivity.class);
			i.putExtra(ApplyUploadActivity.KEY_UPDATE_INFO, ui);
			//TODO check!
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}

	}

	public void cancelDownload()
	{
		DeleteDownloadStatusNotification();
		//Thread.currentThread().interrupt();
		mDownloading = false;
		if(mHandlerThread != null)
		{
		    HandlerThread tempThread = mHandlerThread;
		    mHandlerThread = null;
		    tempThread.interrupt();
		}
	}
	
	public void DeleteDownloadStatusNotification()
	{
		if(mNotificationManager != null)
		{
			//Delete the Downloading in Statusbar Notification
			mNotificationManager.cancel(NOTIFICATION_DOWNLOAD_STATUS);
		}
	}
}