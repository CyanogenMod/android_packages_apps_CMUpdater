package cmupdaterapp.interfaces;

import android.app.Activity;

public abstract class IDownloadActivity extends Activity
{
	public abstract void updateDownloadProgress(int downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
	
	public abstract void updateDownloadMirror(String mirror);
}