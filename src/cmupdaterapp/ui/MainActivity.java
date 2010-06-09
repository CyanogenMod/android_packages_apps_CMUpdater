package cmupdaterapp.ui;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import cmupdaterapp.changelog.Changelog;
import cmupdaterapp.changelog.Changelog.ChangelogType;
import cmupdaterapp.changelog.Version;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.customization.Customization;
import cmupdaterapp.listadapters.UpdateListAdapter;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.misc.State;
import cmupdaterapp.tasks.MD5CheckerTask;
import cmupdaterapp.tasks.UpdateCheckTask;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.StringUtils;
import cmupdaterapp.utils.SysUtils;
import cmupdaterapp.utils.UpdateFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity";

	//Dialogs
	private static final int DIALOG_NO_SDCARD = 1;
	private static final int DIALOG_OVERWRITE_UPDATE = 2;
	private static final int DIALOG_DELETE_EXISTING = 3;
	private static final int DIALOG_RUNNING_OLD_VERSION = 4;
	private static final int DIALOG_NO_MD5 = 5;

	private Boolean showDebugOutput = true;

	private Spinner mUpdatesSpinner;
	private Spinner mThemesSpinner;
	private Spinner mExistingUpdatesSpinner;
	private File mUpdateFolder;
	private ProgressDialog ChangelogProgressDialog;
	public static Handler ChangelogProgressHandler;
	private Thread ChangelogThread;
	private ViewFlipper flipper;
	private Preferences prefs;
	private Boolean runningOldVersion = false;
	private AsyncTask<File, Void, Boolean> md5CheckerTask;
	private File foo;
	private UpdateInfo updateForDownload;
	private String existingUpdateFilename;

	private final View.OnClickListener ButtonOnClickListener = new View.OnClickListener()
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
				case R.id.button_available_updates:
					if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_UPDATES)
						flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_UPDATES);
					break;
				case R.id.button_available_themes:
					if(flipper.getDisplayedChild() != Constants.FLIPPER_AVAILABLE_THEMES)
						flipper.setDisplayedChild(Constants.FLIPPER_AVAILABLE_THEMES);
					break;
				case R.id.button_existing_updates:
					if(flipper.getDisplayedChild() != Constants.FLIPPER_EXISTING_UPDATES)
						flipper.setDisplayedChild(Constants.FLIPPER_EXISTING_UPDATES);
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
			showDialog(DIALOG_NO_SDCARD);
			return;
		}
		if (showDebugOutput) Log.d(TAG, "Download Rom Button clicked");
		updateForDownload = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
		//Check if the File is present, so prompt the User to overwrite it
		foo = new File(mUpdateFolder + "/" + updateForDownload.getFileName());
		if (foo.isFile() && foo.exists())
		{

			showDialog(DIALOG_OVERWRITE_UPDATE);
			return;
		}
		//Otherwise download it
		else
		{
			downloadRequestedUpdate(updateForDownload);
		}
	}

	private void DownloadThemeButtonListener()
	{
		if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
		{
			showDialog(DIALOG_NO_SDCARD);
			return;
		}

		if (showDebugOutput) Log.d(TAG, "Download Theme Button clicked");
		updateForDownload = (UpdateInfo) mThemesSpinner.getSelectedItem();
		//Check if the File is present, so prompt the User to overwrite it
		foo = new File(mUpdateFolder + "/" + updateForDownload.getFileName());
		if (foo.isFile() && foo.exists())
		{
			showDialog(DIALOG_OVERWRITE_UPDATE);
			return;
		}
		//Otherwise download it
		else
		{
			downloadRequestedUpdate(updateForDownload);
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
			showDialog(DIALOG_NO_SDCARD);
			return;
		}
		else
		{
			showDialog(DIALOG_DELETE_EXISTING);
		}
	}

	private void ApplyExistingButtonListener()
	{
		if(!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState()))
		{
			showDialog(DIALOG_NO_SDCARD);
			return;
		}

		if (runningOldVersion)
		{
			showDialog(DIALOG_RUNNING_OLD_VERSION);
		}

		existingUpdateFilename = (String) mExistingUpdatesSpinner.getSelectedItem();
		if (showDebugOutput) Log.d(TAG, "Selected to Apply Existing update: " + existingUpdateFilename);
		File Update = new File(mUpdateFolder + "/" +existingUpdateFilename);
		File MD5 = new File(mUpdateFolder + "/" +existingUpdateFilename + ".md5sum");
		//IF no MD5 exists, ask the User what to do
		if(!MD5.exists() || !MD5.canRead())
		{
			showDialog(DIALOG_NO_MD5);
		}
		//If MD5 exists, apply the update normally
		else
		{
			Resources res = getResources();
			ProgressDialog progressDialog = ProgressDialog.show(
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

			md5CheckerTask = new MD5CheckerTask(this, progressDialog, existingUpdateFilename, showDebugOutput).execute(Update);
		}
	}

	private final Spinner.OnItemSelectedListener mUpdateSpinnerChanged = new Spinner.OnItemSelectedListener()
	{
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			Button updateChangelogButton = (Button) findViewById(R.id.show_changelog_button);
			String changelog = ((UpdateInfo) mUpdatesSpinner.getSelectedItem()).getDescription();
			if (changelog == null || changelog.equals(""))
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

			if (changelog == null || changelog.equals(""))
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
			Log.i(TAG,"SDcard Available");
		}
		else
		{
			Log.i(TAG,"SDcard Not Available");
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

		if (showDebugOutput) Log.d(TAG, "onCreate called");

		//Sets the Title to Appname + Mod Version
		Resources res = getResources();
		setTitle(res.getString(R.string.app_name) + " " + res.getString(R.string.title_running) + " " + SysUtils.getModVersion());
		setContentView(R.layout.main);

		//Inflate the Screenshot View if enabled
		if (Customization.Screenshotsupport)
		{
			findViewById(R.id.main_stub_themes).setVisibility(View.VISIBLE);
		}

		//Layout
		flipper = (ViewFlipper)findViewById(R.id.Flipper);
        Button btnAvailableUpdates = (Button) findViewById(R.id.button_available_updates);
        Button btnExistingUpdates = (Button) findViewById(R.id.button_existing_updates);
        Button btnAvailableThemes = (Button) findViewById(R.id.button_available_themes);
		//Make the ScreenshotButton invisible
		if (!Customization.Screenshotsupport)
		{
			btnAvailableThemes.setVisibility(View.GONE);
		}

		//Flipper Buttons
		btnAvailableUpdates.setOnClickListener(ButtonOnClickListener);
		btnExistingUpdates.setOnClickListener(ButtonOnClickListener);
		if (Customization.Screenshotsupport)
		{
			btnAvailableThemes.setOnClickListener(ButtonOnClickListener);
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
			showDialog(DIALOG_RUNNING_OLD_VERSION);
			return;
		}
	}

	@Override
	protected void onResume()
	{
		if (showDebugOutput) Log.d(TAG, "onResume called");
		super.onResume();

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
		FullUpdateInfo mAvailableUpdates = null;
		try
		{
			mAvailableUpdates = State.loadState(this, showDebugOutput);
		}
		catch (IOException e)
		{
			Log.e(TAG, "Unable to restore activity status", e);
		}

		//Rom Layout
		View roms = findViewById(R.id.rom_layout);
		TextView experimentalBuildsRomtv = (TextView) roms.findViewById(R.id.experimental_rom_updates_textview);
		TextView showDowngradesRomtv = (TextView) roms.findViewById(R.id.show_rom_downgrades_textview);
		TextView lastRomUpdateChecktv = (TextView) roms.findViewById(R.id.last_rom_update_check);
		Button selectUploadButton = (Button) roms.findViewById(R.id.download_update_button);
		Spinner mUpdatesSpinner = (Spinner) roms.findViewById(R.id.available_updates_list);
		TextView DownloadText = (TextView) roms.findViewById(R.id.available_updates_text);
		LinearLayout stableExperimentalInfoUpdates = (LinearLayout) roms.findViewById(R.id.stable_experimental_description_container_updates);
		Button changelogButton = (Button) roms.findViewById(R.id.show_changelog_button);
		//No ROM Updates Found Layout
		Button CheckNowUpdateChooserUpdates = (Button) roms.findViewById(R.id.check_now_button_update_chooser_updates);
		TextView CheckNowUpdateChooserTextUpdates = (TextView) roms.findViewById(R.id.check_now_update_chooser_text_updates);

		//Existing Updates Layout
		View existing = findViewById(R.id.existing_layout);
		TextView mdownloadedUpdateText = (TextView) existing.findViewById(R.id.downloaded_update_found);
		Spinner mExistingUpdatesSpinner = (Spinner) existing.findViewById(R.id.found_updates_list);
		Button mdeleteOldUpdatesButton = (Button) existing.findViewById(R.id.delete_updates_button);
		Button mapplyUpdateButton = (Button) existing.findViewById(R.id.apply_update_button);
		TextView mNoExistingUpdatesFound = (TextView) existing.findViewById(R.id.no_existing_updates_found_textview);

		//Theme Layout
		View themes = null;
		TextView showDowngradesThemetv = null;
		TextView experimentalBuildsThemetv = null;
		TextView lastThemeUpdateChecktv = null;
		Button btnDownloadTheme = null;
		Spinner mThemesSpinner = null;
		TextView tvThemeDownloadText = null;
		LinearLayout stableExperimentalInfoThemes = null;
		Button btnThemechangelogButton = null;
		Button btnThemeScreenshotButton = null;
		TextView tvNoThemeUpdateServer = null;
		Button CheckNowUpdateChooserThemes = null;
		TextView CheckNowUpdateChooserTextThemes = null;
		if (Customization.Screenshotsupport)
		{
			themes = findViewById(R.id.themes_layout);
			showDowngradesThemetv = (TextView) themes.findViewById(R.id.show_theme_downgrades_textview);
			experimentalBuildsThemetv = (TextView) themes.findViewById(R.id.experimental_theme_updates_textview);
			lastThemeUpdateChecktv = (TextView) themes.findViewById(R.id.last_theme_update_check);
			btnDownloadTheme = (Button) themes.findViewById(R.id.download_theme_button);
			mThemesSpinner = (Spinner) themes.findViewById(R.id.available_themes_list);
			tvThemeDownloadText = (TextView) themes.findViewById(R.id.available_themes_text);
			stableExperimentalInfoThemes = (LinearLayout) themes.findViewById(R.id.stable_experimental_description_container_themes);
			btnThemechangelogButton = (Button) themes.findViewById(R.id.show_theme_changelog_button);
			btnThemeScreenshotButton = (Button) themes.findViewById(R.id.theme_screenshots_button);
			tvNoThemeUpdateServer = (TextView) themes.findViewById(R.id.no_theme_update_server_configured);
			//No Theme Updates Found Layout
			CheckNowUpdateChooserThemes = (Button) themes.findViewById(R.id.check_now_button_update_chooser_themes);
			CheckNowUpdateChooserTextThemes = (TextView) themes.findViewById(R.id.check_now_update_chooser_text_themes);
		}

		//Read existing Updates
		List<String> existingFilenames = null;
		mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + prefs.getUpdateFolder());
		FilenameFilter f = new UpdateFilter(".zip");
		File[] files = mUpdateFolder.listFiles(f);
		//If Folder Exists and Updates are present(with md5files)
		if(mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null && files.length>0)
		{
			//To show only the Filename. Otherwise the whole Path with /sdcard/cm-updates will be shown
			existingFilenames = new ArrayList<String>();
            for (File file : files) {
            	existingFilenames.add(file.getName());
            }
			//For sorting the Filenames, have to find a way to do natural sorting
            existingFilenames = Collections.synchronizedList(existingFilenames);
            Collections.sort(existingFilenames, Collections.reverseOrder());
		}
		files = null;

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

		Resources res = getResources();
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
		if (existingFilenames != null && existingFilenames.size() > 0)
		{
			ArrayAdapter<String> localUpdates = new ArrayAdapter<String>(
					this,
					android.R.layout.simple_spinner_item,
					existingFilenames);
			localUpdates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mExistingUpdatesSpinner.setAdapter(localUpdates);
		  	mapplyUpdateButton.setOnClickListener(ButtonOnClickListener);
			mdeleteOldUpdatesButton.setOnClickListener(ButtonOnClickListener);
		}
		else
		{
			mNoExistingUpdatesFound.setVisibility(View.VISIBLE);
			mExistingUpdatesSpinner.setVisibility(View.GONE);
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
				List<Version> ChangelogList;
				if (ChangelogProgressDialog != null)
					ChangelogProgressDialog.dismiss();
				if (msg.obj instanceof String)
				{
					Toast.makeText(MainActivity.this, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
					ChangelogList = null;
					MainActivity.this.ChangelogThread.interrupt();
					ChangelogProgressDialog.dismiss();
					displayChangelog(ChangelogType.APP, ChangelogList);
				}
				else if (msg.obj instanceof List<?>)
				{
					ChangelogList = (List<Version>) msg.obj;
					MainActivity.this.ChangelogThread.interrupt();
					ChangelogProgressDialog.dismiss();
					displayChangelog(ChangelogType.APP, ChangelogList);
				}
	        }
	    };

	    List<Version> ChangelogList;
		switch (changelogType)
		{
			case ROM:
				//Get the ROM Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mUpdatesSpinner.getSelectedItem());
				displayChangelog(ChangelogType.ROM, ChangelogList);
				break;
			case THEME:
				//Get the THEME Changelog and Display the Changelog
				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mThemesSpinner.getSelectedItem());
				displayChangelog(ChangelogType.THEME, ChangelogList);
				break;
			case APP:
				//Show a ProgressDialog and start the Thread. The Dialog is shown in the Handler Function
				Resources res = getResources();
				ChangelogProgressDialog = ProgressDialog.show(this, res.getString(R.string.changelog_progress_title), res.getString(R.string.changelog_progress_body), true);
				ChangelogThread = new Thread(new Changelog(this));
				ChangelogThread.start();
				break;
			default:
				return;
		}
	}

	private void displayChangelog(ChangelogType changelogtype, List<Version> ChangelogList)
	{
		if (ChangelogList == null)
			return;
		boolean ChangelogEmpty = true;
		Dialog dialog = new Dialog(this);
		int dialogTitle;
		switch (changelogtype)
		{
			case ROM:
				dialogTitle = R.string.changelog_title_rom;
				break;
			case THEME:
				dialogTitle = R.string.changelog_title_theme;
				break;
			case APP:
				dialogTitle = R.string.changelog_title_app;
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
			Toast.makeText(this, R.string.no_changelog_found, Toast.LENGTH_LONG).show();
		System.gc();
	}

	private void showConfigActivity()
	{
		Intent i = new Intent(this, ConfigActivity.class);
		startActivity(i);
	}

	private void checkForUpdates()
	{
		Resources res = getResources();
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
		dialog.setTitle(R.string.about_dialog_title);
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
		boolean success;
		//updateFolder: Foldername
		//mUpdateFolder: Foldername with fullpath of SDCARD
		String updateFolder = prefs.getUpdateFolder();
		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory() && !updateFolder.trim().equals("") && !updateFolder.trim().equals("/"))
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
		else if(updateFolder.trim().equals("") || updateFolder.trim().equals("/"))
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
			Toast.makeText(this, MessageFormat.format(getResources().getString(R.string.delete_single_update_success_message), filename), Toast.LENGTH_LONG).show();
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
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
	    switch(id) {
		    case DIALOG_NO_SDCARD:
		    	return new AlertDialog.Builder(this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}).create();
		    case DIALOG_OVERWRITE_UPDATE:
		    	return new AlertDialog.Builder(this)
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
						foo.delete();
						if (showDebugOutput) Log.d(TAG, "Start downlading update: " + updateForDownload.getFileName());
						downloadRequestedUpdate(updateForDownload);
					}
				}).create();
		    case DIALOG_DELETE_EXISTING:
		    	return new AlertDialog.Builder(this)
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
						deleteUpdate(f);
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
						deleteOldUpdates();
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
				}).create();
		    case DIALOG_RUNNING_OLD_VERSION:
		    	return new AlertDialog.Builder(MainActivity.this)
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
				}).create();
		    case DIALOG_NO_MD5:
		    	return new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.no_md5_found_title)
				.setMessage(R.string.no_md5_found_summary)
				.setPositiveButton(R.string.no_md5_found_positive, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						//Directly call on Postexecute, cause we need no md5check
						new MD5CheckerTask(MainActivity.this, null, existingUpdateFilename, showDebugOutput).onPostExecute(true);
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.no_md5_found_negative, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}).create();
		    default:
		        return null;
	    }
	}
}