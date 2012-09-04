package com.cyanogenmod.updater.tasks;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.cyanogenmod.updater.changelog.ChangelogHandler;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.customTypes.Version;
import com.cyanogenmod.updater.misc.Log;
import com.cyanogenmod.updater.ui.R;
import com.cyanogenmod.updater.utils.Preferences;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class ChangelogTask extends AsyncTask<Object, Void, List<Version>> {
    private static final String TAG = "ChangelogCheckerTask";

    private final Context mCtx;
    private String mException = null;
    private ChangelogType mChangelogType;
    private URL changelogUrl;
    private ProgressDialog d;

    public enum ChangelogType {
        ROM,
        APP
    }

    public ChangelogTask(Context ctx) {
        mCtx = ctx;
    }

    @Override
    public void onPreExecute() {
    	try {
			changelogUrl = new URL(new Preferences(mCtx).getChangelogURL());
		} catch (MalformedURLException e) {
			Log.e(TAG, "Exception on parsing URL", e);
			mException = e.toString();
		}
		Resources res = mCtx.getResources();
		d = ProgressDialog.show(mCtx, res.getString(R.string.changelog_progress_title), res.getString(R.string.changelog_progress_body), true);
    }

    @Override
    public List<Version> doInBackground(Object... params) {
        if (mException != null)
        	return null;
        if (params == null || params.length < 1) {
        	mException = "No Parameters for Changelog";
        	return null;
        }

        if (!(params[0] instanceof ChangelogType)) {
        	mException = "First Parameter not Changelogtype";
        	return null;
        }
        mChangelogType = (ChangelogType) params[0];

        if (!mChangelogType.equals(ChangelogType.APP)) {
        	//ROM Changelog
        	if (params.length != 2 || !(params[1] instanceof UpdateInfo)) {
            	mException = "Second Parameter not UpdateInfo";
            	return null;
            }
        	UpdateInfo ui = (UpdateInfo) params[1];
        	Version v = new Version();
            List<Version> returnValue = new LinkedList<Version>();
            v.Version = ui.getVersion();
            List<String> changelog = ui.getDescription();
            if (changelog != null && changelog.size() > 0) {
	            for (String str : changelog) {
                if (!str.equals(""))
                    v.ChangeLogText.add(str);
            }
            returnValue.add(v);
            }
            return returnValue;
        }

        List<Version> ret = null;

        try {
        	InputSource i = new InputSource(changelogUrl.openStream());
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            ChangelogHandler ch = new ChangelogHandler();
            xr.setContentHandler(ch);
            xr.parse(i);
            ret = ch.getParsedData();
        }
        catch (MalformedURLException e) {
        	mException = e.toString();
            Log.e(TAG, "Malformed URL!", e);
            ret = null;
        }
        catch (IOException e) {
        	mException = e.toString();
            Log.e(TAG, "Exception on opening Input Stream", e);
            ret = null;
        }
        catch (ParserConfigurationException e) {
        	mException = e.toString();
            Log.e(TAG, "Exception on parsing XML File", e);
            ret = null;
        }
        catch (SAXException e) {
        	mException = e.toString();
            Log.e(TAG, "Exception while creating SAXParser", e);
            ret = null;
        }
        return ret;
    }

    @Override
    public void onPostExecute(List<Version> result) {
    	if (mException != null) {
        	d.dismiss();
    		Toast.makeText(mCtx, mException, Toast.LENGTH_LONG);
    		return;
    	}
    	if (result == null || result.isEmpty()) {
        	d.dismiss();
        	Toast.makeText(mCtx, R.string.no_changelog_found, Toast.LENGTH_LONG).show();
            return;
    	}

        Dialog dialog = new Dialog(mCtx);
        int dialogTitle;
        switch (mChangelogType) {
            case ROM:
                dialogTitle = R.string.changelog_title_rom;
                break;
            case APP:
                dialogTitle = R.string.changelog_title_app;
                break;
            default:
            	return;
        }
        dialog.setTitle(dialogTitle);
        dialog.setContentView(R.layout.changelog);
        LinearLayout main = (LinearLayout) dialog.findViewById(R.id.ChangelogLinearMain);

        LayoutParams lp1 = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
        LayoutParams lp3 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        //Foreach Version
        for (Version v : result) {
            if (v.ChangeLogText.isEmpty()) {
                continue;
            }
            TextView versiontext = new TextView(mCtx);
            versiontext.setLayoutParams(lp1);
            versiontext.setGravity(Gravity.CENTER);
            versiontext.setTextColor(Color.RED);
            versiontext.setText("Version " + v.Version);
            versiontext.setTypeface(null, Typeface.BOLD);
            versiontext.setTextSize((versiontext.getTextSize() * (float) 1.5));
            main.addView(versiontext);
            //Foreach Changelogtext
            for (String Change : v.ChangeLogText) {
                LinearLayout l = new LinearLayout(mCtx);
                l.setLayoutParams(lp2);
                l.setGravity(Gravity.CENTER_VERTICAL);
                ImageView i = new ImageView(mCtx);
                i.setLayoutParams(lp3);
                i.setImageResource(R.drawable.icon);
                l.addView(i);
                TextView ChangeText = new TextView(mCtx);
                ChangeText.setLayoutParams(lp3);
                ChangeText.setText(Change);
                l.addView(ChangeText);
                main.addView(l);
                //Horizontal Line
                View ruler = new View(mCtx);
                ruler.setBackgroundColor(Color.WHITE);
                main.addView(ruler, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
            }
        }
    	d.dismiss();
        dialog.show();
    }

    @Override
    public void onCancelled()
    {
    	d.dismiss();
    	super.onCancelled();
    }
}
