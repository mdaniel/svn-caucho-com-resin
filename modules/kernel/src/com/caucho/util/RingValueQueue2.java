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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RingValueQueue2<T> {
  private static final Logger log
    = Logger.getLogger(RingValueQueue2.class.getName());
  
  private final Item<T> [] _ring;
  
  private final AtomicLong _headAlloc = new AtomicLong();
  private final AtomicLong _head = new AtomicLong();
  
  private final AtomicLong _tailAlloc = new AtomicLong();
  private final AtomicLong _tail = new AtomicLong();
  
  private final int _size;
  private final int _mask;
  
  private final AtomicBoolean _isOfferWait = new AtomicBoolean();
  
  public RingValueQueue2(int capacity)
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
    long head = _head.get();
    long tail = _tail.get();
    
    return (int) (head - tail);
  }
  
  public final long getHead()
  {
    return _head.get();
  }
  
  public final long getHeadAlloc()
  {
    return _headAlloc.get();
  }
  
  public final long getTail()
  {
    return _tail.get();
  }
  
  public final long getTailAlloc()
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
    
    final AtomicLong headRef = _head;
    final AtomicLong headAllocRef = _headAlloc;
    final AtomicLong tailRef = _tail;
    final int mask = _mask;
    
    while (true) {
      long headAlloc = headAllocRef.get();
      long tail = tailRef.get();
          
      long nextHeadAlloc = headAlloc + 1;
      
      if ((nextHeadAlloc & mask) == (tail & mask)) {
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
    final long head = _head.get();
    final long headAlloc = _headAlloc.get();
    
    if (head != headAlloc && get(head).isSet()) {
      finishOffer(head);
      return true;
    }
    else {
      return false;
    }
  }

  private void finishOffer(final long index)
  {
    final AtomicLong headRef = _head;
    final AtomicLong headAllocRef = _headAlloc;
    final int mask = _mask;

    // limit retry in high-contention situation, since we've acked the entry
    final int retryMax = (int) (((index & 0xf) + 1) << 4);
    int retryCount = retryMax;
    int count = 4;
    
    while (retryCount-- >= 0) {
      long head = headRef.get();
      long headAlloc = headAllocRef.get();

      if (head == headAlloc) {
        return;
      }
      
      if (get(head).isSet()) {
        long nextHead = head + 1;
        
        if (headRef.compareAndSet(head, nextHead) && count-- <= 0) {
          return;
        }
        
        retryCount = retryMax;
      }
    }
  }
  
  public final T peek()
  {
    long tail = _tailAlloc.get();
    
    return getValue(tail);
  }
 
  public final T poll()
  {
    // long nextTail;
    // long tailAlloc;
    
    final AtomicLong tailAllocRef = _tailAlloc;
    final AtomicLong tailRef = _tail;
    final AtomicLong headRef = _head;
    
    while (true) {
      long tailAlloc = tailAllocRef.get();
      long head = headRef.get();
      
      if (head == tailAlloc) {
        if (! finishOffer()) {
          return null;
        }
      }
      
      final long nextTail = tailAlloc + 1;
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
    final long tail = _tail.get();
    final long headAlloc = _tailAlloc.get();
    
    if (tail != headAlloc && ! get(tail).isSet()) {
      completePoll(tail);
      return true;
    }
    else {
      return false;
    }
  }

  private void completePoll(final long index)
  {
    final AtomicLong tailRef = _tail;
    final AtomicLong tailAllocRef = _tailAlloc;
    
    // int ringLength = ring.length;
    // int halfSize = _halfSize;
    
    // limit retry in high-contention situation
    final int retryMax = (int) (((index & 0xf) + 1) << 4);
    int retryCount = retryMax;
    int count = 4;

    while (retryCount-- >= 0) {
      final long tail = tailRef.get();
      final long tailAlloc = tailAllocRef.get();
      
      if (tail == tailAlloc) {
        wakeAvailable();
        return;
      }
      
      if (! get(tail).isSet()) {
        final long nextTail = tail + 1;
        
        if (tailRef.compareAndSet(tail, nextTail) && count-- <= 0) {
          wakeAvailable();
          return;
        }
        
        retryCount = retryMax;
      }
    }
  }
  
  private void waitForAvailable(long headAlloc, long tail)
  {
    if (_headAlloc.get() == headAlloc && _tail.get() == tail) {
      synchronized (_isOfferWait) {
        if (_headAlloc.get() == headAlloc
            && _tail.get() == tail) {
          _isOfferWait.set(true);

          try {
            _isOfferWait.wait(100);
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    }
  }
  
  private Item<T> get(long index)
  {
    return _ring[(int) (index & _mask)];
  }
  
  public T getValue(long index)
  {
    return get(index).get();
  }
  
  /*
  public int nextIndex(int index)
  {
    return (index + 1) & _mask;
  }
  
  public int prevIndex(int index)
  {
    return (index + _mask) & _mask;
  }
  */
  
  public final long nextIndex(long index)
  {
    return index + 1;
  }
  
  public long prevIndex(long index)
  {
    return index + _mask;
  }
  
  private void wakeAvailable()
  {
    if (_isOfferWait.compareAndSet(true, false)) {
      synchronized (_isOfferWait) {
        _isOfferWait.notifyAll();
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
