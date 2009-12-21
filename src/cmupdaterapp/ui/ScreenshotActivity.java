package cmupdaterapp.ui;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.database.DbAdapter;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
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
		db.open();
		for (URI s : ui.screenshots)
		{
			Screenshot screeni = new Screenshot();
			screeni.ForeignThemeListKey = ui.PrimaryKey;
			screeni.url = s;
			screeni.Screenshot = ImageUtilities.load(s.toString());
			db.insertScreenshot(screeni);
			ss.add(screeni);
		}
		db.close();
		ImageView image1 = (ImageView)findViewById(R.id.image1);
		ImageView image2 = (ImageView)findViewById(R.id.image2);
		ImageView image3 = (ImageView)findViewById(R.id.image3);
		image1.setImageDrawable(ss.get(0).Screenshot.getPictureAsDrawable());
		image2.setImageDrawable(ss.get(0).Screenshot.getPictureAsDrawable());
		image3.setImageDrawable(ss.get(0).Screenshot.getPictureAsDrawable());
	}
}