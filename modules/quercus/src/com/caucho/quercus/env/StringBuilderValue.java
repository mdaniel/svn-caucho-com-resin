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

import com.caucho.quercus.Quercus;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a PHP string value.
 */
public class StringBuilderValue extends UnicodeValue {
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
      capacity = 64;

    _buffer = new char[capacity];
  }

  public StringBuilderValue(String value)
  {
    if (value == null)
      value = "";
    
    int length = value.length();
    
    _buffer = new char[length];

    _length = length;

    value.getChars(0, length, _buffer, 0);

    _value = value;
  }

  public StringBuilderValue(String value, int minLength)
  {
    if (value == null)
      value = "";
    
    _length = value.length();
    _value = value;
    
    int length = _length;

    if (length < minLength)
      length = minLength;
    
    _buffer = new char[length];

    value.getChars(0, _length, _buffer, 0);
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

  public StringBuilderValue(Character []buffer)
  {
    int length = buffer.length;
    
    _buffer =  new char[length];
    _length = length;
    
    for (int i = 0; i < length; i++) {
      _buffer[i] = buffer[i].charValue();
    }
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
      return true;

    int i = 0;
    char ch = _buffer[0];

    if (ch == '-' || ch == '+')
      i++;

    for (; i < len; i++) {
      ch = _buffer[i];

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
  @Override
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

    if (len == 0) {
      // php/120y
      return IS_STRING;
    }

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

    char ch = buffer[offset];
    if (ch == '-') {
      sign = -1;
      offset++;
    }
    else if (ch == '+')
      offset++;

    while (offset < end) {
      ch = buffer[offset++];

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return sign * value;
    }

    return sign * value;
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

      if (i == 1)
	return 0;
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

    try {
      if (i == len)
	return Double.parseDouble(toString());
      else
	return Double.parseDouble(new String(_buffer, 0, i));
    } catch (NumberFormatException e) {
      return 0;
    }
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
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    return new StringBuilderValue(_buffer, 0, _length);
  }

  /**
   * Append to a string builder.
   */
  @Override
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
    return charValueAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  public Value getRef(Value key)
  {
    return charValueAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
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
  @Override
  public Value setCharValueAt(long index, String value)
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
  // CharSequence
  //

  /**
   * Returns the length of the string.
   */
  @Override
  public int length()
  {
    return _length;
  }

  void setLength(int length)
  {
    _length = length;
  }

  /**
   * Returns the character at a particular location
   */
  @Override
  public final char charAt(int index)
  {
    return _buffer[index];
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return StringValue.EMPTY;
    
    char []newBuffer = new char[end - start];
    
    System.arraycopy(_buffer, start, newBuffer, 0, end - start);
		     
    return new StringBuilderValue(newBuffer, 0, end - start);
  }

  //
  // java.lang.String
  //
  
  /**
   * Returns the last index of the match string, starting from the head.
   */
  @Override
  public int indexOf(char match)
  {
    int length = _length;
    char []buffer = _buffer;
    
    for (int head = 0; head < length; head++) {
      if (buffer[head] == match)
	return head;
    }

    return -1;
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  @Override
  public int indexOf(char match, int head)
  {
    int length = _length;
    char []buffer = _buffer;
    
    for (; head < length; head++) {
      if (buffer[head] == match)
	return head;
    }

    return -1;
  }
    
  /**
   * Returns the first index of the match string, starting from the head.
   */
  @Override
  public int indexOf(CharSequence match, int head)
  {
    int length = _length;
    int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    else if (head < 0)
      return -1;
    
    int end = length - matchLength;
    char first = match.charAt(0);

    char []buffer = _buffer;
    
    loop:
    for (; head <= end; head++) {
      if (buffer[head] != first)
	continue;

      for (int i = 1; i < matchLength; i++) {
	if (buffer[head + i] != match.charAt(i))
	  continue loop;
      }

      return head;
    }

    return -1;
  }

  /**
   * Returns a character array
   */
  @Override
  public char []toCharArray()
  {
    char[] dest = new char[_length];
    
    System.arraycopy(_buffer, 0, dest, 0, _length);
    
    return dest;
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

  //
  // append code
  //

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s)
  {
    int len = s.length();

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(0, len, _buffer, _length);

    _length += len;
    _value = null;

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s, int start, int end)
  {
    int len = end - start;

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(start, end, _buffer, _length);

    _length += len;
    _value = null;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(buf, offset, _buffer, _length, length);

    _length += length;
    _value = null;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(CharSequence buf, int head, int tail)
  {
    _value = null;
    
    int length = tail - head;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    if (buf instanceof StringBuilderValue) {
      StringBuilderValue sb = (StringBuilderValue) buf;
      
      System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

      _length += tail - head;
    } else {
      for (; head < tail; head++)
	_buffer[_length++] = buf.charAt(head);
    }

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(StringBuilderValue sb, int head, int tail)
  {
    _value = null;
    
    int length = tail - head;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

    _length += tail - head;

    return this;
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue append(char v)
  {
    _value = null;
    
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = v;

    return this;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public final StringValue append(Value v)
  {
    _value = null;
    
    v.appendTo(this);

    return this;
  }

  //
  // java.lang.Object methods
  //

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    int hash = 37;

    int length = _length;
    char []buffer = _buffer;

    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + buffer[i];
    }

    return hash;
  }

  /**
   * Test for equality
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o instanceof StringBuilderValue) {
      StringBuilderValue s = (StringBuilderValue) o;

      int aLength = _length;
      int bLength = s._length;

      if (aLength != bLength)
	return false;

      char []aBuffer = _buffer;
      char []bBuffer = s._buffer;

      for (int i = aLength - 1; i >= 0; i--) {
	if (aBuffer[i] != bBuffer[i])
	  return false;
      }

      return true;
    }
    else if (o instanceof StringValue) {
      StringValue s = (StringValue) o;

      int aLength = length();
      int bLength = s.length();

      if (aLength != bLength)
	return false;

      for (int i = aLength - 1; i >= 0; i--) {
	if (charAt(i) != s.charAt(i))
	  return false;
      }

      return true;
    } else {
      return false;
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
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("new InternStringValue(\"");
    printJavaString(out, toString());
    out.print("\")");
  }

  private void ensureCapacity(int capacity)
  {
    if (capacity <= _buffer.length)
      return;

    int newCapacity;

    if (capacity < 4096) {
      newCapacity = _buffer.length;

      if (newCapacity < 64)
	newCapacity = 64;

      while (newCapacity < capacity)
	newCapacity = 4 * newCapacity;
    }
    else
      newCapacity = (capacity + 4096) & ~4095;

    char []buffer = new char[newCapacity];
    System.arraycopy(_buffer, 0, buffer, 0, _length);

    _buffer = buffer;
  }
}

