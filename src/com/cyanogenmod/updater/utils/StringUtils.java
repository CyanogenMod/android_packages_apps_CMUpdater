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

package com.cyanogenmod.updater.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class StringUtils {
    private static final String TAG = "StringUtils";

    /**
     * Converts a String array to an String, joined by the Seperator
     * 
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
     * Compare two versions.
     * 
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

        //Make the 2 Arrays the Same size filling it with 0. So Version 2 compared to 2.1 will be 2.0 to 2.1
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
        //
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


        Log.i(TAG, "NewVersion: " + newVersion + ", oldVersion: " + oldVersion + " dates (old/new) " + oldDate + "/" + newDate);
        if (iNewVersion < iOldVersion) {
            return false;
        } else if (iNewVersion > iOldVersion) {
            return true;
        } else {
            return newDate > oldDate + 3600; /* The jenkins timestamp is for 
                                                build completion, not the 
                                                actual build.date prop */
        }
    }
}
