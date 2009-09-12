package cmupdaterapp.ui;

import android.util.Config;

public class Log
{
	private final static String LOGTAG = "cmupdater";

	//static final boolean DEBUG = false;
	//static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;

	public static void v(String TAG, String logMe)
	{
		if(Config.LOGV) android.util.Log.v(LOGTAG, TAG + " " + logMe);
	}
	
	public static void d(String TAG, String logMe)
	{
		if(Config.LOGD) android.util.Log.d(LOGTAG, TAG + " " + logMe);
	}
	
	public static void e(String TAG, String logMe)
	{
		android.util.Log.e(LOGTAG, TAG + " " + logMe);
	}

	public static void e(String TAG, String logMe, Exception ex)
	{
		android.util.Log.e(LOGTAG, TAG + " " + logMe, ex);
	}
}