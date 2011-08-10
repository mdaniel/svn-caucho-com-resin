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
public class WebSocketInputStream extends InputStream 
  implements WebSocketConstants
{
  private final FrameInputStream _is;
  private long _length;
  private boolean _isFinal;

  public WebSocketInputStream(FrameInputStream is)
    throws IOException
  {
    _is = is;
  }
  
  public void init()
  {
    _isFinal = _is.isFinal();
    _length = _is.getLength();
  }

  public boolean startBinaryMessage()
    throws IOException
  {
    if (! _is.readFrameHeader())
      return false;
    
    if (_is.getOpcode() != OP_BINARY)
      throw new UnsupportedOperationException("Expected binary at: " + _is.getOpcode());
    
    init();
    
    return true;
  }

  public long getLength()
  {
    return _is.getLength();
  }

  @Override
  public int read()
    throws IOException
  {
    while (_length == 0 && ! _isFinal) {
      if (! _is.readFrameHeader())
        return -1;
      
      _length = _is.getLength();
      _isFinal = _is.isFinal();
    }

    if (_length > 0) {
      int ch = _is.read();
      _length--;

      return ch;
    }
    else
      return -1;
  }

  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    while (_length == 0 && ! _isFinal) {
      if (! _is.readFrameHeader())
        return -1;
      
      _length = _is.getLength();
      _isFinal = _is.isFinal();
    }

    if (_length <= 0)
      return -1;
    
    int sublen = (int) _length;
    if (length < sublen)
      sublen = length;
    
    sublen = _is.read(buffer, offset, sublen);

    if (sublen < 0)
      return -1;
    
    _length -= sublen;

    return sublen;
  }
}
