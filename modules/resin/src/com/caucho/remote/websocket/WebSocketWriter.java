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
import java.io.Writer;

/**
 * WebSocketOutputStream writes a single WebSocket packet.
 *
 * <code><pre>
 * </pre></code>
 */
public class WebSocketWriter extends Writer
  implements WebSocketConstants
{
  private OutputStream _os;
  private byte []_buffer;
  private int _offset;
  
  private MessageState _state = MessageState.IDLE;
  private boolean _isAutoFlush = true;

  public WebSocketWriter(OutputStream os, byte []buffer)
    throws IOException
  {
    _os = os;
    _buffer = buffer;
  }
  
  public void init()
    throws IOException
  {
    if (_state != MessageState.IDLE)
      throw new IllegalStateException(String.valueOf(_state));
    
    _state = MessageState.FIRST;
    
    if (_buffer.length <= _offset) {
      _os.flush();
    }
    
    _offset = 4;
  }

  @Override
  public void write(int ch)
  {
    if (! _state.isActive())
      throw new IllegalStateException(String.valueOf(_state));
    
    byte []buffer = _buffer;

    if (_offset == buffer.length) {
      complete(false);
    }

    if (ch < 0x80)
      buffer[_offset++] = (byte) ch;
    else if (ch < 0x800) {
      if (buffer.length <= _offset + 1) {
        complete(false);
      }
      
      buffer[_offset++] = (byte) (0xc0 + (ch >> 6)); 
      buffer[_offset++] = (byte) (0x80 + (ch & 0x3f));
    }
    else {
      if (buffer.length <= _offset + 2) { 
        complete(false);
      }
      
      buffer[_offset++] = (byte) (0xe0 + (ch >> 12)); 
      buffer[_offset++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
      buffer[_offset++] = (byte) (0x80 + (ch & 0x3f));
    }
  }

  @Override
  public void write(char []buffer, int offset, int length)
  {
    if (! _state.isActive())
      throw new IllegalStateException(String.valueOf(_state));
    
    byte []wsBuffer = _buffer;
    int wsOffset = _offset;

    for (; length > 0; length--) {
      if (wsOffset + 2 >= wsBuffer.length) {
        _offset = wsOffset;
        complete(false);
      }
      
      char ch = buffer[offset++];

      if (ch < 0x80)
        wsBuffer[wsOffset++] = (byte) ch;
      else if (ch < 0x800) {
        wsBuffer[wsOffset++] = (byte) (0xc0 + (ch >> 6)); 
        wsBuffer[wsOffset++] = (byte) (0x80 + (ch & 0x3f));
      }
      else {
        wsBuffer[wsOffset++] = (byte) (0xe0 + (ch >> 12)); 
        wsBuffer[wsOffset++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
        wsBuffer[wsOffset++] = (byte) (0x80 + (ch & 0x3f));
      }
    }
    
    _offset = wsOffset;
  }

  @Override
  public void flush()
  {
    try {
      complete(false);

      _os.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close()
  {
    if (_state == MessageState.IDLE)
      return;

    try {
      complete(true);
    
      _state = MessageState.IDLE;
    
      if (_isAutoFlush)
        _os.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void complete(boolean isFinal)
  {
    try {
      byte []buffer = _buffer;

      int offset = _offset;
      _offset = 4;
      int length = offset - 4;
      
      // don't flush empty chunk
      if (length == 0 && ! isFinal)
        return;

      int code1;

      if (_state == MessageState.FIRST)
        code1 = OP_TEXT;
      else
        code1 = OP_CONT;

      _state = MessageState.CONT;

      if (isFinal)
        code1 |= FLAG_FIN;

      if (length < 0x7e) {
        buffer[2] = (byte) code1;
        buffer[3] = (byte) length;

        _os.write(buffer, 2, offset - 2);
      }
      else {
        buffer[0] = (byte) code1;
        buffer[1] = (byte) 0x7e;
        buffer[2] = (byte) (length >> 8);
        buffer[3] = (byte) (length);

        _os.write(buffer, 0, offset);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
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
  
  static class DummyWriter extends Writer {
    public void write(int ch) throws IOException
    {
      
    }
    
    public void write(char []data, int offset, int length) throws IOException
    {
      
    }
    
    public void flush()
    {
      
    }
    
    public void close()
    {
    }
  }
}
