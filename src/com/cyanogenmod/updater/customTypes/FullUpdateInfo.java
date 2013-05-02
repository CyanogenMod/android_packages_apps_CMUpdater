/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.customTypes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.Serializable;
import java.util.LinkedList;

public class FullUpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = -2719765435535504941L;

    public LinkedList<UpdateInfo> roms;
    public LinkedList<UpdateInfo> incrementalRoms;

    public FullUpdateInfo() {
        roms = new LinkedList<UpdateInfo>();
        incrementalRoms = new LinkedList<UpdateInfo>();
    }

    private FullUpdateInfo(Parcel in) {
        roms = new LinkedList<UpdateInfo>();
        incrementalRoms = new LinkedList<UpdateInfo>();
        readFromParcel(in);
    }

    public static final Parcelable.Creator<FullUpdateInfo> CREATOR = new Parcelable.Creator<FullUpdateInfo>() {
        public FullUpdateInfo createFromParcel(Parcel in) {
            return new FullUpdateInfo(in);
        }

        public FullUpdateInfo[] newArray(int size) {
            return new FullUpdateInfo[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel arg0, int arg1) {
        arg0.writeList(roms);
        arg0.writeList(incrementalRoms);
    }

    void readFromParcel(Parcel in) {
        in.readList(roms, null);
        in.readList(incrementalRoms, null);
    }

    @Override
    public String toString() {
        return "FullUpdateInfo";
    }

    public int getRomCount() {
        if (roms == null) return 0;
        return roms.size();
    }
    
    public int getIncrementalRomCount() {
        if (incrementalRoms == null) return 0;
        return incrementalRoms.size();
    }

    public int getUpdateCount(Context context) {
        int romssize = roms == null ? 0 : roms.size();
        int incrementalromssize = incrementalRoms == null ? 0 : incrementalRoms.size();
        
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("numUpdates", romssize + incrementalromssize).apply();
		Log.d("IMPORTANT FROM FullUpdateInfo", "" + sp.getInt("numUpdates", -99));
        
        return romssize + incrementalromssize;
    }
}
