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

import cmupdaterapp.ui.Constants;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.SysUtils;

import android.content.Context;
import android.util.Log;

public class PlainTextUpdateServer implements IUpdateServer
{
	private static final String TAG = "<CM-Updater> PlainTextUpdateServer";

	private Preferences mPreferences;

	private String systemMod;
	private String systemRom;
	private int[] sysVersion;
	private ThemeInfo themeInfos;
	
	private boolean allowExperimentalRom;
	private boolean showAllUpdatesRom;
	private boolean allowExperimentalTheme;
	private boolean showAllUpdatesTheme;
	
	private boolean WildcardUsed = false;
	
	public PlainTextUpdateServer(Context ctx)
	{
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
		FullUpdateInfo retValue = new FullUpdateInfo();
		boolean romException = false;
		boolean themeException = false;
		HttpClient romHttpClient = new DefaultHttpClient();
		HttpClient themeHttpClient = new DefaultHttpClient();
		HttpEntity romResponseEntity = null;
		HttpEntity themeResponseEntity = null;
		systemMod = mPreferences.getConfiguredModString();
		systemRom = SysUtils.getReadableModVersion();
		sysVersion = SysUtils.getSystemModVersion();
		themeInfos = mPreferences.getThemeInformations();
		allowExperimentalRom = mPreferences.allowExperimentalRom();
		showAllUpdatesRom = mPreferences.showDowngradesRom();
		allowExperimentalTheme = mPreferences.allowExperimentalTheme();
		showAllUpdatesTheme = mPreferences.showDowngradesTheme();
		boolean ThemeUpdateUrlSet = mPreferences.ThemeUpdateUrlSet();
		
		//If Wildcard is used or no themes.theme file present set the variable
		if (themeInfos == null || themeInfos.name.equalsIgnoreCase(Constants.UPDATE_INFO_WILDCARD))
		{
			Log.d(TAG, "Wildcard is used for Theme Updates");
			themeInfos = new ThemeInfo();
			WildcardUsed = true;
		}
		
		//Get the actual Rom Updateserver URL
		try
		{
			URI RomUpdateServerUri = URI.create(mPreferences.getRomUpdateFileURL());
			HttpUriRequest romReq = new HttpGet(RomUpdateServerUri);
			romReq.addHeader("Cache-Control", "no-cache");
			HttpResponse romResponse = romHttpClient.execute(romReq);
			int romServerResponse = romResponse.getStatusLine().getStatusCode();
			if (romServerResponse != 200)
			{
				Log.e(TAG, "Server returned status code for ROM " + romServerResponse);
				romException = true;
			}
			if (!romException)
				romResponseEntity = romResponse.getEntity();
		}
		catch (IllegalArgumentException e)
		{
			Log.d(TAG, "Rom Update URI wrong: " + mPreferences.getRomUpdateFileURL());
			romException = true;
		}
		
		//Get the actual Theme Updateserver URL
		if(ThemeUpdateUrlSet)
		{
			try
			{
				URI ThemeUpdateServerUri = URI.create(mPreferences.getThemeUpdateFileURL());
				HttpUriRequest themeReq = new HttpGet(ThemeUpdateServerUri);
				themeReq.addHeader("Cache-Control", "no-cache");
				HttpResponse themeResponse = themeHttpClient.execute(themeReq);
				int themeServerResponse = themeResponse.getStatusLine().getStatusCode();
				if (themeServerResponse != 200)
				{
					Log.e(TAG, "Server returned status code for Themes " + themeServerResponse);
					themeException = true;
				}
				if(!themeException)
					themeResponseEntity = themeResponse.getEntity();
			}
			catch (IllegalArgumentException e)
			{
				Log.d(TAG, "Theme Update URI wrong: " + mPreferences.getThemeUpdateFileURL());
				themeException = true;
			}
		}
		
		try
		{
			if (!romException)
			{
				//Read the Rom Infos
				BufferedReader romLineReader = new BufferedReader(new InputStreamReader(romResponseEntity.getContent()),2 * 1024);
				StringBuffer romBuf = new StringBuffer();
				String romLine;
				while ((romLine = romLineReader.readLine()) != null)
				{
					romBuf.append(romLine);
				}
				romLineReader.close();
	
				LinkedList<UpdateInfo> romUpdateInfos = parseJSON(romBuf);
				retValue.roms = getRomUpdates(romUpdateInfos);
			}
			else
				Log.d(TAG, "There was an Exception on Downloading the Rom JSON File");
			if (!themeException && ThemeUpdateUrlSet)
			{
				//Read the Theme Infos
				BufferedReader themeLineReader = new BufferedReader(new InputStreamReader(themeResponseEntity.getContent()),2 * 1024);
				StringBuffer themeBuf = new StringBuffer();
				String themeLine;
				while ((themeLine = themeLineReader.readLine()) != null)
				{
					themeBuf.append(themeLine);
				}
				themeLineReader.close();
				
				LinkedList<UpdateInfo> themeUpdateInfos = parseJSON(themeBuf);
				retValue.themes = getThemeUpdates(themeUpdateInfos);
			}
			else
				Log.d(TAG, "There was an Exception on Downloading the Theme JSON File");
		}
		finally
		{
			if (romResponseEntity != null)
				romResponseEntity.consumeContent();
			if (themeResponseEntity != null)
				themeResponseEntity.consumeContent();
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
			JSONArray mirrorList = mainJSONObject.getJSONArray(Constants.JSON_MIRROR_LIST);
			JSONArray updateList = mainJSONObject.getJSONArray(Constants.JSON_UPDATE_LIST);

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
			String[] Boards = obj.getString(Constants.JSON_BOARD).split("\\|");
			for(String item:Boards)
			{
				if(item!=null)
					ui.board.add(item);
			}
			ui.type = obj.getString(Constants.JSON_TYPE);
			ui.mod = new LinkedList<String>();
			String[] mods= obj.getString(Constants.JSON_MOD).split("\\|");
			for(String mod:mods)
			{
				if(mod!=null)
					ui.mod.add(mod);
			}
			ui.name = obj.getString(Constants.JSON_NAME);
			ui.version = obj.getString(Constants.JSON_VERSION);
			ui.description = obj.getString(Constants.JSON_DESCRIPTION);
			ui.branchCode = obj.getString(Constants.JSON_BRANCH);
			ui.fileName = obj.getString(Constants.JSON_FILENAME);
			
			ui.updateFileUris = new LinkedList<URI>();

			for (int i = 0, max = mirrorList.length() ; i < max ; i++)
			{
				try
				{
					ui.updateFileUris.add(new URI(mirrorList.getString(i) + ui.fileName));
				}
				catch (URISyntaxException e)
				{
					Log.w(TAG, "Unable to parse mirror url (" + mirrorList.getString(i) + ui.fileName + "). Ignoring this mirror", e);
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

		if (ui.branchCode.equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_EXPERIMENTAL))
		{
			if (experimentalAllowed == true)
				allow = true;
		}
		else
		{
			allow = true;
		}

		return allow;
	}

	private boolean boardMatches(UpdateInfo ui, String systemMod)
	{
		if(ui == null) return false;
		//If * is provided, all Boards are supported
		if(ui.board.equals(Constants.UPDATE_INFO_WILDCARD) || systemMod.equals(Constants.UPDATE_INFO_WILDCARD)) return true;
		//Log.d(TAG, "BoardString:" + ui.board + "; System Mod:" + systemMod);
		for(String board:ui.board)
		{
			if(board.equalsIgnoreCase(systemMod) || board.equalsIgnoreCase(Constants.UPDATE_INFO_WILDCARD))
				return true;
		}
		return false;
	}
	
	private boolean romMatches(UpdateInfo ui, String systemRom)
	{
		if(ui == null) return false;
		if(ui.mod.equals(Constants.UPDATE_INFO_WILDCARD) || systemRom.equals(Constants.UPDATE_INFO_WILDCARD)) return true;
		//Log.d(TAG, "ThemeRom:" + ui.mod + "; SystemRom:" + systemRom);
		for(String mod:ui.mod)
		{
			if(mod.equalsIgnoreCase(systemRom) || mod.equalsIgnoreCase(Constants.UPDATE_INFO_WILDCARD))
				return true;
		}
		return false;
	}

	private boolean updateIsNewer(UpdateInfo ui, int[] sysVersion, boolean defaultValue)
	{	
		String[] updateVersion = ui.version.split("\\.");

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
	
	private LinkedList<UpdateInfo> getRomUpdates(LinkedList<UpdateInfo> updateInfos)
	{
		LinkedList<UpdateInfo> ret = new LinkedList<UpdateInfo>();
		for (int i = 0, max = updateInfos.size() ; i < max ; i++)
		{
			UpdateInfo ui = updateInfos.poll();
			if (ui.type.equalsIgnoreCase(Constants.UPDATE_INFO_TYPE_ROM))
			{
				if (boardMatches(ui, systemMod))
				{
					if(showAllUpdatesRom || updateIsNewer(ui, sysVersion, true))
					{
						if (branchMatches(ui, allowExperimentalRom))
						{
							Log.d(TAG, "Adding Rom: " + ui.name + " Version: " + ui.version + " Filename: " + ui.fileName);
							ret.add(ui);
						}
						else
						{
							Log.d(TAG, "Discarding Rom " + ui.name + " (Branch mismatch - stable/experimental)");
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
			else
			{
				Log.d(TAG, String.format("Discarding Rom %s Version %s", ui.name, ui.version));
			}
		}
		return ret;
	}
	
	private LinkedList<UpdateInfo> getThemeUpdates(LinkedList<UpdateInfo> updateInfos)
	{
		LinkedList<UpdateInfo> ret = new LinkedList<UpdateInfo>();
		for (int i = 0, max = updateInfos.size() ; i < max ; i++)
		{
			UpdateInfo ui = updateInfos.poll();
			
			//Theme installed and in correct format?
			if (themeInfos != null)
			{
				//Json object is a theme
				if (ui.type.equalsIgnoreCase(Constants.UPDATE_INFO_TYPE_THEME))
				{
					//Rom matches (must also match, if there is a * in the themes.theme file, or the file does not exist)
					if (romMatches(ui, systemRom))
					{
						//Name matches or is *
						if (WildcardUsed || showAllUpdatesTheme || (themeInfos.name != null && themeInfos.name != "" && ui.name.equalsIgnoreCase(themeInfos.name)))
						{
							//Version matches or name is *. If *, display all Versions
							if(WildcardUsed || showAllUpdatesTheme || updateIsNewer(ui, mPreferences.convertVersionToIntArray(themeInfos.version), true))
							{
								//Branch matches
								if (branchMatches(ui, allowExperimentalTheme))
								{
									Log.d(TAG, "Adding Theme: " + ui.name + " Version: " + ui.version + " Filename: " + ui.fileName);
									ret.add(ui);
								}
								else
								{
									Log.d(TAG, String.format("Discarding Theme (branch mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos.name, themeInfos.version, ui.name, ui.version));
								}
							}
							else
							{
								Log.d(TAG, String.format("Discarding Theme (Version mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos.name, themeInfos.version, ui.name, ui.version));
							}
						}
						else
						{
							Log.d(TAG, String.format("Discarding Theme (name mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos.name, themeInfos.version, ui.name, ui.version));
						}
					}
					else
					{
						Log.d(TAG, String.format("Discarding Theme (rom mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.name, themeInfos.name, themeInfos.version, ui.name, ui.version));
					}
				}
				else
				{
					Log.d(TAG, String.format("Discarding Update(not a Theme) %s Version %s", ui.name, ui.version));
				}
			}
			else
			{
				Log.d(TAG, String.format("Discarding Theme %s Version %s. Invalid or no Themes installed", ui.name, ui.version));
			}
		}
		return ret;
	}
}