package com.cyanogenmod.updater2.misc;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cyanogenmod.updater2.utils.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 5499890003569313403L;
    private static final Pattern sIncrementalPattern =
            Pattern.compile("^incremental-(.*)-(.*).zip$");

    public static final String CHANGELOG_EXTENSION = ".changelog.html";

    public enum Type {
        UNKNOWN,
        STABLE,
        RC,
        SNAPSHOT,
        NIGHTLY,
        INCREMENTAL
    }

    private String mUiName;
    private String mFileName;
    private Type mType;
    private int mApiLevel;
    private long mBuildDate;
    private String mDownloadUrl;
    private String mChangelogUrl;
    private String mMd5Sum;
    private String mIncremental;
    private int mStyle;

    private Boolean mIsNewerThanInstalled;

    private UpdateInfo() {
        // Use the builder
    }

    private UpdateInfo(Parcel in) {
        readFromParcel(in);
    }

    public File getChangeLogFile(Context context) {
        return new File(context.getCacheDir(), mFileName + CHANGELOG_EXTENSION);
    }

    public int getApiLevel() {
        return mApiLevel;
    }

    public String getName() {
        return mUiName;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public Type getType() {
        return mType;
    }

    public String getMD5Sum() {
        return mMd5Sum;
    }

    public long getDate() {
        return mBuildDate;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public String getChangelogUrl() {
        return mChangelogUrl;
    }

    public String getIncremental() {
        return mIncremental;
    }

    public void setStyle(int mStyle) {
        this.mStyle = mStyle;
    }

    public int getStyle() {
        return mStyle;
    }

    public boolean isIncremental() {
        Matcher matcher = sIncrementalPattern.matcher(getFileName());
        if (matcher.find() && matcher.groupCount() == 2) {
            return true;
        } else {
            return false;
        }
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

    public static final Parcelable.Creator<UpdateInfo> CREATOR =
            new Parcelable.Creator<UpdateInfo>() {
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

    public static class Builder {
        private String mUiName;
        private String mFileName;
        private Type mType = Type.UNKNOWN;
        private int mApiLevel;
        private long mBuildDate;
        private String mDownloadUrl;
        private String mChangelogUrl;
        private String mMd5Sum;
        private String mIncremental;


        public Builder setName(String uiName) {
            mUiName = uiName;
            return this;
        }

        public Builder setFileName(String fileName) {
            initializeName(fileName);
            return this;
        }

        public Builder setType(String typeString) {
            Type type;
            if (TextUtils.equals(typeString, "stable")) {
                type = UpdateInfo.Type.STABLE;
            } else if (TextUtils.equals(typeString, "RC")) {
                type = UpdateInfo.Type.RC;
            } else if (TextUtils.equals(typeString, "snapshot")) {
                type = UpdateInfo.Type.SNAPSHOT;
            } else if (TextUtils.equals(typeString, "nightly")) {
                type = UpdateInfo.Type.NIGHTLY;
            } else {
                type = UpdateInfo.Type.UNKNOWN;
            }
            mType = type;
            return this;
        }

        public Builder setType(Type type) {
            mType = type;
            return this;
        }

        public Builder setApiLevel(int apiLevel) {
            mApiLevel = apiLevel;
            return this;
        }

        public Builder setBuildDate(long buildDate) {
            mBuildDate = buildDate;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            mDownloadUrl = downloadUrl;
            return this;
        }

        public Builder setChangelogUrl(String changelogUrl) {
            mChangelogUrl = changelogUrl;
            return this;
        }

        public Builder setMD5Sum(String md5Sum) {
            mMd5Sum = md5Sum;
            return this;
        }

        public Builder setIncremental(String incremental) {
            mIncremental = incremental;
            return this;
        }

        public UpdateInfo build() {
            UpdateInfo info = new UpdateInfo();
            info.mUiName = mUiName;
            info.mFileName = mFileName;
            info.mType = mType;
            info.mApiLevel = mApiLevel;
            info.mBuildDate = mBuildDate;
            info.mDownloadUrl = mDownloadUrl;
            info.mChangelogUrl = mChangelogUrl;
            info.mMd5Sum = mMd5Sum;
            info.mIncremental = mIncremental;
            return info;
        }


        private void initializeName(String fileName) {
            mFileName = fileName;
            if (!TextUtils.isEmpty(fileName)) {
                mUiName = extractUiName(fileName);
            } else {
                mUiName = null;
            }
        }
    }
}
