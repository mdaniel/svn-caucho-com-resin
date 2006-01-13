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

import java.util.Collection;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.program.AbstractFunction;

/**
 * Represents a PHP variable value.
 */
public class Var extends Value {
  private Value _value = NullValue.NULL;
  private int _refCount;

  public Var()
  {
  }

  public Var(Value value)
  {
    _value = value.toValue();
  }

  /**
   * Adds a reference.
   */
  public void setReference()
  {
    _refCount = 1;
  }

  /**
   * Sets as a global variable
   */
  public void setGlobal()
  {
    _refCount = 1;
  }
  
  /**
   * Sets the value.
   */
  public Value set(Value value)
  {
    _value = value.toValue();

    return _value;
  }
  
  /**
   * Sets the value.
   */
  protected Value setRaw(Value value)
  {
    // quercus/0431
    _value = value;

    if (value instanceof ArrayValue.Entry)
      throw new IllegalStateException();

    return _value;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return _value.getType();
  }

  /**
   * Returns true for a set type.
   */
  public boolean isset()
  {
    return _value.isset();
  }

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return _value.isA(name);
  }
  
  /**
   * True for a number
   */
  public boolean isNull()
  {
    return _value.isNull();
  }
  
  /**
   * True for a long
   */
  public boolean isLong()
  {
    return _value.isLong();
  }
  
  /**
   * True to a double.
   */
  public boolean isDouble()
  {
    return _value.isDouble();
  }
  
  /**
   * True for a number
   */
  public boolean isNumber()
  {
    return _value.isNumber();
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return _value.isScalar();
  }
  
  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _value.toBoolean();
  }
  
  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _value.toLong();
  }
  
  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _value.toDouble();
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
    throws Throwable
  {
    return _value.toString(env);
  }
  
  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return _value.toJavaObject();
  }
  
  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return _value.toObject(env);
  }
  
  /**
   * Returns to the value value.
   */
  public final Value getRawValue()
  {
    return _value;
  }
  
  /**
   * Converts to a raw value.
   */
  public Value toValue()
  {
    return _value;
  }
  
  /**
   * Converts to a raw value.
   */
  public Value toArgValue()
  {
    return _value.toArgValue();
  }
  
  /**
   * Converts to a function argument ref value.
   */
  public Value toRefValue()
  {
    // php/344r
    return _value;
  }
  
  /**
   * Converts to a variable
   */
  public Var toVar()
  {
    // php/3d04
    return new Var(toArgValue());
  }
  
  /**
   * Converts to a reference variable
   */
  public Var toRefVar()
  {
    _refCount = 2;

    return this;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return _value.toKey();
  }
  
  /**
   * Copy the value.
   */
  public Value copy()
  {
    // php/041d
    return _value.copy();
  }
  
  /**
   * Copy the value as an array item.
   */
  public Value copyArrayItem()
  {
    _refCount = 2;
    
    // php/041d
    return this;
  }
  
  /**
   * Copy the value as a return value.
   */
  public Value copyReturn()
  {
    if (_refCount < 1)
      return _value;
    else
      return _value.copy();
  }
  
  /**
   * Converts to a variable reference (for function  arguments)
   */
  public Value toRef()
  {
    _refCount = 2;
    
    return new RefVar(this);
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return _value.isArray();
  }

  /**
   * Negates the value.
   */
  public Value neg()
    throws Throwable
  {
    return _value.neg();
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
    throws Throwable
  {
    return _value.add(rValue);
  }

  /**
   * Adds to the following value.
   */
  public Value add(long rValue)
    throws Throwable
  {
    return _value.add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
    throws Throwable
  {
    _value = _value.preincr(incr);
    
    return _value;
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
    throws Throwable
  {
    Value value = _value;

    _value = value.postincr(incr);
    
    return value;
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
    throws Throwable
  {
    return _value.sub(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
    throws Throwable
  {
    return _value.mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long lValue)
    throws Throwable
  {
    return _value.mul(lValue);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
    throws Throwable
  {
    return _value.div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
    throws Throwable
  {
    return _value.lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
    throws Throwable
  {
    return _value.rshift(rValue);
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    return _value.eq(rValue);
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
    throws Throwable
  {
    return _value.eql(rValue);
  }

  /**
   * Returns the array/object size
   */
  public int getSize()
  {
    return _value.getSize();
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    return _value.getIndices();
  }

  /**
   * Returns the array keys.
   */
  public Value []getKeyArray()
  {
    return _value.getKeyArray();
  }

  /**
   * Returns the array values.
   */
  public Value []getValueArray(Env env)
  {
    return _value.getValueArray(env);
  }

  /**
   * Returns the array ref.
   */
  public Value get(Value index)
  {
    return _value.get(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getRef(Value index)
  {
    return _value.getRef(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getArray()
  {
    if (! _value.isset())
      _value = new ArrayValueImpl();
    
    return _value;
  }

  /**
   * Returns the array ref.
   */
  public Value getArgArray()
  {
    if (_value.isset())
      return _value;
    else {
      // quercus/3d52
      return new ArgArrayVarValue(this);
    }
  }

  /**
   * Returns the array ref.
   */
  public Value getArgObject(Env env)
  {
    if (_value.isset())
      return _value;
    else
      return new ArgObjectVarValue(this, env);
  }

  /**
   * Returns the array ref.
   */
  public Value getArgRef(Value index)
  {
    return _value.getArgRef(index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getObject(Env env)
  {
    if (! _value.isset())
      _value = env.createObject();
    
    return _value;
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value index, Value value)
  {
    return _value.put(index, value);
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value value)
  {
    return _value.put(value);
  }

  /**
   * Returns the array ref.
   */
  public Value putRef()
  {
    return _value.putRef();
  }

  /**
   * Return unset the value.
   */
  public Value remove(Value index)
  {
    return _value.remove(index);
  }

  /**
   * Returns the character at an index
   */
  public Value charAt(long index)
  {
    return _value.charAt(index);
  }

  /**
   * Sets the character at an index
   */
  public Value setCharAt(long index, String value)
  {
    return _value.setCharAt(index, value);
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return _value.hasCurrent();
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    return _value.key();
  }

  /**
   * Returns the current value
   */
  public Value current()
  {
    return _value.current();
  }

  /**
   * Returns the current value
   */
  public Value next()
  {
    return _value.next();
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value []args)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    return _value.evalMethod(env, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value evalMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return _value.evalMethod(env, methodName, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Value []args)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Value a0)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethodRef(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value evalMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return _value.evalMethodRef(env, methodName, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value evalClassMethod(Env env, AbstractFunction fun, Value []args)
    throws Throwable
  {
    return _value.evalClassMethod(env, fun, args);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    _value.print(env);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    _value.serialize(sb);
  }

  public String toString()
  {
    return _value.toString();
  }
}

