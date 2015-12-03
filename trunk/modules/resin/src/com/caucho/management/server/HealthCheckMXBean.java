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
  public void silenceForPeriodMs(long periodMs);

  /**
   * Sets intermittent periods of silence
   */
  public void setLogPeriodMs(long periodMs);
  
  /**
   * Returns log period in millis
   */
  public long getLogPeriodMs();
}
