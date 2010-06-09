package cmupdaterapp.misc;

import android.util.Config;

public class Log {
    private final static String LOGTAG = "cmupdater";

    //static final boolean DEBUG = false;
    //public static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;
    //Replace all Log.d with if(Log.LOGV) Log.v() to prevent Strings to be builded and to save memory

    public static void v(String TAG, String logMe) {
        if (Config.LOGV) android.util.Log.v(LOGTAG, TAG + ": " + logMe);
    }

    public static void d(String TAG, String logMe) {
        if (Config.LOGD) android.util.Log.d(LOGTAG, TAG + ": " + logMe);
    }

    public static void e(String TAG, String logMe) {
        android.util.Log.e(LOGTAG, TAG + ": " + logMe);
    }

    public static void e(String TAG, String logMe, Throwable ex) {
        android.util.Log.e(LOGTAG, TAG + ": " + logMe, ex);
    }

    public static void i(String TAG, String logMe) {
        if (Config.LOGD) android.util.Log.i(LOGTAG, TAG + ": " + logMe);
    }
}