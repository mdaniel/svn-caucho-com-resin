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

import java.util.Date;

import javax.management.ObjectName;

import com.caucho.jmx.Description;

/**
 * A client-view of a cluster's server.  The load balancer and
 * persistent store will use the ClusterClient to communicate to
 * other servers in the cluster.
 *
 * The JMX name looks like:
 * <pre>
 *   resin:type=ClusterClient,name=web-a
 * </pre>
 */
@Description("Client-view of a cluster's server, i.e. a target server with which this instance can communicate")
public interface ClusterClientMBean {
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @Description("The JMX ObjectName for the MBean")
  public String getObjectName();

  /**
   * The server id that identifies the target server.  Also, the
   * JMX name of the mbean.
   */
  @Description("The -server name for this server.  Also, the JMX name property")
  public String getName();

  /**
   * The JMX type of this MBean.
   */
  @Description("The JMX type property, 'ClusterClient'")
  public String getType();

  /**
   * The object name of the containing cluster.
   */
  @Description("The object name of the containing Cluster")
  public String getCluster();

  /**
   * The cluster index of the server.
   */
  @Description("The index of this server in the cluster, used for distributed objects.")
  public int getClusterIndex();

  /**
   * The timeout in milliseconds for connecting to the server.
   */
  @Description("Timeout for a client connect to the server")
  public long getConnectTimeout();

  /**
   * Returns the ip address or host name of the server.
   */
  @Description("The IP address or host name of the server")
  public String getAddress();

  /**
   * Returns the resin/admin port number of the server.
   */
  @Description("The port number of the target server")
  public int getPort();

  /**
   * Returns the timeout for assuming a target server remains unavailable once
   * a connection attempt fails. When the timeout period elapses another attempt
   * is made to connect to the target server
   */
  @Description("Timeout for assuming a target server remains" +
	      " unavailable once a connection attempt fails." +
	      " When the timeout period elapses another" +
	      " attempt is made to connect to the target server")
  public long getFailRecoverTime();

  /**
   * Returns the timeout for an idle socket that is connected to the target
   * server. If the socket is not used within the timeout period the idle
   * connection is closed.
   */
  @Description("Timeout for an idle socket that is connected" +
               " to the target server. If the socket is not" +
	       " used within the timeout period the idle" +
	       " connection is closed")
  public long getMaxIdleTime();

  /**
   * Returns the timeout to use for reads when communicating with
   * the target server.
   */
  @Description("Timeout for a client read from the server")
  public long getReadTimeout();

  /**
   * Returns the slow-start time in milliseconds.
   */
  @Description("Returns the slow-start time in milliseconds for ramping up connections to the server")
  public long getSlowStartTime();

  /**
   * Returns the load-balancer weight, defaulting to 100.
   *
   */
  @Description("The load balance weight.  Weights over 100 will get more traffic and weights less than 100 will get less traffic")
  public int getWeight();

  /**
   * Returns the timeout to use for writes when communicating with
   * the target server.
   */
  @Description("Timeout for a client write to the server")
  public long getWriteTimeout();

  //
  // State attributes
  //

  /**
   * Returns the lifecycle state.
   */
  @Description("The lifecycle state of the client")
  public String getState();

  //
  // Statistics attributes
  //

  /**
   * Returns the number of connections actively being used to communicate with
   * the target server.
   */
  @Description("The number of connections actively being used"
    + " to communicate with the target server")
  public int getActiveCount();

  /**
   * Returns the number of open but currently unused connections to the
   * target server.
   */
  @Description("The number of idle connections in the connection pool")
  public int getIdleCount();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @Description("The number of new connections that have been made" +
	       " to the target server")
  public long getConnectTotalCount();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @Description("The number of keepalive connections that have been made" +
	       " to the target server")
  public long getKeepaliveTotalCount();

  /**
   * Returns the number of connections which could not connect
   * to the target server.
   */
  @Description("The number of failed connections attempts" +
	       " to the target server")
  public long getFailTotalCount();

  /**
   * Returns the time of the last failure.
   */
  @Description("The time of the last failed connection")
  public Date getLastFailTime();

  /**
   * Enables connections to the target server.
   */
  @Description("Enables connections to the target server")
  public void start();

  /**
   * Disables connections to the target server.
   */
  @Description("Disables connections to the target server")
  public void stop();

  /**
   * Returns true if a connection can be made to the target server.
   */
  @Description("Tries to connect to the target server, returning true if successful")
  public boolean ping();
}
