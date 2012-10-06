/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.utils;

import java.util.ArrayList;
import java.util.Arrays;

public class StringUtils {
    private static final String TAG = "StringUtils";

    /**
     * Converts a String array to a String, joined by the Seperator
     * @param items     The String Array to Join
     * @param seperator The Seperator used to join the String
     * @return The Joined String
     */
    public static String arrayToString(String[] items, String seperator) {
        if ((items == null) || (items.length == 0)) {
            return "";
        } else {
            StringBuffer buffer = new StringBuffer(items[0]);
            for (int i = 1; i < items.length; i++) {
                buffer.append(seperator);
                buffer.append(items[i]);
            }
            return buffer.toString();
        }
    }

    /**
     * Compare two versions
     * @param newVersion new version to be compared
     * @param oldVersion old version to be compared
     * @return true if newVersion is greater then oldVersion,
     * false on exceptions or newVersion=oldVersion and newVersion is lower then oldVersion
     */
    public static boolean compareVersions(String newVersion, String oldVersion, Integer newDate, Integer oldDate) {
        //Replace all - by . So a CyanogenMod-4.5.4-r2 will be a CyanogenMod.4.5.4.r2 
        newVersion = newVersion.replaceAll("-", "\\.");
        oldVersion = oldVersion.replaceAll("-", "\\.");

        String[] sNewVersion = newVersion.split("\\.");
        String[] sOldVersion = oldVersion.split("\\.");

        ArrayList<String> newVersionArray = new ArrayList<String>();
        ArrayList<String> oldVersionArray = new ArrayList<String>();

        newVersionArray.addAll(Arrays.asList(sNewVersion));
        oldVersionArray.addAll(Arrays.asList(sOldVersion));

        //Make the 2 Arrays the same size filling it with 0. So Version 2 compared to 2.1 will be 2.0 to 2.1
        if (newVersionArray.size() > oldVersionArray.size()) {
            int difference = newVersionArray.size() - oldVersionArray.size();
            for (int i = 0; i < difference; i++) {
                oldVersionArray.add("0");
            }
        } else {
            int difference = oldVersionArray.size() - newVersionArray.size();
            for (int i = 0; i < difference; i++) {
                newVersionArray.add("0");
            }
        }

        // Collapse them again
        newVersion = "";
        for (String s : newVersionArray) {
            newVersion += s;
        }

        oldVersion = "";
        for (String s : oldVersionArray) {
            oldVersion += s;
        }

        Integer iNewVersion = new Integer(newVersion);
        Integer iOldVersion = new Integer(oldVersion);

        if (iNewVersion < iOldVersion) {
            return false;
        } else if (iNewVersion > iOldVersion) {
            return true;
        } else {
            // The jenkins timestamp is for build completion, not the actual build.date prop
            return newDate > oldDate + 3600;
        }
    }
}
