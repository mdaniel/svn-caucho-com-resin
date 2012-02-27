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

package com.caucho.boot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.cloud.security.SecurityService;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.ResinSystem;
import com.caucho.security.AdminAuthenticator;
import com.caucho.util.HostUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

public class BootResinConfig // implements EnvironmentBean
{
  private static final L10N L = new L10N(BootResinConfig.class);
  private static final Logger log
    = Logger.getLogger(BootResinConfig.class.getName());

  private boolean _isWatchdogManagerConfig;
  
  private ArrayList<ContainerProgram> _clusterDefaultList
    = new ArrayList<ContainerProgram>();

  private ArrayList<BootClusterConfig> _clusterList
    = new ArrayList<BootClusterConfig>();
  
  private HashMap<String,WatchdogClient> _watchdogMap
    = new HashMap<String,WatchdogClient>();
  
  private HashMap<String,WatchdogConfig> _serverMap
    = new HashMap<String,WatchdogConfig>();

  private ClassLoader _classLoader;

  private ResinSystem _system;
  private WatchdogArgs _args;
  
  private Path _resinHome;
  private Path _rootDirectory;
  private Path _resinDataDirectory;
  
  private BootManagementConfig _management;
  private String _resinSystemKey;
  private String _homeCluster;
  
  BootResinConfig(ResinSystem system,
                  WatchdogArgs args)
  {
    _system = system;
    _args = args;
  
    _classLoader = _system.getClassLoader();
  }
  
  WatchdogArgs getArgs()
  {
    return _args;
  }
  
  public Path getResinHome()
  {
    if (_resinHome != null)
      return _resinHome;
    else
      return _args.getResinHome();
  }
  
  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public Path getRootDirectory()
  {
    if (_rootDirectory != null)
      return _rootDirectory;
    else
      return _args.getRootDirectory();
  }
  
