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
 * @author Scott Ferguson
 */

package com.caucho.cloud.hmtp;

import com.caucho.bam.Message;
import com.caucho.bam.broker.Broker;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.hemp.servlet.ClientStubManager;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.hemp.servlet.ServerLinkActor;

/**
 * Underlying stream handling HTTP requests.
 */
class HmtpLinkActor extends ServerLinkActor {
  private Object _linkClosePayload;
  private NetworkClusterSystem _clusterService;
  
  public HmtpLinkActor(Broker toLinkBroker,
                       ClientStubManager clientManager,
                       ServerAuthManager authManager,
                       String ipAddress)
  {
    super(toLinkBroker, clientManager, authManager, ipAddress);
  }
  
  void onCloseConnection()
  {
    super.onClose();

    NetworkClusterSystem clusterService = _clusterService;
    _clusterService = null;
    
    Object linkClosePayload = _linkClosePayload;
    _linkClosePayload = null;
    
    if (linkClosePayload != null) {
      clusterService.notifyLinkClose(linkClosePayload);
    }  
  }
  
  /**
   * On login, wake the associated ServerPool so we can send a proper response.
   */
  @Override
  protected void notifyValidLogin(String address)
  {
    if (address == null || ! address.endsWith("admin.resin"))
      return;
    
    int p = address.indexOf('@');
    
    if (p > 0)
      address = address.substring(p + 1);
    
    ClusterServer clusterServer = findServerByAddress(address);
    
    if (clusterServer != null)
      clusterServer.getClusterSocketPool().wake();
  }

  private ClusterServer findServerByAddress(String address)
  {
    /*
    for (Cluster cluster : _server.getClusterList()) {
      for (ClusterPod pod : cluster.getPodList()) {
        for (ClusterServer clusterServer : pod.getServerList()) {
          if (clusterServer.getBamAdminName().equals(address))
            return clusterServer;
        }
      }
    }
    */
    
    return null;
  }
  
  //
  // message handling
  //
  
  @Message
  @SuppressWarnings("unused")
  private void onLinkRegister(String to, 
                              String from,
                              HmtpLinkRegisterMessage registerMessage)
  {
    NetworkClusterSystem clusterService = NetworkClusterSystem.getCurrent();
    
    if (clusterService == null)
      throw new IllegalStateException(getClass().getSimpleName());
    
    _clusterService = clusterService;
    _linkClosePayload = registerMessage.getPayload();
  }
}
