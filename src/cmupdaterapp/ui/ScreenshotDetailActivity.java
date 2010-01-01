package cmupdaterapp.ui;

import cmupdaterapp.customTypes.InvalidPictureException;
import cmupdaterapp.listadapters.ScreenshotGridViewAdapter;
import cmupdaterapp.misc.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ScreenshotDetailActivity extends Activity
{
	private int mCurrentScreenshotIndex = 0;
	private ImageView imageView;
	private TextView statusText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screenshots_detail);
		imageView = (ImageView) findViewById(R.id.image_view);
		statusText = (TextView) findViewById(R.id.status_text);
		
		Intent i = getIntent();
		Bundle b = i.getExtras();
		
		mCurrentScreenshotIndex = b.getInt(Constants.SCREENSHOTS_POSITION, 0);
		
		showScreenshot(mCurrentScreenshotIndex);

        ImageButton nextButton = (ImageButton) findViewById(R.id.next_button);
        ImageButton prevButton = (ImageButton) findViewById(R.id.previous_button);
        
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	mCurrentScreenshotIndex = (mCurrentScreenshotIndex < (ScreenshotGridViewAdapter.items.size() - 1)) ?
            			(mCurrentScreenshotIndex + 1) : (ScreenshotGridViewAdapter.items.size() - 1);
            	showScreenshot(mCurrentScreenshotIndex);
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	mCurrentScreenshotIndex = (mCurrentScreenshotIndex == 0) ? 0 : (mCurrentScreenshotIndex - 1);
            	showScreenshot(mCurrentScreenshotIndex);
            }
        });
	}
	
	private void showScreenshot(int _screenshotIndex)
	{
        try
        {
			imageView.setImageBitmap((ScreenshotGridViewAdapter.items.get(_screenshotIndex)).getBitmap());
		}
        catch (InvalidPictureException e)
        {
        	imageView.setImageResource(Constants.SCREENSHOTS_FALLBACK_IMAGE);
		}
        statusText.setText(String.format("%d/%d", _screenshotIndex + 1, ScreenshotGridViewAdapter.items.size()));
    }

	@Override
	protected void onStop()
	{
		super.onStop();
		System.gc();
	}
}