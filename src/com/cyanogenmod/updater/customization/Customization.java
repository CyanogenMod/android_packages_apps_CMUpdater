/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.customization;

public class Customization {
    // The string from build.prop before the version
    public static final String RO_MOD_START_STRING = "CyanogenMod";
    // Minimum supported version (so the user has to install Google Apps and so before)
    public static final String MIN_SUPPORTED_VERSION_STRING = RO_MOD_START_STRING + "";
    // MUST be the first package name
    public static final String PACKAGE_FIRST_NAME = "com.cyanogenmod.updater";
    // File name for instance save
    public static final String STORED_STATE_FILENAME = "cmupdater.state";
    // Android Board type
    public static final String BOARD = "ro.cm.device";
    // Current ROM build date
    public static final String BUILD_DATE = "ro.build.date.utc";
    // Name of the current ROM
    public static final String SYS_PROP_MOD_VERSION = "ro.cm.version";
}
