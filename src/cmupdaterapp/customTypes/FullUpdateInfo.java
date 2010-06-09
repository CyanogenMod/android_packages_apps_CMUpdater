package cmupdaterapp.customTypes;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.LinkedList;

public class FullUpdateInfo implements Parcelable, Serializable
{
	private static final long serialVersionUID = -2719765435535504941L;

	public LinkedList<UpdateInfo> roms;
	public LinkedList<UpdateInfo> incrementalRoms;
	public LinkedList<UpdateInfo> themes;

	public FullUpdateInfo()
	{
		roms = new LinkedList<UpdateInfo>();
		incrementalRoms = new LinkedList<UpdateInfo>();
		themes = new LinkedList<UpdateInfo>();
	}

	private FullUpdateInfo(Parcel in)
	{
		roms = new LinkedList<UpdateInfo>();
		incrementalRoms = new LinkedList<UpdateInfo>();
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
		arg0.writeList(incrementalRoms);
		arg0.writeList(themes);
	}

	void readFromParcel(Parcel in)
	{
		in.readList(roms, null);
		in.readList(incrementalRoms, null);
		in.readList(themes, null);
	}

	@Override
	public String toString()
	{
		return "FullUpdateInfo";
	}

	public int getRomCount()
	{
		if (roms == null) return 0;
		return roms.size();
	}
	
	public int getIncrementalRomCount()
	{
		if (incrementalRoms == null) return 0;
		return incrementalRoms.size();
	}

	public int getThemeCount()
	{
		if (themes == null) return 0;
		return themes.size();
	}

	public int getUpdateCount()
	{
		int themessize = themes == null ? 0 : themes.size();
		int romssize = roms == null ? 0 : roms.size();
		int incrementalromssize = incrementalRoms == null ? 0 : incrementalRoms.size();
		return themessize + romssize + incrementalromssize;
	}
}