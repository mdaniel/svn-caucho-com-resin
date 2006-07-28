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

import java.util.IdentityHashMap;

import java.io.InputStream;
import java.io.IOException;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.Quercus;

/**
 * Represents a Quercus string value.
 */
abstract public class StringValue extends Value implements CharSequence {
  public static final StringValue EMPTY = new StringValueImpl("");

  private final static StringValue []CHAR_STRINGS;

  protected static final int IS_STRING = 0;
  protected static final int IS_LONG = 1;
  protected static final int IS_DOUBLE = 2;

  /**
   * Creates the string.
   */
  public static Value create(String value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return new StringValueImpl(value);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new StringValueImpl(String.valueOf(value));
  }

  /**
   * Creates the string.
   */
  public static Value create(Object value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return new StringValueImpl(value.toString());
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
   * Returns true for a long
   */
  public boolean isLongConvertible()
  {
    int len = length();

    if (len == 0)
      return false;

    for (int i = 0; i < len; i++) {
      char ch = charAt(i);

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
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();

    if (rValue instanceof BooleanValue) {
      return toBoolean() == rValue.toBoolean();
    }

    int type = getNumericType();

    if (type == IS_STRING) {
      if (rValue instanceof StringValue)
        return equals(rValue);
      else if (rValue.isLongConvertible())
        return toLong() ==  rValue.toLong();
      else if (rValue instanceof BooleanValue)
        return toLong() == rValue.toLong();
      else
        return equals(rValue.toStringValue());
    }
    else if (rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return equals(rValue.toStringValue());
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
  public boolean eql(Value rValue)
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof StringValue))
      return false;

    String rString = rValue.toString();

    return toString().equals(rString);
  }

  /**
   * Compare two strings
   */
  public int cmpString(StringValue rValue)
  {
    if (isNumberConvertible() && rValue.isNumberConvertible()) {
      double thisDouble = toDouble();
      double rDouble = rValue.toDouble();
      if (thisDouble < rDouble) return -1;
      if (thisDouble > rDouble) return 1;
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

    if (string.charAt(0) == '-') {
      sign = -1;
      i = 1;
    }

    for (; i < len; i++) {
      char ch = string.charAt(i);

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
    int len = length();
    int i = 0;
    int ch = 0;

    if (i < len && ((ch = charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = charAt(i)) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = charAt(i)) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = charAt(i)) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = charAt(i)) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i == 0)
      return 0;
    else if (i == len)
      return Double.parseDouble(toString());
    else
      return Double.parseDouble(substring(0, i).toString());
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
      return StringValue.EMPTY;
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
      return (new StringBuilderValue()
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
    if (incr > 0) {
      StringBuilder tail = new StringBuilder();

      for (int i = length() - 1; i >= 0; i--) {
        char ch = charAt(i);

        if (ch == 'z') {
          if (i == 0)
            return new StringBuilderValue().append("aa").append(tail);
          else
            tail.insert(0, 'a');
        }
        else if ('a' <= ch && ch < 'z') {
          return (new StringBuilderValue()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if (ch == 'Z') {
          if (i == 0)
            return new StringBuilderValue().append("AA").append(tail);
          else
            tail.insert(0, 'A');
        }
        else if ('A' <= ch && ch < 'Z') {
          return (new StringBuilderValue()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if ('0' <= ch && ch <= '9' && i == length() - 1) {
          return LongValue.create(toLong() + 1);
        }
      }

      return new StringBuilderValue(tail.toString());
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


  /**
   * Append to a string builder.
   */
  @Override
  public void appendTo(StringBuilderValue sb)
  {
    int length = length();

    for (int i = 0; i < length; i++)
      sb.append(charAt(i));
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
  public InternStringValue intern(Quercus quercus)
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
    return new StringValueImpl(toString().substring(start, end));
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
    return lastIndexOf(match, 0);
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

    if (length < tail)
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
    
    StringBuilderValue string = new StringBuilderValue(length);

    char []buffer = string.toCharArray();
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

    string.setLength(length);

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
    return new StringValueInputStream();
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

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    String s = toString();

    out.print("string(" + s.length() + ") \"" + s + "\"");
  }

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
    CHAR_STRINGS = new StringValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new StringValueImpl(String.valueOf((char) i));
  }
}

