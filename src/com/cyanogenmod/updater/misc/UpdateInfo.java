/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cyanogenmod.updater.utils.Utils;

import java.io.Serializable;

public class UpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 5499890003569313403L;

    public enum Type {
        UNKNOWN,
        STABLE,
        SNAPSHOT,
        NIGHTLY
    };

    private String mUiName;
    private String mFileName;
    private String mVersion;
    private Type mType;
    private long mBuildDate;
    private String mDownloadUrl;
    private String mMd5Sum;
    private String mChangeLog;

    public UpdateInfo(String fileName, long date, String url,
            String md5, Type type, String changeLog) {
        initializeName(fileName);
        mBuildDate = date;
        mDownloadUrl = url;
        mMd5Sum = md5;
        mType = type;
        mChangeLog = changeLog;
    }

    public UpdateInfo(String fileName, String changeLog) {
        this(fileName, 0, null, null, Type.UNKNOWN, changeLog);
    }

    private UpdateInfo(Parcel in) {
        readFromParcel(in);
    }

    /**
      * Return changelog
      */
    public String getChangeLog() {
        return mChangeLog;
    }

    /**
     * Get name for UI display
     */
    public String getName() {
        return mUiName;
    }

    /**
     * Get version
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * Get file name
     */
    public String getFileName() {
        return mFileName;
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

    public void setChangeLog(String changeLog) {
        mChangeLog = changeLog;
    }

    private void initializeName(String fileName) {
        mFileName = fileName;
        if (!TextUtils.isEmpty(fileName)) {
            String deviceType = Utils.getDeviceType();
            mUiName = fileName.replaceAll("\\.zip$", "");
            mUiName = mUiName.replaceAll("-" + deviceType + "-?", "");
            mVersion = fileName.replaceAll(".*?([0-9.]+?)-.+","$1");
        } else {
            mUiName = null;
            mVersion = null;
        }
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
                && TextUtils.equals(mMd5Sum, ui.mMd5Sum);
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
        out.writeString(mVersion);
        out.writeString(mType.toString());
        out.writeLong(mBuildDate);
        out.writeString(mDownloadUrl);
        out.writeString(mMd5Sum);
        out.writeString(mChangeLog);
    }

    private void readFromParcel(Parcel in) {
        mUiName = in.readString();
        mFileName = in.readString();
        mVersion = in.readString();
        mType = Enum.valueOf(Type.class, in.readString());
        mBuildDate = in.readLong();
        mDownloadUrl = in.readString();
        mMd5Sum = in.readString();
        mChangeLog = in.readString();
    }
}
