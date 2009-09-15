package cmupdaterapp.interfaces;

import cmupdaterapp.customTypes.FullUpdateInfo;

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
}