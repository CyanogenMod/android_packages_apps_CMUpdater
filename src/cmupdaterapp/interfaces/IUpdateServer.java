package cmupdaterapp.interfaces;

import java.io.IOException;

import cmupdaterapp.customTypes.FullUpdateInfo;

public interface IUpdateServer
{
	public FullUpdateInfo getAvailableUpdates() throws IOException;
}