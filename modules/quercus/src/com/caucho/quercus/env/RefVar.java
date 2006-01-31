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
import java.util.IdentityHashMap;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.program.AbstractFunction;

/**
 * Represents a reference to a PHP variable in a function call.
 */
public class RefVar extends Value {
  private Var _var;

  public RefVar(Var var)
  {
    _var = var;
  }

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return _var.isA(name);
  }

  /**
   * True for a long
   */
  public boolean isLong()
  {
    return _var.isLong();
  }

  /**
   * True to a double.
   */
  public boolean isDouble()
  {
    return _var.isDouble();
  }

  /**
   * True for a number
   */
  public boolean isNumber()
  {
    return _var.isNumber();
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return _var.isScalar();
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _var.toBoolean();
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _var.toLong();
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _var.toDouble();
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
    throws Throwable
  {
    return _var.toString(env);
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return _var.toObject(env);
  }

  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return _var.toJavaObject();
  }

  /**
   * Converts to a raw value.
   */
  public Value toValue()
  {
    return _var.toValue();
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return _var.isArray();
  }

  /**
   * Copy the value.
   */
  public Value copy()
  {
    // quercus/0d05
    return this;
  }

  /**
   * Converts to an argument value.
   */
  public Value toArgValue()
  {
    // php/343k
    return _var;
  }

  /**
   * Converts to a variable
   */
  public Var toVar()
  {
    return _var;
  }

  /**
   * Converts to a reference variable
   */
  public Var toRefVar()
  {
    return _var;
  }

  /**
   * Negates the value.
   */
  public Value neg()
    throws Throwable
  {
    return _var.neg();
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
    throws Throwable
  {
    return _var.add(rValue);
  }

  /**
   * Adds to the following value.
   */
  public Value add(long rValue)
    throws Throwable
  {
    return _var.add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
    throws Throwable
  {
    return _var.preincr(incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
    throws Throwable
  {
    return _var.postincr(incr);
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
    throws Throwable
  {
    return _var.sub(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
    throws Throwable
  {
    return _var.mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long lValue)
    throws Throwable
  {
    return _var.mul(lValue);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
    throws Throwable
  {
    return _var.div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
    throws Throwable
  {
    return _var.lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
    throws Throwable
  {
    return _var.rshift(rValue);
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue, Env env)
    throws Throwable
  {
    return _var.equals(rValue.toValue());
  }

  /**
   * Returns the array/object size
   */
  public int getSize()
  {
    return _var.getSize();
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    return _var.getIndices();
  }

  /**
   * Returns the array keys.
   */
  public Value []getKeyArray()
  {
    return _var.getKeyArray();
  }

  /**
   * Returns the array values.
   */
  public Value []getValueArray(Env env)
  {
    return _var.getValueArray(env);
  }

  /**
   * Returns the array ref.
   */
  public Value get(Value index)
  {
    return _var.get(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getRef(Value index)
  {
    return _var.getRef(index);
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value index, Value value)
  {
    return _var.put(index, value);
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value value)
  {
    return _var.put(value);
  }

  /**
   * Returns the character at an index
   */
  /* XXX: need test first
  public Value charAt(long index)
  {
    return _ref.charAt(index);
  }
  */

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value []args)
    throws Throwable
  {
    return _var.evalMethod(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    return _var.evalMethod(env, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    return _var.evalMethod(env, methodName, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value evalClassMethod(Env env, AbstractFunction fun, Value []args)
    throws Throwable
  {
    return _var.evalClassMethod(env, fun, args);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    _var.print(env);
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value,String> valueSet)
    throws Throwable
  {
    out.print("&");
    toValue().varDumpImpl(env, out, depth, valueSet);
  }
}

