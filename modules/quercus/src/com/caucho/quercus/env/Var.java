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

import java.io.IOException;

import java.util.*;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.program.AbstractFunction;

/**
 * Represents a PHP variable value.
 */
public class Var extends Value {
  Value _value;
  private int _refCount;

  public Var()
  {
    _value = NullValue.NULL;
  }

  public Var(Value value)
  {
    _value = value;
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
  public boolean isLongConvertible()
  {
    return _value.isLongConvertible();
  }

  /**
   * True to a double.
   */
  public boolean isDoubleConvertible()
  {
    return _value.isDoubleConvertible();
  }

  /**
   * True for a number
   */
  public boolean isNumberConvertible()
  {
    return _value.isNumberConvertible();
  }

  /**
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    return _value.isNumeric();
  }

  /**
   * Returns true for a scalar
   */
  /*
  public boolean isScalar()
  {
    return _value.isScalar();
  }
  */

  /**
   * Returns true for a StringValue.
   */
  public boolean isString()
  {
    return _value.isString();
  }

  /**
   * Returns true for a BinaryValue.
   */
  public boolean isBinary()
  {
    return _value.isBinary();
  }

  /**
   * Returns true for a UnicodeValue.
   */
  public boolean isUnicode()
  {
    return _value.isUnicode();
  }

  //
  // Conversions
  //

  public String toString()
  {
    return _value.toString();
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
  public StringValue toString(Env env)
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
  @Override
  public Object toJavaObject(Env env, Class type)
  {
    return _value.toJavaObject(env, type);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    return _value.toJavaObjectNotNull(env, type);
  }

  /**
   * Converts to a java map.
   */
  public Map toJavaMap(Env env, Class type)
  {
    return _value.toJavaMap(env, type);
  }


  /**
   * Converts to an array
   */
  public Value toArray()
  {
    return _value.toArray();
  }

  /**
   * Converts to an array
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return _value.toArrayValue(env);
  }

  /**
   * Converts to an array
   */
  public Value toAutoArray()
  {
    _value = _value.toAutoArray();
    
    return _value;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return _value.toObject(env);
  }

  /**
   * Append to a string builder.
   */
  public void appendTo(StringBuilderValue sb)
  {
    _value.appendTo(sb);
  }

  /**
   * Append to a string builder.
   */
  public void appendTo(BinaryBuilderValue sb)
  {
    _value.appendTo(sb);
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
   * Converts to a function argument value that is never assigned or modified.
   */
  @Override
  public Value toArgValueReadOnly()
  {
    return _value;
  }

  /**
   * Converts to a raw value.
   */
  @Override
  public Value toArgValue()
  {
    return _value.toArgValue();
  }

  /**
   * Converts to a function argument ref value, i.e. an argument
   * declared as a reference, but not assigned
   */
  @Override
  public Value toRefValue()
  {
    // php/344r
    return _value.toRefValue();
  }

  /**
   * Converts to a variable
   */
  @Override
  public Var toVar()
  {
    // php/3d04
    return new Var(_value.toArgValue());
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

  @Override
  public StringValue toStringValue()
  {
    return _value.toStringValue();
  }

  @Override
  public BinaryValue toBinaryValue(Env env)
  {
    return _value.toBinaryValue(env);
  }

  @Override
  public UnicodeValue toUnicodeValue(Env env)
  {
    return _value.toUnicodeValue(env);
  }

  @Override
  public StringValue toStringBuilder()
  {
    return _value.toStringBuilder();
  }

  //
  // Operations
  //

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
  {
    return _value.neg();
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
  {
    return _value.add(rValue);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long rValue)
  {
    return _value.add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
  {
    _value = _value.preincr(incr);

    return _value;
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
  {
    Value value = _value;

    _value = value.postincr(incr);

    return value;
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
  {
    return _value.sub(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
  {
    return _value.mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long lValue)
  {
    return _value.mul(lValue);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
  {
    return _value.div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
  {
    return _value.lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
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
  {
    return _value.eql(rValue);
  }

  /**
   * Compares the two values
   */
  public int cmp(Value rValue)
  {
    return _value.cmp(rValue);
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
  public Value getArray()
  {
    if (! _value.isset())
      _value = new ArrayValueImpl();

    return _value;
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
  public Value get(Value index)
  {
    return _value.get(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getRef(Value index)
  {
    // php/3d1a
    if (! _value.isset())
      _value = new ArrayValueImpl();

    return _value.getRef(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getArg(Value index)
  {
    if (_value.isset())
      return _value.getArg(index);
    else
      return new ArgGetValue(this, index); // php/3d2p
  }

  /**
   * Returns the array ref.
   */
  public Value getArgRef(Value index)
  {
    if (_value.isset())
      return _value.getArgRef(index);
    else
      return new ArgGetValue(this, index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getArray(Value index)
  {
    // php/3d11
    _value = _value.toAutoArray();

    return _value.getArray(index);
  }

  /**
   * Returns the value, doing a copy-on-write if needed.
   */
  public Value getDirty(Value index)
  {
    return _value.getDirty(index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getObject(Env env, Value index)
  {
    // php/3d2p
    _value = _value.toAutoArray();

    return _value.getObject(env, index);
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value index, Value value)
  {
    _value = _value.toAutoArray();
    
    return _value.put(index, value);
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value value)
  {
    _value = _value.toAutoArray();
    
    return _value.put(value);
  }

  /**
   * Returns the array ref.
   */
  public Value putRef()
  {
    _value = _value.toAutoArray();
    
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
   * Returns the field ref.
   */
  @Override
  public Value getField(Env env, String index)
  {
    return _value.getField(env, index);
  }

  /**
   * Returns the field ref.
   */
  public Value getFieldRef(Env env, String index)
  {
    // php/3a0r
    _value = _value.toAutoObject(env);
    
    return _value.getFieldRef(env, index);
  }

  /**
   * Returns the array ref.
   */
  public Value getFieldArg(Env env, String index)
  {
    if (_value.isset())
      return _value.getFieldArg(env, index);
    else
      return new ArgGetFieldValue(env, this, index);
  }

  /**
   * Returns the field value as an array
   */
  public Value getFieldArray(Env env, String index)
  {
    // php/3d1q
    _value = _value.toAutoObject(env);
    
    return _value.getFieldArray(env, index);
  }

  /**
   * Returns the field value as an object
   */
  public Value getFieldObject(Env env, String index)
  {
    _value = _value.toAutoObject(env);
    
    return _value.getFieldObject(env, index);
  }

  /**
   * Sets the field.
   */
  public Value putField(Env env, String index, Value value)
  {
    // php/3a0s
    _value = _value.toAutoObject(env);

    return _value.putField(env, index, value);
  }

  /**
   * Unsets the field.
   */
  public void removeField(String index)
  {
    _value.removeField(index);
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    return _value.charValueAt(index);
  }

  /**
   * Sets the character at an index
   */
  @Override
  public Value setCharValueAt(long index, String value)
  {
    return _value.setCharValueAt(index, value);
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
  public Value callMethod(Env env, String methodName, Expr []args)
  {
    return _value.callMethod(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value []args)
  {
    return _value.callMethod(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName)
  {
    return _value.callMethod(env, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a0)
  {
    return _value.callMethod(env, methodName, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a0, Value a1)
  {
    return _value.callMethod(env, methodName, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2)
  {
    return _value.callMethod(env, methodName, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return _value.callMethod(env, methodName, a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _value.callMethod(env, methodName, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Expr []args)
  {
    return _value.callMethodRef(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value []args)
  {
    return _value.callMethodRef(env, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName)
  {
    return _value.callMethodRef(env, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a0)
  {
    return _value.callMethodRef(env, methodName, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a0, Value a1)
  {
    return _value.callMethodRef(env, methodName, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2)
  {
    return _value.callMethodRef(env, methodName, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return _value.callMethodRef(env, methodName, a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _value.callMethodRef(env, methodName, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return _value.callClassMethod(env, fun, args);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
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

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("&");
    _value.varDump(env, out, depth, valueSet);
  }
}

