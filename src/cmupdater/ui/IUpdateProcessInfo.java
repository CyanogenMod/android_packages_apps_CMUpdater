package cmupdater.ui;

import java.util.List;

import cmupdater.service.UpdateInfo;

import android.app.Activity;

public abstract class IUpdateProcessInfo extends Activity {
	
	/**
	 * Switches to the update chooser layout, providing the user with an UI to choose which update
	 * to download
	 * 
	 * @param availableUpdates
	 */
	public abstract void switchToUpdateChooserLayout(List<UpdateInfo> availableUpdates);
	
	/**
	 * Switches to the update chooser layout, showing the option to apply the provided update
	 * @param downloadedUpdate
	 */
	//abstract void switchToUpdateChooserLayout(UpdateInfo downloadedUpdate);
	
	public abstract void switchToNoUpdatesAvailable();
	
	public abstract void switchToDownloadingLayout(UpdateInfo downloadingUpdate);
	
	public abstract void updateDownloadProgress(int downloaded, int total, long StartTime);
	
	public abstract void updateDownloadMirror(String mirror);
}
