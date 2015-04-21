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
import java.io.InputStream;
import java.util.zip.CRC32;

import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

public class HashChunkInputStream extends InputStream
{
  private static final L10N L = new L10N(HashChunkInputStream.class);
  
  private InputStream _next;
  
  private CRC32 _crc = new CRC32();
  
  private TempBuffer _tBuf;
  private byte []_buffer;
  
  private boolean _isLast;
  private int _offset;
  private int _length;

  public HashChunkInputStream(InputStream next)
  {
    _next = next;
    
    _tBuf = TempBuffer.allocate();
    _buffer = _tBuf.getBuffer();
  }
  
  @Override
  public int read()
    throws IOException
  {
    if (_length < _offset && ! fillBuffer()) {
      return -1;
    }

    return _buffer[_offset++] & 0xff;
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
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    int readLength = 0;

    while (length > 0) {
      int sublen = Math.min(length, _length - _offset);
    
      if (sublen <= 0 && ! fillBuffer()) {
        return readLength > 0 ? readLength : -1;
      }
      
      System.arraycopy(_buffer, _offset, buffer, offset, sublen);
      
      length -= sublen;
      _offset += sublen;
      offset += sublen;
      readLength += sublen;
    }
    
    return readLength;
  }

  /**
   * Closes the stream output.
   */
  @Override
  public void close() throws IOException
  {
    _next.close();
  }
  
  private boolean fillBuffer()
    throws IOException
  {
    InputStream is = _next;
    
    if (is == null || _isLast) {
      return false;
    }
    
    byte []buffer = _buffer;
    
    int len0 = is.read();
    int len1 = is.read();
    
    if (len1 < 0) {
      return false;
    }
    
    buffer[0] = (byte) len0;
    buffer[1] = (byte) len1;
    
    _isLast = (len0 & 0x80) != 0;
    len0 = len0 & 0x7f;
    
    int len = (len0 << 8) + len1;
    int end = len + 4;
    
    int offset = 2;
    
    while (offset < end) {
      int sublen = is.read(buffer, offset, end - offset);
      
      if (sublen < 0) {
        return false;
      }
      
      offset += sublen;
    }
    
    _crc.update(buffer, 0, end - 4);
    long digest = _crc.getValue();
    
    long readDigest = (((buffer[end - 4] & 0xffL) << 24)
                     + ((buffer[end - 3] & 0xffL) << 16)
                     + ((buffer[end - 2] & 0xffL) << 8)
                     + ((buffer[end - 1] & 0xffL) << 0));
    
    if (digest != readDigest) {
      _isLast = true;
      _offset = 0;
      _length = 0;
      
      throw new IOException(L.l("Mismatch crc read={0} calc={1}",
                                Long.toHexString(readDigest),
                                Long.toHexString(digest)));
    }
    else {
      _offset = 2;
      _length = end - 4;
    }
    
    return true;
  }
}
