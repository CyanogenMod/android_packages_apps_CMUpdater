package com.cyanogenmod.updater.interfaces;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.interfaces.IDownloadServiceCallback;

interface IDownloadService
{    
    void Download(in UpdateInfo ui);
    UpdateInfo getCurrentUpdate();
    boolean DownloadRunning();
    void PauseDownload();
    void cancelDownload();
    void registerCallback(in IDownloadServiceCallback cb);
    void unregisterCallback(in IDownloadServiceCallback cb);
}
