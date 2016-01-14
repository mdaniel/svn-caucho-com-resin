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
import java.util.Objects;

import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.ConfigurableDeprecated;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.vfs.ServerSocketJni;
import com.caucho.v5.vfs.ServerSocketBar;

@Configurable
public class ClusterConfigBoot
{
  public static final String DEFAULT_NAME = "cluster";
  
  private RootConfigBoot _root;
  
  private final String _id;
  
  private ConfigProgram _stdoutLog;

  private ContainerProgram _clusterProgram
    = new ContainerProgram();
  
  private PodConfigBoot _podSelf = new PodConfigBoot();
  
  private ArrayList<PodConfigBoot> _podList
    = new ArrayList<>();
  
  private ArrayList<ServerConfigBoot> _serverList
    = new ArrayList<>();

  private String _serverHeader;

  ClusterConfigBoot(RootConfigBoot root, String id)
  {
    Objects.requireNonNull(root);
    Objects.requireNonNull(id);
    
    if ("".equals(id)) {
      throw new IllegalArgumentException();
    }
    
    _root = root;
    _id = id;
    
    _podSelf.setId("cluster");
    _podSelf.setType(PodBartender.PodType.cluster);
    
    _podList.add(_podSelf);
  }
  
  public RootConfigBoot getRoot()
  {
    return _root;
  }
  
  //
  // configuration
  //

  public String getId()
  {
    return _id;
  }
  
  //
  // allowed attributes
  //
  
  @ConfigurableDeprecated
  public void addAccessLog(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addCache(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addDevelopmentModeErrorPage(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addEarDefault(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void addHost(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void addHostDefault(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void addHostDeploy(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void setMaxUriLength(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void setUrlLengthMax(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @Configurable
  public void setSslSessionCookie(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * Creates a new pod.
   */
  @Configurable
  public void addPod(PodConfigBoot pod)
  {
    _podList.add(pod);
  }

  public ArrayList<PodConfigBoot> getPodList()
  {
    return _podList;
  }
  
  /**
   * access-log-buffer-size
   */
  @Configurable
  public void setAccessLogBufferSize(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addProxyCache(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * ignore-client-disconnect
   */
  @Configurable
  public void setIgnoreClientDisconnect(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * Creates a new server.
   */
  @Configurable
  public void addServer(ServerConfigBootProgram server)
  {
    _podSelf.addServer(server);
  }

  public void addServer(String address, int port)
  {
    if (findServerByAddress(address, port) != null) {
      return;
    }

    ServerConfigBootProgram server = new ServerConfigBootProgram();
    server.setAddress(address);
    server.setPort(port);
    
    addServer(server);
  }

  
  /**
   * Adds a <server-default> for default server configuration.
   */
  @Configurable
  public void addServerDefault(ConfigProgram program)
  {
    _podSelf.addServerDefault(program);
  }

  @Configurable
  public void addServerMulti(ServerMultiConfigBoot serverMulti)
  {
    int index = 0;
    for (String address : serverMulti.getAddressList()) {
      ServerConfigBootProgram server = new ServerConfigBootProgram();
      
      server.setPort(serverMulti.getPort());
      server.setAddress(address);
      server.setId(serverMulti.getIdPrefix() + index);
      
      server.addContentProgram(serverMulti.getServerProgram());
      
      addServer(server);
      
      index++;
    }
    
    // getDefaultPod().addServerMulti(serverMulti);
  }

  /**
   * Returns the server default program.
   */
  public ContainerProgram getServerDefault()
  {
    return _podSelf.getServerDefault();
  }
  
  //
  // backwards compat
  //
  
  @ConfigurableDeprecated
  public void addAlternateSessionUrlPrefix(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addPersistentStore(ConfigProgram program)
  {
    
  }
  
  /**
   * session-cookie
   */
  @ConfigurableDeprecated
  public void addSessionCookie(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * session-url-prefix
   */
  @ConfigurableDeprecated
  public void addSessionUrlPrefix(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  @ConfigurableDeprecated
  public void addStdoutLog(ContainerProgram config)
  {
    _stdoutLog = config;
  }
  
  public ConfigProgram getStdoutLog()
  {
    return _stdoutLog;
  }
  
  @Configurable
  public void addRootDirectory(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * server-header
   */
  @Configurable
  public void addServerHeader(String serverHeader)
  {
    _serverHeader = serverHeader;
  }
  
  public String getServerHeader()
  {
    return _serverHeader;
  }
  
  /**
   * url-character-encoding
   */
  @ConfigurableDeprecated
  public void addUrlCharacterEncoding(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * web-app-default
   */
  @Configurable
  public void addWebAppDefault(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  /**
   * Add a program for each bean, like foo:MyBean. The beans will be created
   * and initialized when the server starts.
   */
  public void addContentProgram(ConfigProgram program)
  {
    _clusterProgram.addProgram(program);
  }
  
  //
  // init
  //
  
  void initServers()
  {
    // _serverContainer.initServers(this);
    for (PodConfigBoot pod : _podList) {
      pod.initServers(this);
    }
  }

  void addServerForInit(ServerConfigBoot server)
  {
    if (! _serverList.contains(server)) {
      _serverList.add(server);
     
      // network/0406 vs baratine/80c1
      _podSelf.getServerDefault().configure(server);
    
      _root.addServerInit(server);
    }
  }
  
  //
  // queries
  //

  public ConfigProgram getProgram()
  {
    return _clusterProgram;
  }
  
  
  public ArrayList<ServerConfigBoot> getServerList()
  {
    return _serverList;
  }

  public ServerConfigBoot findServerByAddress(String address, int port)
  {
    return null;
  }

  public ServerConfigBoot createDynamicServer()
  {
    try {
      int port = 0;
      
      ServerSocketBar ss = ServerSocketJni.create(0, port);
      
      if (port <= 0) {
        port = ss.getLocalPort();
      }
      
      ServerConfigBoot server = new ServerConfigBoot(this, "", "", port);
      
      getServerDefault().configure(server);
      
      server.setDynamic(true);
      server.setEphemeral(true);
      server.setServerSocket(ss);
      
      _serverList.add(server);
      
      return server;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
    
  }

  public ServerConfigBoot createDynamicServer(int port)
  {
    if (port == 0) {
      return createDynamicServer();
    }
      
    ServerConfigBoot server = new ServerConfigBoot(this, "", "", port);
    
    server.setDynamic(true);
      
    getServerDefault().configure(server);
    
    _serverList.add(server);
      
    return server;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
