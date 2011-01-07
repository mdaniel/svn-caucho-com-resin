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

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;

/**
 * WebSocketInputStream reads a single WebSocket packet.
 *
 * <code><pre>
 * +-+------+---------+-+---------+
 * |F|xxx(3)|opcode(4)|R|len(7)   |
 * +-+------+---------+-+---------+
 * 
 * OPCODES
 *   0 - close
 *   1 - hello
 *   2 - binary
 *   3 - text
 *   4 - ping
 *   5 - pong
 * </pre></code>
 */
public class WebSocketInputStream extends InputStream 
  implements WebSocketConstants
{
  private static final L10N L = new L10N(WebSocketInputStream.class);
  
  private InputStream _is;
  
  private boolean _isFinal;
  private long _length;

  public WebSocketInputStream(InputStream is)
    throws IOException
  {
    _is = is;
  }

  public void init(boolean isFinal, long length)
    throws IOException
  {
    _isFinal = isFinal;
    _length = length;
  }
  
  public boolean startBinaryMessage()
    throws IOException
  {
    int frame1 = _is.read();
    int frame2 = _is.read();

    if (frame2 < 0)
      return false;
    
    boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
    
    int op = frame1 & 0xf;
    
    if (op != OP_BINARY)
      throw new IllegalStateException(getClass().getSimpleName() + " requires a binary frame");
    
    long len = frame2 & 0x7f;
    
    if (len == 0x7e) {
      len = (_is.read() << 8) + _is.read();
    }
    else if (len == 0x7f) {
      len = (((long) _is.read() << 56)
            + ((long) _is.read() << 48)
            + ((long) _is.read() << 40)
            + ((long) _is.read() << 32)
            + ((long) _is.read() << 24)
            + ((long) _is.read() << 16)
            + ((long) _is.read() << 8)
            + ((long) _is.read()));
    }
    
    _isFinal = isFinal;
    _length = len;
    
    return true;
  }

  public long getLength()
  {
    return _length;
  }

  @Override
  public int read()
    throws IOException
  {
    InputStream is = _is;
    
    if (_length == 0 && ! _isFinal) {
      readFrameHeader();
    }

    if (_length > 0) {
      int ch = is.read();
      _length--;
      return ch;
    }
    else
      return -1;
  }

  private boolean readFrameHeader()
    throws IOException
  {
    InputStream is = _is;

    if (_isFinal || _length > 0)
      throw new IllegalStateException();
    
    while (! _isFinal && _length == 0) {
      int frame1 = is.read();
      int frame2 = is.read();
      
      if (frame2 < 0)
        return false;

      boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
      int op = frame1 & 0xf;

      if (op != OP_CONT) {
        throw new IOException(L.l("{0}: expected op=CONT '0x{1}' because WebSocket binary protocol expects 0x80 at beginning",
                                  this, Integer.toHexString(frame1 & 0xffff)));
      }

      _isFinal = isFinal;

      long length = frame2 & 0x7f;

      if (length < 0x7e) {
      }
      else if (length == 0x7e) {
        length = ((((long) is.read()) << 8)
            + (((long) is.read())));
      }
      else {
        length = ((((long) is.read()) << 56)
            + (((long) is.read()) << 48)
            + (((long) is.read()) << 40)
            + (((long) is.read()) << 32)
            + (((long) is.read()) << 24)
            + (((long) is.read()) << 16)
            + (((long) is.read()) << 8)
            + (((long) is.read())));
      }

      _length = length;
    }
    
    return true;
  }

  @Override
  public void close()
    throws IOException
  {
    while (_length > 0 && !_isFinal) {
      skip(_length);
    }
  }
}
