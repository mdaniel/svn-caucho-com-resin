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

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.vfs.Path;
import com.caucho.server.admin.Management;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

public class ResinConfig implements EnvironmentBean
{
  private ArrayList<ContainerProgram> _clusterDefaultList
    = new ArrayList<ContainerProgram>();
  
  private ArrayList<ClusterConfig> _clusterList
    = new ArrayList<ClusterConfig>();

  private ClassLoader _classLoader;

  private Management _management;

  private ResinWatchdogManager _manager;
  private Path _resinHome;
  private Path _rootDirectory;
  private String _password;

  ResinConfig(ResinWatchdogManager manager, Path resinHome, Path serverRoot)
  {
    _manager = manager;
    
    _resinHome = resinHome;
    _rootDirectory = serverRoot;

    _classLoader = new EnvironmentClassLoader();
  }

  public ResinWatchdogManager getManager()
  {
    return _manager;
  }
  
  public Path getResinHome()
  {
    return _resinHome;
  }

  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Adds the management configuration
   */
  public void addManagement(Management management)
  {
    _management = management;
  }
  
  /**
   * Returns the management password.
   */
  public String getManagementPassword()
  {
    if (_management != null)
      return _management.getRemoteCookie();
    else
      return null;
  }

  /**
   * Adds a new default to the cluster.
   */
  public void addClusterDefault(ContainerProgram program)
    throws Throwable
  {
    _clusterDefaultList.add(program);
  }

  public ClusterConfig createCluster()
  {
    ClusterConfig cluster = new ClusterConfig(this);

    for (int i = 0; i < _clusterDefaultList.size(); i++)
      _clusterDefaultList.get(i).configure(cluster);
    
    _clusterList.add(cluster);

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

  public void setManagement(ConfigProgram program)
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

  /**
   * Finds a server.
   */
  public ClusterConfig findCluster(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      ClusterConfig cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
	return cluster;
    }

    return null;
  }

  /**
   * Finds a server.
   */
  public ResinWatchdog findServer(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      ClusterConfig cluster = _clusterList.get(i);

      ResinWatchdog server = cluster.findServer(id);

      if (server != null)
	return server;
    }

    return null;
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
      ResinWatchdog server = findServer(_id);

      if (server != null)
	return;

      ClusterConfig cluster = findCluster("");

      if (cluster == null)
	cluster = createCluster();

      server = cluster.createServer();
      server.setId(_id);
      cluster.addServer(server);
    }
  }
}
