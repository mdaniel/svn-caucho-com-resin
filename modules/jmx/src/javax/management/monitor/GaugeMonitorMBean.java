/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 * Management interface of a gauge monitor.  The gauge
 * monitor continually checks the value of a numeric attribute.
 */
public interface GaugeMonitorMBean extends MonitorMBean {
  /**
   * Returns the derived gauge for the MBean.
   */
  public Number getDerivedGauge(ObjectName object);

  /**
   * Returns the derived gauge timestamp for the MBean.
   */
  public long getDerivedGaugeTimeStamp(ObjectName object);

  /**
   * Returns the upper threshold for the MBean.
   */
  public Number getHighThreshold();

  /**
   * Returns the lower threshold for the MBean.
   */
  public Number getLowThreshold();

  /**
   * Sets the upper threshold for the MBean.
   */
  public void setThresholds(Number highValue, Number lowValue)
    throws IllegalArgumentException;

  /**
   * Gets the notification
   */
  public boolean getNotifyHigh();

  /**
   * Sets the notification
   */
  public void setNotifyHigh(boolean value)
    throws IllegalArgumentException;

  /**
   * Sets the notification
   */
  public boolean getNotifyLow();

  /**
   * Sets the notification
   */
  public void setNotifyLow(boolean value)
    throws IllegalArgumentException;

  /**
   * Gets the difference mode flag.
   */
  public boolean getDifferenceMode();

  /**
   * Sets the difference mode flag.
   */
  public void setDifferenceMode(boolean value)
    throws IllegalArgumentException;
  
  /**
   * Returns the derived gauge for the MBean.
   *
   * @deprecated
   */
  public Number getDerivedGauge();

  /**
   * Returns the derived gauge timestamp for the MBean.
   *
   * @deprecated
   */
  public long getDerivedGaugeTimeStamp();

}
  
