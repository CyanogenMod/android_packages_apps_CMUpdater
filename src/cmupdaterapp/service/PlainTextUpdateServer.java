package cmupdaterapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.SysUtils;

import android.content.Context;
import android.util.Log;

public class PlainTextUpdateServer implements IUpdateServer
{
	private static final String TAG = "<CM-Updater> PlainTextUpdateServer";

	private HttpClient httpClient;
	private URI mUpdateServerUri;
	private Preferences mPreferences;

	public PlainTextUpdateServer(Context ctx)
	{
		httpClient = new DefaultHttpClient();
		Preferences p = mPreferences = Preferences.getPreferences(ctx);
		String sm = p.getConfiguredModString();

		if(sm == null)
		{
			Log.e(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
		}
		else
		{
			Log.i(TAG, "System's Mod version:" + sm);
		}
	}
	public FullUpdateInfo getAvailableUpdates() throws IOException
	{
		String systemMod = mPreferences.getConfiguredModString();
		String systemRom = SysUtils.getReadableModVersion();
		int[] sysVersion = SysUtils.getSystemModVersion();
		String[] themeInfos = mPreferences.getThemeInformations();
		//Instantiate it, so we can use later [0] and [1] without checking
		if (themeInfos == null)
			themeInfos = new String[2];
		//Get the actual Updateserver URL
		mUpdateServerUri = URI.create(mPreferences.getUpdateFileURL());
		HttpUriRequest req = new HttpGet(mUpdateServerUri);
		req.addHeader("Cache-Control", "no-cache");

		HttpResponse response = httpClient.execute(req);

		int serverResponse = response.getStatusLine().getStatusCode();
		if (serverResponse != 200)
		{
			Log.e(TAG, "Server returned status code " + serverResponse);
			throw new IOException("Server returned status code "
					+ serverResponse);
		}

		FullUpdateInfo retValue = new FullUpdateInfo();
		
		HttpEntity responseEntity = response.getEntity();

		try
		{
			/**
			 * Read Entity into BufferedReader and eventually into a StringBuffer
			 */
			
			BufferedReader lineReader = new BufferedReader(
					new InputStreamReader(responseEntity.getContent()),
					2 * 1024);
			
			StringBuffer buf = new StringBuffer();
			String line;

			while ((line = lineReader.readLine()) != null)
			{
				buf.append(line);
			}

			lineReader.close();
			
			
			/**
			 * Parse StringBuffer (JSON Document)
			 */

			LinkedList<UpdateInfo> updateInfos = parseJSON(buf);
			for (int i = 0, max = updateInfos.size() ; i < max ; i++)
			{
				UpdateInfo ui = updateInfos.poll();
				//For Roms
				if (ui.type.toLowerCase().equals("rom"))
				{
					if (boardMatches(ui, systemMod))
					{
						if(mPreferences.showDowngrades() || updateIsNewer(ui, sysVersion, true))
						{
							if (branchMatches(ui, mPreferences.allowExperimental()))
							{
								retValue.roms.add(ui);
							}
						}
						else
						{
							Log.d(TAG, "Discarding Rom " + ui.name + " (older version)");
						}
					}
					else
					{
						Log.d(TAG, "Discarding Rom " + ui.name + " (mod mismatch)");
					}
				}
				//For Themes
				else if (ui.type.toLowerCase().equals("theme"))
				{
					if (themeInfos != null
						&& ui.name.equals(themeInfos[0])
						&& Integer.valueOf(ui.displayVersion) > Integer.valueOf(themeInfos[1]))
					{
						if (romMatches(ui, systemRom))
						{
							if(mPreferences.showDowngrades() || updateIsNewer(ui, sysVersion, true))
							{
								if (branchMatches(ui, mPreferences.allowExperimental()))
								{
									retValue.themes.add(ui);
								}
							}
							else
							{
								Log.d(TAG, String.format("Discarding Theme %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos[0], themeInfos[1], ui.name, ui.displayVersion));
							}
						}
						else
						{
							Log.d(TAG, String.format("Discarding Theme %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos[0], themeInfos[1], ui.name, ui.displayVersion));
						}
					}
					else
					{
						Log.d(TAG, String.format("Discarding Theme %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos[0], themeInfos[1], ui.name, ui.displayVersion));
					}
				}
			}

		}
		finally
		{
			responseEntity.consumeContent();
		}
		
		return retValue;
	}

	private LinkedList<UpdateInfo> parseJSON(StringBuffer buf)
	{
		LinkedList<UpdateInfo> uis = new LinkedList<UpdateInfo>();

		JSONObject mainJSONObject;
		try
		{
			mainJSONObject = new JSONObject(buf.toString());
			JSONArray mirrorList = mainJSONObject.getJSONArray("MirrorList");
			JSONArray updateList = mainJSONObject.getJSONArray("UpdateList");

			for (int i = 0, max = updateList.length() ; i < max ; i++)
			{
				uis.add(parseUpdateJSONObject(updateList.getJSONObject(i),mirrorList));
			}

		}
		catch (JSONException e)
		{
			Log.e(TAG, "Error in JSON File: ", e);
		}

		return uis;
	}

	private UpdateInfo parseUpdateJSONObject(JSONObject obj, JSONArray mirrorList)
	{
		UpdateInfo ui = new UpdateInfo();

		try
		{
			ui.board = new LinkedList<String>();
			String[] Boards = obj.getString("board").split("\\|");
			for(String item:Boards)
			{
				if(item!=null)
					ui.board.add(item);
			}
			ui.type = obj.getString("type");
			ui.mod = obj.getString("mod");
			ui.name = obj.getString("name");
			ui.displayVersion = obj.getString("version");
			ui.description = obj.getString("description");
			ui.branchCode = obj.getString("branch");
			ui.fileName = obj.getString("filename");
			
			ui.updateFileUris = new LinkedList<URI>();

			for (int i = 0, max = mirrorList.length() ; i < max ; i++)
			{
				try
				{
					ui.updateFileUris.add(new URI(mirrorList.getString(i) + ui.fileName));
				}
				catch (URISyntaxException e)
				{
					Log.w(TAG, "Unable to parse mirror url (" + mirrorList.getString(i) + ui.fileName
							+ "). Ignoring this mirror", e);
				}
			}
		}
		catch (JSONException e)
		{
			Log.e(TAG, "Error in JSON File: ", e);
		}
		return ui;
	}

	private boolean branchMatches(UpdateInfo ui, boolean experimentalAllowed )
	{
		if(ui == null) return false;

		boolean allow = false;

		if (ui.branchCode.charAt(0) == 'X')
		{
			if (experimentalAllowed == true)
				allow = true;
		}
		else
		{
			allow = true;
		}

		Log.d(TAG, "Update Branch:" + ui.branchCode + "; Experimental Allowed:" + experimentalAllowed);
		return allow;
	}

	private boolean boardMatches(UpdateInfo ui, String systemMod)
	{
		if(ui == null) return false;
		//If * is provided, all Boards are supported
		if(ui.board.equals("*") || systemMod.equals("*")) return true;
		Log.d(TAG, "BoardString:" + ui.board + "; System Mod:" + systemMod);
		for(String board:ui.board)
		{
			if(board.equals(systemMod))
				return true;
		}
		return false;
	}
	
	private boolean romMatches(UpdateInfo ui, String systemRom)
	{
		if(ui == null) return false;
		if(ui.mod.equals("*") || systemRom.equals("*")) return true;
		Log.d(TAG, "ThemeRom:" + ui.mod + "; SystemRom:" + systemRom);
		if(ui.mod.equals(systemRom))
				return true;
		return false;
	}

	private boolean updateIsNewer(UpdateInfo ui, int[] sysVersion, boolean defaultValue)
	{
		/*
		String modVersion = Preferences.getSystemProperty(Preferences.SYS_PROP_MOD_VERSION);
		if(modVersion == null || modVersion.length() < PROP_MOD_VERSION_SKIP_CHARS) return defaultValue;

		String[] sysVersion = modVersion.substring(PROP_MOD_VERSION_SKIP_CHARS).split("\\.");*/

		
		String[] updateVersion = ui.displayVersion.split("\\.");

		Log.d(TAG, "Update Version:" + Arrays.toString(updateVersion) + "; System Version:" + Arrays.toString(sysVersion));

		int sys, update;
		int max = Math.min(sysVersion.length, updateVersion.length);
		for(int i = 0; i < max; i++)
		{
			try
			{
				sys = sysVersion[i];
				update = Integer.parseInt(updateVersion[i]);

				if(sys != update) return sys < update; 

			}
			catch (NumberFormatException ex)
			{
				Log.d(TAG, "NumberFormatException while parsing version values:", ex);
				return defaultValue;
			}
		}

		return sysVersion.length < updateVersion.length;
	}
}