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

import com.caucho.vfs.*;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.QuercusModuleException;

import java.io.*;
import java.util.IdentityHashMap;

/**
 * Represents a PHP 5 style string builder (unicode.semantics = off)
 */
public class StringBuilderValue
  extends StringValue
{
  public static final StringBuilderValue EMPTY = new StringBuilderValue("");
  
  private static final int MIN_LENGTH = 32;

  private static final StringBuilderValue []CHAR_STRINGS;
  
  protected char []_buffer;
  protected int _length;
  protected boolean _isCopy;
 
  private int _hashCode;

  protected String _value;

  public StringBuilderValue()
  {
    _buffer = new char[MIN_LENGTH];
  }

  public StringBuilderValue(int capacity)
  {
    if (capacity < MIN_LENGTH)
      capacity = MIN_LENGTH;

    _buffer = new char[capacity];
  }

  public StringBuilderValue(char []buffer, int offset, int length)
  {
    _buffer = new char[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  /**
   * Creates a new StringBuilderValue with the buffer without copying.
   */
  public StringBuilderValue(char []buffer, int length)
  {
    _buffer = buffer;
    _length = length;
  }

  public StringBuilderValue(byte []buffer, int offset, int length)
  {
    _buffer = new char[length];
    _length = length;

    for (int i = offset; i < length; i++)
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
  
  public StringBuilderValue(Character []buffer)
  {
    int length = buffer.length;
    
    _buffer =  new char[length];
    _length = length;
    
    for (int i = 0; i < length; i++) {
      _buffer[i] = buffer[i];
    }
  }

  public StringBuilderValue(char ch)
  {
    _buffer = new char[1];
    _length = 1;

    _buffer[0] = ch;
  }

  public StringBuilderValue(String s)
  {
    int len = s.length();
    
    _buffer = new char[len];
    _length = len;

    s.getChars(0, len, _buffer, 0);
  }

  public StringBuilderValue(char []s)
  {
    int len = s.length;
    
    _buffer = new char[len];
    _length = len;

    System.arraycopy(s, 0, _buffer, 0, len);
  }

  public StringBuilderValue(char []s, Value v1)
  {
    int len = s.length;

    int bufferLength = MIN_LENGTH;
    while (bufferLength < len)
      bufferLength *= 2;
    
    _buffer = new char[bufferLength];
    
    _length = len;

    System.arraycopy(s, 0, _buffer, 0, len);

    v1.appendTo(this);
  }

  public StringBuilderValue(Value v1)
  {
    _buffer = new char[MIN_LENGTH];

    v1.appendTo(this);
  }
  
  public StringBuilderValue(StringBuilderValue v)
  {
    if (v._isCopy) {
      _buffer = new char[v._buffer.length];
      System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
      _length = v._length;
    }
    else {
      _buffer = v._buffer;
      _length = v._length;
      v._isCopy = true;
    }
  }
  
  public StringBuilderValue(StringBuilderValue v, boolean isCopy)
  {
    _buffer = new char[v._buffer.length];
    System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
    _length = v._length;
  }

  public StringBuilderValue(Value v1, Value v2)
  {
    _buffer = new char[MIN_LENGTH];

    v1.appendTo(this);
    v2.appendTo(this);
  }

  public StringBuilderValue(Value v1, Value v2, Value v3)
  {
    _buffer = new char[MIN_LENGTH];

    v1.appendTo(this);
    v2.appendTo(this);
    v3.appendTo(this);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new StringBuilderValue(value);
  }

  /**
   * Creates a PHP string from a Java String.
   * If the value is null then NullValue is returned.
   */
  public static Value create(String value)
  {
    if (value == null)
      return NullValue.NULL;
    else if (value.length() == 0)
      return StringBuilderValue.EMPTY;
    else
      return new StringBuilderValue(value);
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
   * Returns true for a scalar
   */
  @Override
  public boolean isScalar()
  {
    return true;
  }
  
  /*
   * Returns true if this is a PHP5 string.
   */
  @Override
  public boolean isPHP5String()
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
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue()
  {
    return this;
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return toUnicodeValue();
  }

  /**
   * Converts to a UnicodeValue in desired charset.
   */
  @Override
  public StringValue toUnicodeValue(Env env, String charset)
  {
    return toUnicodeValue();
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
   * Returns true if the value is empty.
   */
  @Override
  public final boolean isEmpty()
  {
    return _length == 0 || _length == 1 && _buffer[0] == '0';
  }

  /**
   * Writes to a stream
   */
  @Override
  public void writeTo(OutputStream os)
  {
    try {
      int len = _length;

      for (int i = 0; i < len; i++)
        os.write(_buffer[i]);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);

    return bb;
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);
    
    return bb;
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);
    
    return bb;
  }

  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);
    
    return bb;
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

    for (int i = _length - 1; i >= 0; i--)
      bytes[i] = (byte) _buffer[i];

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
   * Sets the array ref.
   */
  @Override
  public Value put(Value index, Value value)
  {
    setCharValueAt(index.toLong(), value.toString());

    return value;
  }
  
  /**
   * Sets the array ref.
   */
  @Override
  public Value append(Value index, Value value)
  {
    if (_length > 0)
      return setCharValueAt(index.toLong(), value.toString());
    else
      return new ArrayValueImpl().append(index, value);
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
  public final int length()
  {
    return _length;
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
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return UnsetStringValue.UNSET;
    else {
      int ch = _buffer[(int) index];

      if (ch < CHAR_STRINGS.length)
        return CHAR_STRINGS[ch];
      else
        return new StringBuilderValue((char) ch);
    }
  }
  
  /**
   * sets the character at an index
   */
  @Override
  public Value setCharValueAt(long indexL, String value)
  {
    int len = _length;

    if (indexL < 0)
      return this;
    else {
      // php/03mg, #2940
      
      int index = (int) indexL;

      StringBuilderValue sb = (StringBuilderValue) copyStringBuilder();

      sb.ensureCapacity(index + 1);
      
      int padLen = index - len;

      if (padLen > 0) {
        for (int i = 0; i <= padLen; i++) {
          sb.append(' ');
        }
      }
      
      if (value.length() == 0)
        sb._buffer[index] = 0;
      else
        sb._buffer[index] = value.charAt(0);

      return sb;
    }
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
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
   * Returns a subsequence
   */
  @Override
  public String stringSubstring(int start, int end)
  {
    if (end <= start)
      return "";

    return new String(_buffer, start, end - start);
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
   * Returns true if the region matches
   */
  public boolean regionMatches(int offset,
			       char []mBuffer, int mOffset, int mLength)
  {
    int length = _length;

    if (length < offset + mLength)
      return false;

    char []buffer = _buffer;

    for (int i = 0; i < mLength; i++) {
      if (buffer[offset + i] != mBuffer[mOffset + i])
	return false;
    }

    return true;
  }
    
  /**
   * Returns true if the region matches
   */
  public boolean
    regionMatchesIgnoreCase(int offset,
			    char []mBuffer, int mOffset, int mLength)
  {
    int length = _length;

    if (length < offset + mLength)
      return false;

    char []buffer = _buffer;

    for (int i = 0; i < mLength; i++) {
      char a = buffer[offset + i];
      char b = mBuffer[mOffset + i];

      if ('A' <= a && a <= 'Z')
	a += 'a' - 'A';

      if ('A' <= b && b <= 'Z')
	b += 'a' - 'A';
      
      if (a != b)
	return false;
    }

    return true;
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
  public StringValue toStringBuilder()
  {
    return new StringBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  public StringValue copyStringBuilder()
  {
    return new StringBuilderValue(this, true);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new StringBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    StringBuilderValue v = new StringBuilderValue(this);

    value.appendTo(v);
    
    return v;
  }
  
  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value)
  {
    StringBuilderValue v = new StringBuilderValue(this);

    value.appendTo(v);
    
    return v;
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
    int end = _length + length;
    
    if (_buffer.length < end)
      ensureCapacity(end);

    char []buffer = _buffer;
    int bufferLength = _length;
    
    while (--length >= 0)
      buffer[bufferLength + length] = buf[offset + length];
    
    _length = end;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    char []buffer = _buffer;
    int bufferLength = _length;

    for (; length > 0; length--)
      buffer[bufferLength++] = buf[offset++];

    _buffer = buffer;
    _length = bufferLength;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf)
  {
    int length = buf.length;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    char []buffer = _buffer;
    int bufferLength = _length;
    _length = bufferLength + length;

    for (length--; length >= 0; length--)
      buffer[bufferLength + length] = buf[length];

    _buffer = buffer;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf)
  {
    int length = buf.length;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    char []buffer = _buffer;
    int bufferLength = _length;
    _length = bufferLength + length;

    for (length--; length >= 0; length--)
      buffer[bufferLength + length] = buf[length];

    _buffer = buffer;

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

    loop:
    for (; head <= end; head++) {
      if (_buffer[head] != first)
	continue;

      for (int i = 1; i < matchLength; i++) {
	if (_buffer[head + i] != match.charAt(i))
	  continue loop;
      }

      return head;
    }

    return -1;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public StringValue appendUnicode(Value v)
  {
    v.appendTo(this);

    return this;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public StringValue appendUnicode(Value v1, Value v2)
  {
    v1.appendTo(this);
    v2.appendTo(this);

    return this;
  }

  /**
   * Append a buffer to the value.
   */
  public StringValue append(byte []buf, int offset, int length)
  {
    int end = _length + length;
    
    if (_buffer.length < end)
      ensureCapacity(end);

    char []charBuffer = _buffer;
    int charLength = _length;

    for (int i = length - 1; i >= 0; i--) {
      charBuffer[charLength + i] = (char) (buf[offset + i] & 0xff);
    }

    _length = end;

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
   * Append a buffer to the value.
   */
  @Override
  public StringValue appendUtf8(byte []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    char []charBuffer = _buffer;
    int charLength = _length;

    int end = offset + length;
    
    while (offset < end) {
      int ch = buf[offset++] & 0xff;

      if (ch < 0x80)
	charBuffer[charLength++] = (char) ch;
      else if (ch < 0xe0) {
	int ch2 = buf[offset++] & 0xff;
	
	int v = (char) (((ch & 0x1f) << 6) + (ch2 & 0x3f));
	
	charBuffer[charLength++] = (char) (v & 0xff);
      }
      else {
	int ch2 = buf[offset++] & 0xff;
	int ch3 = buf[offset++] & 0xff;
	
	int v = (char) (((ch & 0xf) << 12)
			+ ((ch2 & 0x3f) << 6)
			+ ((ch3) << 6));
	
	charBuffer[charLength++] = (char) (v & 0xff);
      }
    }

    _length = charLength;

    return this;
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
   * Append Java bytes to the value without conversions.
   */
  @Override
  public final StringValue appendBytes(byte []bytes, int offset, int end)
  {
    int length = _length + end - offset;

    if (_buffer.length < length)
      ensureCapacity(length);

    for (int i = offset; i < end; i++) {
      _buffer[_length++] = (char) bytes[i];
    }

    return this;
  }

  @Override
  public StringValue append(Reader reader, long length)
    throws IOException
  {
    // php/4407 - oracle clob callback passes very long length

    int sublen = (int) Math.min(8192L, length);

    try {
      while (length > 0) {
        ensureAppendCapacity(sublen);

        int count = reader.read(_buffer, _length, sublen);

        if (count <= 0)
          break;

        length -= count;
        _length += count;
      }

    } catch (IOException e) {
      throw new QuercusModuleException(e);
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
   * Prints the value.
   * @param env
   */
  public void print(Env env, WriteStream out)
  {
    try {
      out.print(_buffer, 0, _length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
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

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("binary(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length < 256 ? length : 256;

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

    for (int i = 0; i < length; i++) {
      int ch = charAt(i);

      out.print((char) ch);
    }

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

  protected void ensureCapacity(int newCapacity)
  {
    int bufferLength = _buffer.length;
    
    if (newCapacity <= bufferLength)
      return;

    if (bufferLength < MIN_LENGTH)
      bufferLength = MIN_LENGTH;

    while (bufferLength <= newCapacity)
      bufferLength = 2 * bufferLength;

    char []buffer = new char[bufferLength];
    System.arraycopy(_buffer, 0, buffer, 0, _length);
    _buffer = buffer;
    _isCopy = false;
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    int hash = _hashCode;

    if (hash != 0)
      return hash;
    
    hash = 37;

    int length = _length;
    char []buffer = _buffer;

    if (length > 256) {
      for (int i = 127; i >= 0; i--) {
	hash = 65521 * hash + buffer[i];
      }

      for (int i = length - 128; i < length; i++) {
	hash = 65521 * hash + buffer[i];
      }

      _hashCode = hash;

      return hash;
    }

    for (int i = length - 1; i >= 0; i--) {
      hash = 65521 * hash + buffer[i];
    }

    _hashCode = hash;

    return hash;
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();
    
    ValueType typeB = rValue.getValueType();

    if (typeB.isNumber()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    else if (typeB.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }
      
    ValueType typeA = getValueType();
    if (typeA.isNumberCmp() && typeB.isNumberCmp()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    
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
      String rString = rValue.toString();
      
      int len = rString.length();

      if (_length != len)
	return false;

      for (int i = len - 1; i >= 0; i--) {
	if (_buffer[i] != rString.charAt(i))
	  return false;
      }
      
      return true;
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

  static {
    CHAR_STRINGS = new StringBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new StringBuilderValue((char) i);
  }
}

