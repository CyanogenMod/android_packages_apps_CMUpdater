package cmupdaterapp.service;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;

import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.interfaces.IUpdateServer;
import cmupdaterapp.ui.Constants;
import cmupdaterapp.ui.R;
import cmupdaterapp.ui.UpdateProcessInfo;
import cmupdaterapp.utils.Preferences;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class UpdateCheckerService extends Service
{	
	private static final String TAG = "<CM-Updater> UpdateService";
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private IUpdateServer mUpdateServer;
	private NotificationManager mNM;
	private boolean mWaitingForDataConnection = false;

	private TelephonyManager mTelephonyManager;
	
	private final PhoneStateListener mDataStateListener = new PhoneStateListener()
	{
		@Override
		public void onDataConnectionStateChanged(int state)
		{
			if(state == TelephonyManager.DATA_CONNECTED)
			{
				synchronized (mTelephonyManager)
				{
					mTelephonyManager.notifyAll();
					mTelephonyManager.listen(mDataStateListener, PhoneStateListener.LISTEN_NONE);
					mWaitingForDataConnection = false;
				}
			}
		}
	};
	
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
            
            int request = arguments.getInt(Constants.KEY_REQUEST); 
            switch(request)
            {
            	case Constants.REQUEST_CHECK_FOR_UPDATES:
					checkForUpdates();
					break;
            	default:
            		Log.e(TAG, "Unknown request ID:" + request);
            }

            Log.i(TAG, "Done with #" + msg.arg1);
            stopSelf(msg.arg1);
		}
	}
	

	@Override
	public void onCreate()
	{
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        
		HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mUpdateServer = new PlainTextUpdateServer(this);
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		synchronized (mTelephonyManager)
		{
			if(mWaitingForDataConnection)
			{
				Log.i(TAG, "Another update check is waiting for data connection. Skipping");
				return;
			}
		}
        Log.i(TAG, "Starting #" + startId + ": " + intent.getExtras());
        
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getExtras();
        mServiceHandler.sendMessage(msg);
        Log.d(TAG, "Sending: " + msg);
	}
	
	private boolean isDataConnected()
	{
		int state = mTelephonyManager.getDataState(); 
		return state == TelephonyManager.DATA_CONNECTED || state == TelephonyManager.DATA_SUSPENDED;
	}

	@Override
	public void onDestroy()
	{
		mServiceLooper.quit();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	private void checkForUpdates()
	{
		Resources res = getResources();
		
		FullUpdateInfo availableUpdates;
		while (true)
		{
			
			//wait for a data connection
			while(!isDataConnected())
			{
				Log.d(TAG, "No data connection, waiting for a data connection");
				registerDataListener();
				synchronized (mTelephonyManager)
				{
					try
					{
						mTelephonyManager.wait();
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
				Log.i(TAG, "Checking for updates...");
				availableUpdates = mUpdateServer.getAvailableUpdates();
				break;
			}
			catch (IOException ex)
			{
				Log.e(TAG, "IOEx while checking for updates", ex);
				if(isDataConnected())
				{
					notificateCheckError();
					return;
				}
			}
			catch (RuntimeException ex)
			{
				Log.e(TAG, "RuntimeEx while checking for updates", ex);
				notificateCheckError();
				return;
			}
		}

		Preferences prefs = Preferences.getPreferences(this);
		prefs.setLastUpdateCheck(new Date());
		
		
		int updateCountRoms = availableUpdates.getRomCount();
		int updateCountThemes = availableUpdates.getThemeCount();
		int updateCount = availableUpdates.getUpdateCount();
		Log.i(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
		
		if(updateCountRoms > 0 || updateCountThemes > 0)
		{
			Intent i = new Intent(this, UpdateProcessInfo.class)
							.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_NEW_UPDATE_LIST)
							.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)availableUpdates);
			
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
												PendingIntent.FLAG_ONE_SHOT);
			
			Notification notification = new Notification(R.drawable.icon_notification,
												res.getString(R.string.not_new_updates_found_ticker),
												System.currentTimeMillis());
			
			//To remove the Notification, when the User clicks on it
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			
			String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);
			notification.setLatestEventInfo(this, res.getString(R.string.not_new_updates_found_title), text, contentIntent);
			
			Uri notificationRingtone = prefs.getConfiguredRingtone();
			if(prefs.getVibrate())
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
			mNM.notify(R.string.not_new_updates_found_title, notification);
		}
		else
		{
			Log.i(TAG, "No updates found");
		}
	}

	private void registerDataListener()
	{
		synchronized (mTelephonyManager)
		{
			mTelephonyManager.listen(mDataStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
			mWaitingForDataConnection = true;
		}
	}

	private void notificateCheckError()
	{
		Resources res = getResources();
		Intent i = new Intent(this, UpdateProcessInfo.class)
						.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_UPDATE_CHECK_ERROR);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
															PendingIntent.FLAG_ONE_SHOT);
		
		Notification notification = new Notification(android.R.drawable.stat_notify_error,
												res.getString(R.string.not_update_check_error_ticker),
												System.currentTimeMillis());
		
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		notification.setLatestEventInfo(
							this,
							res.getString(R.string.not_update_check_error_title),
							res.getString(R.string.not_update_check_error_body),
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
		mNM.notify(R.string.not_update_downloaded_title, notification);
	}
}