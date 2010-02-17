package cmupdaterapp.utils;

import java.util.ArrayList;

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
	 * Compare two versions.
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
		Log.d(TAG, "NewVersion: " + newVersion + ", oldVersion: " + oldVersion);
		if (newVersion.equals(oldVersion))
			return false;
		
		//Replace all - by . So a CyanogenMod-4.5.4-r2 will be a CyanogenMod.4.5.4.r2 
		newVersion = newVersion.replaceAll("-","\\.");
		oldVersion = oldVersion.replaceAll("-","\\.");
		
		String[] sNewVersion = newVersion.split("\\.");
		String[] sOldVersion = oldVersion.split("\\.");
	
		ArrayList<String> newVersionArray = new ArrayList<String>();
		ArrayList<String> oldVersionArray = new ArrayList<String>();
		
		for (String s : sNewVersion)
		{
			newVersionArray.add(s);
		}
		for (String s : sOldVersion)
		{
			oldVersionArray.add(s);
		}
		
		//Make the 2 Arrays the Same size filling it with 0. So Version 2 compared to 2.1 will be 2.0 to 2.1
		if (newVersionArray.size() > oldVersionArray.size())
		{
			int difference = newVersionArray.size() - oldVersionArray.size();
			for(int i = 0; i < difference; i++)
			{
				oldVersionArray.add("0");
			}
		}
		else
		{
			int difference = oldVersionArray.size() - newVersionArray.size();
			for(int i = 0; i < difference; i++)
			{
				newVersionArray.add("0");
			}
		}

	
		int i = 0;
		for(String s : newVersionArray)
		{
			String old = oldVersionArray.get(i);
			//First try an Int Compare, if its a string, make a string compare
			try
			{
				int newVer = Integer.parseInt(s);
				int oldVer = Integer.parseInt(old);
				if (newVer > oldVer)
					return true;
				else if (newVer < oldVer)
					return false;
				else
					i++;
			}
			catch (Exception ex)
			{
				//If we reach here, we have to string compare cause the version contains strings
				int temp = s.compareToIgnoreCase(old);
				if (temp < 0)
					return false;
				else if (temp > 0)
					return true;
				else
					//its the same value so continue
					i++;
			}
		}
		//Its Bigger so return true
		return true;
	}
}