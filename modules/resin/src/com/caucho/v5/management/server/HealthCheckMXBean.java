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

import com.caucho.v5.jmx.Name;
import com.caucho.v5.jmx.Units;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Interface for a HealthCheck
 *
 * <pre>
 * resin:type=HealthCheck,name="Resin|MyHealth"
 * </pre>
 */
public interface HealthCheckMXBean extends ManagedObjectMXBean
{
  /**
   * Returns the last health check status.
   */
  public String getStatus();
  
  /**
   * Returns the last health check message.
   */
  public String getMessage();
  
  /**
   * Returns the last status value as an integer for graphing.
   */
  public int getStatusOrdinal();

  /**
   * Prevents check result logging for a period of time
   */
  public void silenceForPeriodMs(@Name("period") @Units("milliseconds") 
                                 long periodMs);

  /**
   * Sets intermittent periods of silence
   */
  public void setLogPeriodMs(@Name("period") @Units("milliseconds") 
                             long periodMs);
  
  /**
   * Returns log period in millis
   */
  @Units("milliseconds")
  public long getLogPeriodMs();
}
