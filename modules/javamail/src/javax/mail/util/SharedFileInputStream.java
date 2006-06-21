/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail.util;
import javax.mail.internet.SharedInputStream;
import javax.mail.*;
import java.io.*;

/**
 * SharedFileInputStream backed by a File
 *
 * XXX: this is not implemented the way Sun intends; I need to go back
 *      and actually use the RandomAccessFile.  But the current form
 *      works and should be indistinguishable.
 */
public class SharedFileInputStream extends BufferedInputStream
  implements SharedInputStream {

  private int  _pos;
  private File _file;

  /**
   * The file offset that corresponds to the first byte in the read buffer.
   */
  protected long bufpos;

  /**
   * The normal size of the read buffer.
   */
  protected int bufsize;

  /**
   * The amount of data in this subset of the file.
   */
  protected long datalen;

  protected RandomAccessFile in;

  /**
   * The file offset of the start of data in this subset of the file.
   */
  protected long start;

  public SharedFileInputStream(File file) throws IOException
  {
    this(file, (int)file.length());
  }

  public SharedFileInputStream(File file, int size) throws IOException
  {
    this(file, 0, size);
  }

  SharedFileInputStream(File file, int offset, int size) throws IOException
  {
    super(new FileInputStream(file));
    if (size <= 0)
      throw new IllegalArgumentException("SharedFileInputStream size <= 0");

    this.in      = new RandomAccessFile(file, "r");
    this.datalen = size;
    this.start   = offset;
    this._pos    = offset;
    this._file   = file;

    // skip over the part at the beginning
    long mustskip = start;
    
    while(mustskip > 0)
      mustskip -= super.skip(start);
  }

  public SharedFileInputStream(String file) throws IOException
  {
    this(new File(file));
  }

  public SharedFileInputStream(String file, int size) throws IOException
  {
    this(new File(file), size);
  }

  public int available() throws IOException
  {
    return super.available();
  }

  public void close() throws IOException
  {
    // XXX: implement this
  }

  protected void finalize() throws Throwable
  {
    // XXX: implement this
  }

  public long getPosition()
  {
    return _pos - start;
  }

  // XXX: test this
  /**
   * See the general contract of the mark method of InputStream.
   */
  public void mark(int readlimit)
  {
    super.mark(readlimit);
  }

  public boolean markSupported()
  {
    return true;
  }

  public InputStream newStream(long start, long end)
  {
    try {
      return new SharedFileInputStream(_file, (int)start, (int)(end-start));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int read() throws IOException
  {
    if (getPosition() >= datalen)
      return -1;

    int valueRead = super.read();

    if (valueRead != -1)
      _pos++;

    return valueRead;
  }

  public int read(byte[] b, int off, int len) throws IOException
  {
    if (getPosition() >= datalen)
      return -1;

    if (len + getPosition() > datalen)
      len = (int)(datalen - getPosition());
    
    int numread = super.read(b, off, len);

    if (numread >= -1)
      _pos += numread;

    return numread;
  }

  public void reset() throws IOException
  {
    super.reset();
  }

  public long skip(long n) throws IOException
  {
    if (getPosition() >= datalen)
      return -1;
    
    if (n + getPosition() > datalen)
      n = datalen - getPosition();
    
    long numskipped = super.skip(n);

    if (numskipped >= 0)
      _pos += numskipped;

    return numskipped;
  }

}
