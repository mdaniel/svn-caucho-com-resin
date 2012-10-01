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

import com.caucho.env.thread.ActorQueue.ItemProcessor;
import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public class ValueActorQueue<T>
{
  private final ActorQueue<ValueItem<T>> _actorQueue;
 
  public ValueActorQueue(int capacity,
                         ValueProcessor<T> processor)
  {
    if (processor == null)
      throw new NullPointerException();
    
    _actorQueue = new ActorQueue<ValueItem<T>>(capacity,
                                 new ValueItemFactory<T>(),
                                 new ValueItemProcessor<T>(processor));
  }
  
  public boolean isEmpty()
  {
    return _actorQueue.isEmpty();
  }
  
  public int getSize()
  {
    return _actorQueue.getSize();
  }
  
  public final void offer(T value)
  {
    ActorQueue<ValueItem<T>> actorQueue = _actorQueue;
    
    ValueItem<T> item = actorQueue.startOffer(true);
    item.init(value);
    actorQueue.finishOffer(item);
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
    
    return true;
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
  
  public interface ValueProcessor<T> {
    public String getThreadName();
    
    public void process(T value) throws Exception;
    
    public void onProcessComplete() throws Exception;
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
    implements ItemProcessor<ValueItem<T>>
  {
    private final ValueProcessor<T> _processor;
    
    ValueItemProcessor(ValueProcessor<T> processor)
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
