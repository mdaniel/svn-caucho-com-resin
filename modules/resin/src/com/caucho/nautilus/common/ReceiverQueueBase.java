/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.caucho.nautilus.ReceiverQueue;

/**
 * abstract receiver from a message store
 */
abstract public class ReceiverQueueBase<T> implements ReceiverQueue<T> {
  /**
   * Polls the queue with a timeout.
   */
  abstract protected T pollMicros(long timeoutMicros);
  
  protected void onBuild()
  {
  }

  @Override
  public boolean isEmpty()
  {
    return true;
  }

  @Override
  public int size()
  {
    return 0;
  }

  @Override
  public T poll(long timeout, TimeUnit unit)
  {
    return pollMicros(unit.toMicros(timeout));
  }
  
  @Override
  public T take() throws InterruptedException
  {
    return pollMicros(Integer.MAX_VALUE);
  }

  @Override
  public T element()
  {
    return peek();
  }

  @Override
  public T peek()
  {
    return null;
  }

  @Override
  public T poll()
  {
    return pollMicros(0);
  }

  @Override
  public T remove()
  {
    return null;
  }
  
  public long getLastMessageId()
  {
    return 0;
  }

  @Override
  public void accepted(long mid)
  {
  }

  @Override
  public void rejected(long mid, String errorMessage)
  {
  }

  @Override
  public void released(long mid)
  {
  }

  @Override
  public void modified(long mid,
                       boolean isFailed, 
                       boolean isUndeliverableHere)
  {
  }

  public void close()
  {
    
  }
  
  //
  // non-receiving
  //

  @Override
  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean addAll(Collection<? extends T> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void put(T e) throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());

  }

  @Override
  public int remainingCapacity()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Iterator<T> iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <X> X[] toArray(X[] a)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean add(T e)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean contains(Object o)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> c, int maxElements)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean offer(T e)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean offer(T e, long timeout, TimeUnit unit)
      throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
