package cmupdaterapp.interfaces;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import cmupdaterapp.customTypes.FullUpdateInfo;

public interface IUpdateServer
{
	public FullUpdateInfo getAvailableUpdates() throws IOException;
	public List<String> Exceptions = new LinkedList<String>();
}