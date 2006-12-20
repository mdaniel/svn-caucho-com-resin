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

import com.caucho.quercus.program.JavaClassDef;

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.UnimplementedException;

import java.util.*;
import java.util.logging.*;

/**
 * Represents a marshalled Map argument.
 */
public class JavaMapAdapter
  extends JavaAdapter
{
  private static final Logger log
    = Logger.getLogger(JavaMapAdapter.class.getName());
  
  //XXX: parameterized type
  private Map _map;
  
  private long _nextAvailableIndex;

  public JavaMapAdapter(Env env, Map map)
  {
    this(env, map, env.getJavaClassDefinition(map.getClass().getName()));
  }
  
  public JavaMapAdapter(Env env, Map map, JavaClassDef def)
  {
    super(env, map, def);
    _map = map;
    
    updateNextAvailableIndex();
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    _map.clear();
    
    _nextAvailableIndex = 0;
  }

  public int size()
  {
    return _map.size();
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObject(Env env, Class type)
  {
    if (type.isAssignableFrom(_map.getClass())) {
      return _map;
    }
    else {
      env.warning(L.l("Can't assign {0} to {1}",
              _map.getClass().getName(), type.getName()));
    
      return null;
    }
  }

  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    try {
      Class cl = _map.getClass();

      Map map = (Map)cl.newInstance();

      map.putAll(_map);

      return new JavaMapAdapter(getEnv(), map, getClassDef());
    }
    catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Returns the size.
   */
  public int getSize()
  {
    return size();
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    Object obj = _map.get(key.toJavaObject());
    
    if (obj != null)
      return wrapJava(_map.get(key.toJavaObject()));
    else
      return UnsetValue.UNSET;
  }
  
  /**
   * Removes a value.
   */
  public Value remove(Value key)
  {
    updateNextAvailableIndex();

    if (key.isLongConvertible() || key instanceof BooleanValue) {
    //if (key instanceof LongValue) {
      long pos = key.toLong();
      
      Object value = _map.remove(Long.valueOf(pos));
      
      if (value != null) {
        if (pos + 1 == _nextAvailableIndex)
          updateNextAvailableIndex();
        
        return wrapJava(value);
      }
    }
    else {
      Object value = _map.remove(key.toJavaObject());
      
      if (value != null)
        return wrapJava(value);
    }
    
    return UnsetValue.UNSET;
  }
  
  /**
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    updateNextAvailableIndex();
    return LongValue.create(_nextAvailableIndex);
  }
  
  /**
   * Adds a new value.
   */
  public Value putImpl(Value key, Value value)
  {
    Object keyObject;
    
    if (key.isLongConvertible() || key instanceof BooleanValue) {
      keyObject = Long.valueOf(key.toLong());
    }
    else {
      keyObject = key.toJavaObject();
    }

    Value val = wrapJava(_map.put(keyObject, value.toJavaObject()));

    updateNextAvailableIndex(keyObject);
    
    return val;
  }
  
  /**
   * Returns a set of all the of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new MapSet();
  }
  
  /**
   * Returns a collection of the values.
   */
  public Set<Map.Entry> objectEntrySet()
  {
    return _map.entrySet();
  }

  /**
   * Returns a collection of the values.
   */
  public Collection<Value> values()
  {
    return new ValueCollection();
  }

  public Value []getValueArray(Env env)
  {
    Value[] values = new Value[getSize()];

    int i = 0;
    for (Object entry: _map.values()) {
      values[i++] = env.wrapJava(entry);
    }

    return values;
  }
  
  /**
   * Updates _nextAvailableIndex on a remove of the highest value
   */
  private void updateNextAvailableIndex()
  {
    _nextAvailableIndex = 0;

    for (Object key : _map.keySet()) {
      updateNextAvailableIndex(key);
    }
  }
  
  /**
   * Updates _nextAvailableIndex.
   */
  private void updateNextAvailableIndex(Object objectKey)
  { 
    if (objectKey instanceof Long) {
      long key = ((Long)objectKey).longValue();
    
      if (_nextAvailableIndex <= key)
        _nextAvailableIndex = key + 1;
    }
  }

  public class MapSet
    extends AbstractSet<Map.Entry<Value,Value>>
  {
    MapSet()
    {
    }

    public int size()
    {
      return getSize();
    }

    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new MapIterator(_map);
    }
  }

  public class MapIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private Iterator<Map.Entry> _iterator;

    public MapIterator(Map map)
    {
      _iterator = map.entrySet().iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
      Map.Entry entry = _iterator.next();
      
      Value key = wrapJava(entry.getKey());
      Value value = wrapJava(entry.getValue());

      return new MapEntry(key, value);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class MapEntry
    implements Map.Entry<Value,Value>
  {
    private final Value _key;
    private Value _value;

    public MapEntry(Value key, Value value)
    {
      _key = key;
      _value = value;
    }

    public Value getKey()
    {
      return _key;
    }

    public Value getValue()
    {
      return _value;
    }

    public Value setValue(Value value)
    {
      Value oldValue = _value;

      _value = value;

      return oldValue;
    }
  }

  public class ValueCollection
    extends AbstractCollection<Value>
  {
    ValueCollection()
    {
    }

    public int size()
    {
      return getSize();
    }

    public Iterator<Value> iterator()
    {
      return new ValueIterator(_map.values());
    }
  }

  public class ValueIterator
    implements Iterator<Value>
  {
    private Iterator _iterator;

    public ValueIterator(Collection collection)
    {
      _iterator = collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Value next()
    {
      return wrapJava(_iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
