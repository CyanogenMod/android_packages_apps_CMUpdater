package com.cyanogenmod.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

public class ApplyUpdate extends Activity {
    private static final String TAG = "ApplyUpdate";

    private Boolean DEBUG = false;

    private UpdateInfo mUpdateInfo;
    private PowerManager mPowerManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set things up
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (DEBUG)
            Log.d(TAG, "Starting the ApplyUpdate activity");

        mUpdateInfo = (UpdateInfo) getIntent().getExtras().getSerializable(Constants.KEY_UPDATE_INFO);
        startUpdate();
    }

    private void startUpdate() {
        if (DEBUG)
            Log.d(TAG, "Filename selected to flash: " + mUpdateInfo.getFileName());

        // Get the message body right
        String dialogBody = MessageFormat.format(
                getResources().getString(R.string.apply_update_dialog_text),
                mUpdateInfo.getFileName());

        // Display the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.apply_update_dialog_title);
            builder.setMessage(dialogBody);
            builder.setPositiveButton(R.string.dialog_update, new DialogInterface.OnClickListener() {
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
                        // Set the 'boot recovery' command
                        Process p = Runtime.getRuntime().exec("sh");
                        OutputStream os = p.getOutputStream();
                        os.write("mkdir -p /cache/recovery/\n".getBytes());
                        os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());

                        // See if backups are enabled and add the nandroid flag
                        SharedPreferences prefs = getSharedPreferences("CMUpdate", Context.MODE_MULTI_PROCESS);
                        if (prefs.getBoolean(Constants.BACKUP_PREF, true)) {
                            os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
                        }

                        // Add the update folder/file name
                        // TODO: this is where it handled the external storage command, now assume /sdcard/cmupdater
                        String cmd = "echo '--update_package=/sdcard/cmupdater/" + mUpdateInfo.getFileName()
                                + "' >> /cache/recovery/command\n";
                        os.write(cmd.getBytes());
                        os.flush();
                        Toast.makeText(ApplyUpdate.this, R.string.apply_trying_to_get_root_access, Toast.LENGTH_SHORT).show();
                        mPowerManager.reboot("recovery");

                    } catch (IOException e) {
                        Log.e(TAG, "Unable to reboot into recovery mode:", e);
                        Toast.makeText(ApplyUpdate.this, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
