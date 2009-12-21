package cmupdaterapp.utils;

import java.io.InputStream;
import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdaterapp.customTypes.CustomDrawable;
import cmupdaterapp.misc.Log;

public class ImageUtilities
{
    private static final String TAG = "ImageUtilities";

    public static CustomDrawable load(String url, long lastModifiedInMillis)
    {
    	CustomDrawable cd = new CustomDrawable();

    	HttpClient httpCrap = new DefaultHttpClient();

    	final HttpGet get = new HttpGet(url);

    	HttpEntity entity = null;

    	try
    	{
    		final HttpResponse response = httpCrap.execute(get);
    		
    		//Set last ModifyDate
    		final Header header = response.getFirstHeader("Last-Modified");
    		if (header != null)
    			cd.setModifyDate(header.getValue());
    		//If null set to today
    		else
    			cd.setModifyDate(null);
    		
    		//Do not Download if not changed
    		if (lastModifiedInMillis == cd.getModifyDateAsMillis())
    			return null;
    		
    		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
    		{
    			entity = response.getEntity();

    			InputStream in = null;

    			try
    			{    			
    				in = entity.getContent();
    				cd.setPictureFromInputstream(in);
    			}
    			catch (IOException e)
    			{
    				Log.e(TAG, "Could not load image from " + url, e);
    			}
    			finally
    			{
    				if (in != null) in.close();
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
    	return cd;
    }
}