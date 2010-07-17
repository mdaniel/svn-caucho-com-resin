/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.env.service.AbstractResinService;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.network.listen.SocketLinkListener;

public class ClusterNetworkService extends AbstractResinService
{
  private static final Logger log
    = Logger.getLogger(ClusterNetworkService.class.getName());
  
  public static final int START_PRIORITY_CLUSTER_NETWORK = 2000;
  public static final int START_PRIORITY_CLUSTER_SERVICE = 2100;
  
  private final SocketLinkListener _clusterPort;

  /**
   * Creates a new servlet server.
   */
  public ClusterNetworkService(SocketLinkListener clusterPort)
  {
    if (clusterPort == null)
      throw new NullPointerException();
    
    _clusterPort = clusterPort;
  }

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_CLUSTER_NETWORK;
  }

  @Override
  public void start()
    throws Exception
  {
    // server/2l32
    // _authManager.setAuthenticator(getAdminAuthenticator());

    startClusterPort();

    TopologyService topology = TopologyService.getCurrent();
    
    if (topology != null) {
      for (CloudCluster cluster : topology.getSystem().getClusterList()) {
        for (CloudPod pod : cluster.getPodList()) {
          for (CloudServer cloudServer : pod.getServerList()) {
            ClusterServer server = cloudServer.getData(ClusterServer.class);
            
            if (server != null) {
              ClientSocketFactory pool = server.getServerPool();

              if (pool != null)
                pool.start();
            }
          }
        }
      }
    }
  }

  /**
   * Start the cluster port
   */
  private void startClusterPort()
    throws Exception
  {
    SocketLinkListener port = _clusterPort;

    if (port != null && port.getPort() != 0) {
      log.info("");
      port.bind();
      port.start();
      log.info("");
    }
  }

  /**
   * Closes the server.
   */
  @Override
  public void stop()
    throws Exception
  {
    try {
      if (_clusterPort != null)
        _clusterPort.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _clusterPort + "]");
  }
}
