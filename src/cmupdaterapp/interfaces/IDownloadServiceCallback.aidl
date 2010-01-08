package cmupdaterapp.interfaces;
import cmupdaterapp.customTypes.UpdateInfo;

interface IDownloadServiceCallback
{    
    void updateDownloadProgress(long downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
    void UpdateDownloadMirror(String mirror);
    void DownloadFinished(in UpdateInfo u);
    void DownloadError();
    void ResumeSupported(boolean supported);
    void noMD5Found();
}