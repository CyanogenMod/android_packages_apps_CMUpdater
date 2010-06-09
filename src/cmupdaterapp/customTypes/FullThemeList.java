package cmupdaterapp.customTypes;

import cmupdaterapp.misc.Log;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

public class FullThemeList implements Serializable {
    private static final long serialVersionUID = -2577705903002871714L;

    private static final String TAG = "FullThemeList";

    private final LinkedList<ThemeList> Themes;

    public FullThemeList() {
        Themes = new LinkedList<ThemeList>();
    }

    public LinkedList<ThemeList> returnFullThemeList() {
        Collections.sort(Themes);
        return Themes;
    }

    public void addThemeToList(ThemeList t) {
        Themes.add(t);
    }

    public boolean removeThemeFromList(ThemeList t) {
        try {
            Themes.remove(Themes.indexOf(t));
            return true;
        }
        catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Exception on Deleting Theme from List", e);
            return false;
        }
    }

    public int getThemeCount() {
        return Themes.size();
    }
}