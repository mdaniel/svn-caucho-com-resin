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

import java.util.Iterator;
import java.util.Map;

/**
 * A factory that produces iterators for Quercus objects.
 */
abstract public class AbstractDelegate {
  private AbstractDelegate _next;

  public AbstractDelegate()
  {
  }

  public void init(AbstractDelegate next)
  {
    _next = next;
  }

  /**
   * Returns the value for the count() function.
   */
  public LongValue getCount(Env env,
                            Value obj,
                            boolean isRecursive)
  {
    return _next.getCount(env, obj, isRecursive);
  }

  public boolean offsetExists(Env env, Value obj, Value offset)
  {
    return _next.offsetExists(env, obj, offset);
  }

  public Value offsetSet(Env env, Value obj, Value offset, Value value)
  {
    return _next.offsetSet(env, obj, offset, value);
  }

  public Value offsetGet(Env env, Value obj, Value offset)
  {
    return _next.offsetGet(env, obj, offset);
  }

  public Value offsetUnset(Env env, Value obj, Value offset)
  {
    return _next.offsetUnset(env, obj, offset);
  }
  
  /**
   * Returns the key => value pairs.
   */
  public Iterator<Map.Entry<Value,Value>> getIterator(Env env, Value obj)
  {
    return _next.getIterator(env, obj);
  }

  /**
   * Returns the keys.
   */
  public Iterator<Value> getKeyIterator(Env env, Value obj)
  {
    return _next.getKeyIterator(env, obj);
  }

  /**
   * Returns the values.
   */
  public Iterator<Value> getValueIterator(Env env, Value obj)
  {
    return _next.getValueIterator(env, obj);
  }

}
