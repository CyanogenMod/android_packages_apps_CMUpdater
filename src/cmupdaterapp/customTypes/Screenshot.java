package cmupdaterapp.customTypes;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.drawable.Drawable;

public class Screenshot implements Serializable
{	
	private static final long serialVersionUID = 6238950270313891695L;

	private static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mi:ss");
	
	public int PrimaryKey;
	public int ForeignThemeListKey;
	public URI url;
	public Date ModifyDate;
	public byte[] Picture;
	
	public Drawable getPictureAsDrawable()
	{
		return Drawable.createFromStream(new ByteArrayInputStream(Picture), "Screenshot");
	}
	
	public static Date StringToDate(String s)
	{
		try
		{
			return df.parse(s);
		}
		catch (ParseException e)
		{
			return null;
		}
	}
	
	public static String DateToString(Date d)
	{
		return df.format(d);
	}
}