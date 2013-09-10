/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
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
