package cmupdater.utils.md5;

import java.io.*;


/** 
 * MD5InputStream, a subclass of FilterInputStream implementing MD5
 * functionality on a stream.
 * <p>
 * Originally written by Santeri Paavolainen, Helsinki Finland 1996 <br>
 * (c) Santeri Paavolainen, Helsinki Finland 1996 <br>
 * Some changes Copyright (c) 2002 Timothy W Macinta <br>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * <p>
 * See http://www.twmacinta.com/myjava/fast_md5.php for more information
 * on this file.
 * <p>
 * Please note: I (Timothy Macinta) have put this code in the
 * com.twmacinta.util package only because it came without a package.  I
 * was not the the original author of the code, although I did
 * optimize it (substantially) and fix some bugs.
 *
 * @author	Santeri Paavolainen <santtu@cs.hut.fi>
 * @author	Timothy W Macinta (twm@alum.mit.edu) (added main() method)
 **/


public class MD5InputStream extends FilterInputStream {
  /**
   * MD5 context
   */
  private MD5	md5;
  
  /**
   * Creates a MD5InputStream
   * @param in	The input stream
   */
  public MD5InputStream (InputStream in) {
    super(in);

    md5 = new MD5();
  }

  /**
   * Read a byte of data. 
   * @see java.io.FilterInputStream
   */
  public int read() throws IOException {
    int c = in.read();

    if (c == -1)
	return -1;

    if ((c & ~0xff) != 0) {
      System.out.println("MD5InputStream.read() got character with (c & ~0xff) != 0)!");
    } else {
      md5.Update(c);
    }

    return c;
  }

  /**
   * Reads into an array of bytes.
   *
   * @see java.io.FilterInputStream
   */
  public int read (byte bytes[], int offset, int length) throws IOException {
    int	r;
    
    if ((r = in.read(bytes, offset, length)) == -1)
      return r;

    md5.Update(bytes, offset, r);

    return r;
  }

  /**
   * Returns array of bytes representing hash of the stream as
   * finalized for the current state. 
   * @see MD5#Final
   */
  public byte[] hash () {
    return md5.Final();
  }

  public MD5 getMD5() {
    return md5;
  }

}

