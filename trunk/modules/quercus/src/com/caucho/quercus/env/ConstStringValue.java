/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Represents a StringValue that is never modified.
 * For compiled code.
 */
public class ConstStringValue
  extends StringBuilderValue
{
  public static final ConstStringValue EMPTY = new ConstStringValue();

  private LongValue _longValue;
  private DoubleValue _doubleValue;
  private String _string;

  private Value _key;
  private ValueType _valueType;
  private char []_serializeValue;

  private StringValue _lowerCase;

  public ConstStringValue()
  {
    super();
  }

  public ConstStringValue(StringBuilderValue sb)
  {
    super(sb.getBuffer(), 0, sb.length());
  }

  public ConstStringValue(byte []buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  public ConstStringValue(char []buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  /**
   * Creates a new StringBuilderValue with the buffer without copying.
   */
  public ConstStringValue(char []buffer, int length)
  {
    super(buffer, length);
  }

  public ConstStringValue(byte []buffer)
  {
    super(buffer);
  }

  public ConstStringValue(char ch)
  {
    super(1, true);
    setLength(1);

    byte[] buffer = getBuffer();
    buffer[0] = (byte) (ch & 0xff);
  }

  public ConstStringValue(byte ch) {
    super(1, true);
    setLength(1);

    byte[] buffer = getBuffer();
    buffer[0] = (byte) (ch & 0xff);
  }

  public ConstStringValue(String s)
  {
    super(s);

    _string = s;
  }

  public ConstStringValue(char []s)
  {
    super(s);
  }

  public ConstStringValue(char []s, Value v1)
  {
    super(s, v1);
  }

  public ConstStringValue(byte []s, Value v1)
  {
    super(s, v1);
  }

  public ConstStringValue(Value v1)
  {
    super(v1);
  }

  public ConstStringValue(Value v1, Value v2)
  {
    super(v1, v2);
  }

  public ConstStringValue(Value v1, Value v2, Value v3)
  {
    super(v1, v2, v3);
  }

  public boolean isStatic()
  {
    return true;
  }

  protected void setLongValue(LongValue value)
  {
    _longValue = value;
  }

  protected void setDoubleValue(DoubleValue value)
  {
    _doubleValue = value;
  }

  protected void setString(String value)
  {
    _string = value;
  }

  protected void setKey(Value value)
  {
    _key = value;
  }

  protected void setValueType(ValueType valueType)
  {
    _valueType = valueType;
  }

  protected void setLowerCase(StringValue lowerCase)
  {
    _lowerCase = lowerCase;
  }

  /**
   * Converts to a long vaule
   */
  @Override
  public LongValue toLongValue()
  {
    if (_longValue == null)
      _longValue = LongValue.create(super.toLong());

    return _longValue;
  }

  /**
   * Converts to a double vaule
   */
  @Override
  public DoubleValue toDoubleValue()
  {
    if (_doubleValue == null)
      _doubleValue = new DoubleValue(super.toDouble());

    return _doubleValue;
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return toLongValue().toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDoubleValue().toDouble();
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    if (_valueType == null) {
      _valueType = super.getValueType();
    }

    return _valueType;
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    if (_key == null) {
      _key = super.toKey();
    }

    return _key;
  }

  @Override
  public StringValue toLowerCase(Locale locale)
  {
    if (_lowerCase == null) {
      _lowerCase = super.toLowerCase(locale);
    }

    return _lowerCase;
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb)
  {
    if (_serializeValue == null) {
      StringBuilder s = new StringBuilder();

      super.serialize(env, s);

      int len = s.length();

      _serializeValue = new char[len];
      s.getChars(0, len, _serializeValue, 0);
    }

    sb.append(_serializeValue, 0, _serializeValue.length);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PrintWriter out)
    throws IOException
  {
    generateImpl(out, this);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public static void generateImpl(PrintWriter out, StringBuilderValue value)
    throws IOException
  {
    // max JVM constant string length
    int maxSublen = 0xFFFE;

    int len = value.length();

    if (len == 1) {
      out.print("(ConstStringValue.create((char) '");
      printJavaChar(out, value.charAt(0));
      out.print("'))");
    }
    else if (len < maxSublen) {
      out.print("(new CompiledConstStringValue (\"");
      printJavaString(out, value);
      out.print("\", ");
      value.toLongValue().generate(out);
      out.print(", ");
      value.toDoubleValue().generate(out);
      out.print(", ");
      out.print(value.getValueType());
      out.print(", ");

      Value key = value.toKey();

      if (key instanceof LongValue) {
        key.generate(out);
        out.print(", ");
      }

      out.print(value.hashCode());
      out.print("))");
    }
    else {
      out.print("(new ConstStringValue(new StringBuilderValue(\"");

      // php/313u
      for (int i = 0; i < len; i += maxSublen) {
        if (i != 0)
          out.print("\").append(\"");

        printJavaString(out, value.substring(i, Math.min(i + maxSublen, len)));
      }

      out.print("\")))");
    }
  }

  public String toString()
  {
    if (_string == null)
      _string = super.toString();

    return _string;
  }
}
