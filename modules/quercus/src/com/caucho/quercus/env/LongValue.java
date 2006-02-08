/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;

/**
 * Represents a PHP long value.
 */
public class LongValue extends Value {
  public static final LongValue MINUS_ONE = new LongValue(-1);
  public static final LongValue ZERO = new LongValue(0);
  public static final LongValue ONE = new LongValue(1);

  public static final int STATIC_MIN = -32;
  public static final int STATIC_MAX = 32;

  public static final LongValue[]STATIC_VALUES;

  private final long _value;

  public LongValue(long value)
  {
    _value = value;
  }

  public static LongValue create(long value)
  {
    if (STATIC_MIN <= value && value <= STATIC_MAX)
      return STATIC_VALUES[(int) (value - STATIC_MIN)];
    else
      return new LongValue(value);
  }

  public static LongValue create(Number value)
  {
    if (value == null)
      return LongValue.ZERO;
    else
      return new LongValue(value.longValue());
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "integer";
  }

  /**
   * Returns true for a long.
   */
  public boolean isLong()
  {
    return true;
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _value != 0;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _value;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _value;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return String.valueOf(_value);
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return String.valueOf(_value);
  }

  /**
   * Negates the value.
   */
  public Value neg()
    throws Throwable
  {
    return new LongValue(- _value);
  }

  /**
   * Negates the value.
   */
  public Value pos()
    throws Throwable
  {
    return this;
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
    throws Throwable
  {
    return new LongValue(_value + incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
    throws Throwable
  {
    return new LongValue(_value + incr);
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof LongValue))
      return false;

    long rLong = ((LongValue) rValue)._value;

    return _value == rLong;
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
    sb.append("i:");
    sb.append(_value);
    sb.append(";");
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append(_value);
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
    if (_value == 0)
      out.print("LongValue.ZERO");
    else if (_value == 1)
      out.print("LongValue.ONE");
    else if (_value == -1)
      out.print("LongValue.MINUS_ONE");
    else if (STATIC_MIN <= _value && _value <= STATIC_MAX)
      out.print("LongValue.STATIC_VALUES[" + (_value - STATIC_MIN) + "]");
    else
      out.print("new LongValue(" + _value + "L)");
  }

  /**
   * Returns the hash code
   */
  public int hashCode()
  {
    return (int) (37 + 65537 * _value);
  }

  /**
   * Compare for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof LongValue))
      return false;

    LongValue value = (LongValue) o;

    return _value == value._value;
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value,String> valueSet)
    throws Throwable
  {
    out.print("int(" + toLong() + ")");
  }

  protected int compareToImpl(Value o)
  {
    if ((o instanceof LongValue) || (o instanceof DoubleValue)) {
      double thisValue = toDouble();
      double otherValue = o.toDouble();

      return thisValue == otherValue ? 0 : (thisValue < otherValue ? -1 : 1);
    }

    return -1 * o.compareTo(this);
  }

  static {
    STATIC_VALUES = new LongValue[STATIC_MAX - STATIC_MIN + 1];

    for (int i = STATIC_MIN; i <= STATIC_MAX; i++) {
      STATIC_VALUES[i - STATIC_MIN] = new LongValue(i);
    }
  }
}
