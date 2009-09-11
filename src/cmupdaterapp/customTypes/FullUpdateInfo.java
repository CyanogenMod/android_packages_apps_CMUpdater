package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.util.LinkedList;

import cmupdaterapp.customTypes.UpdateInfo;

public class FullUpdateInfo implements Serializable
{
	private static final long serialVersionUID = -2719765435535504941L;
	
	public LinkedList<UpdateInfo> roms;
	public LinkedList<UpdateInfo> themes;
	
	public FullUpdateInfo()
	{
		roms = new LinkedList<UpdateInfo>();
		themes = new LinkedList<UpdateInfo>();
	}
	
	@Override
	public String toString()
	{
		return "FullUpdateInfo";
	}
	
	public int getRomCount()
	{
		return roms.size();
	}
	
	public int getThemeCount()
	{
		return themes.size();
	}
	
	public int getUpdateCount()
	{
		return themes.size() + roms.size();
	}
}