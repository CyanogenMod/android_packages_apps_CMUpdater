package cmupdaterapp.misc;

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
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customTypes.ThemeInfo;
import cmupdaterapp.customTypes.ThemeList;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.interfaces.IUpdateCheckHelper;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.misc.Log;
import cmupdaterapp.misc.State;
import cmupdaterapp.ui.R;
import cmupdaterapp.utils.Preferences;
import cmupdaterapp.utils.StringUtils;
import cmupdaterapp.utils.SysUtils;

import android.content.Context;
import android.content.res.Resources;

public class UpdateCheckHelper implements IUpdateCheckHelper
{
	private static final String TAG = "UpdateCheckHelper";

	private Preferences mPreferences;
	private Resources res;

	private String systemMod;
	private String systemRom;
	private ThemeInfo themeInfos;
	
	private boolean showExperimentalRomUpdates;
	private boolean showAllRomUpdates;
	private boolean showExperimentalThemeUpdates;
	private boolean showAllThemeUpdates;
	
	private boolean WildcardUsed = false;
	
	private Context context;
	
	private int PrimaryKeyTheme = -1;
	
	public UpdateCheckHelper(Context ctx)
	{
		Preferences p = mPreferences = Preferences.getPreferences(ctx);
		systemMod = p.getBoardString();
		context = ctx;
		res = context.getResources();

		if(systemMod == null)
		{
			Log.d(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
		}
		else
		{
			Log.d(TAG, "System's Mod version:" + systemMod);
		}
	}
	public FullUpdateInfo getAvailableUpdates() throws IOException
	{
		FullUpdateInfo retValue = new FullUpdateInfo();
		boolean romException = false;
		HttpClient romHttpClient = new DefaultHttpClient();
		HttpClient themeHttpClient = new DefaultHttpClient();
		HttpEntity romResponseEntity = null;
		HttpEntity themeResponseEntity = null;
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
			if (romServerResponse != HttpStatus.SC_OK)
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
					PrimaryKeyTheme = -1;
					Log.d(TAG, "Trying to download ThemeInfos for " + t.url.toString());
					URI ThemeUpdateServerUri = t.url;
					HttpUriRequest themeReq = new HttpGet(ThemeUpdateServerUri);
					themeReq.addHeader("Cache-Control", "no-cache");
					try
					{
						HttpResponse themeResponse = themeHttpClient.execute(themeReq);
						int themeServerResponse = themeResponse.getStatusLine().getStatusCode();
						if (themeServerResponse != HttpStatus.SC_OK)
						{
							Log.d(TAG, "Server returned status code for Themes " + themeServerResponse);
							themeResponseEntity = themeResponse.getEntity();
							continue;
						}
						themeResponseEntity = themeResponse.getEntity();
					}
					catch (IOException ex)
					{
						//when theres an Exception Downloading the Theme, continue
						Exceptions.add(res.getString(R.string.theme_download_exception) + t.name + ": " + ex.getMessage());
						Log.e(TAG, "There was an error downloading Theme " + t.name + ": ", ex);
						continue;
					}
					//Read the Theme Infos
					BufferedReader themeLineReader = new BufferedReader(new InputStreamReader(themeResponseEntity.getContent()),2 * 1024);
					StringBuffer themeBuf = new StringBuffer();
					String themeLine;
					while ((themeLine = themeLineReader.readLine()) != null)
					{
						themeBuf.append(themeLine);
					}
					themeLineReader.close();
					
					//Set the PrimaryKey for the Database
					if (t.PrimaryKey > 0)
						PrimaryKeyTheme = t.PrimaryKey;
					
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
				PrimaryKeyTheme = -1;
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
			if (PrimaryKeyTheme > 0)
				ui.PrimaryKey = PrimaryKeyTheme;

			ui.board = new LinkedList<String>();
			String[] Boards = obj.getString(Constants.JSON_BOARD).split("\\|");
			for(String item:Boards)
			{
				if(item!=null)
					ui.board.add(item.trim());
			}
			ui.setType(obj.getString(Constants.JSON_TYPE).trim());
			ui.mod = new LinkedList<String>();
			String[] mods= obj.getString(Constants.JSON_MOD).split("\\|");
			for(String mod:mods)
			{
				if(mod!=null)
					ui.mod.add(mod.trim());
			}
			ui.setName(obj.getString(Constants.JSON_NAME).trim());
			ui.setVersion(obj.getString(Constants.JSON_VERSION).trim());
			ui.setDescription(obj.getString(Constants.JSON_DESCRIPTION).trim());
			ui.setBranchCode(obj.getString(Constants.JSON_BRANCH).trim());
			ui.setFileName(obj.getString(Constants.JSON_FILENAME).trim());
			
			ui.updateFileUris = new LinkedList<URI>();

			for (int i = 0, max = mirrorList.length() ; i < max ; i++)
			{
				try
				{
					if (!mirrorList.isNull(i))
						ui.updateFileUris.add(new URI(mirrorList.getString(i).trim() + ui.getFileName()));
					else
						Log.d(TAG, "Theres an error in your JSON File. Maybe a , after the last mirror");
				}
				catch (URISyntaxException e)
				{
					Log.e(TAG, "Unable to parse mirror url (" + mirrorList.getString(i) + ui.getFileName() + "). Ignoring this mirror", e);
				}
			}
			
			//Screenshots (only Themes)
			ui.screenshots = new LinkedList<URI>();
			//Only if there is a Screenshot Array in the JSON
			if (obj.has(Constants.JSON_SCREENSHOTS))
			{
				JSONArray screenshots = obj.getJSONArray(Constants.JSON_SCREENSHOTS);
				if (screenshots != null && screenshots.length() > 0)
				{
					for (int screenshotcounter = 0; screenshotcounter < screenshots.length(); screenshotcounter++)
					{
						try
						{
							if (!screenshots.isNull(screenshotcounter))
								ui.screenshots.add(new URI(screenshots.getString(screenshotcounter)));
							else
								Log.d(TAG, "Theres an error in your JSON File. Maybe a , after the last screenshot");
						}
						catch (URISyntaxException e)
						{
							Log.e(TAG, "Unable to parse Screenshot url (" + screenshots.getString(screenshotcounter) + ") Theme: " + ui.getName() + ". Ignoring this Screenshot", e);
						}
					}
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

		if (ui.getBranchCode().equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_EXPERIMENTAL))
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
			if (ui.getType().equalsIgnoreCase(Constants.UPDATE_INFO_TYPE_ROM))
			{
				if (boardMatches(ui, systemMod))
				{
					if(showAllRomUpdates || StringUtils.compareVersions(Constants.RO_MOD_START_STRING + ui.getVersion(), systemRom))
					{
						if (branchMatches(ui, showExperimentalRomUpdates))
						{
							Log.d(TAG, "Adding Rom: " + ui.getName() + " Version: " + ui.getVersion() + " Filename: " + ui.getFileName());
							ret.add(ui);
						}
						else
						{
							Log.d(TAG, "Discarding Rom " + ui.getName() + " (Branch mismatch - stable/experimental)");
						}
					}
					else
					{
						Log.d(TAG, "Discarding Rom " + ui.getName() + " (older version)");
					}
				}
				else
				{
					Log.d(TAG, "Discarding Rom " + ui.getName() + " (mod mismatch)");
				}
			}
			else
			{
				Log.d(TAG, String.format("Discarding Rom %s Version %s", ui.getName(), ui.getVersion()));
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
				if (ui.getType().equalsIgnoreCase(Constants.UPDATE_INFO_TYPE_THEME))
				{
					//Rom matches (must also match, if there is a * in the themes.theme file, or the file does not exist)
					if (romMatches(ui, systemRom))
					{
						//Name matches or is *
						if (WildcardUsed || showAllThemeUpdates || (themeInfos.name != null && themeInfos.name != "" && ui.getName().equalsIgnoreCase(themeInfos.name)))
						{
							//Version matches or name is *. If *, display all Versions
							if(WildcardUsed || showAllThemeUpdates || StringUtils.compareVersions(ui.getVersion(), themeInfos.version))
							{
								//Branch matches
								if (branchMatches(ui, showExperimentalThemeUpdates))
								{
									Log.d(TAG, "Adding Theme: " + ui.getName() + " Version: " + ui.getVersion() + " Filename: " + ui.getFileName());
									ret.add(ui);
								}
								else
								{
									Log.d(TAG, String.format("Discarding Theme (branch mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.getName(), themeInfos.name, themeInfos.version, ui.getName(), ui.getVersion()));
								}
							}
							else
							{
								Log.d(TAG, String.format("Discarding Theme (Version mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.getName(), themeInfos.name, themeInfos.version, ui.getName(), ui.getVersion()));
							}
						}
						else
						{
							Log.d(TAG, String.format("Discarding Theme (name mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.getName(), themeInfos.name, themeInfos.version, ui.getName(), ui.getVersion()));
						}
					}
					else
					{
						Log.d(TAG, String.format("Discarding Theme (rom mismatch) %s: Your Theme: %s %s; From JSON: %s %s", ui.getName(), themeInfos.name, themeInfos.version, ui.getName(), ui.getVersion()));
					}
				}
				else
				{
					Log.d(TAG, String.format("Discarding Update(not a Theme) %s Version %s", ui.getName(), ui.getVersion()));
				}
			}
			else
			{
				Log.d(TAG, String.format("Discarding Theme %s Version %s. Invalid or no Themes installed", ui.getName(), ui.getVersion()));
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