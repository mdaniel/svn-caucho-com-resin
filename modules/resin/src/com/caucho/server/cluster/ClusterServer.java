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

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.management.ObjectName;

import com.caucho.log.Log;

import com.caucho.jmx.Jmx;

import com.caucho.management.server.ServerConnectorMXBean;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadWritePair;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * Defines a member of the cluster.
 *
 * A {@link ClusterClient} obtained with {@link #getClient} is used to actually
 * communicate with this ClusterServer when it is active in another instance of
 * Resin .
 */
public class ClusterServer {
  private static final Logger log = Log.open(ClusterServer.class);
  private static final L10N L = new L10N(ClusterServer.class);

  private ObjectName _objectName;

  private Cluster _cluster;
  private ClusterGroup _group;

  private ClusterPort _port;

  private int _srunIndex;
  private Path _tcpPath;

  private ClusterClient _client;

  private ClusterClientAdmin _admin;

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
   * Returns the admin.
   */
  public ServerConnectorMXBean getAdmin()
  {
    return _admin;
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
  public String getAddress()
  {
    return _port.getAddress();
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
   * Returns the time in milliseconds for the slow start throttling.
   */
  public long getWarmupTime()
  {
    return _cluster.getClientWarmupTime();
  }

  /**
   * Returns the socket timeout when connecting to the
   * target server.
   */
  public long getClientConnectTimeout()
  {
    return _cluster.getClientConnectTimeout();
  }

  /**
   * Returns the socket timeout when reading from the
   * target server.
   */
  public long getClientReadTimeout()
  {
    return _cluster.getClientReadTimeout();
  }

  /**
   * Returns the socket timeout when writing to the
   * target server.
   */
  public long getClientWriteTimeout()
  {
    return _cluster.getClientWriteTimeout();
  }

   /**
   * @deprecated
   *
   * Use {@link #getReadTimeout} or {@link #getWriteTimeout}
   */
  public long getTimeout()
  {
    return getClientReadTimeout();
  }

  /**
   * Returns how long the connection can be cached in the free pool.
   */
  public long getMaxIdleTime()
  {
    return _cluster.getClientMaxIdleTime();
  }

  /**
   * Returns how long the connection will be treated as dead.
   */
  public long getFailRecoverTime()
  {
    return _cluster.getClientFailRecoverTime();
  }

  /**
   * Returns the load balance weight.
   */
  public int getClientWeight()
  {
    return _port.getClientWeight();
  }

  /**
   * Initialize
   */
  public void init()
    throws Exception
  {
    String address = getAddress();

    if (address == null)
      address = "localhost";

    HashMap<String,Object> attr = new HashMap<String,Object>();
    attr.put("connect-timeout", new Long(getClientConnectTimeout()));

    if (_port.isSSL())
      _tcpPath = Vfs.lookup("tcps://" + address + ":" + getPort(), attr);
    else
      _tcpPath = Vfs.lookup("tcp://" + address + ":" + getPort(), attr);

    _client = new ClusterClient(this);

    _admin = new ClusterClientAdmin(_client);

    try {
      String name = getId();

      if (name == null)
        name = "";

      _admin.register();
      /*
      _objectName = new ObjectName("resin:type=ClusterServer,name=" + name);

      Jmx.register(_admin, _objectName);
      */
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    return ! _client.isActive();
  }

  /**
   * Returns true if active.
   */
  public boolean isActive()
  {
    return _client.isEnabled();
  }

  /**
   * Enable the client
   */
  public void enable()
  {
    _client.start();
  }

  /**
   * Disable the client
   */
  public void disable()
  {
    _client.stop();
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
        stream.free();

        return true;
      }

      return false;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  /**
   * Generate the primary, secondary, tertiary, returning the value encoded
   * in a long.
   */
  public long generateBackupCode()
  {
    ClusterServer []srunList = getCluster().getServerList();
    int srunLength = srunList.length;

    long index = _port.getIndex();
    long backupCode = index;
    
    long backupLength = srunLength;
    if (backupLength < 3)
      backupLength = 3;
    int backup;

    if (srunLength <= 1) {
      backup = 0;
      backupCode |= 1L << 16;
    }
    else if (srunLength == 2) {
      backup = 0;
      
      backupCode |= ((index + 1L) % 2) << 16;
    }
    else {
      int sublen = srunLength - 1;
      if (sublen > 7)
	sublen = 7;
	
      backup = RandomUtil.nextInt(sublen);
      
      backupCode |= ((index + backup + 1L) % backupLength) << 16;
    }

    if (srunLength <= 2)
      backupCode |= 2L << 32;
    else {
      int sublen = srunLength - 2;
      if (sublen > 6)
	sublen = 6;

      int third = RandomUtil.nextInt(sublen);

      if (backup <= third)
	third += 1;

      backupCode |= ((index + third + 1) % backupLength) << 32;
    }

    return backupCode;
  }

  /**
   * Close any clients.
   */
  public void close()
  {
    _client.close();
  }

  public String toString()
  {
    return ("ClusterServer[id=" + _port.getServerId() +
            " index=" + _port.getIndex() +
            " address=" + _port.getAddress() + ":" + _port.getPort() +
            " cluster=" + _cluster.getId() + "]");
  }

}
