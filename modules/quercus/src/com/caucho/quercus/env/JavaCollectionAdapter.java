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
 * Represents a marshalled Collection argument.
 */
public class JavaCollectionAdapter extends JavaAdapter
{
  private static final Logger log
    = Logger.getLogger(JavaCollectionAdapter.class.getName());

  //XXX: parameterized type
  private Collection _collection;

  public JavaCollectionAdapter(Env env, Collection coll, JavaClassDef def)
  {
    super(env, coll, def);
    
    _collection = coll;
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    _collection.clear();
  }

  public int size()
  {
    return _collection.size();
  }
  
  //
  // Conversions
  //

  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    try {
      Class cl = _collection.getClass();

      Collection coll = (Collection)cl.newInstance();

      coll.addAll(_collection);

      return new JavaCollectionAdapter(getEnv(), coll, getClassDef());
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
   * Creatse a tail index.
   */
  public Value createTailKey()
  {
    return LongValue.create(size());
  }
  
  public Value putImpl(Value key, Value value)
  {
    if (key.toInt() != size())
      throw new UnsupportedOperationException("random assignment into Collection");
    
    _collection.add(value.toJavaObject());
    
    return value;
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _collection) {
      if (pos-- > 0)
        continue;
      
      return wrapJava(obj);
    }
    
    return UnsetValue.UNSET;
  }
  
  /**
   * Removes a value.
   */
  public Value remove(Value key)
  { 
    int i = 0;
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _collection) {
      if (pos-- > 0)
        continue;
      
      Value val = wrapJava(obj);
      
      _collection.remove(obj);
      return val;
    }

    return UnsetValue.UNSET;
  }
  
  /**
   * Returns a set of all the of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new CollectionValueSet();
  }

  /**
   * Returns a collection of the values.
   */
  public Set<Map.Entry> objectEntrySet()
  {
    return new CollectionSet();
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
    for (Object ob: _collection) {
      values[i++] = env.wrapJava(ob);
    }

    return values;
  }

  public class CollectionSet
    extends AbstractSet<Map.Entry>
  {
    CollectionSet()
    {
    }

    public int size()
    {
      return getSize();
    }

    public Iterator<Map.Entry> iterator()
    {
      return new CollectionIterator(_collection);
    }
  }
  
  public class CollectionIterator
    implements Iterator
  {
    private int _index;
    private Iterator _iterator;

    public CollectionIterator(Collection collection)
    {
      _index = 0;
      _iterator = collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry next()
    {
      return new CollectionEntry(_index++, _iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class CollectionEntry
    implements Map.Entry
  {
    private final int _key;
    private Object _value;

    public CollectionEntry(int key, Object value)
    {
      _key = key;
      _value = value;
    }

    public Object getKey()
    {
      return Long.valueOf(_key);
    }

    public Object getValue()
    {
      return _value;
    }

    public Object setValue(Object value)
    {
      Object oldValue = _value;

      _value = value;

      return oldValue;
    }
  }

  public class CollectionValueSet
    extends AbstractSet<Map.Entry<Value,Value>>
  {
    CollectionValueSet()
    {
    }
  
    public int size()
    {
      return getSize();
    }
  
    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new CollectionValueIterator(_collection);
    }
  }

  public class CollectionValueIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private int _index;
    private Iterator _iterator;

    public CollectionValueIterator(Collection collection)
    {
      _index = 0;
      _iterator = collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
       Value val = wrapJava(_iterator.next());

       return new CollectionValueEntry(LongValue.create(_index++), val);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public static class CollectionValueEntry
    implements Map.Entry<Value,Value>
  {
    private final Value _key;
    private Value _value;

    public CollectionValueEntry(Value key, Value value)
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
      return new ValueIterator(_collection);
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
