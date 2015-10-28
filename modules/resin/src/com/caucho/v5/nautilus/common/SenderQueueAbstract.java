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

package com.caucho.v5.nautilus.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.caucho.v5.nautilus.MessagePropertiesFactory;
import com.caucho.v5.nautilus.SenderQueue;

/**
 * local connection to the message store
 */
abstract public class SenderQueueAbstract<T> implements BlockingQueue<T> {
  private static final MessagePropertiesFactory<?> NULL_FACTORY = new MessageFactoryNull();
  
  /**
   * Offers a value to the queue.
   */
  abstract protected boolean offerMicros(MessagePropertiesFactory<T> factory,
                                         T value,
                                         long timeoutMicros);
  
  protected MessagePropertiesFactory<T> getMessageFactory()
  {
    return (MessagePropertiesFactory) NULL_FACTORY;
  }

  @Override
  public boolean add(T value)
  {
    return offer(value);
  }

  @Override
  public boolean offer(T value)
  {
    MessagePropertiesFactory<T> factory = getMessageFactory();
    
    return offerMicros(factory, value, 0);
  }

  @Override
  public boolean offer(T value, long timeout, TimeUnit timeUnit)
  {
    MessagePropertiesFactory<T> factory = getMessageFactory();
    
    return offerMicros(factory, value, timeUnit.toMicros(timeout));
  }

  @Override
  public void put(T value) throws InterruptedException
  {
    MessagePropertiesFactory<T> factory = getMessageFactory();
    
    offerMicros(factory, value, 0);
  }

  @Override
  public boolean addAll(Collection<? extends T> arg0)
  {
    return false;
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }

  //
  // receiver side
  //

  @Override
  public T poll(long arg0, TimeUnit arg1) throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean remove(Object arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T take() throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T element()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T poll()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T remove()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean containsAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean isEmpty()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Iterator<T> iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean removeAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean retainAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int size()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <X> X[] toArray(X[] arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean contains(Object arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> arg0, int arg1)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
