package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.util.LinkedList;

import android.os.Parcel;
import android.os.Parcelable;

import cmupdaterapp.customTypes.UpdateInfo;

public class FullUpdateInfo implements Parcelable, Serializable
{
	private static final long serialVersionUID = -2719765435535504941L;
	
	public LinkedList<UpdateInfo> roms;
	public LinkedList<UpdateInfo> themes;
	
	public FullUpdateInfo()
	{
		roms = new LinkedList<UpdateInfo>();
		themes = new LinkedList<UpdateInfo>();
	}
	
	private FullUpdateInfo(Parcel in)
	{
		roms = new LinkedList<UpdateInfo>();
		themes = new LinkedList<UpdateInfo>();
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<FullUpdateInfo> CREATOR = new Parcelable.Creator<FullUpdateInfo>()
	{
		public FullUpdateInfo createFromParcel(Parcel in)
        {
        	return new FullUpdateInfo(in);
        }
		public FullUpdateInfo[] newArray(int size)
		{
			return new FullUpdateInfo[size];
		}
	};
	
	public int describeContents()
	{
		return 0;
	}

	public void writeToParcel(Parcel arg0, int arg1)
	{
		arg0.writeList(roms);
		arg0.writeList(themes);
	}
	
	public void readFromParcel(Parcel in)
	{
		in.readList(roms, null);
		in.readList(themes, null);
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