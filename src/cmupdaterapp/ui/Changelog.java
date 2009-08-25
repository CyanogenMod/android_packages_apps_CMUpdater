package cmupdaterapp.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import cmupdaterapp.service.UpdateInfo;
import cmupdaterapp.utils.Preferences;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Changelog extends Activity
{
	private static final String TAG = "<CM-Updater> Changelog";
	static List<Version> r = null;
	static Preferences p;
	public static Thread t;
	
	static List<Version> getAppChangelog(final IUpdateProcessInfo upi)
	{ 
		p = Preferences.getPreferences(upi);

		t = new Thread()
		{
			public void run()
			{
				URL url;
				InputSource i;
				
				Message m = null;
				try
				{
					m = new Message();
					url = new URL(p.getChangelogURL());
					i = new InputSource(url.openStream());
		        	SAXParserFactory spf = SAXParserFactory.newInstance(); 
		        	SAXParser sp = spf.newSAXParser();
		        	XMLReader xr = sp.getXMLReader(); 
		        	ChangelogHandler ch = new ChangelogHandler(); 
		        	xr.setContentHandler(ch); 
		        	xr.parse(i);  
		        	m.obj = ch.getParsedData();
		        }
		        catch (MalformedURLException e)
				{
		        	m.obj = e.toString();
					Log.e(TAG, "Malformed URL!", e);
				}
				catch (IOException e)
				{
					m.obj = e.toString();
					Log.e(TAG, "Exception on opening Input Stream", e);
				}
		        catch (Exception e)
		        {
		        	m.obj = e.toString();
		        	Log.e(TAG, "Exception in Reading ChangelogXMLFile", e);
		        }
		        handler.sendMessage(m);
			}
			private Handler handler = new Handler()
			{
				@SuppressWarnings("unchecked")
				public void handleMessage(Message msg)
				{
					if (UpdateProcessInfo.pd != null)
						UpdateProcessInfo.pd.dismiss();
					if (msg.obj instanceof String)
					{
						Toast.makeText(upi, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
						r = null;
					}
					else if (msg.obj instanceof List<?>)
						r = (List<Version>) msg.obj;
					Thread.currentThread().interrupt();
		        }
		    };
      };
      t.start();
      return r;
	}

	static List<Version> getRomChangelog(UpdateInfo ui)
	{
		Version v = new Version();
		List<Version> returnValue = new LinkedList<Version>();
		v.Version = ui.displayVersion;
		for (String str : ui.description.split("\\|"))
		{
			if(str != "")
				v.ChangeLogText.add(str);
		}
		returnValue.add(v);
		return returnValue;
	}
}