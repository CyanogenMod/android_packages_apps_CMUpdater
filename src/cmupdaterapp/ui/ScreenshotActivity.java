package cmupdaterapp.ui;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import cmupdaterapp.utils.ImageUtilities;

public class ScreenshotActivity extends Activity
{
	private DbAdapter db;
	private UpdateInfo ui;
	private GridView gridview;
	private ScreenshotGridViewAdapter imageAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screenshots);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		ui = (UpdateInfo) b.get(Constants.SCREENSHOTS_UPDATE);
		
		gridview = (GridView) findViewById(R.id.gridview);
		imageAdapter = new ScreenshotGridViewAdapter(this, ui.screenshots.size());
	    gridview.setAdapter(imageAdapter);
	    // Set a item click listener, and just Toast the clicked position
        gridview.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                Toast.makeText(ScreenshotActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(ScreenshotActivity.this, ScreenshotDetailActivity.class);
        		startActivity(i);
            }
        });

		db = new DbAdapter();
		
		List<Screenshot> ss = new LinkedList<Screenshot>();
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
				Screenshot temp = db.ScreenshotExists(ui.PrimaryKey, uri.toString());
				if (temp.PrimaryKey != -1)
				{
					ScreenFound = true;
					screeni = temp;
				}
				Screenshot s = ImageUtilities.load(uri.toString(), screeni.getModifyDateAsMillis(), ui.PrimaryKey);
				//Null when Modifydate not changed
				if (s != null)
				{
					NeedsUpdate = true;
					screeni = s;
				}
				//When not found insert in DB
				if (!ScreenFound)
				{
					screeni.ForeignThemeListKey = ui.PrimaryKey;
					screeni = s;
					screeni.url = uri;
					screeni.PrimaryKey = db.insertScreenshot(screeni);
				}
				//Only Update if Screenshot was there
				else if (ScreenFound && NeedsUpdate)
				{
					db.updateScreenshot(screeni.PrimaryKey, screeni);
				}
				ss.add(screeni);
				PrimaryKeys[counter] = Long.toString(screeni.PrimaryKey);
				counter++;
				ScreenFound = false;
				NeedsUpdate = false;
				temp = null;
				screeni = null;
			}
			
			//Delete old Screenshots from DB
			db.removeScreenshotExcept(ui.PrimaryKey, PrimaryKeys);
		}
		finally
		{
			if(db != null)
				db.close();
		}
		imageAdapter.items = ss;
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		System.gc();

	}
}