/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.remote.websocket;

import java.io.IOException;
import java.io.InputStream;

/**
 * WebSocketInputStream reads a single WebSocket packet.
 *
 * <code><pre>
 * +-+------+---------+-+---------+
 * |F|xxx(3)|opcode(4)|R|len(7)   |
 * +-+------+---------+-+---------+
 * 
 * OPCODES
 *   0 - cont
 *   1 - close
 *   2 - ping
 *   3 - pong
 *   4 - text
 *   5 - binary
 * </pre></code>
 */
public class MaskedFrameInputStream extends FrameInputStream
{
  private InputStream _is;
  
  private final byte[] _mask = new byte[4];
  private int _maskOffset;
  
  private boolean _isFinal;
  private int _op;
  private long _length;
  

  public MaskedFrameInputStream()
  {
  }
  
  public void init(InputStream is)
  {
    _is = is;
  }

  @Override
  public int getOpcode()
  {
    return _op;
  }
  
  @Override
  public boolean isFinal()
  {
    return _isFinal;
  }

  @Override
  public long getLength()
  {
    return _length;
  }

  @Override
  public int read()
    throws IOException
  {
    long length = _length;
    
    if (length > 0) {
      int ch = _is.read();
      
      _length = length - 1;

      if (ch > 0) {
        int maskOffset = _maskOffset;
        _maskOffset = (maskOffset + 1) & 0x3;
        
        return (ch ^ _mask[maskOffset]) & 0xff;
      }
      else
        return -1;
    }
    else
      return -1;
  }
  
  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_length == 0 && ! _isFinal) {
      readFrameHeader();
      
      if (_length == 0)
        return -1;
    }
    
    int sublen = (int) _length;
    
    if (length < sublen)
      sublen = length;
    
    sublen = _is.read(buffer, offset, sublen);
    
    if (sublen < 0)
      return sublen;
    
    int maskOffset = _maskOffset;
    byte []mask = _mask;
    
    for (int i = 0; i < sublen; i++) {
      buffer[offset + i] ^= mask[(maskOffset + i) & 0x3];
    }
    
    _length -= sublen;
    _maskOffset = (maskOffset + sublen) & 0x3;
    
    return sublen;
  }
  
  @Override
  protected boolean readFrameHeaderImpl()
    throws IOException
  {
    InputStream is = _is;
    
    byte []mask = _mask;
    
    mask[0] = (byte) is.read();
    mask[1] = (byte) is.read();
    mask[2] = (byte) is.read();
    mask[3] = (byte) is.read();
    
    int frame1 = is.read();
    int frame2 = is.read();

    if (frame2 < 0)
      return false;
    
    frame1 = ((frame1 ^ mask[0]) & 0xff);
    frame2 = ((frame2 ^ mask[1]) & 0xff);

    boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
    _op = frame1 & 0xf;

    _isFinal = isFinal;

    long length = frame2 & 0x7f;

    if (length < 0x7e) {
      _maskOffset = 2;
    }
    else if (length == 0x7e) {
      _maskOffset = 0;
      
      length = ((((is.read() ^ mask[2]) & 0xffL) << 8)
          + (((is.read() ^ mask[3]) & 0xffL)));
    }
    else {
      _maskOffset = 2;
      
      length = ((((is.read() ^ mask[2]) & 0xffL) << 56)
               + (((is.read() ^ mask[3]) & 0xffL) << 48)
               + (((is.read() ^ mask[0]) & 0xffL) << 40)
               + (((is.read() ^ mask[1]) & 0xffL) << 32)
               + (((is.read() ^ mask[2]) & 0xffL) << 24)
               + (((is.read() ^ mask[3]) & 0xffL) << 16)
               + (((is.read() ^ mask[0]) & 0xffL) << 8)
               + (((is.read() ^ mask[1]) & 0xffL)));
    }

    _length = length;
    
    return true;
  }
}
