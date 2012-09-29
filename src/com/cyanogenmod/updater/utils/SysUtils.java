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

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.cyanogenmod.updater.customization.Customization;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class SysUtils {
    private static final String TAG = "SysUtils";

    /**
     * Returns (if available) a human-readable string containing current mod version
     * 
     * @return a human-readable string containing current mod version
     */
    public static String getModVersion() {
        String modVer = getSystemProperty(Customization.SYS_PROP_MOD_VERSION);

        modVer = modVer.replaceAll("([0-9.]+?)-.+","$1");
        return (modVer == null || modVer.length() == 0 ? "Unknown" : modVer);
    }

    /**
     * Returns a SystemProperty
     * 
     * @param propName The Property to retrieve
     * @return The Property, or NULL if not found
     */
    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        }
        catch (IOException ex) {
            Log.e(TAG, "Unable to read sysprop " + propName, ex);
            return null;
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "Exception while closing InputStream", e);
                }
            }
        }
        return line;
    }

    /**
     * Checks if there is enough Space on SDCard
     * 
     * @param UpdateSize Size to Check
     * @return True if the Update will fit on SDCard, false if not enough space on SDCard
     * 		Will also return false, if the SDCard is not mounted as read/write
     */
    public static boolean EnoughSpaceOnSdCard(long UpdateSize) {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED))
            return false;
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return (UpdateSize < availableBlocks * blockSize);
    }
}
