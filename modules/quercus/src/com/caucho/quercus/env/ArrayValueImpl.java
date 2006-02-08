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

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.vfs.WriteStream;

import com.caucho.util.RandomUtil;

/**
 * Represents a PHP array value.
 */
public class ArrayValueImpl extends ArrayValue {
  private static final Logger log
    = Logger.getLogger(ArrayValueImpl.class.getName());

  private static final StringValue KEY = new StringValue("key");
  private static final StringValue VALUE = new StringValue("value");
  
  private static final int DEFAULT_SIZE = 16;
  
  private static final int SORT_REGULAR = 0;
  private static final int SORT_NUMERIC = 1;
  private static final int SORT_STRING = 2;
  private static final int SORT_LOCALE_STRING = 5;
  
  public static final GetKey GET_KEY = new GetKey();
  public static final GetValue GET_VALUE = new GetValue();
  
  private Entry []_entries;
  private int _hashMask;

  private int _size;
  private long _index;

  private Entry _head;
  private Entry _tail;

  public ArrayValueImpl()
  {
    _entries = new Entry[DEFAULT_SIZE];
    _hashMask = _entries.length - 1;
  }

  public ArrayValueImpl(int size)
  {
    int capacity = DEFAULT_SIZE;

    while (capacity < 4 * size)
      capacity *= 2;
    
    _entries = new Entry[capacity];
    _hashMask = _entries.length - 1;
  }

