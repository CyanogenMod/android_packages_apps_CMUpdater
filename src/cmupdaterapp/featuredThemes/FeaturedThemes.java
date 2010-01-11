package cmupdaterapp.featuredThemes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.os.Message;
import cmupdaterapp.misc.Log;
import cmupdaterapp.ui.ThemeListActivity;
import cmupdaterapp.utils.Preferences;

public class FeaturedThemes implements Runnable
{
	private static final String TAG = "FeaturedThemes";
	private Preferences p;

	public FeaturedThemes(Context ctx)
	{
		p = Preferences.getPreferences(ctx);
	}

	public void run()
	{
		URL url;
		InputSource i;
		
		Message m = ThemeListActivity.FeaturedThemesProgressHandler.obtainMessage();
		try
		{
			url = new URL(p.getFeaturedThemesURL());
			i = new InputSource(url.openStream());
        	SAXParserFactory spf = SAXParserFactory.newInstance(); 
        	SAXParser sp = spf.newSAXParser();
        	XMLReader xr = sp.getXMLReader(); 
        	FeaturedThemesHandler fth = new FeaturedThemesHandler(); 
        	xr.setContentHandler(fth); 
        	xr.parse(i);  
        	m.obj = fth.getParsedData();
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
		catch (ParserConfigurationException e)
		{
			m.obj = e.toString();
			Log.e(TAG, "Exception on parsing XML File", e);
		}
		catch (SAXException e)
		{
			m.obj = e.toString();
			Log.e(TAG, "Exception while creating SAXParser", e);
		}
		ThemeListActivity.FeaturedThemesProgressHandler.sendMessage(m);
	}
}