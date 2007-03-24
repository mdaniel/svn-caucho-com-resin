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

import java.io.*;

import com.caucho.vfs.*;

class InputStreamMessageReader extends MessageReader
{
  private InputStream _is;
  private int _offset;
  private int _chunkOffset;
  private int _align;

  private TempBuffer _tempBuffer;
  private byte []_buffer;

  private int _length;
  private boolean _isLast;

  InputStreamMessageReader(InputStream is, boolean isLast, int offset)
  {
    try {
      _is = is;

      _isLast = isLast;
      
      _length = (((_is.read() & 0xff) << 24)
		 + ((_is.read() & 0xff) << 16)
		 + ((_is.read() & 0xff) << 8)
		 + ((_is.read() & 0xff)));

      if (_length <= TempBuffer.SIZE) {
	_tempBuffer = TempBuffer.allocate();
	_buffer = _tempBuffer.getBuffer();
      }
      else
	_buffer = new byte[_length + (1024 - _length % 1024) % 1024];

      is.read(_buffer, 0, _length);

      // writeHexGroup(_buffer, 0, _length);

      offset = 12;
	       
      _chunkOffset = offset;
      _offset = offset;
      _align = 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    return _offset + _align;
  }

  /**
   * Sets the offset.
   */
  public int setOffset(int offset)
  {
    int oldOffset = _offset + _align;

    _align = offset - _offset;

    return oldOffset;
  }
  
  /**
   * Reads a byte.
   */
  public int read()
  {
    try {
      if (_length <= _offset - _chunkOffset)
	readFragmentHeader();

      return _buffer[_offset++ - _chunkOffset] & 0xff;
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
	int sublen = _length - (_offset - _chunkOffset);

	if (sublen <= 0) {
	  readFragmentHeader();
	  sublen = _length - (_offset - _chunkOffset);
	}

	if (length < sublen)
	  sublen = length;

	System.arraycopy(_buffer, _offset - _chunkOffset,
			 buffer, offset,
			 sublen);

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

  private void readFragmentHeader()
    throws IOException
  {
    if (_isLast)
      throw new EOFException("read past end of file");
    
    if (_is.read() != 'G'
	|| _is.read() != 'I'
	|| _is.read() != 'O'
	|| _is.read() != 'P')
      throw new IOException("Missing GIOP header.");
    
    int major = _is.read();
    int minor = _is.read();

    if (major != 1)
      throw new IOException("unknown major");

    int flags = _is.read();
    int type = _is.read();

    if (type != IiopReader.MSG_FRAGMENT)
      throw new IOException("expected fragment at " + type);

    _isLast = (flags & 2) != 2;

    _chunkOffset = _offset;
    _length = (((_is.read() & 0xff) << 24)
	       + ((_is.read() & 0xff) << 16)
	       + ((_is.read() & 0xff) << 8)
	       + ((_is.read() & 0xff)));

    if (minor >= 2) {
      int reqId = (((_is.read() & 0xff) << 24)
		   + ((_is.read() & 0xff) << 16)
		   + ((_is.read() & 0xff) << 8)
		   + ((_is.read() & 0xff)));

      _length -= 4;
    }

    if (_length < 0 || _length > 65536)
      throw new RuntimeException();
    
    _is.read(_buffer, 0, _length);
  }

  private void writeHexGroup(byte []buffer, int offset, int length)
  {
    int end = offset + length;
      
    while (offset < end) {
      int chunkLength = 16;
	
      for (int j = 0; j < chunkLength; j++) {
	System.out.print(" ");
	printHex(buffer[offset + j]);
      }

      System.out.print(" ");
      for (int j = 0; j < chunkLength; j++) {
	printCh(buffer[offset + j]);
      }

      offset += chunkLength;
	
      System.out.println();
    }
  }

  private void printHex(int d)
  {
    int ch1 = (d >> 4) & 0xf;
    int ch2 = d & 0xf;

    if (ch1 >= 10)
      System.out.print((char) ('a' + ch1 - 10));
    else
      System.out.print((char) ('0' + ch1));
    
    if (ch2 >= 10)
      System.out.print((char) ('a' + ch2 - 10));
    else
      System.out.print((char) ('0' + ch2));
  }

  private void printCh(int d)
  {
    if (d >= 0x20 && d <= 0x7f)
      System.out.print("" + ((char) d));
    else
      System.out.print(".");
  }

  private String toCh(int d)
  {
    if (d >= 0x20 && d <= 0x7f)
      return "" + (char) d;
    else
      return "" + d;
  }

  private static String toHex(int v)
  {
    StringBuilder cb = new StringBuilder();
    for (int i = 28; i >= 0; i -= 4) {
      int h = (v >> i) & 0xf;

      if (h >= 10)
        cb.append((char) ('a' + h - 10));
      else
        cb.append(h);
    }

    return cb.toString();
  }
}
