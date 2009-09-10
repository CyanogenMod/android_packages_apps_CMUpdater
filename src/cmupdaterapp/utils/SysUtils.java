package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmupdaterapp.ui.Constants;
import android.util.Log;

public class SysUtils
{
	private static final String TAG = "<CM-Updater> SysUtils";

	/**
	 * Returns (if available) a human-readable string containing current mod version
	 * 
	 * @return a human-readable string containing current mod version
	 */
	public static String getReadableModVersion()
	{
		String modVer = getSystemProperty(Constants.SYS_PROP_MOD_VERSION);
		
		return (modVer == null || modVer.length() == 0 ? "Unknown" : modVer);
	}
	
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
	
	/**
	 * 
	 * @return the system mod version or a int[0] if the property is not found or not parseable
	 */
	public static int[] getSystemModVersion()
	{
		String modVersion = getSystemProperty(Constants.SYS_PROP_MOD_VERSION);
		
		if(modVersion == null || modVersion.length() < Constants.PROP_MOD_VERSION_SKIP_CHARS) return new int[0];
		
		String version[] = modVersion.substring(Constants.PROP_MOD_VERSION_SKIP_CHARS).split("\\.");
		
		int[] retValue = new int[version.length];
		try
		{
			for(int i = 0; i < version.length; i++)
			{
				retValue[i] = Integer.parseInt(version[i]);
			}
		}
		catch (NumberFormatException e)
		{
			return new int[0];
		}

		return retValue;
	}
}