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

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.vfs.*;

import java.io.*;
import java.util.IdentityHashMap;

/**
 * Represents a PHP string value.
 */
public class UnicodeBuilderValue extends StringBuilderValue
{
  public static final UnicodeBuilderValue EMPTY = new UnicodeBuilderValue("");

  private final static UnicodeBuilderValue []CHAR_STRINGS;

  public UnicodeBuilderValue()
  {
  }

  public UnicodeBuilderValue(int capacity)
  {
    super(capacity);
  }

  public UnicodeBuilderValue(String value)
  {
    super(value);
  }

  public UnicodeBuilderValue(String value, int minLength)
  {
    super(value);
  }

  public UnicodeBuilderValue(char []buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  public UnicodeBuilderValue(char []buffer)
  {
    super(buffer, 0, buffer.length);
  }

  public UnicodeBuilderValue(char []buffer, int length)
  {
    super(buffer, 0, length);
  }

  public UnicodeBuilderValue(char []buffer, int offset, int length,
                            boolean isExact)
  {
    super(buffer, offset, length);
  }

  public UnicodeBuilderValue(Character []buffer)
  {
    super(buffer);
  }

  public UnicodeBuilderValue(char ch)
  {
    super(ch);
  }

  public UnicodeBuilderValue(char []s, Value v1)
  {
    super(s, v1);
  }

  public UnicodeBuilderValue(Value v1)
  {
    super(v1);
  }

  public UnicodeBuilderValue(StringBuilderValue v)
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

  public UnicodeBuilderValue(StringBuilderValue v, boolean isCopy)
  {
    _buffer = new char[v._buffer.length];
    System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
    _length = v._length;
  }
  
  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new UnicodeBuilderValue(value);
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
      return UnicodeBuilderValue.EMPTY;
    else
      return new UnicodeBuilderValue(value);
  }

  /*
   * Decodes the Unicode str from charset.
   * 
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  @Override
  public StringValue create(Env env, StringValue str, String charset)
  {
    return str;
  }
  
  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  public StringValue convertToUnicode(Env env, String charset)
  {
    return this;
  }
  
  /**
   * Returns true for UnicodeValue
   */
  @Override
  public boolean isUnicode()
  {
    return true;
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
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return getValueType(_buffer, 0, _length);
  }

