/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TosDialogFragment extends DialogFragment {

    public static String TAG = TosDialogFragment.class.getSimpleName();

    private View mRootView;
    private WebView mWebView;
    private Uri mUri;
    private AlertDialog mDialog;

    public static TosDialogFragment newInstance(Uri uri) {
        final TosDialogFragment frag = new TosDialogFragment();
        Bundle b = new Bundle();
        b.putParcelable("uri", uri);
        frag.setArguments(b);
        return frag;
    }

    public TosDialogFragment() {

    }

    public TosDialogFragment setUri(String uri) {
        mUri = Uri.parse(uri);
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mUri = getArguments().getParcelable("uri");
        mRootView = getActivity().getLayoutInflater().inflate(R.layout.terms_webview, null, false);
        mWebView = (WebView) mRootView.findViewById(R.id.webview);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

        });
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.loadUrl(mUri.toString());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null) {
            mDialog = new AlertDialog.Builder(getActivity())
                    .setView(mRootView)
                    .setCancelable(false)
                    .setNegativeButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();

        }
        return mDialog;
    }


}

