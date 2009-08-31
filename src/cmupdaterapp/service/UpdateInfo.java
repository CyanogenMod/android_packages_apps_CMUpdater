package cmupdaterapp.service;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class UpdateInfo implements Serializable
{
	private static final long serialVersionUID = 8671456102755862106L;
	
	public boolean needsWipe;
	public String mod;
	public List<String> board;
	public String name;
	public String displayVersion;
	public String type;
	public String branchCode;
	public String description;
	public String fileName;
	
	public List<URI> updateFileUris;
	@Override
	public String toString()
	{
		return name;
	}
}