package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.util.LinkedList;

import android.net.Uri;
import cmupdaterapp.ui.Log;

public class ThemeList implements Serializable
{
	private static final long serialVersionUID = 8861171977383611130L;
	
	public String name;
	public Uri url;
}

class FullThemeList implements Serializable
{
	private static final long serialVersionUID = -2577705903002871714L;
	
	private static final String TAG = "FullThemeList";

	private LinkedList<ThemeList> Themes;
	
	public FullThemeList()
	{
		Themes = new LinkedList<ThemeList>();
	}
	
	public LinkedList<ThemeList> returnFullThemeList()
	{
		return Themes;
	}
	
	public void addThemeToList(ThemeList t)
	{
		Themes.add(t);
	}
	
	public boolean removeThemeFromList(ThemeList t)
	{
		try
		{
			Themes.remove(Themes.indexOf(t));
			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception on Deleting Theme from List", e);
			return false;
		}
	}
	
	public int getThemeCount()
	{
		return Themes.size();
	}
}