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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.DependencyBean;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.vfs.PersistentDependency;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class BootResinConfig extends AbstractResinConfig
  implements SchemaBean, DependencyBean
{
  private static final Logger log
    = Logger.getLogger(BootResinConfig.class.getName());
  
  private ResinSystem _resinSystem;
  
  private String _clusterSystemKey;
  
  private String _homeServer;
  private String _homeCluster;
  private boolean _isElasticServer;
  
  private ContainerProgram _resinProgram
    = new ContainerProgram();

  private ArrayList<ConfigProgram> _clusterDefaults
    = new ArrayList<ConfigProgram>();

  private ArrayList<BootClusterConfig> _clusters
    = new ArrayList<BootClusterConfig>();

  private int _elasticServerPort;

  private String _elasticServerAddress;

  /**
   * Creates a new resin server.
   */
  public BootResinConfig(ResinSystem resinSystem)
  {
    _resinSystem = resinSystem;
  }
  
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }

  /**
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/resin.rnc";
  }
  
  /**
   * Obsolete
   */
  @Configurable
  public void setResinSystemAuthKey(String key)
  {
    setClusterSystemKey(key);
  }
  
  @Configurable
  public void setClusterSystemKey(String key)
  {
    _clusterSystemKey = key;
  }
  
  public String getClusterSystemKey()
  {
    return _clusterSystemKey;
  }
  
  public void setHomeServer(String homeServer)
  {
    if (homeServer != null && ! homeServer.isEmpty())
      _homeServer = homeServer;
  }
  
  public String getHomeServer()
  {
    return _homeServer;
  }
  
  public void setHomeCluster(String homeCluster)
  {
    _homeCluster = homeCluster;
  }
  
  public String getHomeCluster()
  {
    return _homeCluster;
  }
  
  public void setElasticServer(boolean isElasticServer)
  {
    _isElasticServer = isElasticServer;
  }

  public boolean isElasticServer(ResinArgs args)
  {
    if (args.isElasticServer()) {
      return args.isElasticServer();
    }
    else {
      return _isElasticServer;
    }
  }
  
  public void setElasticServerPort(int port)
  {
    _elasticServerPort = port;
  }

  public int getElasticServerPort(ResinArgs args)
  {
    if (args.getElasticServerPort() > 0) {
      return args.getElasticServerPort();
    }
    else {
      return _elasticServerPort;
    }
  }
  
  public void setElasticServerAddress(String address)
  {
    _elasticServerAddress = address;
  }

  public String getElasticServerAddress(ResinArgs args)
  {
    if (args.getElasticServerAddress() != null) {
      return args.getElasticServerAddress();
    }
    else {
      return _elasticServerAddress;
    }
  }
  
  /**
   * Adds a <cluster-default> for default cluster configuration.
   */
  @Configurable
  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaults.add(program);
  }

  @Configurable
  public void addCluster(BootClusterProxy clusterProxy)
    throws ConfigException
  {
    BootClusterConfig cluster = addClusterById(clusterProxy.getId());
    
    clusterProxy.getProgram().configure(cluster);
  }

  BootClusterConfig addClusterById(String id)
    throws ConfigException
  {
    BootClusterConfig cluster = findCluster(id);

    if (cluster == null) {
      cluster = new BootClusterConfig(this);
      cluster.setId(id);
    
      _clusters.add(cluster);
      
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();
      Object oldCluster = null;
      
      try {
        oldCluster = Config.getProperty("cluster");
        Config.setProperty("cluster", new ClusterConfigVar(cluster));
        
        for (int i = 0; i < _clusterDefaults.size(); i++) {
          _clusterDefaults.get(i).configure(cluster);
        }
      
        cluster.init();
      } finally {
        Config.setProperty("cluster", oldCluster);
        
        thread.setContextClassLoader(loader);
      }
    }
    
    return cluster;
  }

  public BootClusterConfig findCluster(String id)
  {
    if (id == null) {
      if (_clusters.size() == 1)
        return _clusters.get(0);
      else
        return null;
    }
    
    for (BootClusterConfig cluster : _clusters) {
      if (id.equals(cluster.getId())) {
        return cluster;
      }
    }
    
    return null;
  }

  public ArrayList<BootClusterConfig> getClusterList()
  {
    return _clusters;
  }
  
  BootServerConfig findServer(String id)
  {
    for (BootClusterConfig cluster : getClusterList()) {
      for (BootPodConfig pod : cluster.getPodList()) {
        for (BootServerConfig server : pod.getServerList()) {
          if (id.equals(server.getId()))
            return server;
        }
      }
    }
    
    return null;
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _resinProgram.addProgram(program);
  }
  
  public ConfigProgram getProgram()
  {
    return _resinProgram;
  }
  
  @Override
  public void addDependency(PersistentDependency dependency)
  {
    getClassLoader().addDependency(dependency);
  }
  
  //
  // topology init
  //
  
  CloudSystem initTopology()
  {
    TopologyService topology  = _resinSystem.getService(TopologyService.class);
    
    if (topology == null) {
      _resinSystem.addServiceIfAbsent(new TopologyService(_resinSystem.getId()));
      
      topology  = _resinSystem.getService(TopologyService.class);
    }
    
    initTopology(topology.getSystem());
    
    return topology.getSystem();
  }
  
  private void initTopology(CloudSystem cloudSystem)
  {
    for (BootClusterConfig bootCluster : _clusters) {
      initTopology(bootCluster);
    }
  }

  /**
   * @param bootClusterConfig
   */
  public void initTopology(BootClusterConfig bootCluster)
  {
    CloudSystem cloudSystem = TopologyService.getCurrentSystem();
    
    CloudCluster cloudCluster = cloudSystem.findCluster(bootCluster.getId());
    
    if (cloudCluster == null)
      cloudCluster = cloudSystem.createCluster(bootCluster.getId());
    
    bootCluster.initTopology(cloudCluster);
  }

  public BootServerConfig findLocalServer()
  {
    for (BootClusterConfig cluster : getClusterList()) {
      for (BootPodConfig pod : cluster.getPodList()) {
        for (BootServerConfig  server : pod.getServerList()) {
          if (server.isRequireExplicitId())
            continue;
          
          try {
            InetAddress address = InetAddress.getByName(server.getAddress());

            if (address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isLoopbackAddress()) {
              return server;
            }
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    }
    
    return null;
  }
  
  static class ClusterConfigVar {
    private BootClusterConfig _cluster;

    public ClusterConfigVar(BootClusterConfig cluster)
    {
      _cluster = cluster;
    }
    
    public String getId()
    {
      return _cluster.getId();
    }
  }
}
