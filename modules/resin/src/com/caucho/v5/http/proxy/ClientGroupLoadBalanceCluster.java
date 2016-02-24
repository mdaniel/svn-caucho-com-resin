/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.http.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.network.ServerNetwork;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.network.balance.ClientSocketFactory;
import com.caucho.v5.util.L10N;

/**
 * Manages a load balancer.
 */
class ClientGroupLoadBalanceCluster 
  extends ClientGroupLoadBalanceBase
  //implements ServerBartenderTopologyEvents
{
  private static final L10N L = new L10N(ClientGroupLoadBalanceCluster.class);
  private static final Logger log
    = Logger.getLogger(ClientGroupLoadBalanceCluster.class.getName());
  
  private final PodBartender _pod;
  private final int _port;
  private ClientSocketFactory []_clientList;

  ClientGroupLoadBalanceCluster(PodBartender pod)
  {
    this(pod, 0);
  }
  
  ClientGroupLoadBalanceCluster(PodBartender pod, int port)
  {
    _pod = pod;
    _port = port;
    
    if (pod == null)
      throw new NullPointerException();
    
    // EventService eventService = AmpSystem.getCurrent().getEventService();
    //eventService.subscribe(ServerBartenderTopologyEvents.class, this);
    // pod.addServerListener(this);
    
    update();
  }
  
  /**
   * Returns the current socket factory list.
   */
  @Override
  public ClientSocketFactory []getClientList()
  {
    return _clientList;
  }

  //@Override
  public void onServerAdd(ServerBartender server)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(L.l("adding cloud server `{0}': `{1}'", server, server.getState()));

    update();
  }

  //@Override
  public void onServerRemove(ServerBartender server)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(L.l("removing cloud server `{0}'", server));

    update();
  }

  //@Override
  public void onServerStateChange(ServerBartender server)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(L.l("updating cloud server state `{0}': `{1}'",
                    server,
                    server.getState()));

    update();
  }

  //@Override
  public void onTriadAdd(ServerBartender server)
  {
  }

  //@Override
  public void onTriadRemove(ServerBartender server)
  {
  }
  
  private void update()
  {
    int length = _pod.nodeCount();
    
    ClientSocketFactory []clientList = new ClientSocketFactory[length];
    
    for (int i = 0; i < length; i++) {
      ServerBartender server = _pod.getNode(i).server(0);

      if (server == null) {
        continue;
      }

      if (server.getState().isDisabled()) {
        continue;
      }
      
      int port;
      
      if (_port > 0) {
        port = _port;
      }
      else {
        port = server.port();
      }
      
      if (port > 0) {
        // XXX: port issue when _port is defined
        ClientSocketFactory factory
          = new ClientSocketFactory(server);
        
        factory.init();
        
        clientList[i] = factory;
        continue;
      }

      ServerNetwork serverNetwork = null; // NetworkSystem.getCurrent().getServer(server);

      if (serverNetwork != null)
        clientList[i] = serverNetwork.getLoadBalanceSocketPool();
    }
    
    _clientList = clientList;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _pod + "]";
  }
}
