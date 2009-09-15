package cmupdaterapp.misc;

public class Constants
{
	//UpdateInfo
	public static final String UPDATE_INFO_TYPE_ROM = "rom";
	public static final String UPDATE_INFO_TYPE_THEME = "theme";
	public static final String UPDATE_INFO_BRANCH_STABLE = "s";
	public static final String UPDATE_INFO_BRANCH_EXPERIMENTAL = "x";
	public static final String UPDATE_INFO_WILDCARD = "*";
	
	//JSON Objects
	public static final String JSON_MIRROR_LIST = "MirrorList"; 
	public static final String JSON_UPDATE_LIST = "UpdateList";
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
	public static final String KEY_MIRROR_NAME = "cmupdaterapp.mirrorName";
	
	//Flipper
	public static final int FLIPPER_AVAILABLE_UPDATES = 0;
	public static final int FLIPPER_EXISTING_UPDATES = 1;
	public static final int FLIPPER_AVAILABLE_THEMES = 2;
	
	//Changelog
	public static final int CHANGELOGTYPE_ROM = 1;
	public static final int CHANGELOGTYPE_APP = 2;
	public static final int CHANGELOGTYPE_THEME = 3;
	
	//Startup Requests
	public static final int REQUEST_NEW_UPDATE_LIST = 1;
	public static final int REQUEST_UPDATE_CHECK_ERROR = 2;
	public static final int REQUEST_DOWNLOAD_FAILED = 3;
	public static final int REQUEST_MD5CHECKER_CANCEL = 4;
	
	//Request Updatecheck
	public static final int REQUEST_CHECK_FOR_UPDATES = 1;
	
	//Request Downloadservice
	public static final int REQUEST_DOWNLOAD_UPDATE = 1;
	
	//Menu main Layout
	public static final int MENU_ID_UPDATE_NOW = 1;
	public static final int MENU_ID_SCAN_QR = 2;
	public static final int MENU_ID_CONFIG = 3;
	public static final int MENU_ID_ABOUT = 4;
	public static final int MENU_ID_CHANGELOG = 5;
	
	//Menu ThemeList Layout
	public static final int MENU_THEME_LIST_ADD = 1;
	
	//Menu ThemeListContextMenu
	public static final int MENU_THEME_LIST_CONTEXT_EDIT = 10;
	public static final int MENU_THEME_LIST_CONTEXT_DELETE = 11;
	
	//Filename for Instance save
	public static final String STORED_STATE_FILENAME = "CMUpdater.ser";
	
	//Notifications
	public static final int NOTIFICATION_DOWNLOAD_STATUS = 100;
	public static final int NOTIFICATION_DOWNLOAD_FINISHED = 200;
	
	//Android Board type
	public static final String SYS_PROP_DEVICE = "ro.product.board";
	
	//Update Check Frequencies
	public static final int UPDATE_FREQ_AT_BOOT = -1;
	public static final int UPDATE_FREQ_NONE = -2;
	
	//Name of the Current Rom and the Skip Chars to get the Version
	public static final String SYS_PROP_MOD_VERSION = "ro.modversion";
	public static final int PROP_MOD_VERSION_SKIP_CHARS = 12;
	
	//ChangelogHandler
	public static final String VERSION_TAG = "Version";
	public static final String VERSION_NAME_TAG = "name";
	
	//ThemeListNew
	public static final String THEME_LIST_NEW_NAME = "name";
	public static final String THEME_LIST_NEW_URI = "uri";
	public static final String THEME_LIST_NEW_ENABLED = "enabled";
	public static final String THEME_LIST_NEW_PRIMARYKEY = "pk";
	public static final String THEME_LIST_NEW_UPDATE = "update";
	
	//IntentExtra for DownloadActivity
	public static final String UPDATE_INFO = "UpdateInfo";
}