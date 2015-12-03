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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.caucho.amqp.AmqpException;
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
  
  private byte []_buffer;
  private int _offset;
  private int _length;
  
  public AmqpReader()
  {
    _buffer = new byte[256];
  }
  
  public void init(InputStream is)
  {
    _is = is;
    
    _offset = 0;
    _length = 0;
  }
  
  public int getFrameAvailable()
  {
    try {
      return _length - _offset + _is.available();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  public boolean isNull()
  {
    return _isNull;
  }
  
  public int read()
    throws IOException
  {
    int offset = _offset;
    int length = _length;
    
    if (length <= offset) {
      if (! fillBuffer()) {
        return -1;
      }
      
      offset = _offset;
      length = _length;
    }
    
    int value = _buffer[offset++] & 0xff;
    
    _offset = offset;
    
    return value;
  }
  
  public long peekDescriptor()
    throws IOException
  {
    ensureBuffer(10);
    
    int offset = _offset;
    
    long desc = readDescriptor();
    
    _offset = offset;
    
    return desc;
  }
  
  private boolean ensureBuffer(int len)
    throws IOException
  {
    if (len <= _length - _offset)
      return true;
    
    System.arraycopy(_buffer, _offset, _buffer, 0, _length - _offset);
    
    int sublen = _buffer.length - _offset;
    
    sublen = _is.read(_buffer, _offset, sublen);
    
    if (sublen >= 0) {
      _length = _offset + sublen;
      _offset = 0;
      return true;
    }
    else {
      _length = _offset;
      _offset = 0;
      
      return false;
    }
  }
  
  private boolean fillBuffer()
    throws IOException
  {
    _length = _is.read(_buffer, 0, _buffer.length);
    _offset = 0;
    
    return _length > 0;
  }
  
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    int readLength = 0;
    
    while (readLength < length) {
      int ch = read();
      
      if (ch < 0) {
        return readLength > 0 ? readLength : -1;
      }
      
      buffer[offset + readLength] = (byte) ch;
      
      readLength++;
    }
    
    return readLength;
  }
  
  public boolean readBoolean()
    throws IOException
  {
    _isNull = false;
    
    int code = read();
      
    switch (code) {
    case E_NULL:
      _isNull = true;
      return false;
      
    case E_TRUE:
      return true;
      
    case E_FALSE:
      return false;
      
    case E_BOOLEAN_1:
      return read() != 0;
      
    default:
      throw new IOException("unknown boolean code: " + Integer.toHexString(code));
    }
  }
 
  public int readInt()
    throws IOException
  {
    _isNull = false;
    
    int code = read();
      
    switch (code) {
    case E_NULL:
      _isNull = true;
      return 0;
      
    case E_I0:
      return 0;
      
    case E_BYTE_1:
    case E_INT_1:
      return (byte) read();
      
    case E_UBYTE_1:
    case E_UINT_1:
      return read() & 0xff;
      
    case E_SHORT:
      return (short) readShort();
      
    case E_USHORT:
      return readShort() & 0xffff;
      
    case E_INT_4:
    case E_UINT_4:
      return readIntImpl();
      
    default:
      throw new IOException("unknown int code: " + Integer.toHexString(code));
    }
  }
  
  public long readLong()
    throws IOException
  {
    _isNull = false;
    
    int code = read();
      
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
      return (byte) read();
      
    case E_UBYTE_1:
    case E_UINT_1:
    case E_ULONG_1:
      return read() & 0xff;
      
    case E_SHORT:
      return (short) readShort();
      
    case E_USHORT:
      return readShort() & 0xffff;
      
    case E_INT_4:
    case E_UINT_4:
      return readIntImpl() & 0xffffffffL;
      
    case E_LONG_8:
    case E_ULONG_8:
      return readLongImpl();
      
    default:
      throw new IOException("unknown long code: " + Integer.toHexString(code));
    }
  }
  
  public List<String> readSymbolArray()
    throws IOException
  {
    _isNull = false;
    
    int code = read();
    
    if (code == E_NULL) {
      return null;
    }
    
    ArrayList<String> values = new ArrayList<String>();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_SYMBOL_1:
    {
      String value = readSymbol(read() & 0xff);
      values.add(value);
      return values;
    }
      
    case E_SYMBOL_4:
    {
      String value = readSymbol(readIntImpl());
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
      
      int code = read();
      
      switch (code) {
      case E_NULL:
        return null;
        
      case E_SYMBOL_1:
        return readSymbol(read() & 0xff);
        
      case E_SYMBOL_4:
        return readSymbol(readIntImpl());
        
      default:
        throw new IOException("unknown symbol code: " + Integer.toHexString(code));
      }
  }

  public byte []readBinary()
    throws IOException
  {
    _isNull = false;
      
    int code = read();
    int len;
      
    switch (code) {
    case E_NULL:
      return null;
        
    case E_BIN_1:
    {
      len = (read() & 0xff);
      byte []data = new byte[len];
     
      // XXX: read, all
      for (int i = 0; i < len; i++) {
        data[i] = (byte) read();
      }
      
      return data;
    }
        
    case E_BIN_4:
    {
      len = readIntImpl();
      byte []data = new byte[len];
       
      // XXX: read, all
      for (int i = 0; i < len; i++) {
        data[i] = (byte) read();
      }
        
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
    
    int code = read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_UTF8_1:
      return readUtf8(read() & 0xff);
      
    case E_UTF8_4:
      return readUtf8(readIntImpl());
      
    default:
      throw new IOException("unknown symbol code: " + Integer.toHexString(code));
    }
  }
  
  public long readDescriptor()
    throws IOException
  {
    _isNull = false;
    
    int code = read();
    
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
    int code = read();
    int len;
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_UTF8_1:
      len = read() & 0xff;
      return readUtf8(len);
      
    case E_UTF8_4:
      len = readIntImpl();
      return readUtf8(len);
      
    case E_SYMBOL_1:
      len = read() & 0xff;
      return readSymbol(len);
      
    case E_SYMBOL_4:
      len = readIntImpl();
      return readSymbol(len);
          
    default:
      throw new IllegalStateException("unknown code: 0x" + Integer.toHexString(code));
    }
  }
  
  public List<?> readList()
    throws IOException
  {
    _isNull = false;
      
    int code = read();
      
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
    
    int code = read();
    
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
      int size = readIntImpl();
      int count = readIntImpl();
      
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
    
    int code = read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    default:
      throw new IOException("unknown array code: " + Integer.toHexString(code));
    }
  }
  
  public Map<String,Object> readFieldMap()
    throws IOException
  {
    return (Map) readMap();
  }
  
  public Map<?,?> readMap()
  throws IOException
  {
    _isNull = false;
    
    int code = read();
    
    switch (code) {
    case E_NULL:
      return null;
      
    case E_MAP_1:
    {
      int length = read() & 0xff;
      int size = read() & 0xff;
      
      HashMap<Object,Object> map = new HashMap<Object,Object>();
      
      for (int i = 0; i < size; i++) {
        map.put(readObject(), readObject());
      }
      
      return map;
    }
      
    default:
      throw new IOException("unknown map code: " + Integer.toHexString(code));
    }
  }

  private String readSymbol(int length)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < length; i++) {
      int ch = read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private String readUtf8(int length)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < length; i++) {
      int ch = read();
      
      sb.append((char) ch);
    }
    
    return sb.toString();
  }

  private int readShort()
    throws IOException
  {
    return (((read() & 0xff) << 8)
           + ((read() & 0xff)));
  }
  
  private int readIntImpl()
    throws IOException
  {
    return (((read() & 0xff) << 24)
           + ((read() & 0xff) << 16)
           + ((read() & 0xff) << 8)
           + ((read() & 0xff)));
  }
  
  private long readLongImpl()
    throws IOException
  {
    return (((read() & 0xffL) << 56)
           + ((read() & 0xffL) << 48)
           + ((read() & 0xffL) << 40)
           + ((read() & 0xffL) << 32)
           + ((read() & 0xffL) << 24)
           + ((read() & 0xffL) << 16)
           + ((read() & 0xffL) << 8)
           + ((read() & 0xffL)));
  }
}
