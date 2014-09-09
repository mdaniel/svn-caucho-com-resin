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

import com.caucho.config.Config;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.vfs.Path;

/**
 * Configuration handle for a watchdog before full configuration.
 */
class WatchdogConfigHandle
{
  private String _id = "default";
  private final int _index;
  
  private final BootClusterConfig _cluster;

  private final WatchdogArgs _args;
  private final Path _rootDirectory;
  
  private String _serverAddress = "127.0.0.1";
  private int _serverPort;
  
  private ContainerProgram _program = new ContainerProgram();

  WatchdogConfigHandle(BootClusterConfig cluster,
                       WatchdogArgs args,
                       Path rootDirectory,
                       int index)
  {
    _cluster = cluster;
    
    _args = args;
    _rootDirectory = rootDirectory;
    _index = index;
  }

  public void setId(String id)
  {
    if (id == null || "".equals(id))
      id = "default";
    
    _id = id;
  }

  public String getId()
  {
    return _id;
  }
  
  public int getIndex()
  {
    return _index;
  }
  
  public BootClusterConfig getCluster()
  {
    return _cluster;
  }
  
  public String getHomeCluster()
  {
    if (_cluster != null)
      return _cluster.getResin().getHomeCluster();
    else
      return null;
  }

  WatchdogArgs getArgs()
  {
    return _args;
  }

  public void setAddress(String address)
  {
    _serverAddress = address;
  }
  
  public String getAddress()
  {
    return _serverAddress;
  }

  public void setPort(int port)
  {
    _serverPort = port;
  }
  
  public int getPort()
  {
    return _serverPort;
  }

  /**
   * Ignore items we can't understand.
   */
  public void addProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Configures the actual watchdog config.
   */
  WatchdogConfig configure()
  {
    WatchdogConfig config
      = new WatchdogConfig(_id, _cluster, _args, _rootDirectory, _index);
    
    String oldRvar0 = (String) Config.getProperty("rvar0");
    String oldRvar1 = (String) Config.getProperty("rvar1");

    try {
      Config.setProperty("rvar0", _id);
      Config.setProperty("rvar1", _cluster.getId());
      
      config.setAddress(_serverAddress);
      config.setPort(_serverPort);

      _program.configure(config);
    } finally {
      Config.setProperty("rvar0", oldRvar0);
      Config.setProperty("rvar1", oldRvar1);
    }
    
    return config;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
