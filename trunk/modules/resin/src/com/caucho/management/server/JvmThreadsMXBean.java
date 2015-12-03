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

package com.caucho.management.server;

import com.caucho.jmx.Description;

/**
 * Information about the jvm threads.
 *
 * <pre>
 * resin:type=JvmThreads
 * </pre>
 */
@Description("ThreadPool manages all threads used by the Resin server")
public interface JvmThreadsMXBean extends ManagedObjectMXBean {
  //
  // configuration
  //
  
  /**
   * Returns the number of active threads.
   */
  @Description("The number of active threads")
  public int getThreadCount();
  
  /**
   * Returns the number of runnable threads.
   */
  @Description("The number of runnable threads")
  public int getRunnableCount();
  
  /**
   * Returns the number of native threads.
   */
  @Description("The number of native threads")
  public int getNativeCount();
  
  /**
   * Returns the number of waiting threads.
   */
  @Description("The number of waiting threads")
  public int getWaitingCount();
  
  /**
   * Returns the number of blocked threads.
   */
  @Description("The number of blocked threads")
  public int getBlockedCount();
}
