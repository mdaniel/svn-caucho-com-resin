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

package com.caucho.ejb.entity;

import com.caucho.amber.AmberQuery;
import com.caucho.amber.manager.AmberConnection;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
  
/**
 * Represents a lazy collection.
 */
abstract public class CmpCollectionImpl extends AbstractList {
  private ArrayList _base = new ArrayList();
  private AmberQuery _query;

  public void __caucho_init(AmberConnection aConn)
  {
    _query.init(aConn);
  }
  
  protected void fill(AmberQuery query)
  {
    try {
      _query = query;
      
      _base.clear();
      query.list(_base);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public int size()
  {
    return _base.size();
  }
  
  public Object get(int index)
  {
    return _base.get(index);
  }
  
  public boolean add(Object value)
  {
    if (addImpl(value) && ! _base.contains(value)) {
      _base.add(value);
      return true;
    }
    else
      return false;
  }

  protected boolean addImpl(Object value)
  {
    throw new UnsupportedOperationException();
  }
  
  public Object remove(int index)
  {
    Object oldValue = _base.remove(index);

    if (removeImpl(oldValue))
      return oldValue;
    else
      return null;
  }
  
  public boolean remove(Object value)
  {
    return removeImpl(value);
  }

  protected boolean removeImpl(Object oldValue)
  {
    throw new UnsupportedOperationException();
  }
  
  public Iterator iterator()
  {
    return new Itr();
  }

  /**
   * Iterator of the collection's elements.
   */
  public class Itr implements Iterator {
    int _index;
    int _size;

    Itr()
    {
      _size = size();
    }

    /**
     * Returns true if there are more items in the collection.
     */
    public boolean hasNext()
    {
      return _index < _size;
    }
    
    /**
     * Returns the next item in the collection.
     */
    public Object next()
    {
      if (_index < _size)
	return get(_index++);
      else
	return null;
    }
    
    /**
     * Removes the previous item.
     */
    public void remove()
    {
      _index--;
      _size--;
      
      CmpCollectionImpl.this.remove(_index);
    }
  }
}
