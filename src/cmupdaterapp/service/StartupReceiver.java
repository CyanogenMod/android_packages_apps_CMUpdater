package cmupdaterapp.service;

import java.security.InvalidParameterException;
import java.util.Date;

import cmupdaterapp.service.UpdateCheckService;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.utils.Preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver
{
	private static final String TAG = "StartupReceiver";
	
	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Preferences prefs = Preferences.getPreferences(ctx);
		boolean notificationsEnabled = prefs.notificationsEnabled();
		int updateFreq = prefs.getUpdateFrequency();
		 
		//Only check for updates if notifications are enabled
		if(updateFreq == Constants.UPDATE_FREQ_AT_BOOT && notificationsEnabled)
		{
			Intent i = new Intent(ctx, UpdateCheckService.class);
			
			Log.d(TAG, "Asking UpdateService to check for updates...");
			ctx.startService(i);
		}
		else if(!(updateFreq == Constants.UPDATE_FREQ_NONE) && notificationsEnabled)
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
		Intent i = new Intent(ctx, UpdateCheckService.class);
		PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Log.d(TAG, "Canceling any previous alarms");
		am.cancel(pi);
	}

	public static void scheduleUpdateService(Context ctx, int updateFrequency)
	{
		if(updateFrequency < 0) throw new InvalidParameterException("updateFrequency can't be negative"); 
			
		Log.d(TAG, "Scheduling alarm to go off every "  + updateFrequency + " msegs");
		Intent i = new Intent(ctx, UpdateCheckService.class);
		PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);

		Date lastCheck = Preferences.getPreferences(ctx).getLastUpdateCheck();
		Log.d(TAG, "Last check on " + lastCheck.toString());

		cancelUpdateChecks(ctx);
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Log.d(TAG, "Setting alarm for UpdateService");
		am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck.getTime() + updateFrequency, updateFrequency, pi);
	}
}