/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
import java.util.logging.Level;

import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import com.caucho.log.Log;

import com.caucho.server.resin.mbean.ThreadPoolMBean;
import com.caucho.jmx.AdminAttributeCategory;
import com.caucho.jmx.IntrospectionAttributeDescriptor;
import com.caucho.jmx.IntrospectionMBeanDescriptor;

public class ThreadPoolAdmin implements ThreadPoolMBean {
  private static final L10N L = new L10N(ThreadPoolAdmin.class);
  private static final Logger log = Log.open(ThreadPoolAdmin.class);

  public void describe(IntrospectionMBeanDescriptor descriptor)
  {
    descriptor.setTitle(L.l("ThreadPool"));
    descriptor.setDescription(L.l("The ThreadPool manages all threads used by Resin."));
  }

  /**
   * Returns the maximum number of threads.
   */
  public int getThreadMax()
  {
    return ThreadPool.getThreadMax();
  }

  public void describeThreadMax(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The maximum number of threads that Resin can allocate."));
    descriptor.setSortOrder(100);
  }

  /**
   * Returns the minimum number of spare threads.
   */
  public int getSpareThreadMin()
  {
    return ThreadPool.getSpareThreadMin();
  }

  public void describeSpareThreadMin(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);

    descriptor.setDescription(L.l(
      "The minimum number of threads Resin should have available for new"
        + " requests or other tasks.  This value causes a minimum number of idle"
        + " threads, useful for situations where there is a sudden"
        + " increase in the number of threads required."));

    descriptor.setSortOrder(200);
  }

  /**
   * Returns the current number of threads.
   */
  public int getThreadCount()
  {
    return ThreadPool.getThreadCount();
  }

  public void describeThreadCount(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setDescription(L.l("The current total number of threads managed by the pool."));
    descriptor.setSortOrder(1000);

  }

  /**
   * Returns the current number of active threads.
   */
  public int getActiveThreadCount()
  {
    return ThreadPool.getActiveThreadCount();
  }

  public void describeActiveThreadCount(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);

    descriptor.setDescription(L.l(
      "The number of active threads. These threads are busy servicing requests"
        + " or performing other tasks."));

    descriptor.setSortOrder(1100);
  }

  /**
   * Returns the current number of idle threads.
   */
  public int getIdleThreadCount()
  {
    return ThreadPool.getIdleThreadCount();
  }

  public void describeIdleThreadCount(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);

    descriptor.setDescription(L.l(
      "The number of idle threads. These threads are allocated but inactive,"
        + " available for new requests or tasks."));

    descriptor.setSortOrder(1200);
  }

}
