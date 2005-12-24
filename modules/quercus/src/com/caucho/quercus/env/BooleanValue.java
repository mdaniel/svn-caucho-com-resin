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

import java.io.IOException;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP boolean value.
 */
public class BooleanValue extends Value {
  public static final BooleanValue TRUE = new BooleanValue(true);
  public static final BooleanValue FALSE = new BooleanValue(false);
  
  private final boolean _value;

  private BooleanValue(boolean value)
  {
    _value = value;
  }

  public static BooleanValue create(boolean value)
  {
    return value ? TRUE : FALSE;
  }

  public static BooleanValue create(Boolean value)
  {
    if (value == null)
      return FALSE;
    else if (Boolean.TRUE.equals(value))
      return TRUE;
    else
      return FALSE;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "boolean";
  }
  
  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _value;
  }
  
  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _value ? 1 : 0;
  }
  
  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _value ? 1 : 0;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return _value ? "1" : "";
  }
  
  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    Value obj = env.createObject();

    obj.put(SCALAR_V, this);
    
    return obj;
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return _value ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return _value ? LongValue.ONE : LongValue.ZERO;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    if (rValue instanceof StringValue) {
      String v = rValue.toString();
      
      if (_value)
	return ! v.equals("") && ! v.equals("0");
      else
	return v.equals("") || v.equals("0");
    }
    else if (rValue.isNumber())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
  }
  
  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws IOException
  {
    env.getOut().print(_value ? "1" : "");
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
    if (_value)
      out.print("com.caucho.quercus.env.BooleanValue.TRUE");
    else
      out.print("com.caucho.quercus.env.BooleanValue.FALSE");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    if (_value)
      out.print("true");
    else
      out.print("false");
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("b:");
    sb.append(_value ? 1 : 0);
    sb.append(';');
  }

  /**
   * Returns the hash code
   */
  public int hashCode()
  {
    return _value ? 17 : 37;
  }

  /**
   * Compare for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o.getClass() != getClass())
      return false;

    BooleanValue value = (BooleanValue) o;

    return _value == value._value;
  }
}

