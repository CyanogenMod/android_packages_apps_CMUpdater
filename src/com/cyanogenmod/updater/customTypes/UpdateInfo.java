/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at http://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.customTypes;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UpdateInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 5499890003569313403L;

    public int PrimaryKey = -1;
    private String name;
    private String version;
    private String branchCode;
    private List<String> description;
    private String fileName;
    private Integer buildDate;
    private String downloadUrl;
    private String md5sum;
    private String changes;
    private String changelogUrl;

    /**
      * Set changelog url
      */
    public void setChangelogUrl(String _url) {
        if(_url != null)
            changelogUrl = _url.trim();
        else
            changelogUrl = "";
    }

    /**
      * Return changelog url
      */
    public String getChangelogUrl() {
        return changelogUrl;
    }

    /**
      * Set changelog
      */
    public void setChanges(String _changes) {
        if(_changes != null)
            changes = _changes.trim();
        else
            changes = "";
    }

    /**
      * Return changelog
      */
    public String getChanges() {
        return changes;
    }

    /**
     * Set Name
     */
    public void setName(String _name) {
        if (_name != null) {
            name = _name.trim();
        } else {
            name = "";
        }
        name.replaceAll(".zip$","");
    }

    /**
     * Get Name
     * returns INCR: + name, if its an incremental udpate
     */
    public String getName() {
        return name;
    }

    /**
     * Set Version
     */
    public void setVersion(String _filename) {
        if (_filename != null) {
            version = _filename.trim();
        } else {
            version = "";
        }
        version = version.replaceAll(".*?([0-9.]+?)-.+","$1");
    }

    /**
     * Get Version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set BranchCode
     */
    public void setBranchCode(String _branchCode) {
        if (_branchCode != null) {
            branchCode = _branchCode.trim();
        } else {
            branchCode = "";
        }
    }

    /**
     * Get BranchCode
     */
    public String getBranchCode() {
        return branchCode;
    }

    /**
     * Set Descrition
     */
    public void setDescription(String _description) {
        if (_description != null) {
            Collections.addAll(description, _description.trim().split("\\|"));
        } else {
            description = null;
        }
    }

    /**
     * Set Descrition
     */
    public void setDescription(List<String> _description) {
        description = _description;
    }

    /**
     * Get Description
     */
    public List<String> getDescription() {
        return description;
    }

    /**
     * Set Filename
     */
    public void setFileName(String _fileName) {
        if (_fileName != null) {
            fileName = _fileName.trim();
        } else {
            fileName = "";
        }
    }

    /**
     * Get Filename
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set MD5
     */
    public void setMD5(String _md5) {
        if (_md5 != null) {
            md5sum = _md5.trim();
        } else {
            md5sum = "";
        }
    }

    /**
     * Get MD5
     */
    public String getMD5() {
        return md5sum;
    }

    /**
     * Set build date
     */
    public void setDate(String _timeStamp) {
        if (_timeStamp != null) {
            buildDate = new Integer(_timeStamp.trim());
        } else {
            buildDate = 0;
        }
    }

    /**
     * Get build date
     */
    public Integer getDate() {
        return buildDate;
    }

    /**
     * Set download location
     */
    public void setDownloadUrl(String _url) {
        if (_url != null) {
            downloadUrl = _url.trim();
        } else {
            downloadUrl = "";
        }
    }

    /**
     * Get download location
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
        public String toString() {
            return name;
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
            //For the old stored updates
            return ui.name.equals(name)
                && ui.version.equals(version)
                && ui.branchCode.equals(branchCode)
                && ui.description.equals(description)
                && ui.fileName.equals(fileName)
                && ui.md5sum.equals(md5sum)
                && ui.downloadUrl.equals(downloadUrl)
                && ui.PrimaryKey == PrimaryKey;
        }

    public UpdateInfo() {
        description = new LinkedList<String>();
    }

    private UpdateInfo(Parcel in) {
        description = new LinkedList<String>();
        readFromParcel(in);
    }

    public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>() {
        public UpdateInfo createFromParcel(Parcel in) {
            return new UpdateInfo(in);
        }

        public UpdateInfo[] newArray(int size) {
            return new UpdateInfo[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel arg0, int arg1) {
        arg0.writeInt(PrimaryKey);
        arg0.writeString(name);
        arg0.writeString(version);
        arg0.writeString(branchCode);
        arg0.writeStringList(description);
        arg0.writeString(fileName);
        arg0.writeString(md5sum);
        arg0.writeString(downloadUrl);
        arg0.writeString(changes);
        arg0.writeString(changelogUrl);
    }

    void readFromParcel(Parcel in) {
        PrimaryKey = in.readInt();
        name = in.readString();
        version = in.readString();
        branchCode = in.readString();
        in.readStringList(description);
        fileName = in.readString();
        md5sum = in.readString();
        downloadUrl = in.readString();
        changes = in.readString();
        changelogUrl = in.readString();
    }
}
