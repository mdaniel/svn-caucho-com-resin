/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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


package com.caucho.server.cluster;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;

import java.util.Date;

/**
 * Implementation of the ClusterServer's administration mbean.
 */
public class ClusterServerAdmin extends AbstractManagedObject
  implements ClusterServerMXBean
{
  private final ClusterServer _server;

  public ClusterServerAdmin(ClusterServer server)
  {
    _server = server;
  }

  /**
   * Returns the -server id.
   */
  public String getName()
  {
    return _server.getId();
  }

  @Override
  public String getType()
  {
    return "ClusterServer";
  }

  /**
   * Returns the owning cluster's object name.
   */
  public ClusterMXBean getCluster()
  {
    return _server.getCluster().getAdmin();
  }

  /**
   * Returns the cluster index.
   */
  public int getClusterIndex()
  {
    return _server.getIndex();
  }

  /**
   * Returns the server's IP address.
   */
  public String getAddress()
  {
    return _server.getClusterPort().getAddress();
  }

  /**
   * Returns the server's port.
   */
  public int getPort()
  {
    return _server.getPort();
  }

  /**
   * Returns true for a dynamic server
   */
  public boolean isDynamicServer()
  {
    return _server.isDynamic();
  }

  /**
   * Returns true for a triad server
   */
  public boolean isTriadServer()
  {
    return _server.isTriad();
  }

  /**
   * Returns true for the self server
   */
  public boolean isSelfServer()
  {
    return _server.isSelf();
  }

  //
  // Client connection/load-balancing parameters
  //

  /**
   * Returns the time the client will consider the connection dead
   * before retrying.
   */
  public long getRecoverTime()
  {
    return _server.getLoadBalanceRecoverTime();
  }

  /**
   * Returns the maximum time a socket can remain idle in the pool.
   */
  public long getIdleTime()
  {
    return _server.getLoadBalanceIdleTime();
  }

  /**
   * Returns the connect timeout for a client.
   */
  public long getConnectTimeout()
  {
    return _server.getLoadBalanceConnectTimeout();
  }

  /**
   * Returns the socket timeout for a client.
   */
  public long getSocketTimeout()
  {
    return _server.getLoadBalanceSocketTimeout();
  }

  /**
   * Returns the warmup time in milliseconds.
   */
  public long getWarmupTime()
  {
    return _server.getLoadBalanceWarmupTime();
  }

  /**
   * Returns the load-balance weight.
   */
  public int getWeight()
  {
    return _server.getLoadBalanceWeight();
  }

  //
  // State
  //

  public String getState()
  {
    ServerPool pool = _server.getServerPool();
    
    if (pool != null)
      return pool.getState();
    else
      return "self";
  }

  public int getConnectionActiveCount()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getActiveCount();
    else
      return 0;
  }

  public int getConnectionIdleCount()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getIdleCount();
    else
      return 0;
  }

  public long getConnectionNewCountTotal()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getConnectCountTotal();
    else
      return 0;
  }

  public long getConnectionFailCountTotal()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getFailCountTotal();
    else
      return 0;
  }

  public Date getLastFailTime()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getLastFailTime();
    else
      return null;
  }

  public Date getLastSuccessTime()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return new Date(pool.getLastSuccessTime());
    else
      return null;
  }

  public double getLatencyFactor()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getLatencyFactor();
    else
      return 0;
  }

  public long getConnectionBusyCountTotal()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getBusyCountTotal();
    else
      return 0;
  }

  public Date getLastBusyTime()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getLastBusyTime();
    else
      return null;
  }

  public long getConnectionKeepaliveCountTotal()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getKeepaliveCountTotal();
    else
      return 0;
  }

  public double getServerCpuLoadAvg()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.getCpuLoadAvg();
    else
      return 0;
  }

  public void start()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      pool.start();
  }

  public void stop()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      pool.stop();
  }

  public void enableSessionOnly()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      pool.enableSessionOnly();
  }

  public boolean ping()
  {
    ServerPool pool = _server.getServerPool();

    if (pool != null)
      return pool.canConnect();
    else
      return true;
  }

  /**
   * Remove the server as a dynamic server
   */
  public void removeDynamicServer()
  {
    ClusterServer clusterServer = _server;
    
    ClusterPort port = clusterServer.getClusterPort();
    
    clusterServer.getClusterPod().removeDynamicServer(clusterServer.getId(),
						      port.getAddress(),
						      port.getPort());
  }

  protected void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() +  "[" + getObjectName() + "]";
  }
}
