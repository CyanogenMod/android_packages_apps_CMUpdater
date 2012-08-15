package com.cyanogenmod.updater.featuredThemes;

import com.cyanogenmod.updater.customTypes.FullThemeList;
import com.cyanogenmod.updater.customTypes.ThemeList;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.misc.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URI;

class FeaturedThemesHandler extends DefaultHandler {
	private static final String TAG = "FeaturedThemesHandler";

	private FullThemeList fullThemeList;
	private ThemeList currentTheme;
	private boolean error = false;

    public FullThemeList getParsedData() {
         return this.fullThemeList;
    }

	@Override
    public void startDocument() throws SAXException {
		this.fullThemeList = new FullThemeList();
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
        if (localName.equalsIgnoreCase(Constants.FEATURED_THEMES_TAG)) {
			//New Theme. Start a new Object
			currentTheme = new ThemeList();
			currentTheme.featured = true;
			currentTheme.enabled = true;
			if (atts.getValue(Constants.FEATURES_THEMES_TAG_NAME) == null)
				error = true;
			else
				currentTheme.name = atts.getValue(Constants.FEATURES_THEMES_TAG_NAME).trim();
			if (atts.getValue(Constants.FEATURES_THEMES_TAG_URI) == null)
				error = true;
			else
				currentTheme.url = URI.create(atts.getValue(Constants.FEATURES_THEMES_TAG_URI).trim());
		}
	}

    /**
     * Gets be called on closing tags like:
     * </tag>
     */
	@Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equalsIgnoreCase(Constants.FEATURED_THEMES_TAG)) {
            if (!error)
				fullThemeList.addThemeToList(currentTheme);
			else
				Log.e(TAG, "There was an error in the XML File. A value was NULL");
			currentTheme = null;
		}
	}
}