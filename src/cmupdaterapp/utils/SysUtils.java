package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;

public class SysUtils
{
	private static final String TAG = "SysUtils";

	/**
	 * Returns (if available) a human-readable string containing current mod version
	 * 
	 * @return a human-readable string containing current mod version
	 */
	public static String getModVersion()
	{
		String modVer = getSystemProperty(Constants.SYS_PROP_MOD_VERSION);
		
		return (modVer == null || modVer.length() == 0 ? "Unknown" : modVer);
	}
	
	/**
	 * Returns a SystemProperty
	 * 
	 * @param propName
	 *            The Property to retrieve
	 * @return The Property, or NULL if not found
	 */
	public static String getSystemProperty(String propName)
	{
		String line;
		BufferedReader input = null;
        try
        {
        	Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        }
        catch (IOException ex)
        {
        	Log.e(TAG, "Unable to read sysprop " + propName, ex);
        	return null;
        }
        finally
        {
        	if(input != null)
        	{
				try
				{
					input.close();
				}
				catch (IOException e)
				{
					Log.e(TAG, "Exception while closing InputStream", e);
				}
        	}
        }
        return line;
	}
}