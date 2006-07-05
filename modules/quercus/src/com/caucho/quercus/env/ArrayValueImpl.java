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

  private static final StringValue KEY = new StringValueImpl("key");
  private static final StringValue VALUE = new StringValueImpl("value");
  
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
  private long _nextAvailableIndex;
  private boolean _isDirty;

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
      put(ptr._key, ptr._value.copyArrayItem());
    }
  }

  public ArrayValueImpl(ArrayValueImpl copy)
  {
    copy._isDirty = true;
    _isDirty = true;
    
    _size = copy._size;
    _entries = copy._entries;
    _hashMask = copy._hashMask;

    _head = copy._head;
    _current = copy._current;
    _tail = copy._tail;
    _nextAvailableIndex = copy._nextAvailableIndex;
  }

  public ArrayValueImpl(Env env,
			IdentityHashMap<Value,Value> map,
			ArrayValue copy)
  {
    this();
    
    map.put(copy, this);

    for (Entry ptr = copy.getHead(); ptr != null; ptr = ptr._next) {
      put(ptr._key, ptr._value.toValue().copy(env, map));
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

  private void copyOnWrite()
  {
    if (! _isDirty)
      return;

    _isDirty = false;
    
    Entry []entries = new Entry[_entries.length];
    
    Entry prev = null;
    for (Entry ptr = _head; ptr != null; ptr = ptr._next) {
      Entry ptrCopy = new Entry(ptr._key, ptr._value.copyArrayItem());
      
      entries[ptr._index] = ptrCopy;
      ptrCopy._index = ptr._index;

      if (prev == null)
	_head = _current = ptrCopy;
      else {
	prev._next = ptrCopy;
	ptrCopy._prev = prev;
      }

      prev = ptrCopy;
    }

    _tail = prev;

    _entries = entries;
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
  public StringValue toString(Env env)
  {
    return new StringValueImpl("Array");
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
    _isDirty = true;
    
    return new ArrayValueImpl(this);
  }
  
  /**
   * Copy for assignment.
   */
  public Value copyReturn()
  {
    _isDirty = true;
    
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
  @Override
  public Value toArgValue()
  {
    return copy();
  }
  
  /**
   * Convert to an argument declared as a reference
   */
  @Override
  public Value toRefValue()
  {
    return this;
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
    if (_isDirty) {
      _entries = new Entry[_entries.length];
      _isDirty = false;
    }
    
    _size = 0;
    _head = _tail = _current = null;

    _nextAvailableIndex = 0;
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
    if (_isDirty)
      copyOnWrite();
    
    Entry entry = createEntry(key);

    // php/0434
    Value oldValue = entry._value;

    if (value instanceof Var) {
      // php/0a59
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
   * Add to the beginning
   */
  public ArrayValue unshift(Value value)
  {
    if (_isDirty)
      copyOnWrite();
    
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
   * Replace a section of the array.
   */
  public ArrayValue splice(int start, int end, ArrayValue replace)
  {
    if (_isDirty)
      copyOnWrite();

    int index = 0;

    ArrayValueImpl result = new ArrayValueImpl();

    Entry ptr = _head;
    Entry next = null;
    for (; ptr != null; ptr = next) {
      next = ptr._next;
      
      Value key = ptr.getKey();
      
      if (index < start) {
      }
      else if (index < end) {
	_size--;
	
	if (ptr._prev != null)
	  ptr._prev._next = ptr._next;
	else
	  _head = ptr._next;
	
	if (ptr._next != null)
	  ptr._next._prev = ptr._prev;
	else
	  _tail = ptr._prev;

	if (ptr.getKey() instanceof StringValue)
	  result.put(ptr.getKey(), ptr.getValue());
	else
	  result.put(ptr.getValue());
      }
      else if (replace == null) {
	return result;
      }
      else {
	for (Entry replaceEntry = replace.getHead();
	     replaceEntry != null;
	     replaceEntry = replaceEntry._next) {
	  _size++;
	  
	  if (_entries.length <= 2 * _size)
	    expand();

	  Entry entry = new Entry(createTailKey(), replaceEntry.getValue());

	  addEntry(entry);

	  entry._next = ptr;
	  entry._prev = ptr._prev;

	  if (ptr._prev != null)
	    ptr._prev._next = entry;
	  else
	    _head = entry;

	  ptr._prev = entry;
	}

	return result;
      }

      index++;
    }

    if (replace != null) {
      for (Entry replaceEntry = replace.getHead();
	   replaceEntry != null;
	   replaceEntry = replaceEntry._next) {
	put(replaceEntry.getValue());
      }
    }

    return result;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    if (_isDirty) // XXX: needed?
      copyOnWrite();
    
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
   * Returns the value as an array.
   */
  public Value getArray(Value index)
  {
    if (_isDirty)
      copyOnWrite();
    
    Value value = get(index);

    Value array = value.toAutoArray();
    
    if (value != array) {
      value = array;

      put(index, value);
    }

    return value;
  }

  /**
   * Returns the value as an array, using copy on write if necessary.
   */
  public Value getDirty(Value index)
  {
    if (_isDirty)
      copyOnWrite();
    
    return get(index);
  }

  /**
   * Add
   */
  public Value put(Value value)
  {
    if (_isDirty) 
      copyOnWrite();
    
    Value key = createTailKey();

    put(key, value);

    return value;
  }

  /**
   * Sets the array ref.
   */
  public Value putRef()
  {
    if (_isDirty) 
      copyOnWrite();
    
    // 0d0d
    Value tailKey = createTailKey();
    
    return getRef(tailKey);
  }

  /**
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    return LongValue.create(_nextAvailableIndex);
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
      else if (key.equals(entry._key))
	return entry._value.toValue(); // quercus/39a1

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
      return null;
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
      else if (key.equals(entry._key))
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
    if (_isDirty)
      copyOnWrite();
    
    int capacity = _entries.length;

    key = key.toKey();

    int hash = key.hashCode() & _hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
	return UnsetValue.UNSET;
      else if (key.equals(entry._key)) {
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

	if (key instanceof LongValue
	    && key.toLong() + 1 == _nextAvailableIndex) {
	  updateNextAvailableIndex();
	}

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
    if (_isDirty)
      copyOnWrite();
    
    Entry entry = createEntry(index);
    // quercus/0431
    Value value = entry._value;

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
    // XXX: "A key may be either an integer or a string. If a key is
    //       the standard representation of an integer, it will be
    //       interpreted as such (i.e. "8" will be interpreted as 8,
    //       while "08" will be interpreted as "08")."
    //
    //            http://us3.php.net/types.array

    if (_isDirty)
      copyOnWrite();
    
    int capacity = _entries.length;

    key = key.toKey();
    
    int hashMask = _hashMask;

    int hash = key.hashCode() & hashMask;

    int count = capacity;
    for (; count >= 0; count--) {
      Entry entry = _entries[hash];

      if (entry == null)
	break;
      else if (key.equals(entry._key))
	return entry;

      hash = (hash + 1) & hashMask;
    }
    
    _size++;

    Entry newEntry = new Entry(key);
    updateNextAvailableIndex(newEntry);
    _entries[hash] = newEntry;
    newEntry._index = hash;

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

    int hash = entry._key.hashCode() & _hashMask;

    for (int i = capacity; i >= 0; i--) {
      if (_entries[hash] == null) {
	_entries[hash] = entry;
	updateNextAvailableIndex(entry);
	entry._index = hash;
	return;
      }

      hash = (hash + 1) & _hashMask;
    }
  }

  /**
   * Updates _nextAvailableIndex on a remove of the highest value
   */
  private void updateNextAvailableIndex()
  {
    _nextAvailableIndex = 0;

    for (Entry entry = _head; entry != null; entry = entry._next) {
      updateNextAvailableIndex(entry);
    }
  }

  /**
   * Updates _nextAvailableIndex; this must be invoked for every insertion
   */
  private void updateNextAvailableIndex(Entry entry)
  {
    if (entry._key instanceof LongValue) {
      long key = entry._key.toLong();
	
      if (_nextAvailableIndex <= key)
	_nextAvailableIndex = key + 1;
    }
  }


  /**
   * Pops the top value.
   */
  public Value pop()
  {
    if (_isDirty)
      copyOnWrite();
    
    if (_tail != null) {
      Value value = remove(_tail._key);
      
      return value;
    }
    else
      return BooleanValue.FALSE;
  }

  public Entry getHead()
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
    if (_isDirty)
      copyOnWrite();
    
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

