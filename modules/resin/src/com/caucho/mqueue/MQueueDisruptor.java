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

package com.caucho.mqueue;

import java.util.concurrent.atomic.AtomicInteger;

import com.caucho.env.thread.TaskWorker;

/**
 * Interface for the transaction log.
 */
public final class MQueueDisruptor<T>
{
  private final int _size;
  private final int _mask;
  private final MQueueItem<T> []_itemRing;
  
  private final MQueueItemFactory<T> _factory;
  
  private final AtomicInteger _head = new AtomicInteger();
  
  private final ConsumerWorker<T> _consumerWorker;
  
  private volatile int _sequence = 1;
  private volatile int _tail;
 
  public MQueueDisruptor(int capacity,
                         MQueueItemFactory<T> itemFactory)
  {
    int size = 1;
    
    for (; size < capacity; size *= 2) {
    }
    
    _size = size;
    
    _mask = size - 1;
    
    _itemRing = new MQueueItem[size];
    
    for (int i = 0; i < _size; i++) {
      _itemRing[i] = new MQueueItem<T>(i, itemFactory.create(i));
    }
    
    _factory = itemFactory;
    _consumerWorker = new ConsumerWorker<T>(this);
    }
  
  public final MQueueItem<T> startConsumer()
  {
    int head;
    MQueueItem<T> item;
    int nextHead;
    
    do {
      head = _head.get();
      item = _itemRing[head];
      
      nextHead = (head + 1) & _mask;
      
      // full queue
      if (nextHead == _tail)
        return null;
    } while (! _head.compareAndSet(head, nextHead));
    
    return item;
  }
  
  public final void finishConsumer(MQueueItem<T> item)
  {
    item.setSequence(_sequence + 1);

    _consumerWorker.wake();
  }
  
  private final boolean doConsume()
  {
    int tail = _tail;
    int head = _head.get();
    
    if (head == tail) {
      return false;
    }
    
    MQueueItem<T> item = _itemRing[tail];
      
    int nextTail = tail + 1;
    int sequence = _sequence;
    
    while (item.getSequence() < sequence) {
      // wait for init complete
    }
    
    try {
      _factory.process(item.getValue());
    } finally {
      
      if (_mask < nextTail) {
        _sequence = sequence + 1;
      }
      
      _tail = nextTail & _mask;
    }
      
    return true;
  }
  
  static class ConsumerWorker<T> extends TaskWorker {
    private final MQueueDisruptor<T> _disruptor;
    
    ConsumerWorker(MQueueDisruptor<T> disruptor)
    {
      _disruptor = disruptor;
    }

    @Override
    public long runTask()
    {
      while (_disruptor.doConsume()) {
      }

      return 0;
    }
    
  }
}
