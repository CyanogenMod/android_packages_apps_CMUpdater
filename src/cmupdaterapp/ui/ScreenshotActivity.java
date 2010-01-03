package cmupdaterapp.ui;

import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.listadapters.ScreenshotGridViewAdapter;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import cmupdaterapp.tasks.DownloadImageTask;

public class ScreenshotActivity extends Activity
{	
	private UpdateInfo ui;
	public static ScreenshotGridViewAdapter imageAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screenshots);
		Intent i = getIntent();
		Bundle b = i.getExtras();
		ui = (UpdateInfo) b.get(Constants.SCREENSHOTS_UPDATE);
		
		for(Screenshot s : ScreenshotGridViewAdapter.items)
		{
			s.DestroyImage();
		}
		ScreenshotGridViewAdapter.items.clear();
		
		GridView gridview = (GridView) findViewById(R.id.gridview);
		imageAdapter = new ScreenshotGridViewAdapter(this, ui.screenshots.size());

	    gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
            	//Only start the Activity, when the Image is loaded
            	if (ScreenshotGridViewAdapter.items.size() > position)
            	{
            		Intent i = new Intent(ScreenshotActivity.this, ScreenshotDetailActivity.class);
                    i.putExtra(Constants.SCREENSHOTS_POSITION, position);
            		startActivity(i);
            	}
            	else { }
            }
        });
        
        //In onCreate, cause when pressing back from Detail, the old Screenshots remain in the List
        new DownloadImageTask().execute(ui);
	}
}