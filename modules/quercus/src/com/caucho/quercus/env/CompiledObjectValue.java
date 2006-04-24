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

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a compiled object value.
 */
public class CompiledObjectValue extends Value {
  private static final StringValue TO_STRING = new StringValueImpl("__toString");
  private static final Value []NULL_FIELDS = new Value[0];

  private final QuercusClass _cl;

  public final Value []_fields;

  private ObjectValue _object;

  public CompiledObjectValue(QuercusClass cl)
  {
    _cl = cl;

    int size = cl.getFieldSize();
    if (size != 0)
      _fields = new Value[cl.getFieldSize()];
    else
      _fields = NULL_FIELDS;
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _cl.getName();
  }

  /**
   * Returns the parent class
   */
  public String getParentName()
  {
    return _cl.getParentName();
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
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return _cl.isA(name);
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
   * Returns the number of entries.
   */
  public int getSize()
  {
    int size = 0;

    for (int i = 0; i < _fields.length; i++) {
      if (_fields[i] != UnsetValue.UNSET)
	size++;
    }

    if (_object != null)
      size += _object.getSize();

    return size;
  }

  /**
   * Gets a new value.
   */
  public Value getField(String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0)
	return _fields[index].toValue();
    }
    
    if (_object != null)
      return _object.getField(key);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Returns the array ref.
   */
  public Var getFieldRef(Env env, String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0) {
	Var var = _fields[index].toRefVar();
	
	_fields[index] = var;

	return var;
      }
    }

    if (_object == null)
      _object = new ObjectValue(_cl);
    
    return _object.getFieldRef(env, key);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArg(Env env, String index)
  {
    // XXX:

    if (_object == null)
      _object = new ObjectValue(_cl);
    
    return _object.getFieldArg(env, index);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArgRef(Env env, String index)
  {
    // XXX:

    if (_object == null)
      _object = new ObjectValue(_cl);
    
    return _object.getFieldArgRef(env, index);
  }

  /**
   * Returns field as an array.
   */
  public Value getFieldArray(Env env, String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0) {
	_fields[index] = _fields[index].toAutoArray();
	
	return _fields[index];
      }
    }

    if (_object == null)
      _object = new ObjectValue(_cl);
    
    return _object.getFieldArray(env, key);
  }

  /**
   * Returns field as an object.
   */
  public Value getFieldObject(Env env, String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0) {
	_fields[index] = _fields[index].toAutoObject(env);
	
	return _fields[index];
      }
    }

    if (_object == null)
      _object = new ObjectValue(_cl);
    
    return _object.getFieldObject(env, key);
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    return getField(key.toString());
  }

  public Value put(Value index, Value value)
  {
    throw new UnsupportedOperationException();
  }

  public Value put(Value value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, Value value)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0) {
	_fields[index] = _fields[index].set(value);

	return value;
      }
    }
    
    if (_object == null)
      _object = new ObjectValue(_cl);

    return _object.putField(env, key, value);
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, String value)
  {
    return putField(null, key, new StringValueImpl(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, long value)
  {
    return putField(null, key, LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, double value)
  {
    return putField(null, key, DoubleValue.create(value));
  }

  /**
   * Removes a value.
   */
  public void removeField(String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0) {
	_fields[index] = UnsetValue.UNSET;

	return;
      }
    }
    
    if (_object != null)
      _object.removeField(key);
  }

  /**
   * Returns the key array
   */
  public Value []getKeyArray()
  {
    return new Value[0];
  }

  /**
   * Returns the value array
   */
  public Value []getValueArray(Env env)
  {
    return new Value[0];
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    ArrayList<Value> indices = new ArrayList<Value>();

    return indices;
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

    // XXX:
    // return new ObjectValue(env, map, _cl, getArray());

    return this;
  }

  /**
   * Clone the object
   */
  public Value clone()
  {
    throw new UnsupportedOperationException();
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

    /*
    for (Map.Entry<String,Value> entry : entrySet()) {
      String key = entry.getKey();

      sb.append("s:");
      sb.append(key.length());
      sb.append(":\"");
      sb.append(key);
      sb.append("\";");

      entry.getValue().serialize(sb);
    }
    */

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
      return _cl.getName() + "[]";
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
   * Converts to an array.
   */
  public Value toArray()
  {
    ArrayValue array = new ArrayValueImpl();

    /*
    for (Map.Entry<String,Value> entry : entrySet()) {
      array.put(new StringValueImpl(entry.getKey()), entry.getValue());
    }
    */

    return array;
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

  public Set<Map.Entry<String,Value>> entrySet()
  {
    throw new UnsupportedOperationException();
    // return new EntrySet();
  }

  /**
   * Returns a Set of entries, sorted by key.
   */
  public Set<Map.Entry<String,Value>> sortedEntrySet()
  {
    throw new UnsupportedOperationException();
    //return new TreeSet<Map.Entry<String, Value>>(entrySet());
  }

  public String toString()
  {
    return "CompiledObjectValue@" + System.identityHashCode(this) +  "[" + _cl.getName() + "]";
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    out.println("object(" + getName() + ") (" + getSize() + ") {");

    ArrayList<String> names = _cl.getFieldNames();
    
    if (names != null) {
      int index = 0;

      for (int i = 0; i < names.size(); i++) {
	if (_fields[i] == UnsetValue.UNSET)
	  continue;

	printDepth(out, 2 * depth + 2);
	out.println("[\"" + names.get(i) + "\"]=>");
	printDepth(out, 2 * depth + 2);
	_fields[i].varDumpImpl(env, out, depth + 1, valueSet);
	out.println();
      }
    }

    if (_object != null) {
      for (Map.Entry<String,Value> mapEntry : _object.sortedEntrySet()) {
	ObjectValue.Entry entry = (ObjectValue.Entry) mapEntry;

	entry.varDumpImpl(env, out, depth + 1, valueSet);
      }
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    out.print(_cl.getName());
    out.print(' ');
    out.println("Object");
    printDepth(out, 4 * depth);
    out.println("(");

    /*
    for (Map.Entry<String,Value> mapEntry : sortedEntrySet()) {
      ObjectValue.Entry entry = (ObjectValue.Entry) mapEntry;

      entry.printRImpl(env, out, depth + 1, valueSet);
    }
    */

    printDepth(out, 4 * depth);
    out.println(")");
  }
}

