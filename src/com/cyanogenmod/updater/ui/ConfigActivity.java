package com.cyanogenmod.updater.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.webkit.URLUtil;
import android.widget.Toast;
import com.cyanogenmod.updater.customization.Customization;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.receiver.StartupReceiver;
import com.cyanogenmod.updater.utils.Preferences;

public class ConfigActivity extends PreferenceActivity {
    private static final String TAG = "ConfigActivity";

    private Preferences prefs;
    private boolean RomBarcodeRequested;
    private Resources res;

    private final Preference.OnPreferenceChangeListener mUpdateCheckingFrequencyListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int updateFreq = Integer.parseInt((String) newValue) * 1000;

            if (updateFreq > 0)
                StartupReceiver.scheduleUpdateService(ConfigActivity.this, updateFreq);
            else
                StartupReceiver.cancelUpdateChecks(ConfigActivity.this);

            return true;
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            prefs = new Preferences(this);
            res = getResources();

            ListPreference updateCheckFreqPref = (ListPreference) findPreference(res.getString(R.string.PREF_UPDATE_CHECK_FREQUENCY));

            updateCheckFreqPref.setOnPreferenceChangeListener(mUpdateCheckingFrequencyListener);

            //Reset UpdateFolder
            Preference pref = findPreference(res.getString(R.string.PREF_UPDATE_FOLDER_DEF));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                    if (prefs.setUpdateFolder(Customization.DOWNLOAD_DIR)) {
                    Toast.makeText(getBaseContext(), R.string.p_update_folder_def_toast, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "UpdateFolder set back to default: " + prefs.getUpdateFolder());
                    } else {
                    Toast.makeText(getBaseContext(), R.string.p_update_folder_error, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Error on Setting UpdateFolder: " + prefs.getUpdateFolder());
                    }
                    ConfigActivity.this.finish();
                    return true;
                    }
                    });

            //Progress Update Frequency
            pref = findPreference(res.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF));
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                    prefs.setProgressUpdateFreq(res.getString(R.string.PREF_PROGRESS_UPDATE_FREQUENCY_DEF_VALUE));
                    Toast.makeText(getBaseContext(), R.string.p_progress_update_freq_def_toast, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "ProgressUpdateFreq set back to default: " + prefs.getProgressUpdateFreq());
                    return true;
                    }
                    });

            //Display All Rom Updates
            pref = findPreference(res.getString(R.string.PREF_DISPLAY_OLDER_ROM_VERSIONS));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getBaseContext(), R.string.p_display_older_rom_versions_changed, Toast.LENGTH_LONG).show();
                    return true;
                    }
                    });

            //Show Nightly Roms
            pref = findPreference(res.getString(R.string.PREF_DISPLAY_NIGHTLY_ROM_VERSIONS));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getBaseContext(), R.string.p_allow_nightly_rom_versions_changed, Toast.LENGTH_LONG).show();
                    return true;
                    }
                    });

            //Change Update Folder
            pref = findPreference(res.getString(R.string.PREF_UPDATE_FOLDER));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (prefs.setUpdateFolder((String) newValue)) {
                    Log.d(TAG, "UpdateFolder set to: " + prefs.getUpdateFolder());
                    } else {
                    Toast.makeText(getBaseContext(), R.string.p_update_folder_error, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Error on Setting UpdateFolder: " + prefs.getUpdateFolder());
                    }
                    ConfigActivity.this.finish();
                    return true;
                    }
                    });

            //Display Debug Output
            pref = findPreference(res.getString(R.string.PREF_DEBUG_OUTPUT));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getBaseContext(), R.string.p_debug_output_changed, Toast.LENGTH_LONG).show();
                    return true;
                    }
                    });
        }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult requestCode: " + requestCode);
        //Needs to be an Object, because when giving the toString() here, it crashes when NULL is returned
        //Object ringtone = intent.getExtras().get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        //intent = null when pressing back on the ringtonpickerdialog
        if (intent == null)
            return;
        Uri ringtone = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI); 
        if (ringtone != null)
            prefs.setNotificationRingtone(ringtone.toString());
        else
            prefs.setNotificationRingtone(null);
    }
}
