/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.container;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.ThreadPoolMXBean;

public class ThreadPoolAdmin extends ManagedObjectBase
  implements ThreadPoolMXBean
{
  private final ThreadPool _threadPool;

  private ThreadPoolAdmin(ThreadPool threadPool)
  {
    _threadPool = threadPool;
  }

  /**
   * The registration needs to be controlled externally to make
   * the timing work correctly.
   */
  public static ThreadPoolAdmin create()
  {
    return new ThreadPoolAdmin(ThreadPool.current());
  }

  /**
   * The thread pool is unique so it doesn't have a name.
   */
  public String getName()
  {
    return null;
  }

  //
  // configuration
  //

  /**
   * Returns the maximum number of threads.
   */
  public int getThreadMax()
  {
    return _threadPool.getThreadMax();
  }

  /**
   * Returns the maximum number of executor threads.
   */
  public int getThreadExecutorMax()
  {
    return _threadPool.getExecutorTaskMax();
  }

  /**
   * Returns the minimum number of idle threads.
   */
  @Override
  public int getThreadIdleMin()
  {
    return _threadPool.getIdleMin();
  }

  /**
   * Returns the maximum number of idle threads.
   */
  @Override
  public int getThreadIdleMax()
  {
    return _threadPool.getIdleMax();
  }

  /**
   * Returns the minimum number of saved priority threads.
   */
  @Override
  public int getThreadPriorityMin()
  {
    return _threadPool.getPriorityIdleMin();
  }

  //
  // statistics
  //

  /**
   * Returns the total number of threads.
   */
  public int getThreadCount()
  {
    return _threadPool.getThreadCount();
  }

  /**
   * Returns the current number of active threads.
   */
  public int getThreadActiveCount()
  {
    return _threadPool.getThreadActiveCount();
  }

  /**
   * Returns the current number of starting threads.
   */
  public int getThreadStartingCount()
  {
    return _threadPool.getThreadStartingCount();
  }

  /**
   * Returns the current number of idle threads.
   */
  public int getThreadIdleCount()
  {
    return _threadPool.getThreadIdleCount();
  }

  /**
   * Returns the current number of waiting threads.
   */
  public int getThreadWaitCount()
  {
    return _threadPool.getThreadWaitCount();
  }

  /**
   * Returns the current number of starting threads.
   */
  public long getThreadCreateCountTotal()
  {
    return _threadPool.getThreadCreateCountTotal();
  }

  /**
   * Returns the current number of overflow threads.
   */
  public long getThreadOverflowCountTotal()
  {
    return _threadPool.getThreadOverflowCountTotal();
  }

  /**
   * Returns the thread priority queue size
   */
  public int getThreadPriorityQueueSize()
  {
    return _threadPool.getThreadPriorityQueueSize();
  }
  
  /**
   * Returns the thread task queue size
   */
  public int getThreadTaskQueueSize()
  {
    return _threadPool.getThreadTaskQueueSize();
  }

  public void register()
  {
    registerSelf();
  }

  public void unregister()
  {
    unregisterSelf();
  }
}
