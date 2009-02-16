/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.util.*;

/**
 * Represents a 8-bit PHP 6 style binary builder (unicode.semantics = on)
 */
public class BinaryBuilderValue
  extends BinaryValue
{
  public static final BinaryBuilderValue EMPTY = new BinaryBuilderValue("");

  private final static BinaryBuilderValue []CHAR_STRINGS;
  
  protected byte []_buffer;
  protected int _length;

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

  public BinaryBuilderValue(String s)
  {
    int len = s.length();
    
    _buffer = new byte[len];
    _length = len;

    for (int i = 0; i < len; i++)
      _buffer[i] = (byte) s.charAt(i);
  }
  
  public BinaryBuilderValue(char []buffer)
  {
    _buffer = new byte[buffer.length];
    _length = buffer.length;

    for (int i = 0; i < buffer.length; i++)
      _buffer[i] = (byte) buffer[i];
  }
  
  public BinaryBuilderValue(char []s, Value v1)
  {
    int len = s.length;

    if (len < 128)
      _buffer = new byte[128];
    else
      _buffer = new byte[len + 32];
    
    _length = len;

    for (int i = 0; i < len; i++) {
      _buffer[i] = (byte) s[i];
    }

    v1.appendTo(this);
  }
  
  public BinaryBuilderValue(Byte []buffer)
  {
    int length = buffer.length;
    
    _buffer =  new byte[length];
    _length = length;
    
    for (int i = 0; i < length; i++) {
      _buffer[i] = buffer[i].byteValue();
    }
  }

  public BinaryBuilderValue(byte ch)
  {
    _buffer = new byte[1];
    _length = 1;

    _buffer[0] = ch;
  }

  /**
   * Creates the string.
   */
  public static StringValue create(int value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new BinaryBuilderValue(value);
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
    return getValueType(_buffer, 0, _length);
  }

  /**
   * Returns true for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    byte []buffer = _buffer;
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
    return false;
  }

  /**
   * Returns true for a number
   */
  @Override
  public boolean isNumber()
  {
    return false;
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
    return parseLong(_buffer, 0, _length);
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDouble(_buffer, 0, _length);
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
    return new BinaryBuilderValue(_buffer, 0, _length);
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a BinaryValue in desired charset.
   */
  @Override
  public StringValue toBinaryValue(Env env, String charset)
  {
    return this;
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

    return LongValue.create(sign * value);
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
  @Override
  public Value charValueAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return UnsetBinaryValue.UNSET;
    else
      return BinaryBuilderValue.create(_buffer[(int) index] & 0xff);
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
      BinaryBuilderValue sb = new BinaryBuilderValue(_buffer, 0, (int) index);
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
      return EMPTY;

    return new BinaryBuilderValue(_buffer, start, end - start);
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;
    
    BinaryBuilderValue string = new BinaryBuilderValue(length);
    
    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];
      
      if ('A' <= ch && ch <= 'Z')
	dstBuffer[i] = (byte) (ch + 'a' - 'A');
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
    
    BinaryBuilderValue string = new BinaryBuilderValue(_length);

    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];
      
      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (byte) (ch + 'A' - 'a');
      else
	dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  //
  // append code
  //

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder()
  {
    return new BinaryBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new BinaryBuilderValue(length);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new BinaryBuilderValue(_buffer, 0, _length);
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf, int offset, int length)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

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
      byte []buffer = _buffer;
      int bufferLength = _length;
      
      for (; head < tail; head++) {
	buffer[bufferLength++] = (byte) buf.charAt(head);
      }

      _length = bufferLength;

      return this;
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
    /*
    if (v.length() == 0)
      return this;
    else {
      // php/033a
      v.appendTo(this);

      return this;
    }
    */

    return v.appendTo(this);
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
  public final StringValue append(char v)
  {
    int length = _length + 1;

    if (_buffer.length < length)
      ensureCapacity(length);

    _buffer[_length++] = (byte) v;

    return this;
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  public final StringValue append(byte v)
  {
    int length = _length + 1;

    if (_buffer.length < length)
      ensureCapacity(length);

    _buffer[_length++] = v;

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
  public StringValue append(String s)
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
   * Append a bytes to the value.
   */
  @Override
  public StringValue append(String s, int start, int end)
  {
    int sublen = end - start;

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    for (int i = start; i < end; i++) {
      _buffer[_length++] = (byte) s.charAt(i);
    }

    return this;
  }
  
  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    int end = offset + length;
    
    for (int i = offset; i < end; i++) {
      _buffer[_length++] = (byte) buf[i];
    }

    return this;
  }
  
  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s);

    return sb;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s, int start, int end)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s, start, end);

    return sb;
  }

  /**
   * Append a value to the value.
   */
  @Override
  public final StringValue appendUnicode(Value value)
  {
    value = value.toValue();

    if (value instanceof BinaryBuilderValue) {
      append((BinaryBuilderValue) value);

      return this;
    }
    else if (value.isString()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue();

      appendTo(sb);
      sb.append(value);

      return sb;
    }
    else
      return value.appendTo(this);
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue appendUnicode(char ch)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(ch);

    return sb;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue appendUnicode(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue appendUnicode(long v)
  {
    // XXX: this probably is frequent enough to special-case
    
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue appendUnicode(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java object to the value.
   */
  @Override
  public StringValue appendUnicode(Object v)
  {
    if (v instanceof String)
      return appendUnicode(v.toString());
    else
      return append(v.toString());
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

    _buffer[_length++] = (byte) v;

    return this;
  }
  
  /**
   * Append Java bytes to the value without conversions.
   */
  @Override
  public final StringValue appendBytes(byte []bytes, int offset, int end)
  {
    int length = _length + end - offset;

    if (_buffer.length < length)
      ensureCapacity(length);

    for (int i = offset; i < end; i++) {
      _buffer[_length++] = (byte) bytes[i];
    }

    return this;
  }
  
  /**
   * Append to a string builder.
   */
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    if (length() == 0)
      return sb;
    
    Env env = Env.getInstance();

    try {
      Reader reader = env.getRuntimeEncodingFactory().create(toInputStream());
      
      if (reader != null) {
        sb.append(reader);

        reader.close();
      }

      return sb;
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
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
   * Prints the value.
   * @param env
   */
  public void print(Env env, WriteStream out)
  {
    try {
      out.write(_buffer, 0, _length);
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("s:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(toString());
    sb.append("\";");
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
  @Override
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
    else if (typeA.isNumberCmp() && typeB.isNumberCmp()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }

    rValue = rValue.toValue();
    
    if (rValue instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) rValue;

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
    else {
      return toString().equals(rValue.toString());
    }
  }

  @Override
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
    else
      return false;
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

    // QA needs to distinguish php5 string from php6 binary
    if (Alarm.isTest())
      out.print("binary");
    else
      out.print("string");
    
    out.print("(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++) {
      char ch = charAt(i);

      if (0x20 <= ch && ch <= 0x7f || ch == '\t' || ch == '\r' || ch == '\n')
	out.print(ch);
      else if (ch <= 0xff)
	out.print("\\x" + Integer.toHexString(ch / 16) + Integer.toHexString(ch % 16));
      else {
	out.print("\\u"
		  + Integer.toHexString((ch >> 12) & 0xf)
		  + Integer.toHexString((ch >> 8) & 0xf)
		  + Integer.toHexString((ch >> 4) & 0xf)
		  + Integer.toHexString((ch) & 0xf));
      }
    }

    out.print("\"");
  }

  //
  // Java serialization code
  //
  
  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeInt(_length);
    out.write(_buffer, 0, _length);
  }
  
  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _length = in.readInt();
    _buffer = new byte[_length];
    
    in.read(_buffer, 0, _length);
  }

  //
  // static helper functions
  //

  //
  // static helper functions
  //

  public static ValueType getValueType(byte []buffer, int offset, int len)
  {
    if (len == 0)
      return ValueType.LONG_ADD;

    int i = offset;
    int ch = 0;
    boolean hasPoint = false;

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return ValueType.STRING;

    ch = buffer[i];

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
        return ValueType.DOUBLE_CMP;
      }

      return ValueType.STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return ValueType.STRING;

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    if (len <= i)
      return ValueType.LONG_EQ;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = buffer[i]) && ch <= '9' ||
                       ch == '+' || ch == '-' || ch == 'e' || ch == 'E');
           i++) {
      }

      if (i < len)
        return ValueType.STRING;
      else
        return ValueType.DOUBLE_CMP;
    }
    else
      return ValueType.STRING;
  }

  public static int getNumericType(byte []buffer, int offset, int len)
  {
    if (len == 0)
      return IS_STRING;

    int i = offset;
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

  public static double toDouble(byte []buffer, int offset, int len)
  {
    int i = offset;
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
      return Double.parseDouble(new String(buffer, 0, i));
    } catch (NumberFormatException e) {
      return 0;
    }
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

      System.arraycopy(_buffer, _index, buffer, offset, sublen);

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

  static {
    CHAR_STRINGS = new BinaryBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new BinaryBuilderValue((byte) i);
  }
}

