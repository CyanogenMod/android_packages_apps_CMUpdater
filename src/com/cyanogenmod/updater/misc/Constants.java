/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.updater.misc;

import com.cyanogenmod.updater.customization.Customization;

public class Constants {

    // System Info
    public static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"; 

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

    // Service start parameter
    public static final String CHECK_FOR_UPDATE = "check_for_update";

    // Notifications
    public static final int NOTIFICATION_DOWNLOAD_STATUS = 100;
    public static final int NOTIFICATION_DOWNLOAD_FINISHED = 200;

    // Update Check Frequencies
    public static final int UPDATE_FREQ_AT_BOOT = -1;
    public static final int UPDATE_FREQ_NONE = -2;

    // ChangelogHandler
    public static final String VERSION_TAG = "Version";
    public static final String VERSION_NAME_TAG = "name";

}
