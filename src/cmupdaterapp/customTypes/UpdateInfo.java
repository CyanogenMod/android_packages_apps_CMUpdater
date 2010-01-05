package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateInfo implements Parcelable, Serializable
{
	private static final long serialVersionUID = 5499890003569313403L;

	public int PrimaryKey = -1;
	public List<String> mod;
	public List<String> board;
	private String name;
	private String version;
	private String type;
	private String branchCode;
	private String description;
	private String fileName;
	public List<URI> screenshots;
	public List<URI> updateFileUris;

	/**
	 * Set Name
	 */
	public void setName(String _name)
	{
		if (_name != null)
			name = _name;
		else
			name = "";
	}
	
	/**
	 * Get Name
	 */
	public String getName() { return name; }
	
	/**
	 * Set Version
	 */
	public void setVersion(String _version)
	{
		if (_version != null)
			version = _version;
		else
			version = "";
	}
	
	/**
	 * Get Version
	 */
	public String getVersion() { return version; }
	
	/**
	 * Set Type
	 */
	public void setType(String _type)
	{
		if (_type != null)
			type = _type;
		else
			type = "";
	}
	
	/**
	 * Get Type
	 */
	public String getType() { return type; }
	
	/**
	 * Set BranchCode
	 */
	public void setBranchCode(String _branchCode)
	{
		if (_branchCode != null)
			branchCode = _branchCode;
		else
			branchCode = "";
	}
	
	/**
	 * Get BranchCode
	 */
	public String getBranchCode() { return branchCode; }
	
	/**
	 * Set Descrition
	 */
	public void setDescription(String _description)
	{
		if (_description != null)
			description = _description;
		else
			description = "";
	}
	
	/**
	 * Get Description
	 */
	public String getDescription() { return description; }
	
	/**
	 * Set Filename
	 */
	public void setFileName(String _fileName)
	{
		if (_fileName != null)
			fileName = _fileName;
		else
			fileName = "";
	}
	
	/**
	 * Get Filename
	 */
	public String getFileName() { return fileName; }
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof UpdateInfo))
			return false;
		UpdateInfo ui = (UpdateInfo)o;
		//For the old stored updates
		if (ui.screenshots == null)
			ui.screenshots = new LinkedList<URI>();
		if (screenshots == null)
			screenshots = new LinkedList<URI>();
		if (ui.mod.equals(mod)
				&& ui.board.equals(board)
				&& ui.name.equals(name)
				&& ui.version.equals(version)
				&& ui.type.equals(type)
				&& ui.branchCode.equals(branchCode)
				&& ui.description.equals(description)
				&& ui.fileName.equals(fileName)
				&& ui.screenshots.equals(screenshots)
				&& ui.PrimaryKey == PrimaryKey)
			return true;
		return false;
	}

	public UpdateInfo()
	{
		screenshots = new LinkedList<URI>();
		updateFileUris = new LinkedList<URI>();
		mod = new LinkedList<String>();
		board = new LinkedList<String>();
	}
	
	private UpdateInfo(Parcel in)
	{
		screenshots = new LinkedList<URI>();
		updateFileUris = new LinkedList<URI>();
		mod = new LinkedList<String>();
		board = new LinkedList<String>();
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>()
	{
		public UpdateInfo createFromParcel(Parcel in)
        {
        	return new UpdateInfo(in);
        }
		public UpdateInfo[] newArray(int size)
		{
			return new UpdateInfo[size];
		}
	};
	
	public int describeContents()
	{
		return 0;
	}

	public void writeToParcel(Parcel arg0, int arg1)
	{
		arg0.writeInt(PrimaryKey);
		arg0.writeList(mod);
		arg0.writeList(board);
		arg0.writeString(name);
		arg0.writeString(version);
		arg0.writeString(type);
		arg0.writeString(branchCode);
		arg0.writeString(description);
		arg0.writeString(fileName);
		arg0.writeList(screenshots);
		arg0.writeList(updateFileUris);
	}
	
	public void readFromParcel(Parcel in)
	{
		PrimaryKey = in.readInt();
		in.readList(mod, null);
		in.readList(board, null);
		name = in.readString();
		version = in.readString();
		type = in.readString();
		branchCode = in.readString();
		description = in.readString();
		fileName = in.readString();
		in.readList(screenshots, null);
		in.readList(updateFileUris, null);
	}
}