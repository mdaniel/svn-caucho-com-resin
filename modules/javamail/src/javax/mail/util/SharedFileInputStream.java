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
 * A SharedFileInputStream is a BufferedInputStream that buffers data
 * from the file and supports the mark and reset methods. It also
 * supports the newStream method that allows you to create other
 * streams that represent subsets of the file. A RandomAccessFile
 * object is used to access the file data.  Since: JavaMail 1.4
 */
public class SharedFileInputStream extends BufferedInputStream
  implements SharedInputStream {

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

  /**
   * The file containing the data. Shared by all related SharedFileInputStreams.
   */
  protected RandomAccessFile in;

  /**
   * The file offset of the start of data in this subset of the file.
   */
  protected long start;

  /**
   * Creates a SharedFileInputStream for the file.
   * file - the file
   */
  public SharedFileInputStream(File file) throws IOException
  {
    super(null); // remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Creates a SharedFileInputStream with the specified buffer size.
   * file - the filesize - the buffer size.
   * IllegalArgumentException - if size <= 0. IOException
   */
  public SharedFileInputStream(File file, int size) throws IOException
  {
    super(null); // remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Creates a SharedFileInputStream for the named file
   * file - the file
   */
  public SharedFileInputStream(String file) throws IOException
  {
    super(null); // remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Creates a SharedFileInputStream with the specified buffer size.
   * file - the filesize - the buffer size.
   * IllegalArgumentException - if size <= 0. IOException
   */
  public SharedFileInputStream(String file, int size) throws IOException
  {
    super(null); // remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the number of bytes that can be read from this input
   * stream without blocking.
   */
  public int available() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Closes this input stream and releases any system resources
   * associated with the stream.
   */
  public void close() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Force this stream to close.
   */
  protected void finalize() throws Throwable
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the current position in the InputStream, as an offset from
   * the beginning of the InputStream.
   */
  public long getPosition()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * See the general contract of the mark method of InputStream.
   */
  public void mark(int readlimit)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Tests if this input stream supports the mark and reset
   * methods. The markSupported method of SharedFileInputStream
   * returns true.
   */
  public boolean markSupported()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a new InputStream representing a subset of the data from
   * this InputStream, starting at start (inclusive) up to end
   * (exclusive). start must be non-negative. If end is -1, the new
   * stream ends at the same place as this stream. The returned
   * InputStream will also implement the SharedInputStream interface.
   */
  public InputStream newStream(long start, long end)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * See the general contract of the read method of InputStream.
   */
  public int read() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Reads bytes from this stream into the specified byte array,
   * starting at the given offset.
   *
   * This method implements the general contract of the corresponding
   * read method of the InputStream class.
   */
  public int read(byte[] b, int off, int len) throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * See the general contract of the reset method of InputStream.
   *
   * If markpos is -1 (no mark has been set or the mark has been
   * invalidated), an IOException is thrown. Otherwise, pos is set
   * equal to markpos.
   */
  public void reset() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * See the general contract of the skip method of InputStream.
   */
  public long skip(long n) throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