  public Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null)
      return logDirectory;
    else
      return getRootDirectory().lookup("log");
  }
  
  public void setResinDataDirectory(Path path)
  {
    _resinDataDirectory = path;
  }
  
  public Path getResinDataDirectory()
  {
    if (_resinDataDirectory != null)
      return _resinDataDirectory;
    else
      return getRootDirectory().lookup("resin-data");
  }
  
  public void setResinSystemAuthKey(String digest)
  {
    if (digest == null || "".equals(digest))
      return;
    
    _resinSystemKey = digest;
    
    SecurityService security = SecurityService.getCurrent();
    
    if (security != null)
      security.setSignatureSecret(digest);
  }
  
  public String getResinSystemAuthKey()
  {
    return _resinSystemKey;
  }
  
  @Configurable
  public void setJoinCluster(String joinCluster)
  {
    setHomeCluster(joinCluster);
  }
  
  @Configurable
  public void setHomeCluster(String homeCluster)
  {
    _homeCluster = homeCluster;
  }
  
  public String getHomeCluster()
  {
    return _homeCluster;
  }
  
  public boolean isHomeCluster()
  {
    return _homeCluster != null && ! "".equals(_homeCluster);
  }

  /*
  @Override
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  */

  public void add(AdminAuthenticator auth)
  {
    createManagement().setAdminAuthenticator(auth);
  }

  public BootManagementConfig createManagement()
  {
    if (_management == null)
      _management = new BootManagementConfig();

    return _management;
  }

  /**
   * Adds the management configuration
   */
  public void setManagement(BootManagementConfig management)
  {
    _management = management;
  }

  /**
   * The management configuration
   */
  public BootManagementConfig getManagement()
  {
    return _management;
  }
  
  /**
   * Returns true if there is a <watchdog-manager> config.
   */
  public boolean isWatchdogManagerConfig()
  {
    return _isWatchdogManagerConfig;
  }

  /**
   * Finds a server.
   */
  public WatchdogClient findClient(String id)
  {
    return _watchdogMap.get(id);
  }
  
  
  WatchdogClient findClient(String serverId, WatchdogArgs args)
  {
    WatchdogClient client = null;

    if (serverId != null) {
      client = findClient(serverId);

      if (client != null)
        return client;
      
      // cloud/1292, server/6e11
      if (args.isDynamicServer()) {
        client = findShutdownClient(_args.getClusterId());
        
        if (client != null)
          return client;
        
        throw new ConfigException(L.l("Resin/{0}: {1} -cluster '{2}' does not match any defined <server>\nin {3}.",
                                      VersionFactory.getVersion(),
                                      _args.getCommand(),
                                      _args.getClusterId(), 
                                      _args.getResinConf()));
      }
      
      if (! args.getCommand().isStart() && ! args.getCommand().isConsole()) {
        throw new ConfigException(L.l("Resin/{0}: {1} -server '{2}' does not match any defined <server>\nin {3}.",
                                      VersionFactory.getVersion(),
                                      _args.getCommand(),
                                      _args.getServerId(), 
                                      _args.getResinConf()));
      }
    }
    
    // backward-compat default behavior
    if (serverId == null)
      client = findClient("");
    
    if (client != null)
      return client;
    
    ArrayList<WatchdogClient> clientList = findLocalClients();
    
    if (clientList.size() == 1) {
      client = clientList.get(0);
      
      // server/6e10
      if (client.getConfig().isRequireExplicitId())
        client = null;
    }

    // server/6e10
    if (args.isDynamicServer() || getHomeCluster() != null)
      return null;

    /*
    if (client == null && _args.getCommand().isShutdown()) {
      client = findShutdownClient(_args.getClusterId());
    }
    */

    if (client == null
        && (! _args.getCommand().isStart()
            && ! _args.getCommand().isConsole()
            || _args.isDynamicServer())) {
      client = findShutdownClient(_args.getClusterId());
    }

    if (client == null) {
      throw new ConfigException(L.l("Resin/{0}: default server cannot find a unique <server> or <server-multi>\nin {2}.",
                                    VersionFactory.getVersion(), 
                                    _args.getServerId(), 
                                    _args.getResinConf()));
    }
    
    return client;
  }

  public ArrayList<WatchdogClient> findLocalClients()
  {
    ArrayList<WatchdogClient> clientList = new ArrayList<WatchdogClient>();
    
    fillLocalClients(clientList);
    
    return clientList;
  }

  /**
   * Finds a server.
   */
  public WatchdogClient findShutdownClient(String clusterId)
  {
    WatchdogClient bestClient = null;
    
    for (WatchdogClient client : _watchdogMap.values()) {
      if (client == null)
        continue;
      
      if (clusterId != null && ! clusterId.equals(client.getClusterId()))
        continue;
      
      if (bestClient == null || client.getIndex() < bestClient.getIndex())
        bestClient = client;
    }
    
    return bestClient;
  }
  
  public void fillLocalClients(ArrayList<WatchdogClient> clientList)
  {
    ArrayList<InetAddress> localAddresses = getLocalAddresses();

    for (WatchdogClient client : _watchdogMap.values()) {
      if (client == null)
        continue;

      if (isLocalClient(localAddresses, client.getConfig())) {
        clientList.add(client);
      }
    }
  }
  
  public static boolean isLocalClient(ArrayList<InetAddress> localAddresses,
                                      WatchdogConfig config)
  {
    if (config.isRequireExplicitId()) {
      return false;
    }
    
    String address = config.getAddress();
    
    return isLocalAddress(localAddresses, address);
  }
  
  static ArrayList<InetAddress> getLocalAddresses()
  {
    return HostUtil.getLocalAddresses();
  }
  
  private static boolean isLocalAddress(ArrayList<InetAddress> localAddresses,
                                        String address)
  {
    if (address == null || "".equals(address))
      return false;
    
    try {
      InetAddress addr = InetAddress.getByName(address);
      
      if (localAddresses.contains(addr)) {
        return true;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    }
    
    return false;
  }
  
  public int getNextIndex()
  {
    return _watchdogMap.size();
  }

  /**
   * Finds a server.
   */
  public void addClient(WatchdogClient client)
  {
    _watchdogMap.put(client.getId(), client);
  }

  /**
   * Finds a server.
   */
  public WatchdogConfig findServer(String id)
  {
    return _serverMap.get(id);
  }

  /**
   * Finds a server.
   */
  public void addServer(WatchdogConfig config)
  {
    _serverMap.put(config.getId(), config);
  }

  /**
   * Finds a server.
   */
  WatchdogClient addDynamicClient(WatchdogArgs args)
  {
    if (! args.isDynamicServer() && ! isHomeCluster())
      throw new IllegalStateException();

    String clusterId = args.getClusterId();
    
    if (clusterId == null)
      clusterId = getHomeCluster();
    
    String address = args.getDynamicAddress();
    int port = args.getDynamicPort();
    
    BootClusterConfig cluster = findCluster(clusterId);

    if (cluster == null) {
      throw new ConfigException(L.l("'{0}' is an unknown cluster. --cluster must specify an existing cluster",
                                    clusterId));
    }

    if (! cluster.isDynamicServerEnable()) {
      throw new ConfigException(L.l("cluster '{0}' does not have <resin:ElasticCloudService>. --home-cluster requires a <resin:ElasticCloudService> tag.",
                                    clusterId));
    }

    WatchdogConfig config = cluster.createServer();
    config.setId(args.getDynamicServerId());
    config.setDynamic(true);
    config.setAddress(address);
    config.setPort(port);

    cluster.addServer(config);

    addServer(config);

    WatchdogClient client = new WatchdogClient(_system, BootResinConfig.this, config);
    addClient(client);

    return client;
  }

  /**
   * Creates the watchdog-manager config
   */
  public WatchdogManagerConfig createWatchdogManager()
  {
    _isWatchdogManagerConfig = true;
    
    return new WatchdogManagerConfig(_system, this);
  }

  /**
   * Adds a new default to the cluster.
   */
  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaultList.add(program);
  }

  public void addCluster(BootClusterProxy proxy)
  {
    BootClusterConfig cluster = findCluster(proxy.getId());
    
    if (cluster == null) {
      cluster = new BootClusterConfig(_system, this);
      cluster.setId(proxy.getId());

      for (int i = 0; i < _clusterDefaultList.size(); i++) {
        _clusterDefaultList.get(i).configure(cluster);
      }

      _clusterList.add(cluster);
    }
    
    proxy.getProgram().configure(cluster);
  }

  BootClusterConfig findCluster(String id)
  {
    if (id == null)
      return null;

    for (int i = 0; i < _clusterList.size(); i++) {
      BootClusterConfig cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
        return cluster;
    }

    return null;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
  }
}
