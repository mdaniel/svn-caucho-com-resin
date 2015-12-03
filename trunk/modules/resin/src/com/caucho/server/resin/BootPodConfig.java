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

package com.caucho.server.resin;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.cloud.network.ClusterServerProgram;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;

/**
 * The BootClusterConfig is the first-pass configuration of the cluster.
 * 
 * It matches the &lt;cluster> tag in the resin.xml
 */
public class BootPodConfig
{
  private BootClusterConfig _clusterConfig;
  
  private String _id;

  private ContainerProgram _serverDefaultProgram
    = new ContainerProgram();

  private ContainerProgram _podProgram
    = new ContainerProgram();

  private ArrayList<BootServerConfig> _servers
    = new ArrayList<BootServerConfig>();
  
  private CloudPod _cloudPod;

  /**
   * Creates a new resin server.
   */
  public BootPodConfig(BootClusterConfig clusterConfig)
  {
    _clusterConfig = clusterConfig;
  }
  
  /**
   * Returns the pod's id
   */
  public String getId()
  {
    return _id;
  }
  
  /**
   * Sets the pod's id
   */
  @Configurable
  public void setId(String id)
  {
    _id = id;
  }
  
  public BootClusterConfig getCluster()
  {
    return _clusterConfig;
  }
  
  /**
   * Adds a <server-default> for default server configuration.
   */
  @Configurable
  public void addServerDefault(ContainerProgram program)
  {
    _serverDefaultProgram.addProgram(program);
  }
  
  public ContainerProgram getServerDefault()
  {
    return _serverDefaultProgram;
  }

  @Configurable
  public BootServerConfig createServer()
    throws ConfigException
  {
    BootServerConfig server = new BootServerConfig(this);
    
    // _servers.add(server);

    return server;
  }

  @Configurable
  public void addServer(BootServerConfig server)
  {
    _servers.add(server);
  }

  public ArrayList<BootServerConfig> getServerList()
  {
    return _servers;
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _podProgram.addProgram(program);
  }
  
  ConfigProgram getProgram()
  {
    return _podProgram;
  }
  
  @PostConstruct
  public void init()
  {
    /*
    if (_id == null)
      throw new ConfigException(L.l("'id' is a required attribute for <pod>"));
      */
  }
  
  void initTopology(CloudPod cloudPod)
  {
    _cloudPod = cloudPod;
    
    cloudPod.putData(new ClusterServerProgram(_serverDefaultProgram));

    for (BootServerConfig bootServer : _servers) {
      initTopology(bootServer);
    }
  }
  
  void initTopology(BootServerConfig bootServer)
  {
    CloudPod cloudPod = _cloudPod;
    
    if (cloudPod == null) {
      getCluster().initTopology(this);
      cloudPod = _cloudPod;
    }
    
    CloudServer cloudServer;
    
    String id = bootServer.getId();
    String address = bootServer.getAddress();
    int port = bootServer.getPort();
    boolean isSecure = bootServer.isSecure();
    boolean isAllowExternal = bootServer.isAllowExternalAddress();
    
    cloudServer = cloudPod.findServer(id);
    
    if (cloudServer != null) {
    }
    else if (bootServer.isExternalAddress()) {
      cloudServer = cloudPod.createExternalStaticServer(id,
                                                        address,
                                                        port,
                                                        isSecure);
    } else {
      cloudServer = cloudPod.createStaticServer(id, address, port, 
                                                isSecure, isAllowExternal);
    }
    
    bootServer.initTopology(cloudServer);

  }
  
  /*
  CloudPod getCloudPod()
  {
    if (_cloudPod == null) {
      CloudCluster cluster = _clusterConfig.getCloudCluster();
    
      _cloudPod = cluster.createPod();
    }
    
    return _cloudPod;
  }
  */
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
