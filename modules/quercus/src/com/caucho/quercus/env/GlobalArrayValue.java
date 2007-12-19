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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.caucho.quercus.env.ArrayValue.Entry;
import com.caucho.quercus.env.ArrayValue.EntryIterator;
import com.caucho.quercus.env.ArrayValue.KeyIterator;
import com.caucho.quercus.env.ArrayValue.ValueIterator;

/**
 * Represents the server
 */
public class GlobalArrayValue extends ArrayValueImpl {
  private final Env _env;

  public GlobalArrayValue(Env env)
  {
    _env = env;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public StringValue toString(Env env)
  {
    return new UnicodeValueImpl("Array");
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    return _env.setGlobalValue(key.toString(), value);
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    return _env.getGlobalValue(key.toString());
  }
  
  /**
   * Returns the array ref.
   */
  public Var getRef(Value key)
  {
    return _env.getGlobalRef(key.toString());
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  public Value getArg(Value index)
  {
    return getRef(index);
  }

  /**
   * Unsets a value.
   */
  @Override
  public Value remove(Value key)
  {
    return _env.unsetGlobalVar(key.toString());
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return this;
  }
  
  /*
   * Returns the size.
   */
  public int getSize()
  {
    return _env.getGlobalEnv().size();
  }
  
  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print("Array");
  }

  /**
   * Returns an iterator of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return createAndFillArray().entrySet();
  }
  
  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return createAndFillArray().getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return createAndFillArray().getKeyIterator(env);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return createAndFillArray().getValueIterator(env);
  }
  
  private ArrayValue createAndFillArray()
  {
    ArrayValue array = new ArrayValueImpl();
    
    for(Map.Entry<String,Var> entry : _env.getGlobalEnv().entrySet()) {
      Value key = _env.createString(entry.getKey());
      Value val = entry.getValue().toValue();
      
      array.put(key, val);
    }
    
    return array;
  }
}

