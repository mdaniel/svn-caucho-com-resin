/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.management.server;

import com.caucho.jmx.*;

/**
 * Management interface for the thread pool.
 *
 * <pre>
 * resin:type=ThreadPool
 * </pre>
 */
@Description("ThreadPool manages all threads used by the Resin server")
public interface ThreadPoolMXBean extends ManagedObjectMXBean {
  /**
   * Returns the maximum number of threads.
   */
  @Description("The configured maximum number of threads")
  public int getThreadMax();
  
  /**
   * Returns the minimum number of spare threads.
   */
  @Description("The configured minimum number of spare threads")
  public int getSpareThreadMin();

  //
  // Statistics
  //
  
  /**
   * Returns the current number of threads.
   */
  @Description("The current number of managed threads")
  public int getThreadCount();
  
  /**
   * Returns the current number of active threads.
   */
  @Description("The current number of active threads")
  public int getThreadActiveCount();
  
  /**
   * Returns the current number of idle threads.
   */
  @Description("The current number of idle threads")
  public int getThreadIdleCount();
}
