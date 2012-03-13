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

package com.caucho.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RingQueue<T extends RingItem> {
  private static final Logger log
    = Logger.getLogger(RingQueue.class.getName());
  
  private final T [] _ring;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicInteger _head = new AtomicInteger();
  
  private final AtomicInteger _tailAlloc = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  
  private final int _mask;
  private final int _updateSize;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
  
  public RingQueue(int capacity, RingItemFactory<T> itemFactory)
  {
    int size = 8;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _ring = (T[]) new RingItem[size];
    _mask = size - 1;
    _updateSize = size >> 2;
    
    for (int i = 0; i < _ring.length; i++) {
      _ring[i] = itemFactory.createItem(i);
    }
  }
  
  public boolean isEmpty()
  {
    return _head.get() == _tail.get();
  }
  
  public int getSize()
  {
    int head = _head.get();
    int tail = _tail.get();
    
    return (_ring.length + head - tail) & _mask;
  }
  
  public int getHead()
  {
    return _head.get();
  }
  
  public int getHeadAlloc()
  {
    return _headAlloc.get();
  }
  
  public int getTail()
  {
    return _tail.get();
  }
  
  public int getTailAlloc()
  {
    return _tailAlloc.get();
  }
  
  public final T beginOffer(boolean isWait)
  {
    // offer must allow only one thread to succeed, because 
    // completeOffer must be single threaded. A multi-threaded
    // completeOffer creates too much contention and spinning.
    
    final AtomicInteger headAllocRef = _headAlloc;
    final AtomicInteger tailRef = _tail;
    final int mask = _mask;
    int retry = 256;
    
    while (true) {
      int headAlloc = headAllocRef.get();
      int tail = tailRef.get();
          
      int nextHeadAlloc = (headAlloc + 1) & mask;
      
      if (nextHeadAlloc == tail) {
        if (! isWait) {
          return null;
        }
        else {
          waitForAvailable(headAlloc, tail);
        }
      }
      else if (headAllocRef.compareAndSet(headAlloc, nextHeadAlloc)) {
        return _ring[headAlloc];
      }
    }
  }
  
  public final void completeOffer(final T item)
  {
    item.setRingValue();
    
    completeOffer(item.getIndex());
  }
  
  private void completeOffer(final int index)
  {
    final AtomicInteger headRef = _head;
    final int mask = _mask;
    
    int nextHead = (index + 1) & mask;
    
    if (headRef.compareAndSet(index, nextHead)) {
      return;
    }
    
    final AtomicInteger headAllocRef = _headAlloc;
    final T []ring = _ring;

    // limit retry in high-contention situation, since we've acked the entry
    // int retryCount = 1024 + ((index & 0xf) << 8);
    
    while (true) {
      int head = headRef.get();
      int headAlloc = headAllocRef.get();

      if (head == headAlloc) {
        return;
      }
      
      if (ring[head].isRingValue()) {
        nextHead = (head + 1) & mask;
        
        if (headRef.compareAndSet(head, nextHead)) {
          return;
        }
      }
      
      /*
      if (((head + ring.length - index) & mask) < _updateSize) {
        // someone else acked us
        return;
      }
      */
    }
  }
 
  public final T beginPoll()
  {
    int nextTail;
    int tailAlloc;
    
    final AtomicInteger tailAllocRef = _tailAlloc;
    final AtomicInteger headRef = _head;
    final int mask = _mask;
    
    while (true) {
      tailAlloc = tailAllocRef.get();
      int head = headRef.get();
      
      if (head == tailAlloc) {
        return null;
      }
      
      nextTail = (tailAlloc + 1) & mask;
      if (tailAllocRef.compareAndSet(tailAlloc, nextTail)) {
        return _ring[tailAlloc];
      }
    }
  }
  
  public final void completePoll(final T item)
  {
    item.clearRingValue();
    
    completePoll(item.getIndex());
  }
    
  private void completePoll(final int index)
  {
    final AtomicInteger tailRef = _tail;
    final int mask = _mask;
    
    int nextTail = (index + 1) & mask;
    
    if (tailRef.compareAndSet(index, nextTail)) {
      wakeAvailable();
      return;
    }
    
    final AtomicInteger tailAllocRef = _tailAlloc;
    final T []ring = _ring;
    // int ringLength = ring.length;
    // int halfSize = _halfSize;
    
    // limit retry in high-contention situation
    // int retryCount = 1024 + ((index & 0xf) << 8);

    while (true) {
      final int tail = tailRef.get();
      final int tailAlloc = tailAllocRef.get();
      
      if (tail == tailAlloc) {
        break;
      }
      
      if (! ring[tail].isRingValue()) {
        nextTail = (tail + 1) & mask;
        
        if (tailRef.compareAndSet(tail, nextTail)) {
          break;
        }
      }

      /*
      if (((tail + ring.length - index) & mask) < _updateSize) {
        // someone else acked us
        break;
      }
      */
    }
    
    wakeAvailable();
  }
  
  private void waitForAvailable(int headAlloc, int tail)
  {
    _isWait.set(true);
    
    if (_headAlloc.get() == headAlloc && _tail.get() == tail) {
      synchronized (_isWait) {
        if (_headAlloc.get() == headAlloc
            && _tail.get() == tail
            && _isWait.get()) {
          try {
            _isWait.wait(100);
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    }
  }
  
  private boolean isFull()
  {
    int head = _head.get();
    int tail = _tail.get();
    
    int nextHead = (head + 1) & _mask;
    
    return nextHead == tail;
  }
  
  private void wakeAvailable()
  {
    if (_isWait.compareAndSet(true, false)) {
      synchronized (_isWait) {
        _isWait.notifyAll();
      }
    }
  }
}
