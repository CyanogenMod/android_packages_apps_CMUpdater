/*
 * JF Updater: Auto-updater for modified Android OS
 *
 * Copyright (c) 2009 Sergi Vélez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package cmupdater.utils;

import java.security.InvalidParameterException;
import java.util.Date;

import cmupdater.service.UpdateCheckerService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
	
	private static final String TAG = "StartupReceiver";
	
	@Override
	public void onReceive(Context ctx, Intent intent) {
		Preferences prefs = Preferences.getPreferences(ctx);
		
		if(prefs.getConfiguredModString() == null) {
			Preferences.getPreferences(ctx).configureModString();
		}
		
		int updateFreq = prefs.getUpdateFrequency(); 
		if(updateFreq == Preferences.UPDATE_FREQ_AT_BOOT) {

			Intent i = new Intent(ctx, UpdateCheckerService.class)
							.putExtra(UpdateCheckerService.KEY_REQUEST, UpdateCheckerService.REQUEST_CHECK_FOR_UPDATES);
			
			Log.i(TAG, "Asking UpdateService to check for updates...");
			ctx.startService(i);
		} else {
			scheduleUpdateService(ctx, updateFreq * 1000);
		}
	}
	
	public static void cancelUpdateChecks(Context ctx) {
		Intent i = new Intent(ctx, UpdateCheckerService.class)
						.putExtra(UpdateCheckerService.KEY_REQUEST, UpdateCheckerService.REQUEST_CHECK_FOR_UPDATES);
		PendingIntent pi = PendingIntent.getService(ctx, 0, i, 0);
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Log.d(TAG, "Canceling any previous alarms");
		am.cancel(pi);
	}

	public static void scheduleUpdateService(Context ctx, int updateFrequency) {
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
