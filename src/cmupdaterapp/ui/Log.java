package cmupdaterapp.ui;

import android.util.Config;

public class Log
{
	private final static String LOGTAG = "cmupdater";

	//static final boolean DEBUG = false;
	//static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;
	private static final boolean LOGV = Config.LOGV;

	public static void v(String TAG, String logMe)
	{
		if(Log.LOGV) android.util.Log.v(LOGTAG + " " + TAG, logMe);
	}
	
	public static void e(String TAG, String logMe)
	{
		android.util.Log.e(LOGTAG + " " + TAG, logMe);
	}

	public static void e(String TAG, String logMe, Exception ex)
	{
		android.util.Log.e(LOGTAG + " " + TAG, logMe, ex);
	}
}