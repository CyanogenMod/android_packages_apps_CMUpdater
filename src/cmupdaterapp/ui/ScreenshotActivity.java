package cmupdaterapp.ui;

import java.net.URI;

import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.listadapters.ScreenshotGridViewAdapter;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import cmupdaterapp.utils.ImageUtilities;

public class ScreenshotActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screenshots);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		final UpdateInfo ui = (UpdateInfo) b.get(Constants.SCREENSHOTS_UPDATE);
		
		for(Screenshot s : ScreenshotGridViewAdapter.items)
		{
			s.DestroyImage();
		}
		ScreenshotGridViewAdapter.items.clear();
		
		GridView gridview = (GridView) findViewById(R.id.gridview);
		ScreenshotGridViewAdapter imageAdapter = new ScreenshotGridViewAdapter(this, ui.screenshots.size());
	    gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                Intent i = new Intent(ScreenshotActivity.this, ScreenshotDetailActivity.class);
                i.putExtra(Constants.SCREENSHOTS_POSITION, position);
        		startActivity(i);
            }
        });

        DbAdapter db = new DbAdapter();

		try
		{
			db.open();
			String[] PrimaryKeys = new String[ui.screenshots.size()];
			boolean ScreenFound = false;
			boolean NeedsUpdate = false;
			int counter = 0;
			for (URI uri : ui.screenshots)
			{
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
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		System.gc();
	}
}