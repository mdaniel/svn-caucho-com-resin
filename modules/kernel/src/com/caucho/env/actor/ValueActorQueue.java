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

import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public class ValueActorQueue<T> implements ActorQueueApi<T>
{
  private final ActorQueue<ValueItem<T>> _actorQueue;
 
  public ValueActorQueue(int capacity,
                         ActorProcessor<? super T> ...processors)
  {
    if (processors == null)
      throw new NullPointerException();
    
    ActorProcessor<ValueItem<T>> []valueProcessors;
    
    valueProcessors = new ActorProcessor[processors.length];
    
    for (int i = 0; i < processors.length; i++) {
      valueProcessors[i] = new ValueItemProcessor(processors[i]);
    }
    
    _actorQueue = new ActorQueue<ValueItem<T>>(capacity,
                                               new ValueItemFactory<T>(),
                                               valueProcessors);
  }
  
  @Override
  public int getAvailable()
  {
    return _actorQueue.getAvailable();
  }
  
  @Override
  public boolean isEmpty()
  {
    return _actorQueue.isEmpty();
  }
  
  @Override
  public int getSize()
  {
    return _actorQueue.getSize();
  }
  
  @Override
  public final void offer(T value)
  {
    ActorQueue<ValueItem<T>> actorQueue = _actorQueue;
    
    ValueItem<T> item = actorQueue.startOffer(true);
    item.init(value);
    actorQueue.finishOffer(item);
    
    wake();
  }
  
  public final boolean offer(T value, boolean isWait)
  {
    ActorQueue<ValueItem<T>> actorQueue = _actorQueue;
    
    ValueItem<T> item = actorQueue.startOffer(isWait);
    
    if (item == null) {
      return false;
    }
    
    item.init(value);
    actorQueue.finishOffer(item);
    
    wake();
    
    return true;
  }

  public String getWorkerState()
  {
    return _actorQueue.getWorkerState();
  }

  public void wake()
  {
    _actorQueue.wake();
  }
  
  public void close()
  {
    // _disruptor.close();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _actorQueue + "]";
  }
  
  private static final class ValueItem<T> extends RingItem {
    private T _value;
    
    ValueItem(int index)
    {
      super(index);
    }
    
    public void init(T value)
    {
      _value = value;
    }
    
    public T getAndClear()
    {
      T value = _value;
      _value = null;
      return value;
    }
  }
  
  private static final class ValueItemFactory<T> 
    implements RingItemFactory<ValueItem<T>>
  {
    @Override
    public ValueItem<T> createItem(int index)
    {
      return new ValueItem<T>(index);
    }
  }
  
  private static final class ValueItemProcessor<T> 
    extends AbstractActorProcessor<ValueItem<T>>
  {
    private final ActorProcessor<T> _processor;
    
    ValueItemProcessor(ActorProcessor<T> processor)
    {
      _processor = processor;
    }

    @Override
    public String getThreadName()
    {
      return _processor.getThreadName();
    }
    
    @Override
    public void process(ValueItem<T> item)
      throws Exception
    {
      T value = item.getAndClear();
      
      _processor.process(value);
    }

    @Override
    public void onProcessComplete() throws Exception
    {
      _processor.onProcessComplete();
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _processor + "]";
    }
  }
}
