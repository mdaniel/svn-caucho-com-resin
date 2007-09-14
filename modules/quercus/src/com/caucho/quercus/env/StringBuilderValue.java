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

import java.io.*;
import java.util.*;

import com.caucho.vfs.*;

/**
 * Represents a PHP 5 style string builder (unicode.semantics = off)
 */
public class StringBuilderValue
  extends StringValue
{
  public static final StringBuilderValue EMPTY = new StringBuilderValue("");
  
  protected char []_buffer;
  protected int _length;

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

  public StringBuilderValue(char []buffer, int offset, int length)
  {
    _buffer = new char[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public StringBuilderValue(byte []buffer, int offset, int length)
  {
    _buffer = new char[length];
    _length = length;

    for (int i = 0; i < length; i++)
      _buffer[i] = (char) buffer[i];
  }

  public StringBuilderValue(byte []buffer)
  {
    this(buffer, 0, buffer.length);
  }
  
  public StringBuilderValue(Byte []buffer)
  {
    int length = buffer.length;
    
    _buffer =  new char[length];
    _length = length;
    
    for (int i = 0; i < length; i++) {
      _buffer[i] = (char) buffer[i].byteValue();
    }
  }

  public StringBuilderValue(String s)
  {
    int len = s.length();
    
    _buffer = new char[len];
    _length = len;

    for (int i = 0; i < len; i++)
      _buffer[i] = s.charAt(i);
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
  @Override
  public String getType()
  {
    return "string";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return UnicodeBuilderValue.getValueType(_buffer, 0, _length);
  }

  /**
   * Returns true for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    char []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return true;

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
  @Override
  public boolean isNumber()
  {
    return getNumericType() != IS_STRING;
  }

  /**
   * Returns true for a scalar
   */
  @Override
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Converts to a double.
   */
  @Override
  protected int getNumericType()
  {
    return UnicodeBuilderValue.getNumericType(_buffer, 0, _length);
  }

  /**
   * Converts to a boolean.
   */
  @Override
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
  @Override
  public long toLong()
  {
    return UnicodeBuilderValue.toLong(_buffer, 0, _length);
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return UnicodeBuilderValue.toDouble(_buffer, 0, _length);
  }

  /**
   * Convert to an input stream.
   */
  @Override
  public InputStream toInputStream()
  {
    return new BuilderInputStream();
  }

  /**
   * Converts to a string.
   */
  @Override
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
  @Override
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
    // XXX: can this just return this, or does it need to return a copy?
    return new StringBuilderValue(_buffer, 0, _length);
  }
  
  /**
   * Append to a string builder.
   */
  public void appendTo(StringValue bb)
  {
    bb.append(_buffer, 0, _length);
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    char []buffer = _buffer;
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
      return UnsetUnicodeValue.UNSET;
    else
      return StringBuilderValue.create((char) (_buffer[(int) index] & 0xff));
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
      return StringBuilderValue.EMPTY;

    return new StringBuilderValue(_buffer, start, end - start);
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;
    
    StringBuilderValue string = new StringBuilderValue(length);
    
    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];
      
      if ('A' <= ch && ch <= 'Z')
	dstBuffer[i] = (char) (ch + 'a' - 'A');
      else
	dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }
  
  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toUpperCase()
  {
    int length = _length;
    
    StringBuilderValue string = new StringBuilderValue(_length);

    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];
      
      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (char) (ch + 'A' - 'a');
      else
	dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder()
  {
    return new StringBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new StringBuilderValue(length);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new StringBuilderValue(_buffer, 0, _length);
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
    int sublen = s.length();

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    for (int i = 0; i < sublen; i++) {
      _buffer[_length++] = s.charAt(i);
    }

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s, int start, int end)
  {
    int sublen = end - start;

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    char []buffer = _buffer;
    int length = _length;

    for (; start < end; start++)
      buffer[length++] = s.charAt(start);

    _length = length;

    return this;
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue append(char ch)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = ch;
    
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

    char []buffer = _buffer;
    int bufferLength = _length;

    for (; length > 0; length--)
      buffer[bufferLength++] = buf[offset--];

    _buffer = buffer;
    _length = bufferLength;

    return this;
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

    if (buf instanceof StringBuilderValue) {
      StringBuilderValue sb = (StringBuilderValue) buf;
      
      System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

      _length += tail - head;

      return this;
    }
    else {
      char []buffer = _buffer;
      int bufferLength = _length;
      
      for (; head < tail; head++) {
	buffer[bufferLength++] = buf.charAt(head);
      }

      _length = bufferLength;

      return this;
    }
  }

  /**
   * Append a Java buffer to the value.
   */
  // @Override
  public final StringValue append(StringBuilderValue sb, int head, int tail)
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
    /*
    if (v.length() == 0)
      return this;
    else {
      // php/033a
      v.appendTo(this);

      return this;
    }
    */
    
    v.appendTo(this);

    return this;
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
   * Append a Java byte to the value without conversions.
   */
  @Override
  public final StringValue appendByte(int v)
  {
    int length = _length + 1;

    if (_buffer.length < length)
      ensureCapacity(length);

    _buffer[_length++] = (char) v;

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue append(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue append(long v)
  {
    // XXX: this probably is frequent enough to special-case
    
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue append(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a bytes to the value.
   */
  @Override
  public StringValue appendBytes(String s)
  {
    int sublen = s.length();

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    for (int i = 0; i < sublen; i++) {
      _buffer[_length++] = s.charAt(i);
    }

    return this;
  }

  /**
   * Returns the buffer.
   */
  public char []getBuffer()
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

  //
  // Java generator code
  //

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
    sb.append(toString());
    sb.append("\";");
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("binary(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length > 256 ? 256 : length;

    for (int i = 0; i < appendLength; i++)
      sb.append(charAt(i));

    if (length > 256)
      sb.append(" ...");

    sb.append('"');

    return sb.toString();
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int length = length();

    if (length < 0)
        length = 0;
    
    out.print("string(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++)
      out.print(charAt(i));

    out.print("\"");
  }

  /**
   * Returns an OutputStream.
   */
  public OutputStream getOutputStream()
  {
    return new BuilderOutputStream();
  }

  public void ensureAppendCapacity(int newCapacity)
  {
    ensureCapacity(_length + newCapacity);
  }

  private void ensureCapacity(int newCapacity)
  {
    if (newCapacity <= _buffer.length)
      return;
    else if (newCapacity < 4096)
      newCapacity = 4 * newCapacity;
    else
      newCapacity = newCapacity + 4096;

    char []buffer = new char[newCapacity];
    System.arraycopy(_buffer, 0, buffer, 0, _length);

    _buffer = buffer;
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    int hash = 37;

    int length = _length;

    char []buffer = _buffer;
    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + (buffer[i] & 0xff);
    }

    return hash;
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    ValueType typeA = getValueType();
    ValueType typeB = rValue.getValueType();

    if (typeB.isNumber()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    else if (typeB.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }
    else if (typeA.isNumberConvertable() && typeB.isNumberConvertable()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }

    rValue = rValue.toValue();
    
    if (rValue instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) rValue;

      int length = _length;
      
      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else {
      return toString().equals(rValue.toString());
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) o;

      int length = _length;
      
      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    /*
    else if (o instanceof UnicodeValue) {
      UnicodeValue value = (UnicodeValue)o;
      
      return value.equals(this);
    }
    */
    else
      return false;
  }

  @Override
  public boolean eql(Value o)
  {
    o = o.toValue();
    
    if (o instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) o;

      int length = _length;
      
      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
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
  @Override
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("new StringBuilderValue(\"");
    printJavaString(out, this);
    out.print("\")");
  }
  
  //
  // Java serialization code
  //
  
  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeInt(_length);

    for (int i = 0; i < _length; i++)
      out.write(_buffer[i]);
  }
  
  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _length = in.readInt();
    _buffer = new char[_length];

    for (int i = 0; i < _length; i++)
      _buffer[i] = (char) in.read();
  }

  class BinaryInputStream extends InputStream {
    private int _offset;

    /**
     * Reads the next byte.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = _length - _index;

      if (length < sublen)
	sublen = length;

      if (sublen <= 0)
	return -1;

      for (int i = 0; i < sublen; i++)
	buffer[offset + i] = (byte) _buffer[_index + i];

      _index += sublen;

      return sublen;
    }
  }

  class BuilderOutputStream extends OutputStream {
    /**
     * Writes the next byte.
     */
    @Override
    public void write(int ch)
    {
      append(ch);
    }

    /**
     * Reads into a buffer.
     */
    @Override
    public void write(byte []buffer, int offset, int length)
    {
      append(buffer, offset, length);
    }
  }
}

