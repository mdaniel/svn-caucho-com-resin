/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.v5.amp.queue;

import sun.misc.Unsafe;

import com.caucho.v5.config.util.UnsafeUtil;
import com.caucho.v5.util.L10N;


final class ArrayRingUnsafe<T> implements ArrayRing<T>
{
  private static final L10N L = new L10N(ArrayRingUnsafe.class);
  
  private static final Unsafe _unsafe;
  private static final int _objectBase;

  private static int _objectScale;
  
  private final T[] _ring;
  private final int _length;
  private final int _mask;
  
  private ArrayRingUnsafe(int length)
  {
    _length = length;
    
    if (Integer.bitCount(_length) != 1) {
      throw new IllegalArgumentException(L.l("Invalid ring capacity {0}",
                                             Long.toHexString(_length)));
    }
    
    _ring = (T[]) new Object[_length];
    _mask = _length - 1;
  }
  
  static <X> ArrayRingUnsafe<X> create(int length)
  {
    if (UnsafeUtil.isEnabled()) {
      return new ArrayRingUnsafe<X>(length);
    }
    else {
      return null;
    }
  }

  @Override
  public final int getLength()
  {
    return _length;
  }
  
  @Override
  public final T get(long index)
  {
    return (T) _unsafe.getObjectVolatile(_ring, getArrayOffset(index));
  }
  
  private final long getArrayOffset(long index)
  {
    return _objectBase + getIndex(index) *  _objectScale;
  }
  
  @Override
  public final void set(long index, T value)
  {
    _unsafe.putObjectVolatile(_ring, getArrayOffset(index), value);
  }
  
  @Override
  public final void setLazy(long index, T value)
  {
    long arrayOffset = getArrayOffset(index);
    
    _unsafe.putObject(_ring, arrayOffset, value);
  }
  
  @Override
  public final T takeAndClear(long index)
  {
    long arrayOffset = getArrayOffset(index);
    
    T oldValue;
    
    while ((oldValue = (T) _unsafe.getObjectVolatile(_ring, arrayOffset)) == null) {
    }
    
    _unsafe.putObjectVolatile(_ring, arrayOffset, null);
    
    return oldValue;
  }
  
  @Override
  public final T pollAndClear(long index)
  {
    throw new UnsupportedOperationException();
  }
  
  public final int getIndex(long ptr)
  {
    return (int) (ptr & _mask);
  }
 
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _ring.getClass().getSimpleName()
            + "," + getLength() + "]");
  }
  
  static {
    _unsafe = UnsafeUtil.getUnsafe();

    int objectOffset = 0;
    int objectScale = 0;
    
    if (_unsafe != null) {
      objectOffset = _unsafe.arrayBaseOffset(Object[].class);
      objectScale = _unsafe.arrayIndexScale(Object[].class);
    }
    
    _objectBase = objectOffset;
    _objectScale = objectScale;
  }
}
