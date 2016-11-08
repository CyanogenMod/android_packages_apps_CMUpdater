package com.cyanogenmod.updater2.utils;

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
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.cyanogenmod.updater2.R;
import com.cyanogenmod.updater2.misc.Constants;
import com.cyanogenmod.updater2.service.UpdateCheckService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Utils {
    private Utils() {
        // this class is not supposed to be instantiated
    }

    public static File makeUpdateFolder(Context context) {
        return context.getDir(Constants.UPDATES_FOLDER, Context.MODE_PRIVATE);
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
        return netInfo != null && netInfo.isConnected();
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
        // Create the path for the update package
        String updatePackagePath = makeUpdateFolder(context).getPath() + "/" + updateFileName;

        // Reboot into recovery and trigger the update
        android.os.RecoverySystem.installPackage(context, new File(updatePackagePath));
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

    /**
     * Extract date from build file name
     *
     * @param mContext for getting localized string
     * @param mBuild YYYYMMDD date extracted from file name
     *
     * @return MMMM dd, yyyy formatted date
     */
    public static String getBuildDate(Context mContext, String mBuild) {
        Calendar mCal = Calendar.getInstance();
        mCal.set(Calendar.YEAR, Integer.parseInt(mBuild.substring(0, 4)));
        mCal.set(Calendar.MONTH, Integer.parseInt(mBuild.substring(4, 6)) - 1);
        mCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mBuild.substring(6, 8)));

        String mDate = new SimpleDateFormat(mContext.getString(R.string.date_formatting),
                mContext.getResources().getConfiguration().locale)
                .format(mCal.getTime());

        int mPosition = 0;
        boolean mWorking = true;
        while (mWorking) {
            if (!Character.isDigit(mDate.charAt(mPosition))) {
                mWorking = false;
            } else {
                mPosition++;
            }
        }

        return mDate.substring(0, mPosition) +
                String.valueOf(mDate.charAt(mPosition)).toUpperCase() +
                mDate.substring(mPosition + 1, mDate.length());
    }
}
