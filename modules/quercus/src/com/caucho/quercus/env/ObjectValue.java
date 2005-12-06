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
import java.util.Map;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.expr.Expr;

/**
 * Represents a PHP object value.
 */
public class ObjectValue extends Value {
  private static final StringValue TO_STRING = new StringValue("__toString");
  
  private final PhpClass _cl;

  private final ArrayValue _map = new ArrayValueImpl();

  public ObjectValue(PhpClass cl)
  {
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
   * Returns the indices
   */
  public Collection<Value> getIndices()
  {
    return _map.getIndices();
  }

  /**
   * Returns the field value.
   */
  public Value get(Value fieldName)
  {
    Value value = _map.get(fieldName);
    
    return value;
  }

  /**
   * Returns a reference to the field value.
   */
  public Value getArgRef(Value fieldName)
  {
    return _map.getArgRef(fieldName);
  }

  /**
   * Returns a reference to the field value.
   */
  public Value getRef(Value fieldName)
  {
    return _map.getRef(fieldName);
  }

  /**
   * Returns the value as an array.
   */
  public Value getArray(Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = new ArrayValueImpl();

      put(fieldName, value);
    }
    
    return value;
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
   * Returns the field value, creating an object if it's unset.
   */
  public Value getObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = env.createObject();

      put(fieldName, value);
    }
    
    return value;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    // quercus/3d9d
    
    /*
    Value value = get(index);

    if (value.isset())
      return value;
    else {
      return new ArgGetValue(this, index);
    }
    */
    return _map.getArg(index);
  }
  
  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    return _map.put(key, value);
  }

  /**
   * Removes the field value.
   */
  public Value remove(Value fieldName)
  {
    Value value = _map.remove(fieldName);
    
    return value;
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    return _map.getSize();
  }

  /**
   * Returns an iterator of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return _map.entrySet();
  }

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
    
    sb.append("};");
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

