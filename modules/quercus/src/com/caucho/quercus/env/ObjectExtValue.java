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

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a PHP object value.
 */
public class ObjectExtValue extends ObjectValue
  implements Serializable
{
  private static final StringValue TO_STRING
    = new StringBuilderValue("__toString");

  private static final int DEFAULT_SIZE = 16;

  private Entry []_entries;
  private int _hashMask;

  private int _size;

  public ObjectExtValue(QuercusClass cl)
  {
    super(cl);

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
   * Returns the number of entries.
   */
  @Override
  public int getSize()
  {
    return _size;
  }

  /**
   * Gets a field value.
   */
  @Override
  public Value getField(Env env, String key, boolean create)
  {
    int capacity = _entries.length;

    int hashMask = _hashMask;
    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null) {
        if (create)
          return createEntry(key).getValue();
        else
          return _quercusClass.getField(env, this, key, create);
      }
      else if (key.equals(entry.getKey())) {
        return entry.getValue();
      }

      hash = (hash + 1) & hashMask;
    }

    return _quercusClass.getField(env, this, key, create);
  }

  /**
   * Returns the array ref.
   */
  @Override
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
  @Override
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
  @Override
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
   * Sets/adds field to this object.
   */
  @Override
  public Value putThisField(Env env, String key, Value value)
  {
    Entry entry = createEntry(key);

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
  @Override
  public Value initField(Env env, String key, Value value)
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
    
    AbstractFunction setField = getQuercusClass().getSetField();
    if (setField != null) {
      
      entry = getEntry(key);
      
      if (entry == null)
        return setField.callMethod(env, this, new UnicodeValueImpl(key), value);
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
  @Override
  public Value putField(String key, String value)
  {
    return putField(null, key, new StringBuilderValue(value));
  }

  /**
   * Adds a new value.
   */
  @Override
  public Value putField(String key, long value)
  {
    return putField(null, key, LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  @Override
  public Value putField(String key, double value)
  {
    return putField(null, key, DoubleValue.create(value));
  }

  /**
   * Removes a value.
   */
  @Override
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
   * Returns an iterator for the key => value pairs.
   */
  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    Iterator<Map.Entry<Value, Value>> iter =  super.getIterator(env);

    if (iter != null)
      return iter;

    return new Iterator<Map.Entry<Value,Value>>() {

      final Iterator<Map.Entry<String,Value>> _iterator = new EntryIterator(_entries);

      public boolean hasNext()
      {
        return _iterator.hasNext();
      }

      public Map.Entry<Value, Value> next()
      {
        final Map.Entry<String,Value> next = _iterator.next();

        return new Map.Entry<Value,Value>() {

          public Value getKey() { return new StringBuilderValue(next.getKey()); }
          public Value getValue() { return next.getValue(); }
          public Value setValue(Value value) { return next.setValue(value); }
        };
      }

      public void remove()
      {
        _iterator.remove();
      }
    };
  }

  /**
   * Returns an iterator for the keys.
   */
  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    Iterator<Value> iter =  super.getKeyIterator(env);

    if (iter != null)
      return iter;

    return new Iterator<Value>() {
      final Iterator<Map.Entry<String,Value>> _iterator
	= new EntryIterator(_entries);

      public boolean hasNext()
      {
        return _iterator.hasNext();
      }

      public Value next()
      {
	return new StringBuilderValue(_iterator.next().getKey());
      }

      public void remove()
      {
        _iterator.remove();
      }
    };
  }

  /**
   * Returns an iterator for the values.
   */
  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    Iterator<Value> iter =  super.getValueIterator(env);

    if (iter != null)
      return iter;

    return new Iterator<Value>() {

      final Iterator<Map.Entry<String,Value>> _iterator = new EntryIterator(_entries);

      public boolean hasNext()
      {
        return _iterator.hasNext();
      }

      public Value next()
      {
        return _iterator.next().getValue();
      }

      public void remove()
      {
        _iterator.remove();
      }
    };
  }

  /**
   * Finds the method name.
   */
  @Override
  public AbstractFunction findFunction(String methodName)
  {
    return _quercusClass.findFunction(methodName);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Expr []args)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value []args)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen,
                                    a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen,
                                    a0, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1, Value a2)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen,
                                    a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1, Value a2, Value a3)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen,
                                    a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _quercusClass.callMethod(env, this, hash, name, nameLen,
                                    a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Expr []args)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value []args)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen,
                                       a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen,
                                       a0, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen,
                                       a0, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen,
                                       a0, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _quercusClass.callMethodRef(env, this, hash, name, nameLen,
                                       a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return fun.callMethod(env, this, args);
  }

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  @Override
  public Value getObject(Env env)
  {
    return this;
  }

  @Override
  public Value getObject(Env env, Value index)
  {
    // php/3d92

    env.error(L.l("Can't use object '{0}' as array", getName()));

    return NullValue.NULL;
  }

  @Override
  public Value getObject(Env env, Location location, Value index)
  {
    env.error(location, L.l("Can't use object '{0}' as array", getName()));

    return NullValue.NULL;
  }

  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return this;
  }

  /**
   * Copy for serialization
   */
  @Override
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
  @Override
  public Value clone()
  {
    ObjectExtValue newObject = new ObjectExtValue(_quercusClass);

    for (Map.Entry<String,Value> entry : entrySet())
      newObject.putField(null, entry.getKey(), entry.getValue());

    return newObject;
  }

  // XXX: need to check the other copy, e.g. for sessions

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(StringBuilder sb)
  {
    sb.append("O:");
    sb.append(_quercusClass.getName().length());
    sb.append(":\"");
    sb.append(_quercusClass.getName());
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
  @Override
  public StringValue toString(Env env)
  {
    AbstractFunction fun = _quercusClass.findFunction("__toString");

    if (fun != null)
      return fun.callMethod(env, this, new Expr[0]).toStringValue();
    else
      return env.createString(_quercusClass.getName() + "[]");
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    env.print(toString(env));
  }

  /**
   * Converts to an array.
   */
  @Override
  public Value toArray()
  {
    ArrayValue array = new ArrayValueImpl();

    for (Map.Entry<String,Value> entry : entrySet()) {
      array.put(new StringBuilderValue(entry.getKey()), entry.getValue());
    }

    return array;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return this;
  }

  @Override
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

  //
  // debugging
  //

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    // XXX: push up to super, and use varDumpObject
    out.println("object(" + getName() + ") (" + getSize() + ") {");

    for (Map.Entry<String,Value> mapEntry : sortedEntrySet()) {
      ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

      entry.varDumpImpl(env, out, depth + 1, valueSet);
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(getName());
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
    out.writeObject(_quercusClass.getName());

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

    QuercusClass cl = env.findClass(name);

    init();

    if (cl != null) {
      setQuercusClass(cl);
    }
    else {
      cl = env.getQuercus().getStdClass();

      setQuercusClass(cl);

      putField(env,
               "__Quercus_Class_Definition_Not_Found",
               env.createString(name));
    }

    int size = in.readInt();
    
    for (int i = 0; i < size; i++) {
      putField(env, (String) in.readObject(), (Value) in.readObject());
    }
  }

  @Override
  public String toString()
  {
    return "ObjectExtValue@" + System.identityHashCode(this) +  "[" + _quercusClass.getName() + "]";
  }
  
  public class EntrySet extends AbstractSet<Map.Entry<String,Value>> {
    EntrySet()
    {
    }

    @Override
    public int size()
    {
      return ObjectExtValue.this.getSize();
    }

    @Override
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

    @Override
    public String toString()
    {
      return "ObjectExtValue.Entry[" + getKey() + "]";
    }
  }
}

