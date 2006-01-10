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

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.expr.Expr;

/**
 * Represents a PHP object value.
 */
public class ObjectValue extends ArrayValueWrapper {
  private static final StringValue TO_STRING = new StringValue("__toString");
  
  private final QuercusClass _cl;

  public ObjectValue(QuercusClass cl)
  {
    super(new ArrayValueImpl());
    _cl = cl;

    
    // _cl.initFields(_map);
  }

  public ObjectValue(Env env, IdentityHashMap<Value,Value> map,
		     QuercusClass cl, ArrayValue oldValue)
  {
    super(new ArrayValueImpl(env, map, oldValue));
    _cl = cl;
    
    // _cl.initFields(_map);
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _cl.getName();
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
   * Returns true for an object.
   */
  public boolean isObject()
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
   * Finds the method name.
   */
  public AbstractFunction findFunction(String methodName)
  {
    return _cl.findFunction(methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Expr []args)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value []args)
    throws Throwable
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.evalMethod(env, this, args);
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
			   _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this, a0, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this,
						  a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this,
						  a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethod(env, this,
						  a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Expr []args)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Value []args)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Value a0)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this, a0, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this,
						  a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this,
						  a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return _cl.getFunction(methodName).evalMethodRef(env, this,
						  a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value evalClassMethod(Env env, AbstractFunction fun, Value []args)
    throws Throwable
  {
    Value oldThis = env.getThis();

    try {
      env.setThis(this);
	
      return fun.eval(env, args);
    } finally {
      env.setThis(oldThis);
    }
  }

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  public Value getObject(Env env)
  {
    return this;
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return this;
  }
  
  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    Value oldValue = map.get(this);

    if (oldValue != null)
      return oldValue;

    return new ObjectValue(env, map, _cl, getArray());
  }
  
  /**
   * Clone the object
   */
  public Value clone()
  {
    ObjectValue newObject = new ObjectValue(_cl);

    for (ArrayValue.Entry ptr = getHead(); ptr != null; ptr = ptr.getNext())
      newObject.put(ptr.getKey(), ptr.getValue());
    
    return newObject;
  }

  // XXX: need to check the other copy, e.g. for sessions

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("O:");
    sb.append(_cl.getName().length());
    sb.append(":\"");
    sb.append(_cl.getName());
    sb.append("\":");
    sb.append(getSize());
    sb.append(":{");

    for (Map.Entry<Value,Value> entry : entrySet()) {
      entry.getKey().serialize(sb);
      entry.getValue().serialize(sb);
    }
    
    sb.append("}");
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
    throws Throwable
  {
    AbstractFunction fun = _cl.findFunction("__toString");

    if (fun != null)
      return fun.evalMethod(env, this, new Expr[0]).toString(env);
    else
      return "Object id #1";
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    env.getOut().print(toString(env));
  }
  
  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return this;
  }
  
  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return this;
  }

  public String toString()
  {
    return "ObjectValue@" + System.identityHashCode(this) + "[" + _cl.getName() + "]";
  }
}

