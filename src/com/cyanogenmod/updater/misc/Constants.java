/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import com.cyanogenmod.updater.customization.Customization;

public class Constants {

    // System Info
    public static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"; 

    // Download related
    public static final String UPDATES_FOLDER = "/cmupdater";
    public static final String DOWNLOAD_ID = "download_id";
    public static final String DOWNLOAD_FULLPATH = "download_fullpath";
    public static final String DOWNLOAD_MD5 = "download_md5";
    public static final String CHANGELOG_ID = "changelog_id";

    //UpdateInfo
    public static final String UPDATE_INFO_BRANCH_STABLE = "stable";
    public static final String UPDATE_INFO_BRANCH_NIGHTLY = "nightly";
    public static final String UPDATE_INFO_WILDCARD = "*";

    // JSON Objects
    public static final String JSON_UPDATE_LIST = "result";
    public static final String JSON_TIMESTAMP = "timestamp";
    public static final String JSON_URL = "url";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_BRANCH = "channel";
    public static final String JSON_FILENAME = "filename";
    public static final String JSON_MD5SUM = "md5sum";
    public static final String JSON_CHANGES = "changes";

    // Keys
    public static final String KEY_UPDATE_INFO = Customization.PACKAGE_FIRST_NAME + ".fullUpdateList";
    public static final String KEY_AVAILABLE_UPDATES = Customization.PACKAGE_FIRST_NAME + ".availableUpdates";
    public static final String KEY_UPDATE_PREFERENCE = "update_preference";

    // Preferences
    public static final String ENABLE_PREF = "pref_enable_updates";
    public static final String BACKUP_PREF = "pref_backup_rom";
    public static final String UPDATE_CHECK_PREF = "pref_update_check";
    public static final String UPDATE_TYPE_PREF = "pref_update_type";
    public static final String LAST_UPDATE_CHECK_PREF = "pref_last_update_check";

    // Activity start parameters
    public static final String CHECK_FOR_UPDATE = "check_for_update";
    public static final String START_UPDATE = "start_update";
    public static final String STOP_DOWNLOAD = "stop_download";
    public static final String DOWNLOAD_COMPLETED = "download_completed";

    // Update Check items
    public static final String BOOT_CHECK_COMPLETED = "boot_check_completed";
    public static final int UPDATE_FREQ_AT_BOOT = -1;
    public static final int UPDATE_FREQ_NONE = -2;
    public static final int UPDATE_FREQ_TWICE_DAILY = 43200;
    public static final int UPDATE_FREQ_DAILY = 86400;
    public static final int UPDATE_FREQ_WEEKLY = 604800;
    public static final int UPDATE_FREQ_BI_WEEKLY = 1209600;
    public static final int UPDATE_FREQ_MONTHLY = 2419200;

}
