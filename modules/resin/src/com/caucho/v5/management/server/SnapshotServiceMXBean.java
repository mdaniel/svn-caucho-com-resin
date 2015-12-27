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

import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Interface for taking and retrieving a snapshot.
 *
 * <pre>
 * resin:type=SnapshotService
 * </pre>
 */
public interface SnapshotServiceMXBean extends ManagedObjectMXBean
{
  @Description("take a snapshot of the jmx values")
  public void snapshotJmx();
  
  @Description("take a snapshot of the heap")
  public void snapshotHeap();
  
  @Description("take a snapshot of the threads")
  public void snapshotThreadDump();
  
  @Description("take a snapshot of the health checks")
  public void snapshotHealth();
  
  @Description("take a snapshot of the thread scoreboards")
  public void snapshotScoreboards();

  @Description("start CPU profiling")
  public void startProfile(long samplingRate, int stackDepth);
  
  @Description("stop CPU profiling")
  public void stopProfile();
  
  @Description("take a snapshot of the last profiling information")
  public void snapshotProfile();
  
  @Description("take a snapshot of the profiling information for the period")
  public void snapshotProfile(long period, long samplingRate, int stackDepth);
}
