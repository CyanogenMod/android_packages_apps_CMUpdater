package cmupdaterapp.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IMainActivity;
import cmupdaterapp.listadapters.UpdateListAdapter;
import cmupdaterapp.service.PlainTextUpdateServer;
import cmupdaterapp.tasks.UpdateCheck;
import cmupdaterapp.tasks.UserTask;
import cmupdaterapp.utils.MD5;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.SysUtils;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.misc.State;
import cmupdaterapp.changelog.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends IMainActivity
{
	private static final String TAG = "MainActivity";

	private Spinner mUpdatesSpinner;
	private Spinner mThemesSpinner;
	private PlainTextUpdateServer mUpdateServer;
	private FullUpdateInfo mAvailableUpdates;

	private File mUpdateFolder;
	private Spinner mExistingUpdatesSpinner;

	private ProgressDialog ChangelogProgressDialog;
	public static Handler ChangelogProgressHandler;
	private Thread ChangelogThread;
	private List<Version> ChangelogList = null;

	private List<String> mfilenames;

	private TextView mdownloadedUpdateText;
	private Spinner mspFoundUpdates;
	private Button mdeleteOldUpdatesButton;
	private Button mapplyUpdateButton;
	private TextView mNoExistingUpdatesFound;

	private ViewFlipper flipper;

	private Preferences prefs;
	private Resources res;

	private final View.OnClickListener mDownloadUpdateButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return;
			}

			Log.d(TAG, "Download Rom Button clicked");
			final UpdateInfo ui = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
			//Check if the File is present, so prompt the User to overwrite it
			File foo = new File(mUpdateFolder + "/" + ui.fileName);
			if (foo.isFile() && foo.exists())
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.overwrite_update_title)
				.setMessage(R.string.overwrite_update_summary)
				.setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						Log.d(TAG, "Start downlading Rom update: " + ui.fileName);
						downloadRequestedUpdate(ui);
					}
				})
				.show();
				return;
			}
			//Otherwise download it
			else
			{
				downloadRequestedUpdate((UpdateInfo) mUpdatesSpinner.getSelectedItem());
			}
		}
	};
	
	private final View.OnClickListener mDownloadThemeButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return;
			}
			
			Log.d(TAG, "Download Theme Button clicked");
			final UpdateInfo ui = (UpdateInfo) mThemesSpinner.getSelectedItem();
			//Check if the File is present, so prompt the User to overwrite it
			File foo = new File(mUpdateFolder + "/" + ui.fileName);
			if (foo.isFile() && foo.exists())
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.overwrite_update_title)
				.setMessage(R.string.overwrite_update_summary)
				.setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						Log.d(TAG, "Start downlading Theme update: " + ui.fileName);
						downloadRequestedUpdate((UpdateInfo) mThemesSpinner.getSelectedItem());
					}
				})
				.show();
				return;
			}
			//Otherwise download it
			else
			{
				downloadRequestedUpdate((UpdateInfo) mThemesSpinner.getSelectedItem());
			}
		}
	};
	
	private final Spinner.OnItemSelectedListener mUpdateSpinnerChanged = new Spinner.OnItemSelectedListener()
	{
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			Button updateChangelogButton = (Button) findViewById(R.id.show_changelog_button);
			String changelog = ((UpdateInfo) mUpdatesSpinner.getSelectedItem()).description;
			if (changelog == null || changelog == "")
			{
				updateChangelogButton.setVisibility(View.GONE);
			}
			else
			{
				updateChangelogButton.setVisibility(View.VISIBLE);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{

		}
	};
	
	private final Spinner.OnItemSelectedListener mThemeSpinnerChanged = new Spinner.OnItemSelectedListener()
	{
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			Button themeChangelogButton = (Button) findViewById(R.id.show_theme_changelog_button);
			String changelog = ((UpdateInfo) mThemesSpinner.getSelectedItem()).description;
			if (changelog == null || changelog == "")
			{
				themeChangelogButton.setVisibility(View.GONE);
			}
			else
			{
				themeChangelogButton.setVisibility(View.VISIBLE);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{

		}
	};
	
	private final View.OnClickListener mUpdateChangelogButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Log.d(TAG, "Rom Changelog Button clicked");
			getChangelog(Constants.CHANGELOGTYPE_ROM);
		}
	};
	
	private final View.OnClickListener mThemeChangelogButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Log.d(TAG, "Theme Changelog Button clicked");
			getChangelog(Constants.CHANGELOGTYPE_THEME);
		}
	};

	private final View.OnClickListener mDeleteUpdatesButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return;
			}
			else
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.delete_updates_text)
				.setMessage(R.string.confirm_delete_update_folder_dialog_message)
				//Delete Only Selected Update
				.setNeutralButton(R.string.confirm_delete_update_folder_dialog_neutral, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						//Delete Updates here
						String f = (String) mExistingUpdatesSpinner.getSelectedItem();
						Log.d(TAG, "Delete single Update selected: " + f);
						if(deleteUpdate(f))
						{
							mfilenames.remove(f);
							//mfilenames.trimToSize();
						}
						//If Updates are cached or Present, reload the View
						if(mAvailableUpdates != null)
						{
							switchToUpdateChooserLayout(mAvailableUpdates);
						}
						//Otherwise switch to Updatechooserlayout. If no Updates are found and no files in Updatefolder, the Functions redirects you to NO ROMS FOUND
						else
						{
							switchToUpdateChooserLayout(null);
						}
					}
				})
				//Delete All Updates
				.setPositiveButton(R.string.confirm_delete_update_folder_dialog_yes, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						Log.d(TAG, "Delete all Updates selected");
						//Delete Updates here
						//Set the Filenames to null, so the Spinner will be empty
						if(deleteOldUpdates())
							mfilenames = null;
						//If Updates are cached or Present, reload the View
						if(mAvailableUpdates != null)
						{
							switchToUpdateChooserLayout(mAvailableUpdates);
						}
						//Otherwise switch to Updatechooserlayout. If no Updates are found and no files in Updatefolder, the Functions redirects you to NO ROMS FOUND
						else
						{
							switchToUpdateChooserLayout(null);
						}
					}
				})
				//Delete no Update
				.setNegativeButton(R.string.confirm_delete_update_folder_dialog_no, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						Log.d(TAG, "Delete no updates selected");
						dialog.dismiss();
					}
				})
				.show();
			}
		}
	};

	//To Apply Existing Update
	private final class mApplyExistingButtonListener implements View.OnClickListener
	{
		private ProgressDialog mDialog;
		private UserTask<File, Void, Boolean> mBgTask;
		private String filename;
		private File Update;
		
		public void onClick(View v)
		{	
			if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return;
			}

			filename = (String) mExistingUpdatesSpinner.getSelectedItem();
			Log.d(TAG, "Selected to Apply Existing update: " + filename);
			Update = new File(mUpdateFolder + "/" +filename);
			File MD5 = new File(mUpdateFolder + "/" +filename + ".md5sum");
			//IF no MD5 exists, ask the User what to do
			if(!MD5.exists() || !MD5.canRead())
			{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.no_md5_found_title)
				.setMessage(R.string.no_md5_found_summary)
				.setPositiveButton(R.string.no_md5_found_positive, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						//Directly call on Postexecute, cause we need no md5check
						new MD5CheckerTask(null, filename).onPostExecute(true);
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.no_md5_found_negative, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
			}
			//If MD5 exists, apply the update normally
			else
			{
				mDialog = ProgressDialog.show(
						MainActivity.this,
						res.getString(R.string.verify_and_apply_dialog_title),
						res.getString(R.string.verify_and_apply_dialog_message),
						true,
						true,
						new DialogInterface.OnCancelListener()
						{
							public void onCancel(DialogInterface arg0)
							{
								mBgTask.cancel(true);
							}
						}
				);
	
				mBgTask = new MD5CheckerTask(mDialog, filename).execute(Update);
			}
		}
	}

	private final class MD5CheckerTask extends UserTask<File, Void, Boolean>
	{	
		private ProgressDialog mDialog;
		private String mFilename;
		private boolean mreturnvalue;

		public MD5CheckerTask(ProgressDialog dialog, String filename)
		{
			mDialog = dialog;
			mFilename = filename;
		}

		@Override
		public Boolean doInBackground(File... params)
		{
			
			boolean MD5exists = false;
			try
			{
				File MD5file = new File(params[0]+".md5sum");
				if (MD5file.exists() && MD5file.canRead())
					MD5exists = true;
				if (params[0].exists() && params[0].canRead())
				{
					//If MD5 File exists, check it
					if(MD5exists)
					{
						//Calculate MD5 of Existing Update
						String calculatedMD5 = MD5.calculateMD5(params[0]);
						//Read the existing MD5SUM
						FileReader input = new FileReader(MD5file);
						BufferedReader bufRead = new BufferedReader(input);
						String firstLine = bufRead.readLine();
						bufRead.close();
						input.close();
						//If the content of the File is not empty, compare it
						if (firstLine != null)
						{
							String[] SplittedString = firstLine.split("  ");
							if(SplittedString[0].equalsIgnoreCase(calculatedMD5))
								mreturnvalue = true;
						}
						else
							mreturnvalue = false;
					}
					else
					{
						return true;
					}
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, "IOEx while checking MD5 sum", e);
				mreturnvalue = false;
			}
			return mreturnvalue;
		}

		@Override
		public void onPostExecute(Boolean result)
		{
			UpdateInfo ui = new UpdateInfo();
			String[] temp = mFilename.split("\\\\");
			ui.name = temp[temp.length-1];
			ui.fileName = mFilename;
			if(result == true)
			{
				Intent i = new Intent(MainActivity.this, ApplyUpdateActivity.class)
				.putExtra(Constants.KEY_UPDATE_INFO, ui);
				startActivity(i);
			}
			else
			{
				Toast.makeText(MainActivity.this, R.string.apply_existing_update_md5error_message, Toast.LENGTH_LONG).show();
			}
			
			//Is null when no MD5SUM is present
			if(mDialog != null)
				mDialog.dismiss();
		}

		@Override
		public void onCancelled()
		{
			Log.d(TAG, "MD5Checker Task cancelled");
			Intent i = new Intent(MainActivity.this, MainActivity.class);
			i.putExtra(Constants.KEY_REQUEST, Constants.REQUEST_MD5CHECKER_CANCEL);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "onCreate called");
		super.onCreate(savedInstanceState);
		prefs = Preferences.getPreferences(this);
		res = getResources();
		setWallpaper();
		
		//Sets the Title to Appname + Mod Version
		setTitle(res.getString(R.string.app_name) + " " + res.getString(R.string.title_running) + " " + SysUtils.getReadableModVersion());
		
		if(prefs.isFirstRun())
		{
			prefs.configureModString();
			prefs.setFirstRun(false);
		}
		//If an older Version was installed, the ModVersion is still ADP1. So reset it
		if(prefs.getConfiguredModString().equalsIgnoreCase("ADP1"))
			prefs.configureModString();

		mUpdateServer = new PlainTextUpdateServer(this);
	}

	@Override
	protected void onStart()
	{
		Log.d(TAG, "onStart called");
		super.onStart();
		
		//Delete any older Versions, because of the changed Signing Key
		while (!deleteOldVersionsOfUpdater())
		{
			//User MUST uninstall old App
			Log.d(TAG, "Old App not uninstalled, try again");
		}
	}

	@Override
	protected void onResume()
	{
		Log.d(TAG, "onResume called");
		super.onResume();
		Intent UpdateIntent = getIntent();
		if (UpdateIntent != null)
		{
			int req = UpdateIntent.getIntExtra(Constants.KEY_REQUEST, -1);
			switch(req)
			{
				case Constants.REQUEST_UPDATE_CHECK_ERROR:
					Log.d(TAG, "Update check error");
					Toast.makeText(this, R.string.not_update_check_error_ticker, Toast.LENGTH_SHORT).show();
					break;
				case Constants.REQUEST_DOWNLOAD_FAILED:
					Log.d(TAG, "Download Error");
					Toast.makeText(this, R.string.exception_while_downloading, Toast.LENGTH_SHORT).show();
					break;
				case Constants.REQUEST_MD5CHECKER_CANCEL:
					Log.d(TAG, "MD5Check canceled. Switching Layout");
					Toast.makeText(this, R.string.md5_check_cancelled, Toast.LENGTH_SHORT).show();
					break;
				default:
					Log.d(TAG, "No Intent. Starting App in Default mode");
					break;
			}
		}
		else
		{
			Log.d(TAG, "Intent is NULL");
		}
		
		mfilenames = null;
		mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + Preferences.getPreferences(this).getUpdateFolder());
		FilenameFilter f = new UpdateFilter(".zip");
		File[] files = mUpdateFolder.listFiles(f);
		//If Folder Exists and Updates are present(with md5files)
		if(mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null && files.length>0)
		{
			//To show only the Filename. Otherwise the whole Path with /sdcard/cm-updates will be shown
			mfilenames = new ArrayList<String>();
			for (int i=0;i<files.length;i++)
			{
				mfilenames.add(files[i].getName());
			}
			//For sorting the Filenames, have to find a way to do natural sorting
			mfilenames = Collections.synchronizedList(mfilenames); 
            Collections.sort(mfilenames, Collections.reverseOrder()); 
		}
		files = null;

		if(DownloadActivity.mUpdateDownloaderService != null && DownloadActivity.mUpdateDownloaderService.isDownloading())
		{
			UpdateInfo ui = DownloadActivity.mUpdateDownloaderService.getCurrentUpdate();
			Intent i = new Intent(MainActivity.this, DownloadActivity.class);
			i.putExtra(Constants.UPDATE_INFO, ui);
			startActivity(i);
		}
		else
		{
			switchToUpdateChooserLayout(null);
		}
	}
	
	@Override
	protected void onStop()
	{
		Log.d(TAG, "onStop called");
		super.onStop();
		Log.d(TAG, "App closed");
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		setWallpaper();
        super.onConfigurationChanged(newConfig); 
        Log.d(TAG, "Orientation Changed. New Orientation: "+newConfig.orientation);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Constants.MENU_ID_UPDATE_NOW, Menu.NONE, R.string.menu_check_now)
		.setIcon(R.drawable.check_now);
		menu.add(Menu.NONE, Constants.MENU_ID_SCAN_QR, Menu.NONE, R.string.menu_qr_code)
		.setIcon(R.drawable.button_scanqr);
		menu.add(Menu.NONE, Constants.MENU_ID_CONFIG, Menu.NONE, R.string.menu_config)
		.setIcon(R.drawable.button_config);
		menu.add(Menu.NONE, Constants.MENU_ID_ABOUT, Menu.NONE, R.string.menu_about)
		.setIcon(R.drawable.button_about);
		menu.add(Menu.NONE, Constants.MENU_ID_CHANGELOG, Menu.NONE, R.string.menu_changelog)
		.setIcon(R.drawable.button_clog);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) 
	{
		boolean superReturn = super.onPrepareOptionsMenu(menu);

		if(DownloadActivity.mUpdateDownloaderService != null && DownloadActivity.mUpdateDownloaderService.isDownloading())
		{
			//Download in progress
			menu.findItem(Constants.MENU_ID_UPDATE_NOW).setEnabled(false);
		}
		else if (mAvailableUpdates != null)
		{
			//Available updates
		}
		else
		{
			//No available updates
		}
		return superReturn;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch(item.getItemId())
		{
			case Constants.MENU_ID_UPDATE_NOW:
				checkForUpdates();
				return true;
			case Constants.MENU_ID_SCAN_QR:
				scanQRURL();
				return true;
			case Constants.MENU_ID_CONFIG:
				showConfigActivity();
				return true;
			case Constants.MENU_ID_ABOUT:
				showAboutDialog();
				return true;
			case Constants.MENU_ID_CHANGELOG:
				getChangelog(Constants.CHANGELOGTYPE_APP);
				//Open the Browser for Changelog
				//Preferences prefs = Preferences.getPreferences(this);
				//Intent i = new Intent(Intent.ACTION_VIEW);
				//i.setData(Uri.parse(prefs.getAboutURL()));
				//startActivity(i);
				return true;
			default:
				Log.d(TAG, "Unknown Menu ID:" + item.getItemId());
				break;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void switchToUpdateChooserLayout(FullUpdateInfo availableUpdates)
	{
		if(availableUpdates != null)
		{
			mAvailableUpdates = availableUpdates;
		}
		else
		{
			try
			{
				mAvailableUpdates = State.loadState(this);
			}
			catch (IOException e)
			{
				Log.e(TAG, "Unable to restore activity status", e);
			}
		}
		
		//Theme Update File URL Set?
		boolean ThemeUpdateUrlSet = prefs.ThemeUpdateUrlSet();
		
		setContentView(R.layout.main);
		flipper = (ViewFlipper)findViewById(R.id.Flipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
		
		//Flipper Buttons
		Button btnAvailableUpdates=(Button)findViewById(R.id.button_available_updates);
		btnAvailableUpdates.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_UPDATES)
					flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_UPDATES);
			}
		});
		Button btnExistingUpdates=(Button)findViewById(R.id.button_existing_updates);
		btnExistingUpdates.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(flipper.getDisplayedChild() != Constants.FLIPPER_EXISTING_UPDATES)
					flipper.setDisplayedChild(Constants.FLIPPER_EXISTING_UPDATES);
			}
		});
		Button btnAvailableThemes=(Button)findViewById(R.id.button_available_themes);
		btnAvailableThemes.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_THEMES)
					flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_THEMES);
			}
		});
		
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);
		
		TextView experimentalBuildsRomtv = (TextView) findViewById(R.id.experimental_rom_updates_textview);
		TextView showDowngradesRomtv = (TextView) findViewById(R.id.show_rom_downgrades_textview);
		TextView experimentalBuildsThemetv = (TextView) findViewById(R.id.experimental_theme_updates_textview);
		TextView showDowngradesThemetv = (TextView) findViewById(R.id.show_theme_downgrades_textview);
		TextView lastRomUpdateChecktv = (TextView) findViewById(R.id.last_rom_update_check);
		TextView lastThemeUpdateChecktv = (TextView) findViewById(R.id.last_theme_update_check);
		
		//Experimental and All
		String showExperimentalRomUpdates;
		String showAllRomUpdates;
		String showExperimentalThemeUpdates;
		String showAllThemeUpdates;
		
		if(prefs.showExperimentalRomUpdates())
			showExperimentalRomUpdates = res.getString(R.string.true_string);
		else
			showExperimentalRomUpdates = res.getString(R.string.false_string);
		
		if(prefs.showAllRomUpdates())
			showAllRomUpdates = res.getString(R.string.true_string);
		else
			showAllRomUpdates = res.getString(R.string.false_string);
		
		if(prefs.showExperimentalThemeUpdates())
			showExperimentalThemeUpdates = res.getString(R.string.true_string);
		else
			showExperimentalThemeUpdates = res.getString(R.string.false_string);
		
		if(prefs.showAllThemeUpdates())
			showAllThemeUpdates = res.getString(R.string.true_string);
		else
			showAllThemeUpdates = res.getString(R.string.false_string);
		
		experimentalBuildsRomtv.setText(MessageFormat.format(res.getString(R.string.p_allow_experimental_rom_versions_title)+": {0}", showExperimentalRomUpdates));
		showDowngradesRomtv.setText(MessageFormat.format(res.getString(R.string.p_display_older_rom_versions_title)+": {0}", showAllRomUpdates));
		experimentalBuildsThemetv.setText(MessageFormat.format(res.getString(R.string.p_allow_experimental_theme_versions_title)+": {0}", showExperimentalThemeUpdates));
		showDowngradesThemetv.setText(MessageFormat.format(res.getString(R.string.p_display_older_theme_versions_title)+": {0}", showAllThemeUpdates));
		lastRomUpdateChecktv.setText(res.getString(R.string.last_update_check_text) + ": " + prefs.getLastUpdateCheckString());
		lastThemeUpdateChecktv.setText(res.getString(R.string.last_update_check_text) + ": " + prefs.getLastUpdateCheckString());
		
		//Existing Updates Layout
		mdownloadedUpdateText = (TextView) findViewById(R.id.downloaded_update_found);
		mspFoundUpdates = mExistingUpdatesSpinner = (Spinner) findViewById(R.id.found_updates_list);
		mdeleteOldUpdatesButton = (Button) findViewById(R.id.delete_updates_button);
		mapplyUpdateButton = (Button) findViewById(R.id.apply_update_button);
		mNoExistingUpdatesFound = (TextView) findViewById(R.id.no_existing_updates_found_textview);
		
		//Rom Layout
		Button selectUploadButton = (Button) findViewById(R.id.download_update_button);
		mUpdatesSpinner = (Spinner) findViewById(R.id.available_updates_list);
		TextView DownloadText = (TextView) findViewById(R.id.available_updates_text);
		LinearLayout stableExperimentalInfoUpdates = (LinearLayout) findViewById(R.id.stable_experimental_description_container_updates);
		Button changelogButton = (Button) findViewById(R.id.show_changelog_button);
		
		//Theme Layout
		Button btnDownloadTheme = (Button) findViewById(R.id.download_theme_button);
		mThemesSpinner = (Spinner) findViewById(R.id.available_themes_list);
		TextView tvThemeDownloadText = (TextView) findViewById(R.id.available_themes_text);
		LinearLayout stableExperimentalInfoThemes = (LinearLayout) findViewById(R.id.stable_experimental_description_container_themes);
		Button btnThemechangelogButton = (Button) findViewById(R.id.show_theme_changelog_button);
		TextView tvNoThemeUpdateServer = (TextView) findViewById(R.id.no_theme_update_server_configured);
		
		//No ROM Updates Found Layout
		Button CheckNowUpdateChooserUpdates = (Button) findViewById(R.id.check_now_button_update_chooser_updates);
		TextView CheckNowUpdateChooserTextUpdates = (TextView) findViewById(R.id.check_now_update_chooser_text_updates);
		CheckNowUpdateChooserTextUpdates.setVisibility(View.GONE);
		CheckNowUpdateChooserUpdates.setVisibility(View.GONE);

		//No Theme Updates Found Layout
		Button CheckNowUpdateChooserThemes = (Button) findViewById(R.id.check_now_button_update_chooser_themes);
		TextView CheckNowUpdateChooserTextThemes = (TextView) findViewById(R.id.check_now_update_chooser_text_themes);
		CheckNowUpdateChooserTextThemes.setVisibility(View.GONE);
		CheckNowUpdateChooserThemes.setVisibility(View.GONE);
		
		//Sets the Theme and Rom Variables
		List<UpdateInfo> availableRoms = null;
		List<UpdateInfo> availableThemes = null;
		if (mAvailableUpdates != null)
		{
			if (mAvailableUpdates.roms != null)
				availableRoms = mAvailableUpdates.roms;
			if (mAvailableUpdates.themes != null)
				availableThemes = mAvailableUpdates.themes;
		}
		
		//Rom Layout
		if(availableRoms != null && availableRoms.size() > 0)
		{
			selectUploadButton.setOnClickListener(mDownloadUpdateButtonListener);
			changelogButton.setOnClickListener(mUpdateChangelogButtonListener);
			mUpdatesSpinner.setOnItemSelectedListener(mUpdateSpinnerChanged);
			
			UpdateListAdapter<UpdateInfo> spAdapterRoms = new UpdateListAdapter<UpdateInfo>(
					this,
					android.R.layout.simple_spinner_item,
					availableRoms);
			spAdapterRoms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mUpdatesSpinner.setAdapter(spAdapterRoms);

		}
		else
		{
			selectUploadButton.setVisibility(View.GONE);
			mUpdatesSpinner.setVisibility(View.GONE);
			DownloadText.setVisibility(View.GONE);
			stableExperimentalInfoUpdates.setVisibility(View.GONE);
			changelogButton.setVisibility(View.GONE);
			CheckNowUpdateChooserTextUpdates.setVisibility(View.VISIBLE);
			CheckNowUpdateChooserUpdates.setVisibility(View.VISIBLE);
			CheckNowUpdateChooserUpdates.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					checkForUpdates();
				}
			});
		}

		//Theme Layout
		//Update URL Set?
		if (!ThemeUpdateUrlSet)
		{
			tvNoThemeUpdateServer.setVisibility(View.VISIBLE);
			btnDownloadTheme.setVisibility(View.GONE);
			mThemesSpinner.setVisibility(View.GONE);
			tvThemeDownloadText.setVisibility(View.GONE);
			stableExperimentalInfoThemes.setVisibility(View.GONE);
			btnThemechangelogButton.setVisibility(View.GONE);
			CheckNowUpdateChooserTextThemes.setVisibility(View.GONE);
			CheckNowUpdateChooserThemes.setVisibility(View.GONE);
		}
		//Themes
		else if(availableThemes != null && availableThemes.size() > 0)
		{
			btnDownloadTheme.setOnClickListener(mDownloadThemeButtonListener);
			btnThemechangelogButton.setOnClickListener(mThemeChangelogButtonListener);
			mThemesSpinner.setOnItemSelectedListener(mThemeSpinnerChanged);
			
			UpdateListAdapter<UpdateInfo> spAdapterThemes = new UpdateListAdapter<UpdateInfo>(
					this,
					android.R.layout.simple_spinner_item,
					availableThemes);
			spAdapterThemes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mThemesSpinner.setAdapter(spAdapterThemes);

		}
		//No Updates Found
		else
		{
			btnDownloadTheme.setVisibility(View.GONE);
			mThemesSpinner.setVisibility(View.GONE);
			tvThemeDownloadText.setVisibility(View.GONE);
			stableExperimentalInfoThemes.setVisibility(View.GONE);
			btnThemechangelogButton.setVisibility(View.GONE);
			CheckNowUpdateChooserTextThemes.setVisibility(View.VISIBLE);
			CheckNowUpdateChooserThemes.setVisibility(View.VISIBLE);
			CheckNowUpdateChooserThemes.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					checkForUpdates();
				}
			});
		}

		//Existing Updates Layout
		if (mfilenames != null && mfilenames.size() > 0)
		{
			ArrayAdapter<String> localUpdates = new ArrayAdapter<String>(
					this,
					android.R.layout.simple_spinner_item,
					mfilenames);
			localUpdates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mspFoundUpdates.setAdapter(localUpdates);
		  	mapplyUpdateButton.setOnClickListener(new mApplyExistingButtonListener());
			mdeleteOldUpdatesButton.setOnClickListener(mDeleteUpdatesButtonListener);
		}
		else
		{
			mNoExistingUpdatesFound.setVisibility(View.VISIBLE);
			mspFoundUpdates.setVisibility(View.GONE);
			mapplyUpdateButton.setVisibility(View.GONE);
			mdownloadedUpdateText.setVisibility(View.GONE);
			mdeleteOldUpdatesButton.setVisibility(View.GONE);
		}
	}

	private void getChangelog(int changelogType)
	{	
		//Handler for the ThreadClass, that downloads the AppChangelog
		ChangelogProgressHandler = new Handler()
		{
			@SuppressWarnings("unchecked")
			public void handleMessage(Message msg)
			{
				if (ChangelogProgressDialog != null)
					ChangelogProgressDialog.dismiss();
				if (msg.obj instanceof String)
				{
					Toast.makeText(MainActivity.this, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
					ChangelogList = null;
					MainActivity.this.ChangelogThread.interrupt();
					ChangelogProgressDialog.dismiss();
					displayChangelog(Constants.CHANGELOGTYPE_APP);
				}
				else if (msg.obj instanceof List<?>)
				{
					ChangelogList = (List<Version>) msg.obj;
					MainActivity.this.ChangelogThread.interrupt();
					ChangelogProgressDialog.dismiss();
					displayChangelog(Constants.CHANGELOGTYPE_APP);
				}
	        }
	    };
		
		switch (changelogType)
		{
			case Constants.CHANGELOGTYPE_ROM:
				//Get the ROM Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mUpdatesSpinner.getSelectedItem());
				displayChangelog(Constants.CHANGELOGTYPE_ROM);
				break;
			case Constants.CHANGELOGTYPE_THEME:
				//Get the ROM Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mThemesSpinner.getSelectedItem());
				displayChangelog(Constants.CHANGELOGTYPE_THEME);
				break;
			case Constants.CHANGELOGTYPE_APP:
				//Show a ProgressDialog and start the Thread. The Dialog is shown in the Handler Function
				ChangelogProgressDialog = ProgressDialog.show(this, res.getString(R.string.changelog_progress_title), res.getString(R.string.changelog_progress_body), true);
				ChangelogThread = new Thread(new Changelog(this));
				ChangelogThread.start();
				break;
			default:
				return;
		}
	}
	
	private void displayChangelog(int changelogtype)
	{
		if (ChangelogList == null)
			return; 
		boolean ChangelogEmpty = true;
		Dialog dialog = new Dialog(this);
		String dialogTitle;
		switch (changelogtype)
		{
			case Constants.CHANGELOGTYPE_ROM:
				dialogTitle = res.getString(R.string.changelog_title_rom);
				break;
			case Constants.CHANGELOGTYPE_THEME:
				dialogTitle = res.getString(R.string.changelog_title_theme);
				break;
			case Constants.CHANGELOGTYPE_APP:
				dialogTitle = res.getString(R.string.changelog_title_app);
				break;
			default:
				return;
		}
		dialog.setTitle(dialogTitle);
		dialog.setContentView(R.layout.changelog);
		LinearLayout main = (LinearLayout) dialog.findViewById(R.id.ChangelogLinearMain);
		
		LayoutParams lp1 = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
		LayoutParams lp3 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		//Foreach Version
		for (Version v:ChangelogList)
		{
			if (v.ChangeLogText.isEmpty())
			{
				continue;
			}
			ChangelogEmpty = false;
			TextView versiontext = new TextView(this);
			versiontext.setLayoutParams(lp1);
			versiontext.setGravity(Gravity.CENTER);
			versiontext.setTextColor(Color.RED);
			versiontext.setText("Version " + v.Version);
			versiontext.setTypeface(null, Typeface.BOLD);
			versiontext.setTextSize((versiontext.getTextSize() * (float)1.5));
			main.addView(versiontext);
			//Foreach Changelogtext
			for(String Change:v.ChangeLogText)
			{
				LinearLayout l = new LinearLayout(this);
				l.setLayoutParams(lp2);
				l.setGravity(Gravity.CENTER_VERTICAL);
				ImageView i = new ImageView(this);
				i.setLayoutParams(lp3);
				i.setImageResource(R.drawable.icon);
				l.addView(i);
				TextView ChangeText = new TextView(this);
				ChangeText.setLayoutParams(lp3);
				ChangeText.setText(Change);
				l.addView(ChangeText);
				main.addView(l);
				//Horizontal Line
				View ruler = new View(this);
				ruler.setBackgroundColor(Color.WHITE);
				main.addView(ruler, new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 1));
			}
		}
		if(!ChangelogEmpty)
			dialog.show();
		else
			Toast.makeText(this, res.getString(R.string.no_changelog_found), Toast.LENGTH_SHORT).show();
		System.gc();
	}

	private void showConfigActivity()
	{
		Intent i = new Intent(this, ConfigActivity.class);
		startActivity(i);
	}

	private void checkForUpdates()
	{
		ProgressDialog pg = ProgressDialog.show(this, res.getString(R.string.checking_for_updates), res.getString(R.string.checking_for_updates), true, true);	
		UpdateCheck u = new UpdateCheck(mUpdateServer, this, pg);
		Thread t = new Thread(u);
		t.start();
	}

	private void showAboutDialog()
	{
		Dialog dialog = new Dialog(this);
		dialog.setTitle(res.getString(R.string.about_dialog_title));
		dialog.setContentView(R.layout.about);
		TextView mVersionName = (TextView) dialog.findViewById(R.id.version_name_about_text_view);            
		try
		{
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);                
			mVersionName.setText("v " + pi.versionName);
		}
		catch (NameNotFoundException e)
		{
			Log.e(TAG, "Can't find version name", e);
			mVersionName.setText("v unknown");
		}
		dialog.show();			
	}

	private void scanQRURL()
	{
		IntentIntegrator.initiateScan(this);
	}

	private void downloadRequestedUpdate(UpdateInfo ui)
	{
		Intent i = new Intent(MainActivity.this, DownloadActivity.class);
		i.putExtra(Constants.UPDATE_INFO, ui);
		startActivity(i);
		Toast.makeText(this, R.string.downloading_update, Toast.LENGTH_SHORT).show();
	}

	private boolean deleteOldUpdates()
	{
		boolean success = false;
		//updateFolder: Foldername
		//mUpdateFolder: Foldername with fullpath of SDCARD
		String updateFolder = prefs.getUpdateFolder();
		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && updateFolder.trim() != "" && updateFolder.trim() != "/")
		{
			deleteDir(mUpdateFolder);
			mUpdateFolder.mkdir();
			Log.d(TAG, "Updates deleted and UpdateFolder created again");
			success=true;
			Toast.makeText(this, R.string.delete_updates_success_message, Toast.LENGTH_LONG).show();
		}
		else if (!mUpdateFolder.exists())
		{
			success = false;
			Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
		}
		else if(updateFolder.trim() == "" || updateFolder.trim() == "/")
		{
			success = false;
			Toast.makeText(this, R.string.delete_updates_root_folder_message, Toast.LENGTH_LONG).show();
		}
		else
		{
			success = false;
			Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
		}
		return success;
	}
	
	private boolean deleteUpdate(String filename)
	{
		boolean success = false;
		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory())
		{
			File ZIPfiletodelete = new File(mUpdateFolder + "/" + filename);
			File MD5filetodelete = new File(mUpdateFolder + "/" + filename + ".md5sum");
			if (ZIPfiletodelete.exists())
			{
				ZIPfiletodelete.delete();
			}
			else
			{
				Log.d(TAG, "Update to delete not found");
				Log.d(TAG, "Zip File: "+ZIPfiletodelete.getAbsolutePath());
				return false;
			}
			if (MD5filetodelete.exists())
			{
				MD5filetodelete.delete();
			}
			else
			{
				Log.d(TAG, "MD5 to delete not found. No Problem here.");
				Log.d(TAG, "MD5 File: "+MD5filetodelete.getAbsolutePath());
			}
			ZIPfiletodelete = null;
			MD5filetodelete = null;
			
			success=true;
			Toast.makeText(this, MessageFormat.format(res.getString(R.string.delete_single_update_success_message), filename), Toast.LENGTH_LONG).show();
		}
		else if (!mUpdateFolder.exists())
		{
			Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
		}
		else
		{
			Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
		}
		return success;
	}

	public static boolean deleteDir(File dir)
	{
		if (dir.isDirectory())
		{ 	
			String[] children = dir.list();
			for (int i=0; i<children.length; i++)
			{
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success)
				{
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (null != scanResult)
		{
			String result = scanResult.getContents();
			if (null != result && !result.equals("") )
			{
				if (result.contains("zip"))
				{
					UpdateInfo ui = new UpdateInfo();
					ui.updateFileUris = new LinkedList<URI>();
					try
					{
						ui.updateFileUris.add(new URI(result));
					}
					catch (URISyntaxException e)
					{
						Log.e(TAG, "Exception while adding URL from QR Scan", e);
					}
					String[] tmp = result.split("/");
					ui.fileName = tmp[tmp.length-1];
					ui.name = ui.fileName;

					Log.d(TAG, "Scanned QR Code: " + scanResult.getContents());
					downloadRequestedUpdate(ui);
				}
				else
				{
					Toast.makeText(getBaseContext(), "Scanned result is not a zip. Please check.", Toast.LENGTH_LONG).show();
				}
			}
			else
			{
				Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
			}

		}
		else
		{
			Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
		}
	}

	private boolean deleteOldVersionsOfUpdater()
	{
		try
		{
			String packageName = "cmupdater.ui";
			PackageManager p = getPackageManager();
			//This throws an Exception, when the Package is not found
			PackageInfo a = p.getPackageInfo(packageName, 0);
			if (a!=null && a.versionCode < 310)
			{
				Log.d(TAG, "Old VersionCode: "+a.versionCode);
				Intent intent1 = new Intent(Intent.ACTION_DELETE); 
				Uri data = Uri.fromParts("package", packageName, null); 
				intent1.setData(data);
				Toast.makeText(getBaseContext(), R.string.toast_uninstall_old_Version, Toast.LENGTH_LONG).show();
				startActivity(intent1);
				Log.d(TAG, "Uninstall Activity started");
				return true;
			}
			else
			{
				throw new PackageManager.NameNotFoundException();
			}
		}
		catch (NameNotFoundException e)
		{
			//No old Version found, so we return true
			Log.d(TAG, "No old Version found");
			return true;
		}
	}
	
	private void setWallpaper()
	{
		getWindow().setBackgroundDrawable(res.getDrawable(R.drawable.background));
	}
}

/**
 * Filename Filter for getting only Files that matches the Given Extensions 
 * Extensions can be split with |
 * Example: .zip|.md5sum  
 *
 * @param  Extensions  String with supported Extensions. Split multiple Extensions with |
 * @return      true when file Matches Extension, otherwise false
 */
class UpdateFilter implements FilenameFilter
{
	private String[] mExtension;
	
	public UpdateFilter(String Extensions)
	{
		mExtension = Extensions.split("\\|");
	}
	
	public boolean accept(File dir, String name)
	{
		for (String Ext : mExtension)
		{
			if (name.endsWith(Ext))
				return true;
		}
		return false;
	}
}