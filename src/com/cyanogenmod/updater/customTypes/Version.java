package com.cyanogenmod.updater.customTypes;

import java.util.LinkedList;
import java.util.List;

public class Version {
    public String Version;
    public final List<String> ChangeLogText;

    public Version() {
        Version = "";
        ChangeLogText = new LinkedList<String>();
    }
}
