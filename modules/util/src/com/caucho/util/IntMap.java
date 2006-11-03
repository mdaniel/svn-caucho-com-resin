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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.*;
/**
 * The IntMap provides a simple hashmap from keys to integers.  The API is
 * an abbreviation of the HashMap collection API.
 *
 * <p>The convenience of IntMap is avoiding all the silly wrapping of
 * integers.
 */
public class IntMap {
  /**
   * Encoding of a null entry.  Since NULL is equal to Integer.MIN_VALUE, 
   * it's impossible to distinguish between the two.
   */
  public final static int NULL = -65536; // Integer.MIN_VALUE + 1;
  private static int DELETED = 0x1;
  private Object []_keys;
  private int _nullValue;
  private int []_values;
  private int _size;
  private int _mask;

  /**
   * Create a new IntMap.  Default size is 16.
   */
  public IntMap()
  {
    _keys = new Object[16];
    _values = new int[16];

    _mask = _keys.length - 1;
    _size = 0;

    _nullValue = NULL;
  }

  /**
   * Create a new IntMap for cloning.
   */
  private IntMap(boolean dummy)
  {
  }

  /**
   * Clear the hashmap.
   */
  public void clear()
  {
    _nullValue = NULL;
    for (int i = 0; i < _values.length; i++) {
      _keys[i] = null;
      _values[i] = 0;
    }
    _size = 0;
  }
  /**
   * Returns the current number of entries in the map.
   */
  public int size() 
  { 
    return _size;
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public int get(Object key)
  {
    if (key == null)
      return _nullValue;

    int hash = key.hashCode() & _mask;
    Object []keys = _keys;

    for (int i = keys.length; i >= 0; i--) {
      Object mapKey = keys[hash];

      if (mapKey == key)
	return _values[hash];
      else if (mapKey == null)
	return NULL;
      else if (mapKey.equals(key))
	return _values[hash];

      hash = (hash + 1) & _mask;
    }

    return NULL;
  }

  /**
   * Expands the property table
   */
  private void resize(int newSize)
  {
    Object []newKeys = new Object[newSize];
    int []newValues = new int[newSize];

    _mask = newKeys.length - 1;

    for (int i = 0; i < _keys.length; i++) {
      if (_keys[i] == null)
	continue;

      int hash = _keys[i].hashCode() & _mask;

      for (int j = _mask; j >= 0; j--) {
	if (newKeys[hash] == null) {
	  newKeys[hash] = _keys[i];
	  newValues[hash] = _values[i];
	  break;
	}
	hash = (hash + 1) & _mask;
      }
    }

    _keys = newKeys;
    _values = newValues;
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public int put(Object key, int value)
  {
    if (key == null) {
      int old = _nullValue;
      _nullValue = value;
      return old;
    }

    int hash = key.hashCode() & _mask;

    for (int count = _size; count >= 0; count--) {
      Object testKey = _keys[hash];

      if (testKey == null) {
	_keys[hash] = key;
	_values[hash] = value;

	_size++;

	if (_keys.length <= 4 * _size)
	  resize(4 * _keys.length);

	return NULL;
      }
      else if (key != testKey && ! testKey.equals(key)) {
	hash = (hash + 1) & _mask;
	continue;
      }
      else {
	int old = _values[hash];

	_values[hash] = value;

	return old;
      }
    }

    return NULL;
  }

  /**
   * Deletes the entry.  Returns true if successful.
   */
  public int remove(Object key)
  {
    if (key == null) {
      int old = _nullValue;
      _nullValue = NULL;
      return old;
    }

    int hash = key.hashCode() & _mask;

    for (int j = _size; j >= 0; j--) {
      Object mapKey = _keys[hash];

      if (mapKey == null)
	return NULL;
      else if (mapKey.equals(key)) {
	_size--;

	_keys[hash] = null;

	int value = _values[hash];

	refillEntries(hash);

	return value;
      }

      hash = (hash + 1) & _mask;
    }

    return NULL;
  }

  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntries(int hash)
  {
    for (int count = _size; count >= 0; count--) {
      hash = (hash + 1) & _mask;

      if (_keys[hash] == null)
	return;

      refillEntry(hash);
    }
  }
  
  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntry(int baseHash)
  {
    Object key = _keys[baseHash];
    int value = _values[baseHash];
    
    int hash = key.hashCode();
    
    for (int count = _size; count >= 0; count--) {
      if (_keys[hash] == null) {
	_keys[hash] = key;
	_values[hash] = value;
	return;
      }

      hash = (hash + 1) & _mask;
    }
  }
  /**
   * Returns an iterator of the keys.
   */
  public Iterator iterator()
  {
    return new IntMapIterator();
  }

  public Object clone()
  {
    IntMap clone = new IntMap(true);

    clone._keys = new Object[_keys.length];
    System.arraycopy(_keys, 0, clone._keys, 0, _keys.length);
    
    clone._values = new int[_values.length];
    System.arraycopy(_values, 0, clone._values, 0, _values.length);
    
    clone._mask = _mask;
    clone._size = _size;

    clone._nullValue = _nullValue;

    return clone;
  }

  public String toString()
  {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("IntMap[");
    boolean isFirst = true;
    for (int i = 0; i <= _mask; i++) {
      if (_keys[i] != null) {
	if (! isFirst)
	  sbuf.append(", ");
	isFirst = false;
	sbuf.append(_keys[i]);
	sbuf.append(":");
	sbuf.append(_values[i]);
      }
    }
    sbuf.append("]");
    return sbuf.toString();
  }

  class IntMapIterator implements Iterator {
    int index;

    public boolean hasNext()
    {
      for (; index < _keys.length; index++)
	if (_keys[index] != null)
	  return true;

      return false;
    }

    public Object next()
    {
      for (; index < _keys.length; index++)
	if (_keys[index] != null)
	  return _keys[index++];

      return null;
    }

    public void remove()
    {
      throw new RuntimeException();
    }
  }
}
