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


package com.caucho.server.cluster;

import java.io.Serializable;

import java.util.Date;

import javax.management.ObjectName;

import com.caucho.management.server.*;

/**
 * Implementation of the ClusterClient's administration mbean.
 */
public class ClusterClientAdmin extends AbstractManagedObject
  implements ServerConnectorMXBean
{
  private final ClusterClient _client;

  public ClusterClientAdmin(ClusterClient client)
  {
    _client = client;
  }

  /**
   * Returns the -server id.
   */
  public String getName()
  {
    return _client.getServer().getId();
  }

  /**
   * Returns the owning cluster's object name.
   */
  public ClusterMXBean getCluster()
  {
    return _client.getServer().getCluster().getAdmin();
  }

  /**
   * Returns the cluster index.
   */
  public int getClusterIndex()
  {
    return _client.getServer().getIndex();
  }

  /**
   * Returns the server's IP address.
   */
  public String getAddress()
  {
    return _client.getServer().getAddress();
  }

  /**
   * Returns the server's port.
   */
  public int getPort()
  {
    return _client.getServer().getPort();
  }

  //
  // Client connection/load-balancing parameters
  //

  /**
   * Returns the time the client will consider the connection dead
   * before retrying.
   */
  public long getFailRecoverTime()
  {
    return _client.getServer().getFailRecoverTime();
  }

  /**
   * Returns the maximum time a socket can remain idle in the pool.
   */
  public long getMaxIdleTime()
  {
    return _client.getServer().getMaxIdleTime();
  }

  /**
   * Returns the connect timeout for a client.
   */
  public long getConnectTimeout()
  {
    return _client.getServer().getClientConnectTimeout();
  }

  /**
   * Returns the read timeout for a client.
   */
  public long getReadTimeout()
  {
    return _client.getServer().getClientReadTimeout();
  }

  /**
   * Returns the write timeout for a client.
   */
  public long getWriteTimeout()
  {
    return _client.getServer().getClientWriteTimeout();
  }

  /**
   * Returns the warmup time in milliseconds.
   */
  public long getWarmupTime()
  {
    return _client.getServer().getWarmupTime();
  }

  /**
   * Returns the load-balance weight.
   */
  public int getWeight()
  {
    return _client.getServer().getClientWeight();
  }

  //
  // State
  //

  public String getState()
  {
    return _client.getState();
  }

  public int getConnectionActiveCount()
  {
    return _client.getActiveCount();
  }

  public int getConnectionIdleCount()
  {
    return _client.getIdleCount();
  }

  public long getConnectionNewCountTotal()
  {
    return _client.getConnectCountTotal();
  }

  public long getConnectionFailCountTotal()
  {
    return _client.getFailCountTotal();
  }

  public Date getLastFailTime()
  {
    return _client.getLastFailTime();
  }

  public long getConnectionBusyCountTotal()
  {
    return _client.getBusyCountTotal();
  }

  public Date getLastBusyTime()
  {
    return _client.getLastBusyTime();
  }

  public long getConnectionKeepaliveCountTotal()
  {
    return _client.getServer().getClient().getKeepaliveCountTotal();
  }

  public void start()
  {
    _client.start();
  }

  public void stop()
  {
    _client.stop();
  }

  public void enableSessionOnly()
  {
    _client.enableSessionOnly();
  }

  public boolean ping()
  {
    return _client.getServer().canConnect();
  }

  public String toString()
  {
    return "ClusterClientAdmin[" + getObjectName() + "]";
  }
}
