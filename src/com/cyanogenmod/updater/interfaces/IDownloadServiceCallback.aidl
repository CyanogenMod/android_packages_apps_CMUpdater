package com.cyanogenmod.updater.interfaces;
import com.cyanogenmod.updater.customTypes.UpdateInfo;

interface IDownloadServiceCallback
{    
    void updateDownloadProgress(long downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
    void DownloadFinished(in UpdateInfo u);
    void DownloadError();
}
