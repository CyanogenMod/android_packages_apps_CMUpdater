/*
 * JF Updater: Auto-updater for modified Android OS
 *
 * Copyright (c) 2009 Sergi Vélez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package cmupdater.service;

import java.io.IOException;
import java.util.List;

public interface IUpdateServer {

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
	public List<UpdateInfo> getAvailableUpdates() throws IOException;
	
	/**
	 * Downloads the update file of the specified UpdateInfo instance.
	 * This method is synchronous, and it's expected to lock until the request is fulfilled.
	 * 
	 * @param ui The <code>UpdateInfo</code> corresponding to the update to download
	 * @param destinationFile The destination where the upload file should be downloaded
	 */
	//public void downloadUpdateFile(UpdateInfo ui, File destinationFile) throws IOException;
}
