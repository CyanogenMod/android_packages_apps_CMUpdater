package cmupdaterapp.tasks;

import java.net.URI;

import android.os.AsyncTask;
import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.listadapters.ScreenshotGridViewAdapter;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.ScreenshotActivity;
import cmupdaterapp.utils.ImageUtilities;

public class DownloadImageTask extends AsyncTask<UpdateInfo, Void, Boolean>
{
	private static final String TAG = "DownloadImageTask";

	@Override
	protected Boolean doInBackground(UpdateInfo... params)
	{
		DbAdapter db = new DbAdapter();

		UpdateInfo ui = params[0];
		
		try
		{
			db.open();
			String[] PrimaryKeys = new String[ui.screenshots.size()];
			boolean ScreenFound = false;
			boolean NeedsUpdate = false;
			int counter = 0;
			for (URI uri : ui.screenshots)
			{
				Log.d(TAG, "Started Downloading Image number " + counter);
				Screenshot screeni = new Screenshot();
				//Add to DB if not there, otherwise get the DatabaseObject
				screeni = db.ScreenshotExists(ui.PrimaryKey, uri.toString());
				if (screeni.PrimaryKey != -1)
				{
					ScreenFound = true;
				}
				Screenshot s = ImageUtilities.load(uri.toString(), screeni.getModifyDateAsMillis(), screeni.PrimaryKey, ui.PrimaryKey);
				//Null when Modifydate not changed
				if (s != null)
				{
					NeedsUpdate = true;
					screeni = s;
				}
				//When not found insert in DB
				if (!ScreenFound)
				{
					screeni = s;
					screeni.ForeignThemeListKey = ui.PrimaryKey;
					screeni.url = uri;
					screeni.PrimaryKey = db.insertScreenshot(screeni);
				}
				//Only Update if Screenshot was there
				else if (ScreenFound && NeedsUpdate)
				{
					db.updateScreenshot(screeni.PrimaryKey, screeni);
				}
				ScreenshotGridViewAdapter.items.add(screeni);
				//Calls onProgressUpdate (runs in UI Thread)
				publishProgress();
				PrimaryKeys[counter] = Long.toString(screeni.PrimaryKey);
				counter++;
				ScreenFound = false;
				NeedsUpdate = false;
			}
			
			//Delete old Screenshots from DB
			db.removeScreenshotExcept(ui.PrimaryKey, PrimaryKeys);
		}
		finally
		{
			if(db != null)
				db.close();
		}
		return true;
	}

	@Override
	protected void onProgressUpdate(Void... unused)
	{
		//This runs in the UI Thread
		ScreenshotActivity.imageAdapter.notifyDataSetChanged();
	}
}