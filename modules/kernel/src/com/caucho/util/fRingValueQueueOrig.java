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

public class fRingValueQueueOrig<T> {
  private static final Logger log
    = Logger.getLogger(fRingValueQueueOrig.class.getName());
  
  private final Item<T> [] _ring;
  
  private final AtomicInteger _headAlloc = new AtomicInteger();
  private final AtomicInteger _head = new AtomicInteger();
  
  private final AtomicInteger _tailAlloc = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  
  private final int _size;
  private final int _mask;
  
  private final AtomicBoolean _isOfferWait = new AtomicBoolean();
  
  public fRingValueQueueOrig(int capacity)
  {
    int size = 8;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _size = size;
    _ring = new Item[size];
    _mask = size - 1;
    
    for (int i = 0; i < _ring.length; i++) {
      _ring[i] = new Item<T>();
    }
  }
  
  public final boolean isEmpty()
  {
    return _head.get() == _tail.get();
  }
  
  public final int getSize()
  {
    int head = _head.get();
    int tail = _tail.get();
    
    return (head - tail + _size) & _mask;
  }
  
  public final int getCapacity()
  {
    return _size;
  }
  
  public final int getHead()
  {
    return _head.get();
  }
  
  public final int getHeadAlloc()
  {
    return _headAlloc.get();
  }
  
  public final int getTail()
  {
    return _tail.get();
  }
  
  public final int getTailAlloc()
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
    
    while (true) {
      int headAlloc = headAllocRef.get();
      int tail = tailRef.get();
          
      int nextHeadAlloc = (headAlloc + 1) & mask;
      
      if (nextHeadAlloc == tail) {
        if (finishPoll()) {
        }
        else if (expireTime <= 0) {
          return false;
        }
        else {
          waitForAvailable(headAlloc, tail);
        }
      }
      else if (headAllocRef.compareAndSet(headAlloc, nextHeadAlloc)) {
        Item<T> item = get(headAlloc);
        item.set(value);
        
        if (! headRef.compareAndSet(headAlloc, nextHeadAlloc)) {
          finishOffer(headAlloc);
        }
        
        wakeAvailable();
        
        return true;
      }
    }
  }
  
  private final boolean finishOffer()
  {
    final int head = _head.get();
    final int headAlloc = _headAlloc.get();
    
    if (head != headAlloc && get(head).isSet()) {
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

    // limit retry in high-contention situation, since we've acked the entry
    final int retryMax = ((index & 0xf) + 1) << 4;
    int retryCount = retryMax;
    int count = 4;
    
    while (retryCount-- >= 0) {
      int head = headRef.get();
      int headAlloc = headAllocRef.get();

      if (head == headAlloc) {
        return;
      }
      
      if (get(head).isSet()) {
        int nextHead = (head + 1) & mask;
        
        if (headRef.compareAndSet(head, nextHead) && count-- <= 0) {
          return;
        }
        
        retryCount = retryMax;
      }
    }
  }
  
  public final T peek()
  {
    int tail = _tailAlloc.get();
    int head = _head.get();
    
    if (head != tail)
      return getValue(tail);
    else
      return null;
  }
 
  public final T poll()
  {
    // long nextTail;
    // long tailAlloc;
    
    final AtomicInteger tailAllocRef = _tailAlloc;
    final AtomicInteger tailRef = _tail;
    final AtomicInteger headRef = _head;
    
    while (true) {
      int tailAlloc = tailAllocRef.get();
      int head = headRef.get();
      
      if (head == tailAlloc) {
        if (! finishOffer()) {
          return null;
        }
      }
      
      final int nextTail = (tailAlloc + 1) & _mask;
      if (tailAllocRef.compareAndSet(tailAlloc, nextTail)) {
        final Item<T> item = get(tailAlloc);
        
        final T value = item.getAndClear();
        
        if (! tailRef.compareAndSet(tailAlloc, nextTail)) {
          completePoll(tailAlloc);
        }
        
        return value;
      }
    }
  }
  
  private final boolean finishPoll()
  {
    final int tail = _tail.get();
    final int headAlloc = _tailAlloc.get();
    
    if (tail != headAlloc && ! get(tail).isSet()) {
      completePoll(tail);
      return true;
    }
    else {
      return false;
    }
  }

  private void completePoll(final int index)
  {
    final AtomicInteger tailRef = _tail;
    final AtomicInteger tailAllocRef = _tailAlloc;
    
    // int ringLength = ring.length;
    // int halfSize = _halfSize;
    
    // limit retry in high-contention situation
    final int retryMax = ((index & 0xf) + 1) << 4;
    int retryCount = retryMax;
    int count = 4;

    while (retryCount-- >= 0) {
      final int tail = tailRef.get();
      final int tailAlloc = tailAllocRef.get();
      
      if (tail == tailAlloc) {
        wakeAvailable();
        return;
      }
      
      if (! get(tail).isSet()) {
        final int nextTail = (tail + 1) & _mask;
        
        if (tailRef.compareAndSet(tail, nextTail) && count-- <= 0) {
          wakeAvailable();
          return;
        }
        
        retryCount = retryMax;
      }
    }
    
    wakeAvailable();
  }
  
  private void waitForAvailable(int headAlloc, int tail)
  {
    if (_headAlloc.get() == headAlloc && _tail.get() == tail) {
      synchronized (_isOfferWait) {
        _isOfferWait.set(true);
        if (_headAlloc.get() == headAlloc
            && _tail.get() == tail) {
          try {
            _isOfferWait.wait(10);
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    }
  }
  
  private Item<T> get(int index)
  {
    return _ring[index];
  }
  
  public T getValue(int index)
  {
    return _ring[index & _mask].get();
  }
  
  public int nextIndex(int index)
  {
    return (index + 1) & _mask;
  }
  
  public int prevIndex(int index)
  {
    return (index + _mask) & _mask;
  }
  
  private void wakeAvailable()
  {
    int size = getSize();
    
    if (_isOfferWait.get()
        && (2 * size <= _size || _size - size > 64)) {
      if (_isOfferWait.compareAndSet(true, false)) {
        synchronized (_isOfferWait) {
          _isOfferWait.notifyAll();
        }
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
