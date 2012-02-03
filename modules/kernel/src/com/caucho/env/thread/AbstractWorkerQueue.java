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

package com.caucho.env.thread;

import com.caucho.env.thread.SingleConsumerRing.RingItem;
import com.caucho.env.thread.SingleConsumerRing.TaskFactory;

/**
 * Ring-based memory queue processed by a single worker.
 */
abstract public class AbstractWorkerQueue<T>
{
  private final QueueWorker<T> _queueConsumer;
  
  public AbstractWorkerQueue(int size)
  {
    _queueConsumer = new QueueWorker<T>(size, new QueueFactory());
  }
  
  public final boolean isEmpty()
  {
    return _queueConsumer.isEmpty();
  }
  
  public final int getSize()
  {
    return _queueConsumer.getSize();
  }
  
  public final boolean offer(T value)
  {
    RingItem<T> item = _queueConsumer.startProducer(true);
    
    item.setValue(value);
    
    _queueConsumer.finishProducer(item);
    
    return true;
  }
  
  abstract protected void processValue(T value);
  
  protected void processOnComplete()
  {
    
  }
  
  private class QueueFactory implements TaskFactory<T> {
    @Override
    public T createValue(int index)
    {
      return null;
    }

    @Override
    public void process(RingItem<T> item)
    {
      T value = item.getAndClearValue();

      processValue(value);
    }

    @Override
    public void processOnComplete()
    {
      AbstractWorkerQueue.this.processOnComplete();
    }
   
    @Override
    public String toString()
    {
      return AbstractWorkerQueue.this.getClass().getSimpleName() + ".QueueFactory[]";
    }
  }
}
