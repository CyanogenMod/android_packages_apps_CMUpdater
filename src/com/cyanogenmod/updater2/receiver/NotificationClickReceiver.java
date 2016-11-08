package com.cyanogenmod.updater2.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cyanogenmod.updater2.ListActivity;

public class NotificationClickReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        // Bring the main app to the foreground
        Intent i = new Intent(context, ListActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }
}
