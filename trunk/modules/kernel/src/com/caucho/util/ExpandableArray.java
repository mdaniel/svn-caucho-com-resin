/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.util;

import java.lang.reflect.Array;

/**
 * Expandable array which allows access to the underlying array.
 */

public final class ExpandableArray<T> {
  private final int INITIAL_SIZE = 8;
  private final int CHUNK_SIZE = 8192;
  
  private final Class<T> _type;
  
  private T []_data;
  private int _size;
  
  public ExpandableArray(Class<T> type)
  {
    _type = type;
    _data = createArray(INITIAL_SIZE);
  }
  
  public final int getCapacity()
  {
    return _data.length;
  }
  
  public int getSize()
  {
    return _size;
  }
  
  public final T []getArray()
  {
    return _data;
  }
  
  public final void add(T value)
  {
    T []data = _data;
    int size = _size;
    
    int capacity = data.length;
    
    if (capacity <= size) {
      expandData(size + 1);
      data = _data;
      size = _size;
    }
    
    data[size] = value;
    
    _size = size + 1;
  }
  
  public final void remove(int index)
  {
    T []data = _data;
    int size = _size;
    
    System.arraycopy(data, index + 1, data, index, size - index - 1);
    
    _size = size - 1;
  }
  
  private void expandData(int requiredCapacity)
  {
    int capacity;
    
    if (CHUNK_SIZE < requiredCapacity) {
      capacity = (requiredCapacity + CHUNK_SIZE - 1) & ~CHUNK_SIZE;
    }
    else {
      capacity = _data.length;
      
      while (capacity < requiredCapacity) {
        capacity = 2 * capacity;
      }
    }
    
    T []newData = createArray(capacity);
    
    System.arraycopy(_data, 0, newData, 0, _size);
    
    _data = newData;
  }
  
  @SuppressWarnings("unchecked")
  private T []createArray(int size)
  {
    return (T []) Array.newInstance(_type, size);
  }
}
