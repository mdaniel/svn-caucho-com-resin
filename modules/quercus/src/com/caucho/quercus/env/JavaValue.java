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

import com.caucho.quercus.expr.Expr;

/**
 * Represents a PHP java value.
 */
public class JavaValue extends ResourceValue {
  private final JavaClassDefinition _classDef;

  private final Object _object;

  public JavaValue(Object object, JavaClassDefinition def)
  {
    _classDef = def;

    _object = object;
  }

  @Override
  public Value getField(String name)
  {
    return _classDef.getField(name, _object);
  }

  @Override
  public Value putField(Env env,
                        String name,
                        Value value)
  {
    return _classDef.putField(env, _object, name, value);
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _classDef.getName();
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "object";
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return 1;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return toLong();
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return new LongValue(System.identityHashCode(this));
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Expr []args)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value []args)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a1)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a1, Value a2)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a1, Value a2, Value a3)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return _classDef.evalMethod(env, _object, methodName, a1, a2, a3, a4, a5);
  }

  /**
   * Returns the iterator values.
   */
  public Value []getValueArray(Env env)
  {
    return _classDef.getValueArray(env, _object);
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return String.valueOf(_object);
  }


  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return _object;
  }
}

