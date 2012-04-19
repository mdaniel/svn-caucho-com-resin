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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;


/**
 * Interface for the transaction log.
 */
public final class ActorQueue<T extends RingItem>
{
  private static final Logger log
    = Logger.getLogger(ActorQueue.class.getName());
  
  private final int _size;
  private final int _mask;
  // private final int _updateSize;
  private final T []_itemRing;
  
  private final ActorWorker<? super T> _firstWorker;
  
  //private final AtomicBoolean _isHeadAlloc = new AtomicBoolean();
  private final AtomicInteger _headAllocRef = new AtomicInteger();
  private final AtomicInteger _headRef = new AtomicInteger();
  // private volatile int _head;
  
  // private final ActorQueueIndex _tailRef;
  private final AtomicInteger _tailRef;
  
  private final AtomicBoolean _isOfferWaitRef = new AtomicBoolean();
 
  public ActorQueue(int capacity,
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
    
    AtomicInteger []tails = new AtomicInteger[processorsSize + 1];
    tails[0] = _headRef;
    
    for (int i = 0; i < processorsSize; i++) {
      tails[i + 1] = new AtomicInteger();
    }
    
    for (int i = 0; i < processorsSize; i++) {
      AtomicBoolean isWaitRef = null;
      
      if (i == processorsSize - 1) {
        isWaitRef = _isOfferWaitRef;
      }
      
      AtomicInteger allocRef = tails[i];
      
      if (i == 0) {
        allocRef = _headAllocRef;
      }
      
      ActorConsumer<T> consumer
        = new ActorConsumer<T>(_itemRing,
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
    return (_headRef.get() + _size - _tailRef.get()) & _mask;
  }
  
  public final int getAvailable()
  {
    return _size - 1 - getSize();
  }
  
  public final void wake()
  {
    if (_headAllocRef.get() != _tailRef.get() || _isOfferWaitRef.get()) {
      _firstWorker.wake();
    }
  }
  
  public final T startOffer(boolean isWait)
  {
    final AtomicInteger headAllocRef = _headAllocRef;
    // final AtomicBoolean isHeadAlloc = _isHeadAlloc;
    // final AtomicInteger headRef = _headRef;
    final AtomicInteger tailRef = _tailRef;
    final T []ring = _itemRing;
    final int mask = _mask;
    
    // int head;
    // T item;
    // int nextHead;
    
    while (true) {
      int headAlloc = headAllocRef.get();
          
      int nextHeadAlloc = (headAlloc + 1) & mask;
      
      int tail = tailRef.get();
      
      if (nextHeadAlloc == tail) {
        if (finishOffer()) {
        }
        else if (isWait) {
          waitForQueue(headAlloc, tail);
        }
        else {
          return null;
        }
      }
      else {
        T item = ring[headAlloc];
        
        if (headAllocRef.compareAndSet(headAlloc, nextHeadAlloc)) {
          return item;
        }
      }
    }
  }
  
  public final void finishOffer(final T item)
  {
    item.setRingValue();
    
    final int index = item.getIndex();
    
    final int nextHead = (index + 1) & _mask;
    
    if (! _headRef.compareAndSet(index, nextHead)) {
      finishOffer(index);
    }
    
    if ((index & 0x3f) == 0) {
      _firstWorker.wake();
    }
    
    wakeOfferQueue();
  }
  
  private final boolean finishOffer()
  {
    final int head = _headRef.get();
    final int headAlloc = _headAllocRef.get();
    // final int mask = _mask;
    
    if (head != headAlloc && get(head).isRingValue()) {
      finishOffer(head);
      return true;
    }
    else {
      return false;
    }
  }
    
  private final void finishOffer(long index)
  {
    final AtomicInteger headAllocRef = _headAllocRef;
    final AtomicInteger headRef = _headRef;
    // final T []ring = _itemRing;
    final int mask = _mask;
    
    // in high-contention, can just finish since another thread will
    // ack us
    int retryMax = (int) (((index & 0xf) + 1) << 2);
    int retryCount = retryMax;
    int count = 2;
    
    while (retryCount-- >= 0) {
      int headAlloc = headAllocRef.get();
      int head = headRef.get();
      
      if (head == headAlloc) {
        return;
      }
      
      if (get(head).isRingValue()) {
        int nextHead = (head + 1) & mask;
        
        if (headRef.compareAndSet(head, nextHead) && count-- <= 0) {
          return;
        }
        
        retryCount = retryMax;
      }
    }
  }
  
  private void waitForQueue(long headAlloc, long tail)
  {
    _firstWorker.wake();
    
    if (_headAllocRef.get() == headAlloc && _tailRef.get() == tail) {
      synchronized (_isOfferWaitRef) {
        if (_headAllocRef.get() == headAlloc 
            && _tailRef.get() == tail) {
          _isOfferWaitRef.set(true);

          System.out.println("WAIT: " + headAlloc + " " + tail);
          try {
            _isOfferWaitRef.wait(100);
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

  private static final class ActorConsumer<T extends RingItem>
  {
    private final T []_itemRing;
    private final int _mask;
    
    private final int _tailChunk;
    
    private final ItemProcessor<? super T> _processor;
    
    private final AtomicInteger _headAllocRef;
    private final AtomicInteger _headRef;
    private final AtomicInteger _tailRef;
    
    private ActorWorker<T> _nextWorker;
    private final AtomicBoolean _isWaitRef;
    
    ActorConsumer(T []ring,
                  ItemProcessor<? super T> processor,
                  AtomicInteger headAllocRef,
                  AtomicInteger headRef,
                  AtomicInteger tailRef,
                  AtomicBoolean isWaitRef)
    {
      _itemRing = ring;
      _mask = _itemRing.length - 1;
      
      int tailChunk = Math.min(32, Math.max(1, _itemRing.length >> 3));
      
      _tailChunk = tailChunk;
      
      _processor = processor;
      _headAllocRef = headRef;
      _headRef = headRef;
      _tailRef = tailRef;
      
      _isWaitRef = isWaitRef;
    }
    
    void setNextWorker(ActorWorker<T> nextWorker)
    {
      _nextWorker = nextWorker;
    }
    
    private final void consumeAll()
    {
      do {
        try {
          doConsume();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }

        if (_nextWorker != null) {
          _nextWorker.wake();
        }
        
        wakeQueue();
      } while (_headAllocRef.get() != _tailRef.get());
      
      // forceWakeQueue();
    }
    
    private final void doConsume()
      throws Exception
    {
      final AtomicInteger headRef = _headRef;
      final AtomicInteger tailRef = _tailRef;
      
      int head = headRef.get();
      int tail = tailRef.get();

      // final T []itemRing = _itemRing;
      
      int mask = _mask;
      
      // int tailChunk = _tailChunk;

      int nextTailChunk = nextTailChunk(head, tail);
      
      final ItemProcessor<? super T> processor = _processor;
      // final ActorWorker<T> nextWorker = _nextWorker;
      
      // final AtomicBoolean isWait = _isWaitRef;

      try {
        while (true) {
          while (tail != nextTailChunk) {
            T item = get(tail);
          
            tail = (tail + 1) & mask;
            
            processor.process(item);
            
            item.clearRingValue();
          }

          tailRef.set(tail);
            
          wakeQueue();
          
          head = headRef.get();
          if (head == tail) {
            return;
          }
            
          nextTailChunk = nextTailChunk(head, tail);
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
    
    private int nextTailChunk(int head, int tail)
    {
      return Math.min(head, tail + _tailChunk);
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
      
      if (isWaitRef.compareAndSet(true, false)) {
        synchronized (isWaitRef) {
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
    public String getThreadName();
    
    public void process(T item) throws Exception;
    
    public void onProcessComplete() throws Exception;
  }
}
