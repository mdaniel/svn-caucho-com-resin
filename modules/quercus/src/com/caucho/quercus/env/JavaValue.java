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

package com.caucho.php.env;

import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import com.caucho.vfs.WriteStream;

import com.caucho.php.program.Function;
import com.caucho.php.program.AbstractFunction;

import com.caucho.php.expr.Expr;

/**
 * Represents a PHP java value.
 */
public class JavaValue extends Value {
  private final JavaClassDefinition _classDef;

  private final Object _object;

  public JavaValue(Object object, JavaClassDefinition def)
  {
    _classDef = def;

    _object = object;
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

