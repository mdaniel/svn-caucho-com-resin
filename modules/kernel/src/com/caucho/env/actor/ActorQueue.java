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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public final class ActorQueue<T extends RingItem>
  implements ActorQueuePreallocApi<T>
{
  private static final Logger log
    = Logger.getLogger(ActorQueue.class.getName());
  
  private final int _size;
  private final int _mask;
  // private final int _updateSize;
  private final T []_itemRing;
  
  private final ActorWorker<? super T> _firstWorker;
  
  //private final AtomicBoolean _isHeadAlloc = new AtomicBoolean();
  private final AtomicLong _headAllocRef = new AtomicLong();
  private final AtomicLong _headRef = new AtomicLong();
  // private volatile int _head;
  
  // private final ActorQueueIndex _tailRef;
  private final AtomicLong _tailRef;
  
  private final AtomicBoolean _isOfferWaitRef = new AtomicBoolean();
 
  public ActorQueue(int capacity,
                    RingItemFactory<T> itemFactory,
                    ActorProcessor<? super T> ...processors)
  {
    if (processors.length < 1)
      throw new IllegalArgumentException();
    
    int size = 8;
    
    for (; size < capacity; size *= 2) {
    }
    
    _size = size;
    _mask = size - 1;
    // _updateSize = _size >> 2;
    
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
    
    ActorConsumer<T> prevConsumer = null;
    ActorWorker<T> firstWorker = null;
    
    AtomicLong []tails = new AtomicLong[processorsSize + 1];
    tails[0] = _headRef;
    
    for (int i = 0; i < processorsSize; i++) {
      tails[i + 1] = new AtomicLong();
    }
    
    for (int i = 0; i < processorsSize; i++) {
      AtomicBoolean isWaitRef = null;
      
      if (i == processorsSize - 1) {
        isWaitRef = _isOfferWaitRef;
      }
      
      AtomicLong allocRef = tails[i];
      
      if (i == 0) {
        allocRef = _headAllocRef;
      }
      
      ActorConsumer<T> consumer
        = new ActorConsumer<T>(this,
                               _itemRing,
                               processors[i],
                               allocRef,
                               tails[i],
                               tails[i + 1],
                               isWaitRef);
      
      ActorWorker<T> worker = new ActorWorker<T>(consumer);
      
      if (prevConsumer != null) {
        prevConsumer.setNextWorker(worker);
      }
      
      if (firstWorker == null) {
        firstWorker = worker;
      }

      prevConsumer = consumer;
    }
    
    _tailRef = tails[tails.length - 1];
    _firstWorker = firstWorker;
  }
  
  @SuppressWarnings("unchecked")
  private T []createRing(int size)
  {
    return (T []) new RingItem[size];
  }
  
  public final boolean isEmpty()
  {
    return _headRef.get() == _tailRef.get();
  }
  
  public final int getSize()
  {
    return (int) ((_headRef.get() - _tailRef.get()) & _mask);
  }
  
  public final int getAvailable()
  {
    return _size - 1 - getSize();
  }
  
  public final String getWorkerState()
  {
    return _firstWorker.getState();
  }
  
  public final void wake()
  {
    _firstWorker.wake();
  }
  
  public final T startOffer(boolean isWait)
  {
    final AtomicLong headAllocRef = _headAllocRef;
    // final AtomicBoolean isHeadAlloc = _isHeadAlloc;
    // final AtomicInteger headRef = _headRef;
    final AtomicLong tailRef = _tailRef;
    final T []ring = _itemRing;
    final int mask = _mask;
    
    // int head;
    // T item;
    // int nextHead;
    
    while (true) {
      long headAlloc = headAllocRef.get();
      long nextHeadAlloc = headAlloc + 1;
      
      int headIndex = (int) (headAlloc & mask);
      
      T item = ring[headIndex];
      
      long tail = tailRef.get();
      
      if (nextHeadAlloc != tail + _size) {
        /*
        if (item.getIndex() != headAlloc)
          throw new IllegalStateException();
          */
        
        if (headAllocRef.compareAndSet(headAlloc, nextHeadAlloc)) {
          return item;
        }
      }
      else {
        if (headAlloc != _headRef.get()) {
          finishOffer();
          _firstWorker.wake();
        }
        
        /*
        if (headAlloc != _headRef.get() && finishOffer()) {
          _firstWorker.wake();
        }
        else
        */
        if (! isWait) {
          return null;
        }

        long timeoutMillis = 1000;
        int timeoutNanos = 0;
        waitForQueue(headAlloc, tail, timeoutMillis, timeoutNanos);
        isWait = false;
      }
    }
  }
  
  public final void finishOffer(final T item)
  {
    /*
    if (item.isRingValue())
      throw new IllegalStateException();
      */
    
    final long index = item.nextRingValue(_size);
    final long nextHead = index + 1;
    
    if (! _headRef.compareAndSet(index, nextHead)) {
      finishOffer();
    }
    /*
    if ((index & 0x3f) == 0) {
      _firstWorker.wake();
    }*/
    
    _firstWorker.wake();
    // wakeOfferQueue();
  }
    
  private final boolean finishOffer()
  {
    final AtomicLong headAllocRef = _headAllocRef;
    final AtomicLong headRef = _headRef;
    final T []ring = _itemRing;
    final int mask = _mask;
    
    long index = headRef.get();
    
    // in high-contention, can just finish since another thread will
    // ack us
    int retryMax = (int) (((index & 0xf) + 1) << 4);
    int retryCount = retryMax;
    int countMax = 4;
    int count = countMax;
    
    while (retryCount-- >= 0) {
      final long headAlloc = headAllocRef.get();
      final long head = headRef.get();
      
      if (head == headAlloc) {
        return head != index;
      }
      
      if (ring[(int) (head & mask)].getRingValue() == head) {
        long nextHead = head + 1;
        
        if (headRef.compareAndSet(head, nextHead) && count-- <= 0) {
          return true;
        }
        
        retryCount = retryMax;
      }
    }
    
    return true;
  }
  
  private void waitForQueue(long headAlloc, 
                            long tail,
                            long timeoutMillis,
                            int timeoutNanos)
  {
    _firstWorker.wake();
    
    // timeoutNanos = Math.min(100, timeoutNanos);
    
    if (_headAllocRef.get() == headAlloc && _tailRef.get() == tail) {
      synchronized (_isOfferWaitRef) {
        _isOfferWaitRef.set(true);

        if (_headAllocRef.get() == headAlloc 
            && _tailRef.get() == tail) {
          try {
            _isOfferWaitRef.wait(timeoutMillis, timeoutNanos);
          } catch (Exception e) {
          }
        }
      }
    }
  }
  
  private T get(int index)
  {
    return _itemRing[index];
  }
  
  private void wakeOfferQueue()
  {
    if (_isOfferWaitRef.compareAndSet(true, false)) {
      synchronized (_isOfferWaitRef) {
        _isOfferWaitRef.notifyAll();
      }
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _firstWorker + "]";
  }

  private static final class ActorConsumer<T extends RingItem>
  {
    private final ActorQueue<T> _queue;
    
    private final T []_itemRing;
    private final int _mask;
    
    private final int _tailChunk;
    
    private final ActorProcessor<? super T> _processor;
    
    private final AtomicLong _headAllocRef;
    private final AtomicLong _headRef;
    private final AtomicLong _tailRef;
    
    private ActorWorker<T> _nextWorker;
    private final AtomicBoolean _isWaitRef;
    
    ActorConsumer(ActorQueue<T> queue,
                  T []ring,
                  ActorProcessor<? super T> processor,
                  AtomicLong headAllocRef,
                  AtomicLong headRef,
                  AtomicLong tailRef,
                  AtomicBoolean isWaitRef)
    {
      _queue = queue;
      _itemRing = ring;
      
      int size = _itemRing.length;
      _mask = size - 1;
      
      int tailChunk = Math.min(32, Math.max(1, size >> 3));
      // tailChunk = 1;
      _tailChunk = tailChunk;
      
      _processor = processor;
      _headAllocRef = headAllocRef;
      _headRef = headRef;
      _tailRef = tailRef;
      
      _headAllocRef.set(size);
      _headRef.set(size);
      _tailRef.set(size);
      
      _isWaitRef = isWaitRef;
    }
    
    void setNextWorker(ActorWorker<T> nextWorker)
    {
      _nextWorker = nextWorker;
    }
    
    boolean isEmpty()
    {
      return _headAllocRef.get() == _tailRef.get();
    }
    
    private final void consumeAll()
    {
      final AtomicLong headAllocRef = _headAllocRef;
      final AtomicLong tailRef = _tailRef;
      final ActorWorker<T> nextWorker = _nextWorker;
      
      do {
        try {
          doConsume();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }

        if (nextWorker != null) {
          nextWorker.wake();
        }
        
        _queue.finishOffer();
        
        wakeQueue();
      } while (headAllocRef.get() != tailRef.get());
      
      // forceWakeQueue();
    }
    
    private final void doConsume()
      throws Exception
    {
      final AtomicLong headRef = _headRef;
      final AtomicLong tailRef = _tailRef;
      
      final T []itemRing = _itemRing;
      
      final int mask = _mask;
      final int tailChunk = _tailChunk;
      
      long head = headRef.get();
      long tail = tailRef.get();

      // int tailChunk = _tailChunk;

      long nextTailEnd = nextTailEnd(head, tail, tailChunk);
      
      final ActorProcessor<? super T> processor = _processor;
      // final ActorWorker<T> nextWorker = _nextWorker;
      
      // final AtomicBoolean isWait = _isWaitRef;

      try {
        while (true) {
          while (tail != nextTailEnd) {
            T item = itemRing[(int) (tail & mask)];
            
            tail++;
            
            processor.process(item);
          }

          tailRef.set(tail);
            
          // wakeQueue();
          
          head = headRef.get();
          if (head == tail) {
            return;
          }
            
          nextTailEnd = nextTailEnd(head, tail, tailChunk);
        }
      } finally {
        tailRef.set(tail);
        
        processor.onProcessComplete();
      }
    }
    
    private T get(final int index)
    {
      return _itemRing[index];
    }
    
    private static long nextTailEnd(long head, long tail, int tailChunk)
    {
      long nextTail = tail + tailChunk;
      
      return Math.min(head, nextTail);
    }
    
    private void wakeOfferWait(AtomicBoolean isWaitRef)
    {
      synchronized (isWaitRef) {
        if (isWaitRef.compareAndSet(true, false)) {
          isWaitRef.notifyAll();
        }
      }
    }
    
    private void wakeQueue()
    {
      AtomicBoolean isWaitRef = _isWaitRef;
      
      if (isWaitRef == null)
        return;
      
      if (isWaitRef.get()) {
        synchronized (isWaitRef) {
          isWaitRef.set(false);
          isWaitRef.notifyAll();
        }
      }
    }
    
    private void forceWakeQueue()
    {
      AtomicBoolean isWaitRef = _isWaitRef;
      
      if (isWaitRef == null)
        return;
      
      synchronized (isWaitRef) {
        isWaitRef.set(false);
        isWaitRef.notifyAll();
      }
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _processor + "]";
    }
  }
  
  private static class ActorWorker<T extends RingItem>
    extends AbstractTaskWorker
  {
    private final ActorConsumer<T> _consumer;
    
    ActorWorker(ActorConsumer<T> consumer)
    {
      _consumer = consumer;
    }
    
    @Override
    protected String getThreadName()
    {
      return _consumer._processor.getThreadName();
    }
    
    @Override
    protected boolean isRetry()
    {
      return ! _consumer.isEmpty();
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
}
