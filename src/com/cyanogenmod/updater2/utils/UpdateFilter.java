package com.cyanogenmod.updater2.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filename Filter for getting only Files that matches the Given Extensions
 * Extensions can be split with |
 * Example: .zip|.md5sum
 */
public class UpdateFilter implements FilenameFilter {
    private final String[] mExtension;

    public UpdateFilter(String extensions) {
        mExtension = extensions.split("\\|");
    }

    public boolean accept(File dir, String name) {
        for (String extension : mExtension) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
