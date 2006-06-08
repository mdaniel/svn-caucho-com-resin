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

package com.caucho.mbeans.server;

import com.caucho.jmx.MBean;
import com.caucho.jmx.MBeanAttribute;
import com.caucho.jmx.MBeanAttributeCategory;
import com.caucho.jmx.MBeanOperation;

import javax.management.ObjectName;

/**
 * A representation of a member of a cluster, a target server
 * with which this instance can communicate.
 */
@MBean(description="A representation of a member of a cluster, a target server with which this instance can communicate")
public interface ClusterClientMBean {
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @MBeanAttribute(description="The JMX ObjectName for the MBean",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public String getObjectName();

  /**
  * The server id that identifies the target server.
   */
  @MBeanAttribute(description="The server id that identifies the target server",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getServerId();

  /**
   */
  @MBeanAttribute(description="The unique index of the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public int getIndex();

  /**
   * Returns the ip address or host name of the target server.
   */
  @MBeanAttribute(description="The ip address or host name of the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public String getHost();

  /**
   * Returns the srun port number of the target server.
   */
  @MBeanAttribute(description="The srun port number of the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public int getPort();

  /**
   * Returns true if the target server is only used by the load balancer
   * as a backup.
   */
  @MBeanAttribute(description="True if the target server is only used by the"  +
                              " load balancer as a backup",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public boolean isBackup();

  /**
   * Returns the timeout to use for reads when communicating with
   * the target server.
   */
  @MBeanAttribute(description="Timeout to use for reads when communicating" +
                              " with the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public long getReadTimeout();

  /**
   * Returns the timeout to use for writes when communicating with
   * the target server.
   */
  @MBeanAttribute(description="Timeout to use for writes when communicating" +
                              " with the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public long getWriteTimeout();

  /**
   * Returns the timeout for an idle socket that is connected to the target
   * server. If the socket is not used within the timeout period the idle
   * connection is closed.
   */
  @MBeanAttribute(description="Timeout for an idle socket that is connected" +
                              " to the target server. If the socket is not" +
                              " used within the timeout period the idle" +
                              " connection is closed",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public long getMaxIdleTime();

  /**
   * Returns the timeout for assuming a target server remains unavailable once
   * a connection attempt fails. When the timeout period elapses another attempt
   * is made to connect to the target server
   */
  @MBeanAttribute(description="Timeout for assuming a target server remains" +
                              " unavailable once a connection attempt fails." +
                              " When the timeout period elapses another" +
                              " attempt is made to connect to the target server",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public long getFailRecoverTime();

  /**
   * Returns the lifecycle state.
   */
  @MBeanAttribute(description="The lifecycle state",
                  category=MBeanAttributeCategory.STATISTIC)
  public String getState();

  @MBeanAttribute(description="True if the target server is not reachable",
                  category=MBeanAttributeCategory.STATISTIC)
  public boolean isDead();

  /**
   * Returns The number of connections actively being used to communicate with
   * the target server.
   */
  @MBeanAttribute(description="The number of connections actively being used"
                              + " to communicate with the target server",
                  category=MBeanAttributeCategory.STATISTIC)
  public int getActiveConnectionCount();

  /**
   * Returns the number of open but currently unused connections to the
   * target server.
   */
  @MBeanAttribute(description="The number of open but currently unused" +
                              " connections to the target server",
                  category=MBeanAttributeCategory.STATISTIC)
  public int getIdleConnectionCount();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @MBeanAttribute(description="The number of connections that have been made" +
                              " to the target server",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getLifetimeConnectionCount();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @MBeanAttribute(description="The number of keepalive connections that have been made" +
                              " to the target server",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getLifetimeKeepaliveCount();

  /**
   * Enables connections to the target server.
   */
  @MBeanOperation(description="Enables connections to the target server")
  public void start();

  /**
   * Disables connections to the target server.
   */
  @MBeanOperation(description="Disables connections to the target server")
  public void stop();

  /**
   * Returns true if a connection can be made to the target server.
   */
  @MBeanOperation(description="Returns true if a connection can be made to the" +
                              " target server")
  public boolean canConnect();
}
