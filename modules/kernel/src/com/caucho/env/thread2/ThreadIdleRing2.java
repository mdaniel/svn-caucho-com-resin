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

package com.caucho.env.thread2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadIdleRing2 {
  private final static int RING_SIZE = 16 * 1024;
  
  private final ResinThread2 [] _ring;
  
  private final AtomicInteger _head = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  private final int _mask;
  
  public ThreadIdleRing2()
  {
    _ring = new ResinThread2[RING_SIZE];
    _mask = RING_SIZE - 1;
  }
  
  public boolean isEmpty()
  {
    return _head.get() == _tail.get();
  }
  
  public int getSize()
  {
    int head = _head.get();
    int tail = _tail.get();
    
    return (head - tail) & _mask;
  }
  
  public boolean offer(ResinThread2 thread)
  {
    int head;
    int nextHead;
    
    if (thread == null) {
      throw new IllegalStateException();
    }
    
    do {
      head = _head.get();
      // nextHead = (head + 1) & _mask;
      nextHead = (head + 1) % _ring.length;
      
      int tail = _tail.get();
      
      if (nextHead == tail) {
        System.out.println("FILLED: " + thread);
        return false;
      }
    } while (! _head.compareAndSet(head, nextHead));

    _ring[head] = thread;
    
    return true;
  }
  
  ResinThread2 take()
  {
    int tail;
    int nextTail;
    
    ResinThread2 thread = null;

    do {
      int head = _head.get();
      
      tail = _tail.get();
      
      if (head == tail) {
        return null;
      }
      
      nextTail = (tail + 1) % _ring.length;
      
      thread = _ring[tail];
    } while (! _tail.compareAndSet(tail, nextTail));
    
    _ring[tail] = null;
    
    return thread;
  }

  class Item {
    private final AtomicBoolean _isSet = new AtomicBoolean();
    
    private final AtomicReference<Runnable> _taskRef
      = new AtomicReference<Runnable>();
    
    private final AtomicReference<ClassLoader> _loaderRef
      = new AtomicReference<ClassLoader>();
    
    public final boolean init(Runnable task, ClassLoader loader)
    {
      if (! _isSet.compareAndSet(false, true))
        return false;
      
      if (! _loaderRef.compareAndSet(null, loader)) {
        System.out.println("DIE:");
        throw new IllegalStateException();
      }
        
      if (! _taskRef.compareAndSet(null, task)) {
        System.out.println("DIE2:");
        throw new IllegalStateException();
      }
      
      return true;
    }
  }
}
