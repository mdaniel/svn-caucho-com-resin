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

package com.caucho.quercus.env;

import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

import java.io.ByteArrayInputStream;

/**
 * Represents a PHP string value implemented as a TempBuffer, with
 * encoding iso-8859-1..
 */
public class TempBufferStringValue extends StringValue {
  private TempBuffer _head;

  private String _string;

  public TempBufferStringValue(TempBuffer buffer)
  {
    _head = buffer;
  }

  /**
   * 
   * @return _head as inputstream
   */
  public ByteArrayInputStream toInputStream()
  {
    return new ByteArrayInputStream(_head.getBuffer());
  }
  
  /**
   * Returns the length as a string.
   */
  public int strlen()
  {
    int len = 0;

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      len += ptr.getLength();
    }

    return len;
  }

  /**
   * Prints the value.
   *
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    WriteStream out = env.getOut();

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      out.write(ptr.getBuffer(), 0, ptr.getLength());
    }
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_string == null) {
      char []cbuf = new char[strlen()];

      int i = 0;
      for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
	byte []buf = ptr.getBuffer();

	int len = ptr.getLength();

	for (int j = 0; j < len; j++)
	  cbuf[i++] = (char) buf[j];
      }

      _string = new String(cbuf);
    }

    return _string;
  }
}

