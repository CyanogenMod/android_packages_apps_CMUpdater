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

package cmupdater.ui;

import cmupdater.ui.R;
import cmupdater.utils.StartupReceiver;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class ConfigActivity extends PreferenceActivity {
	
	//private static final String TAG = "ConfigActivity";
	
	private final Preference.OnPreferenceChangeListener mUpdateCheckingFrequencyListener = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.config);
        
        ListPreference updateCheckFreqPref = (ListPreference) findPreference(getResources().getString(R.string.p_key_update_check_freq));
        
        updateCheckFreqPref.setOnPreferenceChangeListener(mUpdateCheckingFrequencyListener);
    }
}