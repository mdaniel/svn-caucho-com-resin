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
  
  public T beginOffer(boolean isWait)
  {
    int head;
    int nextHead;
    int tail;
        
    do {
      head = _headAlloc.get();
      nextHead = (head + 1) & _mask;
      
      tail = _tail.get();
      
      if (nextHead == tail) {
        if (isWait) {
          waitForEmpty();
        }
        else {
          return null;
        }
      }
    } while (! _headAlloc.compareAndSet(head, nextHead));
   
    T item = _ring[head];
    
    return item;
  }
  
  public void completeOffer(T item)
  {
    int head = item.getIndex();
    int nextHead = (head + 1) & _mask;
    
    while (! _head.compareAndSet(head, nextHead)) {
    }
  }
 
  public T beginTake()
  {
    int head;
    int tail;
    int nextTail;
    
    do {
      tail = _tailAlloc.get();
      nextTail = (tail + 1) & _mask;
      
      head = _head.get();
      
      if (head == tail) {
        return null;
      }
    } while (! _tailAlloc.compareAndSet(tail, nextTail));
    
    return _ring[tail];
  }
  
  public void completeTake(T item)
  {
    int tail = item.getIndex();
    int nextTail = (tail + 1) & _mask;
    
    while (! _tail.compareAndSet(tail, nextTail)) {
    }
    
    wakeEmpty();
  }
  
  private void waitForEmpty()
  {
    synchronized (_isWait) {
      _isWait.set(true);
      try {
        _isWait.wait();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
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
