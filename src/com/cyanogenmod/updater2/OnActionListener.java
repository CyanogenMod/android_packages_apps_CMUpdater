package com.cyanogenmod.updater2;

import com.cyanogenmod.updater2.misc.UpdateInfo;

interface OnActionListener {
    void onStartDownload(UpdateInfo mUpdate);
    void onStopDownload(UpdateInfo mUpdate);
    void onStartUpdate(UpdateInfo mUpdate);
    void onDeleteUpdate(UpdateInfo mUpdate);
}
