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

package com.caucho.server.resin;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import com.caucho.management.server.*;

public class ThreadPoolAdmin extends AbstractManagedObject
  implements ThreadPoolMXBean
{
  private static final L10N L = new L10N(ThreadPoolAdmin.class);
  private static final Logger log
    = Logger.getLogger(ThreadPoolAdmin.class.getName());

  private static ThreadPoolAdmin _admin;

  private ThreadPoolAdmin()
  {
    registerSelf();
  }

  /**
   * The registration needs to be controlled externally to make
   * the timing work correctly.
   */
  public static ThreadPoolMXBean create()
  {
    if (_admin == null)
      _admin = new ThreadPoolAdmin();

    return _admin;
  }

  /**
   * The thread pool is unique so it doesn't have a name.
   */
  public String getName()
  {
    return null;
  }

  /**
   * Returns the maximum number of threads.
   */
  public int getThreadMax()
  {
    return ThreadPool.getThreadMax();
  }

  /**
   * Returns the minimum number of idle threads.
   */
  public int getThreadIdleMin()
  {
    return ThreadPool.getThreadIdleMin();
  }

  /**
   * Returns the maximum number of idle threads.
   */
  public int getThreadIdleMax()
  {
    return ThreadPool.getThreadIdleMax();
  }

  /**
   * Returns the total number of threads.
   */
  public int getThreadCount()
  {
    return ThreadPool.getThreadCount();
  }

  /**
   * Returns the current number of active threads.
   */
  public int getThreadActiveCount()
  {
    return ThreadPool.getThreadActiveCount();
  }

  /**
   * Returns the current number of idle threads.
   */
  public int getThreadIdleCount()
  {
    return ThreadPool.getThreadIdleCount();
  }
}
