package cmupdaterapp.interfaces;

import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customTypes.UpdateInfo;

import android.app.Activity;

public abstract class IMainActivity extends Activity
{
	/**
	 * Switches to the update chooser layout, providing the user with an UI to choose which update
	 * to download
	 * 
	 * @param availableUpdates
	 */
	public abstract void switchToUpdateChooserLayout(FullUpdateInfo availableUpdates);
	
	public abstract void switchToDownloadingLayout(UpdateInfo downloadingUpdate);
	
	//public abstract void updateDownloadProgress(int downloaded, int total, long StartTime);
	public abstract void updateDownloadProgress(int downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
	
	public abstract void updateDownloadMirror(String mirror);
}