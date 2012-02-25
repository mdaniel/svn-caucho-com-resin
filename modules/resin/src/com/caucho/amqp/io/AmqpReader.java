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

package com.caucho.amqp.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

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
  private InputStream _is;
  private boolean _isNull;
  
  public void init(InputStream is)
  {
    _is = is;
  }
  
  public boolean isNull()
  {
    return _isNull;
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
    
    InputStream is = _is;
    
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
    
    InputStream is = _is;
      
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
  
  public long readLong()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
      
    int code = is.read();
      
    switch (code) {
    case E_NULL:
      _isNull = true;
      return 0;
      
    case E_I0:
    case E_L0:
      return 0;
      
    case E_BYTE_1:
    case E_INT_1:
    case E_LONG_1:
      return (byte) is.read();
      
    case E_UBYTE_1:
    case E_UINT_1:
    case E_ULONG_1:
      return is.read() & 0xff;
      
    case E_SHORT:
      return (short) readShort(is);
      
    case E_USHORT:
      return readShort(is) & 0xffff;
      
    case E_INT_4:
    case E_UINT_4:
      return readInt(is) & 0xffffffffL;
      
    case E_LONG_8:
    case E_ULONG_8:
      return readLong(is);
      
    default:
      throw new IOException("unknown long code: " + Integer.toHexString(code));
    }
  }
  
  public List<String> readSymbolArray()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
    int code = is.read();
    
    if (code == E_NULL) {
      return null;
    }
    
    ArrayList<String> values = new ArrayList<String>();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_SYMBOL_1:
    {
      String value = readSymbol(is.read() & 0xff);
      values.add(value);
      return values;
    }
      
    case E_SYMBOL_4:
    {
      String value = readSymbol(readInt(is));
      values.add(value);
      return values;
    }
      
    default:
      throw new IOException("unknown symbol array code: " + Integer.toHexString(code));
    }
  }

  public String readSymbol()
    throws IOException
  {
      _isNull = false;
      
      InputStream is = _is;
      
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

  public byte []readBinary()
    throws IOException
  {
    _isNull = false;
      
    InputStream is = _is;
      
    int code = is.read();
    int len;
      
    switch (code) {
    case E_NULL:
      return null;
        
    case E_BIN_1:
    {
      len = (is.read() & 0xff);
      byte []data = new byte[len];
     
      // XXX: read, all
      is.read(data, 0, data.length);
      
      return data;
    }
        
    case E_BIN_4:
    {
      len = readInt(is);
      byte []data = new byte[len];
       
      // XXX: read, all
      is.read(data, 0, data.length);
        
      return data;
    }
        
    default:
      throw new IOException("unknown binary code: " + Integer.toHexString(code));
    }
  }

  public String readString()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
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
  
  public long readDescriptor()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case -1:
      return -1;
      
    case E_NULL:
      _isNull = true;
      return 0;
      
    case E_DESCRIPTOR:
      return readLong();
      
    default:
      throw new IOException("unknown descriptor code: " + Integer.toHexString(code));
    }
  }
  
  public <T extends AmqpAbstractPacket>
  T readObject(Class<T> type)
    throws IOException
  {
    long descriptor = readDescriptor();
    
    if (_isNull) {
      return null;
    }
   
    return readObject(descriptor, type);
  }
  
  public Object readObject(long descriptor)
    throws IOException
  {
    return readObject(descriptor, AmqpAbstractPacket.class);
  }
  
  public <T extends AmqpAbstractPacket>
  T readObject(long descriptor, Class<T> type)
    throws IOException
  {
    return AmqpAbstractPacket.readType(this, descriptor, type);
  }
  
  public Object readObject()
    throws IOException
  {
    InputStream is = _is;
    
    int code = is.read();
    int len;
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_UTF8_1:
      len = is.read() & 0xff;
      return readUtf8(len);
      
    case E_UTF8_4:
      len = readInt(is);
      return readUtf8(len);
          
    default:
      throw new IllegalStateException("unknown code: 0x" + Integer.toHexString(code));
    }
  }
  
  public List<?> readList()
    throws IOException
  {
      _isNull = false;
      
      InputStream is = _is;
      
      int code = is.read();
      
      switch (code) {
      case E_NULL:
        return null;
        
      default:
        throw new IOException("unknown array code: " + Integer.toHexString(code));
      }
  }
  
  public int startList()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return 0;
      
    case E_LIST_0:
      return 0;
      
    case E_LIST_1:
    {
      int size = read() & 0xff;
      int count = read() & 0xff;
      
      return count;
    }
    
    case E_LIST_4:
    {
      int size = readInt(is);
      int count = readInt(is);
      
      return count;
    }
      
    default:
      throw new IOException("unknown array code: " + Integer.toHexString(code));
    }
  }
  
  public void endList()
  {
    
  }
  
  public List<?> readArray()
    throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
    int code = is.read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    default:
      throw new IOException("unknown array code: " + Integer.toHexString(code));
    }
  }
  
  public Map<String,?> readFieldMap()
    throws IOException
  {
    return (Map) readMap();
  }
  
  public Map<?,?> readMap()
  throws IOException
  {
    _isNull = false;
    
    InputStream is = _is;
    
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
    
    InputStream is = _is;
    for (int i = 0; i < length; i++) {
      int ch = is.read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private String readUtf8(int length)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    InputStream is = _is;
    for (int i = 0; i < length; i++) {
      int ch = _is.read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private int readShort(InputStream is)
    throws IOException
  {
    return (((is.read() & 0xff) << 8)
           + ((is.read() & 0xff)));
  }
  
  private int readInt(InputStream is)
    throws IOException
  {
    return (((is.read() & 0xff) << 24)
           + ((is.read() & 0xff) << 16)
           + ((is.read() & 0xff) << 8)
           + ((is.read() & 0xff)));
  }
  
  private long readLong(InputStream is)
    throws IOException
  {
    return (((is.read() & 0xffL) << 56)
           + ((is.read() & 0xffL) << 48)
           + ((is.read() & 0xffL) << 40)
           + ((is.read() & 0xffL) << 32)
           + ((is.read() & 0xffL) << 24)
           + ((is.read() & 0xffL) << 16)
           + ((is.read() & 0xffL) << 8)
           + ((is.read() & 0xffL)));
  }
}
