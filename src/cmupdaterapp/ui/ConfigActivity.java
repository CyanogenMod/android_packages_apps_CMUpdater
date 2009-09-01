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
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.StartupReceiver;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ConfigActivity extends PreferenceActivity
{
	private static final String TAG = "<CM-Updater> ConfigActivity";
	private Preferences prefs;
	private boolean RomBarcodeRequested;
	private boolean ThemeBarcodeRequested;

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
		final Resources res = getResources();

		ListPreference updateCheckFreqPref = (ListPreference) findPreference(res.getString(R.string.p_key_update_check_freq));

		updateCheckFreqPref.setOnPreferenceChangeListener(mUpdateCheckingFrequencyListener);
		
		//Barcodescanning Stuff
		Preference pref = (Preference) findPreference(res.getString(R.string.p_update_file_url_qr));
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

		pref = (Preference) findPreference(res.getString(R.string.p_theme_file_url_qr));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				ThemeBarcodeRequested = true;
				IntentIntegrator.initiateScan(ConfigActivity.this);
				Log.d(TAG, "Starting Barcodescanner for Theme Update File");
				return true;
			}
		});
		
		//Reset Update URLs
		pref = (Preference) findPreference(res.getString(R.string.p_update_file_url_def));
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
		
		pref = (Preference) findPreference(res.getString(R.string.p_theme_file_url_def));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setThemeUpdateFileURL(res.getString(R.string.conf_theme_server_url_def));
				Toast.makeText(getBaseContext(), R.string.p_theme_file_url_changed, Toast.LENGTH_LONG).show();
				Log.d(TAG, "Theme Update File URL set back to default: " + prefs.getThemeUpdateFileURL());
				ConfigActivity.this.finish();
				return true;
			}
		});
		
		//URL Validation checkers
		pref = (Preference) findPreference(res.getString(R.string.p_update_file_url));
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
		
		pref = (Preference) findPreference(res.getString(R.string.p_theme_file_url));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if(URLUtil.isValidUrl(String.valueOf(newValue)))
				{
					prefs.setThemeUpdateFileURL(String.valueOf(newValue));
					Log.d(TAG, "Theme Update URL Set to: " + String.valueOf(newValue));
				}
				else
				{
					Toast.makeText(getBaseContext(), R.string.p_invalid_url, Toast.LENGTH_LONG).show();
					Log.d(TAG, "Entered Theme Update URL not valid: " + String.valueOf(newValue));
				}
				return true;
			}
		});
		
		//Progress Update Frequency
		pref = (Preference) findPreference(res.getString(R.string.p_progress_update_freq_def));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setProgressUpdateFreq(res.getString(R.string.p_progress_update_frequency_def));
				Toast.makeText(getBaseContext(), R.string.p_progress_update_freq_def_toast, Toast.LENGTH_LONG).show();
				Log.d(TAG, "ProgressUpdateFreq set back to default: " + prefs.getProgressUpdateFreq());
				return true;
			}
		});
		
		//Display All Updates
		pref = (Preference) findPreference(res.getString(R.string.p_display_older_mod_versions));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_display_older_mod_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		//Show Experimental
		pref = (Preference) findPreference(res.getString(R.string.p_display_allow_experimental_versions));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_display_allow_experimental_versions_changed, Toast.LENGTH_LONG).show();
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
						Log.d(TAG, "Requested Theme Barcodescan? " + String.valueOf(ThemeBarcodeRequested));
						if (RomBarcodeRequested)
						{
							Log.d(TAG, "Setting Rom Update File to " + result);
							prefs.setRomUpdateFileURL(result);
							Toast.makeText(getBaseContext(), "Rom Update File URL: " + result, Toast.LENGTH_SHORT).show();
						}
						else if (ThemeBarcodeRequested)
						{
							Log.d(TAG, "Setting Theme Update File to " + result);
							prefs.setThemeUpdateFileURL(result);
							Toast.makeText(getBaseContext(), "Theme Update File URL: " + result, Toast.LENGTH_SHORT).show();
						}
						RomBarcodeRequested = false;
						ThemeBarcodeRequested = false;
						ConfigActivity.this.finish();
					}
					else
					{
						Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
						RomBarcodeRequested = false;
						ThemeBarcodeRequested = false;
					}
					
				}
				else
				{
					Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
					RomBarcodeRequested = false;
					ThemeBarcodeRequested = false;
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