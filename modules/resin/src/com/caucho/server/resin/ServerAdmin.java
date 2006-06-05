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

import javax.management.ObjectName;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.port.Port;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.host.HostController;

import com.caucho.mbeans.server.ServerMBean;
import com.caucho.jmx.MBeanAttribute;
import com.caucho.jmx.MBeanAttributeCategory;

import java.util.ArrayList;

public class ServerAdmin extends DeployControllerAdmin<ServerController>
  implements ServerMBean
{
  private static final L10N L = new L10N(ServerAdmin.class);

  ServerAdmin(ServerController controller)
  {
    super(controller);
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

  public boolean isSelectManager()
  {
    return getDeployInstance().getSelectManager() != null;
  }

  /**
   * Returns the id
   */
  public String getId()
  {
    return getController().getId();
  }

  public ObjectName []getPortObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return new ObjectName[0];

    ArrayList<ObjectName> portNameList = new ArrayList<ObjectName>();

    for (Port port : server.getPorts()) {
      ObjectName name = port.getObjectName();

      if (name != null)
        portNameList.add(name);
    }

    return portNameList.toArray(new ObjectName[portNameList.size()]);
  }

  public String []getClusterObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return new String[0];

    ArrayList<String> clusterNameList = new ArrayList<String>();

    for (Cluster cluster : server.getClusters()) {
      String name = cluster.getObjectName();

      if (name != null)
        clusterNameList.add(name);
    }

    return clusterNameList.toArray(new String[clusterNameList.size()]);
  }

  public ObjectName []getHostObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return new ObjectName[0];

    ArrayList<ObjectName> hostNameList = new ArrayList<ObjectName>();

    for (HostController host : server.getHostControllers()) {
      ObjectName name = host.getObjectName();

      if (name != null)
        hostNameList.add(name);
    }

    return hostNameList.toArray(new ObjectName[hostNameList.size()]);
  }

  public int getTotalConnectionCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int connectionCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getTotalConnectionCount() >= 0) {
        if (connectionCount == -1)
          connectionCount = 0;

        connectionCount += port.getTotalConnectionCount();
      }
    }

    return connectionCount;
  }

  public int getActiveConnectionCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int activeConnectionCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getActiveConnectionCount() >= 0) {
        if (activeConnectionCount == -1)
          activeConnectionCount = 0;

        activeConnectionCount += port.getActiveConnectionCount();
      }
    }

    return activeConnectionCount;
  }

  public int getKeepaliveConnectionCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int keepaliveConnectionCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getKeepaliveConnectionCount() >= 0) {
        if (keepaliveConnectionCount == -1)
          keepaliveConnectionCount = 0;

        keepaliveConnectionCount += port.getKeepaliveConnectionCount();
      }
    }

    return keepaliveConnectionCount;
  }

  public int getSelectConnectionCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    int selectConnectionCount = -1;

    for (Port port : server.getPorts()) {
      if (port.getSelectConnectionCount() >= 0) {
        if (selectConnectionCount == -1)
          selectConnectionCount = 0;

        selectConnectionCount += port.getSelectConnectionCount();
      }
    }

    return selectConnectionCount;
  }

  public long getLifetimeConnectionCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeConnectionCount = 0;

    for (Port port : server.getPorts())
      lifetimeConnectionCount += port.getLifetimeConnectionCount();

    return lifetimeConnectionCount;
  }

  public long getLifetimeConnectionTime()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeConnectionTime = 0;

    for (Port port : server.getPorts())
      lifetimeConnectionTime += port.getLifetimeConnectionTime();

    return lifetimeConnectionTime;
  }

  public long getLifetimeReadBytes()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeReadBytes = 0;

    for (Port port : server.getPorts())
      lifetimeReadBytes += port.getLifetimeReadBytes();

    return lifetimeReadBytes;
  }

  public long getLifetimeWriteBytes()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeWriteBytes = 0;

    for (Port port : server.getPorts())
      lifetimeWriteBytes += port.getLifetimeWriteBytes();

    return lifetimeWriteBytes;
  }

  public long getLifetimeClientDisconnectCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeClientDisconnectCount = 0;

    for (Port port : server.getPorts())
      lifetimeClientDisconnectCount += port.getLifetimeClientDisconnectCount();

    return lifetimeClientDisconnectCount;
  }

  public long getLifetimeKeepaliveCount()
  {
    ServletServer server = getDeployInstance();

    if (server == null)
      return -1;

    long lifetimeKeepaliveCount = 0;

    for (Port port : server.getPorts())
      lifetimeKeepaliveCount += port.getLifetimeKeepaliveCount();

    return lifetimeKeepaliveCount;
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

  public long getInvocationCacheHitCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getInvocationCacheHitCount();
    else
      return -1;
  }

  public long getInvocationCacheMissCount()
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
