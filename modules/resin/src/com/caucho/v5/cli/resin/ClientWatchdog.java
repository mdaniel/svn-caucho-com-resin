/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.v5.cli.resin;

import io.baratine.core.ServiceExceptionConnect;

import java.util.Objects;

import com.caucho.v5.baratine.client.ServiceManagerClient;
import com.caucho.v5.bartender.hamp.ClientBartenderFactory;
import com.caucho.v5.bartender.link.ClientBartenderHamp;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.daemon.DaemonCommandBase;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerAddress;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerId;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerPort;
import com.caucho.v5.cli.daemon.ArgsDaemon.WatchdogPort;
import com.caucho.v5.cli.spi.OptionCommandLine.ArgsType;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.watchdog.WatchdogService;
import com.caucho.v5.util.L10N;

/**
 * Watchdog-based commands such as start and stop contact the watchdog
 * through champ, and launch if necessary. 
 */
public class ClientWatchdog implements AutoCloseable
{
  public static final L10N L = new L10N(ClientWatchdog.class);
  
  private ServiceManagerClient _client;

  private WatchdogService _watchdog;

  private ServerConfigBoot _server;

  public ClientWatchdog(ArgsDaemon args, ConfigBoot boot)
  {
    this(args, findWatchdogServer(args, boot));
  }    
    
  public ClientWatchdog(ArgsDaemon args, ServerConfigBoot server)
  {
    Objects.requireNonNull(server);
    
    _server = server;
    
    _client = createWatchdogClient(args, server);
  }
  
  private static ServerConfigBoot findWatchdogServer(ArgsDaemon args,
                                                     ConfigBoot boot)
  {
    ServerConfigBoot server = boot.findWatchdogServer(args);

    if (server == null) {
      String clusterId = boot.getHomeCluster(args);
  
      throw new ConfigException(L.l("Can't find a watchdog server matching a local IP address in cluster '{0}'",
                                    clusterId));
    }
    
    return server;
  }
  
  public ServerConfigBoot getServer()
  {
    return _server;
  }

  public WatchdogService getWatchdogService()
  {
    if (_watchdog == null) {
      _watchdog = _client.lookup("remote:///watchdog")
          .as(WatchdogService.class);
    }
    
    return _watchdog;
  }

  private final ServiceManagerClient createWatchdogClient(ArgsDaemon args,
                                               ServerConfigBoot server)
  {
    String address = server.getWatchdogAddress(args);
    int port = server.getWatchdogPort(args);
    
    String user = args.getArg("user");
    String password = args.getArg("password");
    
    if (user == null || "".equals(user)) {
      user = "";
      password = server.getClusterSystemKey();
    }
    
    String url = "bartender://" + address + ":" + port;
    
    //return createChampClient(url, user, password);
    
    return new ClientBartenderHamp(url, user, password);
  }
  
  private ServiceManagerClient createChampClient(String url,
                                       String userName,
                                       String password)
  {
    ClientBartenderFactory champFactory
      = new ClientBartenderFactory(url, userName, password);

    try {
      return champFactory.create();
    } catch (ServiceExceptionConnect e) {
      throw new ServiceExceptionConnect(L.l("Connection to watchdog '{0}' failed for remote administration.\n  Ensure the local server has started, or include --server and --port parameters to connect to a remote server.\n  {1}",
                                            url, e.getMessage()), e);
    }
  }
 
  protected String getServerUsageArg(String serverId, String clientId)
  {
    if (serverId != null)
      return " -server '" + serverId + "'"; 
    else
      return "";
  }
  
  @Override
  public void close()
  {
    ServiceManagerClient client = _client;
    _client = null;
    
    if (client != null) {
      client.close();
    }
  }

  public static void addBootOptions(DaemonCommandBase<?> command)
  {
    command.addOption(new ServerId()).tiny("s");
    command.addOption(new ServerAddress()).alias("address").tiny("sa").hide();
    command.addOption(new ServerPort()).alias("server-port").tiny("p").tiny("sp");

    command.addValueOption("watchdog-address", "address", "watchdog address")
           .tiny("wa").hide();

    command.addOption(new WatchdogPort()).tiny("wp").hide();

    command.addValueOption("user", "value", "admin user name for authentication")
           .type(ArgsType.ADMIN);

    command.addValueOption("password", "value", "admin password authentication")
           .type(ArgsType.ADMIN);
  }
}
