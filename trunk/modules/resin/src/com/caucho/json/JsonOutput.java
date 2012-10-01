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

package com.caucho.json;

import java.io.*;

import com.caucho.json.ser.JsonSerializer;
import com.caucho.json.ser.JsonSerializerFactory;
import com.caucho.vfs.WriteStream;

/**
 * Abstract output stream for JSON requests.
 *
 * <pre>
 * OutputStream os = ...; // from http connection
 * AbstractOutput out = new HessianSerializerOutput(os);
 * String value;
 *
 * out.startCall("hello");  // start hello call
 * out.writeString("arg1"); // write a string argument
 * out.completeCall();      // complete the call
 * </pre>
 */
public class JsonOutput {
  private static final char []NULL = new char[] { 'n', 'u', 'l', 'l' };
  private static final char []TRUE = new char[] { 't', 'r', 'u', 'e' };
  private static final char []FALSE = new char[] { 'f', 'a', 'l', 's', 'e' };

  private JsonSerializerFactory _factory = new JsonSerializerFactory();
  
  private PrintWriter _os;
  
  public JsonOutput()
  {
  }

  public JsonOutput(PrintWriter os)
  {
    init(os);
  }

  public JsonOutput(WriteStream out)
  {
    init(out.getPrintWriter());
  }
  
  /**
   * Initialize the output with a new underlying stream.
   */
  public void init(PrintWriter os)
  {
    _os = os;
  }

  public void writeObject(Object value) throws IOException
  {
    writeObject(value, false);
  }

  public void writeObject(Object value, boolean annotated)
    throws IOException
  {
    PrintWriter os = _os;

    if (value == null) {
      os.write(NULL, 0, 4);
      return;
    }

    JsonSerializer ser = _factory.getSerializer(value.getClass(), annotated);

    ser.write(this, value, annotated);
  }

  public void writeNull()
    throws IOException
  {
    PrintWriter os = _os;
    
    os.write(NULL, 0, 4);
  }
    
  public void writeBoolean(boolean value)
    throws IOException
  {
    PrintWriter os = _os;
    
    if (value)
      os.write(TRUE, 0, 4);
    else
      os.write(FALSE, 0, 5);
  }
    
  public void writeLong(long value)
    throws IOException
  {
    writeStringValue(String.valueOf(value));
  }
    
  public void writeDouble(double value)
    throws IOException
  {
    writeStringValue(String.valueOf(value));
  }

  public void writeString(String v)
    throws IOException
  {
    PrintWriter os = _os;
    
    if (v == null) {
      os.write(NULL, 0, 4);
      return;
    }
    
    os.write('"');

    int length = v.length();
    for (int i = 0; i < length; i++) {
      char ch = v.charAt(i);

      writeChar(os, ch);
    }

    os.write('"');
  }

  public void writeString(char []v, int offset, int length)
    throws IOException
  {
    PrintWriter os = _os;
    
    os.write('"');

    for (int i = 0; i < length; i++) {
      char ch = v[offset + i];

      writeChar(os, ch);
    }
    
    os.write('"');
  }

  private void writeChar(PrintWriter os, char ch)
    throws IOException
  {
    switch (ch) {
    case 0:
      os.write('\\');
      os.write('u');
      os.write('0');
      os.write('0');
      os.write('0');
      os.write('0');
      break;
    case '\n':
      os.write('\\');
      os.write('n');
      break;
    case '\r':
      os.write('\\');
      os.write('r');
      break;
    case '\t':
      os.write('\\');
      os.write('t');
      break;
    case '\b':
      os.write('\\');
      os.write('b');
      break;
    case '\f':
      os.write('\\');
      os.write('f');
      break;
    case '\\':
      os.write('\\');
      os.write('\\');
      break;
    case '"':
      os.write('\\');
      os.write('"');
      break;
    default:
      os.write(ch);
      break;
    }
  }

  public void writeArrayBegin()
    throws IOException
  {
    _os.write('[');
  }

  public void writeArrayComma()
    throws IOException
  {
    _os.write(',');
  }

  public void writeArrayEnd()
    throws IOException
  {
    _os.write(']');
  }

  public void writeMapBegin()
    throws IOException
  {
    _os.write('{');
  }

  public void writeMapComma()
    throws IOException
  {
    _os.write(',');
  }

  public void writeMapEntry(String key, Object value)
    throws IOException
  {
    writeMapEntry(key, value, false);
  }

  public void writeMapEntry(String key, Object value, boolean annotated)
    throws IOException
  {
    writeString(key);
    _os.write(':');
    writeObject((Serializable) value, annotated);
  }

  public void writeMapEnd()
    throws IOException
  {
    _os.write('}');
  }

  private void writeStringValue(String s)
    throws IOException
  {
    PrintWriter os = _os;
    
    int len = s.length();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      os.write(ch);
    }
  }

  public void flushBuffer()
    throws IOException
  {
  }

  public void flush()
    throws IOException
  {
  }

  public void close()
    throws IOException
  {
  }
}
