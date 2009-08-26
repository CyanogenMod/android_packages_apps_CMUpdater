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
import android.widget.Toast;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.StartupReceiver;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ConfigActivity extends PreferenceActivity
{
	private static final String TAG = "<CM-Updater> ConfigActivity";
	private Preferences prefs;

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
		
		Preference pref = (Preference) findPreference(res.getString(R.string.p_update_file_url_qr));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				IntentIntegrator.initiateScan(ConfigActivity.this);
				Log.d(TAG, "Intent to Barcode Scanner Sent");
				return true;
			}
		});
		
		pref = (Preference) findPreference(res.getString(R.string.p_update_file_url_def));
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference)
			{
				prefs.setUpdateFileURL(res.getString(R.string.conf_update_server_url_def));
				Toast.makeText(getBaseContext(), R.string.p_update_file_url_changed, Toast.LENGTH_LONG).show();
				Log.d(TAG, "Update File URL set back to default: " + prefs.getUpdateFileURL());
				ConfigActivity.this.finish();
				return true;
			}
		});
		
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
		
		pref = (Preference) findPreference(res.getString(R.string.p_display_older_mod_versions));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Toast.makeText(getBaseContext(), R.string.p_display_older_mod_versions_changed, Toast.LENGTH_LONG).show();
				return true;
			}
		});

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
					prefs.setUpdateFileURL(result);
					Toast.makeText(getBaseContext(), "Update File URL: " + result, Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Scanned QR Code: " + scanResult.getContents());
					ConfigActivity.this.finish();
				}
				else
				{
					Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
				}
				
			}
			else
			{
				Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
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