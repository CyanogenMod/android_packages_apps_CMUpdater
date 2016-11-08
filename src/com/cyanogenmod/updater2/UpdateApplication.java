package com.cyanogenmod.updater2;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class UpdateApplication extends Application implements
        Application.ActivityLifecycleCallbacks {

    private boolean mMainActivityActive;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    @Override
    public void onCreate() {
        mContext = this;
        mMainActivityActive = false;
        registerActivityLifecycleCallbacks(this);
        mRequestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onActivityCreated (Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed (Activity activity) {
    }

    @Override
    public void onActivityPaused (Activity activity) {
    }

    @Override
    public void onActivityResumed (Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState (Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted (Activity activity) {
        /*if (activity instanceof UpdaterActivity) {
            mMainActivityActive = true;
        }*/
    }

    @Override
    public void onActivityStopped (Activity activity) {
        /*if (activity instanceof UpdaterActivity) {
            mMainActivityActive = false;
        }*/
    }

    public boolean isMainActivityActive() {
        return mMainActivityActive;
    }

    public RequestQueue getQueue() {
        return mRequestQueue;
    }

    public static Context getContext() {
        return mContext;
    }
}
