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
 * Represents a PHP null value.
 */
public class NullValue extends Value {
  public static final NullValue NULL = new NullValue();

  protected NullValue()
  {
  }

  /**
   * Returns the null value singleton.
   */
  public static NullValue create()
  {
    return NULL;
  }
  
  /**
   * Returns the type.
   */
  public String getType()
  {
    return "NULL";
  }

  /**
   * Returns true for a set type.
   */
  public boolean isset()
  {
    return false;
  }
  
  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return false;
  }
  
  /**
   * Returns true for a null.
   */
  public boolean isNull()
  {
    return true;
  }
  
  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return 0;
  }
  
  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return 0;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "";
  }
  
  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return null;
  }
  
  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return NullValue.NULL;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return StringValue.EMPTY;
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    return rValue.isNull();
  }
  
  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws IOException
  {
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("N;");
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append("null");
  }

  /**
   * Returns a new array.
   */
  public Value getArray()
  {
    return new ArrayValueImpl();
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
    out.print("NullValue.NULL");
  }

  /**
   * Returns a new object.
   */
  public Value getObject(Env env)
  {
    return env.createObject();
  }
}

