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

public class RingValueQueue<T> {
  private static final Logger log
    = Logger.getLogger(RingValueQueue.class.getName());
  
  private final Item<T> [] _ring;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicInteger _head = new AtomicInteger();
  
  private final AtomicInteger _tailAlloc = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  
  private final int _mask;
  private final int _updateSize;
  
  private final AtomicBoolean _isWait = new AtomicBoolean();
  
  public RingValueQueue(int capacity)
  {
    int size = 8;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _ring = new Item[size];
    _mask = size - 1;
    _updateSize = size >> 2;
    
    for (int i = 0; i < _ring.length; i++) {
      _ring[i] = new Item<T>();
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
  
  public final boolean offer(T value)
  {
    return offer(value, 0);
  }
  
  public final boolean put(T value)
  {
    return offer(value, CurrentTime.getCurrentTime() + Integer.MAX_VALUE);
  }
  
  public final boolean offer(T value, long expireTime)
  {
    if (value == null)
      throw new NullPointerException();
    
    final AtomicInteger headRef = _head;
    final AtomicInteger headAllocRef = _headAlloc;
    final AtomicInteger tailRef = _tail;
    final int mask = _mask;
    int retry = 256;
    
    while (true) {
      int headAlloc = headAllocRef.get();
      int tail = tailRef.get();
          
      int nextHeadAlloc = (headAlloc + 1) & mask;
      
      if (nextHeadAlloc == tail) {
        if (finishOffer()) {
        }
        else if (expireTime <= 0) {
          return false;
        }
        else {
          waitForAvailable(headAlloc, tail);
        }
      }
      else if (headAllocRef.compareAndSet(headAlloc, nextHeadAlloc)) {
        Item<T> item = _ring[headAlloc];
        item.set(value);
        
        if (! headRef.compareAndSet(headAlloc, nextHeadAlloc)) {
          finishOffer(headAlloc);
        }
        
        return true;
      }
    }
  }
  
  private final boolean finishOffer()
  {
    final AtomicInteger headAllocRef = _headAlloc;
    final AtomicInteger headRef = _head;
    
    int head = headRef.get();
    int headAlloc = headAllocRef.get();
    
    if (head != headAlloc && _ring[head].isSet()) {
      finishOffer(head);
      return true;
    }
    else {
      return false;
    }
  }

  private void finishOffer(final int index)
  {
    final AtomicInteger headRef = _head;
    final AtomicInteger headAllocRef = _headAlloc;
    final int mask = _mask;
    
    final Item<T> []ring = _ring;

    // limit retry in high-contention situation, since we've acked the entry
    int retryCount = 1024 + ((index & 0xf) << 8);
    int count = 4;
    
    while (retryCount-- >= 0) {
      int head = headRef.get();
      int headAlloc = headAllocRef.get();

      if (head == headAlloc) {
        return;
      }
      
      if (ring[head].isSet()) {
        int nextHead = (head + 1) & mask;
        
        if (headRef.compareAndSet(head, nextHead) && count-- <= 0) {
          return;
        }
      }
    }
  }
 
  public final T poll()
  {
    int nextTail;
    int tailAlloc;
    
    final AtomicInteger tailAllocRef = _tailAlloc;
    final AtomicInteger tailRef = _tail;
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
        Item<T> item = _ring[tailAlloc];
        T value = item.getAndClear();
        
        if (! tailRef.compareAndSet(tailAlloc, nextTail)) {
          completePoll(tailAlloc);
        }
        
        return value;
      }
    }
  }
    
  private void completePoll(final int index)
  {
    final AtomicInteger tailRef = _tail;
    final AtomicInteger tailAllocRef = _tailAlloc;
    final int mask = _mask;
    final Item<T> []ring = _ring;
    
    // int ringLength = ring.length;
    // int halfSize = _halfSize;
    
    // limit retry in high-contention situation
    int retryCount = 1024 + ((index & 0xf) << 8);
    int count = 4;

    while (retryCount-- >= 0) {
      final int tail = tailRef.get();
      final int tailAlloc = tailAllocRef.get();
      
      if (tail == tailAlloc) {
        wakeAvailable();
        return;
      }
      
      if (! ring[tail].isSet()) {
        int nextTail = (tail + 1) & mask;
        
        if (tailRef.compareAndSet(tail, nextTail) && count-- <= 0) {
          wakeAvailable();
          return;
        }
      }
    }
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
  
  private static final class Item<T> {
    private volatile T _value;
    
    final boolean isSet()
    {
      return _value != null;
    }
    
    final T get()
    {
      return _value;
    }
    
    final T getAndClear()
    {
      T value = _value;
      _value = null;
      
      return value;
    }
    
    final void set(final T value)
    {
      _value = value;
      
    }
    
    final void clear()
    {
      _value = null;
    }
  }
}
