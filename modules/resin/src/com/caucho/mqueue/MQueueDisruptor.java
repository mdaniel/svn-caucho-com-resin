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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.TaskWorker;
import com.caucho.env.thread2.TaskWorker2;

/**
 * Interface for the transaction log.
 */
public final class MQueueDisruptor<T>
{
  private static final Logger log
    = Logger.getLogger(MQueueDisruptor.class.getName());
  
  private final int _size;
  private final int _mask;
  private final MQueueItem<T> []_itemRing;
  
  private final MQueueItemFactory<T> _factory;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicInteger _head = new AtomicInteger();
  // private final AtomicInteger _tailRef = new AtomicInteger();
  private volatile int _tail;
  
  private final ConsumerWorker _consumerWorker;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
 
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
    _consumerWorker = new ConsumerWorker();
  }
  
  private final int getSize()
  {
    int head = _head.get();
    // int tail = _tailRef.get();
    int tail = _tail;
    
    return (_size + tail - head) % _size;
  }
  
  public final MQueueItem<T> startProducer(boolean isWait)
  {
    int head;
    MQueueItem<T> item;
    int nextHead;
    
    do {
      head = _headAlloc.get();
      item = _itemRing[head];
      
      nextHead = (head + 1) & _mask;
      
      // full queue
      /*
      if (nextHead == _tail) {
        return null;
      }
      */
      // int tail = _tailRef.get();
      int tail = _tail;
      
      if (nextHead == tail) {
        if (isWait) {
          waitForQueue(head, tail);
        }
        else
          return null;
      }
    } while (! _headAlloc.compareAndSet(head, nextHead));
    
    return item;
  }
  
  private void waitForQueue(int head, int tail)
  {
    _consumerWorker.wake();
    
    synchronized (_isWait) {
      _isWait.set(true);
      
      while (_headAlloc.get() == head 
             && _tail == tail
             && _isWait.get()) {
        try {
          _isWait.wait(10);
        } catch (Exception e) {
        }
      }
    }
  }
  
  private void wakeQueue()
  {
    boolean isWait = _isWait.get();
    
    if (isWait) {
      synchronized (_isWait) {
        _isWait.set(false);
        _isWait.notifyAll();
      }
    }
  }
  
  public final void finishProducer(MQueueItem<T> item)
  {
    // item.setSequence(_sequence + 1);
    
    int head = item.getIndex();
    int nextHead = (head + 1) & _mask;
    
    while (! _head.compareAndSet(head, nextHead)) {
    }

    _consumerWorker.wake();
  }
  
  private final void consumeAll()
  {
    try {
      doConsume();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    wakeQueue();
  }
  
  private final void doConsume()
    throws Exception
  {
    AtomicInteger headRef = _head;
    int head = headRef.get();
    int tail = _tail;
    
    if (head == tail) {
      return;
    }

    MQueueItemFactory<T> factory = _factory;
    
    MQueueItem<T> []itemRing = _itemRing;
    
    int mask = _mask;
    int tailCheckMask = mask >> 1;
    
    try {
      while (true) {
        if (head == tail) {
          head = headRef.get();
        
          if (head == tail) {
            return;
          }
        }
      
        if ((tail & tailCheckMask) == 0 && _isWait.get()) {
          _tail = tail;
          
          wakeQueue();
        }
    
        MQueueItem<T> item = itemRing[tail];
      
        tail = (tail + 1) & mask;
        
        factory.process(item.getValue());
        
        if ((tail & 0x7f) == 0) {
          _tail = tail;
        }
      }
    } finally {
      _tail = tail;
    }
  }

  private final class ConsumerWorker extends TaskWorker {
    @Override
    public final long runTask()
    {/*
      boolean isWait = _isWait.get();
      
      if (isWait) {
        _wakeWorker.wake();
      }
      */
      /*
      int count = -1;
      
      if (isWait) {
        count = _disruptor.getSize() >> 2;
      }
      */

      consumeAll();
      /*
      while (doConsume()) {
        if (count-- == 0) {
          _disruptor.wakeQueue();
        }
      }
      */
      
      // _disruptor.wakeQueue();
      
      /*
      if (_isWait.get()) {
        _wakeWorker.wake();
      }
      */
      
      wakeQueue();

      return 0;
    }
  }
}
