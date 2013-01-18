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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.cloud.security.SecurityService;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.SystemProperty;
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
  private String _clusterSystemKey;
  private String _homeCluster;
  private String _homeServer;
  private boolean _isElasticServer;
  private int _elasticServerPort;
  private boolean _isElasticDns;
  private ArrayList<ElasticServer> _elasticServerList;
  
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
  
  public void setClusterSystemKey(String digest)
  {
    if (digest == null || "".equals(digest))
      return;
    
    _clusterSystemKey = digest;
    
    SecurityService security = SecurityService.getCurrent();
    
    if (security != null)
      security.setSignatureSecret(digest);
  }

  public void setResinSystemAuthKey(String key)
  {
    setClusterSystemKey(key);
  }

  public String getClusterSystemKey()
  {
    return _clusterSystemKey;
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

  public boolean isElasticServer()
  {
    return _isElasticServer;
  }
  
  @Configurable
  public void setElasticServer(String servers)
  {
    if (servers == null
        || "".equals(servers)
        || "no".equalsIgnoreCase(servers)
        || "false".equalsIgnoreCase(servers)) {
      _isElasticServer = false;
      return;
    }
    
    _elasticServerList = new ArrayList<ElasticServer>();
    
    if ("yes".equalsIgnoreCase(servers)
        || "true".equalsIgnoreCase(servers)) {
      _isElasticServer = true;
      
      _elasticServerList.add(new ElasticServer(null, 1));
      
      return;
    }
    
    parseElasticServers(servers);
    
    _isElasticServer = true;
  }
  
  private void parseElasticServers(String servers)
  {
    String []serverList = servers.split("[\\s,]+");
    
    for (String server : serverList) {
      int p = server.indexOf(':');
      
      String cluster;
      int count;
      
      if (p >= 0) {
        cluster = server.substring(0, p);
        count = Integer.parseInt(server.substring(p + 1));
      }
      else if (Character.isDigit(server.charAt(0))) {
        cluster = null;
        count = Integer.parseInt(server);
      }
      else {
        cluster = server;
        count = 1; 
      }
      
      _elasticServerList.add(new ElasticServer(cluster, count));
    }
  }

  public ArrayList<ElasticServer> getElasticServers()
  {
    return _elasticServerList;
  }
  
  @Configurable
  public void setElasticDns(boolean isElasticDns)
  {
    _isElasticDns = isElasticDns;
  }
  
  public boolean isElasticDns()
  {
    return _isElasticDns;
  }
  
  @Configurable
  public void setElasticServerPort(int port)
  {
    _elasticServerPort = port;
  }
  
  public int getElasticServerPort()
  {
    return _elasticServerPort;
  }
  
  public int getElasticServerPort(WatchdogArgs arg, int count)
  {
    int port = arg.getDynamicPort();
    
    if (port <= 0)
      port = _elasticServerPort;
    
    if (port <= 0)
      port = 6830 + count;
    
    return port;
  }

  /*
  @Override
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  */
  
  @Configurable
  public void setHomeServer(String homeServer)
  {
    if (homeServer != null && ! homeServer.isEmpty())
      _homeServer = homeServer;
  }
  
  public String getHomeServer()
  {
    return _homeServer;
  }

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

  /**
   * Finds a server.
   */
  public WatchdogClient findClientByAddress(String address, int port)
  {
    if (port <= 0) {
      return null;
    }
    
    if (address == null) {
      return null;
    }
    
    for (WatchdogClient client : _watchdogMap.values()) {
      if (address.equals(client.getAddress())
          && port == client.getPort()) {
        return client;
      }
    }
    
    return null;
  }
  
  /**
   * Finds a matching client for the arguments.
   */
  WatchdogClient findClient(String cliServerId, WatchdogArgs args)
  {
    WatchdogClient client = null;
    
    String serverId = getServerId(args);

    if (serverId != null) {
      client = findClient(serverId);

      if (client != null)
        return client;

      // cloud/1292, server/6e11
      if (isElasticServer(args)) {
        return null;
      }
      
      throw new ConfigException(L.l("Resin/{0}: server '{1}' does not match a unique <server> or <server-multi>\nin {2}\nserver ids: {3}.",
                                    VersionFactory.getVersion(), 
                                    getDisplayServerName(cliServerId),
                                    _args.getResinConf(),
                                    toStringList(_watchdogMap.values())));
    }

    // cloud/1292, server/6e11
    if (isElasticServer(args)) {
      return null;
    }
    
    // backward-compat default behavior
    if (serverId == null) {
      client = findClient("default");
    }
    
    return client;
  }
  
  /**
   * Finds a matching client for the arguments.
   */
  WatchdogClient findLocalClient(String cliServerId, WatchdogArgs args)
  {
    ArrayList<WatchdogClient> clientList = findLocalClients(cliServerId);
    
    if (clientList.size() == 0) {
      return null;
    }

    WatchdogClient client = clientList.get(0);

    // server/6e10
    if (client != null && ! client.getConfig().isRequireExplicitId()) {
        return client;
    }
      
    return null;
  }
  
  /**
   * Finds a matching client for the arguments.
   */
  WatchdogClient findUniqueLocalClient(String cliServerId, WatchdogArgs args)
  {
    ArrayList<WatchdogClient> clientList = findLocalClients(cliServerId);
    
    if (clientList.size() == 0) {
      return null;
    }
    
    if (clientList.size() > 1) {
      throw new ConfigException(L.l("Resin/{0}: server '{1}' does not match a unique <server> or <server-multi>\nin {2}\nserver ids: {3}.",
                                    VersionFactory.getVersion(), 
                                    getDisplayServerName(cliServerId),
                                    _args.getResinConf(),
                                    toStringList(clientList)));
    }

    WatchdogClient client = clientList.get(0);

    if (client != null && ! client.getConfig().isRequireExplicitId()) {
      return client;
    }
      
    return null;
  }

  private String toStringList(Collection<WatchdogClient> clientList)
  {
    StringBuilder sb = new StringBuilder();
    
    ArrayList<String> list = new ArrayList<String>();
    
    for (WatchdogClient client : clientList) {
      list.add(client.getId());
    }
    
    Collections.sort(list);
    
    for (int i = 0; i < list.size(); i++) {
      if (i != 0)
        sb.append(", ");
      
      sb.append(list.get(i));
    }
    
    return sb.toString();
  }
  
  private String getDisplayServerName(String name)
  {
    if (name == null || "".equals(name))
      return "default";
    else
      return name;
  }
  
  String getServerId(WatchdogArgs args)
  {
    String serverId = args.getServerId();

    if (serverId != null) {
      return serverId;
    }
    
    return getHomeServer();
  }
  
  boolean isDynamicServerAllowed(WatchdogArgs args)
  {
    return isElasticServer(args) || getHomeCluster() != null;
  }
  
  String getClusterId(WatchdogArgs args)
  {
    if (args.getClusterId() != null) {
      return args.getClusterId();
    }
    else {
      return getHomeCluster();
    }
  }
  
  boolean isElasticServer(WatchdogArgs args)
  {
    if (args.isElasticServer()) {
      return true;
    }
    else {
      return isElasticServer();
    }
  }
  
  boolean isElasticDns(WatchdogArgs args)
  {
    return args.isElasticDns() || isElasticDns();
  }

  public ArrayList<WatchdogClient> findLocalClients(String serverId)
  {
    ArrayList<WatchdogClient> clientList = new ArrayList<WatchdogClient>();
    
    if (serverId != null) {
      // server/6e27
      WatchdogClient client = _watchdogMap.get(serverId);
      
      if (client != null) {
        clientList.add(client);
        return clientList;
      }
    }
    
    fillLocalClients(clientList);
    
    return clientList;
  }
  
  public ArrayList<String> findLocalClientIds(String serverId)
  {
    ArrayList<WatchdogClient> clientList = new ArrayList<WatchdogClient>();
    
    fillLocalClients(clientList);
    
    ArrayList<String> ids = new ArrayList<String>();
    
    for (WatchdogClient client : clientList) {
      ids.add(client.getId());
    }
    
    Collections.sort(ids);
    
    return ids;
  }

  /**
   * Finds a server.
   */
  public WatchdogClient findWatchdogClient(String clusterId)
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
    
    Collections.sort(clientList, new ClientComparator());
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
  WatchdogClient addElasticClient(WatchdogArgs args)
  {
    if (! isElasticServer(args) && ! isHomeCluster()) {
      throw new IllegalStateException();
    }
    
    int count = 0;
    
    for (ElasticServer elasticServer : getElasticServerList()) {
      int serverCount = elasticServer.getCount();
      
      for (int i = 0; i < serverCount; i++) {
        WatchdogClient client = addElasticServer(elasticServer, args, 
                                                 i, count++); 
      
        return client;
      }
    }
    
    return null;
  }
  
  ArrayList<ElasticServer> getElasticServerList()
  {
    ArrayList<ElasticServer> elasticServers = _elasticServerList;
    
    if (elasticServers == null) {
      elasticServers = new ArrayList<ElasticServer>();
      elasticServers.add(new ElasticServer(null, 1));
    }
    
    return elasticServers;
  }
  
  private WatchdogClient addElasticServer(ElasticServer elasticServer,
                                          WatchdogArgs args,
                                          int index,
                                          int count)
  {
    String clusterId = elasticServer.getCluster();

    if (clusterId == null) {
      clusterId = args.getClusterId();
    }
    
    if (clusterId == null) {
      clusterId = getHomeCluster();
    }
    
    String address = args.getDynamicAddress();
    int port = getElasticServerPort(args, count);
    
    BootClusterConfig cluster = findCluster(clusterId);

    if (cluster == null) {
      throw new ConfigException(L.l("'{0}' is an unknown cluster. --cluster must specify an existing cluster",
                                    clusterId));
    }

    if (! cluster.isClusterServerEnable()) {
      throw new ConfigException(L.l("cluster '{0}' does not have <resin:ElasticCloudService>. --elastic-server requires a <resin:ElasticCloudService> tag.",
                                    clusterId));
    }
    
    clusterId = cluster.getId();
    
    String serverId = args.getServerId();
    
    if (serverId == null) {
      serverId = "dyn-" + clusterId + "-" + index;
    }
    
    WatchdogConfigHandle configHandle = cluster.createServer();
    configHandle.setId(serverId);
    // configHandle.setDynamic(true);
    configHandle.setAddress(address);
    configHandle.setPort(port);
    
    if (findClient(configHandle.getId()) != null) {
      throw new ConfigException(L.l("--elastic-server '{0}' already exists as a static server."
                                    + " Elastic server names must not conflict with static server names.",
                                    configHandle.getId()));
    }
    
    WatchdogConfig config = cluster.addServer(configHandle);
    
    config.setElastic(true);
    config.setElasticServerPort(port);
    config.setElasticServerCluster(clusterId);
    
    addServer(config);

    WatchdogClient client
      = new WatchdogClient(_system, BootResinConfig.this, config);
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
      
      Object oldCluster = Config.getProperty("cluster");
      
      try {
        Config.setProperty("cluster", new ConfigVar(cluster));
        
        for (int i = 0; i < _clusterDefaultList.size(); i++) {
          _clusterDefaultList.get(i).configure(cluster);
        }
      } finally {
        Config.setProperty("cluster", oldCluster);
      }

      _clusterList.add(cluster);
    }
    
    proxy.getProgram().configure(cluster);
  }

  BootClusterConfig findCluster(String id)
  {
    if (id == null) {
      // cloud/129c
      if (_clusterList.size() == 1)
        return _clusterList.get(0);
      else
        return null;
    }

    for (int i = 0; i < _clusterList.size(); i++) {
      BootClusterConfig cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
        return cluster;
    }

    return null;
  }
  
  public void addSystemProperty(SystemProperty property)
  {
    // server/6e51
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
  }
  
  public static class ConfigVar {
    private BootClusterConfig _cluster;

    public ConfigVar(BootClusterConfig cluster)
    {
      _cluster = cluster;
    }
    
    public String getId()
    {
      return _cluster.getId();
    }
  }
  
  static class ClientComparator implements Comparator<WatchdogClient> {
    @Override
    public int compare(WatchdogClient a, WatchdogClient b)
    {
      return a.getId().compareTo(b.getId());
    }
  }
  
  static class ElasticServer {
    private final String _cluster;
    private final int _count;
    
    ElasticServer(String cluster, int count)
    {
      _cluster = cluster;
      _count = count;
    }
    
    String getCluster()
    {
      return _cluster;
    }
    
    int getCount()
    {
      return _count;
    }
  }
}
