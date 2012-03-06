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
  private final int _halfSize;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
  
  public RingQueue(int capacity, RingItemFactory<T> itemFactory)
  {
    int size = 8;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _ring = (T[]) new RingItem[size];
    _mask = size - 1;
    _halfSize = size >> 1;
    
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
  
  public final T beginOffer(boolean isWait)
  {
    // offer must allow only one thread to succeed, because 
    // completeOffer must be single threaded. A multi-threaded
    // completeOffer creates too much contention and spinning.
    
    AtomicInteger headAllocRef = _headAlloc;
    
    while (true) {
      int head = headAllocRef.get();
          
      int nextHead = (head + 1) & _mask;
      
      int tail = _tail.get();
      
      if (nextHead == tail) {
        if (isWait) {
          waitForEmpty();
        }
        else {
          return null;
        }
      }
      else {  
        if (headAllocRef.compareAndSet(head, nextHead)) {
          return _ring[head];
        }
      }
    }
  }
  
  public final void completeOffer(T item)
  {
    item.setRingValue();
    int index = item.getIndex();

    while (item.isRingValue()) {
      int headAlloc = _headAlloc.get();
      int head = _head.get();
      
      if (_halfSize < ((index + _ring.length - head) & _mask)) {
        // someone else acked us
        return;
      }
      
      while (head != headAlloc && _ring[head].isRingValue() ) {
        int nextHead = (head + 1) & _mask;
        
        if (! _head.compareAndSet(head, nextHead)) {
          break;
        }
      
        if (head == index) {
          return;
        }
        
        head = nextHead;
      }
    }
  }
 
  public final T beginPoll()
  {
    int nextTail;
    int tailAlloc;
    
    final AtomicInteger tailAllocRef = _tailAlloc;
    final AtomicInteger headRef = _head;
    final int mask = _mask;
    
    do {
      tailAlloc = tailAllocRef.get();
      int head = headRef.get();
      
      if (head == tailAlloc) {
        return null;
      }
      
      nextTail = (tailAlloc + 1) & mask;
    } while (! tailAllocRef.compareAndSet(tailAlloc, nextTail));
    
    return _ring[tailAlloc];
  }
  
  public final void completePoll(T item)
  {
    item.clearRingValue();
    int index = item.getIndex();
    
    AtomicInteger tailAllocRef = _tailAlloc;
    AtomicInteger tailRef = _tail;
    T []ring = _ring;
    int ringLength = ring.length;
    int mask = _mask;
    int halfSize = _halfSize;

    loop:
    while (! item.isRingValue()) {
      int tailAlloc = tailAllocRef.get();
      int tail = tailRef.get();
      
      if (halfSize < ((index + ringLength - tail) & mask)) {
        // someone else acked us
        break;
      }
      
      while (tail != tailAlloc && ! ring[tail].isRingValue() ) {
        int nextTail = (tail + 1) & mask;
        
        if (! tailRef.compareAndSet(tail, nextTail)) {
          break;
        }
      
        if (tail == index) {
          break loop;
        }
        
        tail = nextTail;
      }
    }
    
    wakeEmpty();
  }
  
  private void waitForEmpty()
  {
    synchronized (_isWait) {
      _isWait.set(true);
      
      if (isFull()) {
        try {
          _isWait.wait();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
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
  
  private void wakeEmpty()
  {
    if (_isWait.get()) {
      synchronized (_isWait) {
        _isWait.set(false);
        _isWait.notifyAll();
      }
    }
  }
}
