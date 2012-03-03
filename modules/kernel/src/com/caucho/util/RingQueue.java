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
  
  private final AtomicBoolean _isHeadAlloc = new AtomicBoolean();
  private final AtomicInteger _head = new AtomicInteger();
  
  private final AtomicBoolean _isTailAlloc = new AtomicBoolean();
  private final AtomicInteger _tail = new AtomicInteger();
  
  private final int _mask;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
  
  public RingQueue(int capacity, RingItemFactory<T> itemFactory)
  {
    int size = 8;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _ring = (T[]) new RingItem[size];
    _mask = size - 1;
    
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
    int head;
    int nextHead;
    int tail;
    
    AtomicBoolean isHeadAlloc = _isHeadAlloc;
    
    while (true) {
      while (! isHeadAlloc.compareAndSet(false, true)) {
      }
          
      head = _head.get();
      nextHead = (head + 1) & _mask;
      
      tail = _tail.get();
      
      if (nextHead != tail) {
        return _ring[head];
      }
      
      isHeadAlloc.set(false);
      
      if (isWait) {
        waitForEmpty();
      }
      else {
        return null;
      }
    }
  }
  
  public final void completeOffer(T item)
  {
    // completeOffer must be single-threaded to avoid unnecessary
    // contention
    
    int head = item.getIndex();
    int nextHead = (head + 1) & _mask;
    
    if (! _head.compareAndSet(head, nextHead)) {
      System.out.println("INVALID_HEAD");
    }
    
    if (! _isHeadAlloc.compareAndSet(true, false)) {
      System.out.println("INVALID_COMPLETE");
    }
  }
 
  public final T beginTake()
  {
    int head;
    int tail;
    int nextTail;
    
    AtomicBoolean isTailAlloc = _isTailAlloc;
    
    while (! isTailAlloc.compareAndSet(false, true)) {
      
    }
    
    tail = _tail.get();
    head = _head.get();
      
    if (head != tail) {
      return _ring[tail];
    }
    else {
      isTailAlloc.set(false);
      
      return null;
    }
  }
  
  public final void completeTake(T item)
  {
    int tail = item.getIndex();
    int nextTail = (tail + 1) & _mask;
    
    if (! _tail.compareAndSet(tail, nextTail)) {
      System.out.println("INVALID_TAKE:" + this);
    }
    
    if (! _isTailAlloc.compareAndSet(true, false)) {
      _isTailAlloc.set(false);
      System.out.println("INVALID_TAKE-SET:" + this);
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
