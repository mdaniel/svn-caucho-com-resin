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

import com.caucho.env.thread1.ThreadPool1;
import com.caucho.env.thread2.ThreadPool2;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public final class ThreadPool extends ThreadPool2 {
  private static final AtomicReference<ThreadPool> _globalThreadPool
    = new AtomicReference<ThreadPool>();
  
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
   * Schedules an executor task.
   */
  public boolean scheduleExecutorTask(Runnable task)
  {
    return false;
  }

  /**
   * Called when an executor task completes
   */
  public void completeExecutorTask()
  {
    
  }
  
  public int getExecutorTaskMax()
  {
    return 0;
  }
  
  public void setExecutorTaskMax(int max)
  {
  }
}
