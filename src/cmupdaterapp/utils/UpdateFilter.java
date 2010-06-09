package cmupdaterapp.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filename Filter for getting only Files that matches the Given Extensions
 * Extensions can be split with |
 * Example: .zip|.md5sum
 */
public class UpdateFilter implements FilenameFilter {
    private final String[] mExtension;

    public UpdateFilter(String Extensions) {
        mExtension = Extensions.split("\\|");
    }

    public boolean accept(File dir, String name) {
        for (String Ext : mExtension) {
            if (name.endsWith(Ext))
                return true;
        }
        return false;
    }
}