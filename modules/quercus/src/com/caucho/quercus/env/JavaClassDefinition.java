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

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.Function;

import com.caucho.quercus.module.Marshall;
import com.caucho.quercus.module.JavaMarshall;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents an introspected Java class.
 */
public class JavaClassDefinition extends AbstractQuercusClass {
  private final static Logger log
    = Logger.getLogger(JavaClassDefinition.class.getName());
  private final static L10N L = new L10N(JavaClassDefinition.class);
  
  private final Quercus _quercus;
  
  private final String _name;
  private final Class _type;
  
  private final HashMap<String,Value> _constMap
    = new HashMap<String,Value>();
  
  private final HashMap<String,JavaMethod> _functionMap
    = new HashMap<String,JavaMethod>();

  private JavaConstructor _cons;

  private Method _iterator;

  private Marshall _marshall;

  public JavaClassDefinition(Quercus quercus, String name, Class type)
  {
    _quercus = quercus;
    
    _name = name;

    _type = type;
  }
  
  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the marshall instance.
   */
  public Marshall getMarshall()
  {
    return _marshall;
  }

  /**
   * Creates a new instance.
   */
  public Value newInstance()
  {
    try {
      Object obj = _type.newInstance();

      return new JavaValue(_type.newInstance(), this);
    } catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Expr []args)
    throws Throwable
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Value []args)
    throws Throwable
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Expr []args)
    throws Throwable
  {
    JavaMethod method = _functionMap.get(name);

    if (method == null) {
      env.warning(env.getLocation() + L.l("{0}::{1} is an unknown method.",
					  _name, name));

      return NullValue.NULL;
    }

    return method.eval(env, obj, args);
  }
  
  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value []args)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value a1)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
			  Value a1, Value a2)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
			  Value a1, Value a2, Value a3)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
			  Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3, a4);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3, a4, a5);
  }

  /**
   * Returns the values for an iterator.
   */
  public Value []getValueArray(Env env, Object obj)
  {
    try {
      if (_iterator == null)
	return new Value[0];

      Iterator iter = (Iterator) _iterator.invoke(obj);

      ArrayList<Value> values = new ArrayList<Value>();

      while (iter.hasNext()) {
	Object objValue = iter.next();

	if (objValue instanceof Value)
	  values.add((Value) objValue);
	else
	  values.add(env.wrapJava(objValue));
      }

      Value []valueArray = new Value[values.size()];

      values.toArray(valueArray);

      return valueArray;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private JavaMethod getMethod(Env env, String name)
  {
    JavaMethod method = _functionMap.get(name);

    if (method == null) {
      env.error("'" + name + "' is an unknown method.");
    }

    return method;
  }

  /**
   * Finds the matching constant
   */
  public Value findConstant(Env env, String name)
  {
    return _constMap.get(name);
  }

  /**
   * Introspects the Java class.
   */
  public void introspect(Quercus quercus)
  {
    introspectConstants(quercus, _type);
    introspectMethods(quercus, _type);

    _marshall = new JavaMarshall(this, false);

    Constructor []cons = _type.getConstructors();

    if (cons.length > 0)
      _cons = new JavaConstructor(_quercus, cons[0]);
    else
      _cons = null;

    try {
      Method method = _type.getMethod("iterator", new Class[0]);

      if (method != null &&
	  Iterator.class.isAssignableFrom(method.getReturnType()))
	_iterator = method;
    } catch (Throwable e) {
    }
  }

  /**
   * Introspects the Java class.
   */
  private void introspectConstants(Quercus quercus, Class type)
  {
    if (type == null || type.equals(Object.class))
      return;

    if (! Modifier.isPublic(type.getModifiers()))
      return;
    
    Class []ifcs = type.getInterfaces();
    
    for (int i = 0; i < ifcs.length; i++) {
      introspectConstants(quercus, ifcs[i]);
    }

    Field []fields = type.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];

      if (_constMap.get(field.getName()) != null)
	continue;
      else if (! Modifier.isPublic(field.getModifiers()))
	continue;
      else if (! Modifier.isStatic(field.getModifiers()))
	continue;
      else if (! Modifier.isFinal(field.getModifiers()))
	continue;

      try {
	Value value = Quercus.objectToValue(field.get(null));

	if (value != null)
	  _constMap.put(field.getName(), value);
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    introspectConstants(quercus, type.getSuperclass());
  }

  /**
   * Introspects the Java class.
   */
  private void introspectMethods(Quercus quercus, Class type)
  {
    if (type == null || type.equals(Object.class))
      return;
    
    Class []ifcs = type.getInterfaces();
    
    for (int i = 0; i < ifcs.length; i++) {
      introspectMethods(quercus, ifcs[i]);
    }

    Method []methods = type.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (_functionMap.get(method.getName()) != null)
	continue;
      else if (! Modifier.isPublic(method.getModifiers()))
	continue;

      JavaMethod javaMethod = new JavaMethod(quercus, method);

      _functionMap.put(method.getName(), javaMethod);
    }

    introspectMethods(quercus, type.getSuperclass());
  }
}

