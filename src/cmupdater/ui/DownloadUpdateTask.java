package cmupdater.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Random;

import cmupdater.ui.R;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdater.service.UpdateInfo;
import cmupdater.utils.IOUtils;
import cmupdater.utils.Preferences;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class DownloadUpdateTask extends UserTask<UpdateInfo, Integer, File>{
	
	public static DownloadUpdateTask INSTANCE;
	
	private static final String TAG = "DownloadUpdateTask";
	public static final String KEY_UPDATE_INFO = "updateInfo";
	
	//private ProgressBar mProgressBar;
	private IUpdateProcessInfo mUpdateProcessInfo;
	private HttpClient mHttpClient;
	private File mDestinationFile;
	private File mDestinationMD5File;
	//private boolean pbLenghtSet;
	private UpdateInfo mUpdateInfo;
	private Random mRandom;

	private String mMirrorName;

	private boolean mMirrorNameUpdated;
	
	public DownloadUpdateTask(IUpdateProcessInfo upi) {
		if(INSTANCE != null) throw new RuntimeException("Another instance of " + TAG + " is already running");
		INSTANCE = this;

		setIUpdateProcessInfo(upi);
		
		mHttpClient = new DefaultHttpClient();
		
		String destFileName = upi.getResources().getString(R.string.conf_update_file_name);
		mDestinationFile = new File(Environment.getExternalStorageDirectory(), destFileName);
		mDestinationMD5File = new File(Environment.getExternalStorageDirectory(), destFileName + ".md5");
		
		mRandom = new Random();
	}

	@Override
	public File doInBackground(UpdateInfo... updateInfo) {
		mUpdateInfo = updateInfo[0];
		
		File destinationFile = mDestinationFile;
		HttpClient httpClient = mHttpClient;
		
		HttpUriRequest req;
		HttpResponse response;
		List<URI> updateMirrors = updateInfo[0].updateFileUris;
		int size = updateMirrors.size();
		int start = mRandom.nextInt(size);
		URI updateURI;

		for(int i = 0; i < size; i++) {
			updateURI = updateMirrors.get((start + i)% size);
			mMirrorName = updateURI.getHost();
			mMirrorNameUpdated = false;
			//mUpdateProcessInfo.updateDownloadMirror(updateURI.getHost());
			try {
				req = new HttpGet(updateURI);
				response = httpClient.execute(req);
				int serverResponse = response.getStatusLine().getStatusCode();
				if(serverResponse != 200) {
					Log.e(TAG, "Server returned status code " + serverResponse + " trying next mirror");
				} else {
					dumpFile(response.getEntity(), destinationFile);
					
					Log.i(TAG, "Update download finished. Performing MD5 verification");
					if(!IOUtils.checkMD5(mUpdateInfo, destinationFile)) {
						throw new IOException("MD5 verification failed");
					}
					
					writeMD5(updateInfo[0].md5);
					
					//If we reach here, download & MD5 check went fine :)
					return destinationFile;
				}
			} catch (Exception ex) {
				Log.w(TAG, "An error occured while downloading the update file. Trying next mirror", ex);
			}
			if(Thread.currentThread().isInterrupted()) break;
		}
		
		Log.e(TAG, "Unable to download the update file from any mirror");
		
		if(destinationFile.exists()) destinationFile.delete();
		if(mDestinationMD5File.exists()) mDestinationMD5File.delete();
		
		return null;
	}

	@Override
	public void onProgressUpdate(Integer... values) {
		if(!mMirrorNameUpdated) {
			mUpdateProcessInfo.updateDownloadMirror(mMirrorName);
			mMirrorNameUpdated = true;
		}
		mUpdateProcessInfo.updateDownloadProgress(values[0], values[1]);
	}


	@Override
	public void onPostExecute(File result) {
		IUpdateProcessInfo upi = mUpdateProcessInfo;
		UpdateInfo ui = mUpdateInfo;
		
		upi.updateDownloadMirror(null);
		
		if(result == null) {
			Toast.makeText(upi, R.string.exception_while_downloading, Toast.LENGTH_LONG).show();
		} else {
			Resources res = upi.getResources();
			Intent i = new Intent(upi, ApplyUploadActivity.class)
								.putExtra(KEY_UPDATE_INFO, ui);

			PendingIntent contentIntent = PendingIntent.getActivity(upi, 0, i,
												PendingIntent.FLAG_CANCEL_CURRENT);
			
			Notification notification = new Notification(R.drawable.icon_notification,
												res.getString(R.string.not_update_downloaded_ticker),
												System.currentTimeMillis());
			String notificationBody = MessageFormat.format(res.getString(R.string.not_update_downloaded_ticker),
												ui.displayName, ui.displayVersion);
			notification.setLatestEventInfo(upi, res.getString(R.string.not_update_downloaded_title), notificationBody, contentIntent);
			Uri notificationRingtone = Preferences.getPreferences(upi).getConfiguredRingtone();
			if(notificationRingtone == null) {
				notification.defaults = Notification.DEFAULT_ALL;
			} else {
				notification.defaults = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;
				notification.sound = notificationRingtone;
			}
			
			NotificationManager nm = (NotificationManager) upi.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(R.string.not_update_downloaded_title, notification);
		}

		upi.switchToUpdateChooserLayout(null);
		
		INSTANCE = null;
	}
	
	@Override
	public void onCancelled() {
		onCancelled(false);
	}
	private void onCancelled(boolean finalizing) {
		if(finalizing) return;

		mUpdateProcessInfo.switchToUpdateChooserLayout(null);
		Toast.makeText(mUpdateProcessInfo, R.string.download_canceled_toast_message, Toast.LENGTH_LONG).show();
		INSTANCE = null;
	}

	public void setIUpdateProcessInfo(IUpdateProcessInfo upi) {
		this.mUpdateProcessInfo = upi;
	}

	private void dumpFile(HttpEntity entity, File destinationFile) throws IOException {
		long contentLength = entity.getContentLength();
		if(contentLength < 0) Log.w(TAG, "unable to determine the update file size");
		else Log.i(TAG, "Update size: " + (contentLength/1024) + "KB" );
		
		
		byte[] buff = new byte[64 * 1024];
		int read = 0;
		int totalDownloaded = 0;
		FileOutputStream fos = new FileOutputStream(destinationFile);
		InputStream is = entity.getContent();
		
		try {
			while(!Thread.currentThread().isInterrupted() && (read = is.read(buff)) > 0) {
				fos.write(buff, 0, read);
				totalDownloaded += read;
				publishProgress(totalDownloaded, (int)contentLength);
			}
			
			if(read > 0) {
				throw new IOException("Download Canceled");
			}

			fos.flush();
		} finally {
			//is.close();
			//entity.consumeContent();
			fos.close();
		}
	}

	private void writeMD5(String md5) throws IOException {
		
		Log.d(TAG, "Writing the calculated MD5 to disk");
		FileWriter fw = new FileWriter(mDestinationMD5File);
		try {
			fw.write(md5);
			fw.flush();
		} finally {
			fw.close();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		cancel(true);
	}
}
