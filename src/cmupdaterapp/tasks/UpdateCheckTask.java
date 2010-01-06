package cmupdaterapp.tasks;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.interfaces.IMainActivity;
import cmupdaterapp.interfaces.IUpdateCheckHelper;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.MainActivity;
import cmupdaterapp.ui.R;

public class UpdateCheckTask extends AsyncTask<Void, Void, String>
{
	private static final String TAG = "UpdateCheckTask";

	private IUpdateCheckHelper mUpdateServer;
	private IMainActivity mUpdateProcessInfo;	
	private ProgressDialog p;
	
	private FullUpdateInfo ui = null;
	
	public UpdateCheckTask(IUpdateCheckHelper updateServer, IMainActivity upi, ProgressDialog pg)
	{
		mUpdateServer = updateServer;
		mUpdateProcessInfo = upi;
		p = pg;
	}

	@Override
	protected String doInBackground(Void... arg0)
	{
		try
		{
			ui = mUpdateServer.getAvailableUpdates();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Exception while checking for Updates", e);
			return e.toString();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute (String result)
	{
		IMainActivity upi = mUpdateProcessInfo;
		
		Resources res = upi.getResources();
		if(ui == null)
		{
			if (result != null)
				Toast.makeText(upi, (String) result, Toast.LENGTH_LONG).show();
			else
				Toast.makeText(upi, R.string.exception_while_updating, Toast.LENGTH_LONG).show();
			p.dismiss();
			return;
		}
		
		if (mUpdateServer != null && IUpdateCheckHelper.Exceptions != null && IUpdateCheckHelper.Exceptions.size() > 0)
		{
			for (String e : IUpdateCheckHelper.Exceptions)
			{
				Toast.makeText(upi, e, Toast.LENGTH_LONG).show();
			}
		}
		
		Preferences prefs = Preferences.getPreferences(upi);
		prefs.setLastUpdateCheck(new Date());
		
		int updateCountRoms = ui.getRomCount();
		int updateCountThemes = ui.getThemeCount();
		int updateCount = ui.getUpdateCount();
		if(updateCountRoms == 0 && updateCountThemes == 0)
		{
			Log.d(TAG, "No updates found");
			Toast.makeText(upi, R.string.no_updates_found, Toast.LENGTH_LONG).show();
			p.dismiss();
			upi.switchToUpdateChooserLayout();
		}
		else
		{
			Log.d(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
			upi.switchToUpdateChooserLayout();
			if(prefs.notificationsEnabled())
			{	
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
			p.dismiss();
		}
	}
}