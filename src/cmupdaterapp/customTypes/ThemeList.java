package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.net.URI;

public class ThemeList implements Serializable
{
	private static final long serialVersionUID = 8861171977383611130L;
	
	public int PrimaryKey;
	public String name;
	public URI url;
	public boolean enabled;
}