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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import java.util.ArrayList;

import com.caucho.boot.BootResinConfig.ElasticServer;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;


public abstract class AbstractStartCommand extends AbstractBootCommand
{
  private static final L10N L = new L10N(AbstractStartCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("server", "id", "select a configured server");

    addSpacerOption();

    addFlagOption("elastic-server", 
                  "join a cluster as an elastic server (pro)");
    addValueOption("elastic-server-address", "ip", 
                   "cluster IP address for an elastic server");
    addIntValueOption("elastic-server-port", "port",
                      "cluster port for an elastic server");
    addValueOption("cluster", 
                   "id", "cluster to join as an elastic server (pro)");
    addValueOption("join-cluster", 
                   "id", "cluster to join as an elastic server (pro)", true);
    
    addSpacerOption();

    addFlagOption("preview", "run as a preview (staging) server");
    addValueOption("stage", 
                   "stage", 
                   "select a configuration stage (production, preview)");

    addSpacerOption();

    addIntValueOption("debug-port", "port", "listen to a JVM debug port");
    addIntValueOption("jmx-port", 
                      "port", "listen to an unauthenticated JMX port");
    addIntValueOption("watchdog-port", 
                      "port", "set watchdog port to listen to");
    
    super.initBootOptions();
  }

  @Override
  public int doCommand(ResinBoot boot, WatchdogArgs args)
  {
    if (boot.isElasticServer(args)) {
      validateElasticServer(boot, args);
    }
    return super.doCommand(boot, args);
  }
  
  private void validateElasticServer(ResinBoot boot, WatchdogArgs args)
  {
    for (ElasticServer server : boot.getElasticServerList()) {
      String clusterId = server.getCluster();
      
      if (clusterId == null) {
        clusterId = boot.getHomeCluster(args);
      }
    
      BootClusterConfig cluster = boot.findCluster(clusterId);

      if (cluster != null) {
      }
      else if (clusterId == null) {
        throw new ConfigException(L.l("--elastic-server requires a --cluster or <home-cluster> configuration."
            + " --elastic-server needs to know which cluster to join."));
      }
      else {
        throw new ConfigException(L.l("--cluster '{0}' is an unknown cluster."
                                    + "  --elastic-server requires a configured --cluster.",
                                    clusterId));
      }
    
      ArrayList<WatchdogClient> serverList = cluster.getServerList();
    
      for (int i = 0; i < serverList.size() && i < 3; i++) {
        WatchdogClient triad = serverList.get(i);
      }
    }
  }

  @Override
  public void doWatchdogStart(WatchdogManager manager)
  {
    WatchdogArgs args = manager.getArgs();
    
    String serverId = args.getClientServerId();
    
    manager.startServer(serverId, args.getArgv());
  }
  
  @Override
  protected WatchdogClient findNamedClient(ResinBoot boot, 
                                           WatchdogArgs args,
                                           String serverId)
  {
    WatchdogClient client = findNamedClientImpl(boot, args, serverId);

    /*
    if (boot.isElasticServer(args)) {
      Thread.dumpStack();
      throw new ConfigException(L.l("-server '{0}' is a static server, but --elastic-server is requested.",
                                    serverId));
    }
    else {
      return client;
    }
    */
    
    return client;
  }

  @Override
  protected WatchdogClient findLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    if (boot.isElasticServer(args))
      return findLocalClientImpl(boot, args);
    else
      return findUniqueLocalClient(boot, args);
  }

  @Override
  protected WatchdogClient findWatchdogClient(ResinBoot boot, WatchdogArgs args)
  {
    // server/6e09
    if (boot.isElasticServer(args)) {
      return findWatchdogClientImpl(boot, args);
    }
    else {
      return null;
    }
  }

  protected String getServerUsageArg(WatchdogArgs args, String clientId)
  {
    if (args.getServerId() != null)
      return " -server '" + args.getServerId() + "'";
    else if (args.isElasticServer())
      return " -server '" + args.getElasticServerId() + "'";
    else
      return " -server '" + clientId + "'";
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }
}
