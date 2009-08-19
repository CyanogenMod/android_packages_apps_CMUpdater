package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class SysUtils
{
	private static final String TAG = "<CM-Updater> SysUtils";
	private static final String SYS_PROP_MOD_VERSION = "ro.modversion";
	private static final int PROP_MOD_VERSION_SKIP_CHARS = 12;

	/**
	 * Returns (if available) a human-readable string containing current mod version
	 * 
	 * @return a human-readable string containing current mod version
	 */
	public static String getReadableModVersion()
	{
		String modVer = getSystemProperty(SYS_PROP_MOD_VERSION);
		
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
        	ex.printStackTrace();
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
					e.printStackTrace();
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
		String modVersion = getSystemProperty(SYS_PROP_MOD_VERSION);
		
		if(modVersion == null || modVersion.length() < PROP_MOD_VERSION_SKIP_CHARS) return new int[0];
		
		String version[] = modVersion.substring(PROP_MOD_VERSION_SKIP_CHARS).split("\\.");
		
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
		
	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action)
	{
	    final PackageManager packageManager = context.getPackageManager();
	    final Intent intent = new Intent(action);
	    List<ResolveInfo> list =
	            packageManager.queryIntentActivities(intent,
	                    PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
}