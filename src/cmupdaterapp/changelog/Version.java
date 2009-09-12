package cmupdaterapp.changelog;

import java.util.LinkedList;
import java.util.List;

public class Version
{
	public String Version;
	public List<String> ChangeLogText;
	Version()
	{
		Version = "";
		ChangeLogText = new LinkedList<String>();
	}
}