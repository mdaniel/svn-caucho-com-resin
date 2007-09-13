/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.quercus.env;

import java.util.Map;
import java.util.Iterator;

/**
 * A delegate that performs Array operations for Quercus objects.
 */
abstract public class ArrayDelegate {
  private ArrayDelegate _next;

  public ArrayDelegate()
  {
  }

  public void init(ArrayDelegate next)
  {
    _next = next;
  }

  /**
   * Returns the value for the specified key.
   */
  public Value get(Env env, ObjectValue obj, Value key)
  {
    return _next.get(env, obj, key);
  }

  /**
   * Returns the key => value pairs.
   */
  public Iterator<Map.Entry<Value,Value>> getIterator(Env env, ObjectValue obj)
  {
    return _next.getIterator(env, obj);
  }

  /**
   * Returns the keys.
   */
  public Iterator<Value> getKeyIterator(Env env, ObjectValue obj)
  {
    return _next.getKeyIterator(env, obj);
  }

  /**
   * Returns the values.
   * A return value of null causes the object to use it's default behaviour
   * for iteration.
   */
  public Iterator<Value> getValueIterator(Env env, ObjectValue obj)
  {
    return _next.getValueIterator(env, obj);
  }

  /**
   * Returns the value for the count() function.
   */
  public int getCount(Env env, ObjectValue obj)
  {
    return _next.getCount(env, obj);
  }

  /**
   * Returns the value for the count() function.
   */
  public int getCountRecursive(Env env, ObjectValue obj)
  {
    return _next.getCountRecursive(env, obj);
  }

  /**
   * Sets the value for the spoecified key.
   */
  public Value put(Env env, ObjectValue obj, Value key, Value value)
  {
    return _next.put(env, obj, key, value);
  }

  /**
   * Appends a value.
   */
  public Value put(Env env, ObjectValue obj, Value value)
  {
    return _next.put(env, obj, value);
  }

  /**
   * Removes the value at the speified key.
   */
  public Value remove(Env env, ObjectValue obj, Value key)
  {
    return _next.remove(env, obj, key);
  }
}
