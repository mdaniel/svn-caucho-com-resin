/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Writing to a google stream.
 */
class GoogleRandomAccessStream extends RandomAccessStream {
  private final Path _path;
  private StreamImpl _os;
  
  GoogleRandomAccessStream(Path path, StreamImpl os)
  {
    _path = path;
    _os = os;
  }

  @Override
  public long getLength() throws IOException
  {
    return 0;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int read(char[] buffer, int offset, int length) throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int read(long fileOffset, byte[] buffer, int offset, int length)
      throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException
  {
    _os.write(buffer, offset, length, false);
  }

  @Override
  public void write(long fileOffset, byte[] buffer, int offset, int length)
      throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean seek(long position)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public OutputStream getOutputStream() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /* (non-Javadoc)
   * @see com.caucho.vfs.RandomAccessStream#getInputStream()
   */
  @Override
  public InputStream getInputStream() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int read() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void write(int b) throws IOException
  {
    byte []buf = new byte[] { (byte) b };
    
    write(buf, 0, 1);
  }

  @Override
  public long getFilePointer() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public void closeImpl()
    throws IOException
  {
    StreamImpl os = _os;
    _os = null;
    
    if (os != null)
      os.close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "," + _os + "]";
  }
}
