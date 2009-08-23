package cmupdaterapp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.util.Log;

public class IOUtils
{
    private static final String TAG = "<CM-Updater> IOUtils";
    private static final String MD5_COMMAND = "md5sum";
    
    //Non instantiable class
    private IOUtils()
    {
        //This constructor will not be called
    }
    
    public static boolean checkMD5(String md5, File updateFile) throws IOException
    {
    	String calculatedDigest = calculateMD5(updateFile, false);
    	
    	Log.d(TAG, "Calculated digest: " + calculatedDigest);
		Log.d(TAG, "Provided digest: " + md5);
		
		return calculatedDigest.equalsIgnoreCase(md5);
	}
	
    public static String calculateMD5(File updateFile, boolean su) throws IOException
    {
    	String calculatedDigest;
    	Process process;
		if (su)
		{
			process = Runtime.getRuntime().exec("su");
	    	OutputStream os = process.getOutputStream();
			os.write((MD5_COMMAND + " " + updateFile.getAbsolutePath()).getBytes());
			os.flush();
		}
		else
		{
			process = Runtime.getRuntime().exec(new String[] { MD5_COMMAND, updateFile.getAbsolutePath() });
		}
		try
		{
			process.getOutputStream().close();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()), 1024);
			calculatedDigest = br.readLine();
			if (calculatedDigest == null || calculatedDigest.length() < 32)
				throw new IOException("Returned String is not a MD5 sum: "
						+ calculatedDigest);

			calculatedDigest = calculatedDigest.substring(0, 32);
		}
		finally
		{
			process.destroy();
		}
		try
		{
			process.waitFor();
			Log.d(TAG, MD5_COMMAND + " exit value:" + process.exitValue());
		}
		catch (InterruptedException e)
		{
			Log.e(TAG, "Exception while Calculating MD5", e);
		}
       	
    	return calculatedDigest;
	}
    
    public static String getRecoveryMD5()
	{
		String MD5 = null;
		try
		{
			File recovery = new File("/dev/mtd/mtd1");
			if (recovery.exists())
			{
				MD5 = IOUtils.calculateMD5(recovery, true);
				Log.d(TAG, "Recovery MD5: "+MD5);
			}
			else
				throw new IOException();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error on checking recovery MD5. Message: ", e);
			return null;
		}
		return MD5;
	}
}