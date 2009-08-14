package cmupdater.utils;

import java.util.Date;

import cmupdater.ui.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences {
	
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;
	
	private static final String TAG = "Preferences";
	
	private static final String SYS_PROP_DEVICE = "ro.product.board";
	
	private static final String KEY_FIRST_RUN = "firstRun";
	private static final String KEY_LAST_UPDATE_CHECK = "lastUpdateCheck";
	
	private static Preferences INSTANCE;
	
	private final SharedPreferences mPrefs;
	private final Resources mRes;
	
	private Preferences(SharedPreferences prefs, Resources res) {
		mPrefs = prefs;
		mRes = res;
	}
	
	public static synchronized Preferences getPreferences(Context ctx) {
		if(INSTANCE == null) {
			INSTANCE = new Preferences(PreferenceManager.getDefaultSharedPreferences(ctx), ctx.getResources());
		}
		
		return INSTANCE;
	}

	public int getUpdateFrequency() {
		try {
			return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.p_key_update_check_freq), ""));
		} catch (NumberFormatException ex) {
			return UPDATE_FREQ_AT_BOOT;
		}
	}
	
	public String getConfiguredModString() {
		return mPrefs.getString(mRes.getString(R.string.p_key_configured_updates_mod_version), null);
	}

	public void setConfiguredModString(String modString) {
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_key_configured_updates_mod_version), modString);
		if(!editor.commit()) Log.e(TAG, "Unable to write configured mod string");
	}
	
	public Uri getConfiguredRingtone() {
		String uri = mPrefs.getString(mRes.getString(R.string.p_key_notification_ringtone), null);
		if(uri == null) return null;
		
		return Uri.parse(uri);
	}
	
	public boolean isFirstRun() {
		return mPrefs.getBoolean(KEY_FIRST_RUN, true);
	}
	
	public void setFirstRun(boolean firstRun) {
		Editor editor = mPrefs.edit();
		editor.putBoolean(KEY_FIRST_RUN, firstRun);
		if(!editor.commit()) Log.e(TAG, "Unable to write first run bit");
	}
	
	public Date getLastUpdateCheck() {
		return new Date(mPrefs.getLong(KEY_LAST_UPDATE_CHECK, 0));
	}
	
	public void setLastUpdateCheck(Date d) {
		Editor editor = mPrefs.edit();
		editor.putLong(KEY_LAST_UPDATE_CHECK, d.getTime());
		if(!editor.commit()) Log.e(TAG, "Unable to write last update check");
	}
	

	public void configureModString() {
		String modString = getSystemModString();
		
		if(modString != null) {
			setConfiguredModString(modString);
			Log.i(TAG, "System mod cofigured to " + modString);
		}
	}
	
	public boolean showDowngrades() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_older_mod_versions), mRes.getBoolean(R.string.p_display_older_mod_versions_def_value));
	}
	
	public String getUpdateFileURL() {
		Log.d(TAG, "MetadataFile-Url: "+mPrefs.getString(mRes.getString(R.string.p_update_file_url),  mRes.getString(R.string.conf_update_server_url_def)));
		return mPrefs.getString(mRes.getString(R.string.p_update_file_url),  mRes.getString(R.string.conf_update_server_url_def));
	}
	
	public void setUpdateFileURL(String updateFileURL) {
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_update_file_url), updateFileURL);
		if(!editor.commit()) Log.e(TAG, "Unable to write Update File URL");
	}
	
	public void setNotificationRingtone(String RingTone) {
		Log.d(TAG, "Setting RingtoneURL to "+RingTone);
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_key_notification_ringtone), RingTone);
		if(!editor.commit()) Log.e(TAG, "Unable to write Ringtone URI");
	}
	
	public boolean allowExperimental() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_allow_experimental_versions), mRes.getBoolean(R.string.p_display_allow_experimental_versions_def_value));
	}
	
	public boolean doNandroidBackup() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_do_nandroid_backup), mRes.getBoolean(R.string.p_do_nandroid_backup_def_value));
	}
	
	public boolean getVibrate() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_vibrate), mRes.getBoolean(R.string.p_vibrate_def_value));
	}
	 
	public String getUpdateFolder() {
		return mPrefs.getString(mRes.getString(R.string.conf_update_folder), mRes.getString(R.string.conf_update_folder));
	}

	private String getSystemModString()
	{
		return SysUtils.getSystemProperty(SYS_PROP_DEVICE);
	}
}
