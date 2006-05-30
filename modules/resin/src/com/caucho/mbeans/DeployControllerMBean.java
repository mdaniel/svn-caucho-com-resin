/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.mbeans;

import com.caucho.jmx.MBeanAttribute;
import com.caucho.jmx.MBeanOperation;
import com.caucho.jmx.MBeanAttributeCategory;

import java.util.Date;

import javax.management.ObjectName;

/**
 * Management interface for the deploy controller.
 */
public interface DeployControllerMBean
{
  /**
   * Returns the ObjectName.
   */
  @MBeanAttribute(description="The JMX ObjectName for the MBean",
                  category = MBeanAttributeCategory.CONFIGURATION)
  public ObjectName getObjectName();

  /**
   * Returns the startup mode, one of "default", "automatic", "lazy", or "manual".
   */
  @MBeanAttribute(description="The startup-mode, one of `default', `automatic',"
                              + " `lazy', or `manual'",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getStartupMode();

  /**
   * Returns the redeploy mode, one of "default", "automatic", "lazy", or "manual".
   */
  @MBeanAttribute(description="The redeploy-mode, one of `default', `automatic',"
                              + " `lazy', or `manual'",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getRedeployMode();

  /**
   * Returns the interval between redploy checks.
   */
  @MBeanAttribute(description="The millisecond interval between checks for the"
                              + " need to redeploy",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public long getRedeployCheckInterval();

  /**
   * Returns the controller's state.
   */
  @MBeanAttribute(description="The lifecycle state",
                  category =MBeanAttributeCategory.STATISTIC)
  public String getState();

  /**
   * Returns the time the controller was last started.
   */
  @MBeanAttribute(description="The time of the last start",
                  category =MBeanAttributeCategory.STATISTIC)
  public Date getStartTime();

  /**
   * Starts the instance.
   */
  @MBeanOperation(description="Start")
  public void start()
    throws Exception;

  /**
   * Stops the instance.
   */
  @MBeanOperation(description="Stop")
  public void stop()
    throws Exception;

  /**
   * Restarts the instance.
   */
  @MBeanOperation(description="Restart (Stop then Start)")
  public void restart()
    throws Exception;

  /**
   * Restarts the instance if any changes are detected.
   */
  @MBeanOperation(description="Update")
  public void update()
    throws Exception;
}
