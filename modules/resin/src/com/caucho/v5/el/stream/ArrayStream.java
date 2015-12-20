/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Paul Cowan
 */

package com.caucho.v5.el.stream;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Represents an array source for the stream()
 */
public class ArrayStream<S> extends AbstractStream<S>
{
  private final S _source;
  
  public ArrayStream(S source)
  {
    _source = source;
  }

  @Override
  public Iterator<S> iterator()
  {
    return new ArrayIterator<S>(_source);
  }
  
  public static class ArrayIterator<S> implements Iterator<S>
  {
    private Object _array;
    private int _index;
    private int _length;

    ArrayIterator(Object array)
    {
      _array = array;
      _length = Array.getLength(array);
    }
    
    public boolean hasNext()
    {
      return _index < _length;
    }
    
    public S next()
    {
      if (_index < _length)
        return (S) Array.get(_array, _index++);
      else
        return null;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}