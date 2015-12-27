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

package com.caucho.v5.management.server;

import com.caucho.v5.jmx.Counter;
import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.Gauge;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Management interface for the thread pool.
 *
 * <pre>
 * resin:type=ThreadPool
 * </pre>
 */
@Description("ThreadPool manages all threads used by the Resin server")
public interface ThreadPoolMXBean extends ManagedObjectMXBean {
  //
  // configuration
  //
  
  /**
   * Returns the maximum number of threads.
   */
  @Description("The configured maximum number of threads")
  public int getThreadMax();
  
  /**
   * Returns the maximum number of executor threads.
   */
  @Description("The configured maximum number of executor threads")
  public int getThreadExecutorMax();
  
  /**
   * Returns the priority thread gap
   */
  @Description("The priority thread gap")
  public int getThreadPriorityMin();
  
  /**
   * Returns the minimum number of idle threads.
   */
  @Description("The configured minimum number of idle threads")
  public int getThreadIdleMin();
  
  /**
   * Returns the maximum number of idle threads.
   */
  @Description("The configured maximum number of idle threads")
  public int getThreadIdleMax();

  //
  // Statistics
  //
  
  /**
   * Returns the current number of threads.
   */
  @Description("The current number of managed threads")
  @Gauge
  public int getThreadCount();
  
  /**
   * Returns the current number of active threads.
   */
  @Description("The current number of active threads")
  @Gauge
  public int getThreadActiveCount();
  
  /**
   * Returns the current number of starting threads.
   */
  @Description("The current number of starting threads")
  @Gauge
  public int getThreadStartingCount();
  
  /**
   * Returns the current number of idle threads.
   */
  @Description("The current number of idle threads")
  @Gauge
  public int getThreadIdleCount();
  
  /**
   * Returns the current number of waiting schedule threads.
   */
  @Description("The current number of wait threads")
  @Gauge
  public int getThreadWaitCount();
  
  /**
   * Returns the total number of started threads.
   */
  @Description("The total number of created threads")
  @Counter
  public long getThreadCreateCountTotal();
  
  /**
   * Returns the total number of overflow threads.
   */
  @Description("The total number of overflow threads")
  @Counter
  public long getThreadOverflowCountTotal();
  
  /**
   * Returns the thread priority queue size
   */
  @Description("The priority queue size")
  @Gauge
  public int getThreadPriorityQueueSize();
  
  /**
   * Returns the thread task queue size
   */
  @Description("The task queue size")
  @Gauge
  public int getThreadTaskQueueSize();
}
