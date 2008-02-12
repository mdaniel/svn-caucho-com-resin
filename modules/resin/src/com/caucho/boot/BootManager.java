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
  
  private ArrayList<ContainerProgram> _clusterDefaultList
    = new ArrayList<ContainerProgram>();
  
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
    return _args.getLogDirectory();
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
    
    return cluster;
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

  public class ClusterConfig {
    private ArrayList<ContainerProgram> _serverDefaultList
      = new ArrayList<ContainerProgram>();

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

    public WatchdogClient createServer()
    {
      WatchdogClient client = new WatchdogClient(BootManager.this);

      for (int i = 0; i < _serverDefaultList.size(); i++)
	_serverDefaultList.get(i).configure(client);

      return client;
    }

    public void addServer(WatchdogClient client)
      throws ConfigException
    {
      if (findClient(client.getId()) != null)
	throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
				      client.getId()));
      
      addClient(client);
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
      WatchdogClient client = findClient(_id);

      if (client != null)
	return;
      
      client = new WatchdogClient(BootManager.this);
      
      client.setId(_id);
      
      _watchdogMap.put(_id, client);
    }
  }
}
