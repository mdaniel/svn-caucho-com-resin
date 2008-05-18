/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;

public class BootManager implements EnvironmentBean
{
  private static final L10N L = new L10N(BootManager.class);

  private boolean _isWatchdogManagerConfig;
  
  private ArrayList<ContainerProgram> _clusterDefaultList
    = new ArrayList<ContainerProgram>();

  private ArrayList<ClusterConfig> _clusterList
    = new ArrayList<ClusterConfig>();
  
  private HashMap<String,WatchdogClient> _watchdogMap
    = new HashMap<String,WatchdogClient>();

  private ClassLoader _classLoader;

  private WatchdogArgs _args;
  
  private Path _resinHome;
  private Path _rootDirectory;
  
  private ManagementConfig _management;
  private String _password;

  BootManager(WatchdogArgs args)
  {
    _args = args;
  
    _classLoader = new EnvironmentClassLoader();
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

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Adds the management configuration
   */
  public void setManagement(ManagementConfig management)
  {
    _management = management;
  }
  
  /**
   * Returns the management password.
   */
  public String getAdminCookie()
  {
    if (_management != null)
      return _management.getAdminCookie();
    else
      return _password;
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
  public void addClient(WatchdogClient client)
  {
    _watchdogMap.put(client.getId(), client);
  }

  /**
   * Finds a server.
   */
  public WatchdogClient addDynamicClient(WatchdogArgs args)
  {
    if (! args.isDynamicServer())
      throw new IllegalStateException();

    String clusterId = args.getDynamicCluster();
    String address = args.getDynamicAddress();
    int port = args.getDynamicPort();

    ClusterConfig cluster = findCluster(clusterId);

    if (cluster == null)
      throw new ConfigException(L.l("'{0}' is an unknown cluster. -dynamic-server must specify an existing cluster",
				    clusterId));

    if (! cluster.isDynamicServerEnable()) {
      throw new ConfigException(L.l("cluster '{0}' does not have <dynamic-server-enable>. -dynamic-server requires a <dynamic-server-enable> tag.",
				    clusterId));
    }

    WatchdogConfig config = cluster.createServer();
    config.setId(address + "-" + port);
    config.setDynamic(true);
    config.setAddress(address);
    config.setPort(port);

    cluster.addServer(config);

    WatchdogClient client = new WatchdogClient(BootManager.this, config);
    addClient(client);

    return client;
  }

  /**
   * Creates the watchdog-manager config
   */
  public WatchdogManagerConfig createWatchdogManager()
  {
    _isWatchdogManagerConfig = true;
    
    return new WatchdogManagerConfig();
  }

  /**
   * Adds a new default to the cluster.
   */
  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaultList.add(program);
  }

  public ClusterConfig createCluster()
  {
    ClusterConfig cluster = new ClusterConfig();

    for (int i = 0; i < _clusterDefaultList.size(); i++)
      _clusterDefaultList.get(i).configure(cluster);

    _clusterList.add(cluster);
    
    return cluster;
  }

  ClusterConfig findCluster(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      ClusterConfig cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
	return cluster;
    }

    return null;
  }

  public ServerCompatConfig createServer()
  {
    return new ServerCompatConfig();
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addThreadPool(ConfigProgram program)
  {
  }
  
  public void setGroupName(ConfigProgram program)
  {
  }
  
  public void setUserName(ConfigProgram program)
  {
  }

  public void setMinFreeMemory(ConfigProgram program)
  {
  }
    
  public void setSecurityManager(ConfigProgram program)
  {
  }

  public void addSecurityProvider(Class providerClass)
  {
  }

  public void setEnvironmentSystemProperties(ConfigProgram program)
  {
  }

  //
  // configuration classes
  //

  public class WatchdogManagerConfig {
    private ArrayList<ContainerProgram> _watchdogDefaultList
      = new ArrayList<ContainerProgram>();

    public void setWatchdogPort(int watchdogPort)
    {
      if (_args.getWatchdogPort() == 0)
	_args.setWatchdogPort(watchdogPort);
    }
    
    /**
     * Adds a new server to the cluster.
     */
    public void addWatchdogDefault(ContainerProgram program)
    {
      _watchdogDefaultList.add(program);
    }

    public WatchdogConfig createWatchdog()
    {
      WatchdogConfig config = new WatchdogConfig(getArgs());

      for (int i = 0; i < _watchdogDefaultList.size(); i++)
	_watchdogDefaultList.get(i).configure(config);

      return config;
    }

    public void addWatchdog(WatchdogConfig config)
      throws ConfigException
    {
      if (findClient(config.getId()) != null)
	throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
				      config.getId()));
      
      addClient(new WatchdogClient(BootManager.this, config));
    }
  }

  public class ClusterConfig {
    private String _id = "";
    private boolean _isDynamicServerEnable;
    private ArrayList<ContainerProgram> _serverDefaultList
      = new ArrayList<ContainerProgram>();

    public void setId(String id)
    {
      _id = id;
    }

    public String getId()
    {
      return _id;
    }

    public void setDynamicServerEnable(boolean isEnabled)
    {
      _isDynamicServerEnable = isEnabled;
    }

    public boolean isDynamicServerEnable()
    {
      return _isDynamicServerEnable;
    }

    /**
     * Adds a new server to the cluster.
     */
    public void addServerDefault(ContainerProgram program)
    {
      _serverDefaultList.add(program);
    }

    public void addManagement(ManagementConfig management)
    {
      BootManager.this.setManagement(management);
    }

    public WatchdogConfig createServer()
    {
      WatchdogConfig config = new WatchdogConfig(getArgs());

      for (int i = 0; i < _serverDefaultList.size(); i++)
	_serverDefaultList.get(i).configure(config);

      return config;
    }

    public void addServer(WatchdogConfig config)
      throws ConfigException
    {
      if (_isWatchdogManagerConfig)
	return;
      
      if (findClient(config.getId()) != null)
	throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
				      config.getId()));
      
      addClient(new WatchdogClient(BootManager.this, config));
    }
  
    /**
     * Ignore items we can't understand.
     */
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }

  public class ServerCompatConfig {
    public ClusterCompatConfig createCluster()
    {
      return new ClusterCompatConfig();
    }
    
    public SrunCompatConfig createHttp()
    {
      return new SrunCompatConfig();
    }
    
    /**
     * Ignore items we can't understand.
     */
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }

  public class ClusterCompatConfig {
    public SrunCompatConfig createSrun()
    {
      return new SrunCompatConfig();
    }
    
    /**
     * Ignore items we can't understand.
     */
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }

  public class SrunCompatConfig {
    private String _id = "";

    public void setId(String id)
    {
      _id = id;
    }

    public void setServerId(String id)
    {
      _id = id;
    }
    
    /**
     * Ignore items we can't understand.
     */
    public void addBuilderProgram(ConfigProgram program)
    {
    }

    @PostConstruct
      public void init()
    {
      if (_isWatchdogManagerConfig)
	return;
      
      WatchdogClient client = findClient(_id);

      if (client != null)
	return;

      WatchdogConfig config = new WatchdogConfig(getArgs());
      
      client = new WatchdogClient(BootManager.this, config);
      
      _watchdogMap.put(_id, client);
    }
  }
}
