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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

import java.io.IOException;
import java.net.URL;

import java.util.*;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.program.AbstractFunction;

/**
 * Represents a PHP variable value.
 */
public class JavaAdapterVar extends Var
{
  
  private JavaAdapter _adapter;
  private Value _key;

  public JavaAdapterVar(JavaAdapter adapter, Value key)
  {
    _adapter = adapter;
    _key = key;
  }

  public Value getValue()
  {
    return _adapter.get(_key);
  }
  
  public void setValue(Value value)
  {
    _adapter.putImpl(_key, value);
  }
  
  /**
   * Sets the value.
   */
  public Value set(Value value)
  {
    setRaw(getValue());
    
    value = super.set(value);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return getValue().getType();
  }

  /**
   * Returns true for an object.
   */
  public boolean isObject()
  {
    return getValue().isObject();
  }

  /**
   * Returns true for a set type.
   */
  public boolean isset()
  {
    return getValue().isset();
  }

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return getValue().isA(name);
  }

  /**
   * True for a number
   */
  public boolean isNull()
  {
    return getValue().isNull();
  }

  /**
   * True for a long
   */
  public boolean isLongConvertible()
  {
    return getValue().isLongConvertible();
  }

  /**
   * True to a double.
   */
  public boolean isDoubleConvertible()
  {
    return getValue().isDoubleConvertible();
  }

  /**
   * True for a number
   */
  public boolean isNumberConvertible()
  {
    return getValue().isNumberConvertible();
  }

  /**
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    return getValue().isNumeric();
  }

  /**
   * Returns true for a scalar
   */
  /*
  public boolean isScalar()
  {
    return getValue().isScalar();
  }
  */

  /**
   * Returns true for a StringValue.
   */
  public boolean isString()
  {
    return getValue().isString();
  }

  /**
   * Returns true for a BinaryValue.
   */
  public boolean isBinary()
  {
    return getValue().isBinary();
  }

  /**
   * Returns true for a UnicodeValue.
   */
  public boolean isUnicode()
  {
    return getValue().isUnicode();
  }

  //
  // Conversions
  //

  public String toString()
  {
    return getValue().toString();
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return getValue().toBoolean();
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return getValue().toLong();
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return getValue().toDouble();
  }

  /**
   * Converts to a string.
   * @param env
   */
  public StringValue toString(Env env)
  {
    return getValue().toString(env);
  }

  /**
   * Converts to an object.
   */
  public Object toJavaObject()
  {
    return getValue().toJavaObject();
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject(Env env, Class type)
  {
    return getValue().toJavaObject(env, type);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    return getValue().toJavaObjectNotNull(env, type);
  }

  /**
   * Converts to a java Collection object.
   */
  public Collection toJavaCollection(Env env, Class type)
  {
    return getValue().toJavaCollection(env, type);
  }
  
  /**
   * Converts to a java List object.
   */
  public List toJavaList(Env env, Class type)
  {
    return getValue().toJavaList(env, type);
  }
  
  /**
   * Converts to a java Map object.
   */
  public Map toJavaMap(Env env, Class type)
  {
    return getValue().toJavaMap(env, type);
  }


  /**
   * Converts to an array
   */
  public Value toArray()
  {
    return getValue().toArray();
  }

  /**
   * Converts to an array
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return getValue().toArrayValue(env);
  }

  /**
   * Converts to an array
   */
  public Value toAutoArray()
  {
    setRaw(getValue());
    
    Value value = super.toAutoArray();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return getValue().toObject(env);
  }

  /**
   * Converts to a Java Calendar.
   */
  @Override
  public Calendar toJavaCalendar()
  {
    return getValue().toJavaCalendar();
  }
  
  /**
   * Converts to a Java Date.
   */
  @Override
  public Date toJavaDate()
  {
    return getValue().toJavaDate();
  }
  
  /**
   * Converts to a Java URL.
   */
  @Override
  public URL toJavaURL(Env env)
  {
    return getValue().toJavaURL(env);
  }
  
  /**
   * Append to a string builder.
   */
  public void appendTo(StringBuilderValue sb)
  {
    getValue().appendTo(sb);
  }

  /**
   * Append to a string builder.
   */
  public void appendTo(BinaryBuilderValue sb)
  {
    getValue().appendTo(sb);
  }

  /**
   * Converts to a raw value.
   */
  public Value toValue()
  {
    return getValue();
  }

  /**
   * Converts to a function argument value that is never assigned or modified.
   */
  @Override
  public Value toArgValueReadOnly()
  {
    return getValue();
  }

  /**
   * Converts to a raw value.
   */
  @Override
  public Value toArgValue()
  {
    return getValue().toArgValue();
  }

  /**
   * Converts to a function argument ref value, i.e. an argument
   * declared as a reference, but not assigned
   */
  @Override
  public Value toRefValue()
  {
    setRaw(getValue());
    
    Value value = super.toRefValue();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a variable
   */
  @Override
  public Var toVar()
  {
    setRaw(getValue());
    
    Var value = super.toVar();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return getValue().toKey();
  }

  @Override
  public StringValue toStringValue()
  {
    return getValue().toStringValue();
  }

  @Override
  public BinaryValue toBinaryValue(Env env)
  {
    return getValue().toBinaryValue(env);
  }

  @Override
  public UnicodeValue toUnicodeValue(Env env)
  {
    return getValue().toUnicodeValue(env);
  }

  @Override
  public StringValue toStringBuilder()
  {
    return getValue().toStringBuilder();
  }

  //
  // Operations
  //

  /**
   * Copy the value.
   */
  public Value copy()
  {
    setRaw(getValue());
    
    Value value = super.copy();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Copy the value as a return value.
   */
  public Value copyReturn()
  {
    setRaw(getValue());
    
    Value value = super.copyReturn();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Converts to a variable reference (for function  arguments)
   */
  public Value toRef()
  {
    setRaw(getValue());
    
    Value value = super.toRef();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return getValue().isArray();
  }

  /**
   * Negates the value.
   */
  public Value neg()
  {
    return getValue().neg();
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
  {
    return getValue().add(rValue);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long rValue)
  {
    return getValue().add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
  {
    setRaw(getValue());
    
    Value value = super.preincr(incr);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
  {
    setRaw(getValue());
    
    Value value = super.postincr(incr);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
  {
    return getValue().sub(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
  {
    return getValue().mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long lValue)
  {
    return getValue().mul(lValue);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
  {
    return getValue().div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
  {
    return getValue().lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
  {
    return getValue().rshift(rValue);
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    return getValue().eq(rValue);
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    return getValue().eql(rValue);
  }

  /**
   * Compares the two values
   */
  public int cmp(Value rValue)
  {
    return getValue().cmp(rValue);
  }

  /**
   * Returns the array/object size
   */
  public int getSize()
  {
    return getValue().getSize();
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    return getValue().getIndices();
  }

  /**
   * Returns the array keys.
   */
  public Value []getKeyArray(Env env)
  {
    return getValue().getKeyArray(env);
  }

  /**
   * Returns the array values.
   */
  public Value []getValueArray(Env env)
  {
    return getValue().getValueArray(env);
  }

  /**
   * Returns the array ref.
   */
  public Value getArray()
  {
    setRaw(getValue());
    
    Value value = super.getArray();
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getObject(Env env)
  {
    setRaw(getValue());
    
    Value value = super.getObject(env);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  public Value get(Value index)
  {
    return getValue().get(index);
  }

  /**
   * Returns the array ref.
   */
  public Value getRef(Value index)
  {
    setRaw(getValue());
    
    Value value = super.getRef(index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  public Value getArg(Value index)
  {
    setRaw(getValue());
    
    Value value = super.getArg(index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getArray(Value index)
  {
    setRaw(getValue());
    
    Value value = super.getArray(index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the value, doing a copy-on-write if needed.
   */
  public Value getDirty(Value index)
  {
    return getValue().getDirty(index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  public Value getObject(Env env, Value index)
  {
    setRaw(getValue());
    
    Value value = super.getObject(env, index);
    
    setValue(getRawValue());
    
    return value;
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.put(index, value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the array ref.
   */
  public Value put(Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.put(value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the array ref.
   */
  public Value putRef()
  {
    setRaw(getValue());
    
    Value retValue = super.putRef();
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Return unset the value.
   */
  public Value remove(Value index)
  {
    return getValue().remove(index);
  }

  /**
   * Returns the field ref.
   */
  @Override
  public Value getField(Env env, String index)
  {
    return getValue().getField(env, index);
  }

  /**
   * Returns the field ref.
   */
  public Value getFieldRef(Env env, String index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldRef(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the array ref.
   */
  public Value getFieldArg(Env env, String index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldArg(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the field value as an array
   */
  public Value getFieldArray(Env env, String index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldArray(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Returns the field value as an object
   */
  public Value getFieldObject(Env env, String index)
  {
    setRaw(getValue());
    
    Value retValue = super.getFieldObject(env, index);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Sets the field.
   */
  public Value putField(Env env, String index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.putField(env, index, value);
    
    setValue(getRawValue());
    
    return retValue;
  }

  /**
   * Sets the field.
   */
  public Value putThisField(Env env, String index, Value value)
  {
    setRaw(getValue());
    
    Value retValue = super.putThisField(env, index, value);
    
    setValue(getRawValue());

    return retValue;
  }
  
  /**
   * Unsets the field.
   */
  public void removeField(String index)
  {
    getValue().removeField(index);
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  public Object valuesToArray(Env env, Class elementType)
  {
    return getValue().valuesToArray(env, elementType);
  }
  
  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    return getValue().charValueAt(index);
  }

  /**
   * Sets the character at an index
   */
  @Override
  public Value setCharValueAt(long index, String value)
  {
    return getValue().setCharValueAt(index, value);
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return getValue().hasCurrent();
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    return getValue().key();
  }

  /**
   * Returns the current value
   */
  public Value current()
  {
    return getValue().current();
  }

  /**
   * Returns the current value
   */
  public Value next()
  {
    return getValue().next();
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Expr []args)
  {
    return getValue().callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value []args)
  {
    return getValue().callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen)
  {
    return getValue().callMethod(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0)
  {
    return getValue().callMethod(env, hash, name, nameLen, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1)
  {
    return getValue().callMethod(env, hash, name, nameLen, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2)
  {
    return getValue().callMethod(env, hash, name, nameLen, a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return getValue().callMethod(env, hash, name, nameLen,
                                 a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return getValue().callMethod(env, hash, name, nameLen,
                                 a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Expr []args)
  {
    return getValue().callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    return getValue().callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return getValue().callMethodRef(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0)
  {
    return getValue().callMethodRef(env, hash, name, nameLen, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1)
  {
    return getValue().callMethodRef(env, hash, name, nameLen,
                                    a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2)
  {
    return getValue().callMethodRef(env, hash, name, nameLen,
                                    a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return getValue().callMethodRef(env, hash, name, nameLen,
                                    a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return getValue().callMethodRef(env, hash, name, nameLen,
                                    a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return getValue().callClassMethod(env, fun, args);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    getValue().print(env);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    getValue().serialize(sb);
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("&");
    getValue().varDump(env, out, depth, valueSet);
  }
  
  //
  // Java Serialization
  //
  
  public Object writeReplace()
  {
    return getValue();
  }
}

