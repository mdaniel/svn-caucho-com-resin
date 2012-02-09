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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public final class DisruptorQueue<T extends RingItem>
  extends DisruptorIndex
{
  private static final Logger log
    = Logger.getLogger(DisruptorQueue.class.getName());
  
  private static final AtomicIntegerFieldUpdater<DisruptorQueue<?>> _headUpdater;
  
  private final int _size;
  private final int _mask;
  private final T []_itemRing;
  
  private final DisruptorWorker<? super T> _firstWorker;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private volatile int _head;
  private final DisruptorIndex _tailRef;
  
  private final AtomicBoolean _isWaitRef = new AtomicBoolean();
 
  public DisruptorQueue(int capacity,
                        RingItemFactory<T> itemFactory,
                        ItemProcessor<? super T> ...processors)
  {
    if (processors.length < 1)
      throw new IllegalArgumentException();
    
    int size = 8;
    
    for (; size < capacity; size *= 2) {
    }
    
    _size = size;
    
    _mask = size - 1;
    
    _itemRing = createRing(size);
    
    int processorsSize = processors.length;
    
    // offset to avoid cpu cache conflicts
    for (int j = 0; j < 8; j++) {
      for (int i = 0; i < _size; i += 8) {
        int index = i + j;
        
        _itemRing[index] = itemFactory.createItem(index);
        
        if (_itemRing[index] == null)
          throw new NullPointerException();
      }
    }
    
    DisruptorIndex tail = null;
    DisruptorIndex prevIndex = this;
    
    DisruptorConsumer<T> prevConsumer = null;
    DisruptorWorker<T> firstWorker = null;
    
    for (int i = 0; i < processorsSize; i++) {
      AtomicBoolean isWaitRef = null;
      
      if (i == processorsSize - 1) {
        isWaitRef = _isWaitRef;
      }
      
      DisruptorConsumer<T> consumer
        = new DisruptorConsumer<T>(_itemRing,
                                   processors[i],
                                   prevIndex,
                                   isWaitRef);
      
      DisruptorWorker<T> worker = new DisruptorWorker<T>(consumer);
      
      if (prevConsumer != null) {
        prevConsumer.setNextWorker(worker);
      }
      
      if (firstWorker == null) {
        firstWorker = worker;
      }

      prevConsumer = consumer;
      tail = consumer;
      prevIndex = consumer;
    }
    
    _tailRef = tail;
    _firstWorker = firstWorker;
  }
  
  int get()
  {
    return _head;
  }
  
  @SuppressWarnings("unchecked")
  private T []createRing(int size)
  {
    return (T []) new RingItem[size];
  }
  
  public final boolean isEmpty()
  {
    return _headAlloc.get() == _tailRef.get();
  }
  
  public final int getSize()
  {
    int head = _headAlloc.get();
    int tail = _tailRef.get();
    
    return (_size + tail - head) % _size;
  }
  
  public final void wake()
  {
    if (! isEmpty()) {
      _firstWorker.wake();
    }
  }
  
  public final T startProducer(boolean isWait)
  {
    int head;
    T item;
    int nextHead;
    
    do {
      head = _headAlloc.get();
      item = _itemRing[head];
      
      nextHead = (head + 1) & _mask;
      
      // full queue
      int tail = _tailRef.get();
      
      if (nextHead == tail) {
        if (isWait) {
          head = -1;
          nextHead = 0;
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
    _firstWorker.wake();
    
    synchronized (_isWaitRef) {
      _isWaitRef.set(true);
      
      while (_headAlloc.get() == head 
             && _tailRef.get() == tail
             && _isWaitRef.get()) {
        try {
          _isWaitRef.wait(100);
        } catch (Exception e) {
        }
      }
    }
  }
  
  public final void finishProducer(T item)
  {
    // item.setSequence(_sequence + 1);
    
    int head = item.getIndex();
    int nextHead = (head + 1) & _mask;
    
    while (! _headUpdater.compareAndSet(this, head, nextHead)) {
    }

    // wake mask
    _firstWorker.wake();
  }

  private static final class DisruptorConsumer<T extends RingItem>
    extends DisruptorIndex
  {
    private final T []_itemRing;
    private final int _mask;
    
    private final int _tailChunk;
    
    private final ItemProcessor<? super T> _processor;
    
    private final DisruptorIndex _head;
    
    private volatile int _tail;
    
    private DisruptorWorker<T> _nextWorker;
    private final AtomicBoolean _isWaitRef;
    
    DisruptorConsumer(T []ring,
                     ItemProcessor<? super T> processor,
                     DisruptorIndex head,
                     AtomicBoolean isWaitRef)
    {
      _itemRing = ring;
      _mask = _itemRing.length - 1;
      
      int tailChunk = 0x100;
      
      if (_itemRing.length < tailChunk * 2) {
        tailChunk = _itemRing.length >> 1;
      }
      
      if (tailChunk == 0)
        tailChunk = 1;
      
      _tailChunk = tailChunk;
      
      _processor = processor;
      _head = head;
      
      _isWaitRef = isWaitRef;
    }
    
    void setNextWorker(DisruptorWorker<T> nextWorker)
    {
      _nextWorker = nextWorker;
    }
    
    private final void consumeAll()
    {
      boolean isWakeNext = true;
      
      try {
        isWakeNext = doConsume();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }

      if (isWakeNext) {
        if (_nextWorker != null) {
          _nextWorker.wake();
        }
          
        wakeQueue();
      }
    }
    
    @Override
    public final int get()
    {
      return _tail;
    }
    
    private final boolean doConsume()
      throws Exception
    {
      final DisruptorIndex headRef = _head;
      
      int head = headRef.get();
      int tail = _tail;

      if (head == tail) {
        return false;
      }

      final T []itemRing = _itemRing;
      
      int mask = _mask;
      
      int tailChunk = _tailChunk;
      int tailChunkMask = tailChunk - 1;
      
      int tailWakeMask = mask >> 1;
    
      if (head < tail || (head & ~ tailChunkMask) != (tail & ~ tailChunkMask)) {
        head = (tail + tailChunk) & mask & ~tailChunkMask;
      }
      
      final ItemProcessor<? super T> processor = _processor;
      final DisruptorWorker<T> nextWorker = _nextWorker;
      
      final AtomicBoolean isWait = _isWaitRef;

      try {
        while (true) {
          if (head == tail) {
            _tail = tail;
            
            head = headRef.get();
            
            if (head == tail) {
              return true;
            }
            
            if (nextWorker != null) {
              nextWorker.wake();
            }
            
            if ((tail & tailWakeMask) == 0
                && isWait != null && isWait.get()) {
              wakeQueue();
            }
            
            if (head < tail
                || (head & ~ tailChunkMask) != (tail & ~ tailChunkMask)) {
              head = (tail + tailChunk) & mask & ~tailChunkMask;
            }
          }
      
          T item = itemRing[tail];
        
          tail = (tail + 1) & mask;
          
          processor.process(item);
        }
      } finally {
        _tail = tail;
        
        processor.onEmpty();
      }
    }
    
    private void wakeQueue()
    {
      AtomicBoolean isWaitRef = _isWaitRef;
      
      if (isWaitRef == null)
        return;
      
      boolean isWait = isWaitRef.get();
      
      if (isWait) {
        synchronized (isWaitRef) {
          isWaitRef.set(false);
          isWaitRef.notifyAll();
        }
      }
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _processor + "]";
    }
  }
  
  private static class DisruptorWorker<T extends RingItem>
    extends AbstractTaskWorker
  {
    private final DisruptorConsumer<T> _consumer;
    
    DisruptorWorker(DisruptorConsumer<T> consumer)
    {
      _consumer = consumer;
    }
    
    @Override
    public final long runTask()
    {
      _consumer.consumeAll();

      return 0;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _consumer + "]";
    }
  }
  
  public interface ItemProcessor<T extends RingItem> {
    public void process(T item) throws Exception;
    
    public void onEmpty() throws Exception;
  }
  
  static {
    AtomicIntegerFieldUpdater headUpdater
      = AtomicIntegerFieldUpdater.newUpdater(DisruptorQueue.class, "_head");
    
    _headUpdater = headUpdater;
  }
}
