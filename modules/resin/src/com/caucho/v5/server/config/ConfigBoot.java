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
 * @author Scott Ferguson
 */

package com.caucho.v5.server.config;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.server.container.ArgsServerBase;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;
import com.caucho.v5.vfs.PathImpl;

/**
 * Facade for combining the CLI arguments with the parsed configuration.
 */
public class ConfigBoot
{
  private static final L10N L = new L10N(ConfigBoot.class);
  private static final Logger log = Logger.getLogger(ConfigBoot.class.getName());

  private final RootConfigBoot _rootConfig;

  public ConfigBoot(RootConfigBoot rootConfig)
  {
    _rootConfig = rootConfig;
  }

  public RootConfigBoot getRoot()
  {
    return _rootConfig;
  }

  public PathImpl getRootDirectory(ArgsDaemon args)
  {
    if (args.getRootDirectory() != null) {
      return args.getRootDirectory();
    }
    else if (getRoot().getRootDirectory() != null) {
      return getRoot().getRootDirectory();
    }
    else {
      return args.getDefaultRootDirectory();
    }
  }

  public PathImpl getDataDirectory(ArgsDaemon args)
  {
    if (args.getDataDirectory() != null) {
      return args.getDataDirectory();
    }
    else if (getRoot().getDataDirectory() != null) {
      return getRoot().getDataDirectory();
    }
    else {
      return getRootDirectory(args);
    }
  }

  public PathImpl getLogDirectory(ArgsDaemon args)
  {
    if (args.getLogDirectory() != null) {
      return args.getLogDirectory();
    }
    /*
    else if (getConfig().getLogDirectory() != null) {
      return getConfig().getLogDirectory();
    }
    */
    else {
      return getRootDirectory(args).lookup("log");
    }
  }

  /**
   * The configured server, as specified by the CLI or in the config file.
   */
  public String getServerId(ArgsDaemon args)
  {
    String serverId = args.getServerId();

    if (serverId != null) {
      return serverId;
    }
    else {
      return getRoot().getHomeServer();
    }
  }

  /**
   * The cluster id, used when joining a cluster as a dynamic server.
   */
  public String getHomeCluster(ArgsDaemon args)
  {
    return getHomeCluster(args, getHomeClusterDefault());
  }

  /**
   * The cluster id, used when joining a cluster as a dynamic server.
   */
  public String getHomeCluster(ArgsDaemon args, String defaultCluster)
  {
    String clusterId = args.getClusterId();

    if (clusterId != null) {
      return clusterId;
    }
    
    clusterId = getRoot().getHomeCluster();
    
    if (clusterId != null) {
      return clusterId;
    }
    else {
      return defaultCluster;
    }
  }

  /**
   * The cluster id, used when joining a cluster as a dynamic server.
   * 
   * Use a cluster if it's unique, either by being the only defined cluster
   * or the only cluster with servers.
   * 
   * Otherwise return the default cluster name.
   * 
   * XXX: probably should be more error checking in case of multiple
   * clusters, and no default cluster.
   */
  public String getHomeClusterDefault()
  {
    if (getRoot().getClusters().size() == 1) {
      return getRoot().getClusters().get(0).getId();
    }

    boolean isServerCluster = false;
    ClusterConfigBoot clusterConfig = null;
    
    for (ClusterConfigBoot cluster : getRoot().getClusters()) {
      if (cluster.getServerList().size() == 0) {
        continue;
      }
      
      if (isServerCluster) {
        return ClusterConfigBoot.DEFAULT_NAME;
      }
      
      isServerCluster = true;
      clusterConfig = cluster;
    }
    
    if (clusterConfig != null) {
      return clusterConfig.getId();
    }

    return ClusterConfigBoot.DEFAULT_NAME;
  }

  /**
   * Returns the configured cluster specified by the cluster id.
   */
  public ClusterConfigBoot findCluster(String clusterId)
  {
    return _rootConfig.findCluster(clusterId);
  }

  /**
   * Find the server selected by the CLI args and the config file.
   */
  public ServerConfigBoot findServer(ArgsDaemon args)
  {
    String serverId = getServerId(args);

    ServerConfigBoot server = findServer(serverId);

    if (server != null) {
      return server;
    }

    server = findLocalServer(args);

    if (server == null) {
      server = findWatchdogServer(args);
    }

    /* baratine/b084
    if (server == null) {
      throw new ConfigException(L.l("No <server> can be found listening to a local IP address"));
    }
    */

    return server;
  }

  public ServerConfigBoot findLocalServer(ArgsDaemon args)
  {
    ArrayList<ServerConfigBoot> servers = findLocalServers(args);

    if (servers == null) {
      return null;
    }
    
    int serverPort = args.getServerPort();

    for (ServerConfigBoot server : servers) {
      if (serverPort > 0 && server.getPort() != serverPort) {
        continue;
      }
      
      if (! server.isRequireExplicitId()) {
        return server;
      }
    }

    return null;
  }
  
  /**
   * Returns the servers that the arguments and configuration select.
   * 
   * <ol>
   * <li>Matching --server ids are selected</li>
   * <li>Matching local IP addresses are selected</li>
   * <li>Servers with "*" addresses are selected</li>
   * <li>Explicit --port argument will create a new dynamic server.</li>
   * </ol>
   * 
   * If the --port or --server argument is specified, the list is restricted
   * to match.
   * 
   * Since the port of the server is the primary key for the watchdog, only
   * one server for the given port is allowed per machine.
   */
  public ArrayList<ServerConfigBoot> findStartServers(ArgsDaemon args)
  {
    return findServers(args, findLocalServers(args));
  }
  
  /**
   * Servers used for remote commands like deploy. 
   */
  public ArrayList<ServerConfigBoot> findRemoteServers(ArgsDaemon args)
  {
    return findServers(args, _rootConfig.findServers());
  }
  
