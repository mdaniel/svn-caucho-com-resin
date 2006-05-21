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

import java.io.IOException;

import com.caucho.vfs.WriteStream;

/**
 * Represents a PHP string value.
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
  public static Value create(char value)
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

  // Predicates and relations

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    String v = toString();

    rValue = rValue.toValue();

    if (rValue instanceof BooleanValue) {
      if (rValue.toBoolean())
        return ! v.equals("") && ! v.equals("0");
      else
        return v.equals("") || v.equals("0");
    }

    int type = getNumericType();

    if (type == IS_STRING) {
      if (rValue instanceof StringValue)
        return v.equals(rValue.toString());
      else if (rValue.isLongConvertible())
        return toLong() ==  rValue.toLong();
      else if (rValue instanceof BooleanValue)
        return toLong() == rValue.toLong();
      else
        return v.equals(rValue.toString());
    }
    else if (rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
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
    String s = toString();
    int len = s.length();

    if (len == 0)
      return IS_STRING;

    int i = 0;
    int ch = 0;
    boolean hasPoint = false;

    if (i < len && ((ch = s.charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return IS_STRING;

    ch = s.charAt(i);

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
        return IS_DOUBLE;
      }

      return IS_STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return IS_STRING;

    for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
    }

    if (len <= i)
      return IS_LONG;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = s.charAt(i)) && ch <= '9' ||
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
      String s = toString();

      StringBuilder tail = new StringBuilder();

      for (int i = s.length() - 1; i >= 0; i--) {
        char ch = s.charAt(i);

        if (ch == 'z') {
          if (i == 0)
            return new StringValueImpl("aa" + tail);
          else
            tail.insert(0, 'a');
        }
        else if ('a' <= ch && ch < 'z') {
          return new StringValueImpl(s.substring(0, i) +
                                     (char) (ch + 1) +
                                     tail);
        }
        else if (ch == 'Z') {
          if (i == 0)
            return new StringValueImpl("AA" + tail.toString());
          else
            tail.insert(0, 'A');
        }
        else if ('A' <= ch && ch < 'Z') {
          return new StringValueImpl(s.substring(0, i) +
                                     (char) (ch + 1) +
                                     tail);
        }
        else if ('0' <= ch && ch <= '9' && i == s.length() - 1) {
          return new LongValue(toLong() + 1);
        }
      }

      return new StringValueImpl(tail.toString());
    }
    else if (isLongConvertible()) {
      return new LongValue(toLong() - 1);
    }
    else {
      return this;
    }
  }

  /**
   * Exports the value.
   */
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

  static {
    CHAR_STRINGS = new StringValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new StringValueImpl(String.valueOf((char) i));
  }
}

