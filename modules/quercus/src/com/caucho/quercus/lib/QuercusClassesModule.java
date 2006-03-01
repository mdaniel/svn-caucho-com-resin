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

package com.caucho.quercus.lib;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ReadOnly;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.ObjectValue;

import com.caucho.quercus.env.AbstractQuercusClass;

/**
 * PHP class information
 */
public class QuercusClassesModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusClassesModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusClassesModule.class.getName());

  /**
   * Returns true if the class exists.
   */
  public boolean class_exists(Env env, String className)
  {
    return env.findClass(className) != null;
  }

  /**
   * Returns the object's class name
   */
  public Value get_class(Value value)
    throws Throwable
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      return new StringValue(obj.getName());
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the declared classes
   */
  public static Value get_declared_classes(Env env)
    throws Throwable
  {
    return env.getDeclaredClasses();
  }

  /**
   * Returns the object's variables
   */
  public Value get_object_vars(Value obj)
    throws Throwable
  {
    // 
    ArrayValue result = new ArrayValueImpl();

    for (Value name : obj.getIndices()) {
      result.put(name, obj.getField(name.toString()));
    }

    return result;
  }

  /**
   * Returns true if the object implements the given class.
   */
  public static boolean is_a(@ReadOnly Value value, String name)
  {
    return value.isA(name);
  }

  /**
   * Returns true if the argument is an object.
   */
  public static boolean is_object(@ReadOnly Value value)
  {
    return value instanceof ObjectValue;
  }

  /**
   * Returns the object's class name
   */
  public Value get_parent_class(Env env, @ReadOnly Value value)
    throws Throwable
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      String parent = obj.getParentName();

      if (parent != null)
	return new StringValue(parent);
    }
    else if (value instanceof StringValue) {
      String className = value.toString();

      AbstractQuercusClass cl = env.findClass(className);

      if (cl != null) {
	String parent = cl.getParentName();

	if (parent != null)
	  return new StringValue(parent);
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns true if the named method exists on the object.
   *
   * @param obj the object to test
   * @param methodName the name of the method
   */
  public static boolean method_exists(Value obj, String methodName)
  {
    return obj.findFunction(methodName) != null;
  }
}
