package cmupdaterapp.interfaces;
import cmupdaterapp.customTypes.UpdateInfo;

interface IDownloadServiceCallback
{    
    void updateDownloadProgress(int downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
    void UpdateDownloadMirror(String mirror);
    void DownloadFinished(in UpdateInfo u);
    void DownloadError();
}