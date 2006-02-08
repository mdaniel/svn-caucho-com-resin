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

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents a PHP object value.
 */
public class ObjectValue extends Value {
  private static final StringValue TO_STRING = new StringValue("__toString");

  private static final int DEFAULT_SIZE = 16;

  private final QuercusClass _cl;

  private Entry []_entries;
  private int _hashMask;

  private int _size;

  public ObjectValue(QuercusClass cl)
  {
    _cl = cl;

    _entries = new Entry[DEFAULT_SIZE];
    _hashMask = _entries.length - 1;
  }

  /*
  public ObjectValue(Env env, IdentityHashMap<Value,Value> map,
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
   * Gets a new value.
   */
  public Value getField(String key)
  {
    int capacity = _entries.length;

    int hashMask = _hashMask;
    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null) {
	return UnsetValue.UNSET;
      }
      else if (key.equals(entry.getKey())) {
	return entry.getValue();
      }

      hash = (hash + 1) & hashMask;
    }

    return UnsetValue.UNSET;
  }

  /**
   * Returns the array ref.
   */
  public Var getFieldRef(Env env, String index)
  {
    Entry entry = createEntry(index);

    Value value = entry.getRawValue();

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

    if (entry != null) {
      return entry.toArg();
    }
    else {
      return new ArgGetFieldValue(env, this, index);
    }
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getFieldArgRef(Env env, String index)
  {
    Entry entry = getEntry(index);

    if (entry != null) {
      return entry.toArg();
    }
    else {
      return new ArgGetFieldValue(env, this, index);
    }
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
   * Adds a new value.
   */
  public Value putField(String key, Value value)
  {
    Entry entry = createEntry(key);

    Value oldValue = entry.getRawValue();

    if (value instanceof Var) {
      Var var = (Var) value;
      var.setReference();

      entry.setRaw(var);
    }
    else if (oldValue instanceof Var) {
      oldValue.set(value);
    }
    else {
      entry.setRaw(value);
    }

    return value;
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, String value)
  {
    return putField(key, new StringValue(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, long value)
  {
    return putField(key, LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, double value)
  {
    return putField(key, DoubleValue.create(value));
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
    ObjectValue newObject = new ObjectValue(_cl);

    for (Map.Entry<String,Value> entry : entrySet())
      newObject.putField(entry.getKey(), entry.getValue());

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
      sb.append("\"");

      entry.getValue().serialize(sb);
    }

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

  public String toString()
  {
    return "ObjectValue@" + System.identityHashCode(this) +  "[" + _cl.getName() + "]";
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    out.println("object(" + getName() + ") (" + getSize() + ") {");

    for (Map.Entry<String,Value> mapEntry : entrySet()) {
      ObjectValue.Entry entry = (ObjectValue.Entry) mapEntry;

      entry.varDump(env, out, depth + 1, valueSet);
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

    for (Map.Entry<String,Value> mapEntry : entrySet()) {
      ObjectValue.Entry entry = (ObjectValue.Entry) mapEntry;

      entry.printR(env, out, depth + 1, valueSet);
    }

    printDepth(out, 4 * depth);
    out.println(")");
  }

  public class EntrySet extends AbstractSet<Map.Entry<String,Value>> {
    EntrySet()
    {
    }

    public int size()
    {
      return ObjectValue.this.getSize();
    }

    public Iterator<Map.Entry<String,Value>> iterator()
    {
      return new EntryIterator(ObjectValue.this._entries);
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

  public final static class Entry extends Var
    implements Map.Entry<String,Value> {
    private final String _key;

    public Entry(String key)
    {
      _key = key;
    }

    public Entry(String key, Value value)
    {
      _key = key;
      setValue(value);
    }

    public Value getValue()
    {
      return getRawValue().toValue();
    }

    public String getKey()
    {
      return _key;
    }

    public Value toValue()
    {
      // The value may be a var
      // XXX: need test
      return getRawValue().toValue();
    }

    /**
     * Argument used/declared as a ref.
     */
    public Var toRefVar()
    {
      Value val = getRawValue();

      if (val instanceof Var)
	return (Var) val;
      else {
	Var var = new Var(val);

	setRaw(var);

	return var;
      }
    }

    /**
     * Converts to an argument value.
     */
    public Value toArgValue()
    {
      return getRawValue().toValue();
    }

    public Value setValue(Value value)
    {
      Value oldValue = toValue();

      setRaw(value);

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toRef()
    {
      Value value = toValue();

      if (value instanceof Var)
	return new RefVar((Var) value);
      else
	return new RefVar(this);
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      Value value = toValue();

      if (value instanceof Var)
	return new RefVar((Var) value);
      else
	return new RefVar(this);
    }

    public Value toArg()
    {
      Value value = getRawValue();

      if (value instanceof Var)
	return value;
      else
	return this;
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws Throwable
    {
      printDepth(out, 2 * depth);
      out.println("[\"" + getKey() + "\"]=>");

      printDepth(out, 2 * depth);
      super.toValue().varDump(env, out, depth, valueSet);

      out.println();
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws Throwable
    {
      printDepth(out, 4 * depth);
      out.print("[" + getKey() + "] => ");
      super.toValue().printR(env, out, depth + 1, valueSet);

      out.println();
    }
  }
}

