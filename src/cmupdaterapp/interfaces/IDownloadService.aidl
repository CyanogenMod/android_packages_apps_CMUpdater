package cmupdaterapp.interfaces;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IDownloadServiceCallback;

interface IDownloadService
{    
    void Download(in UpdateInfo ui);
    UpdateInfo getCurrentUpdate();
    String getCurrentMirrorName();
    boolean DownloadRunning();
    boolean PauseDownload();
    boolean cancelDownload();
    void registerCallback(in IDownloadServiceCallback cb);
    void unregisterCallback(in IDownloadServiceCallback cb);
}