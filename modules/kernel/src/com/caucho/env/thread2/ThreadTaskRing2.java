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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.ThreadPool;

public class ThreadTaskRing2 {
  private static final Logger log = Logger.getLogger(ThreadTaskRing2.class.getName());
  
  private final static int RING_SIZE = 16 * 1024;
  
  private final Item [] _ring;
  
  private final AtomicInteger _head = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  private final int _mask;
  
  public ThreadTaskRing2()
  {
    _ring = new Item[RING_SIZE];
    _mask = RING_SIZE - 1;
    
    for (int i = 0; i < _ring.length; i++) {
      _ring[i] = new Item();
    }
    
    System.out.println("MASK: " + Integer.toHexString(_mask));
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
  
  public boolean offer(Runnable task, ClassLoader loader)
  {
    int head;
    int nextHead;
    int tail;
    Item item;
    
    if (task == null) {
      System.out.println("DIE3:");
      throw new IllegalStateException();
    }
    
    do {
      head = _head.get();
      nextHead = (head + 1) & _mask;
      
      tail = _tail.get();
      
      if (nextHead == tail) {
        System.out.println("FILLED: " + task + " " + _head.get() + " " + _tail.get());
        return false;
      }
      
      item = _ring[head];
    } while (! item.init(task, loader));
    
    if (! _head.compareAndSet(head, nextHead)) {
      System.out.println("BAD:" + head + " " + nextHead);
    }
    
    if (getSize() > 5) {
      System.out.println("OOK: " + head + " " + nextHead + " " + tail);
    }
    
    return true;
  }
  
  boolean schedule(ResinThread2 thread)
  {
    int head;
    int tail;
    int nextTail;
    
    Item item;
    

    do {
      tail = _tail.get();
      nextTail = (tail + 1) & _mask;
      
      head = _head.get();
      
      if (head == tail) {
        return false;
      }
      
      
      item = _ring[tail];
    } while (! _tail.compareAndSet(tail, nextTail));
    
    if (getSize() > 5) {
      System.out.println("OOK2: " + head + " " + tail + " " + nextTail);
    }
    
    item.schedule(thread);
    
    return true;
  }

  class Item {
    private final AtomicBoolean _isSet = new AtomicBoolean();
    
    private final AtomicReference<Runnable> _taskRef
      = new AtomicReference<Runnable>();
    
    private final AtomicReference<ClassLoader> _loaderRef
      = new AtomicReference<ClassLoader>();
    
    public final boolean init(Runnable task, ClassLoader loader)
    {
      if (! _isSet.compareAndSet(false, true)) {
        return false;
      }
      
      if (_loaderRef.getAndSet(loader) != null) {
        System.out.println("DIE:");
        // throw new IllegalStateException();
      }
        
      if (_taskRef.getAndSet(task) != null) {
        System.out.println("DIE2:");
        // throw new IllegalStateException();
      }
      
      return true;
    }

    private final void schedule(ResinThread2 thread)
    {
      Runnable task;
      
      if (! _isSet.get()) {
        System.out.println("BAD7:" + _isSet.get()
                           + " " + _head.get() + " " + _tail.get());
      }
    
      while ((task = _taskRef.getAndSet(null)) == null) {
        System.out.println("BAD2:" + _isSet.get()
                           + " " + _head.get() + " " + _tail.get());
      }
      
      if (getSize() > 10) {
        System.out.println("SCHED: " + _head.get() + " " + _tail.get() + " " + task + " " + thread);
      }
    
      ClassLoader loader = _loaderRef.getAndSet(null);

      thread.scheduleTask(task, loader);
      
      if (! _isSet.getAndSet(false)) {
        System.out.println("BAD4:");
      }
    }
  }
}
