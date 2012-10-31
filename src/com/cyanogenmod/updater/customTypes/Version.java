/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

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
