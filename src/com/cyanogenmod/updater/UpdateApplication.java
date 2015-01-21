/*
 * Copyright (C) 2012-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.updater;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class UpdateApplication extends Application implements
        Application.ActivityLifecycleCallbacks {
    private boolean mMainActivityActive;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        mMainActivityActive = false;
        registerActivityLifecycleCallbacks(this);
        mRequestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onActivityCreated (Activity activity, Bundle savedInstanceState) {
        // Do nothing here
    }

    @Override
    public void onActivityDestroyed (Activity activity) {
        // Do nothing here
    }

    @Override
    public void onActivityPaused (Activity activity) {
        // Do nothing here
    }

    @Override
    public void onActivityResumed (Activity activity) {
        // Do nothing here
    }

    @Override
    public void onActivitySaveInstanceState (Activity activity, Bundle outState) {
        // Do nothing here
    }

    @Override
    public void onActivityStarted (Activity activity) {
        if (activity instanceof UpdatesSettings) {
            mMainActivityActive = true;
        }
    }

    @Override
    public void onActivityStopped (Activity activity) {
        if (activity instanceof UpdatesSettings) {
            mMainActivityActive = false;
        }
    }

    public boolean isMainActivityActive() {
        return mMainActivityActive;
    }

    public RequestQueue getQueue() {
        return mRequestQueue;
    }
}
