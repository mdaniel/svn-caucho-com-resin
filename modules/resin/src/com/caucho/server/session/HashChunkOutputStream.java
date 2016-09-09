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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.session;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

import com.caucho.util.Hex;
import com.caucho.vfs.TempBuffer;

public class HashChunkOutputStream extends OutputStream {
  private static final int FLAG_CLOSE = 0x8000;
  private static final int FLAG_CONT = 0x0000;
  
  private OutputStream _next;
  private CRC32 _crc = new CRC32();
  
  private TempBuffer _tBuf;
  private byte []_buffer;
  private int _offset;
  private int _end;

  public HashChunkOutputStream(OutputStream next)
  {
    _next = next;
    
    _tBuf = TempBuffer.allocate();
    _buffer = _tBuf.getBuffer();
    _end = _buffer.length - 4;
    _offset = 2;
  }

  /**
   * Returns the CRC value.
   */
  public long getDigest()
  {
    return _crc.getValue();
  }
  
  @Override
  public void write(int data)
    throws IOException
  {
    int offset = _offset;
    
    if (_end <= offset) {
      offset = flushBuffer(offset, FLAG_CONT);
    }
    
    _buffer[offset++] = (byte) data;
    _offset = offset;
  }

  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   * @param
   * @Override isEnd true when the write is flushing a close.
   */
  @Override
  public void write(byte []bufferWrite, int offsetWrite, int lengthWrite)
    throws IOException
  {
    int offset = _offset;
    int end = _end;
    byte []buffer = _buffer;
    
    while (lengthWrite > 0) {
      int sublen = Math.min(lengthWrite, end - offset);
      
      if (sublen > 0) {
        System.arraycopy(bufferWrite, offsetWrite, buffer, offset, sublen);
        
        offsetWrite += sublen;
        offset += sublen;
        lengthWrite -= sublen;
      }
      else {
        offset = flushBuffer(offset, FLAG_CONT);
      }
    }
    
    _offset = offset;
  }

  /**
   * Closes the stream output.
   */
  @Override
  public void close() throws IOException
  {
    if (_tBuf != null) {
      _offset = flushBuffer(_offset, FLAG_CLOSE);
    
      _buffer = null;
      _tBuf.freeSelf();
      _tBuf = null;
    
      _next.close();
      _next = null;
    }
  }
  
  private int flushBuffer(int offset, int flag)
    throws IOException
  {
    int len = offset + flag;
    
    byte []buffer = _buffer;
    
    buffer[0] = (byte) (len >> 8); 
    buffer[1] = (byte) (len);
    
    _crc.update(buffer, 0, offset);
    
    long digest = _crc.getValue();
    
    buffer[offset + 0] = (byte) (digest >> 24);
    buffer[offset + 1] = (byte) (digest >> 16);
    buffer[offset + 2] = (byte) (digest >> 8);
    buffer[offset + 3] = (byte) (digest >> 0);
    
    _next.write(buffer, 0, offset + 4);
    
    return 2;
  }
}
