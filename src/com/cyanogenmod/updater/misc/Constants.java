package com.cyanogenmod.updater.misc;

import com.cyanogenmod.updater.customization.Customization;

public class Constants {
	//System Infos
	public static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"; 

	//UpdateInfo
	public static final String UPDATE_INFO_BRANCH_STABLE = "stable";
	public static final String UPDATE_INFO_BRANCH_NIGHTLY = "nightly";
	public static final String UPDATE_INFO_WILDCARD = "*";

	//JSON Objects
	public static final String JSON_UPDATE_LIST = "result";
	public static final String JSON_TIMESTAMP = "timestamp";
	public static final String JSON_URL = "url";
	public static final String JSON_DESCRIPTION = "description";
	public static final String JSON_BRANCH = "channel";
	public static final String JSON_FILENAME = "filename";
	public static final String JSON_MD5SUM = "md5sum";

	//Keys
	public static final String KEY_UPDATE_INFO = Customization.PACKAGE_FIRST_NAME + ".fullUpdateList";
	public static final String KEY_AVAILABLE_UPDATES = Customization.PACKAGE_FIRST_NAME + ".availableUpdates";

	//Flipper
	public static final int FLIPPER_AVAILABLE_UPDATES = 0;
	public static final int FLIPPER_EXISTING_UPDATES = 1;

	//Menu main Layout
	public static final int MENU_ID_UPDATE_NOW = 1;
	public static final int MENU_ID_CONFIG = 2;
	public static final int MENU_ID_ABOUT = 3;
	public static final int MENU_ID_CHANGELOG = 4;

	//Notifications
	public static final int NOTIFICATION_DOWNLOAD_STATUS = 100;
	public static final int NOTIFICATION_DOWNLOAD_FINISHED = 200;

	//Update Check Frequencies
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;

	//ChangelogHandler
	public static final String VERSION_TAG = "Version";
	public static final String VERSION_NAME_TAG = "name";

}
