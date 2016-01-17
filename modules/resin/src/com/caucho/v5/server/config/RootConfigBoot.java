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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.io.SocketSystem;
import com.caucho.v5.loader.SystemProperty;
import com.caucho.v5.log.impl.LogConfig;
import com.caucho.v5.log.impl.LogHandlerConfig;
import com.caucho.v5.log.impl.LoggerConfig;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

public class RootConfigBoot
{
  private static final L10N L = new L10N(RootConfigBoot.class);
  private static final Logger log
    = Logger.getLogger(RootConfigBoot.class.getName());

  private PathImpl _configTemplate;
  
  private PathImpl _homeDirectory;
  private PathImpl _rootDirectory;
  private PathImpl _dataDirectory;
  
  private String _clusterSystemKey;
  private String _homeCluster;
  private String _homeServer;
  
  private boolean _isDynamicDns;
  
  private ContainerProgram _rootProgram
    = new ContainerProgram();
  
  //private final ArrayList<DomainConfigBoot> _domainList = new ArrayList<>();
  
  private ArrayList<ClusterConfigBootProgram> _pendingClusters = new ArrayList<>();
  private ArrayList<ClusterConfigBoot> _clusterList = new ArrayList<>();
  
  private ContainerProgram _clusterDefault
    = new ContainerProgram();
  
  private ServerConfigContainer _serverContainer = new ServerConfigContainer();
  
  private ArrayList<ServerConfigBoot> _servers = new ArrayList<>();
  
  private boolean _isSkipLog;
  
  public void setSkipLog(boolean isSkipLog)
  {
    _isSkipLog = isSkipLog;
  }

  public PathImpl getHomeDirectory()
  {
    return _homeDirectory;
  }

  /**
   * Returns the main cluster
   */
  public ClusterConfigBoot getClusterMain()
  {
    if (_clusterList.size() == 1) {
      return _clusterList.get(0);
    }
    
    return findCluster(ClusterConfigBoot.DEFAULT_NAME);
  }

  public List<ClusterConfigBoot>  getClusters()
  {
    return _clusterList;
  }
  
  //
  // root configuration
  //

  /**
   * config-template
   */
  
  @Configurable
  public void setConfigTemplate(PathImpl path)
  {
    _configTemplate = path;

    if (path == null) {
      return;
    }
    
    if (path.getTail().equals("none") || path.getTail().equals("null")) {
      _configTemplate = null;
    }
    else if (! path.canRead()) {
      log.warning(L.l("config-template {0} is not readable", path));
    }
  }
  
  public void setConfigTemplateImpl(PathImpl path)
  {
    _configTemplate = path;
  }
  
  public PathImpl getConfigTemplate()
  {
    return _configTemplate;
  }

  /**
   * cluster
   */
  @Configurable
  public void addCluster(ClusterConfigBootProgram proxy)
  {
    _pendingClusters.add(proxy);
  }
  
  /**
   * cluster-default
   */
  @Configurable
  public void addClusterDefault(ConfigProgram program)
  {
    _clusterDefault.addProgram(program.toContainer());
  }

  public ContainerProgram getClusterDefault()
  {
    return _clusterDefault;
  }
  
  public void setClusterSystemKey(String digest)
  {
    if (digest == null || "".equals(digest)) {
      return;
    }
    
    _clusterSystemKey = digest;
  }

  public String getClusterSystemKey()
  {
    return _clusterSystemKey;
  }

  /**
   * data-directory
   */
  @Configurable
  public void setDataDirectory(PathImpl path)
  {
    _dataDirectory = path;
  }
  
  public PathImpl getDataDirectory()
  {
    return _dataDirectory;
  }
  
  @Configurable
  public void setDynamicDns(boolean isDynamicDns)
  {
    _isDynamicDns = isDynamicDns;
  }
  
  public boolean isDynamicDns()
  {
    return _isDynamicDns;
  }
  
  /**
   * home-cluster
   */
  @Configurable
  public void setHomeCluster(String homeCluster)
  {
    if (homeCluster != null && ! homeCluster.isEmpty()) {
      _homeCluster = homeCluster;
    }
  }
  
