package cmupdaterapp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cmupdaterapp.misc.Log;

public class MD5
{
    private static final String TAG = "MD5";

    //Non instantiable class
    private MD5()
    {
        //This constructor will not be called
    }

    public static boolean checkMD5(String md5, File updateFile) throws IOException
    {
    	if (md5 == null || md5 == "" || updateFile == null)
    	{
    		Log.d(TAG, "md5 String NULL or UpdateFile NULL");
    		return false;
    	}

    	String calculatedDigest = calculateMD5(updateFile);

    	if(calculatedDigest == null)
    	{
    		Log.d(TAG, "calculatedDigest NULL");
    		return false;
    	}

    	Log.d(TAG, "Calculated digest: " + calculatedDigest);
		Log.d(TAG, "Provided digest: " + md5);
		
		return calculatedDigest.equalsIgnoreCase(md5);
	}

    public static String calculateMD5(File updateFile)
    {
    	MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.e(TAG, "Exception while getting Digest", e);
			return null;
		}
    	InputStream is = null;
		try
		{
			is = new FileInputStream(updateFile);
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Exception while getting FileInputStream", e);
			return null;
		}				
    	byte[] buffer = new byte[8192];
    	int read = 0;
    	try
    	{
    		while( (read = is.read(buffer)) > 0)
    		{
    			digest.update(buffer, 0, read);
    		}		
    		byte[] md5sum = digest.digest();
    		BigInteger bigInt = new BigInteger(1, md5sum);
    		String output = bigInt.toString(16);
    		//Fill to 32 chars
    		output = String.format("%32s", output).replace(' ', '0');
    		return output;
    	}
    	catch(IOException e)
    	{
    		throw new RuntimeException("Unable to process file for MD5", e);
    	}
    	finally
    	{
    		try
    		{
    			is.close();
    		}
    		catch(IOException e)
    		{
    			throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
    		}
    	}
    }

    public static String getRecoveryMD5()
	{
		String MD5string = null;
		try
		{
			File recovery = new File("/dev/mtd/mtd1");
			if (recovery.exists())
			{
				MD5string = MD5.calculateMD5(recovery);
				Log.d(TAG, "Recovery MD5: "+MD5string);
			}
			else
				throw new IOException();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error on checking recovery MD5. Message: ", e);
			return null;
		}
		return MD5string;
	}
}