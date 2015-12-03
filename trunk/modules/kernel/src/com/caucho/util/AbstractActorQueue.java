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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.caucho.util.L10N;

/**
 * Value queue with atomic reference.
 */
abstract public class AbstractActorQueue<M> implements ActorQueue<M> {
  private static final L10N L = new L10N(AbstractActorQueue.class);

  //
  // general size information
  //

  @Override
  public boolean isEmpty()
  {
    return size() == 0;
  }

  @Override
  public int size()
  {
    return 0;
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }


  //
  // membership
  //

  @Override
  public boolean contains(Object o)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }


  @Override
  public boolean containsAll(Collection<?> c)
  {
    for (Object o : c) {
      if (! contains(o)) {
        return false;
      }
    }

    return true;
  }

  //
  // offering a new item
  //

  @Override
  public boolean offer(M value)
  {
    return offer(value, 0, TimeUnit.SECONDS);
  }

  @Override
  public boolean offer(M value, long timeout, TimeUnit unit)
  {
    return offer(value, timeout, unit, 0);
  }

  @Override
  public boolean offer(M value, long timeout, TimeUnit unit, int reservedSpace)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void put(M value) throws InterruptedException
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  @Override
  public boolean add(M value)
  {
    if (! offer(value)) {
      throw new IllegalStateException(L.l("No space in queue {0}", this));
    }

    return false;
  }

  @Override
  public boolean addAll(Collection<? extends M> c)
  {
    for (M item : c) {
      if (! add(item)) {
        return false;
      }
    }

    return true;
  }


  //
  // retrieving elements
  //

  @Override
  public M peek()
  {
    return null;
  }

  @Override
  public M element()
  {
    M value = peek();

    if (value == null) {
      throw new IllegalStateException(L.l("No value available for {0}", this));
    }

    return value;
  }

  @Override
  public M poll(long timeout, TimeUnit unit)
  {
    return null;
  }

  @Override
  public M poll()
  {
    return poll(0, TimeUnit.SECONDS);
  }

  @Override
  public M remove()
  {
    M value = poll();

    if (value == null) {
      throw new IllegalStateException(L.l("No value available for {0}", this));
    }

    return null;
  }

  @Override
  public M take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  //
  // misc blocking queue methods
  //

  @Override
  public int drainTo(Collection<? super M> c)
  {
    return 0;
  }

  @Override
  public int drainTo(Collection<? super M> collection,
                     int maxElements)
  {
    return 0;
  }

  @Override
  public Iterator<M> iterator()
  {
    ArrayList<M> list = new ArrayList<M>();

    for (Object item : toArray()) {
      list.add((M) item);
    }

    return list.iterator();
  }

  @Override
  public Object[] toArray()
  {
    Object []array = new Object[size()];

    return toArray(array);
  }

  @Override
  public <X> X[] toArray(X[] a)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean remove(Object o)
  {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    for (Object value : c) {
      if (! remove(value)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
