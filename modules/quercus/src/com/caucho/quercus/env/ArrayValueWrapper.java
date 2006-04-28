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

import java.util.IdentityHashMap;

/**
 * Represents a PHP array value.
 */
public class ArrayValueWrapper extends ArrayValue {
  private ArrayValue _array;

  protected ArrayValueWrapper(ArrayValue array)
  {
    _array = array;
  }

  /**
   * Returns the wrapped array.
   */
  public ArrayValue getArray()
  {
    return _array;
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return _array.copy();
  }
  
  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return _array.copy(env, map);
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    return _array.getSize();
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    _array.clear();
  }
  
  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    return _array.put(key, value);
  }

  /**
   * Add
   */
  public Value put(Value value)
  {
    return _array.put(value);
  }

  /**
   * Add to front.
   */
  public ArrayValue unshift(Value value)
  {
    return _array.unshift(value);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    return _array.getArg(index);
  }

  /**
   * Sets the array ref.
   */
  public Value putRef()
  {
    return _array.putRef();
  }

  /**
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    return _array.createTailKey();
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    return _array.get(key);
  }

  /**
   * Removes a value.
   */
  public Value remove(Value key)
  {
    return _array.remove(key);
  }

  /**
   * Returns the array ref.
   */
  public Var getRef(Value index)
  {
    return _array.getRef(index);
  }
  
  /**
   * Pops the top value.
   */
  public Value pop()
  {
    return _array.pop();
  }

  /**
   * Shuffles the array
   */
  public void shuffle()
  {
    _array.shuffle();
  }

  /**
   * Returns the head.
   */
  public Entry getHead()
  {
    return _array.getHead();
  }

  /**
   * Returns the tail.
   */
  protected Entry getTail()
  {
    return _array.getTail();
  }
  
  /**
   * Returns the current value.
   */
  public Value current()
  {
    return _array.current();
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    return _array.key();
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return _array.hasCurrent();
  }

  /**
   * Returns the next value.
   */
  public Value next()
  {
    return _array.next();
  }

  /**
   * Returns the previous value.
   */
  public Value prev()
  {
    return _array.prev();
  }

  /**
   * The each iterator
   */
  public Value each()
  {
    return _array.each();
  }

  /**
   * Returns the first value.
   */
  public Value reset()
  {
    return _array.reset();
  }

  /**
   * Returns the last value.
   */
  public Value end()
  {
    return _array.end();
  }
  
  /**
   * Returns the corresponding valeu if this array contains the given key
   * 
   * @param key  the key to search for in the array
   * 
   * @return the value if it is found in the array, NULL otherwise
   * 
   * @throws NullPointerException
   */
  public Value containsKey(Value key)
  {
    return _array.containsKey(key);
  }
}

