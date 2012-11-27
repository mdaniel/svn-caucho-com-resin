/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.*;
import com.caucho.bam.actor.RemoteActorSender;
import com.caucho.config.ConfigException;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.util.L10N;

public abstract class AbstractRemoteCommand extends AbstractBootCommand {
  private static final L10N L = new L10N(AbstractRemoteCommand.class);
  private static final Logger log
    = Logger.getLogger(AbstractRemoteCommand.class.getName());
  
  @Override
  protected void initBootOptions()
  {
    addSubsectionHeaderOption("remote connection options:");

    addValueOption("server", "id", "id of a server in the config file");
    addValueOption("address", "ip", "IP address or host name of (triad) server");
    addIntValueOption("port", "port", "IP port of (triad) server");
    
    addSpacerOption();
    
    addValueOption("user", "user", "admin user name for authentication");
    addValueOption("password", "password", "admin password for authentication");
    
    super.initBootOptions();
  }
  
  protected RemoteActorSender createBamClient(WatchdogArgs args,
                                              WatchdogClient client)
  {
    String address = args.getArg("-address");

    int port = -1;
    
    if (address != null) {
      int p = address.lastIndexOf(':');

      if (p >= 0) {
        port = Integer.parseInt(address.substring(p + 1));
        address = address.substring(0, p);
      }
    }
    
    port = args.getArgInt("-port", port);
    
    String user = args.getArg("-user");
    String password = args.getArg("-password");
    
    if (user == null || "".equals(user)) {
      user = "";
      password = client.getClusterSystemKey();
    }
    
    return createBamClient(client, address, port, user, password);
  }
  
  private RemoteActorSender createBamClient(WatchdogClient client,
                                            String address,
                                            int port,
                                            String userName,
                                            String password)
  {
    WatchdogClient liveClient = client;
    
    RemoteActorSender hmuxClient
      = createHmuxClient(client, address, port, userName, password);
    
    if (hmuxClient != null) {
      return hmuxClient;
    }

    if (address == null || address.isEmpty()) {
      liveClient = findLiveClient(client, port);
      
      address = liveClient.getConfig().getAddress();
    }

    if (port <= 0)
      port = findPort(liveClient);

    if (port <= 0) {
      throw new ConfigException(L.l("Cannot find live Resin server for deployment at {0}:{1} was not found",
                                    address, port));
    }
    
    return createHmtpClient(address, port, userName, password);
  }
  
  private RemoteActorSender createHmtpClient(String address,
                                       int port,
                                       String userName,
                                       String password)
  {
    String url = "http://" + address + ":" + port + "/hmtp";
    
    HmtpClient client = new HmtpClient(url);
    try {
      client.setVirtualHost("admin.resin");

      client.connect(userName, password);

      return client;
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote administration.\n  Ensure the local server has started, or include --server and --port parameters to connect to a remote server.\n  {1}",
                                                    url, e.getMessage()), e);
    } catch (RemoteListenerUnavailableException e) {
      throw new RemoteListenerUnavailableException(L.l("Connection to '{0}' failed for remote administration because RemoteAdminService (HMTP) is not enabled.\n  Ensure 'remote_admin_enable' is set true in resin.properties.\n  {1}",
                                                       url, e.getMessage()), e);
    }
  }
  
  
  private RemoteActorSender createHmuxClient(WatchdogClient client,
                                             String address, int port,
                                             String userName,
                                             String password)
  {
    WatchdogClient server;
    
    if (address != null && ! "".equals(address) && port > 0) {
      server = findServer(client, address, port);
    }
    else if (clientCanConnect(client)) {
      server = client;
    }
    else {
      server = findLiveTriad(client);
    }
    
    if (server == null) {
      return null;
    }
    
    address = server.getConfig().getAddress();
    port = server.getConfig().getPort();

    HmuxClientFactory hmuxFactory
      = new HmuxClientFactory(address, port, userName, password);

    try {
      return hmuxFactory.create();
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote administration.\n  Ensure the local server has started, or include --server and --port parameters to connect to a remote server.\n  {1}",
                                                    server, e.getMessage()), e);
    } catch (RemoteListenerUnavailableException e) {
      throw new RemoteListenerUnavailableException(L.l("Connection to '{0}' failed for remote administration because RemoteAdminService (HMTP) is not enabled.\n  Ensure 'remote_admin_enable' is set true in resin.properties.\n  {1}",
                                                       server, e.getMessage()), e);
    }
  }
  
  private WatchdogClient findLiveTriad(WatchdogClient client)
  {
    for (WatchdogClient triad : client.getConfig().getCluster().getClients()) {
      int port = triad.getConfig().getPort();

      if (clientCanConnect(triad, port)) {
        return triad;
      }
      
      if (triad.getIndex() > 2)
        break;
    }
    
    return null;
  }
  
  private WatchdogClient findServer(WatchdogClient client,
                                    String address,
                                    int port)
  {
    for (WatchdogClient server : client.getConfig().getCluster().getClients()) {
      if (! isEqual(address, server.getConfig().getAddress()))
        continue;
      
      if (port != server.getConfig().getPort())
        continue;
      
      return server;
    }
    
    return null;
  }

  private boolean isEqual(String address1, String address2)
  {
    if (address1.equals(address2))
      return true;

    try {
      InetAddress inetAddress1 = InetAddress.getByName(address1);
      InetAddress inetAddress2 = InetAddress.getByName(address2);

      return inetAddress1.equals(inetAddress2);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private WatchdogClient findLiveClient(WatchdogClient client, int port)
  {
    for (WatchdogClient triad : client.getConfig().getCluster().getClients()) {
      int triadPort = port;
      
      if (triadPort <= 0)
        triadPort = findPort(triad);
      
      if (clientCanConnect(triad, triadPort)) {
        return triad;
      }
      
      if (triad.getIndex() > 2)
        break;
    }
    
    return client;
  }
  
  private boolean clientCanConnect(WatchdogClient client, int port)
  {
    return clientCanConnect(client);
  }
  
  private boolean clientCanConnect(WatchdogClient client)
  {
    String address = client.getConfig().getAddress();
    int clusterPort = client.getConfig().getPort();
    
    try {
      Socket s = new Socket(address, clusterPort);
      
      s.close();
      
      return true;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    }
  }
  
  private int findPort(WatchdogClient client)
  {
    for (OpenPort openPort : client.getConfig().getPorts()) {
      if ("http".equals(openPort.getProtocolName()))
        return openPort.getPort();
    }
    
    return 0;
  }
}
