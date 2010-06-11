package cmupdaterapp.misc;

import android.content.Context;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customization.Customization;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class State {
    private static final String TAG = "State";

    public static void saveState(Context ctx, Serializable mAvailableUpdates, Boolean _showDebugOutput) throws IOException {
        if (_showDebugOutput) Log.d(TAG, "Called SaveState");
        if (_showDebugOutput) Log.d(TAG, "Updatecount: " + ((FullUpdateInfo) mAvailableUpdates).getUpdateCount());

        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
        	File f = new File(ctx.getCacheDir(), Customization.STORED_STATE_FILENAME);
        	fos = new FileOutputStream(f);
        	oos = new ObjectOutputStream(fos);
            Map<String, Serializable> data = new HashMap<String, Serializable>();
            data.put(Constants.KEY_AVAILABLE_UPDATES, mAvailableUpdates);
            oos.writeObject(data);
            oos.flush();
        }
        catch (Exception ex) {
            Log.e(TAG, "Exception on saving Instance State", ex);
        }
        finally {
        	if (oos != null) {
        		oos.close();
        	}
            if (fos != null) {
            	fos.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static FullUpdateInfo loadState(Context ctx, Boolean _showDebugOutput) throws IOException {
        if (_showDebugOutput) Log.d(TAG, "Called LoadState");
        FullUpdateInfo mAvailableUpdates = new FullUpdateInfo();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
        	File f = new File(ctx.getCacheDir(), Customization.STORED_STATE_FILENAME);
        	fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            Map<String, Serializable> data = (HashMap<String, Serializable>) ois.readObject();
            Object o = data.get(Constants.KEY_AVAILABLE_UPDATES);
            if (o != null) mAvailableUpdates = (FullUpdateInfo) o;
        }
        catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load stored class", e);
        }
        catch (FileNotFoundException ex) {
            Log.i(TAG, "No State Info stored");
        }
        catch (IOException e) {
            Log.e(TAG, "Exception on Loading State", e);
        }
        finally {
            if (ois != null) {
                ois.close();
            }
            if (fis != null) {
            	fis.close();
            }
        }
        if (_showDebugOutput) Log.d(TAG, "LoadedUpdates: " + mAvailableUpdates.getUpdateCount());
        return mAvailableUpdates;
    }
}