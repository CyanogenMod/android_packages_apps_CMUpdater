package cmupdaterapp.utils;

public class StringUtils
{
	public static String arrayToString(String[] items, String seperator)
	{
		if ((items == null) || (items.length == 0))
		{
			return "";
		}
		else
		{
			StringBuffer buffer = new StringBuffer(items[0]);
			for (int i = 1; i < items.length; i++)
			{
				buffer.append(seperator);
				buffer.append(items[i]);
			}
			return buffer.toString();
		}
	}
}