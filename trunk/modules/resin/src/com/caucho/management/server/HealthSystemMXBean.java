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
 */

package com.caucho.management.server;

import java.util.Date;

import com.caucho.jmx.Description;

/**
 * Interface for the health system
 *
 * <pre>
 * resin:type=HealthSystem
 * </pre>
 */
@Description("The check frequency and recheck rules of the health checking system")
public interface HealthSystemMXBean extends ManagedObjectMXBean
{
  @Description("Is all health checking enabled or disabled")
  public boolean isEnabled();
  
  @Description("The time after startup before actions are triggered")
  public long getStartupDelay();
  
  @Description("The time between checks")
  public long getPeriod();
  
  @Description("The time between rechecks")
  public long getRecheckPeriod();
  
  @Description("The number of rechecks before returning to the normal checking period")
  public int getRecheckMax();
  
  @Description("The time before returning to the normal checking period")
  public long getSystemRecheckTimeout();
  
  @Description("Date the last check cycle started")
  public Date getLastCheckStartTime();
  
  @Description("Date the last check cycle finished")
  public Date getLastCheckFinishTime();
  
  @Description("Health status emumeration")
  public String[] getHealthStatusNames();
  
  @Description("Health status emumeration")
  public String[] getLabels();

  @Description("The lifecycle of the service")
  public String getState();
  
  @Description("Query for health events that have occured in the time period")
  public HealthEventLog []findEvents(int serverIndex, 
                                     long minTime, long maxTime,
                                     int limit);
}
