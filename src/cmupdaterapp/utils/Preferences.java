package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

import cmupdaterapp.customTypes.FullThemeList;
import cmupdaterapp.customTypes.ThemeInfo;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.customization.Customization;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Preferences extends Activity
{
	private static final String TAG = "Preferences";

	private Boolean showDebugOutput = false;
	
	private final SharedPreferences mPrefs;
	private final Resources mRes;
	private String temp;
	private boolean tempbool;

	public Preferences(Context ctx)
	{
		mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		mRes = ctx.getResources();
		showDebugOutput = displayDebugOutput();
		if (showDebugOutput) Log.d(TAG, "Preference Instance set.");
	}

	public int getUpdateFrequency()
	{
		try
		{
			return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.PREF_UPDATE_CHECK_FREQUENCY), ""));
		}
		catch (NumberFormatException ex)
		{
			return Constants.UPDATE_FREQ_AT_BOOT;
		}
	}

	public Date getLastUpdateCheck()
	{
		return new Date(mPrefs.getLong(mRes.getString(R.string.PREF_LAST_UPDATE_CHECK), 0));
	}

	public String getLastUpdateCheckString()
	{
		Date d = getLastUpdateCheck();
		if (d.getTime() == 0)
			return mRes.getString(R.string.no_updatecheck_executed);
		else
			return DateFormat.getDateTimeInstance().format(d);
	}

	public void setLastUpdateCheck(Date d)
	{
		Editor editor = mPrefs.edit();
		editor.putLong(mRes.getString(R.string.PREF_LAST_UPDATE_CHECK), d.getTime());
		if(!editor.commit()) Log.e(TAG, "Unable to write last update check");
	}

	public String getBoardString()
	{
		temp = SysUtils.getSystemProperty(Customization.BOARD);
		if (showDebugOutput) Log.d(TAG, "Board: " + temp);
		return temp;
	}

	public String getChangelogURL()
	{
		temp = mRes.getString(R.string.conf_changelog_url);
		if (showDebugOutput) Log.d(TAG, "ChangelogURL: " + temp);
		return temp;
	}

	public String getFeaturedThemesURL()
	{
		temp = mRes.getString(R.string.conf_featured_themes_url);
		if (showDebugOutput) Log.d(TAG, "FeaturedThemesURL: " + temp);
		return temp;
	}

	//Roms
	public boolean showAllRomUpdates()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS_DEF_VALUE))); 
		if (showDebugOutput) Log.d(TAG, "Display All Rom Updates: " + tempbool);
		return tempbool;
	}

	public boolean showExperimentalRomUpdates()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_ROM_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_ROM_VERSIONS_DEF_VALUE)));
		if (showDebugOutput) Log.d(TAG, "Display Experimental Rom Updates: " + tempbool);
		return tempbool;
	}

	public String getRomUpdateFileURL()
	{
		temp = mPrefs.getString(mRes.getString(R.string.PREF_ROM_UPDATE_FILE_URL),  mRes.getString(R.string.conf_update_server_url_def));
		if (showDebugOutput) Log.d(TAG, "Rom MetadataFile-Url: " + temp);
		return temp;
	}

	public void setRomUpdateFileURL(String updateFileURL)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.PREF_ROM_UPDATE_FILE_URL), updateFileURL);
		if(!editor.commit()) Log.e(TAG, "Unable to write Rom Update File URL");
	}

	//Themes
	public boolean showAllThemeUpdates()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_OLDER_THEME_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_OLDER_THEME_VERSIONS_DEF_VALUE)));
		if (showDebugOutput) Log.d(TAG, "Display All Theme Updates: " + tempbool);
		return tempbool;
	}

	public boolean showExperimentalThemeUpdates()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_THEME_VERSIONS), Boolean.valueOf(mRes.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_THEME_VERSIONS_DEF_VALUE)));
		if (showDebugOutput) Log.d(TAG, "Display Experimental Theme Updates: " + tempbool);
		return tempbool;
	}

	public LinkedList<ThemeList> getThemeUpdateUrls()
	{
		DbAdapter themeListDb = new DbAdapter(showDebugOutput);
		if (showDebugOutput) Log.d(TAG, "Opening Database");
		themeListDb.open();
		//Get the actual ThemeList from the Database
		Cursor themeListCursor = themeListDb.getAllThemesCursor();
		startManagingCursor(themeListCursor);
		themeListCursor.requery();
		FullThemeList fullThemeList = new FullThemeList();
		if (themeListCursor.moveToFirst())
		{
			do
			{
				String name = themeListCursor.getString(DbAdapter.COLUMN_THEMELIST_NAME);
				String uri = themeListCursor.getString(DbAdapter.COLUMN_THEMELIST_URI);
				int pk = themeListCursor.getInt(DbAdapter.COLUMN_THEMELIST_ID);
				int enabled = themeListCursor.getInt(DbAdapter.COLUMN_THEMELIST_ENABLED);
				ThemeList newItem = new ThemeList();
				newItem.name = name;
				newItem.url = URI.create(uri);
				if(enabled == 1) newItem.enabled = true;
				else newItem.enabled = false;
				newItem.PrimaryKey = pk;
				fullThemeList.addThemeToList(newItem);
			}
			while(themeListCursor.moveToNext());
		}
		if (showDebugOutput) Log.d(TAG, "Closing Database");
		themeListDb.close();
		return fullThemeList.returnFullThemeList();	
	}

	public String getThemeFile()
	{
		temp = mPrefs.getString(mRes.getString(R.string.PREF_THEMES_THEME_FILE), mRes.getString(R.string.conf_theme_version_file_def));
		if (showDebugOutput) Log.d(TAG, "ThemeFile: " + temp);
		return temp;
	}

	public void setThemeFile(String path)
	{
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.PREF_THEMES_THEME_FILE), path);
		if(!editor.commit()) Log.e(TAG, "Unable to write Theme File Path");
	}

	public boolean ThemeUpdateUrlSet()
	{
		if(getThemeUpdateUrls().size()==0)
			return false;
		else
			return true;
	}

	//Notifications
	public boolean notificationsEnabled()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_NOTIFICATION_ENABLED), Boolean.valueOf(mRes.getString(R.string.PREF_NOTIFICATION_ENABLED_DEF_VALUE)));
		if (showDebugOutput) Log.d(TAG, "Notifications Enabled: " + tempbool);
		return tempbool;
	}

	public boolean getVibrate()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_NOTIFICATION_VIBRATE), Boolean.valueOf(mRes.getString(R.string.PREF_NOTIFICATION_VIBRATE_DEF_VALUE)));
		if (showDebugOutput) Log.d(TAG, "Notification Vibrate: " + tempbool);
		return tempbool;
	}

	public Uri getConfiguredRingtone()
	{
		String uri = mPrefs.getString(mRes.getString(R.string.PREF_NOTIFICATION_RINGTONE), null);
		if(uri == null) return null;
		
		return Uri.parse(uri);
	}

	public void setNotificationRingtone(String RingTone)
	{
		if (showDebugOutput) Log.d(TAG, "Setting RingtoneURL to " + RingTone);
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.PREF_NOTIFICATION_RINGTONE), RingTone);
		if(!editor.commit()) Log.e(TAG, "Unable to write Ringtone URI");
	}

	public boolean doNandroidBackup()
	{
		//tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DO_NANDROID_BACKUP), Boolean.valueOf(mRes.getString(R.string.PREF_DO_NANDROID_BACKUP_DEF_VALUE)));
		//Log.d(TAG, "Do Nandroid Backup: " + tempbool);
		//return tempbool;
		return false;
	}

	//Advanced Properties
	public String getUpdateFolder()
	{
		temp = mPrefs.getString(mRes.getString(R.string.PREF_UPDATE_FOLDER), mRes.getString(R.string.conf_update_folder)).trim();
		if (showDebugOutput) Log.d(TAG, "UpdateFolder: " + temp);
		return temp;
	}

	public boolean setUpdateFolder(String folder)
	{
		String folderTrimmed = folder.trim();
		File f = new File(Environment.getExternalStorageDirectory() + "/" + folderTrimmed);
		if (f.isFile())
			return false;
		if (!f.exists())
			f.mkdirs();
		if (f.exists() && f.isDirectory())
		{
			Editor editor = mPrefs.edit();
			editor.putString(mRes.getString(R.string.PREF_UPDATE_FOLDER), folderTrimmed);
			if(!editor.commit())
			{
				Log.e(TAG, "Unable to write Update Folder Path");
				return false;
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getProgressUpdateFreq()
	{
		temp = mPrefs.getString(mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY), mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF_VALUE));
		if (showDebugOutput) Log.d(TAG, "ProgressUpdateFrequency: " + temp);
		return Integer.parseInt(temp);
	}

	public void setProgressUpdateFreq(String freq)
	{
		if (showDebugOutput) Log.d(TAG, "Setting ProgressUpdate Frequency to " + freq);
		Editor editor = mPrefs.edit();
		editor.putString(mRes.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY), freq);
		if(!editor.commit()) Log.e(TAG, "Unable to write Update Frequency");
	}
	
	public boolean displayDebugOutput()
	{
		tempbool = mPrefs.getBoolean(mRes.getString(R.string.PREF_DEBUG_OUTPUT), Boolean.valueOf(mRes.getString(R.string.PREF_DEBUG_OUTPUT_DEF_VALUE))); 
		if (showDebugOutput) Log.d(TAG, "Display Debug Output: " + tempbool);
		return tempbool;
	}

	public ThemeInfo getThemeInformations()
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
					ThemeInfo t = new ThemeInfo();
					if (firstLine.equalsIgnoreCase(Constants.UPDATE_INFO_WILDCARD))
					{
						t.name = Constants.UPDATE_INFO_WILDCARD;
						if (showDebugOutput) Log.d(TAG, "Wildcard in themes.theme");
						return t;
					}
					String[] Return = firstLine.split("\\|");
					//Only return if there was a string like Hero|2.0.1
					if (Return.length == 2)
					{
						t.name = Return[0];
						t.version = Return[1];
						return t;
					}
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
			if (showDebugOutput) Log.d(TAG, "No Theme File found. Using Wildcard for Theme Updates instead");
		}
		return null;
	}
}