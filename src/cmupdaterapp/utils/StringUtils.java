package cmupdaterapp.utils;

import cmupdaterapp.misc.Log;

public class StringUtils
{
	private static final String TAG = "StringUtils";

	/**
	 * Converts a String array to an String, joined by the Seperator
	 * 
	 * @param items
	 *            The String Array to Join
	 * @param seperator
	 *            The Seperator used to join the String
	 * @return The Joined String
	 */
	public static String arrayToString(String[] items, String seperator)
	{
		if ((items == null) || (items.length == 0))
		{
			return "";
		}
		else
		{
			StringBuffer buffer = new StringBuffer(items[0]);
			for (int i = 1; i < items.length; i++)
			{
				buffer.append(seperator);
				buffer.append(items[i]);
			}
			return buffer.toString();
		}
	}

	/**
	 * Compare two versions. Will strip off any alphabets in the version number
	 * and then do a number comparison
	 * 
	 * @param newVersion
	 *            new version to be compared
	 * @param oldVersion
	 *            old version to be compared
	 * @return true if newVersion is greater then oldVersion,
	 * false on exceptions or newVersion=oldVersion and newVersion is lower then oldVersion
	 */
	public static boolean compareVersions(String newVersion, String oldVersion)
	{
		String sNewVersion = newVersion.replaceAll("[^0-9]", "");
		String sOldVersion = oldVersion.replaceAll("[^0-9]", "");
		Log.d(TAG, "sNewVersion:"+sNewVersion+":::sOldVersion:"+sOldVersion);
		long lNewVersion;
		long lOldVersion;
		try
		{
			lNewVersion = Long.parseLong(sNewVersion);
			lOldVersion = Long.parseLong(sOldVersion);
		}
		catch(NumberFormatException ex)
		{
			Log.e(TAG, "Exception on Parsing Version String. newVersion: "+newVersion+" oldVersion: "+oldVersion);
			return false;
		}
		if (lNewVersion == lOldVersion)
			return false;
		else if (lNewVersion > lOldVersion)
			return true;
		else
			return false;
	}
}