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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * AMQP frame
 * <pre>
 * b0-b3 - size
 * b4    - data offset
 * b5    - type
 * b6-b7 - extra (frame type specific, channel)
 * </pre>
 */
public class AmqpWriter implements AmqpConstants {
  private static final L10N L = new L10N(AmqpWriter.class);
  
  private AmqpBaseWriter _os;
  
  public void init(OutputStream os)
  {
    _os = new AmqpStreamWriter(Vfs.openWrite(os));
  }
  
  public void initBase(AmqpBaseWriter os)
  {
    _os = os;
  }
  
  public void writeNull()
    throws IOException
  {
    _os.write(E_NULL);
  }
  
  public void writeBoolean(boolean value)
    throws IOException
  {
    _os.write(value ? E_TRUE : E_FALSE);
  }
  
  public void writeByte(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_BYTE_1);
    os.write(value);
  }
  
  public void writeUbyte(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_UBYTE_1);
    os.write(value);
  }
  
  public void writeShort(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_SHORT);
    os.write(value >> 8);
    os.write(value);
  }
  
  public void writeUshort(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_USHORT);
    os.write(value >> 8);
    os.write(value);
  }
  
  public void writeInt(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else if (-0x80 <= value && value <= 0x7f) {
      os.write(E_INT_1);
      os.write(value);
    }
    else {
      os.write(E_INT_4);
      os.write(value >> 24);
      os.write(value >> 16);
      os.write(value >> 8);
      os.write(value);
    }
  }
  
  public void writeUint(int value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else if (value >= 0 && value <= 0xff) {
      os.write(E_UINT_1);
      os.write(value);
    }
    else {
      os.write(E_UINT_4);
      os.write(value >> 24);
      os.write(value >> 16);
      os.write(value >> 8);
      os.write(value);
    }
  }
  
  public void writeLong(long value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == 0) {
      os.write(E_L0);
    }
    else if (-0x80 <= value && value <= 0x7f) {
      os.write(E_LONG_1);
      os.write((int) (value));
    }
    else {
      os.write(E_LONG_8);
      os.write((int) (value >> 56));
      os.write((int) (value >> 48));
      os.write((int) (value >> 40));
      os.write((int) (value >> 32));
      os.write((int) (value >> 24));
      os.write((int) (value >> 16));
      os.write((int) (value >> 8));
      os.write((int) (value));
    }
  }
  
  public void writeUlong(long value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == 0) {
      os.write(E_L0);
    }
    else if (0 <= value && value <= 0xff) {
      os.write(E_ULONG_1);
      os.write((int) (value));
    }
    else {
      os.write(E_ULONG_8);
      os.write((int) (value >> 56));
      os.write((int) (value >> 48));
      os.write((int) (value >> 40));
      os.write((int) (value >> 32));
      os.write((int) (value >> 24));
      os.write((int) (value >> 16));
      os.write((int) (value >> 8));
      os.write((int) (value));
    }
  }
  
  public void writeTimestamp(long value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_TIMESTAMP);
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) (value));
  }
  
  public void writeString(String value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == null) {
      os.write(E_NULL);
      return;
    }
    
    int len = calculateUtf8Length(value);
    
    if (len <= 0xff) {
      os.write(E_UTF8_1);
      os.write(len);
    }
    else {
      os.write(E_UTF8_4);
      os.write(len >> 24);
      os.write(len >> 16);
      os.write(len >> 8);
      os.write(len);
    }
    
    for (int i = 0; i < value.length(); i++) {
      os.write(value.charAt(i));
    }
  }
  
  private int calculateUtf8Length(String value)
  {
    int strlen = value.length();
    int len = 0;
    
    for (int i = 0; i < strlen; i++) {
      int ch = value.charAt(i);
      
      if (ch < 0x80) {
        len += 1;
      }
      else if (ch < 0x800) {
        len += 2;
      }
      else {
        len += 3;
      }
    }
    
    return len;
  }
  
  public void writeSymbol(String value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (value == null) {
      os.write(E_NULL);
      return;
    }
    
    int len = value.length();
    
    if (len <= 0xff) {
      os.write(E_SYMBOL_1);
      os.write(len);
    }
    else {
      os.write(E_SYMBOL_4);
      os.write(len >> 24);
      os.write(len >> 16);
      os.write(len >> 8);
      os.write(len);
    }
    
    for (int i = 0; i < len; i++) {
      os.write(value.charAt(i));
    }
  }
  
  public void writeBinary(byte []buffer)
    throws IOException
  {
    if (buffer == null) {
      _os.write(E_NULL);
      return;
    }

    writeBinary(buffer, 0, buffer.length);
  }
  
  public void writeBinary(byte []buffer, int offset, int length)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (buffer == null) {
      os.write(E_NULL);
      return;
    }
    
    if (length <= 0xff) {
      os.write(E_BIN_1);
      os.write(length);
      os.write(buffer, offset, length);
    }
    else {
      os.write(E_BIN_4);
      writeInt(length);
      os.write(buffer, offset, length);
    }
  }
  
  public void writeDescriptor(long code)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_DESCRIPTOR);
    writeUlong(code);
  }
  
  public void writeObject(AmqpAbstractPacket value)
    throws IOException
  {
    if (value != null)
      value.write(this);
    else
      writeNull();
  }
  
  public void writeObject(Object value)
    throws IOException
  {
    if (value == null) {
      writeNull();
      return;
    }

    if (value instanceof Long) {
      writeLong((Long) value);
    }
    else {
      writeString((String) value);
    }
  }
  
  public void writeList(List<?> list)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (list == null) {
      os.write(E_NULL);
      return;
    }
    
    throw new UnsupportedOperationException();
  }
  
  public int startList()
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_LIST_1);
    os.write(0xff);
    os.write(0xff);
    
    return os.getOffset();
  }
  
  public void finishList(int startOffset, int count)
  {
    AmqpBaseWriter os = _os;
    
    int finishOffset = os.getOffset();

    os.writeByte(startOffset - 2, (finishOffset - startOffset));
    os.writeByte(startOffset - 1, count);
  }
  
  public void writeArray(List<?> list)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (list == null) {
      os.write(E_NULL);
      return;
    }
    
    throw new UnsupportedOperationException();
  }
  
  public int startArray(int code)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_ARRAY_1);
    os.write(0xff);
    os.write(0xff);
    
    int offset = os.getOffset();
    
    os.write(code);
    
    return offset;
  }
  
  public void finishArray(int startOffset, int count)
  {
    AmqpBaseWriter os = _os;
    
    int finishOffset = os.getOffset();

    os.writeByte(startOffset - 2, (finishOffset - startOffset));
    os.writeByte(startOffset - 1, count);
  }
  
  private void writeSymbolValue(String value)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    int len = value.length();
    
    for (int i = 0; i < len; i++) {
      os.write(value.charAt(i));
    }
  }
  
  public void writeSymbolArray(List<String> list)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (list == null || list.size() == 0) {
      os.write(E_NULL);
      return;
    }
    
    if (list.size() == 1) {
      writeSymbol(list.get(0));
      return;
    }
     
    int offset = startArray(E_SYMBOL_1);
    
    for (int i = 0; i < list.size(); i++) {
      String value = list.get(i);
      
      os.write(value.length());
      
      writeSymbolValue(value);
    }
    
    finishArray(offset, list.size());
  }
  
  public void writeMap(Map<?,?> map)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (map == null || map.isEmpty()) {
      os.write(E_NULL);
      return;
    }
    
    int startOffset = startMap();
    
    for (Map.Entry<?,?> entry : map.entrySet()) {
      writeObject(entry.getKey());
      writeObject(entry.getValue());
    }
    
    finishMap(startOffset, map.size());
  }
  
  public void writeAnnotationsMap(Map<?,?> map)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (map == null || map.isEmpty()) {
      os.write(E_NULL);
      return;
    }
    
    int startOffset = startMap();
    
    for (Map.Entry<?,?> entry : map.entrySet()) {
      Object key = entry.getKey();

      if (key instanceof String) {
        writeSymbol((String) key);
      }
      else if (key instanceof Number) {
        writeUlong(((Number) key).longValue());
      }
      else {
        throw new IllegalArgumentException(L.l("'{0}' is an invalid amqp annotations key",
                                               key.getClass().getName()));
      }

      writeObject(entry.getValue());
    }
    
    finishMap(startOffset, map.size());
  }
  
  public void writeFieldsMap(Map<?,?> map)
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    if (map == null || map.isEmpty()) {
      os.write(E_NULL);
      return;
    }
    
    int startOffset = startMap();
    
    for (Map.Entry<?,?> entry : map.entrySet()) {
      Object key = entry.getKey();

      writeSymbol((String) key);
      writeObject(entry.getValue());
    }
    
    finishMap(startOffset, map.size());
  }

  public int startMap()
    throws IOException
  {
    AmqpBaseWriter os = _os;
    
    os.write(E_MAP_1);
    os.write(0xff);
    os.write(0xff);
    
    return os.getOffset();
  }
  
  public void finishMap(int startOffset, int count)
  {
    AmqpBaseWriter os = _os;
    
    int finishOffset = os.getOffset();

    os.writeByte(startOffset - 2, (finishOffset - startOffset));
    os.writeByte(startOffset - 1, count);
  }

  public void flush()
    throws IOException
  {
    _os.flush();
  }
}
