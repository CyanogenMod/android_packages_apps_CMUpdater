package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import cmupdaterapp.ui.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences
{	
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;
	
	private static final String TAG = "<CM-Updater> Preferences";
	
	private static final String SYS_PROP_DEVICE = "ro.product.board";
	
	private static Preferences INSTANCE;
	
	private final SharedPreferences mPrefs;
	private final Resources mRes;
	
	private Preferences(SharedPreferences prefs, Resources res)
	{
		mPrefs = prefs;
		mRes = res;
	}
	
	public static synchronized Preferences getPreferences(Context ctx)
	{
		if(INSTANCE == null)
		{
			INSTANCE = new Preferences(PreferenceManager.getDefaultSharedPreferences(ctx), ctx.getResources());
		}
		
		return INSTANCE;
	}

	public int getUpdateFrequency()
	{
		try
		{
			return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.p_key_update_check_freq), ""));
		}
		catch (NumberFormatException ex)
		{
			return UPDATE_FREQ_AT_BOOT;
		}
	}
	
	public String getConfiguredModString()
	{
		return mPrefs.getString(mRes.getString(R.string.p_key_configured_updates_mod_version), null);
	}

	public void setConfiguredModString(String modString)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_key_configured_updates_mod_version), modString);
		if(!editor.commit()) Log.e(TAG, "Unable to write configured mod string");
	}
	
	public Uri getConfiguredRingtone()
	{
		String uri = mPrefs.getString(mRes.getString(R.string.p_key_notification_ringtone), null);
		if(uri == null) return null;
		
		return Uri.parse(uri);
	}
	
	public boolean isFirstRun()
	{
		return mPrefs.getBoolean(mRes.getString(R.string.p_first_run), true);
	}
	
	public void setFirstRun(boolean firstRun)
	{
		Editor editor = mPrefs.edit();
		editor.putBoolean(mRes.getString(R.string.p_first_run), firstRun);
		if(!editor.commit()) Log.e(TAG, "Unable to write first run bit");
	}
	
	public Date getLastUpdateCheck()
	{
		return new Date(mPrefs.getLong(mRes.getString(R.string.p_last_update_check), 0));
	}
	
	public void setLastUpdateCheck(Date d)
	{
		Editor editor = mPrefs.edit();
		editor.putLong(mRes.getString(R.string.p_last_update_check), d.getTime());
		if(!editor.commit()) Log.e(TAG, "Unable to write last update check");
	}

	public void configureModString()
	{
		String modString = getSystemModString();
		if(modString != null)
		{
			setConfiguredModString(modString);
			Log.i(TAG, "System mod cofigured to " + modString);
		}
	}
	
	public boolean showDowngrades()
	{
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_older_mod_versions), Boolean.valueOf(mRes.getString(R.string.p_display_older_mod_versions_def_value)));
	}
	
	public String getRomUpdateFileURL()
	{
		Log.d(TAG, "Rom MetadataFile-Url: "+ mPrefs.getString(mRes.getString(R.string.p_update_file_url),  mRes.getString(R.string.conf_update_server_url_def)));
		return mPrefs.getString(mRes.getString(R.string.p_update_file_url),  mRes.getString(R.string.conf_update_server_url_def));
	}

	public String getThemeUpdateFileURL()
	{
		Log.d(TAG, "Theme MetadataFile-Url: "+ mPrefs.getString(mRes.getString(R.string.p_theme_file_url),  mRes.getString(R.string.conf_theme_server_url_def)));
		return mPrefs.getString(mRes.getString(R.string.p_theme_file_url),  mRes.getString(R.string.conf_theme_server_url_def));
	}
	
	public void setRomUpdateFileURL(String updateFileURL)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_update_file_url), updateFileURL);
		if(!editor.commit()) Log.e(TAG, "Unable to write Rom Update File URL");
	}

	public void setThemeUpdateFileURL(String updateFileURL)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_theme_file_url), updateFileURL);
		if(!editor.commit()) Log.e(TAG, "Unable to write Theme Update File URL");
	}
	
	public void setNotificationRingtone(String RingTone)
	{
		Log.d(TAG, "Setting RingtoneURL to " + RingTone);
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_key_notification_ringtone), RingTone);
		if(!editor.commit()) Log.e(TAG, "Unable to write Ringtone URI");
	}
	
	public boolean allowExperimental()
	{
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_allow_experimental_versions), Boolean.valueOf(mRes.getString(R.string.p_display_allow_experimental_versions_def_value)));
	}
	
	public boolean doNandroidBackup()
	{
		//return mPrefs.getBoolean(mRes.getString(R.string.p_do_nandroid_backup), Boolean.valueOf(mRes.getString(R.string.p_do_nandroid_backup_def_value)));
		return false;
	}
	
	public boolean getVibrate()
	{
		return mPrefs.getBoolean(mRes.getString(R.string.p_vibrate), Boolean.valueOf(mRes.getString(R.string.p_vibrate_def_value)));
	}
	
	public boolean notificationsEnabled()
	{
		return mPrefs.getBoolean(mRes.getString(R.string.p_notifications), Boolean.valueOf(mRes.getString(R.string.p_notifications_def_value)));
	}
	 
	public String getUpdateFolder()
	{
		return mRes.getString(R.string.conf_update_folder);
	}
	
	public int getProgressUpdateFreq()
	{
		return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.p_progress_update_frequency), mRes.getString(R.string.p_progress_update_frequency_def)));
	}

	public void setProgressUpdateFreq(String freq)
	{
		Log.d(TAG, "Setting ProgressUpdate Frequency to " + freq);
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_progress_update_frequency), freq);
		if(!editor.commit()) Log.e(TAG, "Unable to write Update Frequency");
	}
	
	private String getSystemModString()
	{
		return SysUtils.getSystemProperty(SYS_PROP_DEVICE);
	}
	
	public String getAboutURL()
	{
		return mRes.getString(R.string.conf_about_url);
	}
	
	public String getChangelogURL()
	{
		return mRes.getString(R.string.conf_changelog_url);
	}
	
	public String getThemeFile()
	{
		Log.d(TAG, "ThemeFile: "+ mPrefs.getString(mRes.getString(R.string.p_theme_file), mRes.getString(R.string.conf_theme_version_file_def)));
		return mPrefs.getString(mRes.getString(R.string.p_theme_file), mRes.getString(R.string.conf_theme_version_file_def));
	}
	
	public void setThemeFile(String path)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.p_theme_file), path);
		if(!editor.commit()) Log.e(TAG, "Unable to write Theme File Path");
	}
	
	public String[] getThemeInformations()
	{
		File f = new File(getThemeFile());
		if (f != null && f.exists() && f.canRead())
		{
			try
			{
				FileReader input = new FileReader(f);
				BufferedReader bufRead = new BufferedReader(input);
				String firstLine = bufRead.readLine();
				bufRead.close();
				input.close();
				//Empty File prevention
				if(firstLine != null)
				{
					String[] Return = firstLine.split("\\|");
					//Only return if there was a string like Hero|2.0.1
					if (Return.length == 2)
						return Return;
				}
			}
			catch (FileNotFoundException e)
			{
				Log.e(TAG, "File not Found", e);
			}
			catch (IOException e)
			{
				Log.e(TAG, "Exception in readline", e);
			}
		}
		else
		{
			Log.d(TAG, "No Theme File found. Probably no Theme installed, Theme Path not configured, or Bad Theme File");
		}
		return null;
	}
	
	public int[] convertVersionToIntArray(String oriVersion)
	{
		String version[] = oriVersion.split("\\.");
		int[] retValue = new int[version.length];
		try
		{
			for(int i = 0; i < version.length; i++)
			{
				retValue[i] = Integer.parseInt(version[i]);
			}
			return retValue;
		}
		catch (NumberFormatException e)
		{
			return new int[0];
		}
	}
}