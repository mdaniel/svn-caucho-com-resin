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
 * Management interface of a counter monitor.  The counter
 * monitor continually checks the value of a numeric attribute.
 */
public interface CounterMonitorMBean extends MonitorMBean {
  /**
   * Returns the derived gauge
   */
  public Number getDerivedGauge(ObjectName name);
  
  /**
   * Returns the derived gauge timestamp
   */
  public long getDerivedGaugeTimeStamp(ObjectName name);
  
  /**
   * Returns the threshold value
   */
  public Number getThreshold(ObjectName name);
  
  /**
   * Gets the threshold value
   */
  public Number getInitThreshold()
    throws IllegalArgumentException;
  
  /**
   * Sets the threshold value
   */
  public void setInitThreshold(Number value)
    throws IllegalArgumentException;
  
  /**
   * Returns the offset value
   */
  public Number getOffset();
  
  /**
   * Sets the offset value
   */
  public void setOffset(Number value)
    throws IllegalArgumentException;
  
  /**
   * Returns the modulus value
   */
  public Number getModulus();
  
  /**
   * Sets the modulus value
   */
  public void setModulus(Number value)
    throws IllegalArgumentException;
  
  /**
   * Returns the notify flag
   */
  public boolean getNotify();
  
  /**
   * Sets the notify flag
   */
  public void setNotify(boolean value)
    throws IllegalArgumentException;
  
  /**
   * Returns the differentMode flag
   */
  public boolean getDifferenceMode();
  
  /**
   * Sets the differenceMode flag
   */
  public void setDifferenceMode(boolean value)
    throws IllegalArgumentException;
  
  /**
   * Returns the derived gauge
   *
   * @deprecated
   */
  public Number getDerivedGauge();
  
  /**
   * Returns the derived gauge timestamp
   *
   * @deprecated
   */
  public long getDerivedGaugeTimeStamp();
  
  /**
   * Returns the threshold value
   *
   * @deprecated
   */
  public Number getThreshold();
  
  /**
   * Sets the threshold value
   *
   * @deprecated
   */
  public void setThreshold(Number value)
    throws IllegalArgumentException;
}

  
