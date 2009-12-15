package cmupdaterapp.ui;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
		List<Drawable> d = new LinkedList<Drawable>();
		for (URI s : ui.screenshots)
		{
			Drawable temp = ImageUtilities.load(s.toString());
			if (temp != null)
				d.add(temp);
		}
		ImageView image1 = (ImageView)findViewById(R.id.image1);
		ImageView image2 = (ImageView)findViewById(R.id.image2);
		ImageView image3 = (ImageView)findViewById(R.id.image3);
		image1.setImageDrawable(d.get(0));
		image2.setImageDrawable(d.get(0));
		image3.setImageDrawable(d.get(0));
	}
}