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

package com.caucho.env.thread;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadRing {
  private static final Logger log = Logger.getLogger(ThreadRing.class.getName());
  
  private final static int RING_SIZE = 16 * 1024;
  private final ThreadPool _threadPool;
  private final Item [] _ring;
  
  private final AtomicInteger _head = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  private final int _mask;
  
  private final AtomicInteger _queueCount = new AtomicInteger();
  private final AtomicInteger _idleCount = new AtomicInteger();

  private final ClassLoader _classLoader = ThreadRing.class.getClassLoader();
  private final RingTask _task = new RingTask();
  
  private final int _loopCount = 1 * 1000;
  
  public ThreadRing(ThreadPool threadPool)
  {
    _threadPool = threadPool;
    
    _ring = new Item[RING_SIZE];
    _mask = RING_SIZE - 1;
    
    for (int i = 0; i < _ring.length; i++) {
      _ring[i] = new Item();
    }
  }
  
  public void schedule(Runnable task)
  {
    _queueCount.incrementAndGet();
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    int head;
    int nextHead;
    
    do {
      head = _head.get();
      nextHead = (head + 1) & _mask;
      
      int tail = _tail.get();
      
      if (nextHead == tail) {
        // overflow handled by the base thread 
        _queueCount.decrementAndGet();
        _threadPool.schedule(task);
        return;
      }
    } while (! _head.compareAndSet(head, nextHead));
    
    Item item = _ring[head];
      
    item.init(task, loader);
    
    launch();
  }
  
  private Item nextThread()
  {
    int tail;
    int nextTail;
    int head;
    
    do {
      tail = _tail.get();
      nextTail = (tail + 1) & _mask;
      
      head = _head.get();
      
      if (head == tail)
        return null;
    } while (! _tail.compareAndSet(tail, nextTail));
    
    _queueCount.decrementAndGet();
    
    return _ring[tail];
  }
  
  private void launch()
  {
    int idleCount;
    
    do {
      idleCount = _idleCount.get();
      int queueCount = _queueCount.get();
      
      if (queueCount < idleCount)
        return;
    } while (! _idleCount.compareAndSet(idleCount, idleCount + 1));
    
    _threadPool.schedule(_task, _classLoader);
  }
  
  final class RingTask implements Runnable {
    @Override
    public final void run()
    {
      try {
        int countMax = _loopCount;
        int count = countMax;
    
        while (count-- >= 0) {
          Item item = nextThread();

          if (item != null) {
            count = countMax;
            
            try {
              _idleCount.decrementAndGet();
              launch();
          
              item.runTask();
            } finally {
              _idleCount.incrementAndGet();
            }
          }
        }
      } finally {
        _idleCount.decrementAndGet();
      }
    }
  }

  static class Item {
    private final AtomicReference<Runnable> _task
      = new AtomicReference<Runnable>();
    private final AtomicReference<ClassLoader> _loader
      = new AtomicReference<ClassLoader>();
    
    public final void init(Runnable task, ClassLoader loader)
    {
      if (! _loader.compareAndSet(null, loader))
        throw new IllegalStateException();
        
      if (! _task.compareAndSet(null, task))
        throw new IllegalStateException();
    }

    public final void runTask()
    {
      Runnable task;
    
      while ((task = _task.getAndSet(null)) == null) {
      }
    
      ClassLoader loader = _loader.getAndSet(null);

      Thread thread = Thread.currentThread();
      String oldName = thread.getName();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(loader);

        try {
          Thread.interrupted();
          task.run();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      } finally {
        thread.setName(oldName);
        thread.setContextClassLoader(oldLoader);
      }
    }
  }
  }
