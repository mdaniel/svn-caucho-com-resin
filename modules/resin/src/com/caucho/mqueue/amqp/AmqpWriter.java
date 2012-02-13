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
  private WriteStream _os;
  
  public void init(WriteStream os)
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
    WriteStream os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else {
      os.write(E_BYTE_1);
      os.write(value);
    }
  }
  
  public void writeUbyte(int value)
    throws IOException
  {
    WriteStream os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else {
      os.write(E_UBYTE_1);
      os.write(value);
    }
  }
  
  public void writeShort(int value)
    throws IOException
  {
    WriteStream os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else {
      os.write(E_SHORT);
      os.write(value >> 8);
      os.write(value);
    }
  }
  
  public void writeUshort(int value)
    throws IOException
  {
    WriteStream os = _os;
    
    if (value == 0) {
      os.write(E_I0);
    }
    else {
      os.write(E_USHORT);
      os.write(value >> 8);
      os.write(value);
    }
  }
  
  public void writeInt(int value)
    throws IOException
  {
    WriteStream os = _os;
    
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
    WriteStream os = _os;
    
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
    WriteStream os = _os;
    
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
    WriteStream os = _os;
    
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
    WriteStream os = _os;
    
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
  
  public void writeUtf8(String value)
    throws IOException
  {
    WriteStream os = _os;
    
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
    
    os.printUtf8(value, 0, value.length());
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
    WriteStream os = _os;
    
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
    
    os.print(value, 0, len);
  }
  
  public void flush()
    throws IOException
  {
    _os.flush();
  }
}
