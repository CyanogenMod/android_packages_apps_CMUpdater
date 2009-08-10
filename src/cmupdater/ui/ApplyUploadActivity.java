/*
 * JF Updater: Auto-updater for modified Android OS
 *
 * Copyright (c) 2009 Sergi Vélez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package cmupdater.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cmupdater.service.UpdateInfo;
import cmupdater.utils.Preferences;

public class ApplyUploadActivity extends Activity {

	private static final String TAG = "ApplyUploadActivity";

	public static final String KEY_UPDATE_INFO = "cmupdater.updateInfo";


	private UpdateInfo mUpdateInfo;
	private String mUpdateFolder;
	
	private TextView mTitle;
	//private TextView mSubtitle;
	//private EditText mReleaseNotes;
	private Button mApplyButton;
	private Button mPostponeButton;

	private final View.OnClickListener mApplyButtonListener = new View.OnClickListener() {
		public void onClick(View v) {
			String dialogBody = MessageFormat.format(
					getResources().getString(R.string.apply_update_dialog_text),
					mUpdateInfo.name);

			AlertDialog dialog = new AlertDialog.Builder(ApplyUploadActivity.this)
			.setTitle(R.string.apply_update_dialog_title)
			.setMessage(dialogBody)
			//.setPositiveButton(R.string.apply_update_dialog_backup_and_update_button, mBackupAndApplyUpdateListener)
			.setNeutralButton(R.string.apply_update_dialog_update_button, mBackupAndApplyUpdateListener)
			.setNegativeButton(R.string.apply_update_dialog_cancel_button, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create();

			dialog.show();
		}
	};
	private final DialogInterface.OnClickListener mBackupAndApplyUpdateListener = new ApplyUpdateListener(this);
	private class ApplyUpdateListener implements DialogInterface.OnClickListener {

		private boolean mBackup;
		private Context mCtx;

		public ApplyUpdateListener(Context ctx) {
			mBackup = Preferences.getPreferences(ctx).doNandroidBackup();
			mCtx = ctx;
		}

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
				Process p = Runtime.getRuntime().exec("su");
				OutputStream os = p.getOutputStream();
				os.write("mkdir -p /cache/recovery/\n".getBytes());
				os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());
				if(mBackup) os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
				String cmd = "echo '--update_package=SDCARD:" + mUpdateFolder + "/" + mUpdateInfo.fileName + "' >> /cache/recovery/command\n";
				os.write(cmd.getBytes());
				os.write("reboot recovery\n".getBytes());

				os.flush();

				Toast.makeText(mCtx, R.string.apply_trying_to_get_root_access, Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Log.e(TAG, "Unable to reboot into recovery mode:", e);
				Toast.makeText(mCtx, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_LONG).show();
			}
		}
	};

	private final View.OnClickListener mPostponeButtonListener = new View.OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apply_upload);


		mTitle = (TextView) findViewById(R.id.apply_title_textview);

		//mSubtitle = (TextView) findViewById(R.id.apply_subtitle_textview);
		//mReleaseNotes = (EditText) findViewById(R.id.apply_release_notes);

		mApplyButton = (Button) findViewById(R.id.apply_now_button);
		mApplyButton.setOnClickListener(mApplyButtonListener);

		mPostponeButton = (Button) findViewById(R.id.apply_later_button);
		mPostponeButton.setOnClickListener(mPostponeButtonListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mUpdateInfo = (UpdateInfo) getIntent().getExtras().getSerializable(KEY_UPDATE_INFO);
		//Resources res = getResources();
		String template = getResources().getString(R.string.apply_title_textview_text);
		mTitle.setText(MessageFormat.format(template, mUpdateInfo.name));
		mUpdateFolder = Preferences.getPreferences(this).getUpdateFolder();
	}
}