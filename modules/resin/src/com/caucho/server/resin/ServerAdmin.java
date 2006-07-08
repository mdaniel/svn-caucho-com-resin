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

package com.caucho.server.resin;

import java.util.Date;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.port.Port;
import com.caucho.server.port.AbstractSelectManager;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.host.HostController;

import com.caucho.management.server.*;

import java.util.ArrayList;

public class ServerAdmin extends DeployControllerAdmin<ServerController>
  implements ServerMXBean
{
  private static final L10N L = new L10N(ServerAdmin.class);

  ServerAdmin(ServerController controller)
  {
    super(controller);
  }

  public String getName()
  {
    return null;
  }

  @Override
  public String getRootDirectory()
  {
    Path path = getController().getRootDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  public boolean isSelectManagerEnabled()
  {
    AbstractSelectManager manager = getDeployInstance().getSelectManager();

    return manager != null;
  }

  /**
   * Returns the id
   */
  public String getId()
  {
    return getController().getId();
  }
  
  public ThreadPoolMXBean getThreadPool()
  {
    return getController().getThreadPool();
  }

  public PortMXBean []getPorts()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return new PortMXBean[0];

    ArrayList<PortMXBean> portList = new ArrayList<PortMXBean>();

    for (Port port : server.getPorts()) {
      PortMXBean admin = port.getAdmin();

      if (admin != null)
        portList.add(admin);
    }

    return portList.toArray(new PortMXBean[portList.size()]);
  }

  public PortMXBean getClusterPort()
  {
    return null;
  }

  public ClusterMXBean getCluster()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return null;
    else {
      Cluster cluster = server.getCluster();

      if (cluster != null)
	return cluster.getAdmin();
      else
	return null;
    }
  }

  public String getLocalHost()
  {
    return CauchoSystem.getLocalHost();
  }

  public boolean isDetailedStatistics()
  {
    return CauchoSystem.isDetailedStatistics();
  }

  public HostMXBean []getHosts()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return new HostMXBean[0];

    ArrayList<HostMXBean> hostList = new ArrayList<HostMXBean>();

    for (HostController host : server.getHostControllers()) {
      HostMXBean admin = host.getAdmin();

      if (admin != null)
        hostList.add(admin);
    }

    // XXX: sort

    return hostList.toArray(new HostMXBean[hostList.size()]);
  }

  public String getState()
  {
    return getController().getState();
  }

  public Date getInitialStartTime()
  {
    return new Date(getController().getStartTime());
  }

  public Date getStartTime()
  {
    return new Date(getController().getStartTime());
  }

  public int getThreadActiveCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int activeThreadCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getActiveThreadCount() >= 0) {
        if (activeThreadCount == -1)
          activeThreadCount = 0;

        activeThreadCount += port.getActiveThreadCount();
      }
    }

    return activeThreadCount;
  }

  public int getThreadKeepaliveCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int keepaliveThreadCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getKeepaliveConnectionCount() >= 0) {
        if (keepaliveThreadCount == -1)
          keepaliveThreadCount = 0;

        keepaliveThreadCount += port.getKeepaliveConnectionCount();
      }
    }

    return keepaliveThreadCount;
  }

  public int getSelectKeepaliveCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int keepaliveSelectCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getSelectConnectionCount() >= 0) {
        if (keepaliveSelectCount == -1)
          keepaliveSelectCount = 0;

        keepaliveSelectCount += port.getSelectConnectionCount();
      }
    }

    return keepaliveSelectCount;
  }

  public long getRequestCountTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeRequestCount = 0;

    for (Port port : server.getPorts())
      lifetimeRequestCount += port.getLifetimeRequestCount();

    return lifetimeRequestCount;
  }

  public long getRequestTimeTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeRequestTime = 0;

    for (Port port : server.getPorts())
      lifetimeRequestTime += port.getLifetimeRequestTime();

    return lifetimeRequestTime;
  }

  public long getRequestReadBytesTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeReadBytes = 0;

    for (Port port : server.getPorts())
      lifetimeReadBytes += port.getLifetimeReadBytes();

    return lifetimeReadBytes;
  }

  public long getRequestWriteBytesTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeWriteBytes = 0;

    for (Port port : server.getPorts())
      lifetimeWriteBytes += port.getLifetimeWriteBytes();

    return lifetimeWriteBytes;
  }

  public long getClientDisconnectCountTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeClientDisconnectCount = 0;

    for (Port port : server.getPorts())
      lifetimeClientDisconnectCount += port.getLifetimeClientDisconnectCount();

    return lifetimeClientDisconnectCount;
  }

  public long getKeepaliveCountTotal()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeKeepaliveCount = 0;

    for (Port port : server.getPorts())
      lifetimeKeepaliveCount += port.getLifetimeKeepaliveCount();

    return lifetimeKeepaliveCount;
  }

  public long getRuntimeMemory()
  {
    return Runtime.getRuntime().totalMemory();
  }

  public long getRuntimeMemoryFree()
  {
    return Runtime.getRuntime().freeMemory();
  }

  public void restart()
  {
    getController().restart();
  }

  public void clearCache()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      server.clearCache();
  }

  public void clearCacheByPattern(String hostRegexp, String urlRegexp)
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      server.clearCacheByPattern(hostRegexp, urlRegexp);
  }

  public long getInvocationCacheHitCountTotal()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getInvocationCacheHitCount();
    else
      return -1;
  }

  public long getInvocationCacheMissCountTotal()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getInvocationCacheMissCount();
    else
      return -1;
  }

  public long getProxyCacheHitCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getProxyCacheHitCount();
    else
      return -1;
  }

  public long getProxyCacheMissCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getProxyCacheMissCount();
    else
      return -1;
  }

  protected ServletServer getDeployInstance()
  {
    return getController().getDeployInstance();
  }
}
