package com.cyanogenmod.updater.customTypes;

import java.io.Serializable;
import java.net.URI;

public class ThemeList implements Serializable, Comparable<ThemeList> {
	private static final long serialVersionUID = 8861171977383611130L;

	public int PrimaryKey;
	public String name;
	public URI url;
	public boolean enabled;
	public boolean featured;

    public ThemeList() {
		featured = false;
		enabled = true;
	}

    public int compareTo(ThemeList another) {
		return this.name.compareToIgnoreCase(another.name);
	}
}