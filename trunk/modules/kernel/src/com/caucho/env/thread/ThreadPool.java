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

import java.util.concurrent.atomic.AtomicReference;

import com.caucho.config.ConfigException;
import com.caucho.env.thread2.ThreadPool2;
import com.caucho.util.L10N;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public final class ThreadPool extends ThreadPool2 {
  private static final L10N L = new L10N(ThreadPool.class);
  
  private static final AtomicReference<ThreadPool> _globalThreadPool
    = new AtomicReference<ThreadPool>();
  
  private static final int DEFAULT_EXECUTOR_TASK_MAX = 16;
  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;
  
  //
  // executor
  //
  private int _executorTaskMax = DEFAULT_EXECUTOR_TASK_MAX;

  private final Object _executorLock = new Object();
  // number of executor tasks running
  private int _executorTaskCount;
  // queue for waiting executor tasks
  private ExecutorQueueItem _executorQueueHead;
  private ExecutorQueueItem _executorQueueTail;
  
  public ThreadPool()
  {
    super();
  }

  public ThreadPool(String name)
  {
    super(name);
  }

  public static ThreadPool getCurrent()
  {
    return getThreadPool();
  }

  public static ThreadPool getThreadPool()
  {
    ThreadPool pool = _globalThreadPool.get();
    
    if (pool == null) {
      pool = new ThreadPool();
      
      pool = pool.setAsGlobal();
      
      if (_globalThreadPool.compareAndSet(null, pool)) {
        pool.start();
      }
      else {
        pool = _globalThreadPool.get();
      }
    }
    
    return pool;
  }
  
  protected ThreadPool setAsGlobal()
  {
    if (_globalThreadPool.compareAndSet(null, this)) {
      start();
      
      setAsGlobal(this);
      
      return this;
    }
    else {
      return _globalThreadPool.get();
    }
  }

  /**
   * Sets the maximum number of executor threads.
   */
  public void setExecutorTaskMax(int max)
  {
    if (getThreadMax() < max)
      throw new ConfigException(L.l("<thread-executor-max> ({0}) must be less than <thread-max> ({1})",
                                    max, getThreadMax()));

    if (max == 0)
      throw new ConfigException(L.l("<thread-executor-max> must not be zero."));

    _executorTaskMax = max;
  }

  /**
   * Gets the maximum number of executor threads.
   */
  public int getExecutorTaskMax()
  {
    return _executorTaskMax;
  }

  /**
   * Schedules an executor task.
   */
  public boolean scheduleExecutorTask(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    synchronized (_executorLock) {
      _executorTaskCount++;

      if (_executorTaskCount <= _executorTaskMax || _executorTaskMax < 0) {
        boolean isPriority = false;
        boolean isQueue = true;
        boolean isWake = true;

        return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
      }
      else {
        ExecutorQueueItem item = new ExecutorQueueItem(task, loader);

        if (_executorQueueTail != null)
          _executorQueueTail._next = item;
        else
          _executorQueueHead = item;

        _executorQueueTail = item;

        return false;
      }
    }
  }

  /**
   * Called when an executor task completes
   */
  public void completeExecutorTask()
  {
    ExecutorQueueItem item = null;

    synchronized (_executorLock) {
      _executorTaskCount--;

      assert(_executorTaskCount >= 0);

      if (_executorQueueHead != null) {
        item = _executorQueueHead;

        _executorQueueHead = item._next;

        if (_executorQueueHead == null)
          _executorQueueTail = null;
      }
    }

    if (item != null) {
      Runnable task = item.getRunnable();
      ClassLoader loader = item.getLoader();

      boolean isPriority = false;
      boolean isQueue = true;
      boolean isWake = true;

      scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
    }
  }
 
  static class ExecutorQueueItem {
    Runnable _runnable;
    ClassLoader _loader;

    ExecutorQueueItem _next;

    ExecutorQueueItem(Runnable runnable, ClassLoader loader)
    {
      _runnable = runnable;
      _loader = loader;
    }

    Runnable getRunnable()
    {
      return _runnable;
    }

    ClassLoader getLoader()
    {
      return _loader;
    }
  }
}
