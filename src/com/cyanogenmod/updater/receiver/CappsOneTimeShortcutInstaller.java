package com.cyanogenmod.updater.receiver;

import com.cyanogenmod.updater.UpdatesSettings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.cyanogenmod.updater.R;

public class CappsOneTimeShortcutInstaller extends BroadcastReceiver {

    private static final String TAG = "CappsShortcut";

    // from p/a/Trebuchet/AndroidManifest.xml
    private static final String ACTION_INSTALL_SHORTCUT
            = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String GMS_CORE_PKG = "com.google.android.gms";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isPackageInstalled(context, GMS_CORE_PKG)) {
            // add c-apps shortcut
            Intent shortcutIntent = new Intent(UpdatesSettings.ACTION_GET_CAPPS);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent addIntent = new Intent(ACTION_INSTALL_SHORTCUT);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    context.getString(R.string.capps_get_shortcut_name));
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));

            context.sendBroadcast(addIntent);

            // disable ourselves
            ComponentName me = new ComponentName(context, CappsOneTimeShortcutInstaller.class);

            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(me, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            Log.d(TAG, "added C-Apps shortcut!");
        }
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        if (pkg == null) {
            return false;
        }
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
            if (!pi.applicationInfo.enabled) {
                return false;
            } else {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
