package cmupdaterapp.utils;

import java.security.InvalidParameterException;
import java.util.Date;

import cmupdaterapp.service.UpdateCheckerService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver
{
	private static final String TAG = "<CM-Updater> StartupReceiver";
	
	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Preferences prefs = Preferences.getPreferences(ctx);
		
		//If older Version was installed before, the ModString is ADP1. So reset it
		if(prefs.getConfiguredModString() == null || prefs.getConfiguredModString().equals("ADP1"))
		{
			Preferences.getPreferences(ctx).configureModString();
		}
		
		int updateFreq = prefs.getUpdateFrequency(); 
		//Only check for updates if notifications are enabled
		if(updateFreq == Preferences.UPDATE_FREQ_AT_BOOT && Preferences.getPreferences(ctx).notificationsEnabled())
		{
			Intent i = new Intent(ctx, UpdateCheckerService.class)
							.putExtra(UpdateCheckerService.KEY_REQUEST, UpdateCheckerService.REQUEST_CHECK_FOR_UPDATES);
			
			Log.i(TAG, "Asking UpdateService to check for updates...");
			ctx.startService(i);
		}
		else if(!(updateFreq == Preferences.UPDATE_FREQ_NONE) && Preferences.getPreferences(ctx).notificationsEnabled())
		{
			scheduleUpdateService(ctx, updateFreq * 1000);
		}
		else
		{
			// User selected no updates
			Log.d(TAG, "No Updatecheck");
		}
	}
	
	public static void cancelUpdateChecks(Context ctx)
	{
		Intent i = new Intent(ctx, UpdateCheckerService.class)
						.putExtra(UpdateCheckerService.KEY_REQUEST, UpdateCheckerService.REQUEST_CHECK_FOR_UPDATES);
		PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Log.d(TAG, "Canceling any previous alarms");
		am.cancel(pi);
	}

	public static void scheduleUpdateService(Context ctx, int updateFrequency)
	{
		if(updateFrequency < 0) throw new InvalidParameterException("updateFrequency can't be negative"); 
			
		Log.i(TAG, "Scheduling alarm to go off every "  + updateFrequency + " msegs");
		Intent i = new Intent(ctx, UpdateCheckerService.class)
						.putExtra(UpdateCheckerService.KEY_REQUEST, UpdateCheckerService.REQUEST_CHECK_FOR_UPDATES);
		PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);

		Date lastCheck = Preferences.getPreferences(ctx).getLastUpdateCheck();
		Log.d(TAG, "Last check on " + lastCheck.toString());

		cancelUpdateChecks(ctx);
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Log.i(TAG, "Setting alarm for UpdateService");
		am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck.getTime() + updateFrequency, updateFrequency, pi);
	}
}