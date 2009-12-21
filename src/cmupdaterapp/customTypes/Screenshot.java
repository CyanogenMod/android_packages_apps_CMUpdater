package cmupdaterapp.customTypes;

import java.io.Serializable;
import java.net.URI;

public class Screenshot implements Serializable
{	
	private static final long serialVersionUID = 6238950270313891695L;

	public long PrimaryKey = -1;
	public int ForeignThemeListKey;
	public URI url;
	public CustomDrawable Screenshot;
	
	public Screenshot()
	{
		Screenshot = new CustomDrawable();
	}
}