  /**
   * Interns the string.
   */
  /*
  public StringValue intern(Quercus quercus)
  {
    return quercus.intern(toString());
  }
  */

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    return new UnicodeBuilderValue(this);
  }
  
  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    UnicodeBuilderValue v = new UnicodeBuilderValue(this);

    value.appendTo(v);
    
    return v;
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue copyStringBuilder()
  {
    return new UnicodeBuilderValue(this, true);
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
    return this;
  }

  /**
   * Converts to a UnicodeValue in desired charset.
   */
  @Override
  public StringValue toUnicodeValue(Env env, String charset)
  {
    return this;
  }

  /**
   * Append a buffer to the value.
   */
  /*
  public final StringValue append(byte []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    Env env = Env.getInstance();
    String charset = (env != null
		      ? env.getRuntimeEncoding().toString()
		      : null);

    // ...

    char []charBuffer = _buffer;
    int charLength = _length;

    for (int i = 0; i < length; i++)
      charBuffer[charLength + i] = (char) buf[offset + i];

    _length += length;

    return this;
  }
  */

  /*
   * Appends a Unicode string to the value.
   * 
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  @Override
  public StringValue append(Env env, StringValue unicodeStr, String charset)
  {
    return append(unicodeStr);
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    sb.append(_buffer, 0, _length);

    return sb;
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue()
  {
    return toBinaryValue(Env.getInstance());
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue(Env env)
  {
    return toBinaryValue(env, env.getRuntimeEncoding());
  }

  /**
   * Converts to a BinaryValue in desired charset.
   *
   * @param env
   * @param charset
   */
  public StringValue toBinaryValue(Env env, String charset)
  {
    try {
      BinaryBuilderValue result = new BinaryBuilderValue();
      BinaryBuilderStream stream = new BinaryBuilderStream(result);

      // XXX: can use EncodingWriter directly(?)
      WriteStream out = new WriteStream(stream);
      out.setEncoding(charset);

      out.print(_buffer, 0, _length);

      out.close();

      return result;
    } catch (IOException e) {
      throw new QuercusModuleException(e.getMessage());
    }
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
    else {
      int ch = _buffer[(int) index];

      if (ch < CHAR_STRINGS.length)
	return CHAR_STRINGS[ch];
      else
	return new UnicodeBuilderValue((char) ch);
    }
  }

  /**
   * sets the character at an index
   */
  /*
  @Override
  public Value setCharValueAt(long index, String value)
  {
    if (_isCopy)
      copyOnWrite();
    
    int len = _length;

    if (index < 0 || len <= index)
      return this;
    else {
      UnicodeBuilderValue sb = new UnicodeBuilderValue(_buffer, 0, (int) index);
      sb.append(value);
      sb.append(_buffer, (int) (index + 1), (int) (len - index - 1));

      return sb;
    }
  }
  /*

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return EMPTY;
    
    char []newBuffer = new char[end - start];
    
    System.arraycopy(_buffer, start, newBuffer, 0, end - start);
		     
    return new UnicodeBuilderValue(newBuffer, 0, end - start);
  }

  //
  // java.lang.String
  //

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(length);
    
    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];
      
      if ('A' <= ch && ch <= 'Z')
	dstBuffer[i] = (char) (ch + 'a' - 'A');
      else if (ch < 0x80)
	dstBuffer[i] = ch;
      else if (Character.isUpperCase(ch))
	dstBuffer[i] = Character.toLowerCase(ch);
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
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(_length);

    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];
      
      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (char) (ch + 'A' - 'a');
      else if (ch < 0x80)
	dstBuffer[i] = ch;
      else if (Character.isLowerCase(ch))
        dstBuffer[i] = Character.toUpperCase(ch);
      else
	dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
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

  public char []getRawCharArray()
  {
    return _buffer;
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
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("U:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(_buffer, 0, _length);
    sb.append("\";");
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
    return new UnicodeBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new UnicodeBuilderValue(length);
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("unicode(");
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
    
    out.print("unicode(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++)
      out.print(charAt(i));

    out.print("\"");
  }
  
  //
  // static helper functions
  //

  public static int getNumericType(char []buffer, int offset, int len)
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

  public static ValueType getValueType(char []buffer, int offset, int len)
  {
    if (len == 0) {
      // php/0307
      return ValueType.LONG_ADD;
    }

    int i = offset;
    int ch = 0;
    boolean hasPoint = false;

    while (i < len && Character.isWhitespace(buffer[i])) {
      i++;
    }
    
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
    
    while (i < len && Character.isWhitespace(buffer[i])) {
      i++;
    }

    if (len <= i)
      return ValueType.LONG_EQ;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = buffer[i]) && ch <= '9' ||
                       ch == '+' || ch == '-' || ch == 'e' || ch == 'E');
           i++) {
      }
      
      while (i < len && Character.isWhitespace(buffer[i])) {
        i++;
      }

      if (i < len)
        return ValueType.STRING;
      else
        return ValueType.DOUBLE_CMP;
    }
    else
      return ValueType.STRING;
  }

  /**
   * Converts to a long.
   */
  public static long toLong(char []buffer, int offset, int len)
  {
    return parseLong(buffer, offset, len);
  }

  public static double toDouble(char []buffer, int offset, int len)
  {
    int start = offset;
    int i = offset;
    int ch = 0;
    
    while (i < len && Character.isWhitespace(buffer[i])) {
      start++;
      i++;
    }

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
      return Double.parseDouble(new String(buffer, start, i - start));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  static {
    CHAR_STRINGS = new UnicodeBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new UnicodeBuilderValue((char) i);
  }
}

