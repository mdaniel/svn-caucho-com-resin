/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Stream encapsulating FileInputStream
 */
public class FileReadStream extends StreamImpl {
  FileInputStream _is;
  
  /**
   * Create a new FileReadStream.
   */
  public FileReadStream()
  {
  }
  
  /**
   * Create a new FileReadStream based on the java.io.* stream.
   *
   * @param is the underlying input stream.
   */
  public FileReadStream(FileInputStream is)
  {
    init(is);
  }
  
  /**
   * Create a new FileReadStream based on the java.io.* stream.
   *
   * @param is the underlying input stream.
   * @param path the associated Path.
   */
  public FileReadStream(FileInputStream is, Path path)
  {
    init(is);
    setPath(path);
  }

  /**
   * Initializes a VfsStream with an input/output stream pair.  Before a
   * read, the output will be flushed to avoid deadlocks.
   *
   * @param is the underlying InputStream.
   * @param os the underlying OutputStream.
   */
  public void init(FileInputStream is)
  {
    _is = is;
    setPath(null);
  }

  /**
   * Returns true if there's an associated file.
   */
  public boolean canSkip()
  {
    return _is != null;
  }

  /**
   * Skips bytes in the file.
   *
   * @param n the number of bytes to skip
   *
   * @return the actual bytes skipped.
   */
  public long skip(long n)
    throws IOException
  {
    if (_is != null)
      return _is.skip(n);
    else
      return -1;
  }

  /**
   * Returns true if there's an associated file.
   */
  public boolean canRead()
  {
    return _is != null;
  }

  /**
   * Reads bytes from the file.
   *
   * @param buf a byte array receiving the data.
   * @param offset starting index to receive data.
   * @param length number of bytes to read.
   *
   * @return the number of bytes read or -1 on end of file.
   */
  public int read(byte []buf, int offset, int length) throws IOException
  {
    if (_is == null)
      return -1;

    int len = _is.read(buf, offset, length);

    return len;
  }

  /**
   * Returns the number of bytes available for reading.
   */
  public int getAvailable() throws IOException
  {
    if (_is == null)
      return -1;
    else {
      return _is.available();
    }
  }

  /**
   * Closes the underlying stream.
   */
  public void close() throws IOException
  {
    InputStream is = _is;
    _is = null;
    
    if (is != null)
      is.close();
  }
}
