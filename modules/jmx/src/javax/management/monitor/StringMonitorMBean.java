/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

package javax.management.monitor;

import javax.management.ObjectName;

/**
 * Management interface of a string.  The counter
 * monitor continually checks the value of a string attribute.
 */
public interface StringMonitorMBean extends MonitorMBean {
  /**
   * Returns the derived gauge
   */
  public String getDerivedGauge(ObjectName name);
  
  /**
   * Returns the derived gauge timestamp
   */
  public long getDerivedGaugeTimeStamp(ObjectName name);
  
  /**
   * Returns the string to compare.
   */
  public String getStringToCompare();
  
  /**
   * Sets the string to compare.
   */
  public void setStringToCompare(String value)
    throws IllegalArgumentException;
  
  /**
   * Returns the notify flag
   */
  public boolean getNotifyMatch();
  
  /**
   * Sets the notify flag
   */
  public void setNotifyMatch(boolean value)
    throws IllegalArgumentException;
  
  /**
   * Returns the notify flag
   */
  public boolean getNotifyDiffer();
  
  /**
   * Sets the notify flag
   */
  public void setNotifyDiffer(boolean value)
    throws IllegalArgumentException;
  
  /**
   * Returns the derived gauge
   *
   * @deprecated
   */
  public String getDerivedGauge();
  
  /**
   * Returns the derived gauge timestamp
   *
   * @deprecated
   */
  public long getDerivedGaugeTimeStamp();
  
}

  
