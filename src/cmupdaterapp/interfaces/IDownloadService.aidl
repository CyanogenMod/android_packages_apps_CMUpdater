package cmupdaterapp.interfaces;
import cmupdaterapp.customTypes.UpdateInfo;

interface IDownloadService
{    
    void Download(in UpdateInfo ui);
    UpdateInfo getCurrentUpdate();
    String getCurrentMirrorName();
    boolean DownloadRunning();
    boolean PauseDownload();
    boolean ResumeDownload();
    boolean isPaused();
    boolean cancelDownload();
}