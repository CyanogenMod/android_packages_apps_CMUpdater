package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class UpdateInfo implements Serializable
{
	private static final long serialVersionUID = 8671456102755862106L;
	
	//public boolean needsWipe;
	public List<String> mod;
	public List<String> board;
	public String name;
	public String version;
	public String type;
	public String branchCode;
	public String description;
	public String fileName;
	public List<URI> screenshots;
	
	public List<URI> updateFileUris;
	
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
		if (ui.mod.equals(mod)
				&& ui.board.equals(board)
				&& ui.name.equals(name)
				&& ui.version.equals(version)
				&& ui.type.equals(type)
				&& ui.branchCode.equals(branchCode)
				&& ui.description.equals(description)
				&& ui.fileName.equals(fileName)
				&& ui.screenshots.equals(screenshots))
			return true;
		return false;
	}
}