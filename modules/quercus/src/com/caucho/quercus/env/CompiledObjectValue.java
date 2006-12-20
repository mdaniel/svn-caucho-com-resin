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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a compiled object value.
 */
public class CompiledObjectValue extends ObjectValue
  implements Serializable
{
  private static final StringValue TO_STRING = new StringValueImpl("__toString");
  private static final Value []NULL_FIELDS = new Value[0];

  private QuercusClass _cl;

  public Value []_fields;

  private ObjectExtValue _object;

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

  public CompiledObjectValue toObjectValue()
  {
    return this;
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
  @Override
  public Value getField(Env env, String key)
  {
    if (_fields.length > 0) {
      int index = _cl.findFieldIndex(key);

      if (index >= 0)
	return _fields[index].toValue();
    }
    
    if (_object != null)
      return _object.getField(env, key);
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
      _object = new ObjectExtValue(_cl);
    
    return _object.getFieldRef(env, key);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArg(Env env, String key)
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
      _object = new ObjectExtValue(_cl);
    
    return _object.getFieldArg(env, key);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArgRef(Env env, String key)
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
      _object = new ObjectExtValue(_cl);
    
    return _object.getFieldArgRef(env, key);
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
      _object = new ObjectExtValue(_cl);
    
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
      _object = new ObjectExtValue(_cl);
    
    return _object.getFieldObject(env, key);
  }

  /**
   * Gets a new value.
   */
  @Override
  public Value get(Value key)
  {
    throw new UnsupportedOperationException();
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
      _object = new ObjectExtValue(_cl);

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

    for (int i = 0; i < _fields.length; i++) {
      if (_fields[i] != UnsetValue.UNSET)
	indices.add(_fields[i]);
    }

    if (_object != null)
      indices.addAll(_object.getIndices());

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
  public Value callMethod(Env env, String methodName, Expr []args)
  {
    return _cl.getFunction(methodName).callMethod(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value []args)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, args);
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName)
  {
    return _cl.getFunction(methodName).callMethod(env, this);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a0)
  {
    return _cl.getFunction(methodName).callMethod(env, this, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1)
  {
    return _cl.getFunction(methodName).callMethod(env, this, a0, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2)
  {
    return _cl.getFunction(methodName).callMethod(env, this,
                                                  a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2, Value a3)
  {
    return _cl.getFunction(methodName).callMethod(env, this,
                                                  a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _cl.getFunction(methodName).callMethod(env, this,
                                                  a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Expr []args)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value []args)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a0)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this, a0);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this, a0, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this,
                                                     a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this,
                                                     a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _cl.getFunction(methodName).callMethodRef(env, this,
                                                     a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    Value oldThis = env.getThis();

    try {
      env.setThis(this);

      return fun.call(env, args);
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
    // return new ObjectExtValue(env, map, _cl, getArray());

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

    ArrayList<String> names = _cl.getFieldNames();
    
    if (names != null) {
      int index = 0;

      for (int i = 0; i < names.size(); i++) {
	String key = names.get(i);
	
	if (_fields[i] == UnsetValue.UNSET)
	  continue;
	
	sb.append("s:");
	sb.append(key.length());
	sb.append(":\"");
	sb.append(key);
	sb.append("\";");

	_fields[i].serialize(sb);
      }
    }

    if (_object != null) {
      for (Map.Entry<String,Value> mapEntry : _object.sortedEntrySet()) {
	ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

	String key = entry.getKey();
	
	sb.append("s:");
	sb.append(key.length());
	sb.append(":\"");
	sb.append(key);
	sb.append("\";");

	entry.getValue().serialize(sb);
      }
    }

    sb.append("}");
  }

  /**
   * Converts to a string.
   * @param env
   */
  public StringValue toString(Env env)
  {
    AbstractFunction fun = _cl.findFunction("__toString");

    if (fun != null)
      return fun.callMethod(env, this, new Expr[0]).toString(env);
    else
      return new StringBuilderValue().append(_cl.getName()).append("[]");
  }

  /**
   * Converts to a string.
   * @param env
   */
  public void print(Env env)
  {
    env.print(toString(env));
  }

  /**
   * Converts to an array.
   */
  public Value toArray()
  {
    ArrayValue array = new ArrayValueImpl();

    for (Map.Entry<String,Value> entry : entrySet()) {
      array.put(new StringValueImpl(entry.getKey()), entry.getValue());
    }

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
    throws IOException
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
	ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

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
    throws IOException
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
  
  //
  // Java Serialization
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  { 
    System.err.println("CompiledObjectArray->writeObject()");
    
    out.writeObject(_fields);
    out.writeObject(_object);
    out.writeObject(_cl.getName());
  }
  
  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  { 
    System.err.println("CompiledObjectArray->readObject()");
    
    _fields = (Value []) in.readObject();
    _object = (ObjectExtValue) in.readObject();
    
    Env env = Env.getInstance();
    String name = (String) in.readObject();

    _cl = env.findClass(name);

    if (_cl != null) {
    }
    else {
      _cl = env.getQuercus().getStdClass();

      putField(env,
               "__Quercus_Class_Definition_Not_Found",
               new StringValueImpl(name));
    }
  }
}

