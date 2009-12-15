package cmupdaterapp.utils;

import android.graphics.drawable.Drawable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdaterapp.misc.Log;

public class ImageUtilities
{
    private static final String TAG = "ImageUtilities";

    public static Drawable load(String url)
    {
    	Drawable d = null;

    	HttpClient httpCrap = new DefaultHttpClient();

    	final HttpGet get = new HttpGet(url);

    	HttpEntity entity = null;

    	try
    	{
    		final HttpResponse response = httpCrap.execute(get);
    		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
    		{
    			entity = response.getEntity();

    			InputStream in = null;
    			OutputStream out = null;

    			try
    			{
    				in = entity.getContent();
    				d = Drawable.createFromStream(in, "Image");
    			}
    			catch (IOException e)
    			{
    				Log.e(TAG, "Could not load image from " + url, e);
    			}
    			finally
    			{
    				if (in != null) in.close();
    				if (out != null) out.close();
    			}
    		}
    	}
    	catch (IOException e)
    	{
    		Log.e(TAG, "Could not load image from " + url, e);
    	}
    	finally
    	{
    		if (entity != null)
    		{
    			try
    			{
    				entity.consumeContent();
    			}
    			catch (IOException e)
    			{
    				Log.e(TAG, "Could not load image from " + url, e);
    			}
    		}
    	}
    	return d;
    }
}