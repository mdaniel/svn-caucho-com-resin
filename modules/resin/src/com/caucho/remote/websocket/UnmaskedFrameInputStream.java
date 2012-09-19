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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.websocket.WebSocketContext;

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
 *   1 - text
 *   2 - binary
 *   8 - close
 * </pre></code>
 */
public class UnmaskedFrameInputStream extends FrameInputStream
{
  private static final Logger log
    = Logger.getLogger(UnmaskedFrameInputStream.class.getName());
  
  private InputStream _is;
  
  private boolean _isFinal = true;
  private int _op;
  private long _length;

  public UnmaskedFrameInputStream()
  {
  }
  
  @Override
  public void init(WebSocketContext cxt, InputStream is)
  {
    super.init(cxt, is);
    
    if (is == null)
      throw new NullPointerException();
    
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
    
    if (length == 0 && ! _isFinal) {
      readFrameHeader();
      
      if (_length == 0)
        return -1;
      
      length = _length;
    }
    
    
    int ch = _is.read();
      
    _length = length - 1;

    return ch;
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
    
    int sublen = length;
    
    if (_length < sublen)
      sublen = (int) _length;
    
    sublen = _is.read(buffer, offset, sublen);
    
    if (sublen < 0)
      return sublen;
    
    _length -= sublen;
    
    return sublen;
  }

  @Override
  protected boolean readFrameHeaderImpl()
    throws IOException
  {
    InputStream is = _is;
    
    int frame1 = is.read();
    int frame2 = is.read();

    /*
    System.out.println("WS: 0x" + Integer.toHexString(frame1)
                       + " 0x" + Integer.toHexString(frame2));
                       */

    if (frame2 < 0) {
      return false;
    }

    boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
    _op = frame1 & 0xf;
    
    int rsv = frame1 & 0x70;
    
    if (rsv != 0) {
      if (getContext() != null) {
        getContext().close(CLOSE_ERROR, "illegal request");
      }
      
      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " WebSocket BAD_REQ:"+ Integer.toHexString(frame1)
                 + " " + Integer.toHexString(frame2));
      }

      return false;
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

    return true;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _is + "]";
  }
}
