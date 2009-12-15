package cmupdaterapp.misc;

public class Constants
{
	//System Infos
	//The String from the build.prop before the Version
	public static final String RO_MOD_START_STRING = "CyanogenMod-";
	public static final String MIN_SUPPORTED_VERSION_STRING = RO_MOD_START_STRING + "4.1.99";
	public static final String UPDATE_INSTRUCTIONS_URL = "http://www.simplehelp.net/2009/10/04/how-to-install-cyanogenmod-4-1-99-on-your-g1-android-phone/";

	//UpdateInfo
	public static final String UPDATE_INFO_TYPE_ROM = "rom";
	public static final String UPDATE_INFO_TYPE_THEME = "theme";
	public static final String UPDATE_INFO_BRANCH_STABLE = "s";
	public static final String UPDATE_INFO_BRANCH_EXPERIMENTAL = "x";
	public static final String UPDATE_INFO_WILDCARD = "*";
	
	//JSON Objects
	public static final String JSON_MIRROR_LIST = "MirrorList"; 
	public static final String JSON_UPDATE_LIST = "UpdateList";
	public static final String JSON_SCREENSHOTS = "Screenshots";
	public static final String JSON_BOARD = "board";
	public static final String JSON_TYPE = "type";
	public static final String JSON_MOD = "mod";
	public static final String JSON_NAME = "name";
	public static final String JSON_VERSION = "version";
	public static final String JSON_DESCRIPTION = "description";
	public static final String JSON_BRANCH = "branch";
	public static final String JSON_FILENAME = "filename";
	
	//Keys
	public static final String KEY_REQUEST = "cmupdaterapp.keyRequest";
	public static final String KEY_UPDATE_INFO = "cmupdaterapp.fullUpdateList";
	public static final String KEY_AVAILABLE_UPDATES = "cmupdaterapp.availableUpdates";
	
	//Flipper
	public static final int FLIPPER_AVAILABLE_UPDATES = 0;
	public static final int FLIPPER_EXISTING_UPDATES = 1;
	public static final int FLIPPER_AVAILABLE_THEMES = 2;
	
	//Changelog
	public static final int CHANGELOGTYPE_ROM = 1;
	public static final int CHANGELOGTYPE_APP = 2;
	public static final int CHANGELOGTYPE_THEME = 3;
	
	//Startup Requests
	public static final int REQUEST_UPDATE_CHECK_ERROR = 1;
	public static final int REQUEST_DOWNLOAD_FAILED = 2;
	public static final int REQUEST_MD5CHECKER_CANCEL = 3;
	
	//Request Updatecheck
	public static final int REQUEST_CHECK_FOR_UPDATES = 1;
	
	//Request Downloadservice
	public static final int REQUEST_DOWNLOAD_UPDATE = 1;
	
	//Menu main Layout
	public static final int MENU_ID_UPDATE_NOW = 1;
	public static final int MENU_ID_CONFIG = 2;
	public static final int MENU_ID_ABOUT = 3;
	public static final int MENU_ID_CHANGELOG = 4;
	
	//Menu ThemeList Layout
	public static final int MENU_THEME_LIST_ADD = 1;
	public static final int MENU_THEME_LIST_UPDATE_FEATURED = 2;
	
	//Menu ThemeListContextMenu
	public static final int MENU_THEME_LIST_CONTEXT_EDIT = 10;
	public static final int MENU_THEME_LIST_CONTEXT_DELETE = 11;
	public static final int MENU_THEME_LIST_CONTEXT_ENABLE = 12;
	public static final int MENU_THEME_LIST_CONTEXT_DISABLE = 13;
	
	//Filename for Instance save
	public static final String STORED_STATE_FILENAME = "cmupdater.sst";
	
	//Notifications
	public static final int NOTIFICATION_DOWNLOAD_STATUS = 100;
	public static final int NOTIFICATION_DOWNLOAD_FINISHED = 200;
	
	//Android Board type
	public static final String BOARD = "ro.product.board";
	
	//Update Check Frequencies
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;
	
	//Name of the Current Rom
	public static final String SYS_PROP_MOD_VERSION = "ro.modversion";
	
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
	
	//IntentExtra for DownloadActivity
	public static final String UPDATE_INFO = "UpdateInfo";
	
	//Screenshots
	public static final String SCREENSHOTS_UPDATE = "Screenshots";
}