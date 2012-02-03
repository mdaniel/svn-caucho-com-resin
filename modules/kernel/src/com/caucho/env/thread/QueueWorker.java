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
 * Single-consumer queue.
 */
public class QueueWorker<T>
{
  private final SingleConsumerRing<T> _ring;
  private final TaskFactory<T> _factory;
  private final AbstractTaskWorker _consumerWorker;
 
  public QueueWorker(int capacity, TaskFactory<T> factory)
  {
    _factory = factory;
    _consumerWorker = new ConsumerWorker();
    _ring = new SingleConsumerRing<T>(capacity, factory, _consumerWorker);
  }
  
  public final boolean isEmpty()
  {
    return _ring.isEmpty();
  }
  
  public final int getSize()
  {
    return _ring.getSize();
  }
  
  public final RingItem<T> startProducer(boolean isWait)
  {
    return _ring.startProducer(isWait);
  }
  
  public final void finishProducer(RingItem<T> item)
  {
    _ring.finishProducer(item);
  }

  private final class ConsumerWorker extends AbstractTaskWorker {
    @Override
    public final long runTask()
    {
      _ring.consumeAll(_factory);

      return 0;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _factory + "]";
    }
  }
}
