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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.AbstractFunction;

/**
 * Quercus class information
 */
public class ClassesModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ClassesModule.class);
  private static final Logger log
    = Logger.getLogger(ClassesModule.class.getName());

  /**
   * Returns true if the class exists.
   */
  public boolean class_exists(Env env, String className, @Optional("true") boolean useAutoload)
  {
    return env.findClass(className, useAutoload) != null;
  }

  /**
   * Returns the object's class name
   */
  public Value get_class(Value value)
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      return new StringValueImpl(obj.getName());
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the declared classes
   */
  public static Value get_declared_classes(Env env)
  {
    return env.getDeclaredClasses();
  }

  /**
   * Returns the object's variables
   */
  public Value get_object_vars(Value obj)
  {
    //
    ArrayValue result = new ArrayValueImpl();

    for (Value name : obj.getIndices()) {
      result.put(name, obj.getField(name.toString().intern()));
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
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      String parent = obj.getParentName();

      if (parent != null)
	return new StringValueImpl(parent);
    }
    else if (value instanceof StringValue) {
      String className = value.toString();

      QuercusClass cl = env.findClass(className);

      if (cl != null) {
	String parent = cl.getParentName();

	if (parent != null)
	  return new StringValueImpl(parent);
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
    return obj.findFunction(methodName.intern()) != null;
  }
  
  /**
   * Returns an array of member names and values
   *
   * @param className the name of the class
   * 
   * @return an array of member names and values
   * 
   * @throws errorException
   */
  public static Value get_class_vars(Env env, String className)
  {
  	//  php/1j10
  	
    QuercusClass cl = null;
    
    try{
      cl = env.getClass(className);
    }
    catch(Throwable t) {
    	log.log(Level.WARNING, t.toString(), t);
    	
      return NullValue.NULL;
    }
    
    ArrayValue varArray = new ArrayValueImpl();
    
    for (Map.Entry<String, Expr> entry: cl.getClassVars()) {
    	Value key = StringValue.create(entry.getKey());
    	
    	Value value = entry.getValue().eval(env);
    	
    	varArray.append(key, value);
    }
    
    return varArray;
  }
  
  /**
   * Returns an array of method names and values
   *
   * @param className the name of the class
   * 
   * @return an array of method names and values
   * 
   * @throws errorException
   */
  public static Value get_class_methods(Env env, String className)
  {
  	// php/1j11
  	
    QuercusClass cl = null;
    
    try{
      cl = env.getClass(className);
    }
    catch(Throwable t) {
    	log.log(Level.WARNING, t.toString(), t);
    	
      return NullValue.NULL;
    }
    
    ArrayValue methArray = new ArrayValueImpl();
    
    for (Map.Entry<String, AbstractFunction> entry: cl.getClassMethods()) 	{
    	Value key = StringValue.create(entry.getKey());
    	
    	methArray.append(key);
    }
    
    return methArray;
  }
}
