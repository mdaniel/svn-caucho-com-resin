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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Represents a PHP expression value.
 */
abstract public class Value {
  protected static final L10N L = new L10N(Value.class);

  public static final StringValue SCALAR_V = new StringValueImpl("scalar");

  public static final Value []NULL_VALUE_ARRAY = new Value[0];
  public static final Value []NULL_ARGS = new Value[0];

  //
  // Properties
  //
  
  public QuercusClass getQuercusClass()
  {
    return null;
  }

  /**
   * Returns the value's class name.
   */
  public String getClassName()
  {
    QuercusClass qClass = getQuercusClass();

    if (qClass != null)
      return qClass.getName();

    return getType();
  }

  //
  // Predicates and Relations
  //
  
  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return false;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return false;
  }

  /**
   * Returns true for a double-value.
   */
  public boolean isDoubleConvertible()
  {
    return false;
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isLongConvertible()
  {
    return false;
  }

  /**
   * Returns true for a null.
   */
  public boolean isNull()
  {
    return false;
  }

  /**
   * Returns true for a number.
   */
  public boolean isNumberConvertible()
  {
    return isLongConvertible() || isDoubleConvertible();
  }

  /**
   * Matches is_numeric
   */
  public boolean isNumeric()
  {
    return false;
  }

  /**
   * Returns true for an object.
   */
  public boolean isObject()
  {
    return false;
  }

  /**
   * Returns true for a set type.
   */
  public boolean isset()
  {
    return true;
  }

  /**
   * Returns true for a StringValue.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true for a BinaryValue.
   */
  public boolean isBinary()
  {
    return false;
  }

  /**
   * Returns true for a UnicodeValue.
   */
  public boolean isUnicode()
  {
    return false;
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return false;
  }

  /**
   * Returns true for equality
   */
  public Value eqValue(Value rValue)
  {
    return eq(rValue) ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    if (rValue instanceof BooleanValue)
      return toBoolean() == rValue.toBoolean();
    else if (isLongConvertible() && rValue.isLongConvertible())
      return toLong() == rValue.toLong();
    else if (isNumberConvertible() || rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    return this == rValue.toValue();
  }

  /**
   * Returns a negative/positive integer if this Value is
   * lessthan/greaterthan rValue.
   */
  public int cmp(Value rValue)
  {
    // This is tricky: implemented according to Table 15-5 of
    // http://us2.php.net/manual/en/language.operators.comparison.php
    
    Value lVal = toValue();
    Value rVal = rValue.toValue();

    if (lVal instanceof StringValue && rVal instanceof NullValue)
      return ((StringValue)lVal).cmpString(StringValue.EMPTY);
    if (lVal instanceof NullValue && rVal instanceof StringValue)
      return StringValue.EMPTY.cmpString((StringValue)rVal);
    if (lVal instanceof StringValue && rVal instanceof StringValue)
      return ((StringValue)lVal).cmpString((StringValue)rVal);

    if (lVal instanceof NullValue || lVal instanceof BooleanValue ||
	rVal instanceof NullValue || rVal instanceof BooleanValue) {
      boolean thisBool = toBoolean();
      boolean rBool    = rValue.toBoolean();
      if (!thisBool && rBool) return -1;
      if (thisBool && !rBool) return 1;
      return 0;
    }

    // XXX: check if values belong to same class; if not, incomparable
    if (lVal instanceof ObjectValue && rVal instanceof ObjectValue)
      return ((ObjectValue)lVal).cmpObject((ObjectValue)rVal);

    if ((lVal instanceof StringValue || lVal instanceof NumberValue ||
	 lVal instanceof ResourceValue) &&
	(rVal instanceof StringValue || rVal instanceof NumberValue ||
	 rVal instanceof ResourceValue))
      return NumberValue.compareNum(lVal, rVal);

    if (lVal instanceof ArrayValue && rVal instanceof ArrayValue)
      ((ArrayValue)lVal).compareArray((ArrayValue)rVal);

    if (lVal instanceof ArrayValue) return 1;
    if (rVal instanceof ArrayValue) return -1;
    if (lVal instanceof ObjectValue) return 1;
    if (rVal instanceof ObjectValue) return -1;

    // XXX: proper default case?
    throw new RuntimeException("values are incomparable: " +
			       lVal + " <=> " + rVal);
  }

  /**
   * Returns true for less than
   */
  public final boolean lt(Value rValue)
  {
    return cmp(rValue)<0;
  }

  /**
   * Returns true for less than or equal to
   */
  public final boolean leq(Value rValue)
  {
    return cmp(rValue)<=0;
  }

  /**
   * Returns true for greater than
   */
  public final boolean gt(Value rValue)
  {
    return cmp(rValue)>0;
  }

  /**
   * Returns true for greater than or equal to
   */
  public final boolean geq(Value rValue)
  {
    return cmp(rValue)>=0;
  }

  //
  // Conversions
  //

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return toBoolean() ? 1 : 0;
  }

  /**
   * Converts to an int
   */
  public int toInt()
  {
    return (int) toLong();
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return 0;
  }

  /**
   * Converts to a char
   */
  public char toChar()
  {
    String s = toString();

    if (s == null || s.length() < 1)
      return 0;
    else
      return s.charAt(0);
  }

  /**
   * Converts to a string.
   *
   * @param env
   */
  public StringValue toString(Env env)
  {
    return toStringValue();
  }

  /**
   * Converts to an array.
   */
  public Value toArray()
  {
    return new ArrayValueImpl().append(this);
  }

  /**
   * Converts to an array if null.
   */
  public Value toAutoArray()
  {
    return this;
  }

  /**
   * Casts to an array.
   */
  public ArrayValue toArrayValue(Env env)
  {
    env.warning(L.l("'{0}' ({1}) is not assignable to ArrayValue",
		  this, getType()));

    return null;
  }

  /**
   * Converts to an object if null.
   */
  public Value toAutoObject(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    return env.createObject();
  }

  public CompiledObjectValue toObjectValue()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
		    getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
		    getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Map toJavaMap(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
		    getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to an exception.
   */
  public QuercusException toException(Env env, String file, int line)
  {
    putField(env, "file", new StringValueImpl(file));
    putField(env, "line", LongValue.create(line));
    
    return new QuercusLanguageException(this);
  }

  /**
   * Converts to a raw value.
   */
  public Value toValue()
  {
    return this;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    throw new QuercusRuntimeException(L.l("{0} is not a valid key", this));
  }

  /**
   * Convert to a ref.
   */
  public Value toRef()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is never assigned or modified
   */
  public Value toArgValueReadOnly()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is never assigned, but might be modified, e.g. $a[3] = 9
   */
  public Value toArgValue()
  {
    return this;
  }

  /**
   * Convert to a function argument reference value, e.g. for
   *
   * function foo(&$a)
   *
   * where $a is never assigned in the function
   */
  public Value toRefValue()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is used as a variable in the function
   */
  public Var toVar()
  {
    return new Var(toArgValue());
  }

  /**
   * Convert to a function argument reference value, e.g. for
   *
   * function foo(&$a)
   *
   * where $a is used as a variable in the function
   */
  public Var toRefVar()
  {
    return new Var(this);
  }

  /**
   * Converts to a StringValue.
   */
  public StringValue toStringValue()
  {
    return new StringValueImpl(toString());
  }

  /**
   * Converts to a UnicodeValue.
   */
  public UnicodeValue toUnicodeValue(Env env)
  {
    return new StringValueImpl(toString());
  }

  /**
   * Converts to a BinaryValue.
   */
  public BinaryValue toBinaryValue(Env env)
  {
    try {
      InputStream is = toInputStream();

      BinaryBuilderValue bb = new BinaryBuilderValue();

      int length = 0;
      while (true) {
        bb.prepareReadBuffer();

        int sublen = is.read(bb.getBuffer(),
                             bb.getOffset(),
                             bb.getLength() - bb.getOffset());

        if (sublen <= 0)
          return bb;
        else {
          length += sublen;
          bb.setOffset(length);
        }
      }
    } catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  /**
   * Returns a byteArrayInputStream for the value.
   * See TempBufferStringValue for how this can be overriden
   *
   * @return InputStream
   */
  public InputStream toInputStream()
  {
    return new StringInputStream(toString());
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder()
  {
    return new StringBuilderValue(toString(), 32);
  }

  //
  // Operations
  //

  /**
   * Append to a string builder.
   */
  public void appendTo(StringBuilderValue sb)
  {
    //System.out.println("APPEND: " + toString());
    sb.append(toString());
  }

  /**
   * Append to a binary builder.
   */
  public void appendTo(BinaryBuilderValue sb)
  {
    sb.appendBytes(toString());
  }

  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return this;
  }

  /**
   * Copy as an array item
   */
  public Value copyArrayItem()
  {
    return copy();
  }

  /**
   * Copy as a return value
   */
  public Value copyReturn()
  {
    // php/3a5d

    return this;
  }

  /**
   * Copy for serialization
   */
  public final Value copy(Env env)
  {
    return copy(env, new IdentityHashMap<Value,Value>());
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return this;
  }

  /**
   * Clone for the clone keyword
   */
  public Value clone()
  {
    return this;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "value";
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    return NullValue.NULL;
  }

  /**
   * Returns the current value
   */
  public Value current()
  {
    return NullValue.NULL;
  }

  /**
   * Returns the current value
   */
  public Value next()
  {
    return BooleanValue.FALSE;
  }

  /**
   * Finds the method name.
   */
  public AbstractFunction findFunction(String methodName)
  {
    return null;
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }

    return callMethod(env, methodName, value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value []args)
  {
    switch (args.length) {
    case 0:
      return callMethod(env, methodName);

    case 1:
      return callMethod(env, methodName, args[0]);

    case 2:
      return callMethod(env, methodName, args[0], args[1]);

    case 3:
      return callMethod(env, methodName, args[0], args[1], args[2]);

    case 4:
      return callMethod(env, methodName, args[0], args[1], args[2],
			args[3]);

    case 5:
      return callMethod(env, methodName, args[0], args[1], args[2],
			args[3], args[4]);

    default:
      return errorNoMethod(env, methodName);
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethod(Env env, String methodName)
  {
    return errorNoMethod(env, methodName);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env, String methodName, Value a0)
  {
    return errorNoMethod(env, methodName);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env, String methodName, Value a0, Value a1)
  {
    return errorNoMethod(env, methodName);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2)
  {
    return errorNoMethod(env, methodName);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return errorNoMethod(env, methodName);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethod(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a5)
  {
    return errorNoMethod(env, methodName);
  }

  private Value errorNoMethod(Env env, String methodName)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toDebugString(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }

    return callMethodRef(env, methodName, value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value []args)
  {
    switch (args.length) {
    case 0:
      return callMethodRef(env, methodName);

    case 1:
      return callMethodRef(env, methodName, args[0]);

    case 2:
      return callMethodRef(env, methodName, args[0], args[1]);

    case 3:
      return callMethodRef(env, methodName, args[0], args[1], args[2]);

    case 4:
      return callMethodRef(env, methodName, args[0], args[1], args[2],
			args[3]);

    case 5:
      return callMethodRef(env, methodName, args[0], args[1], args[2],
			args[3], args[4]);

    default:
      return env.error(L.l("{0}: '{1}' is an unknown method.",
			   toString(), methodName));
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethodRef(Env env, String methodName)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethodRef(Env env, String methodName, Value a0)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethodRef(Env env, String methodName, Value a0, Value a1)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethodRef(Env env, String methodName,
			  Value a0, Value a1, Value a2, Value a3, Value a5)
  {
    return env.error(L.l("{0}: '{1}' is an unknown method.",
			 toString(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return NullValue.NULL;
  }

  //
  // Arithmetic operations
  //

  /**
   * Negates the value.
   */
  public Value neg()
  {
    return new LongValue(- toLong());
  }

  /**
   * Negates the value.
   */
  public Value pos()
  {
    return new LongValue(toLong());
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
  {
    if (isLongConvertible() && rValue.isLongConvertible())
      return LongValue.create(toLong() + rValue.toLong());
    
    return DoubleValue.create(toDouble() + rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value add(long lLong)
  {
    return new DoubleValue(lLong + toDouble());
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
  {
    long lValue = toLong();

    return new LongValue(lValue + incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
  {
    long lValue = toLong();

    return new LongValue(lValue + incr);
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
  {
    if (isLongConvertible() && rValue.isLongConvertible())
      return LongValue.create(toLong() - rValue.toLong());
    return DoubleValue.create(toDouble() - rValue.toDouble());
  }

  /**
   * Subtracts
   */
  public Value sub(long rLong)
  {
    return new DoubleValue(toDouble() - rLong);
  }


  /**
   * Substracts from the previous value.
   */
  public Value sub_rev(long lLong)
  {
    return new DoubleValue(lLong - toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
  {
    return new DoubleValue(toDouble() * rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long lLong)
  {
    return new DoubleValue(toDouble() * lLong);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
  {
    double lDouble = toDouble();
    double rDouble = rValue.toDouble();

    return new DoubleValue(lDouble / rDouble);
  }

  /**
   * modulo the following value.
   */
  public Value mod(Value rValue)
  {
    double lDouble = toDouble();
    double rDouble = rValue.toDouble();

    return LongValue.create((long) lDouble % rDouble);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return new LongValue(lLong << rLong);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return new LongValue(lLong >> rLong);
  }

  //
  // string functions
  //

  /**
   * Returns the length as a string.
   */
  public int length()
  {
    return toString().length();
  }

  //
  // Array functions
  //

  /**
   * Returns the array size.
   */
  public int getSize()
  {
    return 0;
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    return new java.util.ArrayList<Value>();
  }

  /**
   * Returns the field keys.
   */
  public Value []getKeyArray()
  {
    return NULL_VALUE_ARRAY;
  }

  /**
   * Returns the field values.
   */
  public Value []getValueArray(Env env)
  {
    return NULL_VALUE_ARRAY;
  }

  /**
   * Returns the array ref.
   */
  public Value get(Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns a reference to the array value.
   */
  public Value getRef(Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the array ref as a function argument.
   */
  public Value getArg(Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the array value, copying on write if necessary.
   */
  public Value getDirty(Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the array ref for an argument.
   */
  public Value getArgRef(Value index)
  {
    return getRef(index);
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public Value getArray()
  {
    return this;
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public Value getArray(Value index)
  {
    return NullValue.NULL;
  }

  //
  // Object operations
  //

  /**
   * Returns the field ref.
   */
  public Value getField(Env env, String index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the field ref.
   */
  public Value getFieldRef(Env env, String index)
  {
    return getField(env, index);
  }

  /**
   * Returns the field ref.
   */
  public Value getFieldArg(Env env, String index)
  {
    return getFieldRef(env, index);
  }

  /**
   * Returns the field ref for an argument.
   */
  public Value getFieldArgRef(Env env, String index)
  {
    return getFieldRef(env, index);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldObject(Env env, String index)
  {
    Value v = getField(env, index);

    if (! v.isset()) {
      v = env.createObject();

      putField(env, index, v);
    }

    return v;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldArray(Env env, String index)
  {
    Value v = getField(env, index);

    Value array = v.toAutoArray();

    if (v == array)
      return v;
    else {
      putField(env, index, array);

      return array;
    }
  }

  /**
   * Returns the field ref.
   */
  public Value putField(Env env, String index, Value object)
  {
    return NullValue.NULL;
  }

  /**
   * Removes the field ref.
   */
  public void removeField(String index)
  {
  }

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  public Value getObject(Env env)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getObject(Env env, Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Sets the value ref.
   */
  public Value set(Value value)
  {
    return value;
  }

  /**
   * Sets the array ref.
   */
  public Value put(Value index, Value value)
  {
    return value;
  }

  /**
   * Sets the array ref.
   */
  public Value put(Value value)
  {
    return value;
  }

  /**
   * Sets the array ref.
   */
  public Value putRef()
  {
    return NullValue.NULL;
  }

  /**
   * Appends the array
   */
  public Value putArray()
  {
    ArrayValue value = new ArrayValueImpl();

    put(value);

    return value;
  }

  /**
   * Appends a new object
   */
  public Value putObject(Env env)
  {
    Value value = env.createObject();

    put(value);

    return value;
  }

  /**
   * Return unset the value.
   */
  public Value remove(Value index)
  {
    return UnsetValue.UNSET;
  }
  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  public <T> T[] valuesToArray(Env env, Class<T> elementType)
  {
    return null;
  }

  /**
   * Returns the character at the named index.
   */
  public Value charValueAt(long index)
  {
    return NullValue.NULL;
  }

  /**
   * Sets the character at the named index.
   */
  public Value setCharValueAt(long index, String value)
  {
    return NullValue.NULL;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(toString(env));
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    // XXX: remove when done
    System.out.println("Generate: " + getClass().getName());

    out.print("com.caucho.quercus.env.NullValue.NULL");
  }

  public String toInternString()
  {
    return toString().intern();
  }

  public String toDebugString()
  {
    return toString();
  }

  final public void varDump(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (valueSet.get(this) != null) {
       out.print("#recursion#");
       return;
     }

    valueSet.put(this, "printing");

    try {
      varDumpImpl(env, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  protected void varDumpImpl(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(toString());
  }

  final public void printR(Env env,
                           WriteStream out,
                           int depth,
                           IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (valueSet.get(this) != null) {
      out.print("#recursion#");
      return;
    }

    valueSet.put(this, "printing");

    try {
      printRImpl(env, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(toString());
  }

  protected void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }
}

