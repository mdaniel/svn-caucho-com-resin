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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single-consumer queue.
 */
public class SingleConsumerRing<T>
{
  private static final Logger log
    = Logger.getLogger(SingleConsumerRing.class.getName());
  
  private final int _size;
  private final int _mask;
  private final RingItem<T> []_itemRing;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicInteger _head = new AtomicInteger();
  
  private final TaskWorker _consumer;
  
  private final int _producerWakeMask;

  private volatile int _tail;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
 
  public SingleConsumerRing(int capacity, 
                            TaskFactory<T> factory,
                            TaskWorker consumer)
  {
    int size = 1;
    
    for (; size < capacity; size *= 2) {
    }
    
    _size = size;
    
    _mask = size - 1;
    
    _itemRing = createRing(size);
    
    _consumer = consumer;
    
    for (int i = 0; i < _size; i++) {
      T value = factory != null ? factory.createValue(i) : null;
      
      _itemRing[i] = new RingItem<T>(i, value);
    }
    
    int wakeMask = (_mask >> 4);
    
    if (0x3f < wakeMask)
      wakeMask = 0x3f;
    
    _producerWakeMask = wakeMask;
  }
  
  public final boolean isEmpty()
  {
    return _head.get() == _tail;
  }
  
  public final int getSize()
  {
    int head = _head.get();
    // int tail = _tailRef.get();
    int tail = _tail;
    
    return (_size + tail - head) % _size;
  }
  
  public final RingItem<T> startProducer(boolean isWait)
  {
    int head;
    RingItem<T> item;
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
          head = -1;
        }
        else {
          return null;
        }
      }
    } while (! _headAlloc.compareAndSet(head, nextHead));
    
    return item;
  }
  
  public final void finishProducer(RingItem<T> item)
  {
    int head = item.getIndex();
    int nextHead = (head + 1) & _mask;
    
    while (! _head.compareAndSet(head, nextHead)) {
    }

    if ((head & _producerWakeMask) == 0) {
      _consumer.wake();
    }
  }
  
  public final void wake()
  {
    _consumer.wake();
  }
  
  @SuppressWarnings("unchecked")
  private RingItem<T> []createRing(int size)
  {
    return new RingItem[size];
  }
  
  private void waitForQueue(int head, int tail)
  {
    _consumer.wake();
    
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
  
  /**
   * Called by the consumer to process all the values.
   */
  public final void consumeAll(TaskFactory<T> factory)
  {
    int count = 4;
    
    for (int i = 0; i < count; i++) {
      try {
        if (doConsume(factory)) {
          i = 0;
        }
      
        factory.processOnComplete();
      } catch (Exception e) {
        e.printStackTrace();
        log.log(Level.FINER, e.toString(), e);
      }
    
      wakeQueue();
    }
  }
  
  /**
   * Returns the next item in the queue, or null if there are
   * no items. Must be called only by the consumer thread, because
   * it is not thread-safe.
   */
  public T take()
  {
    AtomicInteger headRef = _head;
    int head = headRef.get();
    int tail = _tail;
    
    if (head == tail) {
      return null;
    }
    
    RingItem<T> item = _itemRing[tail];
    
    T value = item.getAndClearValue();
  
    _tail = (tail + 1) & _mask;
    
    wakeQueue();
    
    return value;
  }
  
  private final boolean doConsume(TaskFactory<T> factory)
    throws Exception
  {
    AtomicInteger headRef = _head;
    int head = headRef.get();
    int tail = _tail;
    
    if (head == tail) {
      return false;
    }

    RingItem<T> []itemRing = _itemRing;
    
    int mask = _mask;
    int tailCheckMask = (mask >> 3);
    
    int updateMask = (mask >> 6);
    
    try {
      while (true) {
        if (head == tail) {
          head = headRef.get();
        
          if (head == tail) {
            return true;
          }
        }
      
        if ((tail & tailCheckMask) == 0 && _isWait.get()) {
          _tail = tail;
          
          wakeQueue();
        }
    
        RingItem<T> item = itemRing[tail];
      
        tail = (tail + 1) & mask;
        
        factory.process(item);
        
        if ((tail & updateMask) == 0) {
          _tail = tail;
        }
      }
    } finally {
      _tail = tail;
    }
  }
  
  public static class RingItem<T>
  {
    private final int _index;
    
    private T _value;
    
    RingItem(int index, T value)
    {
      _index = index;
      _value = value;
    }
    
    private final int getIndex()
    {
      return _index;
    }
    
    public final T getValue()
    {
      return _value;
    }
    
    public final void setValue(T value)
    {
      _value = value;
    }
    
    public final T getAndClearValue()
    {
      T value = _value;
      
      _value = null;
      
      return value;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _index + "," + _value + "]";
    }
  }
  
  public interface TaskFactory<T> {
    public T createValue(int index);
    
    public void process(RingItem<T> item);
    
    public void processOnComplete();
  }
}
