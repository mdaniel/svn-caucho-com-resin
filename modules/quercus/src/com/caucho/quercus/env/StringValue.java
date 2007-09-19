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
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempCharBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.IdentityHashMap;

/**
 * Represents a Quercus string value.
 */
abstract public class StringValue extends Value implements CharSequence {
  public static final StringValue EMPTY = new StringBuilderValue("");

  private final static StringValue []CHAR_STRINGS;

  protected static final int IS_STRING = 0;
  protected static final int IS_LONG = 1;
  protected static final int IS_DOUBLE = 2;

  /**
   * Creates the string.
   */
  public static Value create(String value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    if (value == null)
      return NullValue.NULL;
    else
      return new StringBuilderValue(value);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new StringBuilderValue(String.valueOf(value));
  }

  /**
   * Creates the string.
   */
  public static Value create(Object value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    if (value == null)
      return NullValue.NULL;
    else
      return new StringBuilderValue(value.toString());
  }

  //
  // Predicates and relations
  //

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
    return ValueType.STRING;
  }

  /**
   * Returns true for a long
   */
  public boolean isLongConvertible()
  {
    int len = length();

    if (len == 0)
      return true;

    int i = 0;
    char ch = charAt(0);

    if (ch == '-' || ch == '+')
      i++;

    for (; i < len; i++) {
      ch = charAt(i);

      if (! ('0' <= ch && ch <= '9'))
        return false;
    }

    return true;
  }

  /**
   * Returns true for a double
   */
  public boolean isDoubleConvertible()
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
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    // php/120y

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
   * Returns true for StringValue
   */
  @Override
  public boolean isString()
  {
    return true;
  }

  /**
   * Returns true for equality
   */
  public int cmp(Value rValue)
  {
    if (isNumberConvertible() || rValue.isNumberConvertible()) {
      double l = toDouble();
      double r = rValue.toDouble();

      if (l == r)
	return 0;
      else if (l < r)
	return -1;
      else
	return 1;
    }
    else
      return toString().compareTo(rValue.toString());
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
    else {
      return toString().equals(rValue.toString());
    }
  }

  /**
   * Compare two strings
   */
  public int cmpString(StringValue rValue)
  {
    if (isNumberConvertible() && rValue.isNumberConvertible()) {
      double thisDouble = toDouble();
      
      double rDouble = rValue.toDouble();
      
      if (thisDouble < rDouble)
	return -1;
      else if (thisDouble > rDouble)
	return 1;
      else
	return 0;
    }
    return toString().compareTo(rValue.toString());
  }

  /**
   * Returns a code for the numeric type of the string
   */
  protected int getNumericType()
  {
    int len = length();

    if (len == 0)
      return IS_STRING;

    int i = 0;
    int ch = 0;
    boolean hasPoint = false;

    if (i < len && ((ch = charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return IS_STRING;

    ch = charAt(i);

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = charAt(i)) && ch <= '9'; i++) {
        return IS_DOUBLE;
      }

      return IS_STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return IS_STRING;

    for (; i < len && '0' <= (ch = charAt(i)) && ch <= '9'; i++) {
    }

    if (len <= i)
      return IS_LONG;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = charAt(i)) && ch <= '9' ||
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

  // Conversions

  /**
   * Converts to a string value.
   */
  public StringValue toStringValue()
  {
    return this;
  }

  /**
   * Converts to a long.
   */
  public static long toLong(String string)
  {
    if (string.equals(""))
      return 0;

    int len = string.length();

    long value = 0;
    long sign = 1;

    int i = 0;
    char ch = string.charAt(0);

    if (ch == '-') {
      sign = -1;
      i = 1;
    }
    else if (ch == '+')
      i = 1;

    for (; i < len; i++) {
      ch = string.charAt(i);

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
    return toDouble(toString());
  }

  /**
   * Converts to a double.
   */
  public static double toDouble(String s)
  {
    int len = s.length();
    int i = 0;
    int ch = 0;

    if (i < len && ((ch = s.charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = s.charAt(i)) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i == 0)
      return 0;
    else if (i == len)
      return Double.parseDouble(s);
    else
      return Double.parseDouble(s.substring(0, i));
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    int length = length();

    if (length == 0)
      return false;
    else if (length > 1)
      return true;
    else
      return charAt(0) != '0';
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    int len = length();

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    char ch = charAt(i);
    if (ch == '-') {
      sign = -1;
      i++;
    }

    for (; i < len; i++) {
      ch = charAt(i);

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return this;
    }

    return LongValue.create(sign * value);
  }

  /**
   * Converts to a Java object.
   */
  public Object toJavaObject()
  {
    return toString();
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, Class elementType)
  {
    if (char.class.equals(elementType)) {
      return toUnicodeValue(env).toCharArray();
    }
    else if (Character.class.equals(elementType)) {
      char[] chars = toUnicodeValue(env).toCharArray();
      
      int length = chars.length;
      
      Character[] charObjects = new Character[length];
      
      for (int i = 0; i <length; i++) {
        charObjects[i] = Character.valueOf(chars[i]);
      }
      
      return charObjects;
    }
    else if (byte.class.equals(elementType)) {
      return toBinaryValue(env).toBytes();
    }
    else if (Byte.class.equals(elementType)) {
      byte[] bytes = toBinaryValue(env).toBytes();
      
      int length = bytes.length;
      
      Byte[] byteObjects = new Byte[length];
      
      for (int i = 0; i <length; i++) {
        byteObjects[i] = Byte.valueOf(bytes[i]);
      }
      
      return byteObjects;
    }
    else {
      env.error(L.l("Can't assign {0} with type {1} to {2}", this, this.getClass(), elementType));
      return null;
    }
  }
  
  /**
   * Converts to an array if null.
   */
  public Value toAutoArray()
  {
    if (length() == 0)
      return new ArrayValueImpl();
    else
      return this;
  }

  // Operations

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
  public Value getArg(Value key)
  {
    // php/03ma
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
    int len = length();

    if (index < 0 || len <= index)
      return UnsetUnicodeValue.UNSET;
    else {
      return StringValue.create(charAt((int) index));
    }
  }

  /**
   * sets the character at an index
   */
  @Override
  public Value setCharValueAt(long index, String value)
  {
    int len = length();

    if (index < 0 || len <= index)
      return this;
    else {
      return (createStringBuilder()
	      .append(this, 0, (int) index)
	      .append(value)
	      .append(this, (int) (index + 1), length()));
    }
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
  {
    return postincr(incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
  {
    // php/03i6
    if (length() == 0) {
      if (incr == 1)
        return createStringBuilder().append("1");
      else
        return LongValue.MINUS_ONE;
    }

    if (incr > 0) {
      StringBuilder tail = new StringBuilder();

      for (int i = length() - 1; i >= 0; i--) {
        char ch = charAt(i);

        if (ch == 'z') {
          if (i == 0)
            return createStringBuilder().append("aa").append(tail);
          else
            tail.insert(0, 'a');
        }
        else if ('a' <= ch && ch < 'z') {
          return (createStringBuilder()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if (ch == 'Z') {
          if (i == 0)
            return createStringBuilder().append("AA").append(tail);
          else
            tail.insert(0, 'A');
        }
        else if ('A' <= ch && ch < 'Z') {
          return (createStringBuilder()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if ('0' <= ch && ch <= '9' && i == length() - 1) {
          return LongValue.create(toLong() + 1);
        }
      }

      return createStringBuilder().append(tail.toString());
    }
    else if (isLongConvertible()) {
      return LongValue.create(toLong() - 1);
    }
    else {
      return this;
    }
  }

  /**
   * Adds to the following value.
   */
  public Value add(long rValue)
  {
    if (isLongConvertible())
      return LongValue.create(toLong() + rValue);
    
    return DoubleValue.create(toDouble() + rValue);
  }

  /**
   * Adds to the following value.
   */
  public Value sub(long rValue)
  {
    if (isLongConvertible())
      return LongValue.create(toLong() - rValue);
    
    return DoubleValue.create(toDouble() - rValue);
  }
  
  /**
   * Serializes the value.
   */
  @Override
  public void serialize(StringBuilder sb)
  {
    sb.append("s:");
    sb.append(length());
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
  public StringValue append(String s)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java string to the value.
   */
  public StringValue append(String s, int start, int end)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(char []buf, int offset, int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue append(char []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(CharSequence buf, int head, int tail)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(UnicodeBuilderValue sb, int head, int tail)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java char to the value.
   */
  public StringValue append(char v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java boolean to the value.
   */
  public StringValue append(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  public StringValue append(long v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue append(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue append(Object v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue append(Value v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Ensure enough append capacity.
   */
  public void ensureAppendCapacity(int size)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a byte buffer to the value.
   */
  public StringValue append(byte []buf, int offset, int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append from a read stream
   */
  public StringValue append(Reader reader)
    throws IOException
  {
    int ch;
    
    while ((ch = reader.read()) >= 0) {
      append((char) ch);
    }

    return this;
  }

  /**
   * Append from a read stream
   */
  public StringValue append(Reader reader, long length)
    throws IOException
  {
    int ch;
    
    while (length-- > 0 && (ch = reader.read()) >= 0) {
      append((char) ch);
    }

    return this;
  }

  /**
   * Append from an input stream
   */
  public StringValue append(InputStream is)
  {
    try {
      int ch;
    
      while ((ch = is.read()) >= 0) {
	appendByte(ch);
      }

      return this;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Append from an input stream
   */
  public StringValue append(InputStream is, long length)
  {
    try {
      int ch;
    
      while (length-- > 0 && (ch = is.read()) >= 0) {
	appendByte(ch);
      }

      return this;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Append from an input stream
   */
  public StringValue append(BinaryInput is)
  {
    try {
      int ch;
    
      while ((ch = is.read()) >= 0) {
	appendByte(ch);
      }

      return this;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Append from an input stream
   */
  public StringValue append(BinaryInput is, long length)
  {
    try {
      int ch;
    
      while (length-- > 0 && (ch = is.read()) >= 0) {
	appendByte(ch);
      }

      return this;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    int length = length();

    for (int i = 0; i < length; i++)
      sb.append(charAt(i));

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  public StringValue appendUnicode(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  public StringValue appendUnicode(long v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue appendUnicode(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue appendUnicode(Object v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java char, possibly converting to a unicode string
   */
  public StringValue appendUnicode(char v)
  {
    return append(v);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(char []buffer, int offset, int length)
  {
    return append(buffer, offset, length);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(String value)
  {
    return append(value);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(String value, int offset, int length)
  {
    return append(value, offset, length);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(Value value)
  {
    return append(value);
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  public StringValue appendByte(int v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Append a Java String to the value without conversions.
   */
  public StringValue appendBytes(String s)
  {
    StringValue sb = this;
    
    for (int i = 0; i < s.length(); i++) {
      sb = sb.appendByte(s.charAt(i));
    }
    
    return sb;
  }
  
  /**
   * Append a Java char[] to the value without conversions.
   */
  public StringValue appendBytes(char []buf, int offset, int length)
  {
    StringValue sb = this;
    int end = Math.min(buf.length, offset + length);
    
    while (offset < end) {
      sb = sb.appendByte(buf[offset++]);
    }
    
    return sb;
  }

  /**
   * Exports the value.
   */
  @Override
  public void varExport(StringBuilder sb)
  {
    sb.append("'");

    String value = toString();
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      switch (ch) {
      case '\'':
        sb.append("\\'");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
      }
    }
    sb.append("'");
  }

  /**
   * Interns the string.
   */
  public StringValue intern(Quercus quercus)
  {
    return quercus.intern(toString());
  }

  //
  // CharSequence
  //

  /**
   * Returns the length of the string.
   */
  public int length()
  {
    return toString().length();
  }

  /**
   * Returns the character at a particular location
   */
  public char charAt(int index)
  {
    return toString().charAt(index);
  }

  /**
   * Returns a subsequence
   */
  public CharSequence subSequence(int start, int end)
  {
    return new StringBuilderValue(toString().substring(start, end));
  }

  //
  // java.lang.String methods
  //

  /**
   * Returns the first index of the match string, starting from the head.
   */
  public final int indexOf(CharSequence match)
  {
    return indexOf(match, 0);
  }
    
  /**
   * Returns the first index of the match string, starting from the head.
   */
  public int indexOf(CharSequence match, int head)
  {
    int length = length();
    int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    else if (head < 0)
      return -1;
    
    int end = length - matchLength;
    char first = match.charAt(0);

    loop:
    for (; head <= end; head++) {
      if (charAt(head) != first)
	continue;

      for (int i = 1; i < matchLength; i++) {
	if (charAt(head + i) != match.charAt(i))
	  continue loop;
      }

      return head;
    }

    return -1;
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int indexOf(char match)
  {
    return indexOf(match, 0);
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int indexOf(char match, int head)
  {
    int length = length();
    
    for (; head < length; head++) {
      if (charAt(head) == match)
	return head;
    }

    return -1;
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public final int lastIndexOf(char match)
  {
    return lastIndexOf(match, Integer.MAX_VALUE);
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int lastIndexOf(char match, int tail)
  {
    int length = length();

    if (tail >= length)
      tail = length - 1;
    
    for (; tail >= 0; tail--) {
      if (charAt(tail) == match)
	return tail;
    }

    return -1;
  }

  /**
   * Returns the last index of the match string, starting from the tail.
   */
  public int lastIndexOf(CharSequence match)
  {
    return lastIndexOf(match, Integer.MAX_VALUE);
  }

  /**
   * Returns the last index of the match string, starting from the tail.
   */
  public int lastIndexOf(CharSequence match, int tail)
  {
    int length = length();
    int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    if (tail < 0)
      return -1;

    if (tail > length - matchLength)
      tail = length - matchLength;

    char first = match.charAt(0);

    loop:
    for (; tail >= 0; tail--) {
      if (charAt(tail) != first)
        continue;

      for (int i = 1; i < matchLength; i++) {
        if (charAt(tail + i) != match.charAt(i))
	      continue loop;
      }

      return tail;
    }

    return -1;
  }

  /**
   * Returns a StringValue substring.
   */
  public StringValue substring(int head)
  {
    return (StringValue) subSequence(head, length());
  }

  /**
   * Returns a StringValue substring.
   */
  public StringValue substring(int begin, int end)
  {
    return (StringValue) subSequence(begin, end);
  }

  /**
   * Returns a character array
   */
  public char []toCharArray()
  {
    int length = length();
    
    char []array = new char[length()];

    getChars(0, array, 0, length);

    return array;
  }

  public char []getRawCharArray()
  {
    return toCharArray();
  }

  /**
   * Copies the chars
   */
  public void getChars(int stringOffset, char []buffer, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      buffer[offset + i] = charAt(stringOffset + i);
  }

  /**
   * Convert to lower case.
   */
  public StringValue toLowerCase()
  {
    int length = length();
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(length);
    
    char []buffer = string.getBuffer();
    getChars(0, buffer, 0, length);

    for (int i = 0; i < length; i++) {
      char ch = buffer[i];
      
      if ('A' <= ch && ch <= 'Z')
	buffer[i] = (char) (ch + 'a' - 'A');
      else if (ch < 0x80) {
      }
      else if (Character.isUpperCase(ch))
	buffer[i] = Character.toLowerCase(ch);
    }

    string.setOffset(length);

    return string;
  }
  
  /**
   * Convert to lower case.
   */
  public StringValue toUpperCase()
  {
    int length = length();
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(length);

    char []buffer = string.getBuffer();
    getChars(0, buffer, 0, length);

    for (int i = 0; i < length; i++) {
      char ch = buffer[i];
      
      if ('a' <= ch && ch <= 'z')
        buffer[i] = (char) (ch + 'A' - 'a');
      else if (ch < 0x80) {
      }
      else if (Character.isLowerCase(ch))
        buffer[i] = Character.toUpperCase(ch);
    }

    string.setOffset(length);

    return string;
  }

  /**
   * Returns a byteArrayInputStream for the value.
   * See TempBufferStringValue for how this can be overriden
   *
   * @return InputStream
   */
  public InputStream toInputStream()
  {
    try {
      //XXX: refactor so that env is passed in
      return toInputStream(Env.getInstance().getRuntimeEncoding().toString());
    }
    catch (UnsupportedEncodingException e) {
      throw new QuercusRuntimeException(e);
    }
    //return new StringValueInputStream();
  }

  /**
   * Returns a byte stream of chars.
   * @param charset to encode chars to
   */
  public InputStream toInputStream(String charset)
    throws UnsupportedEncodingException
  {
    return new ByteArrayInputStream(toString().getBytes(charset));
  }

  /**
   * Returns a char stream.
   * XXX: when decoding fails
   *
   * @param charset to decode bytes by
   */
  public Reader toReader(String charset)
    throws UnsupportedEncodingException
  {
    byte []bytes = toString().getBytes();
    
    return new InputStreamReader(new ByteArrayInputStream(bytes), charset);
  }

  /**
   * Converts to a BinaryValue in desired charset.
   *
   * @param env
   * @param charset
   */
  public StringValue toBinaryValue(Env env, String charset)
  {
    TempBuffer tb = TempBuffer.allocate();
    byte[] buffer = tb.getBuffer();

    try {
      InputStream in = toInputStream(charset);
      TempStream out = new TempStream();

      int sublen = in.read(buffer, 0, buffer.length);

      while (sublen >= 0) {
        out.write(buffer, 0, sublen, false);
        sublen = in.read(buffer, 0, buffer.length);
      }

      out.flush();

      StringValue result = env.createBinaryBuilder();
      for (TempBuffer ptr = out.getHead();
           ptr != null;
           ptr = ptr.getNext()) {
        result.append(ptr.getBuffer(), 0, ptr.getLength());
      }

      TempBuffer.free(out.getHead());

      return result;
    } catch (IOException e) {
      throw new QuercusModuleException(e.getMessage());
    } finally {
      TempBuffer.free(tb);
    }
  }

  public byte []toBytes()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  public StringValue toUnicodeValue(Env env, String charset)
  {
    StringValue sb = env.createUnicodeBuilder();

    TempCharBuffer tb = TempCharBuffer.allocate();
    char[] charBuf = tb.getBuffer();

    try {
      Reader in = toReader(charset);

      int sublen;
      while ((sublen = in.read(charBuf, 0, charBuf.length)) >= 0) {
        sb.append(charBuf, 0, sublen);
      }

    } catch (IOException e) {
      throw new QuercusModuleException(e.getMessage());

    } finally {
      TempCharBuffer.free(tb);
    }

    return sb;
  }

  /**
   * Creates a string builder of the same type.
   */
  public StringValue createStringBuilder()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Creates a string builder of the same type.
   */
  public StringValue createStringBuilder(int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return createStringBuilder().append(this);
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

    int length = length();

    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + charAt(i);
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
    else if (! (o instanceof StringValue))
      return false;

    StringValue s = (StringValue) o;

    if (s.isUnicode() != isUnicode())
      return false;
    
    int aLength = length();
    int bLength = s.length();

    if (aLength != bLength)
      return false;

    for (int i = aLength - 1; i >= 0; i--) {
      if (charAt(i) != s.charAt(i))
	return false;
    }

    return true;
  }

  @Override
  abstract public String toDebugString();

  @Override
  abstract public void varDumpImpl(Env env,
                                   WriteStream out,
                                   int depth,
                                   IdentityHashMap<Value, String> valueSet)
    throws IOException;

  class StringValueInputStream extends java.io.InputStream {
    private final int _length;
    private int _index;

    StringValueInputStream()
    {
      _length = length();
    }
    
    /**
     * Reads the next byte.
     */
    public int read()
    {
      if (_index < _length)
	return charAt(_index++);
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

      int index = _index;

      for (int i = 0; i < sublen; i++)
	buffer[offset + i] = (byte) charAt(index + i);

      _index += sublen;

      return sublen;
    }
  }

  static {
    // XXX: need to update for unicode
    
    CHAR_STRINGS = new StringValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new StringBuilderValue(String.valueOf((char) i));
  }
}

