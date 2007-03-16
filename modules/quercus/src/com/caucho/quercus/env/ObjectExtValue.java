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
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.StringLiteralExpr;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Represents a PHP object value.
 */
public class ObjectExtValue extends ObjectValue
  implements Serializable
{
  private static final StringValue TO_STRING = new StringValueImpl("__toString");

  private static final int DEFAULT_SIZE = 16;

  private QuercusClass _cl;

  private Entry []_entries;
  private int _hashMask;

  private int _size;

  public ObjectExtValue(QuercusClass cl)
  {
    _cl = cl;
    
    init();
  }
  
  private void init()
  {
    _entries = new Entry[DEFAULT_SIZE];
    _hashMask = _entries.length - 1;
  }

  /*
  public ObjectExtValue(Env env, IdentityHashMap<Value,Value> map,
                     QuercusClass cl, ArrayValue oldValue)
  {
    super(new ArrayValueImpl(env, map, oldValue));

    _cl = cl;

    // _cl.initFields(_map);
  }
  */

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
   * Returns the quercus class.
   */
  public QuercusClass getQuercusClass()
  {
    return _cl;
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
    return _size;
  }

  /**
   * Gets a field value.
   */
  @Override
  public Value getField(Env env, String key)
  {
    int capacity = _entries.length;

    int hashMask = _hashMask;
    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null) {
        return _cl.getField(env, this, key);
      }
      else if (key.equals(entry.getKey())) {
        return entry.getValue();
      }

      hash = (hash + 1) & hashMask;
    }

    return _cl.getField(env, this, key);
  }

  /**
   * Returns the array ref.
   */
  public Var getFieldRef(Env env, String index)
  {
    Entry entry = createEntry(index);

    Value value = entry._value;

    if (value instanceof Var)
      return (Var) value;

    Var var = new Var(value);

    entry.setValue(var);

    return var;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArg(Env env, String index)
  {
    Entry entry = getEntry(index);

    if (entry != null)
      return entry.toArg();
    else
      return new ArgGetFieldValue(env, this, index);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArgRef(Env env, String index)
  {
    Entry entry = getEntry(index);

    if (entry != null)
      return entry.toArg();
    else
      return new ArgGetFieldValue(env, this, index);
  }

  /**
   * Gets a new value.
   */
  private Entry getEntry(String key)
  {
    int capacity = _entries.length;

    int hash = key.hashCode() & _hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
        return null;
      else if (key.equals(entry.getKey()))
        return entry;

      hash = (hash + 1) & _hashMask;
    }

    return null;
  }

  /**
   * XXX: to handle foreach
   */
  public Value get(Value key)
  {
    // php/0d92 - expects exception
    return getField(null, key.toString());
  }

  public Value put(Value index, Value value)
  {
    // php/0d94
    /*
    throw new QuercusException(L.l("Object of type '{0}' cannot be used as an array",
				   getClassName()));
    */
    return NullValue.NULL;
  }

  public Value put(Value value)
  {
    throw new QuercusException(L.l("Object of type '{0}' cannot be used as an array",
				   getClassName()));
  }

  /**
   * Adds a new value.
   */
  public Value putFieldInit(Env env, String key, Value value)
  {
    createEntry(key);

    return putField(env, key, value);
  }

  /**
   * Adds a new value.
   */
  @Override
  public Value putField(Env env, String key, Value value)
  {
    Entry entry = null;

    AbstractFunction setField = _cl.getSetField();
    if (setField != null) {
      entry = getEntry(key);

      if (entry == null)
	return setField.callMethod(env, this, new StringValueImpl(key), value);
    }
    else
      entry = createEntry(key);

    Value oldValue = entry._value;

    if (value instanceof Var) {
      Var var = (Var) value;
      var.setReference();

      entry._value = var;
    }
    else if (oldValue instanceof Var) {
      oldValue.set(value);
    }
    else {
      entry._value = value;
    }

    return value;
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
    int capacity = _entries.length;

    int hash = key.hashCode() & _hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
        return;
      else if (key.equals(entry.getKey())) {
        _size--;

        _entries[hash] = null;
        shiftEntries(hash + 1);

        return;
      }

      hash = (hash + 1) & _hashMask;
    }
  }

  /**
   * Shift entries after a delete.
   */
  private void shiftEntries(int index)
  {
    int capacity = _entries.length;

    for (; index < capacity; index++) {
      Entry entry = _entries[index];

      if (entry == null)
        return;

      _entries[index] = null;

      addEntry(entry);
    }
  }

  /**
   * Creates the entry for a key.
   */
  private Entry createEntry(String key)
  {
    int capacity = _entries.length;

    int hashMask = _hashMask;

    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
        break;
      else if (key.equals(entry.getKey()))
        return entry;

      hash = (hash + 1) & hashMask;
    }

    _size++;

    Entry newEntry = new Entry(key);
    _entries[hash] = newEntry;

    if (_entries.length <= 2 * _size)
      expand();

    return newEntry;
  }

  private void expand()
  {
    Entry []entries = _entries;

    _entries = new Entry[2 * entries.length];
    _hashMask = _entries.length - 1;

    for (int i = entries.length - 1; i >= 0; i--) {
      Entry entry = entries[i];

      if (entry != null)
        addEntry(entry);
    }
  }

  private void addEntry(Entry entry)
  {
    int capacity = _entries.length;

    int hash = entry.getKey().hashCode() & _hashMask;

    for (int i = capacity; i >= 0; i--) {
      if (_entries[hash] == null) {
        _entries[hash] = entry;
        return;
      }

      hash = (hash + 1) & _hashMask;
    }
  }

  /**
   * Returns the key array
   */
  public Value []getKeyArray()
  {
    Value []keys = new Value[getSize()];

    int k = 0;
    for (int i = 0; i < _entries.length; i++) {
      Entry entry = _entries[i];

      if (entry != null)
	keys[k++] = new StringValueImpl(entry.getKey());
    }

    return keys;
  }

  /**
   * Returns the value array
   */
  public Value []getValueArray(Env env)
  {
    Value []values = new Value[getSize()];

    int k = 0;
    for (int i = 0; i < _entries.length; i++) {
      Entry entry = _entries[i];

      if (entry != null)
	values[k++] = entry.getValue().toValue();
    }

    return values;
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    ArrayList<Value> indices = new ArrayList<Value>();

    for (int i = 0; i < _entries.length; i++) {
      Entry entry = _entries[i];

      if (entry != null)
	indices.add(new StringValueImpl(entry.getKey()));
    }

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
    AbstractFunction fun = _cl.findFunction(methodName);
    
    if (fun != null)
      return fun.callMethod(env, this, args);
    else if (_cl.getCall() != null) {
      Expr []newArgs = new Expr[args.length + 1];
      newArgs[0] = new StringLiteralExpr(methodName);
      System.arraycopy(args, 0, newArgs, 1, args.length);
      
      return _cl.getCall().callMethod(env, this, newArgs);
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value []args)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, args);
    else if (_cl.getCall() != null) {
      StringValueImpl name = new StringValueImpl(methodName);
      ArrayValue newArgs = new ArrayValueImpl();

      for (int i = 0; i < args.length; i++)
	newArgs.append(args[i]);
      
      return _cl.getCall().callMethod(env, this, name, newArgs);
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl());
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a0)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, a0);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl()
				      .append(a0));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, a0, a1);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl()
				      .append(a0)
				      .append(a1));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, a0, a1, a2);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl().append(a0)
				      .append(a1)
				      .append(a2));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, a0, a1, a2, a3);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl()
				      .append(a0)
				      .append(a1)
				      .append(a2)
				      .append(a3));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethod(env, this, a0, a1, a2, a3, a4);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethod(env,
				      this,
				      new StringValueImpl(methodName),
				      new ArrayValueImpl()
				      .append(a0)
				      .append(a1)
				      .append(a2)
				      .append(a3)
				      .append(a4));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
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
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, args);
    else if (_cl.getCall() != null) {
      StringValueImpl name = new StringValueImpl(methodName);
      ArrayValue newArgs = new ArrayValueImpl();

      for (int i = 0; i < args.length; i++)
	newArgs.append(args[i]);
      
      return _cl.getCall().callMethodRef(env, this, name, newArgs);
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl());
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a0)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, a0);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl()
					 .append(a0));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, a0, a1);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl()
					 .append(a0)
					 .append(a1));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, a0, a1, a2);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl()
					 .append(a0)
					 .append(a1)
					 .append(a2));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, a0, a1, a2, a3);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl()
					 .append(a0)
					 .append(a1)
					 .append(a2)
					 .append(a3));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _cl.findFunction(methodName);

    if (fun != null)
      return fun.callMethodRef(env, this, a0, a1, a2, a3, a4);
    else if (_cl.getCall() != null) {
      return _cl.getCall().callMethodRef(env,
					 this,
					 new StringValueImpl(methodName),
					 new ArrayValueImpl()
					 .append(a0)
					 .append(a1)
					 .append(a2)
					 .append(a3)
					 .append(a4));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           _cl.getName(), methodName));
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return fun.callMethod(env, this, args);
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
    ObjectExtValue newObject = new ObjectExtValue(_cl);

    for (Map.Entry<String,Value> entry : entrySet())
      newObject.putField(null, entry.getKey(), entry.getValue());

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

    for (Map.Entry<String,Value> entry : entrySet()) {
      String key = entry.getKey();

      sb.append("s:");
      sb.append(key.length());
      sb.append(":\"");
      sb.append(key);
      sb.append("\";");

      entry.getValue().serialize(sb);
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
      return fun.callMethod(env, this, new Expr[0]).toStringValue();
    else
      return new StringValueImpl(_cl.getName() + "[]");
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
    return new EntrySet();
  }

  /**
   * Returns a Set of entries, sorted by key.
   */
  public Set<Map.Entry<String,Value>> sortedEntrySet()
  {
    return new TreeSet<Map.Entry<String, Value>>(entrySet());
  }

  public String toString()
  {
    return "ObjectExtValue@" + System.identityHashCode(this) +  "[" + _cl.getName() + "]";
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("object(" + getName() + ") (" + getSize() + ") {");

    for (Map.Entry<String,Value> mapEntry : sortedEntrySet()) {
      ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

      entry.varDumpImpl(env, out, depth + 1, valueSet);
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

    for (Map.Entry<String,Value> mapEntry : sortedEntrySet()) {
      ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

      entry.printRImpl(env, out, depth + 1, valueSet);
    }

    printDepth(out, 4 * depth);
    out.println(")");
  }

  //
  // Java Serialization
  //
  
  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(_cl.getName());

    out.writeInt(_size);
    
    for (Map.Entry<String,Value> entry : entrySet()) {      
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }
  
  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  { 
    Env env = Env.getInstance();
    String name = (String) in.readObject();

    _cl = env.findClass(name);

    init();

    if (_cl != null) {
    }
    else {
      _cl = env.getQuercus().getStdClass();

      putField(env,
               "__Quercus_Class_Definition_Not_Found",
               new StringValueImpl(name));
    }

    int size = in.readInt();
    
    for (int i = 0; i < size; i++) {
      putField(env, (String) in.readObject(), (Value) in.readObject());
    }
  }
  
  public class EntrySet extends AbstractSet<Map.Entry<String,Value>> {
    EntrySet()
    {
    }

    public int size()
    {
      return ObjectExtValue.this.getSize();
    }

    public Iterator<Map.Entry<String,Value>> iterator()
    {
      return new EntryIterator(ObjectExtValue.this._entries);
    }
  }

  public static class EntryIterator
    implements Iterator<Map.Entry<String,Value>> {
    private final Entry []_list;
    private int _index;

    EntryIterator(Entry []list)
    {
      _list = list;
    }

    public boolean hasNext()
    {
      for (; _index < _list.length && _list[_index] == null; _index++) {
      }

      return _index < _list.length;
    }

    public Map.Entry<String,Value> next()
    {
      for (; _index < _list.length && _list[_index] == null; _index++) {
      }

      if (_list.length <= _index)
        return null;

      return _list[_index++];
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public final static class Entry
    implements Map.Entry<String,Value>,
               Comparable<Map.Entry<String, Value>>
  {
    private final String _key;
    private Value _value;

    public Entry(String key)
    {
      _key = key;
      _value = NullValue.NULL;
    }

    public Entry(String key, Value value)
    {
      _key = key;
      _value = value;
    }

    public Value getValue()
    {
      return _value.toValue();
    }

    public String getKey()
    {
      return _key;
    }

    public Value toValue()
    {
      // The value may be a var
      // XXX: need test
      return _value.toValue();
    }

    /**
     * Argument used/declared as a ref.
     */
    public Var toRefVar()
    {
      Value val = _value;

      if (val instanceof Var)
        return (Var) val;
      else {
        Var var = new Var(val);

        _value = var;

        return var;
      }
    }

    /**
     * Converts to an argument value.
     */
    public Value toArgValue()
    {
      return _value.toValue();
    }

    public Value setValue(Value value)
    {
      Value oldValue = toValue();

      _value = value;

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new RefVar((Var) value);
      else {
	Var var = new Var(_value);
	
	_value = var;
	
        return new RefVar(var);
      }
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new RefVar((Var) value);
      else {
	Var var = new Var(_value);
	
	_value = var;
	
        return new RefVar(var);
      }
    }

    public Value toArg()
    {
      Value value = _value;

      if (value instanceof Var)
        return value;
      else {
	Var var = new Var(_value);
	
	_value = var;
	
        return var;
      }
    }

    public int compareTo(Map.Entry<String, Value> other)
    {
      if (other == null)
        return 1;

      String thisKey = getKey();
      String otherKey = other.getKey();

      if (thisKey == null)
        return otherKey == null ? 0 : -1;

      if (otherKey == null)
        return 1;

      return thisKey.compareTo(otherKey);
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 2 * depth);
      out.println("[\"" + getKey() + "\"]=>");

      printDepth(out, 2 * depth);
      
      _value.varDump(env, out, depth, valueSet);
      
      out.println();
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 4 * depth);
      out.print("[" + getKey() + "] => ");
      
      _value.printR(env, out, depth + 1, valueSet);

      out.println();
    }

    private void printDepth(WriteStream out, int depth)
      throws java.io.IOException
    {
      for (int i = 0; i < depth; i++)
	out.print(' ');
    }

    public String toString()
    {
      return "ObjectExtValue.Entry[" + getKey() + "]";
    }
  }
}

