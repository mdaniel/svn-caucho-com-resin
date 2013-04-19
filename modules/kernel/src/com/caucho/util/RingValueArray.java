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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.concurrent.atomic.AtomicReferenceArray;


final class RingValueArray<T>
{
  private static final L10N L = new L10N(RingValueArray.class);

  private final AtomicReferenceArray<T> _ring;
  private final int _length;
  private final int _mask;

  RingValueArray(int length)
  {
    _length = length;

    if (Integer.bitCount(_length) != 1) {
      throw new IllegalArgumentException(L.l("Invalid ring capacity {0}",
                                             Long.toHexString(_length)));
    }

    _ring = new AtomicReferenceArray<T>(_length);
    _mask = _length - 1;
  }

  public final int getLength()
  {
    return _length;
  }

  public final T get(long ptr)
  {
    return _ring.get(getIndex(ptr));
  }

  public final void set(long ptr, T value)
  {
    _ring.set(getIndex(ptr), value);
  }

  public final T takeAndClear(long ptr)
  {
    final int index = getIndex(ptr);
    final AtomicReferenceArray<T> ring = _ring;
    
    T value;
    
    while ((value = ring.getAndSet(index, null)) == null) {
    }
    
    return value;
  }

  public final T pollAndClear(long ptr)
  {
    final int index = getIndex(ptr);
    final AtomicReferenceArray<T> ring = _ring;
    
    T value = ring.get(index);
    
    if (value != null && ring.compareAndSet(index, value, null)) {
      return value;
    }
    else {
      return null;
    }
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
}
