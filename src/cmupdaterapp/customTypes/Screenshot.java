package cmupdaterapp.customTypes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import cmupdaterapp.customExceptions.InvalidPictureException;
import cmupdaterapp.misc.Constants;

public class Screenshot implements Serializable
{	
	private static final long serialVersionUID = 6238950270313891695L;

	public long PrimaryKey = -1;
	public int ForeignThemeListKey;
	public URI url;
	private static final DateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
	private Bitmap Picture;
	private Calendar ModifyDate;

	//Modifydate
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
	
	public long getModifyDateAsMillis()
	{
		if (ModifyDate == null)
			ModifyDate = GregorianCalendar.getInstance();
		return ModifyDate.getTimeInMillis();
	}
	
	//Bitmap
	public Bitmap getBitmap() throws InvalidPictureException
	{
		if (Picture == null)
			throw new InvalidPictureException();
		return Picture;
	}
	
	public byte[] getPictureAsByteArray()
	{
		if (Picture == null)
			return null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Picture.compress(CompressFormat.PNG, 10, bos);
		return bos.toByteArray();
	}
	
	public void setPictureFromInputstream(InputStream is)
	{
		Picture = BitmapFactory.decodeStream(is);
	}
	
	public void setBitmapFromByteArray(byte[] bitmap)
	{
		if (bitmap == null)
			Picture = null;
		else
			Picture = BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length);
	}
	
	public void DestroyImage()
	{
		if (Picture != null)
			Picture.recycle();
	}
}