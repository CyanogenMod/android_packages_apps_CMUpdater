/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.service.UpdateCheckService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class Utils {
    private Utils() {
        // this class is not supposed to be instantiated
    }

    public static File makeUpdateFolder() {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                Constants.UPDATES_FOLDER);
    }

    public static void cancelNotification(Context context) {
        final NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.not_new_updates_found_title);
        nm.cancel(R.string.not_download_success);
    }

    public static String getDeviceType() {
        return SystemProperties.get("ro.cm.device");
    }

    public static String getInstalledVersion() {
        return SystemProperties.get("ro.cm.version");
    }

    public static int getInstalledApiLevel() {
        return SystemProperties.getInt("ro.build.version.sdk", 0);
    }

    public static long getInstalledBuildDate() {
        return SystemProperties.getLong("ro.build.date.utc", 0);
    }

    public static String getIncremental() {
        return SystemProperties.get("ro.build.version.incremental");
    }

    public static String getUserAgentString(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.packageName + "/" + pi.versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            return null;
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static void scheduleUpdateService(Context context, int updateFrequency) {
        // Load the required settings from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastCheck = prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0);

        // Get the intent ready
        Intent i = new Intent(context, UpdateCheckService.class);
        i.setAction(UpdateCheckService.ACTION_CHECK);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Clear any old alarms and schedule the new alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        if (updateFrequency != Constants.UPDATE_FREQ_NONE) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck + updateFrequency, updateFrequency, pi);
        }
    }

    public static void triggerUpdate(Context context, String updateFileName) throws IOException {
        /*
         * Should perform the following steps.
         * 1.- mkdir -p /cache/recovery
         * 2.- echo 'boot-recovery' > /cache/recovery/command
         * 3.- if(mBackup) echo '--nandroid'  >> /cache/recovery/command
         * 4.- echo '--update_package=SDCARD:update.zip' >> /cache/recovery/command
         * 5.- reboot recovery
         */

        // Set the 'boot recovery' command
        Process p = Runtime.getRuntime().exec("sh");
        OutputStream os = p.getOutputStream();
        os.write("mkdir -p /cache/recovery/\n".getBytes());
        os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());

        // See if backups are enabled and add the nandroid flag
        /* TODO: add this back once we have a way of doing backups that is not recovery specific
           if (mPrefs.getBoolean(Constants.BACKUP_PREF, true)) {
           os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
           }
           */

        // Add the update folder/file name
        String primaryStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        // If data media rewrite the path to bypass the sd card fuse layer and trigger uncrypt
        String directPath = Environment.maybeTranslateEmulatedPathToInternal(
                new File(primaryStoragePath)).getAbsolutePath();
        String updatePath = Environment.isExternalStorageEmulated() ? directPath :
                primaryStoragePath;
        String cmd = "echo '--update_package=" + updatePath + "/" + Constants.UPDATES_FOLDER + "/"
                + updateFileName + "' >> /cache/recovery/command\n";
        os.write(cmd.getBytes());
        os.flush();

        // Trigger the reboot
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        powerManager.reboot("recovery");
    }

    public static int getUpdateType() {
        int updateType = Constants.UPDATE_TYPE_NIGHTLY;
        try {
            String cmReleaseType = SystemProperties.get(
                    Constants.PROPERTY_CM_RELEASETYPE);

            // Treat anything that is not SNAPSHOT as NIGHTLY
            if (!cmReleaseType.isEmpty()) {
                if (TextUtils.equals(cmReleaseType,
                        Constants.CM_RELEASETYPE_SNAPSHOT)) {
                    updateType = Constants.UPDATE_TYPE_SNAPSHOT;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return updateType;
    }

    public static boolean hasLeanback(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
