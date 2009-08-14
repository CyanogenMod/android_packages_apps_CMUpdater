package cmupdater.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;

import android.util.Log;

public class IOUtils {
    
    private static final char[] TABLE = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F'};
    private static final String TAG = "<CM-Updater> IOUtils";
    private static final String MD5_COMMAND = "md5sum";
    
    //Non instantiable class
    private IOUtils() {
        //This constructor will not be called
    }
    
    /**
     * Returns a char array with the representation of the specified byte array.
     *
     * @param in The byte array whitch representation will be obtained.
     * @return a char array with the representation of the specified byte array.
     */
    public static char[] toCharArray(byte[] in) {
        final char[] out = new char[in.length*2];
        
        dump(in,out);

        return out;
    }
    
    /**
     * 
     * @param in 
     * @return 
     */
    public static byte[] toByteArray(char[] in) {
        final byte[] out = new byte[in.length/2];
        for (int i = 0; i < out.length; i++) {
            byte b=0;
            for (int j = 0; j < 2; j++) {
                char c = in[ (i*2) + j ];
                if(c>='0' && c<='9') {
                    b=(byte)(b<<4);
                    b= (byte)(b | ( 0 + (c-'0') ));
                } else if(c>='A' && c<='F') {
                    b=(byte)(b<<4);
                    b= (byte)(b | ( 10 + (c-'A') ));
                } else if(c>='a' && c<='f') {
                    b=(byte)(b<<4);
                    b= (byte)(b | ( 10 + (c-'a') ));
                } else {
                    throw new InvalidParameterException("Input data contains invalid character");
                }
            }
            out[i]=b;
        }
        
        return out;
    }
    
    /**
     * Dumps the specified byte array to a char array which contains the string representation
     * of the specified input.
     * 
     * @param in The byte array whitch representation will be obtained.
     * @param out The char array where the representation will be obtained.
     * @throws java.lang.ArrayIndexOutOfBoundsException If <code>out.length</code>
     * is smaller than <code>in.length*2</code>
     */
    private static void dump(byte[] in, char[] out)
    throws ArrayIndexOutOfBoundsException {     
        for(int i=0;i<in.length;i++) {
            out[i*2]= TABLE[ (((in[i] & 0xF0)>>4) & 0x0F) ];
            out[i*2 +1] = TABLE[ (in[i] & 0x0F) ];
        }
    }
    
    
    public static boolean checkMD5(String md5, File updateFile) throws IOException {
    	String calculatedDigest = calculateMD5(updateFile);
    	
    	Log.d(TAG, "Calculated digest: " + calculatedDigest);
		Log.d(TAG, "Provided digest: " + md5);
		
		return calculatedDigest.equalsIgnoreCase(md5);
	}
	
    public static String calculateMD5(File updateFile) throws IOException {
    	String calculatedDigest;
		Process process = Runtime.getRuntime().exec(
				new String[] { MD5_COMMAND, updateFile.getAbsolutePath() });

		try {
			process.getOutputStream().close();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()), 1024);
			calculatedDigest = br.readLine();
			if (calculatedDigest == null || calculatedDigest.length() < 32)
				throw new IOException("Returned String is not a MD5 sum: "
						+ calculatedDigest);

			calculatedDigest = calculatedDigest.substring(0, 32);
		} finally {
			process.destroy();
		}
		try {
			process.waitFor();
			Log.d(TAG, MD5_COMMAND + " exit value:" + process.exitValue());
		} catch (InterruptedException e) {
		}
       	
    	return calculatedDigest;
	}
}
