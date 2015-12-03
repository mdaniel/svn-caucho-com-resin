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
import java.io.OutputStream;

import com.caucho.vfs.TempBuffer;

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
  private static final int BINARY_PASSTHROUGH_SIZE = 2048;
  
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

  public WebSocketOutputStream(OutputStream os)
    throws IOException
  {
    this(os, TempBuffer.allocate().getBuffer());
  }
  
  public void setAutoFlush(boolean isAutoFlush)
  {
    _isAutoFlush = isAutoFlush;
  }
  
  public void init()
  {
    if (_state != MessageState.IDLE) {
      throw new IllegalStateException(String.valueOf(_state));
    }
    
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

    if (length >= BINARY_PASSTHROUGH_SIZE) {
      complete(false);
      
      while (length > 0) {
        int sublen = Math.min(0xffff, length);
        
        int writeOffset = fillHeader(false, sublen + 4);
        
        _os.write(wsBuffer, writeOffset, 4 - writeOffset);
        _os.write(buffer, offset, sublen);
        
        offset += sublen;
        length -= sublen;
      }
      
      return;
    }
      
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
    
    try {
      complete(true);
      
      if (_isAutoFlush) {
        _os.flush();
      }
    } finally {
      _state = MessageState.IDLE;
    }
  }

  private void complete(boolean isFinal)
    throws IOException
  {
    byte []buffer = _buffer;
    
    int offset = _offset;
    _offset = 4;
    
    int writeOffset = fillHeader(isFinal, offset); 

    // don't flush empty chunk
    if (writeOffset < 0)
      return;
    
    _os.write(buffer, writeOffset, offset - writeOffset);
  }

  private int fillHeader(boolean isFinal, int tailOffset)
    throws IOException
  {
    byte []buffer = _buffer;

    // don't flush empty chunk
    if (tailOffset == 4 && ! isFinal)
      return -1;

    int length = tailOffset - 4;
    
    int code1;
    
    if (_state == MessageState.FIRST)
      code1 = OP_BINARY;
    else
      code1 = OP_CONT;
    
    _state = MessageState.CONT;
    
    if (isFinal) {
      code1 |= FLAG_FIN;
    }

    if (length < 0x7e) {
      buffer[2] = (byte) code1;
      buffer[3] = (byte) (length);
    
      return 2;
    }
    else if (length <= 0xffff) {
      buffer[0] = (byte) code1;
      buffer[1] = (byte) 0x7e;
      buffer[2] = (byte) (length >> 8);
      buffer[3] = (byte) (length);
      
      return 0;
    }
    else {
      throw new IllegalStateException();
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
