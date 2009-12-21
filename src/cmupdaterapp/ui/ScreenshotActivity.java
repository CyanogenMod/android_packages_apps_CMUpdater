package cmupdaterapp.ui;

import java.net.URI;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import cmupdaterapp.customTypes.CustomDrawable;
import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
		UpdateInfo ui = (UpdateInfo) b.get(Constants.SCREENSHOTS_UPDATE);
		List<Screenshot> ss = new LinkedList<Screenshot>();
		DbAdapter db = new DbAdapter();
		try
		{
			db.open();
			List<Screenshot> dbScreens = db.getAllScreenshotsForTheme(ui.PrimaryKey);
			String[] PrimaryKeys = new String[ui.screenshots.size()];
			if (dbScreens == null)
				dbScreens = new LinkedList<Screenshot>();
			boolean ScreenFound = false;
			boolean NeedsUpdate = false;
			int counter = 0;
			for (URI s : ui.screenshots)
			{
				Screenshot screeni = new Screenshot();
				//Add to DB if not there, otherwise get the DatabaseObject
				Screenshot temp = db.ScreenshotExists(ui.PrimaryKey, s.toString());
				if (temp.PrimaryKey != -1)
				{
					ScreenFound = true;
					screeni = temp;
				}
				CustomDrawable cd = ImageUtilities.load(s.toString(), screeni.Screenshot.ModifyDate);
				//Null when Modifydate not changed
				if (cd != null)
				{
					NeedsUpdate = true;
					screeni.Screenshot = cd;
				}
				//When not found insert in DB
				if (!ScreenFound)
				{
					screeni.ForeignThemeListKey = ui.PrimaryKey;
					screeni.Screenshot = cd;
					screeni.url = s;
					//Instantiate Modifydate with today, if not found in DB
					screeni.Screenshot.ModifyDate = Calendar.getInstance();
					db.insertScreenshot(screeni);
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
			}
			
			//Delete old Screenshots from DB
			db.removeScreenshotExcept(ui.PrimaryKey, PrimaryKeys);
		}
		finally
		{
			if(db != null)
				db.close();
		}

		LinearLayout main = (LinearLayout) findViewById(R.id.ScreenshotLinearMain);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//Foreach Screenshot
		for (Screenshot s:ss)
		{
			if (s == null)
				continue;
			ImageView iv = new ImageView(this);
			iv.setLayoutParams(lp);
			iv.setImageDrawable(s.Screenshot.getPictureAsDrawable());
			main.addView(iv);
			//Horizontal Line
			View ruler = new View(this);
			ruler.setBackgroundColor(Color.WHITE);
			main.addView(ruler, new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 1));
		}
	}
}