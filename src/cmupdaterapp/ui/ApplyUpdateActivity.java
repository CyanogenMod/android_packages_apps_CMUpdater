package cmupdaterapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.utils.Preferences;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

public class ApplyUpdateActivity extends Activity
{	
	private static final String TAG = "ApplyUpdateActivity";

	private Boolean showDebugOutput = false;
	
	private UpdateInfo mUpdateInfo;
	private String mUpdateFolder;

	private TextView mTitle;

    private Preferences pref;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.applyupdate);
		
		pref = new Preferences(this);
		showDebugOutput = pref.displayDebugOutput();

		mTitle = (TextView) findViewById(R.id.apply_title_textview);

        Button mApplyButton = (Button) findViewById(R.id.apply_now_button);
		mApplyButton.setOnClickListener(mApplyButtonListener);

        Button mPostponeButton = (Button) findViewById(R.id.apply_later_button);
		mPostponeButton.setOnClickListener(mPostponeButtonListener);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Resources res = getResources();
		mUpdateInfo = (UpdateInfo) getIntent().getExtras().getSerializable(Constants.KEY_UPDATE_INFO);
		String template = res.getString(R.string.apply_title_textview_text);
		mTitle.setText(MessageFormat.format(template, mUpdateInfo.getName()));
		mUpdateFolder = pref.getUpdateFolder();
		if (showDebugOutput) Log.d(TAG, "Filename selected to flash: " + mUpdateInfo.getFileName());
	}
	
	private final View.OnClickListener mApplyButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			String dialogBody = MessageFormat.format(
					getResources().getString(R.string.apply_update_dialog_text),
					mUpdateInfo.getName());

			AlertDialog dialog = new AlertDialog.Builder(ApplyUpdateActivity.this)
			.setTitle(R.string.apply_update_dialog_title)
			.setMessage(dialogBody)
			.setNeutralButton(R.string.apply_update_dialog_update_button, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
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
					try
					{
						Boolean mBackup = pref.doNandroidBackup(); 
						Process p = Runtime.getRuntime().exec("su");
						OutputStream os = p.getOutputStream();
						os.write("mkdir -p /cache/recovery/\n".getBytes());
						os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());
						if(mBackup) os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
						String cmd = "echo '--update_package=SDCARD:" + mUpdateFolder + "/" + mUpdateInfo.getFileName() + "' >> /cache/recovery/command\n";
						os.write(cmd.getBytes());
						os.write("reboot recovery\n".getBytes());

						os.flush();

						Toast.makeText(ApplyUpdateActivity.this, R.string.apply_trying_to_get_root_access, Toast.LENGTH_LONG).show();
					}
					catch (IOException e)
					{
						Log.e(TAG, "Unable to reboot into recovery mode:", e);
						Toast.makeText(ApplyUpdateActivity.this, R.string.apply_unable_to_reboot_toast, Toast.LENGTH_LONG).show();
					}
				}
			})
			.setNegativeButton(R.string.apply_update_dialog_cancel_button, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			}).create();

			dialog.show();
		}
	};
	
	

	private final View.OnClickListener mPostponeButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Intent i = new Intent(ApplyUpdateActivity.this, MainActivity.class);
			startActivity(i);
			finish();
		}
	};
}