  public ArrayValueImpl(ArrayValue copy)
  {
    this(copy.getSize());

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr._next) {
      // php/0662 for copy
      put(ptr.getKey(), ptr.getRawValue().copyArrayItem());
    }
  }

  public ArrayValueImpl(Env env,
			IdentityHashMap<Value,Value> map,
			ArrayValue copy)
  {
    this();
    
    map.put(copy, this);

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr._next) {
      put(ptr.getKey(), ptr.getValue().copy(env, map));
    }
  }

  public ArrayValueImpl(Value []keys, Value []values)
  {
    this();

    for (int i = 0; i < keys.length; i++) {
      if (keys[i] != null)
	put(keys[i], values[i]);
      else
	put(values[i]);
    }
  }
  
  /**
   * Returns the type.
   */
  public String getType()
  {
    return "array";
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _size != 0;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
  {
    return "Array";
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return new ArrayValueImpl(this);
  }
  
  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    Value oldValue = map.get(this);

    if (oldValue != null)
      return oldValue;

    return new ArrayValueImpl(env, map, this);
  }
  
  /**
   * Convert to an argument value.
   */
  public Value toArgValue()
  {
    return copy();
  }

  /**
   * Returns the size.
   */
  public int size()
  {
    return _size;
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    return size();
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    _size = 0;
    _head = _tail = _current = null;
    
    for (int j = _entries.length - 1; j >= 0; j--) {
      _entries[j] = null;
    }
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return true;
  }
  
  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    Entry entry = createEntry(key);

    // php/0434
    Value oldValue = entry.getRawValue();

    if (value instanceof Var) {
      // php/0a59
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
   * Add to the beginning
   */
  public ArrayValue unshift(Value value)
  {
    _size++;
    
    if (_entries.length <= 2 * _size)
      expand();

    Value key = createTailKey();

    Entry entry = new Entry(key, value.toArgValue());

    addEntry(entry);

    if (_head != null) {
      _head._prev = entry;
      entry._next = _head;
      _head = entry;
    }
    else {
      _head = _tail = entry;
    }

    return this;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    Entry entry = getEntry(index);

    if (entry != null) {
      // php/3d48, php/39aj
      return entry.toArg();
    }
    else {
      // php/3d49
      return new ArgGetValue(this, index);
    }
  }

  /**
   * Returns the value as an array.
   */
  public Value getArgArray(Value fieldName)
  {
    Value value = get(fieldName);

    /* XXX:
    if (! value.isset()) {
      value = new ArrayValue();

      put(fieldName, value);
    }
    */
    
    return value;
  }

  /**
   * Returns the value as an object
   */
  public Value getArgObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    if (value.isset()) {
      // quercus/3d52
      return value;
    }
    else {
      return new ArgGetObjectValue(env, this, fieldName);
    }
  }

  /**
   * Returns the field value, creating an object if it's unset.
   */
  public Value getObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = env.createObject();

      put(fieldName, value);
    }
    
    return value;
  }

  /**
   * Add
   */
  public ArrayValue put(Value value)
  {
    Value key = createTailKey();

    put(key, value);

    return this;
  }

  /**
   * Sets the array ref.
   */
  public Value putRef()
  {
    // 0d0d
    Value tailKey = createTailKey();
    
    return getRef(tailKey);
  }

  /**
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    return LongValue.create(_index++);
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    int capacity = _entries.length;

    key = key.toKey();

    int hashMask = _hashMask;
    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null) 
	return UnsetValue.UNSET;
      else if (key.equals(entry.getKey()))
	return entry.getValue(); // quercus/39a1

      hash = (hash + 1) & hashMask;
    }

    return UnsetValue.UNSET;
  }

  /**
   * Gets a new value.
   */
  public Value containsKey(Value key)
  {
    Entry entry = getEntry(key);

    if (entry != null)
      return entry.getValue();
    else
      return NullValue.NULL;
  }

  /**
   * Gets a new value.
   */
  private Entry getEntry(Value key)
  {
    int capacity = _entries.length;

    key = key.toKey();

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
   * Removes a value.
   */
  public Value remove(Value key)
  { 
    int capacity = _entries.length;

    key = key.toKey();

    int hash = key.hashCode() & _hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
	return UnsetValue.UNSET;
      else if (key.equals(entry.getKey())) {
	if (entry._prev != null)
	  entry._prev._next = entry._next;
	else
	  _head = entry._next;
	
	if (entry._next != null)
	  entry._next._prev = entry._prev;
	else
	  _tail = entry._prev;

	entry._prev = null;
	entry._next = null;

	_current = _head;

	_size--;

	Value value = entry.getValue();

	_entries[hash] = null;
	shiftEntries(hash + 1);

	return value;
      }

      hash = (hash + 1) & _hashMask;
    }
    
    return UnsetValue.UNSET;
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
   * Returns the array ref.
   */
  public Var getRef(Value index)
  {
    Entry entry = createEntry(index);
    // quercus/0431
    Value value = entry.getRawValue();

    if (value instanceof Var)
      return (Var) value;

    Var var = new Var(value);

    entry.setValue(var);
    
    return var;
  }

  /**
   * Creates the entry for a key.
   */
  private Entry createEntry(Value key)
  {
    int capacity = _entries.length;

    key = key.toKey();
    
    // XXX: check for long only (?) for perf
    if (key instanceof LongValue) {
      long index = key.toLong();

      if (_index <= index)
	_index = index + 1;
    }

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

    Entry newEntry = new Entry();
    _entries[hash] = newEntry;
    
    newEntry._key = key;

    if (_head == null) {
      newEntry._prev = null;
      newEntry._next = null;
      
      _head = newEntry;
      _tail = newEntry;
      _current = newEntry;
    }
    else {
      newEntry._prev = _tail;
      newEntry._next = null;
      
      _tail._next = newEntry;
      _tail = newEntry;
    }

    if (_entries.length <= 2 * _size)
      expand();

    return newEntry;
  }

  private void expand()
  {
    Entry []entries = _entries;
    
    _entries = new Entry[2 * entries.length];
    _hashMask = _entries.length - 1;

    for (Entry entry = _head; entry != null; entry = entry._next) {
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
   * Pops the top value.
   */
  public Value pop()
  {
    if (_tail != null) {
      Value value = remove(_tail.getKey());
      
      return value;
    }
    else
      return BooleanValue.FALSE;
  }

  protected Entry getHead()
  {
    return _head;
  }

  protected Entry getTail()
  {
    return _tail;
  }

  /**
   * Shuffles the array
   */
  public void shuffle()
  {
    Entry []values = new Entry[size()];

    int length = values.length;

    if (length == 0)
      return;

    int i = 0;
    for (Entry ptr = _head; ptr != null; ptr = ptr._next)
      values[i++] = ptr;

    for (i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      Entry temp = values[rand];
      values[rand] = values[i];
      values[i] = temp;
    }

    _head = values[0];
    _head._prev = null;
    
    _tail = values[values.length - 1];
    _tail._next = null;
    
    for (i = 0; i < length; i++) {
      if (i > 0)
	values[i]._prev = values[i - 1];
      if (i < length - 1)
	values[i]._next = values[i + 1];
    }

    _current = _head;
  }
}

