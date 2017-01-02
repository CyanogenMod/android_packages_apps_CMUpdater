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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
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
    private String[] mInstalled;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_updater);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mHeaderCm = (TextView) findViewById(R.id.header_version);
        mHeaderInfo = (TextView) findViewById(R.id.header_info);

        mSettingsFragment = new UpdatesSettings();

        mInstalled = Utils.getInstalledVersion().split("-");
        mHeaderCm.setText(String.format(getString(R.string.header_os), mInstalled[0]));

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, mSettingsFragment).commit();

        updateHeader();
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_logo);
        setSupportActionBar(mToolbar);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSettingsFragment != null) {
            mSettingsFragment.checkForUpdates();
        }
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
    public boolean onCreateOptionsMenu(Menu mMenu) {
        getMenuInflater().inflate(R.menu.menu, mMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mItem) {
        switch (mItem.getItemId()) {
            case R.id.menu_refresh:
                if (mSettingsFragment != null) {
                    mSettingsFragment.checkForUpdates();
                    updateHeader();
                }
                break;
        }

        return super.onOptionsItemSelected(mItem);
    }

    private void updateHeader() {
        String mApi = String.valueOf(Utils.getInstalledApiLevel());
        switch (mApi) {
            case "25":
                mApi = "7.1.1";
                break;
            default:
                mApi = "API " + mApi;
        }

        mHeaderInfo.setText(String.format(getString(R.string.header_summary),
                Utils.getInstalledBuildDateLocalized(this, mInstalled[1]),
                mInstalled[2], mApi, getLastCheck()));
    }

    private String getLastCheck() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Date mLastCheck = new Date(mPrefs.getLong(Constants.LAST_UPDATE_CHECK_PREF, 0));
        String mDate = DateFormat.getLongDateFormat(this).format(mLastCheck);
        String mTime = DateFormat.getTimeFormat(this).format(mLastCheck);
        return String.format("%1$s %2$s (%3$s)", getString(R.string.sysinfo_last_check),
                mDate, mTime);
    }

    void showSnack(String mMessage) {
        Snackbar.make(findViewById(R.id.coordinator), mMessage, Snackbar.LENGTH_LONG).show();
    }
}
