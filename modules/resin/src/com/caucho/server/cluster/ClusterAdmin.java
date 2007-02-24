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
 * @author Sam
 */


package com.caucho.server.cluster;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.HostMXBean;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ServerConnectorMXBean;

public class ClusterAdmin extends AbstractManagedObject
  implements ClusterMXBean
{
  private final Cluster _cluster;

  public ClusterAdmin(Cluster cluster)
  {
    _cluster = cluster;
  }

  public String getName()
  {
    return _cluster.getId();
  }

  public PortMXBean getPort()
  {
    ClusterServer clusterServer = _cluster.getSelfServer();

    if (clusterServer == null)
      return null;

     return clusterServer.getClusterPort().getAdmin();
  }

  public ResinMXBean getResin()
  {
    return _cluster.getResin().getAdmin();
  }

  public PersistentStoreMXBean getPersistentStore()
  {
    return null;
  }

  public HostMXBean []getHosts()
  {
    return new HostMXBean[0];
  }

  public ServerConnectorMXBean []getServers()
  {
    ClusterServer selfServer = _cluster.getSelfServer();

    ClusterServer[] serverList = _cluster.getServerList();

    int len = serverList.length;

    if (selfServer != null)
      len--;

    ServerConnectorMXBean []serverMBeans = new ServerConnectorMXBean[len];

    int j = 0;

    for (int i = 0; i < serverList.length; i++) {
      ClusterServer server = serverList[i];

      if (server != selfServer)
        serverMBeans[j++] = server.getServerConnector().getAdmin();
    }

    return serverMBeans;
  }

  public String toString()
  {
    return "ClusterAdmin[" + getObjectName() + "]";
  }
}
