package cmupdaterapp.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filename Filter for getting only Files that matches the Given Extensions 
 * Extensions can be split with |
 * Example: .zip|.md5sum  
 *
 * @param  Extensions  String with supported Extensions. Split multiple Extensions with |
 * @return      true when file Matches Extension, otherwise false
 */
public class UpdateFilter implements FilenameFilter
{
	private String[] mExtension;
	
	public UpdateFilter(String Extensions)
	{
		mExtension = Extensions.split("\\|");
	}
	
	public boolean accept(File dir, String name)
	{
		for (String Ext : mExtension)
		{
			if (name.endsWith(Ext))
				return true;
		}
		return false;
	}
}