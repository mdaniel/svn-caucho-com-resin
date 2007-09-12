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

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;

/**
 * Represents a PHP variable value.
 */
public class Var extends Value
  implements Serializable
{
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
  @Override
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
  @Override
  public String getType()
  {
    return _value.getType();
  }

  /**
   * Returns the class name.
   */
  @Override
  public String getClassName()
  {
    return _value.getClassName();
  }
  
  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return _value.isObject();
  }

  /**
   * Returns true for a set type.
   */
  @Override
  public boolean isset()
  {
    return _value.isset();
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return _value.isA(name);
  }

  /**
   * True for a number
   */
  @Override
  public boolean isNull()
  {
    return _value.isNull();
  }

  /**
   * True for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    return _value.isLongConvertible();
  }

  /**
   * True to a double.
   */
  @Override
  public boolean isDoubleConvertible()
  {
    return _value.isDoubleConvertible();
  }

  /**
   * True for a number
   */
  @Override
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
  @Override
  public boolean isString()
  {
    return _value.isString();
  }

  /**
   * Returns true for a BinaryValue.
   */
  @Override
  public boolean isBinary()
  {
    return _value.isBinary();
  }

  /**
   * Returns true for a UnicodeValue.
   */
  @Override
  public boolean isUnicode()
  {
    return _value.isUnicode();
  }
  
  /**
   * Returns true for a BooleanValue
   */
  @Override
  public boolean isBoolean()
  {
    return _value.isBoolean();
  }
  
  /**
   * Returns true for a DefaultValue
   */
  @Override
  public boolean isDefault()
  {
    return _value.isDefault();
  }

  //
  // Conversions
  //

  @Override
  public String toString()
  {
    return _value.toString();
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return _value.toBoolean();
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return _value.toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return _value.toDouble();
  }

  /**
   * Converts to a long.
   */
  @Override
  public LongValue toLongValue()
  {
    return _value.toLongValue();
  }

  /**
   * Converts to a double.
   */
  @Override
  public DoubleValue toDoubleValue()
  {
    return _value.toDoubleValue();
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public StringValue toString(Env env)
  {
    return _value.toString(env);
  }

  /**
   * Converts to an object.
   */
  @Override
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
   * Converts to a java Collection object.
   */
  @Override
  public Collection toJavaCollection(Env env, Class type)
  {
    return _value.toJavaCollection(env, type);
  }
  
  /**
   * Converts to a java List object.
   */
  @Override
  public List toJavaList(Env env, Class type)
  {
    return _value.toJavaList(env, type);
  }

  /**
   * Converts to a java map.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    return _value.toJavaMap(env, type);
  }

  /**
   * Converts to a Java Calendar.
   */
  @Override
  public Calendar toJavaCalendar()
  {
    return _value.toJavaCalendar();
  }
  
  /**
   * Converts to a Java Date.
   */
  @Override
  public Date toJavaDate()
  {
    return _value.toJavaDate();
  }
  
  /**
   * Converts to a Java URL.
   */
  @Override
  public URL toJavaURL(Env env)
  {
    return _value.toJavaURL(env);
  }

  /**
   * Converts to an array
   */
  @Override
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
  @Override
  public Value toAutoArray()
  {
    _value = _value.toAutoArray();
    
    return _value;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return _value.toObject(env);
  }

  /**
   * Append to a unicode builder.
   */
  @Override
  public void appendTo(UnicodeBuilderValue sb)
  {
    _value.appendTo(sb);
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public void appendTo(BinaryBuilderValue sb)
  {
    _value.appendTo(sb);
  }

  /**
   * Append to a string builder.
   */
  @Override
  public void appendTo(StringBuilderValue sb)
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

  @Override
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
  @Override
  public Var toRefVar()
  {
    _refCount = 2;

    return this;
  }

  /**
   * Converts to a key.
   */
  @Override
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

  @Override
  public java.io.InputStream toInputStream()
  {
    return _value.toInputStream();
  }

  //
  // Operations
  //

  /**
   * Copy the value.
   */
  @Override
  public Value copy()
  {
    // php/041d
    return _value.copy();
  }

  /**
   * Copy the value as an array item.
   */
  @Override
  public Value copyArrayItem()
  {
    _refCount = 2;

    // php/041d
    return this;
  }

  /**
   * Copy the value as a return value.
   */
  @Override
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
  @Override
  public Value toRef()
  {
    _refCount = 2;

    return new RefVar(this);
  }

  /**
   * Returns true for an array.
   */
  @Override
  public boolean isArray()
  {
    return _value.isArray();
  }

  /**
   * Negates the value.
   */
  @Override
  public Value neg()
  {
    return _value.neg();
  }

  /**
   * Adds to the following value.
   */
  @Override
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
  @Override
  public Value preincr(int incr)
  {
    _value = _value.preincr(incr);

    return _value;
  }

  /**
   * Post-increment the following value.
   */
  @Override
  public Value postincr(int incr)
  {
    Value value = _value;

    _value = value.postincr(incr);

    return value;
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(Value rValue)
  {
    return _value.sub(rValue);
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(long rValue)
  {
    return _value.sub(rValue);
  }
  
  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(Value rValue)
  {
    return _value.mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(long lValue)
  {
    return _value.mul(lValue);
  }

  /**
   * Divides the following value.
   */
  @Override
  public Value div(Value rValue)
  {
    return _value.div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  @Override
  public Value lshift(Value rValue)
  {
    return _value.lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  @Override
  public Value rshift(Value rValue)
  {
    return _value.rshift(rValue);
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    return _value.eq(rValue);
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eql(Value rValue)
  {
    return _value.eql(rValue);
  }

  /**
   * Compares the two values
   */
  @Override
  public int cmp(Value rValue)
  {
    return _value.cmp(rValue);
  }

  /**
   * Returns true for less than
   */
  @Override
  public boolean lt(Value rValue)
  {
    // php/335h
    return _value.lt(rValue);
  }

  /**
   * Returns true for less than or equal to
   */
  @Override
  public boolean leq(Value rValue)
  {
    // php/335h
    return _value.leq(rValue);
  }

  /**
   * Returns true for greater than
   */
  @Override
  public boolean gt(Value rValue)
  {
    // php/335h
    return _value.gt(rValue);
  }

  /**
   * Returns true for greater than or equal to
   */
  @Override
  public boolean geq(Value rValue)
  {
    // php/335h
    return _value.geq(rValue);
  }

  /**
   * Returns the length as a string.
   */
  @Override
  public int length()
  {
    return _value.length();
  }
  
  /**
   * Returns the array/object size
   */
  @Override
  public int getSize()
  {
    return _value.getSize();
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return _value.getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return _value.getKeyIterator(env);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return _value.getValueIterator(env);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value getArray()
  {
    if (! _value.isset())
      _value = new ArrayValueImpl();

    return _value;
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getObject(Env env)
  {
    if (! _value.isset())
      _value = env.createObject();

    return _value;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value get(Value index)
  {
    return _value.get(index);
  }

  /**
   * Returns the array ref.
   */
  @Override
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
  @Override
  public Value getArg(Value index)
  {
    // php/0921, php/3921
    
    if (_value.isset())
      return _value.getArg(index);
    else
      return new ArgGetValue(this, index); // php/3d2p
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getArray(Value index)
  {
    // php/3d11
    _value = _value.toAutoArray();

    return _value.getArray(index);
  }

  /**
   * Returns the value, doing a copy-on-write if needed.
   */
  @Override
  public Value getDirty(Value index)
  {
    return _value.getDirty(index);
  }

  /**
   * Returns the value, creating an object if unset.
   */
  @Override
  public Value getObject(Env env, Value index)
  {
    // php/3d2p
    _value = _value.toAutoArray();

    return _value.getObject(env, index);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value index, Value value)
  {
    _value = _value.toAutoArray();
    
    return _value.put(index, value);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value value)
  {
    _value = _value.toAutoArray();
    
    return _value.put(value);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value putRef()
  {
    _value = _value.toAutoArray();
    
    return _value.putRef();
  }

  /**
   * Return unset the value.
   */
  @Override
  public Value remove(Value index)
  {
    return _value.remove(index);
  }

  /**
   * Returns the field ref.
   */
  @Override
  public Value getField(Env env, String index, boolean create)
  {
    return _value.getField(env, index, create);
  }

  /**
   * Returns the field ref.
   */
  @Override
  public Value getFieldRef(Env env, String index)
  {
    // php/3a0r
    _value = _value.toAutoObject(env);
    
    return _value.getFieldRef(env, index);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value getFieldArg(Env env, String index)
  {
    if (_value.isset())
      return _value.getFieldArg(env, index);
    else {
      // php/3d1q
      return new ArgGetFieldValue(env, this, index);
    }
  }

  /**
   * Returns the field value as an array
   */
  @Override
  public Value getFieldArray(Env env, String index)
  {
    // php/3d1q
    _value = _value.toAutoObject(env);
    
    return _value.getFieldArray(env, index);
  }

  /**
   * Returns the field value as an object
   */
  @Override
  public Value getFieldObject(Env env, String index)
  {
    _value = _value.toAutoObject(env);
    
    return _value.getFieldObject(env, index);
  }

  /**
   * Sets the field.
   */
  @Override
  public Value putField(Env env, String index, Value value)
  {
    // php/3a0s
    _value = _value.toAutoObject(env);

    return _value.putField(env, index, value);
  }

  /**
   * Sets the field.
   */
  @Override
  public Value putThisField(Env env, String index, Value value)
  {
    _value = _value.toAutoObject(env);
    
    return _value.putThisField(env, index, value);
  }
  
  /**
   * Unsets the field.
   */
  @Override
  public void removeField(String index)
  {
    _value.removeField(index);
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, Class elementType)
  {
    return _value.valuesToArray(env, elementType);
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
  @Override
  public boolean hasCurrent()
  {
    return _value.hasCurrent();
  }

  /**
   * Returns the current key
   */
  @Override
  public Value key()
  {
    return _value.key();
  }

  /**
   * Returns the current value
   */
  @Override
  public Value current()
  {
    return _value.current();
  }

  /**
   * Returns the current value
   */
  @Override
  public Value next()
  {
    return _value.next();
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Expr []args)
  {
    return _value.callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value []args)
  {
    return _value.callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen)
  {
    return _value.callMethod(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value a0)
  {
    return _value.callMethod(env, hash, name, nameLen, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value a0, Value a1)
  {
    return _value.callMethod(env, hash, name, nameLen,
                             a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2)
  {
    return _value.callMethod(env, hash, name, nameLen,
                             a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return _value.callMethod(env, hash, name, nameLen,
                             a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _value.callMethod(env, hash, name, nameLen,
                             a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Expr []args)
  {
    return _value.callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    return _value.callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return _value.callMethodRef(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value a0)
  {
    return _value.callMethodRef(env, hash, name, nameLen,
                                a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value a0, Value a1)
  {
    return _value.callMethodRef(env, hash, name, nameLen, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2)
  {
    return _value.callMethodRef(env, hash, name, nameLen,
                                a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return _value.callMethodRef(env, hash, name, nameLen,
                                a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _value.callMethodRef(env, hash, name, nameLen,
                                a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return _value.callClassMethod(env, fun, args);
  }

  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    _value.print(env);
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(StringBuilder sb)
  {
    _value.serialize(sb);
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("&");
    _value.varDump(env, out, depth, valueSet);
  }
  
  //
  // Java Serialization
  //

  public Object writeReplace()
  {
    return _value;
  }
}

