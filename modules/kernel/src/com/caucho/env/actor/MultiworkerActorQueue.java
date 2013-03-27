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

package com.caucho.env.actor;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.util.RingValueQueue;


/**
 * Interface for the transaction log.
 */
public class MultiworkerActorQueue<T> implements ActorQueueApi<T>
{
  private static final Logger log
    = Logger.getLogger(MultiworkerActorQueue.class.getName());
  
  private final RingValueQueue<T> _ringQueue;
  private final int _multiworkerOffset;
  
  private final ActorProcessor<? super T> []_processors;
  private final ActorWorker<T> []_workers;
 
  public MultiworkerActorQueue(int capacity,
                               int multiworkerOffset,
                               ActorProcessor<? super T> ...processors)
  {
    if (processors == null)
      throw new NullPointerException();
    
    _processors = processors;
    
    _ringQueue = new RingValueQueue<T>(capacity);
    
    _workers = new ActorWorker[processors.length];
    
    for (int i = 0; i < _workers.length; i++) {
      _workers[i] = new ActorWorker(_ringQueue, processors[i]);
    }
    
    _multiworkerOffset = Math.max(1, multiworkerOffset);
    
  }
  
  @Override
  public int getAvailable()
  {
    return _ringQueue.getCapacity() - _ringQueue.size();
  }
  
  @Override
  public boolean isEmpty()
  {
    return _ringQueue.isEmpty();
  }
  
  @Override
  public int getSize()
  {
    return _ringQueue.size();
  }
  
  @Override
  public final void offer(T value)
  {
    offer(value, true);
  }
  
  public final boolean offer(T value, boolean isWait)
  {
    boolean result =  _ringQueue.offer(value, isWait ? 600 * 1000L : 0, 
                                       TimeUnit.MILLISECONDS);
    
    wake();
    
    return result;
  }
  
  @Override
  public void wake()
  {
    int size = getSize();
    
    int count = (size + _multiworkerOffset - 1) / _multiworkerOffset;
    
    for (int i = 0; i < count && i < _workers.length; i++) {
      _workers[i].wake();
    }
  }
  
  public void close()
  {
    // _disruptor.close();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _processors[0] + "]";
  }
  
  private static class ActorWorker<T> extends AbstractTaskWorker {
    private final RingValueQueue<T> _queue;
    private final ActorProcessor<? super T> _processor;
    
    ActorWorker(RingValueQueue<T> queue,
                ActorProcessor<? super T> processor)
    {
      _queue = queue;
      _processor = processor;
    }
    
    @Override
    public long runTask()
    {
      T value;
      
      try {
        while ((value = _queue.poll()) != null) {
          _processor.process(value);
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        try {
          _processor.onProcessComplete();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      return 0;
    }
  }
}
