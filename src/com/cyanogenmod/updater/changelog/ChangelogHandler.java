package com.cyanogenmod.updater.changelog;

import com.cyanogenmod.updater.customTypes.Version;
import com.cyanogenmod.updater.misc.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;
import java.util.List;

public class ChangelogHandler extends DefaultHandler {
	private List<Version> co;
	private Version currentVersion;

    public List<Version> getParsedData() {
         return this.co;
    }

    @Override
    public void startDocument() throws SAXException {
         this.co = new LinkedList<Version>();
    }

    @Override
    public void endDocument() throws SAXException {
         // Nothing to do
    }

    /**
     * Gets be called on opening tags like:
     * <tag>
     * Can provide attribute(s), when xml was like:
     * <tag attribute="attributeValue">
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equalsIgnoreCase(Constants.VERSION_TAG)) {
        	 //New Version. Start a new Object
        	 currentVersion = new Version();
             currentVersion.Version = atts.getValue(Constants.VERSION_NAME_TAG);
         }
    }

    /**
     * Gets be called on closing tags like:
     * </tag>
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equalsIgnoreCase(Constants.VERSION_TAG)) {
              //Changelog for this Version finished. Add it to the result Object
        	 co.add(currentVersion);
        	 currentVersion = null;
         }
    }

    /**
     * Gets be called on the following structure:
     * <tag>characters</tag>
     */
    @Override
    public void characters(char ch[], int start, int length) {
    	String a = new String(ch, start, length);
    	a = a.replaceAll("\\n", "");
    	a = a.replaceAll("\\r", "");
    	a = a.replaceAll("\\t", "");
    	// If the object is not initialized because of junk data in the xml like the xml starting tag and so
        if (currentVersion != null && currentVersion.ChangeLogText != null && !a.equals(""))
    		currentVersion.ChangeLogText.add(a);
    }
}