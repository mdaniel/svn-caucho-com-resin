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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import java.io.IOException;

abstract public class MessageReader
{
  /**
   * Returns the offset.
   */
  abstract public int getOffset();
  
  /**
   * Reads a byte.
   */
  abstract public int read();
  
  /**
   * Reads data
   */
  public void read(byte []buffer, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      buffer[i + offset] = (byte) read();
  }
  
  /**
   * Reads a short (with alignment)
   */
  public int read_short()
  {
    int offset = getOffset();

    while (offset % 2 != 0) {
      offset++;
      read();
    }
    
    return (((read() & 0xff) << 8)
	    + (read() & 0xff));
  }
  
  /**
   * Reads an integer
   */
  public int read_long()
  {
    int offset = getOffset();

    while (offset % 4 != 0) {
      offset++;
      read();
    }
    
    return (((read() & 0xff) << 24)
	    + ((read() & 0xff) << 16)
	    + ((read() & 0xff) << 8)
	    + (read() & 0xff));
  }
  
  /**
   * Reads a long.
   */
  public long read_longlong()
  {
    int offset = getOffset();

    while (offset % 8 != 0) {
      offset++;
      read();
    }
    
    return (((read() & 0xffL) << 56)
	    + ((read() & 0xffL) << 48)
	    + ((read() & 0xffL) << 40)
	    + ((read() & 0xffL) << 32)
	    + ((read() & 0xffL) << 24)
	    + ((read() & 0xffL) << 16)
	    + ((read() & 0xffL) << 8)
	    + (read() & 0xffL));
  }

  public void skip(int len)
  {
    for (; len > 0; len--) {
      read();
    }
  }
  
  /**
   * Aligns to a specified value.
   */
  public void align(int v)
  {
    int offset = getOffset();

    while (offset % v != 0) {
      offset++;
      read();
    }
  }

  /**
   * Completes the request
   */
  public void close()
  {
  }
}
