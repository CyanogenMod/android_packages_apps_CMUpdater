package cmupdaterapp.customTypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import cmupdaterapp.misc.Constants;

public class CustomDrawable implements Serializable
{
	private static final long serialVersionUID = -1614979945221634143L;

	private static final DateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);

	public byte[] Picture;

	private Calendar ModifyDate;
	
	public String getModifyDate()
	{
		if (ModifyDate == null)
			ModifyDate = GregorianCalendar.getInstance();
		return df.format(ModifyDate.getTime());
	}
	
	public void setModifyDate(String s)
	{
		if (ModifyDate == null)
			ModifyDate = GregorianCalendar.getInstance();
		
		//When no Date is given, set to today
		if (s == null)
			ModifyDate = GregorianCalendar.getInstance();
		else
		{
			try
			{
				ModifyDate.setTime(df.parse(s));
			}
			catch (ParseException e)
			{
				ModifyDate = null;
			}
		}
	}

	public Drawable getPictureAsDrawable()
	{
		return Drawable.createFromStream(new ByteArrayInputStream(Picture), "Screenshot");
	}
	
	public void setPictureFromInputstream(InputStream is)
	{
		Bitmap b = BitmapFactory.decodeStream(is);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		b.compress(Bitmap.CompressFormat.PNG, 100, baos);   
		Picture = baos.toByteArray();
	}
}