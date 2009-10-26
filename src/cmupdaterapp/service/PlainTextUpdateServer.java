package cmupdaterapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
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

import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customTypes.ThemeInfo;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IUpdateServer;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.misc.State;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.SysUtils;

import android.content.Context;

public class PlainTextUpdateServer implements IUpdateServer
{
	private static final String TAG = "PlainTextUpdateServer";

	private Preferences mPreferences;

	private String systemMod;
	private String systemRom;
	private ThemeInfo themeInfos;
	
	private boolean showExperimentalRomUpdates;
	private boolean showAllRomUpdates;
	private boolean showExperimentalThemeUpdates;
	private boolean showAllThemeUpdates;
	
	private boolean WildcardUsed = false;
	
	private Context context;
	
	public PlainTextUpdateServer(Context ctx)
	{
		Preferences p = mPreferences = Preferences.getPreferences(ctx);
		String sm = p.getBoardString();
		context = ctx;

		if(sm == null)
		{
			Log.d(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
		}
		else
		{
			Log.d(TAG, "System's Mod version:" + sm);
		}
	}
	public FullUpdateInfo getAvailableUpdates() throws IOException
	{
		FullUpdateInfo retValue = new FullUpdateInfo();
		boolean romException = false;
		//boolean themeException = false;
		HttpClient romHttpClient = new DefaultHttpClient();
		HttpClient themeHttpClient = new DefaultHttpClient();
		HttpEntity romResponseEntity = null;
		HttpEntity themeResponseEntity = null;
		systemMod = mPreferences.getBoardString();
		systemRom = SysUtils.getModVersion();
		themeInfos = mPreferences.getThemeInformations();
		showExperimentalRomUpdates = mPreferences.showExperimentalRomUpdates();
		showAllRomUpdates = mPreferences.showAllRomUpdates();
		showExperimentalThemeUpdates = mPreferences.showExperimentalThemeUpdates();
		showAllThemeUpdates = mPreferences.showAllThemeUpdates();
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
				Log.d(TAG, "Server returned status code for ROM " + romServerResponse);
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
				LinkedList<ThemeList> tl = mPreferences.getThemeUpdateUrls();
				for(ThemeList t:tl)
				{
					if(!t.enabled)
					{
						Log.d(TAG, "Theme " + t.name + " disabled. Continuing");
						continue;
					}
					Log.d(TAG, "Trying to download ThemeInfos for " + t.url.toString());
					URI ThemeUpdateServerUri = t.url;
					HttpUriRequest themeReq = new HttpGet(ThemeUpdateServerUri);
					themeReq.addHeader("Cache-Control", "no-cache");
					HttpResponse themeResponse = themeHttpClient.execute(themeReq);
					int themeServerResponse = themeResponse.getStatusLine().getStatusCode();
					if (themeServerResponse != 200)
					{
						Log.d(TAG, "Server returned status code for Themes " + themeServerResponse);
						//themeException = true;
						themeResponseEntity = themeResponse.getEntity();
						continue;
					}
					themeResponseEntity = themeResponse.getEntity();
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
					retValue.themes.addAll(getThemeUpdates(themeUpdateInfos));
				}
			}
			catch (IllegalArgumentException e)
			{
				Log.d(TAG, "Theme Update URI wrong");
				//themeException = true;
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
		}
		finally
		{
			if (romResponseEntity != null)
				romResponseEntity.consumeContent();
			if (themeResponseEntity != null)
				themeResponseEntity.consumeContent();
		}

		FullUpdateInfo ful = FilterUpdates(retValue, State.loadState(context));
		if(!romException)
			State.saveState(context, retValue);
		return ful;
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
			
			Log.d(TAG, "Found "+mirrorList.length()+" mirrors in the JSON");
			Log.d(TAG, "Found "+updateList.length()+" updates in the JSON");
			
			for (int i = 0, max = updateList.length() ; i < max ; i++)
			{
				if(!updateList.isNull(i))
					uis.add(parseUpdateJSONObject(updateList.getJSONObject(i),mirrorList));
				else
					Log.d(TAG, "Theres an error in your JSON File. Maybe a , after the last update");
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
					ui.board.add(item.trim());
			}
			ui.type = obj.getString(Constants.JSON_TYPE).trim();
			ui.mod = new LinkedList<String>();
			String[] mods= obj.getString(Constants.JSON_MOD).split("\\|");
			for(String mod:mods)
			{
				if(mod!=null)
					ui.mod.add(mod.trim());
			}
			ui.name = obj.getString(Constants.JSON_NAME).trim();
			ui.version = obj.getString(Constants.JSON_VERSION).trim();
			ui.description = obj.getString(Constants.JSON_DESCRIPTION).trim();
			ui.branchCode = obj.getString(Constants.JSON_BRANCH).trim();
			ui.fileName = obj.getString(Constants.JSON_FILENAME).trim();
			
			ui.updateFileUris = new LinkedList<URI>();

			for (int i = 0, max = mirrorList.length() ; i < max ; i++)
			{
				try
				{
					if (!mirrorList.isNull(i))
						ui.updateFileUris.add(new URI(mirrorList.getString(i).trim() + ui.fileName));
					else
						Log.d(TAG, "Theres an error in your JSON File. Maybe a , after the last mirror");
				}
				catch (URISyntaxException e)
				{
					Log.e(TAG, "Unable to parse mirror url (" + mirrorList.getString(i) + ui.fileName + "). Ignoring this mirror", e);
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
		for(String mod:ui.mod)
		{
			if(mod.equalsIgnoreCase(systemRom) || mod.equalsIgnoreCase(Constants.UPDATE_INFO_WILDCARD))
				return true;
		}
		return false;
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
					if(showAllRomUpdates || SysUtils.StringCompare(systemRom, Constants.RO_MOD_START_STRING + ui.version))
					{
						if (branchMatches(ui, showExperimentalRomUpdates))
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
						if (WildcardUsed || showAllThemeUpdates || (themeInfos.name != null && themeInfos.name != "" && ui.name.equalsIgnoreCase(themeInfos.name)))
						{
							//Version matches or name is *. If *, display all Versions
							if(WildcardUsed || showAllThemeUpdates || SysUtils.StringCompare(themeInfos.version, ui.version))
							{
								//Branch matches
								if (branchMatches(ui, showExperimentalThemeUpdates))
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
	
	@SuppressWarnings("unchecked")
	private static FullUpdateInfo FilterUpdates(FullUpdateInfo newList, FullUpdateInfo oldList)
	{
		Log.d(TAG, "Called FilterUpdates");
		FullUpdateInfo ful = new FullUpdateInfo();
		ful.roms = (LinkedList<UpdateInfo>) newList.roms.clone();
		ful.themes = (LinkedList<UpdateInfo>) newList.themes.clone();
		ful.roms.removeAll(oldList.roms);
		ful.themes.removeAll(oldList.themes);
		return ful;
	}
}