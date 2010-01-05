package cmupdaterapp.interfaces;

interface IDownloadServiceCallback
{    
    void updateDownloadProgress(int downloaded, int total, String downloadedText, String speedText, String remainingTimeText);
    void sendToastMessage(String msg);
}