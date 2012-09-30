package com.cyanogenmod.updater.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cyanogenmod.updater.UpdatesSettings;

public class NotificationClickReceiver extends BroadcastReceiver{
    private static String TAG = "NotificationClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Bring the main app to the foreground
        /* TODO: Add this back once the handling of partial files are added to updatesSettings
        Intent i = new Intent(context, UpdatesSettings.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
        */
    }
}
