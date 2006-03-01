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
import java.util.IdentityHashMap;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.lib.QuercusStringModule;

/**
 * Represents a PHP string value.
 */
public class StringValue extends AbstractStringValue {
  public static final StringValue EMPTY = new StringValue("");

  protected static final int IS_STRING = 0;
  protected static final int IS_LONG = 1;
  protected static final int IS_DOUBLE = 2;

  private final static StringValue []CHAR_STRINGS;

  private final String _value;

  public StringValue(String value)
  {
    if (value == null)
      throw new NullPointerException();

    _value = value;
  }

  /**
   * Create a string out of a byte array, with no consideration of character
   * encoding. Each byte becomes one character.
   */
  public StringValue(byte[] bytes)
  {
    final int len = bytes.length;

    char[] chars = new char[len];

    for (int i = 0; i < len; i++) {
      chars[i] = (char) bytes[i];
    }

    // XXX: string constructor copies the array
    _value = new String(chars);
  }

  /**
   * Creates the string.
   */
  public static Value create(String value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return new StringValue(value);
  }

  /**
   * Creates the string.
   */
  public static Value create(char value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new StringValue(String.valueOf(value));
  }

  /**
   * Creates the string.
   */
  public static Value create(Object value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return new StringValue(value.toString());
  }

  /**
   * Returns the value.
   */
  public String getValue()
  {
    return _value;
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
    return quercus.intern(_value);
  }

  /**
   * Returns true for a long
   */
  public boolean isLong()
  {
    String s = _value;
    
    int len = s.length();

    if (len == 0)
      return false;

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

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
    String s = _value;
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

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return ! _value.equals("") && ! _value.equals("0");
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return toLong(_value);
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
    int len = _value.length();
    int i = 0;
    int ch = 0;

    if (i < len && ((ch = _value.charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = _value.charAt(i)) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = _value.charAt(i)) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = _value.charAt(i)) == '+' || ch == '-') {
	i++;
      }

      for (; i < len && '0' <= (ch = _value.charAt(i)) && ch <= '9'; i++) {
      }

      if (i == e + 1)
	i = e;
    }

    if (i == 0)
      return 0;
    else if (i == len)
      return Double.parseDouble(_value);
    else
      return Double.parseDouble(_value.substring(0, i));
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
  {
    return _value;
  }

  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return _value;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    String s = _value;
    int len = s.length();

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    char ch = s.charAt(i);
    if (ch == '-') {
      sign = -1;
      i++;
    }

    for (; i < len; i++) {
      ch = s.charAt(i);

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
    String s = _value;
    
    final int len = s.length();
    byte[] bytes = new byte[len];

    for (int i = 0; i < len; i++) {
      bytes[i] = (byte) s.charAt(i);
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
    int len = _value.length();

    if (index < 0 || len <= index)
      return StringValue.EMPTY;
    else
      return StringValue.create(_value.charAt((int) index));
  }

  /**
   * sets the character at an index
   */
  public Value setCharAt(long index, String value)
  {
    int len = _value.length();

    if (index < 0 || len <= index)
      return this;
    else
      return new StringValue(_value.substring(0, (int) index) +
			     value +
			     _value.substring((int) (index + 1)));
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
    throws Throwable
  {
    return postincr(incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
    throws Throwable
  {
    if (incr > 0) {
      StringBuilder tail = new StringBuilder();

      for (int i = _value.length() - 1; i >= 0; i--) {
	char ch = _value.charAt(i);

	if (ch == 'z') {
	  if (i == 0)
	    return new StringValue("aa" + tail);
	  else
	    tail.insert(0, 'a');
	}
	else if ('a' <= ch && ch < 'z') {
	  return new StringValue(_value.substring(0, i) +
				 (char) (ch + 1) +
				 tail);
	}
	else if (ch == 'Z') {
	  if (i == 0)
	    return new StringValue("AA" + tail.toString());
	  else
	    tail.insert(0, 'A');
	}
	else if ('A' <= ch && ch < 'Z') {
	  return new StringValue(_value.substring(0, i) +
				 (char) (ch + 1) +
				 tail);
	}
	else if ('0' <= ch && ch <= '9' && i == _value.length() - 1) {
	  return new LongValue(toLong() + 1);
	}
      }

      return new StringValue(tail.toString());
    }
    else if (isLong()) {
      return new LongValue(toLong() - 1);
    }
    else {
      return this;
    }
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();

    if (rValue instanceof BooleanValue) {
      String v = toString();

      if (rValue.toBoolean())
	return ! v.equals("") && ! v.equals("0");
      else
	return v.equals("") || v.equals("0");
    }

    int type = getNumericType();

    if (type == IS_STRING) {
      if (rValue instanceof StringValue)
	return _value.equals(rValue.toString());
      else if (rValue.isLong())
	return toLong() ==  rValue.toLong();
      else if (rValue instanceof BooleanValue)
	return toLong() == rValue.toLong();
      else
	return _value.equals(rValue.toString());
    }
    else if (rValue.isNumber())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof StringValue))
      return false;

    String rString = ((StringValue) rValue)._value;

    return _value.equals(rString);
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
    out.printJavaString(_value);
    out.print("\")");
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws IOException
  {
    env.getOut().print(_value);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("s:");
    sb.append(_value.length());
    sb.append(":\"");
    sb.append(_value);
    sb.append("\";");
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append("'");

    String value = _value;
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
   * Returns the hash code.
   */
  public int hashCode()
  {
    return _value.hashCode();
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

    StringValue value = (StringValue) o;

    return _value.equals(value._value);
  }

  public String toString()
  {
    return _value;
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    String s = toString();

    out.print("string(" + s.length() + ") \"" + s + "\"");
  }

  static {
    CHAR_STRINGS = new StringValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new StringValue(String.valueOf((char) i));
  }

}

