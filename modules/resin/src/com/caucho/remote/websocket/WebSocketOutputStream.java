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
import java.io.OutputStream;

/**
 * WebSocketOutputStream writes a single WebSocket packet.
 *
 * <code><pre>
 * 0x82 0xNN binarydata
 * </pre></code>
 */
public class WebSocketOutputStream extends OutputStream 
  implements WebSocketConstants
{
  private OutputStream _os;
  private byte []_buffer;
  private int _offset;
  
  private MessageState _state = MessageState.IDLE;
  private boolean _isAutoFlush = true;

  public WebSocketOutputStream(OutputStream os, byte []workingBuffer)
    throws IOException
  {
    if (os == null)
      throw new NullPointerException();
    
    if (workingBuffer == null)
      throw new NullPointerException();
    
    _os = os;

    _buffer = workingBuffer;
  }
  
  public void init()
  {
    if (_state != MessageState.IDLE)
      throw new IllegalStateException(String.valueOf(_state));
    
    _state = MessageState.FIRST;
    
    _offset = 4;
  }

  @Override
  public void write(int ch)
    throws IOException
  {
    if (! _state.isActive())
      throw new IllegalStateException(String.valueOf(_state));
    
    byte []buffer = _buffer;

    if (_offset == buffer.length)
      complete(false);

    buffer[_offset++] = (byte) ch;
  }

  @Override
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (! _state.isActive())
      throw new IllegalStateException(String.valueOf(_state));
    
    byte []wsBuffer = _buffer;

    while (length > 0) {
      if (_offset == wsBuffer.length)
        complete(false);

      int sublen = wsBuffer.length - _offset;
      if (length < sublen)
        sublen = length;

      System.arraycopy(buffer, offset, wsBuffer, _offset, sublen);

      offset += sublen;
      length -= sublen;
      _offset += sublen;
    }
  }

  @Override
  public void flush()
    throws IOException
  {
    complete(false);

    _os.flush();
  }

  @Override
  public void close()
    throws IOException
  {
    if (_state == MessageState.IDLE)
      return;
    
    complete(true);
    
    _state = MessageState.IDLE;
    
    if (_isAutoFlush)
      _os.flush();
  }

  private void complete(boolean isFinal)
    throws IOException
  {
    byte []buffer = _buffer;

    int offset = _offset;
    _offset = 4;

    // don't flush empty chunk
    if (offset == 4 && ! isFinal)
      return;

    int length = offset - 4;
    
    int code1;
    
    if (_state == MessageState.FIRST)
      code1 = OP_BINARY;
    else
      code1 = OP_CONT;
    
    _state = MessageState.CONT;
    
    if (isFinal)
      code1 |= FLAG_FIN;

    if (length < 0x7e) {
      buffer[2] = (byte) code1;
      buffer[3] = (byte) (length);
      
      _os.write(buffer, 2, offset - 2);
    }
    else if (length >= 0x7e) {
      buffer[0] = (byte) code1;
      buffer[1] = (byte) 0x7e;
      buffer[2] = (byte) (length >> 8);
      buffer[3] = (byte) (length);
      
      _os.write(buffer, 0, offset);
    }
  }
  
  public void destroy()
    throws IOException
  {
    _state = MessageState.DESTROYED;
  }
  
  enum MessageState {
    IDLE,
    FIRST {
      @Override
      public boolean isActive() { return true; } 
    },
    CONT {
      @Override
      public boolean isActive() { return true; }
    },
    DESTROYED;
    
    public boolean isActive()
    {
      return false;
    }
  };
}
