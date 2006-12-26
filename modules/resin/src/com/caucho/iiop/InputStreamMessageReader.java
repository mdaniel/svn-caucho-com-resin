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

import java.io.InputStream;
import java.io.IOException;

class InputStreamMessageReader extends MessageReader
{
  private InputStream _is;
  private int _offset;

  private int _length;
  private boolean _isLast;

  InputStreamMessageReader(InputStream is, boolean isLast)
  {
    _is = is;

    _isLast = isLast;
    _length = read_long();
    //    System.out.println("LEN: " + _length);
  }
  
  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    return _offset;
  }

  /**
   * Sets the offset.
   */
  public void setOffset(int offset)
  {
    _offset = offset;
  }
  
  /**
   * Reads a byte.
   */
  public int read()
  {
    try {
      _offset++;
      return _is.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Reads data
   */
  public void read(byte []buffer, int offset, int length)
  {
    try {
      while (length > 0) {
	int sublen = _is.read(buffer, offset, length);

	if (sublen < 0)
	  throw new IOException("unexpected end of file");

	offset += sublen;
	length -= sublen;
	_offset += sublen;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
