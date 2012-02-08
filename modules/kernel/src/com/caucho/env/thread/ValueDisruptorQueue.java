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

import com.caucho.env.thread.DisruptorQueue.ItemProcessor;
import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public class ValueDisruptorQueue<T>
{
  private final DisruptorQueue<ValueItem<T>> _disruptor;
 
  public ValueDisruptorQueue(int capacity,
                             ValueProcessor<T> processor)
  {
    if (processor == null)
      throw new NullPointerException();
    
    _disruptor = new DisruptorQueue<ValueItem<T>>(capacity,
                                 new ValueFactory<T>(),
                                 new ValueItemProcessor<T>(processor));
  }
  
  public void offer(T value)
  {
    ValueItem<T> item = _disruptor.startProducer(true);
    item.init(value);
    _disruptor.finishProducer(item);
  }
  
  public interface ValueProcessor<T> {
    public void process(T value) throws Exception;
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
  
  private static final class ValueFactory<T> 
    implements RingItemFactory<ValueItem<T>>
  {
    public ValueItem<T> createItem(int index)
    {
      return new ValueItem<T>(index);
    }
  }
  
  private static final class ValueItemProcessor<T> 
    extends ItemProcessor<ValueItem<T>>
  {
    private final ValueProcessor<T> _processor;
    
    ValueItemProcessor(ValueProcessor<T> processor)
    {
      _processor = processor;
    }
    
    @Override
    public void process(ValueItem<T> item)
      throws Exception
    {
      T value = item.getAndClear();
      
      _processor.process(value);
    }
  }
}
