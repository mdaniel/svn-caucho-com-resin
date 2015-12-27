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

import com.caucho.v5.jmx.server.ManagedObjectMXBean;
import com.caucho.v5.jmx.server.MeterGraphInfo;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;

/**
 * Interface for the persistent statistics
 *
 * <pre>
 * resin:type=StatService
 * </pre>
 */
public interface StatServiceMXBean extends ManagedObjectMXBean
{
  /**
   * Returns the sample period in milliseconds
   */
  public long getSamplePeriod();

  /**
   * Returns statistics data over a time range.
   */
  public StatServiceValue []statisticsData(String name,
                                           long beginTime,
                                           long endTime,
                                           long stepTime);

  /**
   * Returns the most recent value.
   */
  public double getLastValue(String name);
  
  /**
   * Returns the best available value of the given timeframe less 1 week, or 
   * 1 day, or 1 hour.
   */
  public BaselineQueryResult getBaseline(String name,
                                         long beginTime,
                                         long endTime,
                                         int minSampleSize);

  /**
   * Returns the strings
   */
  public String []statisticsNames();

  /**
   * Returns the start times for the given server.
   */
  public long []getStartTimes(int serverIndex, long beginTime, long endTime);
  
  /**
   * Returns predefined meter graphs.
   */
  public MeterGraphInfo []getMeterGraphs();
  
  /**
   * Returns predefined meter graph pages.
   */
  public MeterGraphPageInfo []getMeterGraphPages();
  
  /**
   * Returns a predefined meter graph page.
   */
  public MeterGraphPageInfo getMeterGraphPage(String name);
  
  /**
   * Returns period of downtime
   */
  public DownTime []getDownTimes(int index, long beginTime, long endTime);
}
