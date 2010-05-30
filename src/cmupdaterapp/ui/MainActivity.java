package cmupdaterapp.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
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
import cmupdaterapp.customization.Customization;
import cmupdaterapp.listadapters.UpdateListAdapter;
import cmupdaterapp.tasks.MD5CheckerTask;
import cmupdaterapp.tasks.UpdateCheckTask;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.StringUtils;
import cmupdaterapp.utils.SysUtils;
import cmupdaterapp.utils.UpdateFilter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.misc.State;
import cmupdaterapp.changelog.*;
import cmupdaterapp.changelog.Changelog.ChangelogType;

public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity";
	
	private Boolean showDebugOutput = false;
	
	private Spinner mUpdatesSpinner;
	private Spinner mThemesSpinner;
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
	private Boolean runningOldVersion = false;
	private Button btnAvailableUpdates;
	private Button btnExistingUpdates;
	private Button btnAvailableThemes;
	private TextView experimentalBuildsRomtv;
	private TextView showDowngradesRomtv;
	private TextView experimentalBuildsThemetv;
	private TextView showDowngradesThemetv;
	private TextView lastRomUpdateChecktv;
	private TextView lastThemeUpdateChecktv;
	private Button selectUploadButton;
	private TextView DownloadText;
	private LinearLayout stableExperimentalInfoUpdates;
	private Button changelogButton;
	private Button btnDownloadTheme;
	private TextView tvThemeDownloadText;
	private LinearLayout stableExperimentalInfoThemes;
	private Button btnThemechangelogButton;
	private Button btnThemeScreenshotButton;
	private TextView tvNoThemeUpdateServer;
	private Button CheckNowUpdateChooserUpdates;
	private TextView CheckNowUpdateChooserTextUpdates;
	private Button CheckNowUpdateChooserThemes;
	private TextView CheckNowUpdateChooserTextThemes;
	private AsyncTask<File, Void, Boolean> md5CheckerTask;

	private View.OnClickListener ButtonOnClickListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			switch (v.getId())
			{
				case R.id.theme_screenshots_button:
					ScreenshotThemesListener();
					break;
				case R.id.download_update_button:
					DownloadUpdateButtonListener();
					break;
				case R.id.download_theme_button:
					DownloadThemeButtonListener();
					break;
				case R.id.show_changelog_button:
					UpdateChangelogButtonListener();
					break;
				case R.id.show_theme_changelog_button:
					ThemeChangelogButtonListener();
					break;
				case R.id.check_now_button_update_chooser_updates:
					checkForUpdates();
					break;
				case R.id.check_now_button_update_chooser_themes:
					checkForUpdates();
					break;
				case R.id.delete_updates_button:
					DeleteUpdatesButtonListener();
					break;
				case R.id.apply_update_button:
					ApplyExistingButtonListener();
					break;
			}
		}
	};
	
	private void ScreenshotThemesListener()
	{
		if (showDebugOutput) Log.d(TAG, "Theme Screenshot Button clicked");
		final UpdateInfo ui = (UpdateInfo) mThemesSpinner.getSelectedItem();
		Intent i = new Intent(MainActivity.this, ScreenshotActivity.class);
		i.putExtra(Constants.SCREENSHOTS_UPDATE, (Serializable)ui);
		startActivity(i);
		return;
	}

	private void DownloadUpdateButtonListener()
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
		if (showDebugOutput) Log.d(TAG, "Download Rom Button clicked");
		final UpdateInfo ui = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
		//Check if the File is present, so prompt the User to overwrite it
		final File foo = new File(mUpdateFolder + "/" + ui.getFileName());
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
					if (showDebugOutput) Log.d(TAG, "Start downlading Rom update: " + ui.getFileName());
					foo.delete();
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

	private void DownloadThemeButtonListener()
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
			
		if (showDebugOutput) Log.d(TAG, "Download Theme Button clicked");
		final UpdateInfo ui = (UpdateInfo) mThemesSpinner.getSelectedItem();
		//Check if the File is present, so prompt the User to overwrite it
		File foo = new File(mUpdateFolder + "/" + ui.getFileName());
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
					if (showDebugOutput) Log.d(TAG, "Start downlading Theme update: " + ui.getFileName());
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

	private void UpdateChangelogButtonListener()
	{
		if (showDebugOutput) Log.d(TAG, "Rom Changelog Button clicked");
		getChangelog(ChangelogType.ROM);
	}
	
	private void ThemeChangelogButtonListener()
	{
		if (showDebugOutput) Log.d(TAG, "Theme Changelog Button clicked");
		getChangelog(ChangelogType.THEME);
	}

	private void DeleteUpdatesButtonListener()
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
					if (showDebugOutput) Log.d(TAG, "Delete single Update selected: " + f);
					if(deleteUpdate(f))
						mfilenames.remove(f);
					switchToUpdateChooserLayout();
				}
			})
			//Delete All Updates
			.setPositiveButton(R.string.confirm_delete_update_folder_dialog_yes, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					if (showDebugOutput) Log.d(TAG, "Delete all Updates selected");
					//Delete Updates here
					//Set the Filenames to null, so the Spinner will be empty
					if(deleteOldUpdates())
						mfilenames = null;
					switchToUpdateChooserLayout();
				}
			})
			//Delete no Update
			.setNegativeButton(R.string.confirm_delete_update_folder_dialog_no, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					if (showDebugOutput) Log.d(TAG, "Delete no updates selected");
					dialog.dismiss();
				}
			})
			.show();
		}
	}

	private void ApplyExistingButtonListener()
	{
		ProgressDialog mDialog;
		final String filename;
		File Update;

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
		
		if (runningOldVersion)
		{
			new AlertDialog.Builder(MainActivity.this)
			.setTitle(R.string.alert_old_version_title)
			.setMessage(R.string.alert_old_version_summary)
			.setPositiveButton(R.string.alert_old_version_ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.alert_old_version_browser, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					//Open the Browser for Instructions
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(Customization.UPDATE_INSTRUCTIONS_URL));
                    startActivity(i);
                    dialog.dismiss();
				}
			})
			.show();
		}

		filename = (String) mExistingUpdatesSpinner.getSelectedItem();
		if (showDebugOutput) Log.d(TAG, "Selected to Apply Existing update: " + filename);
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
					new MD5CheckerTask(MainActivity.this, null, filename, showDebugOutput).onPostExecute(true);
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
							if(md5CheckerTask != null)
								md5CheckerTask.cancel(true);
						}
					}
			);
	
			md5CheckerTask = new MD5CheckerTask(MainActivity.this, mDialog, filename, showDebugOutput).execute(Update);
		}
	}
	
	private final Spinner.OnItemSelectedListener mUpdateSpinnerChanged = new Spinner.OnItemSelectedListener()
	{
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			Button updateChangelogButton = (Button) findViewById(R.id.show_changelog_button);
			String changelog = ((UpdateInfo) mUpdatesSpinner.getSelectedItem()).getDescription();
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
			Button ScreenshotThemeButton = (Button) findViewById(R.id.theme_screenshots_button);
			UpdateInfo item = (UpdateInfo) mThemesSpinner.getSelectedItem();
			String changelog = item.getDescription();
			List<URI> screenshots = item.screenshots;
			int ScreenshotCount = item.screenshots.size();
			
			if (changelog == null || changelog == "")
			{
				themeChangelogButton.setVisibility(View.GONE);
			}
			else
			{
				themeChangelogButton.setVisibility(View.VISIBLE);
			}
			if (screenshots == null || ScreenshotCount < 1)
			{
				ScreenshotThemeButton.setVisibility(View.GONE);
			}
			else
			{
				ScreenshotThemeButton.setVisibility(View.VISIBLE);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			Log.d(TAG,"SDcard Available");
		}
		else
		{
			Log.d(TAG,"SDcard Not Available");
			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.nosdcardtoast,
					(ViewGroup) findViewById(R.id.toast_layout_root));
			Toast toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			toast.show();
			finish();
		}

		prefs = new Preferences(this);
		
		//Debug Output
		showDebugOutput = prefs.displayDebugOutput();
		
		res = getResources();
		
		if (showDebugOutput) Log.d(TAG, "onCreate called");
		
		//Sets the Title to Appname + Mod Version
		setTitle(res.getString(R.string.app_name) + " " + res.getString(R.string.title_running) + " " + SysUtils.getModVersion());
		setContentView(R.layout.main);
		
		//Inflate the Screenshot View if enabled
		if (Customization.Screenshotsupport)
		{
			((ViewStub) findViewById(R.id.main_stub_themes)).setVisibility(View.VISIBLE);
		}
		
		flipper = (ViewFlipper)findViewById(R.id.Flipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
		btnAvailableUpdates = (Button)findViewById(R.id.button_available_updates);
		btnExistingUpdates = (Button)findViewById(R.id.button_existing_updates);
		btnAvailableThemes = (Button)findViewById(R.id.button_available_themes);
		//Make the ScreenshotButton invisible
		if (!Customization.Screenshotsupport)
		{
			btnAvailableThemes.setVisibility(View.GONE);
		}

		experimentalBuildsRomtv = (TextView) findViewById(R.id.experimental_rom_updates_textview);
		showDowngradesRomtv = (TextView) findViewById(R.id.show_rom_downgrades_textview);
		experimentalBuildsThemetv = (TextView) findViewById(R.id.experimental_theme_updates_textview);
		lastRomUpdateChecktv = (TextView) findViewById(R.id.last_rom_update_check);

		//Existing Updates Layout
		mdownloadedUpdateText = (TextView) findViewById(R.id.downloaded_update_found);
		mspFoundUpdates = mExistingUpdatesSpinner = (Spinner) findViewById(R.id.found_updates_list);
		mdeleteOldUpdatesButton = (Button) findViewById(R.id.delete_updates_button);
		mapplyUpdateButton = (Button) findViewById(R.id.apply_update_button);
		mNoExistingUpdatesFound = (TextView) findViewById(R.id.no_existing_updates_found_textview);
		
		//Rom Layout
		selectUploadButton = (Button) findViewById(R.id.download_update_button);
		mUpdatesSpinner = (Spinner) findViewById(R.id.available_updates_list);
		DownloadText = (TextView) findViewById(R.id.available_updates_text);
		stableExperimentalInfoUpdates = (LinearLayout) findViewById(R.id.stable_experimental_description_container_updates);
		changelogButton = (Button) findViewById(R.id.show_changelog_button);
		
		//Theme Layout
		if (Customization.Screenshotsupport)
		{
			showDowngradesThemetv = (TextView) findViewById(R.id.show_theme_downgrades_textview);
			lastThemeUpdateChecktv = (TextView) findViewById(R.id.last_theme_update_check);
			btnDownloadTheme = (Button) findViewById(R.id.download_theme_button);
			mThemesSpinner = (Spinner) findViewById(R.id.available_themes_list);
			tvThemeDownloadText = (TextView) findViewById(R.id.available_themes_text);
			stableExperimentalInfoThemes = (LinearLayout) findViewById(R.id.stable_experimental_description_container_themes);
			btnThemechangelogButton = (Button) findViewById(R.id.show_theme_changelog_button);
			btnThemeScreenshotButton = (Button) findViewById(R.id.theme_screenshots_button);
			tvNoThemeUpdateServer = (TextView) findViewById(R.id.no_theme_update_server_configured);
		}
		
		//No ROM Updates Found Layout
		CheckNowUpdateChooserUpdates = (Button) findViewById(R.id.check_now_button_update_chooser_updates);
		CheckNowUpdateChooserTextUpdates = (TextView) findViewById(R.id.check_now_update_chooser_text_updates);
		CheckNowUpdateChooserTextUpdates.setVisibility(View.GONE);
		CheckNowUpdateChooserUpdates.setVisibility(View.GONE);

		//No Theme Updates Found Layout
		if (Customization.Screenshotsupport)
		{
			CheckNowUpdateChooserThemes = (Button) findViewById(R.id.check_now_button_update_chooser_themes);
			CheckNowUpdateChooserTextThemes = (TextView) findViewById(R.id.check_now_update_chooser_text_themes);
			CheckNowUpdateChooserTextThemes.setVisibility(View.GONE);
			CheckNowUpdateChooserThemes.setVisibility(View.GONE);
		}
		
		//Flipper Buttons
		btnAvailableUpdates.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_UPDATES)
					flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_UPDATES);
			}
		});
		btnExistingUpdates.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(flipper.getDisplayedChild() != Constants.FLIPPER_EXISTING_UPDATES)
					flipper.setDisplayedChild(Constants.FLIPPER_EXISTING_UPDATES);
			}
		});
		if (Customization.Screenshotsupport)
		{
			btnAvailableThemes.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View view)
				{
					if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_THEMES)
						flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_THEMES);
				}
			});
		}
	}

	@Override
	protected void onStart()
	{
		if (showDebugOutput) Log.d(TAG, "onStart called");
		super.onStart();

		//Show a Dialog that the User runs an old rom.
		String mod = SysUtils.getModVersion();
		if (StringUtils.compareVersions(Customization.MIN_SUPPORTED_VERSION_STRING, mod))
		{
			runningOldVersion = true;

			new AlertDialog.Builder(MainActivity.this)
			.setTitle(R.string.alert_old_version_title)
			.setMessage(R.string.alert_old_version_summary)
			.setPositiveButton(R.string.alert_old_version_ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.alert_old_version_browser, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					//Open the Browser for Instructions
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(Customization.UPDATE_INSTRUCTIONS_URL));
                    startActivity(i);
                    dialog.dismiss();
				}
			})
			.show();
			return;
		}
	}

	@Override
	protected void onResume()
	{
		if (showDebugOutput) Log.d(TAG, "onResume called");
		super.onResume();

		mfilenames = null;
		mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + prefs.getUpdateFolder());
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

		try
		{
			if(DownloadActivity.myService != null && DownloadActivity.myService.DownloadRunning())
			{
				UpdateInfo ui = DownloadActivity.myService.getCurrentUpdate();
				Intent i = new Intent(MainActivity.this, DownloadActivity.class);
				i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)ui);
				startActivity(i);
			}
			else
			{
				switchToUpdateChooserLayout();
			}
		}
		catch (RemoteException e)
		{
			Log.e(TAG, "Exception on calling DownloadService", e);
		}
	}

	@Override
	protected void onStop()
	{
		if (showDebugOutput) Log.d(TAG, "onStop called");
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Constants.MENU_ID_UPDATE_NOW, Menu.NONE, R.string.menu_check_now)
		.setIcon(R.drawable.check_now);
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

		try
		{
			if(DownloadActivity.myService != null && DownloadActivity.myService.DownloadRunning())
			{
				//Download in progress
				menu.findItem(Constants.MENU_ID_UPDATE_NOW).setEnabled(false);
			}
		}
		catch (RemoteException e)
		{
			Log.e(TAG, "Exception on calling DownloadService", e);
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
			case Constants.MENU_ID_CONFIG:
				showConfigActivity();
				return true;
			case Constants.MENU_ID_ABOUT:
				showAboutDialog();
				return true;
			case Constants.MENU_ID_CHANGELOG:
				getChangelog(ChangelogType.APP);
				return true;
			default:
				Log.e(TAG, "Unknown Menu ID:" + item.getItemId());
				break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void switchToUpdateChooserLayout()
	{
		try
		{
			mAvailableUpdates = State.loadState(this, showDebugOutput);
		}
		catch (IOException e)
		{
			Log.e(TAG, "Unable to restore activity status", e);
		}

		//Reset all Visibilities
		if (Customization.Screenshotsupport)
		{
			CheckNowUpdateChooserTextThemes.setVisibility(View.GONE);
			CheckNowUpdateChooserThemes.setVisibility(View.GONE);
		}
		CheckNowUpdateChooserTextUpdates.setVisibility(View.GONE);
		CheckNowUpdateChooserUpdates.setVisibility(View.GONE);
		selectUploadButton.setVisibility(View.VISIBLE);
		mUpdatesSpinner.setVisibility(View.VISIBLE);
		DownloadText.setVisibility(View.VISIBLE);
		stableExperimentalInfoUpdates.setVisibility(View.VISIBLE);
		changelogButton.setVisibility(View.VISIBLE);
		if (Customization.Screenshotsupport)
		{
			btnDownloadTheme.setVisibility(View.VISIBLE);
			mThemesSpinner.setVisibility(View.VISIBLE);
			tvThemeDownloadText.setVisibility(View.VISIBLE);
			stableExperimentalInfoThemes.setVisibility(View.VISIBLE);
			btnThemechangelogButton.setVisibility(View.VISIBLE);
			btnThemeScreenshotButton.setVisibility(View.VISIBLE);
		}
		
		//Theme Update File URL Set?
		boolean ThemeUpdateUrlSet = prefs.ThemeUpdateUrlSet();

		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);

		//Experimental and All
		String showExperimentalRomUpdates;
		String showAllRomUpdates;
		String showExperimentalThemeUpdates = "";
		String showAllThemeUpdates = "";

		String trueString = res.getString(R.string.true_string);
		String falseString = res.getString(R.string.false_string);

		if(prefs.showExperimentalRomUpdates())
			showExperimentalRomUpdates = trueString;
		else
			showExperimentalRomUpdates = falseString;

		if(prefs.showAllRomUpdates())
			showAllRomUpdates = trueString;
		else
			showAllRomUpdates = falseString;

		if (Customization.Screenshotsupport)
		{
			if(prefs.showExperimentalThemeUpdates())
				showExperimentalThemeUpdates = trueString;
			else
				showExperimentalThemeUpdates = falseString;
	
			if(prefs.showAllThemeUpdates())
				showAllThemeUpdates = trueString;
			else
				showAllThemeUpdates = falseString;
		}
		experimentalBuildsRomtv.setText(MessageFormat.format(res.getString(R.string.p_allow_experimental_rom_versions_title)+": {0}", showExperimentalRomUpdates));
		showDowngradesRomtv.setText(MessageFormat.format(res.getString(R.string.p_display_older_rom_versions_title)+": {0}", showAllRomUpdates));
		if (Customization.Screenshotsupport)
		{
			experimentalBuildsThemetv.setText(MessageFormat.format(res.getString(R.string.p_allow_experimental_theme_versions_title)+": {0}", showExperimentalThemeUpdates));
			showDowngradesThemetv.setText(MessageFormat.format(res.getString(R.string.p_display_older_theme_versions_title)+": {0}", showAllThemeUpdates));
			lastThemeUpdateChecktv.setText(res.getString(R.string.last_update_check_text) + ": " + prefs.getLastUpdateCheckString());
		}
		lastRomUpdateChecktv.setText(res.getString(R.string.last_update_check_text) + ": " + prefs.getLastUpdateCheckString());

		//Sets the Theme and Rom Variables
		List<UpdateInfo> availableRoms = null;
		List<UpdateInfo> availableThemes = null;
		if (mAvailableUpdates != null)
		{
			if (mAvailableUpdates.roms != null)
				availableRoms = mAvailableUpdates.roms;
			if (Customization.Screenshotsupport && mAvailableUpdates.themes != null)
				availableThemes = mAvailableUpdates.themes;
			//Add the incrementalUpdates
			if (mAvailableUpdates.incrementalRoms != null)
				availableRoms.addAll(mAvailableUpdates.incrementalRoms);
		}

		//Rom Layout
		if(availableRoms != null && availableRoms.size() > 0)
		{
			selectUploadButton.setOnClickListener(ButtonOnClickListener);
			changelogButton.setOnClickListener(ButtonOnClickListener);
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
			CheckNowUpdateChooserUpdates.setOnClickListener(ButtonOnClickListener);
		}

		//Disable the download Button when running an old ROM
		if (runningOldVersion)
			selectUploadButton.setEnabled(false);

		//Theme Layout
		//Update URL Set?
		if (Customization.Screenshotsupport)
		{
			if (!ThemeUpdateUrlSet)
			{
				tvNoThemeUpdateServer.setVisibility(View.VISIBLE);
				btnDownloadTheme.setVisibility(View.GONE);
				mThemesSpinner.setVisibility(View.GONE);
				tvThemeDownloadText.setVisibility(View.GONE);
				stableExperimentalInfoThemes.setVisibility(View.GONE);
				btnThemechangelogButton.setVisibility(View.GONE);
				btnThemeScreenshotButton.setVisibility(View.GONE);
				CheckNowUpdateChooserTextThemes.setVisibility(View.GONE);
				CheckNowUpdateChooserThemes.setVisibility(View.GONE);
			}
			//Themes
			else if(availableThemes != null && availableThemes.size() > 0)
			{
				btnDownloadTheme.setOnClickListener(ButtonOnClickListener);
				btnThemechangelogButton.setOnClickListener(ButtonOnClickListener);
				btnThemeScreenshotButton.setOnClickListener(ButtonOnClickListener);
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
				btnThemeScreenshotButton.setVisibility(View.GONE);
				CheckNowUpdateChooserTextThemes.setVisibility(View.VISIBLE);
				CheckNowUpdateChooserThemes.setVisibility(View.VISIBLE);
				CheckNowUpdateChooserThemes.setOnClickListener(ButtonOnClickListener);
		}
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
		  	mapplyUpdateButton.setOnClickListener(ButtonOnClickListener);
			mdeleteOldUpdatesButton.setOnClickListener(ButtonOnClickListener);
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

	private void getChangelog(ChangelogType changelogType)
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
					displayChangelog(ChangelogType.APP);
				}
				else if (msg.obj instanceof List<?>)
				{
					ChangelogList = (List<Version>) msg.obj;
					MainActivity.this.ChangelogThread.interrupt();
					ChangelogProgressDialog.dismiss();
					displayChangelog(ChangelogType.APP);
				}
	        }
	    };

		switch (changelogType)
		{
			case ROM:
				//Get the ROM Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mUpdatesSpinner.getSelectedItem());
				displayChangelog(ChangelogType.ROM);
				break;
			case THEME:
				//Get the THEME Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mThemesSpinner.getSelectedItem());
				displayChangelog(ChangelogType.THEME);
				break;
			case APP:
				//Show a ProgressDialog and start the Thread. The Dialog is shown in the Handler Function
				ChangelogProgressDialog = ProgressDialog.show(this, res.getString(R.string.changelog_progress_title), res.getString(R.string.changelog_progress_body), true);
				ChangelogThread = new Thread(new Changelog(this));
				ChangelogThread.start();
				break;
			default:
				return;
		}
	}

	private void displayChangelog(ChangelogType changelogtype)
	{
		if (ChangelogList == null)
			return; 
		boolean ChangelogEmpty = true;
		Dialog dialog = new Dialog(this);
		String dialogTitle;
		switch (changelogtype)
		{
			case ROM:
				dialogTitle = res.getString(R.string.changelog_title_rom);
				break;
			case THEME:
				dialogTitle = res.getString(R.string.changelog_title_theme);
				break;
			case APP:
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
			Toast.makeText(this, res.getString(R.string.no_changelog_found), Toast.LENGTH_LONG).show();
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
		//Refresh the Layout when UpdateCheck finished
		pg.setOnDismissListener(new DialogInterface.OnDismissListener()
		{
			public void onDismiss (DialogInterface dialog)
			{
				switchToUpdateChooserLayout();
			}
		});
		new UpdateCheckTask(this, pg, showDebugOutput).execute((Void) null);
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

	private void downloadRequestedUpdate(UpdateInfo ui)
	{
		Intent i = new Intent(MainActivity.this, DownloadActivity.class);
		i.putExtra(Constants.KEY_UPDATE_INFO, (Serializable)ui);
		startActivity(i);
		Toast.makeText(this, R.string.downloading_update, Toast.LENGTH_LONG).show();
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
			if (showDebugOutput) Log.d(TAG, "Updates deleted and UpdateFolder created again");
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
				if (showDebugOutput) Log.d(TAG, "Update to delete not found");
				if (showDebugOutput) Log.d(TAG, "Zip File: "+ZIPfiletodelete.getAbsolutePath());
				return false;
			}
			if (MD5filetodelete.exists())
			{
				MD5filetodelete.delete();
			}
			else
			{
				if (showDebugOutput) Log.d(TAG, "MD5 to delete not found. No Problem here.");
				if (showDebugOutput) Log.d(TAG, "MD5 File: "+MD5filetodelete.getAbsolutePath());
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

	private static boolean deleteDir(File dir)
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
}