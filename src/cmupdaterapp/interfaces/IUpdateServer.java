package cmupdaterapp.interfaces;

import java.io.IOException;

import cmupdaterapp.customTypes.FullUpdateInfo;

public interface IUpdateServer
{
	/**
	 * Returns a list with all available updates on that server.
	 * 
	 * Classes implementing this interface are required to check if the available updates are compatible with
	 * the currently running service; the caller is assured that every <code>UpdateInfo</code> returned by this method will
	 * be compatible with the currently running service. This method is synchronous, and it's expected to lock
	 * until the request is fulfilled.
	 * 
	 * @return A list of <code>UpdateInfo</code> instances, one per each update available 
	 */
	public FullUpdateInfo getAvailableUpdates() throws IOException;
}