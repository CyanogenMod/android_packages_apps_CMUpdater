package cmupdater.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import cmupdater.utils.Preferences;
import cmupdater.utils.SysUtils;

import android.content.Context;
import android.util.Log;

public class PlainTextUpdateServer implements IUpdateServer {

	private static final String TAG = "PlainTextUpdateServer";

	private HttpClient httpClient;
	private URI mUpdateServerUri;
	private Preferences mPreferences;

	public PlainTextUpdateServer(URI updateServerUri, Context ctx) {
		httpClient = new DefaultHttpClient();
		mUpdateServerUri = updateServerUri;
		Preferences p = mPreferences = Preferences.getPreferences(ctx);
		String sm = p.getConfiguredModString();

		if(sm == null) {
			Log.e(TAG, "Unable to determine System's Mod version. Updater will show all available updates");
		} else {
			Log.i(TAG, "System's Mod version:" + sm);
		}
	}
	public List<UpdateInfo> getAvailableUpdates() throws IOException {

		String systemMod = mPreferences.getConfiguredModString();
		HttpUriRequest req = new HttpGet(mUpdateServerUri);
		req.addHeader("Cache-Control", "no-cache");

		HttpResponse response = httpClient.execute(req);

		int serverResponse = response.getStatusLine().getStatusCode();
		if (serverResponse != 200) {
			Log.e(TAG, "Server returned status code " + serverResponse);
			throw new IOException("Server returned status code "
					+ serverResponse);
		}

		LinkedList<UpdateInfo> retValue = new LinkedList<UpdateInfo>();
		HttpEntity responseEntity = response.getEntity();
		

		try {
			
			/**
			 * Read Entity into BufferedReader and eventually into a StringBuffer
			 */
			
			BufferedReader lineReader = new BufferedReader(
					new InputStreamReader(responseEntity.getContent()),
					2 * 1024);
			
			StringBuffer buf = new StringBuffer();
			String line;

			while ((line = lineReader.readLine()) != null) {
				buf.append(line);
			}

			lineReader.close();
			
			
			/**
			 * Parse StringBuffer (JSON Document)
			 */

			LinkedList<UpdateInfo> updateInfos = parseJSON(buf);
			for (int i = 0, max = updateInfos.size() ; i < max ; i++) {
				UpdateInfo ui = updateInfos.poll();
				if (modMatches(ui, systemMod)) {
					if(mPreferences.showDowngrades() || updateIsNewer(ui, true)) {
						if (branchMatches(ui, mPreferences.allowExperimental())) {
							retValue.add(ui);
						}
					} else {
						Log.d(TAG, "Discarding " + ui.name + " (older version)");
					}
				} else {
					Log.d(TAG, "Discarding " + ui.name + " (mod mismatch)");
				}
			}

		} finally {
			responseEntity.consumeContent();
		}

		return retValue;
	}

	private LinkedList<UpdateInfo> parseJSON(StringBuffer buf) {
		//mod|versionCode|displayVersion|displayName|md5sum|mirror1|mirror2|mirror3|mirror4|etc
		LinkedList<UpdateInfo> uis = new LinkedList<UpdateInfo>();

		JSONObject mainJSONObject;
		try {
			mainJSONObject = new JSONObject(buf.toString());
			JSONArray updateList = mainJSONObject.getJSONArray("UpdateList");

			for (int i = 0, max = updateList.length() ; i < max ; i++) {
				uis.add(parseUpdateJSONObject(updateList.getJSONObject(i)));
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return uis;
	}

	private UpdateInfo parseUpdateJSONObject(JSONObject obj) {
		UpdateInfo ui = new UpdateInfo();

		try {
			ui.mod = obj.getString("mod");
			ui.branchCode = obj.getString("branch");
			ui.displayVersion = obj.getString("version");
			ui.name = obj.getString("name");
			ui.description = obj.getString("description");
			ui.type = obj.getString("type");
			ui.updateFileUris = new LinkedList<URI>();
			JSONArray mirrors = obj.getJSONArray("mirrors");

			for (int i = 0, max = mirrors.length() ; i < max ; i++) {
				try {
					ui.updateFileUris.add(new URI(mirrors.getString(i)));
				} catch (URISyntaxException e) {
					Log.w(TAG, "Unable to parse mirror url (" + mirrors.getString(i)
							+ "). Ignoring this mirror", e);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ui;
	}

	private boolean branchMatches(UpdateInfo ui, boolean experimentalAllowed ) {
		if(ui == null) return false;

		boolean allow = false;

		if (ui.branchCode.charAt(0) == 'X') {
			if (experimentalAllowed == true)
				allow = true;
		} else {
			allow = true;
		}

		Log.d(TAG, "Update Branch:" + ui.branchCode + "; Experimental Allowed:" + experimentalAllowed);
		return allow;
	}

	private boolean modMatches(UpdateInfo ui, String systemMod) {
		if(ui == null) return false;

		Log.d(TAG, "Update Mod:" + ui.mod + "; System Mod:" + systemMod);

		if(ui.mod.equals("*") || systemMod.equals("*")) return true;

		return ui.mod.equals(systemMod);
	}

	private boolean updateIsNewer(UpdateInfo ui, boolean defaultValue) {
		/*
		String modVersion = Preferences.getSystemProperty(Preferences.SYS_PROP_MOD_VERSION);
		if(modVersion == null || modVersion.length() < PROP_MOD_VERSION_SKIP_CHARS) return defaultValue;

		String[] sysVersion = modVersion.substring(PROP_MOD_VERSION_SKIP_CHARS).split("\\.");*/

		int[] sysVersion = SysUtils.getSystemModVersion();
		String[] updateVersion = ui.displayVersion.split("\\.");

		Log.d(TAG, "Update Version:" + Arrays.toString(updateVersion) + "; System Version:" + Arrays.toString(sysVersion));

		int sys, update;
		int max = Math.min(sysVersion.length, updateVersion.length);
		for(int i = 0; i < max; i++) {
			try{
				sys = sysVersion[i];
				update = Integer.parseInt(updateVersion[i]);

				if(sys != update) return sys < update; 

			} catch (NumberFormatException ex) {
				Log.d(TAG, "NumberFormatException while parsing version values:", ex);
				return defaultValue;
			}
		}

		return sysVersion.length < updateVersion.length;
	}
}
