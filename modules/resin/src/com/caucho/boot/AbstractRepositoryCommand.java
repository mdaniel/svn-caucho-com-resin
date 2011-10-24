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
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.NotAuthorizedException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.actor.ActorSender;
import com.caucho.config.ConfigException;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.hmtp.HmtpClient;
import com.caucho.network.listen.TcpSocketLinkListener;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;

public abstract class AbstractRepositoryCommand extends AbstractBootCommand {
  private static final L10N L = new L10N(AbstractRepositoryCommand.class);
  private static final Logger log
    = Logger.getLogger(AbstractRepositoryCommand.class.getName());

  @Override
  public final int doCommand(WatchdogArgs args,
                             WatchdogClient client)
    throws BootArgumentException
  {
    WebAppDeployClient deployClient = null;

    try {
      deployClient = getDeployClient(args, client);

      return doCommand(args, client, deployClient);
    } catch (Exception e) {
      if (args.isVerbose())
        e.printStackTrace();
      else
        System.out.println(e.toString());

      if (e instanceof NotAuthorizedException)
        return 1;
      else
        return 2;
    } finally {
      if (deployClient != null)
        deployClient.close();
    }
  }

  protected abstract int doCommand(WatchdogArgs args,
                                   WatchdogClient client,
                                   WebAppDeployClient deployClient);

  protected final void fillInVersion(CommitBuilder commit, String version) {
    String []parts = version.split("\\.");
    if (parts.length < 2)
      throw new ConfigException(L.l(
        "erroneous version '{0}'. Version expected in format %d.%d[.%d[.%s]]",
        version));

    int major = Integer.parseInt(parts[0]);
    int minor = Integer.parseInt(parts[1]);
    int micro = 0;

    if (parts.length > 2)
      micro = Integer.parseInt(parts[2]);

    String qualifier = null;

    if (parts.length == 4)
      qualifier = parts[3];

    commit.version(major, minor, micro, qualifier);
  }
  
  protected WebAppDeployClient getDeployClient(WatchdogArgs args,
                                               WatchdogClient client)
  {
    ActorSender sender = createBamClient(args, client);
    
    // return new WebAppDeployClient(address, port, user, password);
    
    return new WebAppDeployClient(sender);
  }
  
  private ActorSender createBamClient(WatchdogArgs args,
                                      WatchdogClient client)
  {
    String address = args.getArg("-address");

    int port = -1;

    String portArg = args.getArg("-port");

    try {
      if (portArg != null && ! portArg.isEmpty())
        port = Integer.parseInt(portArg);
    } catch (NumberFormatException e) {
      NumberFormatException e1 = new NumberFormatException("-port argument is not a number '" + portArg + "'");
      e1.setStackTrace(e.getStackTrace());

      throw e;
    }
    
    String user = args.getArg("-user");
    String password = args.getArg("-password");
    
    if (user == null || "".equals(user)) {
      user = "";
      password = client.getResinSystemAuthKey();
    }
    
    return createBamClient(client, address, port, user, password);
  }
  
  private ActorSender createBamClient(WatchdogClient client,
                                      String address,
                                      int port,
                                      String userName,
                                      String password)
  {
    WatchdogClient liveClient = client;
    
    ActorSender hmuxClient
      = createHmuxClient(client, address, port, userName, password);
    
    if (hmuxClient != null)
      return hmuxClient;

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
  
  private ActorSender createHmtpClient(String address, int port,
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
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote deploy. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {1}",
                                                    url, e.getMessage()),
                                                e);
    }
  }
  
  
  private ActorSender createHmuxClient(WatchdogClient client,
                                       String address, int port,
                                       String userName,
                                       String password)
  {
    WatchdogClient triad = findLiveTriad(client);

    if (triad == null)
      return null;
    
    address = triad.getConfig().getAddress();
    port = triad.getConfig().getPort();

    HmuxClientFactory hmuxFactory
      = new HmuxClientFactory(address, port, userName, password);
                                                          
    try {
      return hmuxFactory.create();
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote deploy. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {1}",
                                                    triad, e.getMessage()),
                                                e);
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
    for (TcpSocketLinkListener listener : client.getConfig().getPorts()) {
      if (listener instanceof OpenPort) {
        OpenPort openPort = (OpenPort) listener;
        
        if ("http".equals(openPort.getProtocolName()))
          return openPort.getPort();
      }
    }
    
    return 0;
  }
}
