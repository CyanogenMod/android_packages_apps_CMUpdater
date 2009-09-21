package cmupdaterapp.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cmupdaterapp.customTypes.FullUpdateInfo;

import android.content.Context;
import android.os.Bundle;

public class State
{
	private static final String TAG = "State";
	
	public static void saveState(Context ctx, FullUpdateInfo mAvailableUpdates) throws IOException
	{
		Log.d(TAG, "Called SaveState");
		ObjectOutputStream oos = new ObjectOutputStream(ctx.openFileOutput(Constants.STORED_STATE_FILENAME, Context.MODE_PRIVATE));
		try
		{
			Map<String,Serializable> data = new HashMap<String, Serializable>();
			data.put("mAvailableUpdates", (Serializable)mAvailableUpdates);
			oos.writeObject(data);
			oos.flush();
		}
		finally
		{
			oos.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static FullUpdateInfo loadState(Context ctx) throws IOException
	{
		Log.d(TAG, "Called LoadState");
		FullUpdateInfo mAvailableUpdates = new FullUpdateInfo();
		ObjectInputStream ois = null;
		try
		{
			ois = new ObjectInputStream(ctx.openFileInput(Constants.STORED_STATE_FILENAME));
			Map<String,Serializable> data = (Map<String, Serializable>) ois.readObject();

			Object o = data.get("mAvailableUpdates"); 
			if(o != null) mAvailableUpdates = (FullUpdateInfo) o;
		}
		catch (ClassNotFoundException e)
		{
			Log.e(TAG, "Unable to load stored class", e);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
		}
		finally
		{
			if(ois != null)
				ois.close();
		}
		return mAvailableUpdates;
	}
	
	public static FullUpdateInfo restoreSavedInstanceValues(Bundle b)
	{
		if(b == null) return new FullUpdateInfo();
		return (FullUpdateInfo) b.getSerializable(Constants.KEY_AVAILABLE_UPDATES);
	}
}