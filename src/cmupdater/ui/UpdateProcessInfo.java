package cmupdater.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.text.DateFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import cmupdater.service.PlainTextUpdateServer;
import cmupdater.service.UpdateDownloaderService;
import cmupdater.service.UpdateInfo;
import cmupdater.utils.IOUtils;
import cmupdater.utils.Preferences;
import cmupdater.utils.SysUtils;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class UpdateProcessInfo extends IUpdateProcessInfo {

	private static final String TAG = "<CM-Updater> UpdateProcessInfo";
	private static final String STORED_STATE_FILENAME = "UpdateProcessInfo.ser";

	private static final int MENU_ID_UPDATE_NOW = 1;
	private static final int MENU_ID_SCAN_QR = 2;
	private static final int MENU_ID_CONFIG= 3;
	private static final int MENU_ID_ABOUT= 4;

	private static final String KEY_AVAILABLE_UPDATES = "cmupdater.availableUpdates";
	private static final String KEY_MIRROR_NAME = "cmupdater.mirrorName";

	public static final int REQUEST_NEW_UPDATE_LIST = 1;
	public static final int REQUEST_UPDATE_CHECK_ERROR = 2;
	public static final int REQUEST_DOWNLOAD_FAILED = 3;

	public static final String KEY_REQUEST = "cmupdater.keyRequest";
	public static final String KEY_UPDATE_LIST = "cmupdater.updateList";

	private Spinner mUpdatesSpinner;
	private PlainTextUpdateServer mUpdateServer;
	private ProgressBar mProgressBar;
	private TextView mDownloadedBytesTextView;
	private TextView mDownloadMirrorTextView;
	private TextView mDownloadFilenameTextView;
	private TextView mDownloadSpeedTextView;
	private TextView mRemainingTimeTextView;
	private List<UpdateInfo> mAvailableUpdates;
	private String mMirrorName;
	private String mFileName;
	private UpdateDownloaderService mUpdateDownloaderService;
	private Intent mUpdateDownloaderServiceIntent;

	private File mUpdateFolder;
	private Spinner mExistingUpdatesSpinner;
	
	private int mSpeed;
	private long mRemainingTime;
	
	private ArrayList<String> mfilenames;
	
	TextView mdownloadedUpdateText;
	Spinner mspFoundUpdates;
	Button mdeleteOldUpdatesButton;
	Button mapplyUpdateButton;
	View mseparator;


	private final ServiceConnection mUpdateDownloaderServiceConnection = new ServiceConnection(){

		public void onServiceConnected(ComponentName className, IBinder service) {
			mUpdateDownloaderService = ((UpdateDownloaderService.LocalBinder)service).getService();
			if(mUpdateDownloaderService.isDownloading()) {
				switchToDownloadingLayout(mUpdateDownloaderService.getCurrentUpdate());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mUpdateDownloaderService = null;
		}
	};

	//static so the reference is kept while the thread is running
	//private static DownloadUpdateTask mDownloadUpdateTask;


	private final View.OnClickListener mSelectUpdateButtonListener = new View.OnClickListener() {
		public void onClick(View v) {

			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				new AlertDialog.Builder(UpdateProcessInfo.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
				return;
			}

			
			UpdateInfo ui = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
			//Check if the File is present, so prompt the User to overwrite it
			File foo = new File(mUpdateFolder + "/" + ui.fileName);
			if (foo.isFile() && foo.exists())
			{
				new AlertDialog.Builder(UpdateProcessInfo.this)
				.setTitle(R.string.overwrite_update_title)
				.setMessage(R.string.overwrite_update_summary)
				.setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						downloadRequestedUpdate((UpdateInfo) mUpdatesSpinner.getSelectedItem());
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

	private final View.OnClickListener mDeleteUpdatesButtonListener = new View.OnClickListener() {
		public void onClick(View v) {

			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				new AlertDialog.Builder(UpdateProcessInfo.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
				return;
			}
			else
			{
				new AlertDialog.Builder(UpdateProcessInfo.this)
				.setTitle(R.string.delete_updates_text)
				.setMessage(R.string.confirm_delete_update_folder_dialog_message)
				//Delete Only Selected Update
				.setNeutralButton(R.string.confirm_delete_update_folder_dialog_neutral, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						//Delete Updates here
						String f = (String) mExistingUpdatesSpinner.getSelectedItem();
						if(deleteUpdate(f))
						{
							mfilenames.remove(f);
							mfilenames.trimToSize();
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
				.setPositiveButton(R.string.confirm_delete_update_folder_dialog_yes, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						//Delete Updates here
						deleteOldUpdates();
						//Set the Filenames to null, so the Spinner will be empty
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
				.setNegativeButton(R.string.confirm_delete_update_folder_dialog_no, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
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
		public void onClick(View v)
		{	
			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			{
				new AlertDialog.Builder(UpdateProcessInfo.this)
				.setTitle(R.string.sdcard_is_not_present_dialog_title)
				.setMessage(R.string.sdcard_is_not_present_dialog_body)
				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				})
				.show();
				return;
			}

			String filename = (String) mExistingUpdatesSpinner.getSelectedItem();
			File Update = new File(mUpdateFolder + "/" +filename);

			Resources r = getResources();
			mDialog = ProgressDialog.show(
					UpdateProcessInfo.this,
					r.getString(R.string.verify_and_apply_dialog_title),
					r.getString(R.string.verify_and_apply_dialog_message),
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

	private final class MD5CheckerTask extends UserTask<File, Void, Boolean>
	{	
		private ProgressDialog mDialog;
		private String mFilename;

		public MD5CheckerTask(ProgressDialog dialog, String filename)
		{
			mDialog = dialog;
			mFilename = filename;
		}

		@Override
		public Boolean doInBackground(File... params)
		{
			boolean returnvalue = false;
			try
			{
				File MD5 = new File(params[0]+".md5sum");
				if (params[0].exists() && params[0].canRead() && MD5.exists() && MD5.canRead())
				{
					//Calculate MD5 of Existing Update
					String calculatedMD5 = IOUtils.calculateMD5(params[0]);
					//Read the existing MD5SUM
					FileReader input = new FileReader(MD5);
					BufferedReader bufRead = new BufferedReader(input);
					String firstLine = bufRead.readLine();
					bufRead.close();
					input.close();
					//If the content of the File is not empty, compare it
					if (firstLine != null)
					{
						String[] SplittedString = firstLine.split("  ");
						if(SplittedString[0].equalsIgnoreCase(calculatedMD5))
							returnvalue = true;
					}
					else
						returnvalue = false;
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, "IOEx while checking MD5 sum", e);
				returnvalue = false;
			}
			return returnvalue;
		}

		@Override
		public void onPostExecute(Boolean result)
		{
			UpdateInfo ui = new UpdateInfo();
			ui.name = "Existing Update";
			ui.fileName = mFilename;
			if(result == true)
			{
				Intent i = new Intent(UpdateProcessInfo.this, ApplyUploadActivity.class)
				.putExtra(ApplyUploadActivity.KEY_UPDATE_INFO, ui);
				startActivity(i);
			}
			else
			{
				Toast.makeText(UpdateProcessInfo.this, R.string.apply_existing_update_md5error_message, Toast.LENGTH_LONG).show();
			}

			mDialog.dismiss();
		}

		@Override
		public void onCancelled()
		{
			//TODO cancel MD% check
		}
	};

	private final View.OnClickListener mCancelDownloadListener = new View.OnClickListener() {

		public void onClick(View arg0) {

			new AlertDialog.Builder(UpdateProcessInfo.this)
			.setMessage(R.string.confirm_download_cancelation_dialog_message)
			.setPositiveButton(R.string.confirm_download_cancelation_dialog_yes, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					mUpdateDownloaderService.cancelDownload();
					switchToUpdateChooserLayout(null);
				}
			})
			.setNegativeButton(R.string.confirm_download_cancelation_dialog_no, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.show();
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preferences prefs = Preferences.getPreferences(this);
		if(prefs.isFirstRun()) {
			prefs.configureModString();
			prefs.setFirstRun(false);
		}
		//If an older Version was installed, the ModVersion is still ADP1. So reset it
		if(prefs.getConfiguredModString().equals("ADP1"))
			prefs.configureModString();

		try {
			loadState();
		} catch (IOException e) {
			Log.e(TAG, "Unable to load application state");
		}

		restoreSavedInstanceValues(savedInstanceState);

		URI uri = URI.create(prefs.getUpdateFileURL());
		mUpdateServer = new PlainTextUpdateServer(uri, this);

		mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + Preferences.getPreferences(this).getUpdateFolder());

		mUpdateDownloaderServiceIntent = new Intent(this, UpdateDownloaderService.class);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void onStart() {
		super.onStart();

		try {
			loadState();
		} catch (FileNotFoundException e) {
			//Ignored, data was not saved
		} catch (IOException e) {
			Log.w(TAG, "Unable to restore activity status", e);
		}

		bindService(mUpdateDownloaderServiceIntent, mUpdateDownloaderServiceConnection, Context.BIND_AUTO_CREATE);

		Intent UpdateIntent = getIntent();
		if (UpdateIntent != null)
		{
			int req = UpdateIntent.getIntExtra(KEY_REQUEST, -1);
			switch(req)
			{
				case REQUEST_NEW_UPDATE_LIST:
					mAvailableUpdates = (List<UpdateInfo>) getIntent().getSerializableExtra(KEY_UPDATE_LIST);
					try
					{
						saveState();
					}
					catch (IOException e)
					{
						Log.e(TAG, "Unable to save application state");
					}
					break;
				case REQUEST_UPDATE_CHECK_ERROR:
					//TODO
					Log.w(TAG, "Update check error");
					break;
		
				case REQUEST_DOWNLOAD_FAILED:
					//TODO
					Log.w(TAG, "Update check error");
					break;
				default:
					Log.w(TAG, "Uknown KEY_REQUEST in Intent. Maybe its the first start.");
					break;
			}
		}
		else
		{
			Log.w(TAG, "Intent is NULL");
		}
		
		//Outside the if to prevent a empty spinnercontrol
		FilenameFilter f = new UpdateFilter();
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
		}
		files = null;
		if(mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading()) {
			switchToDownloadingLayout(mUpdateDownloaderService.getCurrentUpdate());
		} else if (mAvailableUpdates != null || (mfilenames != null && mfilenames.size() > 0)) {
			switchToUpdateChooserLayout(mAvailableUpdates);
		} else {
			switchToNoUpdatesAvailable();
		}
		UpdateDownloaderService.setUpdateProcessInfo(UpdateProcessInfo.this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        Log.i(TAG, "Orientation Changed. New Orientation: "+newConfig.orientation);
    }
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		if (mUpdateDownloaderService != null)
		{
			try {
				saveState();
			} catch (IOException e) {
				Log.w(TAG, "Unable to save state", e);
			}
			Log.d(TAG, "Cancel the download");
			mUpdateDownloaderService.cancelDownload();
			unbindService(mUpdateDownloaderServiceConnection);
			UpdateDownloaderService.setUpdateProcessInfo(null);
		}
		else
			Log.e(TAG, "mUpdateDownloaderService is NULL");
	}

	private void saveState() throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(STORED_STATE_FILENAME, Context.MODE_PRIVATE));
		try {
			Map<String,Serializable> data = new HashMap<String, Serializable>();
			data.put("mAvailableUpdates", (Serializable)mAvailableUpdates);
			data.put("mMirrorName", mMirrorName);
			oos.writeObject(data);
			oos.flush();
		} finally {
			oos.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadState() throws IOException {
		ObjectInputStream ois = new ObjectInputStream(openFileInput(STORED_STATE_FILENAME));
		try {
			Map<String,Serializable> data = (Map<String, Serializable>) ois.readObject();

			Object o = data.get("mAvailableUpdates"); 
			if(o != null) mAvailableUpdates = (List<UpdateInfo>) o;

			o = data.get("mMirrorName"); 
			if(o != null) mMirrorName =  (String) o;
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "Unable to load stored class", e);
		} finally {
			ois.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void restoreSavedInstanceValues(Bundle b) {
		if(b == null) return;
		mAvailableUpdates = (List<UpdateInfo>) b.getSerializable(KEY_AVAILABLE_UPDATES);
		mMirrorName = b.getString(KEY_MIRROR_NAME);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_AVAILABLE_UPDATES, (Serializable)mAvailableUpdates);
		outState.putString(KEY_MIRROR_NAME, mMirrorName);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		int req = intent.getIntExtra(KEY_REQUEST, -1);
		switch(req) {
		case REQUEST_NEW_UPDATE_LIST:
			switchToUpdateChooserLayout((List<UpdateInfo>) intent.getSerializableExtra(KEY_UPDATE_LIST));
			break;
		case REQUEST_UPDATE_CHECK_ERROR:
			//TODO
			Log.w(TAG, "Update check error");
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MENU_ID_UPDATE_NOW, Menu.NONE, R.string.menu_check_now)
		.setIcon(R.drawable.check_now);
		menu.add(Menu.NONE, MENU_ID_SCAN_QR, Menu.NONE, R.string.menu_qr_code)
		.setIcon(android.R.drawable.ic_menu_compass);
		menu.add(Menu.NONE, MENU_ID_CONFIG, Menu.NONE, R.string.menu_config)
		.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, MENU_ID_ABOUT, Menu.NONE, R.string.menu_about)
		.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean superReturn = super.onPrepareOptionsMenu(menu);

		if(mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading()) {
			//Download in progress
			menu.findItem(MENU_ID_UPDATE_NOW).setEnabled(false);
		} else if (mAvailableUpdates != null) {
			//Available updates
		} else {
			//No available updates
		}
		return superReturn;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId())
		{
			case MENU_ID_UPDATE_NOW:
				//If downloading, cancel it so the download wont proceed in the background
				cancelDownloading();
				checkForUpdates();
				return true;
			case MENU_ID_SCAN_QR:
				//If downloading, cancel it so the download wont proceed in the background
				cancelDownloading();
				scanQRURL();
				return true;
			case MENU_ID_CONFIG:
				//If downloading, cancel it so the download wont proceed in the background
				cancelDownloading();
				showConfigActivity();
				return true;
			case MENU_ID_ABOUT:
				//If downloading, cancel it so the download wont proceed in the background
				cancelDownloading();
				showAboutDialog();
				return true;
			default:
				Log.w(TAG, "Unknown Menu ID:" + item.getItemId());
				break;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void switchToNoUpdatesAvailable() {
		mAvailableUpdates = null;
		try {
			saveState();
		} catch (IOException e) {
			Log.w(TAG, "Unable to save application state", e);
		}

		setContentView(R.layout.no_updates);
		LinearLayout checkForUpdatesLayout = (LinearLayout) findViewById(R.id.no_updates_chec_for_updates_layout);
		LinearLayout noModLayout = (LinearLayout) findViewById(R.id.no_updates_no_mod_layout);

		noModLayout.setVisibility(View.GONE);
		checkForUpdatesLayout.setVisibility(View.VISIBLE);
		((Button)findViewById(R.id.check_now_button)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				checkForUpdates();
			}
		});
			
		TextView currentVersion = (TextView) findViewById(R.id.no_updates_current_version);
		TextView experimentalBuilds = (TextView) findViewById(R.id.experimental_updates_textview);
		TextView showDowngrades = (TextView) findViewById(R.id.show_downgrades_textview);
		TextView lastUpdateCheck = (TextView) findViewById(R.id.last_update_check);
		String pattern = getResources().getString(R.string.current_version_text);
		Preferences prefs = Preferences.getPreferences(this);
		currentVersion.setText(MessageFormat.format(pattern, SysUtils.getReadableModVersion()));
		experimentalBuilds.setText(MessageFormat.format(getResources().getString(R.string.p_display_allow_experimental_versions_title)+": {0}", Boolean.toString(prefs.allowExperimental())));
		showDowngrades.setText(MessageFormat.format(getResources().getString(R.string.p_display_older_mod_versions_title)+": {0}", Boolean.toString(prefs.showDowngrades())));
		lastUpdateCheck.setText(MessageFormat.format(getResources().getString(R.string.last_update_check_text)+": {0} {1}", DateFormat.getDateInstance().format(prefs.getLastUpdateCheck()), DateFormat.getTimeInstance().format(prefs.getLastUpdateCheck())));
	}

	@Override
	public void switchToDownloadingLayout(UpdateInfo downloadingUpdate) {
		setContentView(R.layout.update_download_info);
		try {
			String[] temp = downloadingUpdate.updateFileUris.get(0).toURL().getFile().split("/");
			mFileName = temp[temp.length-1];
		} catch (MalformedURLException e) {
			mFileName = "Unable to get Filename";
			e.printStackTrace();
		}
		mProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
		mDownloadedBytesTextView = (TextView) findViewById(R.id.bytes_downloaded_text_view);

		mDownloadMirrorTextView = (TextView) findViewById(R.id.mirror_text_view);

		mDownloadFilenameTextView = (TextView) findViewById(R.id.filename_text_view);

		mDownloadSpeedTextView = (TextView) findViewById(R.id.speed_text_view);
		mRemainingTimeTextView = (TextView) findViewById(R.id.remaining_time_text_view);

		if(mMirrorName != null)
			mDownloadMirrorTextView.setText(mMirrorName);
		if(mFileName != null)
			mDownloadFilenameTextView.setText(mFileName);
		((Button)findViewById(R.id.cancel_download_buton)).setOnClickListener(mCancelDownloadListener);
	}

	@Override
	public void switchToUpdateChooserLayout(List<UpdateInfo> availableUpdates) {
		/*
		 * If availableUpdates is null, use the cached value.
		 * If not, cache the value for future uses
		 */
		if(availableUpdates == null)
		{
			if (null == mAvailableUpdates)
			{
				if (mfilenames == null || mfilenames.size() <= 0)
				{
					//No Updates and nothing downloaded
					switchToNoUpdatesAvailable();
					return;
				}
			}
			else
			{
				availableUpdates = mAvailableUpdates;
			}
		}
		else
		{
			mAvailableUpdates = availableUpdates;
		}

		setContentView(R.layout.update_chooser);
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);
		
		TextView currentVersion = (TextView) findViewById(R.id.up_chooser_current_version);
		TextView experimentalBuilds = (TextView) findViewById(R.id.experimental_updates_textview);
		TextView showDowngrades = (TextView) findViewById(R.id.show_downgrades_textview);
		TextView lastUpdateCheck = (TextView) findViewById(R.id.last_update_check);
		String pattern = getResources().getString(R.string.current_version_text);
		Preferences prefs = Preferences.getPreferences(this);
		currentVersion.setText(MessageFormat.format(pattern, SysUtils.getReadableModVersion()));
		experimentalBuilds.setText(MessageFormat.format(getResources().getString(R.string.p_display_allow_experimental_versions_title)+": {0}", Boolean.toString(prefs.allowExperimental())));
		showDowngrades.setText(MessageFormat.format(getResources().getString(R.string.p_display_older_mod_versions_title)+": {0}", Boolean.toString(prefs.showDowngrades())));
		lastUpdateCheck.setText(MessageFormat.format(getResources().getString(R.string.last_update_check_text)+": {0} {1}", DateFormat.getDateInstance().format(prefs.getLastUpdateCheck()), DateFormat.getTimeInstance().format(prefs.getLastUpdateCheck())));
		
		mdownloadedUpdateText = (TextView) findViewById(R.id.downloaded_update_found);
		mspFoundUpdates = mExistingUpdatesSpinner = (Spinner) findViewById(R.id.found_updates_list);
		mdeleteOldUpdatesButton = (Button) findViewById(R.id.delete_updates_button);
		mapplyUpdateButton = (Button) findViewById(R.id.apply_update_button);
		mseparator = findViewById(R.id.downloaded_update_found_separator);
		
		final Button selectUploadButton = (Button) findViewById(R.id.download_update_button);
		Spinner sp = mUpdatesSpinner = (Spinner) findViewById(R.id.available_updates_list);
		TextView DownloadText = (TextView) findViewById(R.id.available_updates_text);
		
		Button CheckNowUpdateChooser = (Button) findViewById(R.id.check_now_button_update_chooser);
		TextView CheckNowUpdateChooserText = (TextView) findViewById(R.id.check_now_update_chooser_text);
		CheckNowUpdateChooserText.setVisibility(View.GONE);
		CheckNowUpdateChooser.setVisibility(View.GONE);
		
		if(availableUpdates != null)
		{
			selectUploadButton.setOnClickListener(mSelectUpdateButtonListener);

			ArrayAdapter<UpdateInfo> spAdapter = new ArrayAdapter<UpdateInfo>(
					this,
					android.R.layout.simple_spinner_item,
					availableUpdates);
			spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(spAdapter);

		}
		else
		{
			selectUploadButton.setVisibility(View.GONE);
			sp.setVisibility(View.GONE);
			DownloadText.setVisibility(View.GONE);
		}
		
		if (mfilenames != null && mfilenames.size() > 0)
		{
			if(availableUpdates == null)
			{
				//Display the Check Now Button and add the Event
				//only Display when there are no Updates available
				//or this Button and the Apply Button will be there
				CheckNowUpdateChooserText.setVisibility(View.VISIBLE);
				CheckNowUpdateChooser.setVisibility(View.VISIBLE);
				CheckNowUpdateChooser.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						checkForUpdates();
					}
				});
			}
			
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
			mseparator.setVisibility(View.GONE);
			mspFoundUpdates.setVisibility(View.GONE);
			mapplyUpdateButton.setVisibility(View.GONE);
			mdownloadedUpdateText.setVisibility(View.GONE);
			mdeleteOldUpdatesButton.setVisibility(View.GONE);
		}
		
		if (availableUpdates == null && (mfilenames == null || mfilenames.size() <= 0))
			switchToNoUpdatesAvailable();
	}

	@Override
	public void updateDownloadProgress(final int downloaded, final int total, final long StartTime) {
		if(mProgressBar ==null)return;

		mProgressBar.post(new Runnable(){
			public void run() {
				if(total < 0) {
					mProgressBar.setIndeterminate(true);
				} else {
					mProgressBar.setIndeterminate(false);
					mProgressBar.setMax(total);
				}
				mProgressBar.setProgress(downloaded);

				mSpeed = (downloaded/(int)(System.currentTimeMillis() - StartTime));
				mSpeed = (mSpeed > 0) ? mSpeed : 1;
				mRemainingTime = ((total - downloaded)/mSpeed)/1000;

				mDownloadedBytesTextView.setText((downloaded/(1024*1024)) + "/" + (total/(1024*1024)) + " MB");
				mDownloadSpeedTextView.setText(Integer.toString(mSpeed) + " kb/s");
				mRemainingTimeTextView.setText(Long.toString(mRemainingTime) + " seconds");
			}
		});
	}

	@Override
	public void updateDownloadMirror(final String mirror) {
		if(mDownloadMirrorTextView == null) return;

		mDownloadMirrorTextView.post(new Runnable(){
			public void run() {
				mDownloadMirrorTextView.setText(mirror);
				mMirrorName = mirror;
			}
		});
	}

	private void showConfigActivity() {
		Intent i = new Intent(this, ConfigActivity.class);
		startActivity(i);
	}

	private void checkForUpdates() {
		new CheckForUpdatesTask(mUpdateServer, this).execute();
		Toast.makeText(this, R.string.checking_for_updates, Toast.LENGTH_SHORT).show();
		finish();
	}

	private void showAboutDialog() {
		// use OI About if available, otherwise use the simple About dialog
		if (SysUtils.isIntentAvailable(this,"org.openintents.action.SHOW_ABOUT_DIALOG")) {
			Intent intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
			startActivityForResult(intent, 0);
		} else {
			// TODO redirect user to install OI About
			Dialog dialog = new Dialog(this);
			dialog.setTitle("About");
			dialog.setContentView(R.layout.about);
			TextView mVersionName = (TextView) dialog.findViewById(R.id.version_name_about_text_view);            
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);                
				mVersionName.setText("v " + pi.versionName);
			} catch (Exception e) {
				Log.e(TAG, "Can't find version name", e);
				mVersionName.setText("v unknown");
			}
			dialog.show();
		}			
	}

	private void scanQRURL() {
		IntentIntegrator.initiateScan(this);
	}

	private void downloadRequestedUpdate(UpdateInfo ui) {
		switchToDownloadingLayout(ui);
		mUpdateDownloaderServiceIntent.putExtra(UpdateDownloaderService.KEY_REQUEST, UpdateDownloaderService.REQUEST_DOWNLOAD_UPDATE);
		mUpdateDownloaderServiceIntent.putExtra(UpdateDownloaderService.KEY_UPDATE_INFO, ui);
		startService(mUpdateDownloaderServiceIntent);
		Toast.makeText(this, R.string.downloading_update, Toast.LENGTH_SHORT).show();
	}

	private boolean deleteOldUpdates() {
		boolean success = false;
		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
			deleteDir(mUpdateFolder);
			success=true;
			Toast.makeText(this, R.string.delete_updates_success_message, Toast.LENGTH_LONG).show();
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
	
	private boolean deleteUpdate(String filename) {
		boolean success = false;
		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory()) {
			File ZIPfiletodelete = new File(mUpdateFolder + "/" + filename);
			File MD5filetodelete = new File(mUpdateFolder + "/" + filename + ".md5sum");
			if (ZIPfiletodelete.exists() && MD5filetodelete.exists())
			{
				ZIPfiletodelete.delete();
				MD5filetodelete.delete();
			}
			else
			{
				Log.e(TAG, "Update to delete not found");
				Log.e(TAG, "Zip File: "+ZIPfiletodelete.getAbsolutePath());
				Log.e(TAG, "MD5 File: "+MD5filetodelete.getAbsolutePath());
				return false;
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

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) { 	
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (null != scanResult) {
			String result = scanResult.getContents();
			if (null != result && !result.equals("") ) {
				if (result.contains("zip")) {

					UpdateInfo ui = new UpdateInfo();
					ui.updateFileUris = new LinkedList<URI>();
					try {
						ui.updateFileUris.add(new URI(result));
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String[] tmp = result.split("/");
					ui.fileName = tmp[tmp.length-1];
					ui.name = ui.fileName;

					Log.d(TAG, "Scanned QR Code: " + scanResult.getContents());
					downloadRequestedUpdate(ui);
				}
				else {
					Toast.makeText(getBaseContext(), "Scanned result is not a zip. Please check.", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
			}

		}
		else {
			Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{ 
		switch (keyCode)
		{ 
	        case KeyEvent.KEYCODE_BACK: 
	             cancelDownloading(); 
	             break; 
	        case KeyEvent.KEYCODE_HOME: 
	        	cancelDownloading();
	        	break; 
	        case KeyEvent.KEYCODE_DPAD_CENTER : 
	        	cancelDownloading();
	        	break; 
	        case KeyEvent.KEYCODE_ENDCALL : 
	        	cancelDownloading();
	        	break; 
	        case KeyEvent.ACTION_DOWN : 
	        	cancelDownloading();
	        	break; 
	        default: 
	        	break; 
        } 
        return super.onKeyDown(keyCode, event); 
   }
	
	private void cancelDownloading()
	{
		if(mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading())
		{
			mUpdateDownloaderService.cancelDownload();
			Log.i(TAG, "Download Canceled due to Event");
		}
		else
			Log.i(TAG, "Not Downloading. Proceed with the Event");
	}
}

//Class for Checking existing Updates
class UpdateFilter implements FilenameFilter
{
	public boolean accept(File dir, String name)
	{
		boolean status = false;
		File MD5 = new File(dir + "/" + name + ".md5sum");
		if (MD5.exists() && name.endsWith(".zip"))
			status = true;
		return status;
	}
}