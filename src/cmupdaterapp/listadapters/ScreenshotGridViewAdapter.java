package cmupdaterapp.listadapters;

import java.util.LinkedList;
import java.util.List;

import cmupdaterapp.customTypes.InvalidPictureException;
import cmupdaterapp.customTypes.Screenshot;
import cmupdaterapp.misc.Constants;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ScreenshotGridViewAdapter extends BaseAdapter
{
	public final static String TAG_IMAGE = "Image";
	public final static String TAG_PROGRESS = "Progress";
	
    private Context mContext;
    private int length;
    
    public static List<Screenshot> items = new LinkedList<Screenshot>();

    public ScreenshotGridViewAdapter(Context c, int numberOfItems)
    {
    	mContext = c;
    	length = numberOfItems;
    }

    public int getCount()
    {
    	return length;
    }
    
    public Object getItem(int position)
    {
    	return position;
    }

    public long getItemId(int position)
    {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	boolean ImageLoaded = ScreenshotGridViewAdapter.items.size() > position;
    	
        ImageView imageView = null;
        ProgressBar pg = null;
        if (convertView == null) // if it's not recycled, initialize some attributes
        {
        	if (ImageLoaded)
        	{
	            imageView = createNewImageView();
        	}
        	else
        	{
        		pg = new ProgressBar(mContext);
        		pg.setIndeterminate(true);
        		pg.setTag(TAG_PROGRESS);
        		pg.setLayoutParams(new GridView.LayoutParams(85, 85));
        		pg.setPadding(8, 8, 8, 8);
        		pg.setVisibility(View.VISIBLE);
        	}
        }
        else
        {
        	//If its a Progressbar, and the Image is loaded, we have to Convert it to an ImageView
        	if (ImageLoaded && ((String)convertView.getTag()).equals(TAG_PROGRESS))
        		imageView = createNewImageView();
        	//If its not loaded and the existing View is a ProgressBar, leave it
        	else if (!ImageLoaded && ((String)convertView.getTag()).equals(TAG_PROGRESS))
        		pg = (ProgressBar) convertView;
        	//Otherwise its an ImageView
        	else
        		imageView = (ImageView) convertView;
        }

        if (!ImageLoaded)
        	return pg;

        try
        {
        	imageView.setImageBitmap(items.get(position).getBitmap());
        }
        catch (InvalidPictureException ex)
        {
        	imageView.setImageResource(Constants.SCREENSHOTS_FALLBACK_IMAGE);
        }
        return imageView;
    }

	private ImageView createNewImageView()
	{
		ImageView imageView = new ImageView(mContext);
		imageView.setTag(TAG_IMAGE);
		imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setPadding(8, 8, 8, 8);
		return imageView;
	}
}