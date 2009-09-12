package cmupdaterapp.ui;

import java.io.IOException;
import java.io.Serializable;
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
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.interfaces.IUpdateProcessInfo;
import cmupdaterapp.interfaces.IUpdateServer;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.ui.Log;

public class UpdateCheck implements Runnable
{
	private static final String TAG = "UpdateCheck";

	private IUpdateServer mUpdateServer;
	private IUpdateProcessInfo mUpdateProcessInfo;	
	private ProgressDialog p;
	
	private FullUpdateInfo ui = null;
	
	public UpdateCheck(IUpdateServer updateServer, IUpdateProcessInfo upi, ProgressDialog pg)
	{
		mUpdateServer = updateServer;
		mUpdateProcessInfo = upi;
		p = pg;
	}
	
	public void run()
	{
		Message m = new Message();
		try
		{
			ui = mUpdateServer.getAvailableUpdates();
			h.sendEmptyMessage(0);
		}
		catch (IOException e)
		{
			Log.e(TAG, "Exception while checking for Updates", e);
			m.obj = e.toString();
			h.sendMessage(m);
		}
	}
	private Handler h = new Handler()
	{
		//@Override
		public void handleMessage(Message msg)
		{
			IUpdateProcessInfo upi = mUpdateProcessInfo;
			
			Resources res = upi.getResources();
			if(ui == null)
			{
				if (msg != null && msg.obj != null)
					Toast.makeText(upi, (String) msg.obj, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(upi, R.string.exception_while_updating, Toast.LENGTH_LONG).show();
				p.dismiss();
				return;
			}
			
			Preferences prefs = Preferences.getPreferences(upi);
			prefs.setLastUpdateCheck(new Date());
			
			int updateCountRoms = ui.getRomCount();
			int updateCountThemes = ui.getThemeCount();
			int updateCount = ui.getUpdateCount();
			if(updateCountRoms == 0 && updateCountThemes == 0)
			{
				Log.d(TAG, "No updates found");
				Toast.makeText(upi, R.string.no_updates_found, Toast.LENGTH_SHORT).show();
				p.dismiss();
				upi.switchToUpdateChooserLayout(null);
			}
			else
			{
				Log.d(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
				upi.switchToUpdateChooserLayout(ui);
				if(prefs.notificationsEnabled())
				{	
					Intent i = new Intent(upi, Main.class)
							.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)ui)
							.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_NEW_UPDATE_LIST);
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
	};
}