/*
 * Copyright (C) 2013-2015 The CyanogenMod Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class NotifyingWebView extends WebView {
    public interface OnInitialContentReadyListener {
        void onInitialContentReady(WebView view);
    }

    private OnInitialContentReadyListener mListener;
    private boolean mContentReady = false;

    public NotifyingWebView(Context context) {
        super(context);
    }

    public NotifyingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnInitialContentReadyListener(OnInitialContentReadyListener listener) {
        mListener = listener;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (getContentHeight() > 0 && !mContentReady) {
            if (mListener != null) {
                mListener.onInitialContentReady(this);
            }
            mContentReady = true;
        }
    }
}
