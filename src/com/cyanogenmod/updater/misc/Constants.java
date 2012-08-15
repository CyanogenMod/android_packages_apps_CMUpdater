package com.cyanogenmod.updater.misc;

import com.cyanogenmod.updater.customization.Customization;

public class Constants {
	//System Infos
	public static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"; 

	//UpdateInfo
	public static final String UPDATE_INFO_BRANCH_STABLE = "stable";
	public static final String UPDATE_INFO_BRANCH_EXPERIMENTAL = "nightly";
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
	public static final int FLIPPER_AVAILABLE_THEMES = 2;

	//Menu main Layout
	public static final int MENU_ID_UPDATE_NOW = 1;
	public static final int MENU_ID_CONFIG = 2;
	public static final int MENU_ID_ABOUT = 3;
	public static final int MENU_ID_CHANGELOG = 4;

	//Menu ThemeList Layout
	public static final int MENU_THEME_LIST_ADD = 1;
	public static final int MENU_THEME_LIST_UPDATE_FEATURED = 2;
	public static final int MENU_THEME_DELETE_ALL = 3;
	public static final int MENU_THEME_DELETE_ALL_FEATURED = 4;
	public static final int MENU_THEME_DISABLE_ALL = 5;
	public static final int MENU_THEME_DISABLE_ALL_FEATURED = 6;
	public static final int MENU_THEME_ENABLE_ALL = 7;
	public static final int MENU_THEME_ENABLE_ALL_FEATURED = 8;

	//Menu ThemeListContextMenu
	public static final int MENU_THEME_LIST_CONTEXT_EDIT = 10;
	public static final int MENU_THEME_LIST_CONTEXT_DELETE = 11;
	public static final int MENU_THEME_LIST_CONTEXT_ENABLE = 12;
	public static final int MENU_THEME_LIST_CONTEXT_DISABLE = 13;

	//Notifications
	public static final int NOTIFICATION_DOWNLOAD_STATUS = 100;
	public static final int NOTIFICATION_DOWNLOAD_FINISHED = 200;

	//Update Check Frequencies
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;

	//ChangelogHandler
	public static final String VERSION_TAG = "Version";
	public static final String VERSION_NAME_TAG = "name";

	//Featured Themes Handler
	public static final String FEATURED_THEMES_TAG = "Theme";
	public static final String FEATURES_THEMES_TAG_NAME = "name";
	public static final String FEATURES_THEMES_TAG_URI = "url";

	//ThemeListNew
	public static final String THEME_LIST_NEW_NAME = "name";
	public static final String THEME_LIST_NEW_URI = "uri";
	public static final String THEME_LIST_NEW_ENABLED = "enabled";
	public static final String THEME_LIST_NEW_PRIMARYKEY = "pk";
	public static final String THEME_LIST_NEW_UPDATE = "update";
	public static final String THEME_LIST_NEW_FEATURED = "featured";

	//ThemeListItem
	public static final int THEME_LIST_ITEM_DISABLED_ALPHA = 70;

	//Screenshots
	public static final String SCREENSHOTS_UPDATE = "Screenshots";
	public static final int SCREENSHOTS_FALLBACK_IMAGE = android.R.drawable.ic_delete;
    public static final int SCREENSHOTS_LOADING_IMAGE = com.cyanogenmod.updater.ui.R.drawable.cmu_loading;
	public static final String SCREENSHOTS_POSITION = "ScreenshotPosition";
}
