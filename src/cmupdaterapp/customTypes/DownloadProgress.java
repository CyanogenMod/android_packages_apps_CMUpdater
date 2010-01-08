package cmupdaterapp.customTypes;

public class DownloadProgress
{
	private final long downloaded;
	private final int total;
	private final String downloadedText;
	private final String speedText;
	private final String remainingTimeText;
	
	public DownloadProgress(long _downloaded, int _total, String _downloadedText, String _speedText, String _remainingTimeText)
	{
		downloaded = _downloaded;
		total = _total;
		downloadedText = _downloadedText;
		speedText = _speedText;
		remainingTimeText = _remainingTimeText;
	}
	
	public long getDownloaded() { return downloaded; }
	public int getTotal() { return total; }
	public String getDownloadedText() { return downloadedText; }
	public String getSpeedText() { return speedText; }
	public String getRemainingTimeText() { return remainingTimeText; }
}