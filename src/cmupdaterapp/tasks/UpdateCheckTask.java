package cmupdaterapp.tasks;

import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.interfaces.IUpdateCheckService;
import cmupdaterapp.interfaces.IUpdateCheckServiceCallback;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;

public class UpdateCheckTask extends AsyncTask<Void, Void, String>
{
	private static final String TAG = "UpdateCheckTask";

	private List<String> UpdateCheckExceptions = new LinkedList<String>();
	private IUpdateCheckService myService;	
	private ProgressDialog p;
	private Context context;
	private FullUpdateInfo ui = null;
	private boolean mbound;
	private boolean firstException = true;
	
	public UpdateCheckTask(Context ctx, ProgressDialog pg)
	{
		context = ctx;
		p = pg;
	}

	@Override
	protected void onPreExecute()
	{
		mbound = context.bindService(new Intent(IUpdateCheckService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected String doInBackground(Void... arg0)
	{
		try
		{
			//Wait till the Service is bound
			while(myService == null)
			{
				continue;
			}
			myService.checkForUpdates();
			//TODO: Get Update from checkUpdates back
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute (String result)
	{
		try
		{
			Resources res = context.getResources();
			if(ui == null)
			{
				if (result != null)
					Toast.makeText(context, (String) result, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(context, R.string.exception_while_updating, Toast.LENGTH_LONG).show();
				p.dismiss();
				return;
			}
			
			if (myService != null && UpdateCheckExceptions != null && UpdateCheckExceptions.size() > 0)
			{
				for (String e : UpdateCheckExceptions)
				{
					Toast.makeText(context, e, Toast.LENGTH_LONG).show();
				}
			}
			
			Preferences prefs = Preferences.getPreferences(context);
			prefs.setLastUpdateCheck(new Date());
			
			int updateCountRoms = ui.getRomCount();
			int updateCountThemes = ui.getThemeCount();
			int updateCount = ui.getUpdateCount();
			if(updateCountRoms == 0 && updateCountThemes == 0)
			{
				Log.d(TAG, "No updates found");
				Toast.makeText(context, R.string.no_updates_found, Toast.LENGTH_LONG).show();
				p.dismiss();
				//upi.switchToUpdateChooserLayout();
				//TODO: switch To Udpate Chooser Layout
			}
			else
			{
				Log.d(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
				//context.switchToUpdateChooserLayout();
				//TODO: switch To Udpate Chooser Layout
				if(prefs.notificationsEnabled())
				{	
					Intent i = new Intent(context, MainActivity.class);
					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT);
	
					Notification notification = new Notification(R.drawable.icon_notification,
										res.getString(R.string.not_new_updates_found_ticker),
										System.currentTimeMillis());
		
					String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);
					notification.setLatestEventInfo(context, res.getString(R.string.not_new_updates_found_title), text, contentIntent);
	
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
					((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).
							notify(R.string.not_new_updates_found_title, notification);
				}
				p.dismiss();
			}
		}
		finally
		{
			if(mbound)
				context.unbindService(mConnection);
		}
	}
	
	/**
	 * Class for interacting with the main interface of the service.
	 */
    private ServiceConnection mConnection = new ServiceConnection()
    {
    	public void onServiceConnected(ComponentName name, IBinder service)
    	{
    		myService = IUpdateCheckService.Stub.asInterface(service);
    		try
    		{
    			myService.registerCallback(mCallback);
    		}
    		catch (RemoteException e)
    		{ }
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
    
    private IUpdateCheckServiceCallback mCallback = new IUpdateCheckServiceCallback.Stub()
	{
		public void UpdateCheckFinished(FullUpdateInfo fui) throws RemoteException
		{
			
		}

		public void addException(String exception) throws RemoteException
		{
			if (firstException)
			{
				UpdateCheckExceptions.clear();
				firstException = false;
			}
			UpdateCheckExceptions.add(exception);
		}
	};
}