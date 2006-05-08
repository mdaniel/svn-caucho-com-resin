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

abstract public class MessageWriter {
  /**
   * Starts a 1.0 message.
   */
  public void start10Message(int type)
  {
  }

  /**
   * Starts a 1.1 message.
   */
  public void start11Message(int type)
  {
  }

  /**
   * Starts a 1.2 message.
   */
  public void start12Message(int type, int requestId)
  {
  }
  
  /**
   * Returns the offset.
   */
  abstract public int getOffset();
  
  /**
   * Writes a byte.
   */
  abstract public void write(int v);
  
  /**
   * Writes data
   */
  abstract public void write(byte []buffer, int offset, int length);
  
  /**
   * Writes a short
   */
  public void writeShort(int v)
  {
    write(v >> 8);
    write(v);
  }
  
  /**
   * Writes an integer.
   */
  public void writeInt(int v)
  {
    write(v >> 24);
    write(v >> 16);
    write(v >> 8);
    write(v);
  }
  
  /**
   * Writes a long.
   */
  public void writeLong(long v)
  {
    write((int) (v >> 56));
    write((int) (v >> 48));
    write((int) (v >> 40));
    write((int) (v >> 32));
    
    write((int) (v >> 24));
    write((int) (v >> 16));
    write((int) (v >> 8));
    write((int) v);
  }
  
  /**
   * Aligns to a specified value.
   */
  public void align(int v)
  {
    int offset = getOffset();

    while (offset % v != 0) {
      offset++;
      write(0);
    }
  }

  /**
   * Completes the response.
   */
  abstract public void close()
    throws IOException;
}
