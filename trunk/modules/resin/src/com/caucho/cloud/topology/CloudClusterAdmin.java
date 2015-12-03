/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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


package com.caucho.cloud.topology;

import java.util.ArrayList;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.management.server.HostMXBean;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.server.resin.Resin;

public class CloudClusterAdmin extends AbstractManagedObject
  implements ClusterMXBean
{
  private final CloudCluster _cluster;
  private ResinMXBean _resinAdmin;

  public CloudClusterAdmin(CloudCluster cluster)
  {
    _cluster = cluster;
    Resin resin = Resin.getCurrent();
    
    if (resin != null)
      _resinAdmin = resin.getAdmin();
  }

  @Override
  public String getName()
  {
    return _cluster.getId();
  }
  
  @Override
  public HostMXBean []getHosts()
  {
    return new HostMXBean[0];
  }
  
  public PortMXBean getPort()
  {
    /*
    ClusterServer clusterServer = _cluster.getSelfServer();

    if (clusterServer == null)
      return null;

     return clusterServer.getClusterPort().getAdmin();
    */
    return null;
  }

  @Override
  public ResinMXBean getResin()
  {
    if (_resinAdmin == null && Resin.getCurrent() != null)
      _resinAdmin = Resin.getCurrent().getAdmin();

    return _resinAdmin;
  }

  @Override
  public PersistentStoreMXBean getPersistentStore()
  {
    return null;
  }

  @Override
  public ClusterServerMXBean []getServers()
  {
    ArrayList<ClusterServerMXBean> serverMBeansList
      = new ArrayList<ClusterServerMXBean>();

    for (CloudPod pod : _cluster.getPodList()) {
      for (CloudServer server : pod.getServerList()) {
        if (server != null) {
          ClusterServer clusterServer = server.getData(ClusterServer.class);

          if (clusterServer != null)
            serverMBeansList.add(clusterServer.getAdmin());
        }
      }
    }

    ClusterServerMXBean []serverMBeans
      = new ClusterServerMXBean[serverMBeansList.size()];
    serverMBeansList.toArray(serverMBeans);

    return serverMBeans;
  }

  /**
   * Adds a new dynamic server
   */
  public void addDynamicServer(String id, String address, int port)
  {
    // _cluster.addDynamicServer(id, address, port);
    /*
    Server server = _cluster.getResin().getServer();
    
    server.addDynamicServer(_cluster.getId(), id, address, port);
    */
  }

  public boolean isDynamicServerEnable()
  {
    // return _cluster.isDynamicServerEnable();
    return false;
  }


  //
  // lifecycle
  //

  void register()
  {
    registerSelf();
  }
  
  void unregister()
  {
    unregisterSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
