/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class HttpRequestExecutor {
    private HttpClient mHttpClient;
    private HttpRequestBase mRequest;
    private boolean mAborted;

    public HttpRequestExecutor() {
        mHttpClient = new DefaultHttpClient();
        mAborted = false;
    }

    public HttpEntity execute(HttpRequestBase request) throws IOException {
        synchronized (this) {
            mAborted = false;
            mRequest = request;
        }

        HttpResponse response = mHttpClient.execute(request);
        HttpEntity entity = null;

        if (!mAborted && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            entity = response.getEntity();
        }

        synchronized (this) {
            mRequest = null;
        }

        return entity;
    }

    public synchronized void abort() {
        if (mAborted) {
            return;
        }
        mAborted = true;
        if (mRequest != null) {
            abortRequest(mRequest);
        }
    }

    private void abortRequest(final HttpRequestBase request) {
        // HttpRequestBase.abort() may cause network activity, which must not happen in the
        // main thread. Spawn off the cleanup into a separate thread to avoid crashing due
        // to NetworkOnMainThreadException.
        final Thread abortThread = new Thread(new Runnable() {
            @Override
            public void run() {
                request.abort();
            }
        });
        abortThread.start();
    }

    public synchronized boolean isAborted() {
        return mAborted;
    }
}
