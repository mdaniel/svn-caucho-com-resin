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
public final class ActorQueue<T extends RingItem>
{
  private static final Logger log
    = Logger.getLogger(ActorQueue.class.getName());
  
  private final int _size;
  private final int _mask;
  private final T []_itemRing;
  
  private final ActorWorker<? super T> _firstWorker;
  
  // private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicBoolean _isHeadAlloc = new AtomicBoolean();
  private final AtomicInteger _headRef = new AtomicInteger();
  // private volatile int _head;
  
  // private final ActorQueueIndex _tailRef;
  private final AtomicInteger _tailRef;
  
  private final AtomicBoolean _isWaitRef = new AtomicBoolean();
 
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
        isWaitRef = _isWaitRef;
      }
      
      ActorConsumer<T> consumer
        = new ActorConsumer<T>(_itemRing,
                               processors[i],
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
    int head = _headRef.get();
    int tail = _tailRef.get();
    
    return (_size + tail - head) % _size;
  }
  
  public final void wake()
  {
    if (! isEmpty()) {
      _firstWorker.wake();
    }
  }
  
  public final T startOffer(boolean isWait)
  {
    // final AtomicInteger headAlloc = _headAlloc;
    final AtomicBoolean isHeadAlloc = _isHeadAlloc;
    final AtomicInteger headRef = _headRef;
    final AtomicInteger tailRef = _tailRef;
    final T []itemRing = _itemRing;
    final int mask = _mask;
    
    int head;
    T item;
    int nextHead;
    
    while (! isHeadAlloc.compareAndSet(false, true)) {
    }
    
    while (true) {
      head = headRef.get(); // _head
      nextHead = (head + 1) & mask;
      
      item = itemRing[head];
      
      // check for full queue
      int tail = tailRef.get();
      
      if (nextHead != tail) {
        return item;
      }
      
      if (isWait) {
        waitForQueue(head, tail);
      }
      else {
        isHeadAlloc.set(false);
        
        return null;
      }
    }
      // } while (! isHeadAlloc.compareAndSet(head, nextHead));
  }
  
  public final void finishOffer(T item)
  {
    // completeOffer must be single-threaded to avoid unnecessary
    // contention
    
     // item.setSequence(_sequence + 1);
    
    final int head = item.getIndex();
    final int nextHead = (head + 1) & _mask;
    
    final AtomicInteger headRef = _headRef;
    // final AtomicIntegerFieldUpdater<ActorQueue<?>> headUpdater = _headUpdater;
    
    /*
    if (head != _headRef.get()) {
      System.out.println("HA: " + _headRef.get() + " " + head);
    }
    */
    /*
    
    
    while (! headUpdater.compareAndSet(this, head, nextHead)) {
    }
    */
    
    // _head = nextHead;
    if (! headRef.compareAndSet(head, nextHead)) {
      System.out.println("BROKEN: " + headRef.get() + " " + head + " " + nextHead);
      headRef.set(nextHead);
    }
    
    if (! _isHeadAlloc.compareAndSet(true, false)) {
      System.out.println("ALLOC: fail");
      _isHeadAlloc.set(false);
    }

    // wake mask
    // _firstWorker.wake();
    
    if ((_headRef.get() & 0x3f) == 0) {
      _firstWorker.wake();
    }
  }
  
  private void waitForQueue(int head, int tail)
  {
    _firstWorker.wake();
    
    synchronized (_isWaitRef) {
      _isWaitRef.set(true);
      
      while (_headRef.get() == head 
             && _tailRef.get() == tail
             && _isWaitRef.get()) {
        try {
          _isWaitRef.wait(100);
        } catch (Exception e) {
        }
      }
    }
  }

  private static final class ActorConsumer<T extends RingItem>
  {
    private final T []_itemRing;
    private final int _mask;
    
    private final int _tailChunk;
    
    private final ItemProcessor<? super T> _processor;
    
    private final AtomicInteger _headRef;
    private final AtomicInteger _tailRef;
    
    private ActorWorker<T> _nextWorker;
    private final AtomicBoolean _isWaitRef;
    
    ActorConsumer(T []ring,
                     ItemProcessor<? super T> processor,
                     AtomicInteger headRef,
                     AtomicInteger tailRef,
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
    
    private final boolean doConsume()
      throws Exception
    {
      final AtomicInteger headRef = _headRef;
      final AtomicInteger tailRef = _tailRef;
      
      int head = headRef.get();
      int tail = tailRef.get();

      if (head == tail) {
        return false;
      }

      final T []itemRing = _itemRing;
      
      int mask = _mask;
      
      int tailChunk = _tailChunk;
      
      int tailWakeMask = mask >> 1;

      head = nextHeadChunk(head, tail, tailChunk);
      
      final ItemProcessor<? super T> processor = _processor;
      final ActorWorker<T> nextWorker = _nextWorker;
      
      final AtomicBoolean isWait = _isWaitRef;

      try {
        while (true) {
          while (head != tail) {
            T item = itemRing[tail];
          
            tail = (tail + 1) & mask;
            
            processor.process(item);
          }

          tailRef.set(tail);
            
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
            
          head = nextHeadChunk(head, tail, tailChunk);
        }
      } finally {
        tailRef.set(tail);
        
        processor.onProcessComplete();
      }
    }
    
    private int nextHeadChunk(int head, int tail, int tailChunk)
    {
      int tailChunkMask = tailChunk - 1;
      
      if (head < tail
          || (head & ~ tailChunkMask) != (tail & ~ tailChunkMask)) {
        return (tail + tailChunk) & _mask & ~tailChunkMask;
      }
      else
        return head;
      
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
      return String.valueOf(_consumer._processor);
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
    
    public void onProcessComplete() throws Exception;
  }
}
