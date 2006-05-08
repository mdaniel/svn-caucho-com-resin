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

import com.caucho.vfs.WriteStream;

public class StreamMessageWriter extends MessageWriter {
  private WriteStream _out;

  private byte []_buffer;
  private int _bufferLength;
  
  private int _offset;
  private int _align;
  private int _length;

  private int _version;

  public StreamMessageWriter()
  {
  }

  public StreamMessageWriter(WriteStream out)
  {
    init(out);
  }
  
  /**
   * initialize the writer.
   */
  public void init(WriteStream out)
  {
    _out = out;
    _buffer = _out.getBuffer();
    _bufferLength = _buffer.length;
    _align = 0;
    
    _buffer[0] = 'G';
    _buffer[1] = 'I';
    _buffer[2] = 'O';
    _buffer[3] = 'P';
  }

  /**
   * Starts a 1.0 message.
   */
  public void start10Message(int type)
  {
    _version = 0;
    
    _offset = 0;
    _align = 0;
    _length = 12;

    _buffer[4] = 1;
    _buffer[5] = 0;
    _buffer[6] = 0;
    _buffer[7] = (byte) type;
  }

  /**
   * Starts a 1.1 message.
   */
  public void start11Message(int type)
  {
    _version = 1;
    
    _offset = 0;
    _length = 12;
    _align = 0;

    _buffer[4] = 1;
    _buffer[5] = 1;
    _buffer[6] = 0;
    _buffer[7] = (byte) type;
  }

  /**
   * Starts a 1.2 message.
   */
  public void start12Message(int type, int requestId)
  {
    _version = 2;
    
    _offset = 4;
    _length = 16;
    _align = 0;

    _buffer[4] = 1;
    _buffer[5] = 2;
    _buffer[6] = 0;
    _buffer[7] = (byte) type;

    _buffer[12] = (byte) (requestId >> 24);
    _buffer[13] = (byte) (requestId >> 16);
    _buffer[14] = (byte) (requestId >> 8);
    _buffer[15] = (byte) (requestId);
  }

  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    // flush if nearly full to deal with the IIOP 1.2 message header
    if (_bufferLength <= _length + 4)
      flushBuffer();

    return _offset;
  }
  
  /**
   * Writes a byte.
   */
  public void write(int v)
  {
    if (_bufferLength <= _length)
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
      int sublen = _bufferLength - _length;
      
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
    if (_bufferLength <= _length + 1)
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
    if (_bufferLength <= _length + 3)
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
  public void flushBuffer()
  {
    try {
      int size = _length - 12;

      _buffer[6] = 2; // fragmented

      _buffer[8] = (byte) (size >> 24);
      _buffer[9] = (byte) (size >> 16);
      _buffer[10] = (byte) (size >> 8);
      _buffer[11] = (byte) (size >> 0);

      if (_version == 2) {
	_length += (8 - _length % 8) % 8;
      }

      _out.setBufferOffset(_length);
      _out.flushBuffer();

      _buffer[7] = IiopReader.MSG_FRAGMENT;

      if (_version == 2) {
	_offset += 4;
	_length = 16;
      }
      else
	_length = 12;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Completes the response.
   */
  public void close()
    throws IOException
  {
    int size = _length - 12;

    _buffer[6] = 0; // last message

    _buffer[8] = (byte) (size >> 24);
    _buffer[9] = (byte) (size >> 16);
    _buffer[10] = (byte) (size >> 8);
    _buffer[11] = (byte) (size >> 0);

    /*
    if (_version == 2) {
      int delta = (8 - _length % 8) % 8;

      for (int i = 0; i < delta; i++)
	_buffer[_length + i] = (byte) 0xaa;
      
      _length += delta;
    }
    */

    // debugData();

    _out.setBufferOffset(_length);
    _out.flushBuffer();

    _length = 0;
  }

  public void debugData()
  {
    for (int tail = 0; tail < _length; tail += 16) {
      for (int j = 0; j < 16; j++) {
	System.out.print(" ");
	
	if (tail + j < _length)
	  printHex(_buffer[tail + j]);
	else
	  System.out.print("  ");
      }

      System.out.print(" ");
      for (int j = 0; j < 16; j++) {
	if (tail + j < _length)
	  printCh(_buffer[tail + j]);
	else
	  System.out.print(" ");
      }
	
      System.out.println();
    }
    
    System.out.println();
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
    String s = "";
      
    for (int i = 28; i >= 0; i -= 4) {
      int h = (v >> i) & 0xf;

      if (h >= 10)
        s += ((char) ('a' + h - 10));
      else
        s += (h);
    }

    return s;
  }
}
