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

package com.caucho.env.actor2;

import java.util.concurrent.TimeUnit;

import com.caucho.env.actor.ActorProcessor;
import com.caucho.env.actor.ActorQueueApi;
import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.util.L10N;


/**
 * Interface for the transaction log.
 */
public class ActorQueue2MultiWorker<T> implements ActorQueueApi<T>
{
  private static final L10N L = new L10N(ActorQueue2MultiWorker.class);
  
  private final QueueRing<T> _actorQueue;
  private final ActorWorker<T> []_workers;
 
  public ActorQueue2MultiWorker(int capacity,
                                int offset,
                                ActorProcessor<? super T> ...processors)
  {
    if (processors == null || processors.length == 0)
      throw new NullPointerException();
    
    _actorQueue = new QueueRingFixed<T>(capacity);
    
    _workers = new ActorWorker[processors.length];
    
    for (int i = 0; i < processors.length; i++) {
      _workers[i] = new ActorWorker<T>(_actorQueue, processors[i]);
    }
  }
  
  @Override
  public int getAvailable()
  {
    return _actorQueue.remainingCapacity();
  }
  
  @Override
  public boolean isEmpty()
  {
    return _actorQueue.isEmpty();
  }
  
  @Override
  public int getSize()
  {
    return _actorQueue.size();
  }
  
  @Override
  public final void offer(T value)
  {
    offer(value, true);
  }
  
  public final boolean offer(T value, boolean isWait)
  {
    QueueRing<T> queue = _actorQueue;
    
    if (! queue.offer(value, 0, TimeUnit.SECONDS)) {
      if (! isWait) {
        return false;
      }
      
      wake();
      
      if (! queue.offer(value, 5, TimeUnit.MINUTES)) {
        throw new IllegalStateException(L.l("offer timeout {0} {1}", this, value));
      }
    }
    
    wake();
    
    return true;
  }

  public String getWorkerState()
  {
    return _workers[0].getState();
  }

  @Override
  public void wake()
  {
    int len = _actorQueue.size();
    
    for (int i = 0; i < len; i++) {
      _workers[i].wake();
    }
  }
  
  public void close()
  {
    // _disruptor.close();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _actorQueue + "]";
  }
  private static class ActorWorker<T>
    extends AbstractTaskWorker
  {
    private final ActorProcessor<? super T> _processor;
    private final QueueRing<T> _queue;

    ActorWorker(QueueRing<T> queue, ActorProcessor<? super T> processor)
    {
      _queue = queue;
      _processor = processor;
    }

    @Override
    protected String getThreadName()
    {
      return _processor.getThreadName();
    }

    @Override
    protected boolean isRetry()
    {
      return ! _queue.isEmpty();
    }

    @Override
    public final long runTask()
    {
      try {
        try {
          _processor.onProcessStart();
        
          T item;
          
          while ((item = _queue.poll()) != null) {
            _processor.process(item);
          }
        } finally {
          _processor.onProcessComplete();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return 0;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _processor + "]";
    }
  }
}
