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

package com.caucho.vfs;

import java.util.logging.*;

import java.io.*;

/**
 * Reads from a file in a random-access fashion.
 */
public class SpyRandomAccessStream extends RandomAccessStream {
  private static final Logger log
    = Logger.getLogger(SpyRandomAccessStream.class.getName());
  
  private RandomAccessStream _file;

  public SpyRandomAccessStream(RandomAccessStream file)
  {
    _file = file;
  }
  
  /**
   * Returns the length.
   */
  public long getLength()
    throws IOException
  {
    return _file.getLength();
  }
  
  /**
   * Reads a block from a given location.
   */
  public int read(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    log.info("random-read(0x" + Long.toHexString(fileOffset) + "," + length + ")");
    
    return _file.read(fileOffset, buffer, offset, length);
  }

  /**
   * Writes a block from a given location.
   */
  public void write(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    log.info("random-write(0x" + Long.toHexString(fileOffset) + "," + length + ")");
    
    _file.write(fileOffset, buffer, offset, length);
  }

  /**
   * Closes the stream.
   */
  public void close() throws IOException
  {
    _file.close();
  }
}
