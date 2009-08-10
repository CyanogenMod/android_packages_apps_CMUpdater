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
	/*public static final int UPDATE_FREQ_EVERY_HOUR = 3600;
	public static final int UPDATE_FREQ_EVERY_DAY =  86400;
	public static final int UPDATE_FREQ_EVERY_WEEK = 604800;*/
	
	private static final String TAG = "Preferences";

	// getprop keys
	//private static final String PROP_PRODUCT_MODEL = "ro.product.model";
	//private static final String PROP_COUNTRY_CODE = "ro.product.locale.region";
	
	// values returned by getprop
	//private static final String PROP_PRODUCT_MODEL_ADP1_VALUE = "Android Dev Phone 1";
	//private static final String PROP_PRODUCT_MODEL_US_VALUE = "T-Mobile G1";
	//private static final String PROP_PRODUCT_MODEL_UK_VALUE = "T-Mobile G1";
	
	//private static final String PROP_COUNTRY_CODE_ADP1_VALUE = "US";
	//private static final String PROP_COUNTRY_CODE_US_VALUE = "US";
	//private static final String PROP_COUNTRY_CODE_UK_VALUE = "GB";
	
	//Keys used in update file
	private static final String MOD_VERSION_ADP1 = "ADP1";
	//private static final String MOD_VERSION_US = "US";
	//private static final String MOD_VERSION_UK = "UK";
	
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
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_older_mod_versions), false);
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
	
	public boolean allowExperimental() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_display_allow_experimental_versions), false);
	}
	
	public boolean doNandroidBackup() {
		return mPrefs.getBoolean(mRes.getString(R.string.p_do_nandroid_backup), false);
	}
	
	public String getUpdateFolder() {
		return mPrefs.getString(mRes.getString(R.string.conf_update_folder), mRes.getString(R.string.conf_update_folder));
	}

	private String getSystemModString() {
		
		/**
		
		String prodMod = SysUtils.getSystemProperty(PROP_PRODUCT_MODEL);
		String country = SysUtils.getSystemProperty(PROP_COUNTRY_CODE);
		
		if(PROP_PRODUCT_MODEL_ADP1_VALUE.equalsIgnoreCase(prodMod)) {
			
		}
		
		if(PROP_PRODUCT_MODEL_UK_VALUE.equalsIgnoreCase(prodMod) &&
				PROP_COUNTRY_CODE_UK_VALUE.equalsIgnoreCase(country)) {
			return MOD_VERSION_UK;
		}
		
		if(PROP_PRODUCT_MODEL_US_VALUE.equalsIgnoreCase(prodMod) &&
				PROP_COUNTRY_CODE_US_VALUE.equalsIgnoreCase(country)) {
			return MOD_VERSION_US;
		}
		
		*/
		
		return MOD_VERSION_ADP1;

		/**
		
		Log.e(TAG, "Unable to determine system mod. " +
				"Please report these values: Product model:" + prodMod + "; country:" + country);
		return "*";
		
		*/
	}
}
