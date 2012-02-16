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

package com.caucho.mqueue.amqp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.vfs.ReadStream;

/**
 * AMQP frame
 * <pre>
 * b0-b3 - size
 * b4    - data offset
 * b5    - type
 * b6-b7 - extra (frame type specific, channel)
 * </pre>
 */
public class AmqpReader implements AmqpConstants {
  private ReadStream _is;
  private boolean _isNull;
  
  public void init(ReadStream is)
  {
    _is = is;
  }
  
  public int read()
    throws IOException
  {
    return _is.read();
  }
  
  public boolean readBoolean()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
      
    switch (code) {
    case E_NULL:
      _isNull = true;
      return false;
      
    case E_TRUE:
      return true;
      
    case E_FALSE:
      return false;
      
    case E_BOOLEAN_1:
      return is.read() != 0;
      
    default:
      throw new IOException("unknown boolean code: " + Integer.toHexString(code));
    }
  }
 
  public int readInt()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
      
    int code = is.read();
      
    switch (code) {
    case E_NULL:
      _isNull = true;
      return 0;
      
    case E_I0:
      return 0;
      
    case E_BYTE_1:
    case E_INT_1:
      return (byte) is.read();
      
    case E_UBYTE_1:
    case E_UINT_1:
      return is.read() & 0xff;
      
    case E_SHORT:
      return (short) readShort(is);
      
    case E_USHORT:
      return readShort(is) & 0xffff;
      
    case E_INT_4:
    case E_UINT_4:
      return readInt(is);
      
    default:
      throw new IOException("unknown int code: " + Integer.toHexString(code));
    }
  }
  
  public String readSymbol()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_SYMBOL_1:
      return readSymbol(is.read() & 0xff);
      
    case E_SYMBOL_4:
      return readSymbol(readInt(is));
      
    default:
      throw new IOException("unknown symbol code: " + Integer.toHexString(code));
    }
  }
  
  public String readUtf8()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_UTF8_1:
      return readUtf8(is.read() & 0xff);
      
    case E_UTF8_4:
      return readUtf8(readInt(is));
      
    default:
      throw new IOException("unknown symbol code: " + Integer.toHexString(code));
    }
  }
  
  public List<?> readList()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    default:
      throw new IOException("unknown array code: " + Integer.toHexString(code));
    }
  }
  
  public List<?> readArray()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    default:
      throw new IOException("unknown array code: " + Integer.toHexString(code));
    }
  }
  
  public Map<?,?> readMap()
    throws IOException
  {
    _isNull = false;
    
    ReadStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    default:
      throw new IOException("unknown map code: " + Integer.toHexString(code));
    }
  }

  private String readSymbol(int length)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    ReadStream is = _is;
    for (int i = 0; i < length; i++) {
      int ch = _is.read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private String readUtf8(int length)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    ReadStream is = _is;
    for (int i = 0; i < length; i++) {
      int ch = _is.read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private int readShort(ReadStream is)
    throws IOException
  {
    return (((is.read() & 0xff) << 8)
           + ((is.read() & 0xff)));
  }
  
  private int readInt(ReadStream is)
    throws IOException
  {
    return (((is.read() & 0xff) << 24)
           + ((is.read() & 0xff) << 16)
           + ((is.read() & 0xff) << 8)
           + ((is.read() & 0xff)));
  }
}
