/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.env;

import java.io.IOException;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP string value.
 */
public class StringBuilderValue extends StringValue {
  private char []_buffer;
  private int _length;

  private String _value;

  public StringBuilderValue()
  {
    _buffer = new char[128];
  }

  public StringBuilderValue(int capacity)
  {
    if (capacity < 64)
      capacity = 128;
    else
      capacity = 2 * capacity;

    _buffer = new char[capacity];
  }

  public StringBuilderValue(String value)
  {
    this(value.length());

    int length = value.length();

    _length = length;

    value.getChars(0, length, _buffer, 0);
  }

  public StringBuilderValue(char []buffer, int offset, int length)
  {
    int capacity;

    if (length < 64)
      capacity = 128;
    else
      capacity = 2 * length;

    _buffer = new char[capacity];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public StringBuilderValue(char []buffer)
  {
    this(buffer, 0, buffer.length);
  }

  public StringBuilderValue(char []buffer, int offset, int length,
                            boolean isExact)
  {
    _buffer = new char[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  /**
   * Returns the value.
   */
  public String getValue()
  {
    return toString();
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "string";
  }

  /**
   * Interns the string.
   */
  public InternStringValue intern(Quercus quercus)
  {
    return quercus.intern(toString());
  }

  /**
   * Returns true for a long
   */
  public boolean isLongConvertible()
  {
    char []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return false;

    for (int i = 0; i < len; i++) {
      char ch = _buffer[i];

      if (! ('0' <= ch && ch <= '9'))
        return false;
    }

    return true;
  }

  /**
   * Returns true for a double
   */
  public boolean isDouble()
  {
    return getNumericType() == IS_DOUBLE;
  }

  /**
   * Returns true for a number
   */
  public boolean isNumber()
  {
    return getNumericType() != IS_STRING;
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Converts to a double.
   */
  protected int getNumericType()
  {
    char []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return IS_STRING;

    int i = 0;
    int ch = 0;
    boolean hasPoint = false;

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return IS_STRING;

    ch = buffer[i];

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
        return IS_DOUBLE;
      }

      return IS_STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return IS_STRING;

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    if (len <= i)
      return IS_LONG;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = buffer[i]) && ch <= '9' ||
                       ch == '+' || ch == '-' || ch == 'e' || ch == 'E');
           i++) {
      }

      if (i < len)
        return IS_STRING;
      else
        return IS_DOUBLE;
    }
    else
      return IS_STRING;
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    if (_length == 0)
      return false;
    else if (_length == 1 && _buffer[0] == '0')
      return false;
    else
      return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return toLong(_buffer, 0, _length);
  }

  /**
   * Converts to a long.
   */
  public static long toLong(char []buffer, int offset, int len)
  {
    if (len == 0)
      return 0;

    long value = 0;
    long sign = 1;

    int i = 0;
    int end = offset + len;

    if (buffer[offset] == '-') {
      sign = -1;
      offset++;
    }

    while (offset < end) {
      char ch = buffer[offset++];

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return sign * value;
    }

    return value;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    char []buffer = _buffer;
    int len = _length;
    int i = 0;
    int ch = 0;

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = buffer[i]) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i == 0)
      return 0;
    else if (i == len)
      return Double.parseDouble(toString());
    else
      return Double.parseDouble(new String(_buffer, 0, i));
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_value == null)
      _value = new String(_buffer, 0, _length);

    return _value;
  }

  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    if (_value == null)
      _value = new String(_buffer, 0, _length);

    return _value;
  }

  /**
   * Append to a string builder.
   */
  public void appendTo(StringBuilderValue sb)
  {
    sb.append(_buffer, 0, _length);
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    char []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    char ch = buffer[i];
    if (ch == '-') {
      sign = -1;
      i++;
    }

    for (; i < len; i++) {
      ch = buffer[i];

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return this;
    }

    return new LongValue(sign * value);
  }

  /**
   * Converts to a byte array, with no consideration of character encoding.
   * Each character becomes one byte, characters with values above 255 are
   * not correctly preserved.
   */
  public byte[] toBytes()
  {
    char []buffer = _buffer;

    final int len = _length;
    byte[] bytes = new byte[len];

    for (int i = 0; i < len; i++) {
      bytes[i] = (byte) buffer[i];
    }

    return bytes;
  }

  /**
   * Returns the character at an index
   */
  public Value get(Value key)
  {
    return charAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  public Value getRef(Value key)
  {
    return charAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  public Value charAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return StringValue.EMPTY;
    else
      return StringValue.create(_buffer[(int) index]);
  }

  /**
   * sets the character at an index
   */
  public Value setCharAt(long index, String value)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return this;
    else {
      StringBuilderValue sb = new StringBuilderValue(_buffer, 0, (int) index);
      sb.append(value);
      sb.append(_buffer, (int) (index + 1), (int) (len - index - 1));

      return sb;
    }
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("new InternStringValue(\"");
    out.printJavaString(toString());
    out.print("\")");
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(_buffer, 0, _length);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("s:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(_buffer, 0, _length);
    sb.append("\";");
  }

  // append code

  /**
   * Append a Java string to the value.
   */
  public final StringBuilderValue append(String s)
  {
    int len = s.length();

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(0, len, _buffer, _length);

    _length += len;

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  public final StringBuilderValue append(String s, int start, int end)
  {
    int len = end - start;

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(start, end, _buffer, _length);

    _length += len;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  public final StringBuilderValue append(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(buf, offset, _buffer, _length, length);

    _length += length;

    return this;
  }

  /**
   * Append a Java double to the value.
   */
  public final StringBuilderValue append(char []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a Java char to the value.
   */
  public final StringBuilderValue append(char v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = v;

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  public final StringBuilderValue append(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  public final StringBuilderValue append(long v)
  {
    // XXX: change for perf
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  public final StringBuilderValue append(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public final StringBuilderValue append(Object v)
  {
    return append(v.toString());
  }

  /**
   * Append a Java value to the value.
   */
  public final StringBuilderValue append(Value v)
  {
    v.appendTo(this);

    return this;
  }

  private void ensureCapacity(int newCapacity)
  {
    if (newCapacity <= _buffer.length)
      return;
    else if (newCapacity < 4096)
      newCapacity = 2 * newCapacity;
    else
      newCapacity = newCapacity + 4096;

    char []buffer = new char[newCapacity];
    System.arraycopy(_buffer, 0, buffer, 0, _length);

    _buffer = buffer;
  }
}

