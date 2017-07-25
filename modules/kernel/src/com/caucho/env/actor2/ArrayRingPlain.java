/*
 * Copyright (c) 1998-2017 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(TM)
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
 * along with Resin; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.actor2;

import com.caucho.util.L10N;

final class ArrayRingPlain<T> implements ArrayRing<T>
{
  private static final L10N L = new L10N(ArrayRingPlain.class);

  private final T []_ring;
  private final int _length;
  private final int _mask;

  ArrayRingPlain(int length)
  {
    _length = length;

    if (Integer.bitCount(_length) != 1) {
      throw new IllegalArgumentException(L.l("Invalid ring capacity {0}",
                                             Long.toHexString(_length)));
    }

    _ring = (T[]) new Object[_length];
    _mask = _length - 1;
  }

  @Override
  public final int getLength()
  {
    return _length;
  }

  @Override
  public final T get(long index)
  {
    return _ring[getIndex(index)];
  }

  @Override
  public final void set(long ptr, T value)
  {
    int index = getIndex(ptr);

     _ring[index] = value;
  }

  @Override
  public final void setLazy(long ptr , T value)
  {
    _ring[getIndex(ptr)] = value;
  }

  @Override
  public final T takeAndClear(long ptr)
  {
    final T []ring = _ring;
    final int index = getIndex(ptr);

    T value;

    while ((value = ring[index]) == null) {
    }
    ring[index] = null;

    return value;
  }

  @Override
  public final void clear(long ptr, long end)
  {
    final T []ring = _ring;

    for (; ptr < end; ptr++) {
      ring[getIndex(ptr)] = null;
    }
  }

  @Override
  public final T pollAndClear(long ptr)
  {
    final T []ring = _ring;
    final int index = getIndex(ptr);

    T value = ring[index];

    if (value != null && ring[index] == value) {
      ring[index] = null;

      return value;
    }
    else {
      return null;
    }
  }

  @Override
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
