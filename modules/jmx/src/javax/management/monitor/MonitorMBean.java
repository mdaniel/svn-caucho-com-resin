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
 * Common interface for monitor mbeans.
 */
public interface MonitorMBean {
  /**
   * Start the monitor.
   */
  public void start();
  
  /**
   * Stops the monitor.
   */
  public void stop();
  
  /**
   * Adds an observed object.
   *
   * @param object the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public void addObservedObject(ObjectName object);
  
  /**
   * Removes an observed object.
   *
   * @param object the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public void removeObservedObject(ObjectName object);
  
  /**
   * Returns true if the object is observed.
   *
   * @param object the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public boolean containsObservedObject(ObjectName object);
  
  /**
   * Returns the observed objects.
   *
   * @return the name of the observed object
   */
  public ObjectName []getObservedObjects();
  
  /**
   * Returns the observed attribute
   *
   * @return the name of the observed attribute
   */
  public String getObservedAttribute();
  
  /**
   * Sets the attribute to observe.
   *
   * @param name the name of the observed attribute
   */
  public void setObservedAttribute(String name);
  
  /**
   * Gets the period of observation in milliseconds.
   *
   * @return the granularity period
   */
  public long getGranularityPeriod();
  
  /**
   * Sets the period of observation in milliseconds.
   *
   * @param period the granularity period
   */
  public void setGranularityPeriod(long period);
  
  /**
   * Tests if the monitor is active
   *
   * @return true if the monitor is active.
   */
  public boolean isActive();
  
  /**
   * Returns the observed object.
   *
   * @deprecated
   *
   * @return the name of the observed object
   */
  public ObjectName getObservedObject();
  
  /**
   * Sets the observed object.
   *
   * @deprecated
   *
   * @param name the name of the observed object
   */
  public void setObservedObject(ObjectName name);
}

  
