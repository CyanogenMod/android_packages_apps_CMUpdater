/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cyanogenmod.updater.utils.Utils;

import java.io.File;
import java.io.Serializable;

public class UpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 5499890003569313403L;

    public enum Type {
        UNKNOWN,
        STABLE,
        RC,
        SNAPSHOT,
        NIGHTLY,
        INCREMENTAL
    };

    private String mUiName;
    private String mFileName;
    private Type mType;
    private int mApiLevel;
    private long mBuildDate;
    private String mDownloadUrl;
    private String mMd5Sum;
    private String mIncremental;

    private Boolean mIsNewerThanInstalled;

    public UpdateInfo(String fileName, long date, int apiLevel, String url,
            String md5, Type type, String incremental) {
        initializeName(fileName);
        mBuildDate = date;
        mApiLevel = apiLevel;
        mDownloadUrl = url;
        mMd5Sum = md5;
        mType = type;
        mIncremental = incremental;
    }

    public UpdateInfo(String fileName) {
        this(fileName, 0, 0, null, null, Type.UNKNOWN, null);
    }

    private UpdateInfo(Parcel in) {
        readFromParcel(in);
    }

    public File getChangeLogFile(Context context) {
        return new File(context.getCacheDir(), mFileName + ".changelog");
    }

    /**
     * Get name for UI display
     */
    public String getName() {
        return mUiName;
    }

    /**
     * Get file name
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Set file name
     */
    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    /**
     * Get build type
     */
    public Type getType() {
        return mType;
    }

   /**
     * Get MD5
     */
    public String getMD5Sum() {
        return mMd5Sum;
    }

    /**
     * Get build date
     */
    public long getDate() {
        return mBuildDate;
    }

    /**
     * Get download location
     */
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    /**
     * Get incremental version
     */
    public String getIncremental() {
        return mIncremental;
    }

    public boolean isNewerThanInstalled() {
        if (mIsNewerThanInstalled != null) {
            return mIsNewerThanInstalled;
        }

        int installedApiLevel = Utils.getInstalledApiLevel();
        if (installedApiLevel != mApiLevel && mApiLevel > 0) {
            mIsNewerThanInstalled = mApiLevel > installedApiLevel;
        } else {
            // API levels match, so compare build dates.
            mIsNewerThanInstalled = mBuildDate > Utils.getInstalledBuildDate();
        }

        return mIsNewerThanInstalled;
    }

    private void initializeName(String fileName) {
        mFileName = fileName;
        if (!TextUtils.isEmpty(fileName)) {
            mUiName = extractUiName(fileName);
        } else {
            mUiName = null;
        }
    }

    public static String extractUiName(String fileName) {
        String deviceType = Utils.getDeviceType();
        String uiName = fileName.replaceAll("\\.zip$", "");
        return uiName.replaceAll("-" + deviceType + "-?", "");
    }

    @Override
    public String toString() {
        return "UpdateInfo: " + mFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof UpdateInfo)) {
            return false;
        }

        UpdateInfo ui = (UpdateInfo) o;
        return TextUtils.equals(mFileName, ui.mFileName)
                && mType.equals(ui.mType)
                && mBuildDate == ui.mBuildDate
                && TextUtils.equals(mDownloadUrl, ui.mDownloadUrl)
                && TextUtils.equals(mMd5Sum, ui.mMd5Sum)
                && TextUtils.equals(mIncremental, ui.mIncremental);
    }

    public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>() {
        public UpdateInfo createFromParcel(Parcel in) {
            return new UpdateInfo(in);
        }

        public UpdateInfo[] newArray(int size) {
            return new UpdateInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUiName);
        out.writeString(mFileName);
        out.writeString(mType.toString());
        out.writeInt(mApiLevel);
        out.writeLong(mBuildDate);
        out.writeString(mDownloadUrl);
        out.writeString(mMd5Sum);
        out.writeString(mIncremental);
    }

    private void readFromParcel(Parcel in) {
        mUiName = in.readString();
        mFileName = in.readString();
        mType = Enum.valueOf(Type.class, in.readString());
        mApiLevel = in.readInt();
        mBuildDate = in.readLong();
        mDownloadUrl = in.readString();
        mMd5Sum = in.readString();
        mIncremental = in.readString();
    }
}
