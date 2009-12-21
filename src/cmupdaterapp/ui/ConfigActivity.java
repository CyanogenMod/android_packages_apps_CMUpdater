package cmupdaterapp.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.webkit.URLUtil;
import android.widget.Toast;
import cmupdaterapp.service.StartupReceiver;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.misc.Log;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ConfigActivity extends PreferenceActivity
{
	private static final String TAG = "ConfigActivity";
	private Preferences prefs;
	private boolean RomBarcodeRequested;
	private Resources res;
	
	private final Preference.OnPreferenceChangeListener mUpdateCheckingFrequencyListener = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			int updateFreq = Integer.parseInt((String) newValue) * 1000;

			if(updateFreq > 0)
				StartupReceiver.scheduleUpdateService(ConfigActivity.this, updateFreq);
			else
				StartupReceiver.cancelUpdateChecks(ConfigActivity.this);

			return true;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.config);
		
		prefs = Preferences.getPreferences(ConfigActivity.this);
		res = getResources();

		ListPreference updateCheckFreqPref = (ListPreference) findPreference(res.getString(R.string.PREF_UPDATE_CHECK_FREQUENCY));

		updateCheckFreqPref.setOnPreferenceChangeListener(mUpdateCheckingFrequencyListener);
		
		//Barcodescanning Stuff
		Preference pref = (Preference) findPreference(res.getString(R.string.PREF_ROM_UPDATE_FILE_QR));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				RomBarcodeRequested = true;
				IntentIntegrator.initiateScan(ConfigActivity.this);
				Log.d(TAG, "Starting Barcodescanner for Rom Update File");
				return true;
			}
		});
		
		//Reset Update URLs
		pref = (Preference) findPreference(res.getString(R.string.PREF_ROM_UPDATE_FILE_URL_DEF));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setRomUpdateFileURL(res.getString(R.string.conf_update_server_url_def));
				Toast.makeText(getBaseContext(), R.string.p_update_file_url_changed, Toast.LENGTH_LONG).show();
				Log.d(TAG, "Rom Update File URL set back to default: " + prefs.getRomUpdateFileURL());
				ConfigActivity.this.finish();
				return true;
			}
		});
		
		//Reset themes.theme
		pref = (Preference) findPreference(res.getString(R.string.PREF_THEMES_THEME_FILE_DEF));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setThemeFile(res.getString(R.string.conf_theme_version_file_def));
				Toast.makeText(getBaseContext(), R.string.p_theme_file_def_toast, Toast.LENGTH_LONG).show();
				Log.d(TAG, "Theme File Path set back to default: " + prefs.getThemeFile());
				ConfigActivity.this.finish();
				return true;
			}
		});

		//Reset UpdateFolder
		pref = (Preference) findPreference(res.getString(R.string.PREF_UPDATE_FOLDER_DEF));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				if(prefs.setUpdateFolder(res.getString(R.string.conf_update_folder)))
				{
					Toast.makeText(getBaseContext(), R.string.p_update_folder_def_toast, Toast.LENGTH_LONG).show();
					Log.d(TAG, "UpdateFolder set back to default: " + prefs.getUpdateFolder());
				}
				else
				{
					Toast.makeText(getBaseContext(), R.string.p_update_folder_error, Toast.LENGTH_LONG).show();
					Log.d(TAG, "Error on Setting UpdateFolder: " + prefs.getUpdateFolder());
				}
				ConfigActivity.this.finish();
				return true;
			}
		});
		
		//URL Validation checkers
		pref = (Preference) findPreference(res.getString(R.string.PREF_ROM_UPDATE_FILE_URL));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if(URLUtil.isValidUrl(String.valueOf(newValue)))
				{
					prefs.setRomUpdateFileURL(String.valueOf(newValue));
					Log.d(TAG, "Rom Update URL Set to: " + String.valueOf(newValue));
				}
				else
				{
					Toast.makeText(getBaseContext(), R.string.p_invalid_url, Toast.LENGTH_LONG).show();
					Log.d(TAG, "Entered Rom Update URL not valid: " + String.valueOf(newValue));
				}
				return true;
			}
		});
		
		//Progress Update Frequency
		pref = (Preference) findPreference(res.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setProgressUpdateFreq(res.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF_VALUE));
				Toast.makeText(getBaseContext(), R.string.p_progress_update_freq_def_toast, Toast.LENGTH_LONG).show();
				Log.d(TAG, "ProgressUpdateFreq set back to default: " + prefs.getProgressUpdateFreq());
				return true;
			}
		});
		
		//Display All Rom Updates
		pref = (Preference) findPreference(res.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_display_older_rom_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		//Show Experimental Roms
		pref = (Preference) findPreference(res.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_ROM_VERSIONS));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_allow_experimental_rom_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		//Display All Theme Updates
		pref = (Preference) findPreference(res.getString(R.string.PREF_DISPLAY_OLDER_THEME_VERSIONS));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_display_older_theme_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		//Show Experimental Themes
		pref = (Preference) findPreference(res.getString(R.string.PREF_DISPLAY_EXPERIMENTAL_THEME_VERSIONS));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_allow_experimental_theme_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		//Change Update Folder
		pref = (Preference) findPreference(res.getString(R.string.PREF_UPDATE_FOLDER));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if(prefs.setUpdateFolder((String)newValue))
				{
					Log.d(TAG, "UpdateFolder set to: " + prefs.getUpdateFolder());
				}
				else
				{
					Toast.makeText(getBaseContext(), R.string.p_update_folder_error, Toast.LENGTH_LONG).show();
					Log.d(TAG, "Error on Setting UpdateFolder: " + prefs.getUpdateFolder());
				}
				ConfigActivity.this.finish();
				return true;
			}
		});
		
		//Display List of Themes
		pref = (Preference) findPreference(res.getString(R.string.PREF_THEMES_LIST));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				Intent i = new Intent(ConfigActivity.this, ThemeListActivity.class);
				startActivity(i);
				return true;
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		Log.d(TAG, "onActivityResult requestCode: "+requestCode);
		//Switch is necessary, because RingtonePreference and QRBarcodeScanner call the same Event
		switch (requestCode)
		{
			//QR Barcode scanner
			case IntentIntegrator.REQUEST_CODE:
				IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
				if (null != scanResult)
				{
					String result = scanResult.getContents();
					if (null != result && !result.equals("") )
					{
						Log.d(TAG, "Requested Rom Barcodescan? " + String.valueOf(RomBarcodeRequested));
						if (RomBarcodeRequested)
						{
							Log.d(TAG, "Setting Rom Update File to " + result);
							if(URLUtil.isValidUrl(result))
							{
								prefs.setRomUpdateFileURL(result);
								Toast.makeText(getBaseContext(), res.getString(R.string.p_update_file_changed_toast) + result, Toast.LENGTH_LONG).show();
							}
							else
							{
								Toast.makeText(getBaseContext(), R.string.p_invalid_url, Toast.LENGTH_LONG).show();
								Log.d(TAG, "Entered Rom Update URL not valid: " + result);
							}
						}
						else
						{
							//Something wrong here. Barcodescan requested but no Variables set
							Toast.makeText(getBaseContext(), R.string.p_barcode_scan_failure, Toast.LENGTH_LONG).show();
							Log.d(TAG, "Something wrong here. Barcodescan requested but no Variables set");
						}
						RomBarcodeRequested = false;
						ConfigActivity.this.finish();
					}
					else
					{
						Toast.makeText(getBaseContext(), R.string.barcode_scan_no_result, Toast.LENGTH_LONG).show();
						RomBarcodeRequested = false;
					}
					
				}
				else
				{
					Toast.makeText(getBaseContext(), R.string.barcode_scan_no_result, Toast.LENGTH_LONG).show();
					RomBarcodeRequested = false;
				}
				break;
			//RingtonePicker
			case 100:
				//Needs to be an Object, because when giving the toString() here, it crashes when NULL is returned
				//Object ringtone = intent.getExtras().get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				//intent = null when pressing back on the ringtonpickerdialog
				if (intent == null)
					break;
				Uri ringtone = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI); 
				if (ringtone != null)
					prefs.setNotificationRingtone(ringtone.toString());
				else
					prefs.setNotificationRingtone(null);
				break;
			default:
				break;
		}
	}
}