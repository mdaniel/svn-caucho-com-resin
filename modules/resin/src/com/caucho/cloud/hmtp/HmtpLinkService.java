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

package com.caucho.cloud.hmtp;

import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.hemp.servlet.ServerLinkService;
import com.caucho.server.cluster.ClusterServer;

/**
 * Underlying stream handling HTTP requests.
 */
class HmtpLinkService extends ServerLinkService {
  public HmtpLinkService(ActorStream linkStream,
                         Broker broker,
                         ServerAuthManager authManager,
                         String ipAddress,
                         boolean isUnidir)
  {
    super(linkStream, broker, authManager, ipAddress, isUnidir);
  }
  
  /**
   * On login, wake the associated ServerPool so we can send a proper response.
   */
  @Override
  protected void notifyValidLogin(String jid)
  {
    if (jid == null || ! jid.endsWith("admin.resin"))
      return;
    
    int p = jid.indexOf('@');
    
    if (p > 0)
      jid = jid.substring(p + 1);
    
    ClusterServer clusterServer = findServerByJid(jid);
    
    if (clusterServer != null)
      clusterServer.getServerPool().wake();
  }

  private ClusterServer findServerByJid(String jid)
  {
    /*
    for (Cluster cluster : _server.getClusterList()) {
      for (ClusterPod pod : cluster.getPodList()) {
        for (ClusterServer clusterServer : pod.getServerList()) {
          if (clusterServer.getBamAdminName().equals(jid))
            return clusterServer;
        }
      }
    }
    */
    
    return null;
  }
}
