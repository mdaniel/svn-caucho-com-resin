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
 *   1 - close
 *   2 - ping
 *   3 - pong
 *   4 - text
 *   5 - binary
 * </pre></code>
 */
abstract public class FrameInputStream extends InputStream 
  implements WebSocketConstants
{
  private WebSocketContext _cxt;
  private WebSocketReader _textIn;
  
  public void init(WebSocketContext cxt, InputStream is)
  {
    _cxt = cxt;
  }
  
  public WebSocketContext getContext()
  {
    return _cxt;
  }
  
  abstract public int getOpcode();

  abstract public long getLength();
  
  abstract public boolean isFinal();
  
  public WebSocketReader initReader(long length, boolean isFinal)
    throws IOException
  {
    if (_textIn == null)
      _textIn = new WebSocketReader(this);

    _textIn.init(length, isFinal);
    
    return _textIn;
  }

  public boolean readFrameHeader()
    throws IOException
  {
    long length = getLength();

    if (length > 0) {
      skip(length);
    }

    while (true) {
      if (! readFrameHeaderImpl()) {
        return false;
      }
      
      if (handleFrame()) {
        return true;
      }
    }
  }

  abstract protected boolean readFrameHeaderImpl()
    throws IOException;
  
  protected boolean handleFrame()
    throws IOException
  {
    switch (getOpcode()) {
    case OP_PING:
    {
      long length = getLength();
      
      if (! isFinal()) {
        closeError(1002, "ping must be final");
        return true;
      }
      else if (length > 125) {
        closeError(1002, "ping length must be less than 125");
        return true;
      }
    
      byte []value = new byte[(int) length];
    
      for (int i = 0; i < length; i++) {
        value[i] = (byte) read();
      }

      getContext().pong(value);
      
      return false;
    }
  
    case OP_PONG:
    {
      if (! isFinal()) {
        closeError(1002, "pong must be final");
        return true;
      }
      else if (getLength() > 125) {
        closeError(1002, "pong must be less than 125");
        return true;
      }
    
      long length = getLength();
      byte []value = new byte[(int) length];
    
      for (int i = 0; i < length; i++) {
        value[i] = (byte) read();
      }
      
      return false;
    }
  
    case OP_CLOSE:
    {
      int closeCode = 1002;
      String closeMessage = "error";
      
      try {
        // if (true) return true;
        
        long length = getLength();

        if (length > 125) {
          closeCode = 1002;
          closeMessage = "close must be less than 125 in length";
        }
        else if (! isFinal()) {
          closeCode = 1002;
          closeMessage = "close final";
        }
        else if (length > 0) {
          int d1 = read();
          int d2 = read();

          int code = ((d1 & 0xff) << 8) + (d2 & 0xff);
          
          if (d2 < 0)
            code = 1002;
          
          length -= 2;

          WebSocketReader textIn = initReader(length, true);

          StringBuilder sb = new StringBuilder();
          int ch;
          while ((ch = textIn.read()) >= 0) {
            sb.append(ch);
          }

          switch (code) {
          case 1000:
          case 1001:
          case 1003:
          case 1007:
          case 1008:
          case 1009:
          case 1010:
            closeCode = 1000;
            closeMessage = "ok";
            break;

          default:
            if (3000 <= code && code <= 4999) {
              closeCode = 1000;
              closeMessage = "ok";
            }
            break;
          }
        }
        else {
          closeCode = 1000;
          closeMessage = "ok";
        }
        
        _cxt.onClose(closeCode, closeMessage);
        
        return false;
      } finally {
        closeError(closeCode, closeMessage);
      }
    }
    }

    return true;
  }

  public void skipToFrameEnd()
    throws IOException
  {
    while (getLength() > 0 && ! isFinal()) {
      skip(getLength());
    }
  }

  public void closeError(int i, String msg)
  {
    _cxt.close(i, msg);
  }
}
