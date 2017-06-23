/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */
package com.cyanogenmod.updater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.utils.Utils;

import java.util.Date;

public class UpdatesActivity extends AppCompatActivity {

    private TextView mHeaderInfo;
    private UpdatesSettings mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_updater);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView headerCm = (TextView) findViewById(R.id.header_version);
        mHeaderInfo = (TextView) findViewById(R.id.header_info);

        mSettingsFragment = new UpdatesSettings();

        final String version = Utils.getInstalledVersionName();
        headerCm.setText(String.format(getString(R.string.header_os), version));

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, mSettingsFragment).commit();

        updateHeader();
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_logo);
        setSupportActionBar(toolbar);

        final CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout)
                findViewById(R.id.collapsing_toolbar);
        final AppBarLayout appBar = (AppBarLayout) findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean mIsShown = false;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int scrollRange = appBarLayout.getTotalScrollRange();
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(getString(R.string.app_name));
                    mIsShown = true;
                } else if (mIsShown) {
                    collapsingToolbar.setTitle(" ");
                    mIsShown = false;
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if we need to refresh the screen to show new updates
        if (intent.getBooleanExtra(UpdatesSettings.EXTRA_UPDATE_LIST_UPDATED, false)) {
            mSettingsFragment.updateLayout();
        }

        mSettingsFragment.checkForDownloadCompleted(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                if (mSettingsFragment != null) {
                    mSettingsFragment.checkForUpdates();
                    updateHeader();
                }
                break;
            case R.id.menu_delete_all:
                if (mSettingsFragment != null) {
                    mSettingsFragment.confirmDeleteAll();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateHeader() {
        mHeaderInfo.setText(String.format(getString(R.string.header_summary),
                Utils.getDateLocalized(this, Utils.getInstalledBuildDate()),
                Utils.getInstalledBuildType(), Build.VERSION.RELEASE, getLastCheck()));
    }

    private String getLastCheck() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Date lastCheck = new Date(prefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
        String date = DateFormat.getLongDateFormat(this).format(lastCheck);
        String time = DateFormat.getTimeFormat(this).format(lastCheck);
        return String.format("%1$s %2$s (%3$s)", getString(R.string.sysinfo_last_check),
                date, time);
    }

    void showSnack(String message) {
        Snackbar.make(findViewById(R.id.coordinator), message, Snackbar.LENGTH_LONG).show();
    }
}
