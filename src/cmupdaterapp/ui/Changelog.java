package cmupdaterapp.ui;

import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.util.Log;
public class Changelog
{
	private static final String TAG = "<CM-Updater> Changelog";
	
	static List<Version> getChangelog(IUpdateProcessInfo upi)
	{
		InputStream is = upi.getResources().openRawResource(R.raw.changelog);
		InputSource i = new InputSource(is);
        try
        {
        	SAXParserFactory spf = SAXParserFactory.newInstance(); 
        	SAXParser sp = spf.newSAXParser();
        	XMLReader xr = sp.getXMLReader(); 
        	ChangelogHandler ch = new ChangelogHandler(); 
        	xr.setContentHandler(ch); 
        	xr.parse(i);  
        	return ch.getParsedData(); 
        }
        catch (Exception e)
        {
        	Log.e(TAG, "Exception in Reading ChangelogXMLFile", e);
        	return null;
        }
	}
}