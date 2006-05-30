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

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.management.ObjectName;
import javax.management.MBeanOperationInfo;

import com.caucho.log.Log;

import com.caucho.jmx.Jmx;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadWritePair;

import com.caucho.mbeans.server.ClusterClientMBean;
import com.caucho.util.L10N;

/**
 * Defines a connection to one of the servers in a distribution group.
 */
public class ClusterServer
  implements ClusterClientMBean
{
  private static final Logger log = Log.open(ClusterServer.class);
  private static final L10N L = new L10N(ClusterServer.class);

  private ObjectName _objectName;

  private Cluster _cluster;
  private ClusterGroup _group;

  private ClusterPort _port;

  private int _groupIndex;
  private Path _tcpPath;

  private ClusterClient _client;

  public ClusterServer()
  {
  }

  /**
   * Sets the owning cluster.
   */
  public void setCluster(Cluster cluster)
  {
    _cluster = cluster;
  }

  /**
   * Gets the owning cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName;
  }

  /**
   * Sets the owning group.
   */
  public void setGroup(ClusterGroup group)
  {
    _group = group;
  }

  /**
   * Gets the owning group.
   */
  public ClusterGroup getGroup()
  {
    return _group;
  }

  /**
   * Sets the ClusterPort.
   */
  public void setPort(ClusterPort port)
  {
    _port = port;
  }

  /**
   * Gets the cluster port.
   */
  public ClusterPort getClusterPort()
  {
    return _port;
  }

  /**
   * Returns the user-readable id of the target server.
   */
  public String getId()
  {
    return _port.getServerId();
  }

  /**
   * Returns the index of this connection in the connection group.
   */
  public int getIndex()
  {
    return _port.getIndex();
  }

  /**
   * Returns the hostname of the target server.
   */
  public String getHost()
  {
    return _port.getHost();
  }

  /**
   * Gets the port of the target server.
   */
  public int getPort()
  {
    return _port.getPort();
  }

  /**
   * Returns true if the target server is a backup.
   */
  public boolean isBackup()
  {
    return _port.isBackup();
  }

  /**
   * Gets the group index.
   */
  public int getGroupIndex()
  {
    return _groupIndex;
  }

  /**
   * Sets the group index.
   */
  public void setGroupIndex(int index)
  {
    _groupIndex = index;
  }

  /**
   * Returns the socket timeout when reading from the
   * target server.
   */
  public long getReadTimeout()
  {
    return _cluster.getClientReadTimeout();
  }

  /**
   * Returns the socket timeout when writing to the
   * target server.
   */
  public long getWriteTimeout()
  {
    return _cluster.getClientWriteTimeout();
  }

  /**
   * Returns the socket timeout when reading and writing to the
   * target server.
   */
  public long getTimeout()
  {
    return getReadTimeout();
  }

  /**
   * Returns how long the connection can be cached in the free pool.
   */
  public long getLiveTime()
  {
    return _cluster.getClientLiveTime();
  }

  /**
   * Returns how long the connection will be treated as dead.
   */
  public long getDeadTime()
  {
    return _cluster.getClientDeadTime();
  }

  /**
   * Returns the number of active connections.
   */
  public int getActiveCount()
  {
    return _client.getActiveCount();
  }

  /**
   * Initialize
   */
  public void init()
    throws Exception
  {
    String host = getHost();

    if (host == null)
      host = "localhost";

    if (_port.isSSL())
      _tcpPath = Vfs.lookup("tcps://" + host + ":" + getPort());
    else
      _tcpPath = Vfs.lookup("tcp://" + host + ":" + getPort());

    _client = new ClusterClient(this);

    try {
      String clusterName = _cluster.getId();
      if (clusterName == null || clusterName.equals(""))
        clusterName = "default";

      _objectName = Jmx.getObjectName("type=ClusterClient," +
                                      "Cluster=" + clusterName +
                                      ",host=" + host +
                                      ",port=" + getPort());

      Jmx.register(this, _objectName);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    return _client.isDead();
  }

  /**
   * Returns true if active.
   */
  public boolean isActive()
  {
    return _client.isActive();
  }

  /**
   * Enable the client
   */
  public void enable()
  {
    _client.enable();
  }

  /**
   * Disable the client
   */
  public void disable()
  {
    _client.disable();
  }

  /**
   * Returns the client.
   */
  public ClusterClient getClient()
  {
    return _client;
  }

  /**
   * Open a read/write pair to the target srun connection.
   *
   * @return the socket's read/write pair.
   */
  ReadWritePair openTCPPair()
    throws IOException
  {
    return _tcpPath.openReadWrite();
  }

  /**
   * We now know that the server is live.
   */
  public void wake()
  {
    _client.wake();
  }

  /**
   * Returns true if can connect to the client.
   */
  public boolean canConnect()
  {
    try {
      wake();

      ClusterStream stream = _client.open();

      if (stream != null) {
        stream.close();

        return true;
      }

      return false;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  /**
   * We now know that the server is live.
   */
  public void close()
  {
    _client.close();
  }

  public String toString()
  {
    return ("ClusterServer[id=" + _port.getServerId() +
            " index=" + _port.getIndex() +
            " host=" + _port.getHost() + ":" + _port.getPort() +
            " cluster=" + _cluster.getId() + "]");
  }
}
