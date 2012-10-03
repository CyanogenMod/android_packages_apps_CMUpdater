/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.customization;

public class Customization {
    //The String from the build.prop before the Version
    public static final String RO_MOD_START_STRING = "CyanogenMod";
    //Minimum Supported Version (So the User has to install google apps and so before)
    public static final String MIN_SUPPORTED_VERSION_STRING = RO_MOD_START_STRING + "";
    //MUST be the first package name.
    public static final String PACKAGE_FIRST_NAME = "com.cyanogenmod.updater";
    //Filename for Instance save
    public static final String STORED_STATE_FILENAME = "cmupdater.state";
    //Android Board type
    public static final String BOARD = "ro.cm.device";
    //current Rom build date
    public static final String BUILD_DATE = "ro.build.date.utc";
    //Name of the Current Rom
    public static final String SYS_PROP_MOD_VERSION = "ro.cm.version";
}
