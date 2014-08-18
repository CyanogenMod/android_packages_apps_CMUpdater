/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.requests;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UpdatesJsonObjectRequest extends JsonObjectRequest {
    private String mUserAgent;
    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    public UpdatesJsonObjectRequest(String url, String userAgent, JSONObject jsonRequest,
            Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
        mUserAgent = userAgent;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (mUserAgent != null) {
            mHeaders.put("User-Agent", mUserAgent);
        }
        mHeaders.put("Cache-Control", "no-cache");
        return mHeaders;
    }

    public void addHeader(String key, String what) {
        mHeaders.put(key, what);
    }
}
