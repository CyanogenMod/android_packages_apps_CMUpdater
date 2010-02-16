package cmupdaterapp.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import cmupdaterapp.customTypes.FullUpdateInfo;
import cmupdaterapp.customization.Customization;
import android.content.Context;

public class State
{
	private static final String TAG = "State";

	public static void saveState(Context ctx, Serializable mAvailableUpdates) throws IOException
	{
		Log.d(TAG, "Called SaveState");
		ObjectOutputStream oos = new ObjectOutputStream(ctx.openFileOutput(Customization.STORED_STATE_FILENAME, Context.MODE_PRIVATE));
		try
		{
			Map<String,Serializable> data = new HashMap<String, Serializable>();
			data.put(Constants.KEY_AVAILABLE_UPDATES, (Serializable)mAvailableUpdates);
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
			ois = new ObjectInputStream(ctx.openFileInput(Customization.STORED_STATE_FILENAME));
			Map<String,Serializable> data = (Map<String, Serializable>) ois.readObject();

			Object o = data.get(Constants.KEY_AVAILABLE_UPDATES); 
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
}