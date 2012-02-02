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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * FreeList provides a simple class to manage free objects.  This is useful
 * for large data structures that otherwise would gobble up huge GC time.
 *
 * <p>The free list is bounded.  Freeing an object when the list is full will
 * do nothing.
 */
public final class FreeRing<T> {
  private final int _size;
  private final int _mask;
  
  private final AtomicReferenceArray<T> _ring;

  private final AtomicInteger _head = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();

  /**
   * Create a new free list.
   *
   * @param initialSize maximum number of free objects to store.
   */
  public FreeRing(int capacity)
  {
    int size = 1;
    
    while (size < capacity) {
      size *= 2;
    }
    
    _size = size;
    _mask = size - 1;
    
    _ring = new AtomicReferenceArray<T>(size);
  }
  
  /**
   * Try to get an object from the free list.  Returns null if the free list
   * is empty.
   *
   * @return the new object or null.
   */
  public T allocate()
  {
    int head;
    int tail;
    int nextTail;

    do {
      head = _head.get();
      tail = _tail.get();
      
      if (head == tail)
        return null;
      
      nextTail = (tail + 1) & _mask;
    } while (! _tail.compareAndSet(tail, nextTail));

    return _ring.getAndSet(tail, null);
  }
  
  
  /**
   * Frees the object.  If the free list is full, the object will be garbage
   * collected.
   *
   * @param obj the object to be freed.
   */
  public boolean free(T obj)
  {
    int head;
    int nextHead;
    int tail;

    do {
      head = _head.get();
      tail = _tail.get();
      
      nextHead = (head + 1) & _mask;
      
      if (nextHead == tail)
        return false;
    } while (! _ring.compareAndSet(head, null, obj));
    
    _head.set(nextHead);

    return true;
  }

  /**
   * Frees the object.  If the free list is full, the object will be garbage
   * collected.
   *
   * @param obj the object to be freed.
   */
  public boolean freeCareful(T obj)
  {
    if (checkDuplicate(obj))
      throw new IllegalStateException("tried to free object twice: " + obj);

    return free(obj);
  }

  /**
   * Debugging to see if the object has already been freed.
   */
  public boolean checkDuplicate(T obj)
  {
    for (int i = 0; i < _size; i++) {
      if (_ring.get(i) == obj) {
        return true;
      }
    }

    return false;
  }
}
