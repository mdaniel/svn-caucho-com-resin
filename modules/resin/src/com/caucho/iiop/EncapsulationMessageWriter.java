/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.vfs.TempBuffer;

import java.io.IOException;

public class EncapsulationMessageWriter extends MessageWriter
{
  private TempBuffer _head;
  private TempBuffer _tail;

  private byte []_buffer;
  
  private int _offset;
  
  private int _length;

  public EncapsulationMessageWriter()
  {
    _head = _tail = TempBuffer.allocate();

    _buffer = _head.getBuffer();
  }

  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    return _offset;
  }
  
  /**
   * Writes a byte.
   */
  public void write(int v)
  {
    if (_buffer.length <= _length)
      flushBuffer();

    _offset++;
    _buffer[_length++] = (byte) v;
  }
  
  /**
   * Writes data
   */
  public void write(byte []buffer, int offset, int length)
  {
    while (length > 0) {
      int sublen = _buffer.length - _length;
      
      if (length < sublen)
	sublen = length;

      System.arraycopy(buffer, offset, _buffer, _length, sublen);

      _offset += sublen;
      _length += sublen;
      length -= sublen;

      if (length > 0)
	flushBuffer();
    }
  }
  
  /**
   * Writes a short
   */
  public final void writeShort(int v)
  {
    if (_buffer.length <= _length + 1)
      flushBuffer();

    _offset += 2;

    _buffer[_length++] = (byte) (v >> 8);
    _buffer[_length++] = (byte) (v);
  }
  
  /**
   * Writes an integer.
   */
  public void writeInt(int v)
  {
    if (_buffer.length <= _length + 3)
      flushBuffer();

    _offset += 4;
    
    _buffer[_length++] = (byte) (v >> 24);
    _buffer[_length++] = (byte) (v >> 16);
    _buffer[_length++] = (byte) (v >> 8);
    _buffer[_length++] = (byte) (v);
  }
  
  /**
   * Aligns to a specified value.
   */
  public void align(int v)
  {
    int delta = v - _length % v;

    if (delta == v)
      return;

    _offset += delta;

    for (; delta > 0; delta--)
      _buffer[_length++] = 0;
  }

  /**
   * Flushes the buffer.
   */
  private void flushBuffer()
  {
    _tail.setLength(_length);

    TempBuffer tail = TempBuffer.allocate();
    _tail.setNext(tail);
    _tail = tail;

    _buffer = _tail.getBuffer();
    _length = 0;
  }

  /**
   * Completes the response.
   */
  public void close()
  {
    _tail.setLength(_length);
    _tail = null;
  }

  public void writeToWriter(IiopWriter out)
  {
    TempBuffer ptr = _head;
    int fullLen = 0;

    while (ptr != null) {
      TempBuffer next = ptr.getNext();

      int len = ptr.getLength();
      byte []buffer = ptr.getBuffer();

      out.write(buffer, 0, len);

      fullLen += len;

      TempBuffer.free(ptr);
      ptr = next;
    }
  }
}
