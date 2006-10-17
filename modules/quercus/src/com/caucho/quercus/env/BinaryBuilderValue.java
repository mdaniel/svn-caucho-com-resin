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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a 8-bit binary builder
 */
public class BinaryBuilderValue extends BinaryValue {
  private byte []_buffer;
  private int _length;

  private String _value;

  public BinaryBuilderValue()
  {
    _buffer = new byte[128];
  }

  public BinaryBuilderValue(int capacity)
  {
    if (capacity < 64)
      capacity = 128;
    else
      capacity = 2 * capacity;

    _buffer = new byte[capacity];
  }

  public BinaryBuilderValue(byte []buffer, int offset, int length)
  {
    _buffer = new byte[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public BinaryBuilderValue(byte []buffer)
  {
    this(buffer, 0, buffer.length);
  }

  public BinaryBuilderValue(String value)
  {
    this(value.getBytes());
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
   * Returns true for a long
   */
  public boolean isLongConvertible()
  {
    byte []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return false;

    for (int i = 0; i < len; i++) {
      int ch = _buffer[i];

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
    byte []buffer = _buffer;
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
   * Converts to a double.
   */
  public double toDouble()
  {
    byte []buffer = _buffer;
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
   * Convert to an input stream.
   */
  public InputStream toInputStream()
  {
    return new BuilderInputStream();
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    // XXX: encoding
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
  public void appendTo(BinaryBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);
  }

  /**
   * Append to a string builder.
   */
  public void appendTo(StringBuilderValue sb)
  {
    toUnicodeValue(Env.getInstance()).appendTo(sb);
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    byte []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    int ch = buffer[i];
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
    byte []buffer = _buffer;

    int len = _length;
    byte[] bytes = new byte[_length];
    System.arraycopy(_buffer, 0, bytes, 0, _length);

    return bytes;
  }

  //
  // Operations
  //

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
      return StringValue.create((char) (_buffer[(int) index] & 0xff));
  }

  /**
   * sets the character at an index
   */
  /*
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
  */

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

  /**
   * Returns the character at a particular location
   */
  @Override
  public char charAt(int index)
  {
    return (char) (_buffer[index] & 0xff);
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return StringValue.EMPTY;
    
    byte []newBuffer = new byte[end - start];
    
    System.arraycopy(_buffer, start, newBuffer, 0, end - start);
		     
    return new BinaryBuilderValue(newBuffer, 0, end - start);
  }

  //
  // Java generator code
  //

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.write(_buffer, 0, _length);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("s:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(toString());
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
    StringBuilderValue sb = new StringBuilderValue();

    appendTo(sb);
    sb.append(s);

    return sb;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s, int start, int end)
  {
    StringBuilderValue sb = new StringBuilderValue();

    appendTo(sb);
    sb.append(s, start, end);

    return sb;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf, int offset, int length)
  {
    StringBuilderValue sb = new StringBuilderValue();

    appendTo(sb);
    sb.append(buf, offset, length);

    return sb;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(CharSequence buf, int head, int tail)
  {
    int length = tail - head;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    if (buf instanceof BinaryBuilderValue) {
      BinaryBuilderValue sb = (BinaryBuilderValue) buf;
      
      System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

      _length += tail - head;

      return this;
    }
    else {
      StringBuilderValue sb = new StringBuilderValue();

      appendTo(sb);
      sb.append(buf, head, tail);

      return sb;
    }
  }

  /**
   * Append a Java buffer to the value.
   */
  // @Override
  public final StringValue append(BinaryBuilderValue sb, int head, int tail)
  {
    int length = tail - head;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

    _length += tail - head;

    return this;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public final StringValue append(Value v)
  {
    if (v.isUnicode()) {
      StringBuilderValue sb = new StringBuilderValue();

      appendTo(sb);
      v.appendTo(sb);

      return sb;
    }
    else {
      v.appendTo(this);

      return this;
    }
  }

  /**
   * Append a buffer to the value.
   */
  public final StringValue append(byte []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(buf, offset, _buffer, _length, length);

    _length += length;

    return this;
  }

  /**
   * Append a double to the value.
   */
  public final StringValue append(byte []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a byte to the value.
   */
  public final StringValue append(byte v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = v;

    return this;
  }

  /**
   * Append a byte to the value.
   */
  public final StringValue appendByte(int v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = (byte) v;

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue append(boolean v)
  {
    return appendBytes(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue append(long v)
  {
    return appendBytes(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue append(double v)
  {
    return appendBytes(String.valueOf(v));
  }

  /**
   * Append a bytes to the value.
   */
  public StringValue appendBytes(String s)
  {
    int sublen = s.length();

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    for (int i = 0; i < sublen; i++) {
      _buffer[_length++] = (byte) s.charAt(i);
    }

    return this;
  }

  /**
   * Prepares for reading.
   */
  public void prepareReadBuffer()
  {
    ensureCapacity(_buffer.length + 1);
  }

  /**
   * Returns the buffer.
   */
  public byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    return _length;
  }

  /**
   * Sets the offset.
   */
  public void setOffset(int offset)
  {
    _length = offset;
  }

  /**
   * Returns the current capacity.
   */
  public int getLength()
  {
    return _buffer.length;
  }

  /**
   * Returns an OutputStream.
   */
  public OutputStream getOutputStream()
  {
    return new BuilderOutputStream();
  }

  private void ensureCapacity(int newCapacity)
  {
    if (newCapacity <= _buffer.length)
      return;
    else if (newCapacity < 4096)
      newCapacity = 4 * newCapacity;
    else
      newCapacity = newCapacity + 4096;

    byte []buffer = new byte[newCapacity];
    System.arraycopy(_buffer, 0, buffer, 0, _length);

    _buffer = buffer;
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    int hash = 37;

    int length = _length;

    byte []buffer = _buffer;
    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + (buffer[i] & 0xff);
    }

    return hash;
  }

  public boolean equals(Object o)
  {
    if (o instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) o;

      int length = _length;
      
      if (length != value._length)
	return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
	if (bufferA[i] != bufferB[i])
	  return false;
      }

      return true;
    }
    else if (o instanceof StringValue) {
      StringValue value = (StringValue) o;

      int length = _length;
      if (length != value.length())
	return false;

      byte []buffer = _buffer;

      for (int i = length - 1; i >= 0; i--) {
	if ((buffer[i] & 0xff) != value.charAt(i))
	  return false;
      }

      return true;
    }
    else
      return false;
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
    out.print("new BinaryBuilderValue(\"");
    out.printJavaString(toString());
    out.print("\")");
  }

  class BinaryInputStream extends InputStream {
    private int _offset;

    /**
     * Reads the next byte.
     */
    public int read()
    {
      if (_offset < _length)
	return _buffer[_offset++];
      else
	return -1;
    }

    /**
     * Reads into a buffer.
     */
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = _length - _offset;

      if (length < sublen)
	sublen = length;

      if (sublen <= 0)
	return -1;

      System.arraycopy(_buffer, _offset, buffer, offset, sublen);

      _offset += sublen;

      return sublen;
    }
  }

  class BuilderInputStream extends InputStream {
    private int _index;
    
    /**
     * Reads the next byte.
     */
    public int read()
    {
      if (_index < _length)
	return _buffer[_index++] & 0xff;
      else
	return -1;
    }

    /**
     * Reads into a buffer.
     */
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = _length - _index;

      if (length < sublen)
	sublen = length;

      if (sublen <= 0)
	return -1;

      System.arraycopy(_buffer, _index, buffer, offset, sublen);

      _index += sublen;

      return sublen;
    }
  }

  class BuilderOutputStream extends OutputStream {
    /**
     * Writes the next byte.
     */
    public void write(int ch)
    {
      append(ch);
    }

    /**
     * Reads into a buffer.
     */
    public void write(byte []buffer, int offset, int length)
    {
      append(buffer, offset, length);
    }
  }
}

