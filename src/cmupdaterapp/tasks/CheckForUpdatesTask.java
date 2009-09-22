package cmupdaterapp.tasks;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.Toast;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.interfaces.IMainActivity;
import cmupdaterapp.interfaces.IUpdateServer;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;

public class CheckForUpdatesTask extends UserTask<Void, Integer, FullUpdateInfo>
{
	private static final String TAG = "CheckForUpdatesTask";

	private IUpdateServer mUpdateServer;
	private IMainActivity mUpdateProcessInfo;	

	
	public CheckForUpdatesTask(IUpdateServer updateServer, IMainActivity upi)
	{
		mUpdateServer = updateServer;
		mUpdateProcessInfo = upi;
	}

	@Override
	public FullUpdateInfo doInBackground(Void... params)
	{
		try
		{
			Log.d(TAG, "Checking for updates...");
			return mUpdateServer.getAvailableUpdates();
		}
		catch (IOException ex)
		{
			Log.e(TAG, "IOEx while checking for updates", ex);
			return null;
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "RuntimeEx while checking for updates", ex);
			return null;
		}
	}
	
	@Override
	public void onPostExecute(FullUpdateInfo result)
	{
		IMainActivity upi = mUpdateProcessInfo;
		
		Resources res = upi.getResources();
		if(result == null)
		{
			Toast.makeText(upi, R.string.exception_while_updating, Toast.LENGTH_LONG).show();
			return;
		}
		
		Preferences prefs = Preferences.getPreferences(upi);
		prefs.setLastUpdateCheck(new Date());
		
		int updateCountRoms = result.getRomCount();
		int updateCountThemes = result.getThemeCount();
		int updateCount = result.getUpdateCount();
		if(updateCountRoms == 0 && updateCountThemes == 0)
		{
			Log.d(TAG, "No updates found");
			Toast.makeText(upi, R.string.no_updates_found, Toast.LENGTH_LONG).show();
			upi.switchToUpdateChooserLayout();
		}
		else
		{
			Log.d(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
			upi.switchToUpdateChooserLayout();
			
			Intent i = new Intent(upi, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(upi, 0, i, PendingIntent.FLAG_ONE_SHOT);

			Notification notification = new Notification(R.drawable.icon_notification,
								res.getString(R.string.not_new_updates_found_ticker),
								System.currentTimeMillis());
			
			String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);
			notification.setLatestEventInfo(upi, res.getString(R.string.not_new_updates_found_title), text, contentIntent);
			
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
			((NotificationManager)upi.getSystemService(Context.NOTIFICATION_SERVICE)).
					notify(R.string.not_new_updates_found_title, notification);
		}
	}
}