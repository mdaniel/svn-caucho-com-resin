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
import java.io.Reader;

/**
 * WebSocketReader reads a single WebSocket packet.
 *
 * <code><pre>
 * 0x00 utf-8 data 0xff
 * </pre></code>
 */
public class WebSocketReader extends Reader
  implements WebSocketConstants
{
  private static final char UTF8_ERROR = 0xfeff;
  
  private FrameInputStream _is;

  private boolean _isFinal;
  private long _length;
  
  private final char []_buffer = new char[256 + 1];
  private int _charOffset;
  private int _charLength;

  public WebSocketReader(FrameInputStream is)
    throws IOException
  {
    _is = is;
  }

  public void init()
    throws IOException
  {
    init(_is.getLength(), _is.isFinal());
  }

  public void init(long length, boolean isFinal)
  {
    _isFinal = isFinal;
    _length = length;
  }

  public long getLength()
  {
    return _length;
  }

  @Override
  public int read()
    throws IOException
  {
    int offset = _charOffset;
    int length = _charLength;
    
    if (length <= offset) {
      if (! fillBuffer())
        return -1;
      
      offset = _charOffset;
      length = _charLength;
    }
    
    _charOffset = offset + 1;

    return _buffer[offset];
  }
  
  @Override
  public int read(char []buffer, int offset, int length)
    throws IOException
  {
    int charOffset = _charOffset;
    int charLength = _charLength;
    
    if (charLength <= charOffset) {
      if (! fillBuffer()) {
        return -1;
      }
      
      charOffset = _charOffset;
      charLength = _charLength;
    }
    
    int sublen = charLength - charOffset;
    
    if (length < sublen)
      sublen = length;
    
    System.arraycopy(_buffer, charOffset, buffer, offset, sublen);
    
    _charOffset = charOffset + sublen;
    
    return sublen;
  }
  
  private boolean fillBuffer()
    throws IOException
  {
    _charOffset = 0;
    
    int charLength = 0;
    char []charBuffer = _buffer;
    int length = charBuffer.length - 1;
    
    while (charLength < length) {
      int d1 = readByte();
    
      if (d1 < 0) {
        _charLength = charLength;
        return charLength > 0;
      }
      
      char ch;
      
      if (d1 < 0x80) {
        ch = (char) d1;
      }
      else if ((d1 & 0xe0) == 0xc0) {
        int d2 = readByte();
      
        ch = (char) (((d1 & 0x1f) << 6) + (d2 & 0x3f));
        
        if (d2 < 0) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d2 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if (ch < 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
      }
      else if ((d1 & 0xf0) == 0xe0){
        int d2 = readByte();
        int d3 = readByte();
        
        ch = (char) (((d1 & 0x0f) << 12) + ((d2 & 0x3f) << 6) + (d3 & 0x3f)); 

        if (d3 < 0) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d2 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d3 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if (ch < 0x800) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if (0xd800 <= ch && ch <= 0xdfff) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
      }
      else if ((d1 & 0xf8) == 0xf0){
        int d2 = readByte();
        int d3 = readByte();
        int d4 = readByte();
        
        int cp = (((d1 & 0x7) << 18)
                   + ((d2 & 0x3f) << 12)
                   + ((d3 & 0x3f) << 6)
                   + ((d4 & 0x3f)));
        
        cp -= 0x10000;
        
        char h = (char) (0xd800 + ((cp >> 10) & 0x3ff));
        
        charBuffer[charLength++] = h;
        
        ch = (char) (0xdc00 + (cp & 0x3ff));
        
        if (d4 < 0) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d2 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d3 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if ((d4 & 0xc0) != 0x80) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if (cp < 0x0) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
        else if (cp >= 0x100000) {
          _is.closeError(CLOSE_UTF8, "illegal utf-8");
          ch = UTF8_ERROR;
        }
      }
      else {
        _is.closeError(CLOSE_UTF8, "illegal utf-8");
        
        ch = UTF8_ERROR;
      }
      
      charBuffer[charLength++] = ch;
    }
    
    _charLength = charLength;
    
    return true;
  }
  
  private int readByte()
    throws IOException
  {
    FrameInputStream is = _is;

    while (_length == 0 && ! _isFinal) {
      if (! is.readFrameHeader()) {
        return -1;
      }
      
      if (is.getOpcode() == OP_CLOSE) {
        return -1;
      }
      else if (is.getOpcode() != OP_CONT) {
        _is.closeError(CLOSE_ERROR, "illegal fragment");
        return -1;
      }
      
      _isFinal = is.isFinal();
      _length = is.getLength();
    }

    if (_length > 0) {
      int ch = is.read();
      _length--;
      return ch;
    }
    else
      return -1;
  }

  @Override
  public void close()
  throws IOException
  {
    while (read() >= 0) {
    }
  }
}
