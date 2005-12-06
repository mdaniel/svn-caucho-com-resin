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
public class CopyArrayValue extends ArrayValue {
  private static final Logger log
    = Logger.getLogger(CopyArrayValue.class.getName());

  private final ConstArrayValue _constArray;
  private ArrayValue _copyArray;

  public CopyArrayValue(ConstArrayValue constArray)
  {
    _constArray = constArray;
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    if (_copyArray != null)
      return _copyArray.toBoolean();
    else
      return _constArray.toBoolean();
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    if (_copyArray != null)
      return _copyArray.copy();
    else
      return _constArray.copy();
  }
  
  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    if (_copyArray != null)
      return _copyArray.copy(env, map);
    else
      return _constArray.copy(env, map);
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    if (_copyArray != null)
      return _copyArray.getSize();
    else
      return _constArray.getSize();
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    getCopyArray().clear();
  }
  
  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    return getCopyArray().put(key, value);
  }

  /**
   * Add
   */
  public ArrayValue append(Value value)
  {
    return getCopyArray().append(value);
  }

  /**
   * Add
   */
  public ArrayValue unshift(Value value)
  {
    return getCopyArray().unshift(value);
  }

  /**
   * Returns the value as an array.
   */
  public Value getArray(Value fieldName)
  {
    return getCopyArray().getArray(fieldName);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    return getCopyArray().getArg(index);
  }

  /**
   * Returns the value as an array.
   */
  public Value getArgArray(Value fieldName)
  {
    return getCopyArray().getArgArray(fieldName);
  }

  /**
   * Returns the value as an object
   */
  public Value getArgObject(Env env, Value fieldName)
  {
    return getCopyArray().getArgObject(env, fieldName);
  }

  /**
   * Returns the field value, creating an object if it's unset.
   */
  public Value getObject(Env env, Value fieldName)
  {
    return getCopyArray().getObject(env, fieldName);
  }

  /**
   * Add
   */
  public ArrayValue put(Value value)
  {
    return getCopyArray().put(value);
  }

  /**
   * Sets the array ref.
   */
  public Value putRef()
  {
    return getCopyArray().putRef();
  }

  /**
   * Add
   */
  public ArrayValue append(Value key, Value value)
  {
    put(key, value.toArgValue());

    return this;
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    if (_copyArray != null)
      return _copyArray.get(key);
    else
      return _constArray.get(key);
  }

  /**
   * Gets a new value.
   */
  public Value containsKey(Value key)
  {
    if (_copyArray != null)
      return _copyArray.containsKey(key);
    else
      return _constArray.containsKey(key);
  }

  /**
   * Removes a value.
   */
  public Value remove(Value key)
  {
    return getCopyArray().remove(key);
  }

  /**
   * Returns the array ref.
   */
  public Var getRef(Value index)
  {
    return getCopyArray().getRef(index);
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, String value)
  {
    put(new StringValue(key), new StringValue(value));
  }

  /**
   * Pops the top value.
   */
  public Value pop()
  {
    return getCopyArray().pop();
  }

  /**
   * Pops the top value.
   */
  public Value createTailKey()
  {
    return getCopyArray().createTailKey();
  }

  /**
   * Shuffles the array
   */
  public void shuffle()
  {
    getCopyArray().shuffle();
  }

  protected Entry getHead()
  {
    if (_copyArray != null)
      return _copyArray.getHead();
    else
      return _constArray.getHead();
  }

  protected Entry getTail()
  {
    if (_copyArray != null)
      return _copyArray.getTail();
    else
      return _constArray.getTail();
  }

  private ArrayValue getCopyArray()
  {
    if (_copyArray == null)
      _copyArray = new ArrayValueImpl(_constArray);

    return _copyArray;
  }
}