  public String getHomeCluster()
  {
    return _homeCluster;
  }
  
  public boolean isHomeCluster()
  {
    return _homeCluster != null && ! "".equals(_homeCluster);
  }

  @Configurable
  public void setHomeServer(String homeServer)
  {
    if (homeServer != null && ! homeServer.isEmpty()) {
      _homeServer = homeServer;
    }
  }
  
  public String getHomeServer()
  {
    return _homeServer;
  }

  /**
   * log - 
   */
  @Configurable
  public LogConfig createLog()
  {
    return new LogConfig(_isSkipLog);
  }

  /**
   * log-handler
   */
  @Configurable
  public LogHandlerConfig createLogHandler()
  {
    return new LogHandlerConfig(_isSkipLog);
  }

  /**
   * logger
   */
  @Configurable
  public LoggerConfig addLogger()
  {
    return new LoggerConfig(_isSkipLog);
  }
  
  @Configurable
  public void setRootDirectory(PathImpl rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public PathImpl getRootDirectory()
  {
    return _rootDirectory;
  }

  public PathImpl getRootDirectory(ArgsDaemon args)
  {
    if (args.getRootDirectory() != null) {
      return args.getRootDirectory();
    }
    else if (getRootDirectory() != null) {
      return getRootDirectory();
    }
    else {
      return args.getDefaultRootDirectory();
    }
  }
  
  /*
  @Configurable
  public void addServer(ServerConfigBootProgram serverProgram)
  {
    _serverContainer.addServer(serverProgram);
  }
  */
  
  @Configurable
  public void addServerDefault(ContainerProgram program)
  {
    _serverContainer.addServerDefault(program);
  }

  public ContainerProgram getServerDefault()
  {
    return _serverContainer.getServerDefault();
  }

  public PathImpl getLogDirectory(ArgsDaemon args)
  {
    if (args.getLogDirectory() != null) {
      return args.getLogDirectory();
    }
    /*
    else if (getLogDirectory() != null) {
      return getLogDirectory();
    }
    */
    else {
      return getRootDirectory(args).lookup("log");
    }
  }
  
  //
  // configurable compatibility
  //
  
  @Configurable
  public void addSystemProperty(SystemProperty property)
  {
    // server/6e51
  }

  @Configurable
  public void setResinSystemAuthKey(String key)
  {
    setClusterSystemKey(key);
  }

  /*
  @Configurable
  public void addBeanProgram(ConfigProgram program)
  {
    _rootProgram.addProgram(program);
  }
  */
  
  @Configurable
  public void addContentProgram(ConfigProgram program)
  {
    _rootProgram.addProgram(program);
  }
  
  public ConfigProgram getProgram()
  {
    return _rootProgram;
  }
  
  //
  // initialization
  //
  
  public void init()
  {
    initClusters();
    
    // initMain();
  }
  
  private void initClusters()
  {
    ArrayList<ClusterConfigBootProgram> pendingList = new ArrayList<>(_pendingClusters);
    _pendingClusters.clear();
    
    for (ClusterConfigBootProgram proxy : pendingList) {
      ClusterConfigBoot cluster = findCluster(proxy.getId());
    
      if (cluster == null) {
        cluster = new ClusterConfigBoot(this, proxy.getId());

        Object oldCluster = ConfigContext.getProperty("cluster");

        try {
          ConfigContext.setProperty("cluster", new ConfigVar(cluster));

          _clusterDefault.configure(cluster);
        } finally {
          ConfigContext.setProperty("cluster", oldCluster);
        }

        _clusterList.add(cluster);
      }

      proxy.getProgram().configure(cluster);
      
      cluster.initServers();
    }
    
    _pendingClusters.clear();
    
    for (ClusterConfigBoot cluster : _clusterList) {
      cluster.initServers();
    }

    // cloud/1250
    /*
    String clusterMainId = ClusterConfigBoot.DEFAULT_NAME;
    
    ClusterConfigBoot clusterMain = findCluster(clusterMainId);
    
    if (clusterMain == null) {
      clusterMain = new ClusterConfigBoot(this, clusterMainId);
      
      _clusterDefault.configure(clusterMain);
      
      _clusterList.add(clusterMain);
      
      clusterMain.initServers();
    }
    */
  }
  
  public ClusterConfigBoot createDynamicCluster(String clusterId)
  {
    ClusterConfigBoot cluster = findCluster(clusterId);
    
    if (cluster == null) {
      cluster = new ClusterConfigBoot(this, clusterId);

      Object oldCluster = ConfigContext.getProperty("cluster");

      try {
        ConfigContext.setProperty("cluster", new ConfigVar(cluster));

        _clusterDefault.configure(cluster);
      } finally {
        ConfigContext.setProperty("cluster", oldCluster);
      }

      _clusterList.add(cluster);
      
      cluster.initServers();
    }
    
    return cluster;
  }
  
  private void initMain()
  {
    ClusterConfigBoot clusterMain = findCluster("cluster");
    Objects.requireNonNull(clusterMain);
    
    _serverContainer.initServers(clusterMain);
  }
  
  //
  // queries
  //

  public ClusterConfigBoot findCluster(String id)
  {
    if (id == null) {
      // cloud/129c
      if (_clusterList.size() == 1)
        return _clusterList.get(0);
      else
        return null;
    }

    for (int i = 0; i < _clusterList.size(); i++) {
      ClusterConfigBoot cluster = _clusterList.get(i);

      if (id.equals(cluster.getId())) {
        return cluster;
      }
    }

    return null;
  }

  public ClusterConfigBoot getSingleCluster()
  {
    if (_clusterList.size() == 1) {
      return _clusterList.get(0);
    }
    else {
      return null;
    }
  }
  
  /*
  @Override
  public void addDependency(PersistentDependency dependency)
  {
    // getClassLoader().addDependency(dependency);
  }
  */
  
  void addServerInit(ServerConfigBoot server)
  {
    if (! _servers.contains(server)) {
      _servers.add(server);
    }
  }
  
  public ServerConfigBoot findServer(String id)
  {
    if (id == null) {
      return null;
    }
    
    for (ServerConfigBoot server : _servers) {
      if (id.equals(server.getId())) {
        return server;
      }
    }
    
    return null;
  }
  
  public ServerConfigBoot findServerByName(String name)
  {
    for (ServerConfigBoot server : _servers) {
      if (name.equals(server.getDisplayName())) {
        return server;
      }
    }
    
    return null;
  }
  
  public ServerConfigBoot findServerByAddress(String address, int port)
  {
    for (ClusterConfigBoot cluster : _clusterList) {
      ServerConfigBoot server = cluster.findServerByAddress(address, port);
      
      if (server != null) {
        return server;
      }
    }
    
    return null;
  }
  
  /**
   * Returns servers where the address isn't specified.
   */
  public ArrayList<ServerConfigBoot> findDefaultServers()
  {
    ArrayList<ServerConfigBoot> serverList = new ArrayList<>();
    
    for (ServerConfigBoot server : _servers) {
      if ("".equals(server.getAddress())) {
        serverList.add(server);
      }
    }
    
    return serverList;
  }

  /**
   * The ids of servers that listen to one of the local IP addresses.
   */
  public ArrayList<String> findLocalServerIds(ArgsDaemon args)
  {
    ArrayList<String> ids = new ArrayList<>();
    
    for (ServerConfigBoot server : findLocalServers(args)) {
      ids.add(server.getDisplayName());
    }
    
    Collections.sort(ids);
    
    return ids;
  }
  
  /**
   * The servers that listen to one of the local IP addresses.
   */
  public ArrayList<ServerConfigBoot> findLocalServers(ArgsDaemon args)
  {
    ArrayList<ServerConfigBoot> serverList = new ArrayList<>();
    
    fillLocalServers(serverList, args);
    
    return serverList;
  }
  
  public void fillLocalServers(ArrayList<ServerConfigBoot> serverList,
                               ArgsDaemon args)
  {
    // retrives the local IP addresses from the SocketSystem.
    ArrayList<InetAddress> localAddresses
      = SocketSystem.current().getLocalAddresses();
    
    for (ServerConfigBoot server : _servers) {
      if (server == null) {
        continue;
      }
      
      if ("".equals(server.getAddress())) {
        if (findServer(server.getPort(), false) == null) {
          serverList.add(server);
        }
      }
      else if (isLocalServer(localAddresses, args, server)) {
        ServerConfigBoot oldServer = findServer(server.getPort(), true);
        
        if (oldServer != null) {
          serverList.remove(oldServer);
        }
        
        serverList.add(server);
      }
    }
    
    if (serverList.size() == 0) {
      for (ServerConfigBoot server : _servers) {
        if ("".equals(server.getAddress())) {
          serverList.add(server);
        }
      }
    }
    
    Collections.sort(serverList, new ServerComparator());
  }
  
  /**
   * The servers that listen to one of the local IP addresses.
   */
  public ArrayList<ServerConfigBoot> findServers()
  {
    ArrayList<ServerConfigBoot> serverList = new ArrayList<>();
    
    serverList.addAll(_servers);
    
    Collections.sort(serverList, new ServerComparator());
    
    return serverList;
  }
  
  private ServerConfigBoot findServer(int port, boolean isDefault)
  {
    for (ServerConfigBoot server : _servers) {
      if (server.getPort() != port) {
        continue;
      }
      
      if (isDefault && "".equals(server.getAddress())) {
        return server;
      }
      else if (! isDefault && ! "".equals(server.getAddress())) {
        return server;
      }
    }
    
    return null;
  }
  
  /**
   * Checks to see if the server's address is one of the local IP addresses.
   */
  private boolean isLocalServer(ArrayList<InetAddress> localAddresses,
                                ArgsDaemon args,
                                ServerConfigBoot server)
  {
    if (server.isRequireExplicitId()) {
      return server.matchId(args.getServerId());
    }
    
    String address = server.getAddress();
    
    return isLocalAddress(localAddresses, address);
  }
  
  /**
   * Tests if the address is a local IP address. 
   */
  
  private boolean isLocalAddress(ArrayList<InetAddress> localAddresses,
                                 String address)
  {
    if (address == null || "".equals(address)) {
      return true;
    }
    
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

  /**
   * Finds a server in the cluster to be used for watchdog commands. The 
   * watchdog server might not correspond to an actual server, but the 
   * watchdog address and port should match.
   */
  public ServerConfigBoot findWatchdogServer(String clusterId)
  {
    ClusterConfigBoot cluster = findCluster(clusterId);

    if (cluster == null) {
      return null;
    }
    
    for (ServerConfigBoot server : cluster.getServerList()) {
      return server;
    }
    
    return null;
  }
  
  public ServerConfigBoot findWatchdogServer()
  {
    for (ClusterConfigBoot cluster : _clusterList) {
      for (ServerConfigBoot server : cluster.getServerList()) {
        return server;
      }
    }
    
    return null;
  }

  public void configServer(Object bean, ServerBartender server)
  {
    ServerConfigBoot serverConfig = findServer(server.getId());
    
    if (serverConfig != null) {
      serverConfig.configure(bean);
    }
  }
 
  public static class ConfigVar {
    private ClusterConfigBoot _cluster;

    public ConfigVar(ClusterConfigBoot cluster)
    {
      _cluster = cluster;
    }
    
    public String getId()
    {
      return _cluster.getId();
    }
  }
  
  static class ServerComparator implements Comparator<ServerConfigBoot> {
    @Override
    public int compare(ServerConfigBoot a, ServerConfigBoot b)
    {
      String addressA = a.getAddress();
      String addressB = b.getAddress();
      
      int cmp = addressA.compareTo(addressB);
      
      if (cmp != 0) {
        return cmp;
      }
      
      cmp = a.getPort() - b.getPort();
      
      return cmp;
    }
  }
}