  private ArrayList<ServerConfigBoot> 
  findServers(ArgsDaemon args,
              Iterable<ServerConfigBoot> serverList)
  {
    ArrayList<ServerConfigBoot> startServers = new ArrayList<>();
    
    String clusterId = getHomeCluster(args, null);
    String serverId = getServerId(args);
    int port = args.getServerPort();
    
    for (ServerConfigBoot server : serverList) {
      if (serverId != null) {
        if (serverId.equals(server.getId())) {
          startServers.add(server);
        }
      }
      else if (port > 0) {
        if (server.getPort() == port) {
          startServers.add(server);
        }
      }
      else if (clusterId != null
               && ! server.getCluster().getId().equals(clusterId)) {
      }
      else if (! server.isRequireExplicitId()) {
        startServers.add(server);
      }
    }

    if (startServers.size() > 0) {
      return startServers;
    }
    
    if (serverId != null) {
      ServerConfigBoot server = findServer(serverId);
      
      if (server != null) {
        startServers.add(server);
        
        return startServers;
      }
    }
    
    if (port > 0) {
      ServerConfigBoot dynServer = createDynamicServer(args);
    
      if (dynServer != null) {
        startServers.add(dynServer);
      }
    }
    
    return startServers;
  }

  public ServerConfigBoot findUniqueLocalServer(ArgsDaemon args)
  {
    String id = args.getServerId();
    
    if (id != null) {
      ServerConfigBoot server = _rootConfig.findServer(id);
      
      if (server != null) {
        return server;
      }
      
      server = _rootConfig.findServerByName(id);
      
      if (server != null) {
        return server;
      }
    }
    
    ArrayList<ServerConfigBoot> servers = findLocalServers(args);

    if (servers == null) {
      return null;
    }

    ServerConfigBoot foundClient = null;

    for (ServerConfigBoot server : servers) {
      if (server.isRequireExplicitId()) {
        continue;
      }

      if (foundClient == null) {
        foundClient = server;
      }
      else {
        throw new ConfigException(L.l("{0}/{1}: server '{2}' does not match a unique <server> or <server-multi>\nwith a unique local IP in {3}.\n  server ids: {4}",
                                      args.getCommandName(),
                                      Version.getVersion(),
                                      args.getServerId(),
                                      args.getConfigPath(), // .getNativePath(),
                                      findLocalServerIds(args)));
      }
    }

    return foundClient;
  }

  /**
   * Returns the configured server specified by the server id.
   */
  public ServerConfigBoot findServer(String serverId)
  {
    return _rootConfig.findServer(serverId);
  }

  public ServerConfigBoot findServerByName(String serverName)
  {
    return _rootConfig.findServerByName(serverName);
  }

  public ServerConfigBoot findWatchdogServer(ArgsDaemon args)
  {
    String clusterId = getHomeCluster(args, null);

    if (clusterId != null) {
      return _rootConfig.findWatchdogServer(clusterId);
    }
    else {
      return _rootConfig.findWatchdogServer();
    }
  }

  /**
   * Returns the IDs of all servers local to the current machine.
   */
  public ArrayList<String> findLocalServerIds(ArgsDaemon args)
  {
    return _rootConfig.findLocalServerIds(args);
  }

  /**
   * Returns server configuration for all servers local to the current machine.
   */
  public ArrayList<ServerConfigBoot> findLocalServers(ArgsDaemon args)
  {
    return _rootConfig.findLocalServers(args);
  }

  /**
   * Returns server configuration for all servers local to the current machine.
   */
  /*
  public ArrayList<ServerConfigBoot> findLocalServers()
  {
    return _rootConfig.findLocalServers();
  }
  */

  /**
   * Finds the best server to use to contact the watchdog.
   */
  public ServerConfigBoot findWatchdogServer(String clusterId)
  {
    return _rootConfig.findWatchdogServer(clusterId);
  }

  /**
   * Creates a dynamic server. Returns null if none can be created.
   */
  public ServerConfigBoot createDynamicServer(ArgsDaemon args)
  {
    String clusterId = getHomeCluster(args);
    
    ClusterConfigBoot cluster = _rootConfig.findCluster(clusterId);
    
    if (cluster == null) {
      return null;
    }
    
    int portArg = args.getServerPort();
    
    ServerConfigBoot server = cluster.createDynamicServer(portArg);

    int port = server.getPort();
    
    if (port <= 0) {
      server.setDynamic(true);
      
      port = args.getServerPort();
      server.setPort(port);
    }
    
    if (port <= 0) {
      server.setEphemeral(true);
      
      server.setId(cluster.getId() + "-dyn-" + port);
      
      server.setPort(port);
      
      return server;
    }
    else {
      return server;
    }
  }

  /**
   * Creates a dynamic server. Returns null if none can be created.
   */
  public ServerConfigBoot createEmbeddedServer(ArgsDaemon args)
  {
    String clusterId = getHomeCluster(args);
    
    ClusterConfigBoot cluster = _rootConfig.findCluster(clusterId);
    
    if (cluster == null) {
      throw new ConfigException(L.l("{0} is an unknown cluster", clusterId));
    }
    
    int port = args.getServerPort();
    
    ServerConfigBoot server = cluster.createDynamicServer(port);
    
    port = server.getPort();
    
    if (port <= 0) {
      port = args.getServerPort();
    }
    
    if (port > 0) {
      server.setId(cluster.getId() + "-dyn-" + port);
      
      server.setPort(port);
      server.setDynamic(true);
      
      return server;
    }
    else {
      server.setId(cluster.getId() + "-embedded");
      
      // server.setDynamic(true);
      
      return server;
    }
  }
}
