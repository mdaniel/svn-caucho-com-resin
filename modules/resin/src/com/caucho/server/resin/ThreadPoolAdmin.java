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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.resin;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import com.caucho.log.Log;

import com.caucho.mbeans.ThreadPoolMBean;
import com.caucho.jmx.AdminAttributeCategory;
import com.caucho.jmx.AdminInfo;
import com.caucho.jmx.AdminInfoFactory;

public class ThreadPoolAdmin
  implements ThreadPoolMBean, AdminInfoFactory
{
  private static final L10N L = new L10N(ThreadPoolAdmin.class);
  private static final Logger log = Log.open(ThreadPoolAdmin.class);

  public AdminInfo getAdminInfo()
  {
    AdminInfo descriptor = new AdminInfo();

    descriptor.setTitle(L.l("ThreadPool"));
    descriptor.setDescription(L.l("The ThreadPool manages all threads used by Resin."));

    descriptor.createAdminAttributeInfo("ThreadMax")
      .setCategory(AdminAttributeCategory.CONFIGURATION)
      .setDescription(L.l("The maximum number of threads that Resin can allocate."));

    descriptor.createAdminAttributeInfo("SpareThreadMin")
      .setCategory(AdminAttributeCategory.CONFIGURATION)
      .setDescription(L.l(
        "The minimum number of threads Resin should have available for new"
        + " requests or other tasks.  This value causes a minimum number of idle"
        + " threads, useful for situations where there is a sudden"
        + " increase in the number of threads required."));


    descriptor.createAdminAttributeInfo("ThreadCount")
      .setCategory(AdminAttributeCategory.STATISTIC)
      .setDescription(L.l("The current total number of threads managed by the pool."));

    descriptor.createAdminAttributeInfo("ActiveThreadCount")
      .setCategory(AdminAttributeCategory.STATISTIC)
      .setDescription(L.l(
        "The number of active threads. These threads are busy servicing requests"
        + " or performing other tasks."));

    descriptor.createAdminAttributeInfo("IdleThreadCount")
      .setCategory(AdminAttributeCategory.CONFIGURATION)
      .setDescription(L.l(
        "The number of idle threads. These threads are allocated but inactive,"
        + " available for new requests or tasks."));

    return descriptor;
  }

  /**
   * Returns the maximum number of threads.
   */
  public int getThreadMax()
  {
    return ThreadPool.getThreadMax();
  }

  /**
   * Returns the minimum number of spare threads.
   */
  public int getSpareThreadMin()
  {
    return ThreadPool.getSpareThreadMin();
  }

  /**
   * Returns the current number of threads.
   */
  public int getThreadCount()
  {
    return ThreadPool.getThreadCount();
  }

  /**
   * Returns the current number of active threads.
   */
  public int getActiveThreadCount()
  {
    return ThreadPool.getActiveThreadCount();
  }

  /**
   * Returns the current number of idle threads.
   */
  public int getIdleThreadCount()
  {
    return ThreadPool.getIdleThreadCount();
  }
}
