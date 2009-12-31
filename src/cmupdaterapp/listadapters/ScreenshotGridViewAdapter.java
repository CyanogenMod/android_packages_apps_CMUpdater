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

public class ScreenshotGridViewAdapter extends BaseAdapter
{
    private Context mContext;
    private int length;
    
    public List<Screenshot> items = new LinkedList<Screenshot>();

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
    	return getView(position, null, null);
    }

    public long getItemId(int position)
    {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ImageView imageView;
        if (convertView == null) // if it's not recycled, initialize some attributes
        {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        }
        else
        {
            imageView = (ImageView) convertView;
        }

        //When there is an invalid url or image, set to Fallbackimage
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
}