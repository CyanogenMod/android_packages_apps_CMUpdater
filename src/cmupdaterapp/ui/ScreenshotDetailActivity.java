package cmupdaterapp.ui;

import cmupdaterapp.customExceptions.InvalidPictureException;
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
	private ImageButton nextButton;
	private ImageButton prevButton;
	private int maxIndexSize;

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

		maxIndexSize = ScreenshotGridViewAdapter.items.size() - 1;

        nextButton = (ImageButton) findViewById(R.id.next_button);
        prevButton = (ImageButton) findViewById(R.id.previous_button);

        nextButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	mCurrentScreenshotIndex++;
            	showScreenshot();
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	mCurrentScreenshotIndex--;
            	showScreenshot();
            }
        });
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		showScreenshot();
	}

	private void showScreenshot()
	{
		//Reenable the Buttons, they will be removed in the following ifs
		nextButton.setVisibility(View.VISIBLE);
		prevButton.setVisibility(View.VISIBLE);
		if (mCurrentScreenshotIndex >= maxIndexSize)
		{
			mCurrentScreenshotIndex = maxIndexSize;
			nextButton.setVisibility(View.GONE);
		}
		if (mCurrentScreenshotIndex <= 0)
		{
			mCurrentScreenshotIndex = 0;
			prevButton.setVisibility(View.GONE);
		}

        try
        {
			imageView.setImageBitmap((ScreenshotGridViewAdapter.items.get(mCurrentScreenshotIndex)).getBitmap());
		}
        catch (InvalidPictureException e)
        {
        	imageView.setImageResource(Constants.SCREENSHOTS_FALLBACK_IMAGE);
		}
        //Image not yet loaded
        catch (IndexOutOfBoundsException e)
        {
        	imageView.setImageResource(Constants.SCREENSHOTS_FALLBACK_IMAGE);
		}
        statusText.setText(String.format("%d/%d", mCurrentScreenshotIndex + 1, maxIndexSize + 1));
    }

	@Override
	protected void onStop()
	{
		super.onStop();
		System.gc();
	}
}