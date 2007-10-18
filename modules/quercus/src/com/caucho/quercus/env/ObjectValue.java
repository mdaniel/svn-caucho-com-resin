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
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a Quercus object value.
 */
abstract public class ObjectValue extends Value {
 transient protected QuercusClass _quercusClass;

  protected ObjectValue(QuercusClass quercusClass)
  {
    _quercusClass = quercusClass;
  }

  protected void setQuercusClass(QuercusClass cl)
  {
    _quercusClass = cl;
  }

  public QuercusClass getQuercusClass()
  {
    return _quercusClass;
  }

  /**
   * Returns the value's class name.
   */
  public String getClassName()
  {
    return _quercusClass.getName();
  }

  /**
   * Returns a Set of entries.
   */
  // XXX: remove entrySet() and use getIterator() instead
  abstract public Set<? extends Map.Entry<Value,Value>> entrySet();

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _quercusClass.getName();
  }

  /**
   * Returns the parent class
   */
  public String getParentClassName()
  {
    return _quercusClass.getParentName();
  }

  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return true;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "object";
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return _quercusClass.isA(name);
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return 1;
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toLong();
  }

  //
  // Convenience field methods
  //

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, String value)
  {
    Env env = Env.getInstance();
    
    return putThisField(env, env.createString(key), env.createString(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, String value)
  {
    return putThisField(env, env.createString(key), env.createString(value));
  }

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, long value)
  {
    Env env = Env.getInstance();
    
    return putThisField(env, env.createString(key), LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, long value)
  {
    return putThisField(env, env.createString(key), LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, Value value)
  {
    return putThisField(env, env.createString(key), value);
  }

  /**
   * Initializes a new field, does not call __set if it is defined.
   */
  public void initField(StringValue key, Value value)
  {
    putThisField(null, key, value);
  }

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, double value)
  {
    Env env = Env.getInstance();
    
    return putThisField(env, env.createString(key), DoubleValue.create(value));
  }

  /**
   * Returns true for equality
   */
  @Override
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
  public int cmpObject(ObjectValue rValue)
  {
    if (rValue == this)
      return 0;

    // if objects are not equal, then which object is greater is undefined

    int result = getName().compareTo(rValue.getName());
    
    if (result != 0)
      return result;
    
    Set<? extends Map.Entry<Value,Value>> aSet = entrySet();
    Set<? extends Map.Entry<Value,Value>> bSet = rValue.entrySet();
    
    if (aSet.equals(bSet))
      return 0;
    else if (aSet.size() > bSet.size())
      return 1;
    else if (aSet.size() < bSet.size())
      return -1;
    else {
      TreeSet<Map.Entry<Value,Value>> aTree
	= new TreeSet<Map.Entry<Value,Value>>(aSet);

      TreeSet<Map.Entry<Value,Value>> bTree
	= new TreeSet<Map.Entry<Value,Value>>(bSet);

      Iterator<Map.Entry<Value,Value>> iterA = aTree.iterator();
      Iterator<Map.Entry<Value,Value>> iterB = bTree.iterator();

      while (iterA.hasNext()) {
        Map.Entry<Value,Value> a = iterA.next();
        Map.Entry<Value,Value> b = iterB.next();

        result = a.getKey().cmp(b.getKey());

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

  public void varDumpObject(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("object(" + getName() + ") (" + getSize() + ") {");

    ArrayValue sortedEntries = new ArrayValueImpl();

    Iterator<Map.Entry<Value,Value>> iter = getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      sortedEntries.put(entry.getKey(), entry.getValue());
    }

    ArrayModule.ksort(env, sortedEntries, ArrayModule.SORT_STRING);

    iter = sortedEntries.getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();

      Value key = entry.getKey();
      Value value = entry.getValue();

      printDepth(out, 2 * depth);
      out.println("[\"" + key + "\"]=>");

      depth++;

      printDepth(out, 2 * depth);

      value.varDump(env, out, depth, valueSet);

      out.println();
      
      depth--;
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }
}

