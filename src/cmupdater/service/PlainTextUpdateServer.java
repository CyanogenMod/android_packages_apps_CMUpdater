package cmupdater.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


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
	
	private static final int MOD = 0;
	private static final int BRANCH_CODE= 1;
	private static final int DISPLAY_VERSION = 2;
	private static final int DISPLAY_NAME = 3;
	private static final int MD5SUM = 4;
	private static final int FIRST_MIRROR = 5;
	
	
	private static final String TAG = "PlainTextUpdateServer";

	private HttpClient httpClient;
	private URI mUpdateServerUri;
	private Preferences mPreferences;
	//private String mSystemMod;

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
		String line;
		UpdateInfo ui;
		try {
			BufferedReader lineReader = new BufferedReader(
					new InputStreamReader(responseEntity.getContent()),
					2 * 1024);

			while ((line = lineReader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				else {
					if (line.trim().charAt(0) == '#')
						continue;
				}

				ui = parseLine(line);
				if(ui == null) {
					Log.i(TAG, "Unable to parse that line:" + line);
					continue;
				}

				if (modMatches(ui, systemMod)) {
					if(mPreferences.showDowngrades() || updateIsNewer(ui, true)) {
						if (branchMatches(ui, mPreferences.allowExperimental())) {
							retValue.add(ui);
						}
					} else {
						Log.d(TAG, "Discarding " + ui.displayName + " (older version)");
					}
				} else {
					Log.d(TAG, "Discarding " + ui.displayName + " (mod mismatch)");
				}
			}

			lineReader.close();
		} finally {
			responseEntity.consumeContent();
		}

		return retValue;
	}
	
	private UpdateInfo parseLine(String line) {
		//mod|versionCode|displayVersion|displayName|md5sum|mirror1|mirror2|mirror3|mirror4|etc
		UpdateInfo ui = new UpdateInfo();
		String[] p = line.split("\\|");

		if (p.length < FIRST_MIRROR) {
			Log.w(TAG, "Unable to parse update line. Found " + p.length
					+ " parameters, expected >=" + FIRST_MIRROR);
			return null;
		}

		ui.mod = p[MOD];
		ui.branchCode = p[BRANCH_CODE];
		ui.displayVersion = p[DISPLAY_VERSION];
		ui.displayName = p[DISPLAY_NAME];
		ui.md5 = p[MD5SUM];
		ui.updateFileUris = new LinkedList<URI>();
		for (int i = FIRST_MIRROR; i < p.length; i++) {
			try {
				ui.updateFileUris.add(new URI(p[i]));
			} catch (URISyntaxException e) {
				Log.w(TAG, "Unable to parse mirror url (" + p[i]
						+ "). Ignoring this mirror", e);
			}
		}

		if (ui.updateFileUris.size() == 0) {
			Log.w(TAG, "No mirrors found, ignoring this update");
			return null;
		}

		return ui;
	}

	private boolean branchMatches(UpdateInfo ui, boolean experimentalAllowed ) {
		if(ui == null) return false;
		
		Log.d(TAG, "Update Mod:" + ui.branchCode + "; Experimental Allowed:" + experimentalAllowed);
		
		boolean allow = false;
		
		if (ui.branchCode.charAt(0) == 'X') {
			if (experimentalAllowed == true)
				allow = true;
		}
		else {
			allow = true;
		}
		
		return allow;
	}
	
	private boolean modMatches(UpdateInfo ui, String systemMod) {
		if(ui == null) return false;
		
		Log.d(TAG, "Update Branch:" + ui.mod + "; System Mod:" + systemMod);
		
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
