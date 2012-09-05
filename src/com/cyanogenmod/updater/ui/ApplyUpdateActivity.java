package com.cyanogenmod.updater.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.utils.Preferences;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

public class ApplyUpdateActivity extends Activity {
    private static final String TAG = "ApplyUpdateActivity";

    private Boolean showDebugOutput = false;

    private UpdateInfo mUpdateInfo;
    private String mUpdateFolder;

    private Preferences pref;


    @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.applyupdate);

            pref = new Preferences(this);
            showDebugOutput = pref.displayDebugOutput();

            ((Button) findViewById(R.id.apply_now_button)).setOnClickListener(ButtonOnClickListener);
            ((Button) findViewById(R.id.apply_later_button)).setOnClickListener(ButtonOnClickListener);
        }

    @Override
        protected void onStart() {
            super.onStart();
            Resources res = getResources();
            mUpdateInfo = (UpdateInfo) getIntent().getExtras().getSerializable(Constants.KEY_UPDATE_INFO);
            String template = res.getString(R.string.apply_title_textview_text);
            ((TextView) findViewById(R.id.apply_title_textview)).setText(MessageFormat.format(template, mUpdateInfo.getName()));
            mUpdateFolder = pref.getUpdateFolder();
            if (showDebugOutput) Log.d(TAG, "Filename selected to flash: " + mUpdateInfo.getFileName());
        }

    private boolean isStorageRemovable() {
        StorageManager sm = (StorageManager) getBaseContext().getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = sm.getVolumeList();
        String primaryStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        if (volumes.length <= 1) { return false; } // single storage, assume /sdcard
        for (int i = 0; i < volumes.length; i++) {
            StorageVolume v = volumes[i];

            if (v.getPath().equals(primaryStoragePath)) {
                if (v.isRemovable()) {
                    return true;
                }
                return false;
            };
        }
        // Not found, assume removable
        return true;
    }

    private final View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.apply_now_button:
                    String dialogBody = MessageFormat.format(
                            getResources().getString(R.string.apply_update_dialog_text),
                            mUpdateInfo.getName());

                    AlertDialog dialog = new AlertDialog.Builder(ApplyUpdateActivity.this)
                        .setTitle(R.string.apply_update_dialog_title)
                        .setMessage(dialogBody)
                        .setNeutralButton(R.string.apply_update_dialog_update_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                /*
                                 * Should perform the following steps.
                                 * 0.- Ask the user for a confirmation (already done when we reach here)
                                 * 1.- su
                                 * 2.- mkdir -p /cache/recovery
                                 * 3.- echo 'boot-recovery' > /cache/recovery/command
                                 * 4.- if(mBackup) echo '--nandroid'  >> /cache/recovery/command
                                 * 5.- echo '--update_package=SDCARD:update.zip' >> /cache/recovery/command
                                 * 6.- reboot recovery 
                                 */
                                try {
                                PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                Boolean mBackup = pref.doNandroidBackup(); 
                                Process p = Runtime.getRuntime().exec("sh");
                                OutputStream os = p.getOutputStream();
                                os.write("mkdir -p /cache/recovery/\n".getBytes());
                                os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());
                                if (mBackup)
                                os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
                                String cmd = "echo '--update_package="+(isStorageRemovable() ? "/external_sd" : "/sdcard") +"/" + mUpdateFolder + "/" + mUpdateInfo.getFileName() + "' >> /cache/recovery/command\n";
                                os.write(cmd.getBytes());
                                os.flush();
                                Toast.makeText(ApplyUpdateActivity.this, R.string.apply_trying_to_get_root_access, Toast.LENGTH_LONG).show();
                                mPowerManager.reboot("recovery");
                                }
                                catch (IOException e) {
                                    Log.e(TAG, "Unable to reboot into recovery mode:", e);
                                    Toast.makeText(ApplyUpdateActivity.this, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_LONG).show();
                                }
                                }
                        })
                    .setNegativeButton(R.string.apply_update_dialog_cancel_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            }
                            }).create();

                    dialog.show();
                    break;
                case R.id.apply_later_button:
                    Intent i = new Intent(ApplyUpdateActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                    break;
            }
        }
    };
}
