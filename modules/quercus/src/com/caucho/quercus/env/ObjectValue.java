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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a Quercus object value.
 */
abstract public class ObjectValue extends Value {

  /**
   * Returns a Set of entries.
   */
  abstract public Set<Map.Entry<String,Value>> entrySet();

  /**
   * Returns the class name.
   */
  abstract public String getName();

  /**
   * Returns the parent class
   */
  abstract public String getParentName();

  /**
   * Returns true for an object.
   */
  public boolean isObject()
  {
    return true;
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, String value)
  {
    return putField(null, key, new UnicodeValueImpl(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, long value)
  {
    return putField(null, key, LongValue.create(value));
  }
  
  /**
   * Initializes a new field, does not call __set if it is defined.
   */
  public Value initField(Env env, String key, Value value)
  {
    return putField(env, key, value);
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, double value)
  {
    return putField(null, key, DoubleValue.create(value));
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    if (rValue.isObject())
      return cmpObject((ObjectValue)rValue) == 0;
    else
      return super.eq(rValue);
  }
  
  /**
   * Compare two objects
   */
  public final int cmpObject(ObjectValue rValue)
  {
    // if objects are not equal, then which object is greater is undefined

    int result = getName().compareTo(rValue.getName());
    
    if (result != 0)
      return result;
    
    Set<Map.Entry<String,Value>> aSet = entrySet();
    Set<Map.Entry<String,Value>> bSet = rValue.entrySet();
    
    if (aSet.equals(bSet))
      return 0;
    else if (aSet.size() > bSet.size())
      return 1;
    else if (aSet.size() < bSet.size())
      return -1;
    else {
      TreeSet<Map.Entry<String,Value>> aTree
      = new TreeSet<Map.Entry<String,Value>>(aSet);

      TreeSet<Map.Entry<String,Value>> bTree
      = new TreeSet<Map.Entry<String,Value>>(bSet);

      Iterator<Map.Entry<String,Value>> iterA = aTree.iterator();
      Iterator<Map.Entry<String,Value>> iterB = bTree.iterator();

      while (iterA.hasNext()) {
        Map.Entry<String,Value> a = iterA.next();
        Map.Entry<String,Value> b = iterB.next();

        result = a.getKey().compareTo(b.getKey());

        if (result != 0)
          return result;
        
        result = a.getValue().cmp(b.getValue());

        if (result != 0)
          return result;
      }

      // should never reach this
      return 0;
    }
  }

}

