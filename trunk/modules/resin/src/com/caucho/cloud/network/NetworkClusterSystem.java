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

package com.caucho.cloud.network;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.AbstractCloudClusterListener;
import com.caucho.cloud.topology.AbstractCloudPodListener;
import com.caucho.cloud.topology.AbstractCloudServerListener;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.env.service.AbstractResinSubSystem;
import com.caucho.env.service.ResinSystem;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.SocketPollService;
import com.caucho.network.listen.TcpPort;
import com.caucho.server.hmux.HmuxProtocol;
import com.caucho.util.HostUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.net.NetworkSystem;

/**
 * NetworkClusterService manages the cluster network code, the communication
 * between Resin servers in a cluster. 
 */
public class NetworkClusterSystem extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = SocketPollService.START_PRIORITY + 1;

  private static final L10N L = new L10N(NetworkClusterSystem.class);
  private static final Logger log = 
    Logger.getLogger(NetworkClusterSystem.class.getName());
  
  private static final long CLUSTER_IDLE_TIME_MAX = Long.MAX_VALUE / 2;
  private static final long CLUSTER_IDLE_PADDING = 5 * 60000;
  
  private final CloudServer _selfServer;
  
  private TcpPort _clusterListener;
  private HmuxProtocol _clusterProtocol;
  
  private CopyOnWriteArrayList<ClusterServerListener> _serverListeners
    = new CopyOnWriteArrayList<ClusterServerListener>();

  private CopyOnWriteArrayList<ClusterLinkListener> _linkListeners
    = new CopyOnWriteArrayList<ClusterLinkListener>();

  public NetworkClusterSystem(CloudServer selfServer)
  {
    _selfServer = selfServer;
    _selfServer.setSelf(true);
    
    _selfServer.getSystem().addClusterListener(new NetworkClusterListener());
    
    ClusterServer clusterServer = selfServer.getData(ClusterServer.class);
    
    NetworkSystem.createSubSystem(selfServer.getId());
    
    if (clusterServer.getPort() >= 0) {
      _clusterListener = new ClusterTcpPort(clusterServer);
      _clusterProtocol = HmuxProtocol.create(); 
     }
    
  }

  /**
   * Creates a new network cluster service.
   */
  /*
  public static NetworkClusterSystem
  createAndAddService(CloudServer selfServer)
  {
    ResinSystem system = preCreate(NetworkClusterSystem.class);

    NetworkClusterSystem service = new NetworkClusterSystem(selfServer);
    system.addService(NetworkClusterSystem.class, service);

    return service;
  }
  */
  public static void
  createAndAddService(NetworkClusterSystem clusterSystem)
  {
    ResinSystem resinSystem = preCreate(NetworkClusterSystem.class);

    resinSystem.addService(NetworkClusterSystem.class, clusterSystem);
  }

  /**
   * Returns the current network service.
   */
  public static NetworkClusterSystem getCurrent()
  {
    return ResinSystem.getCurrentService(NetworkClusterSystem.class);
  }
  
  /**
   * Returns the current network service.
   */
  public static CloudServer getCurrentSelfServer()
  {
    NetworkClusterSystem clusterService = getCurrent();
    
    if (clusterService == null)
      throw new IllegalStateException(L.l("{0} is not available in this context",
                                          NetworkClusterSystem.class.getSimpleName()));
    
    return clusterService.getSelfServer();
  }
  
  /**
   * Returns the self server for the network.
   */
  public CloudServer getSelfServer()
  {
    return _selfServer;
  }
  
  /**
   * Returns the active server id.
   */
  public String getServerId()
  {
    return getSelfServer().getId();
  }
  
  /**
   * Returns the cluster port.
   */
  public TcpPort getClusterListener()
  {
    return _clusterListener;
  }

  //
  // listeners
  //

  public void addServerListener(ClusterServerListener listener)
  {
    _serverListeners.add(listener);

    CloudServer []serverList = _selfServer.getPod().getServerList();
    int serverLength = _selfServer.getPod().getServerLength();
    
    for (int i = 0; i < serverLength; i++) {
      CloudServer cloudServer = serverList[i];

      if (cloudServer != null) {
        ClusterServer server = cloudServer.getData(ClusterServer.class);
      
        if (server.isHeartbeatActive())
          listener.serverStart(server);
      }
    }
  }

  public void removeServerListener(ServerListener listener)
  {
    _serverListeners.remove(listener);
  }

  protected void notifyHeartbeatStart(ClusterServer server)
  {
    for (ClusterServerListener listener : _serverListeners) {
      listener.serverStart(server);
    }
  }

  protected void notifyHeartbeatStop(ClusterServer server)
  {
    for (ClusterServerListener listener : _serverListeners) {
      listener.serverStop(server);
    }
  }

  public void addLinkListener(ClusterLinkListener listener)
  {
    _linkListeners.add(listener);
  }
  
  public void notifyLinkClose(Object payload)
  {
    for (ClusterLinkListener listener : _linkListeners) {
      listener.onLinkClose(payload);
    }
  }
  
  public void addClusterExtensionProtocol(int id, Protocol protocol)
  {
    _clusterProtocol.putExtension(id, protocol);
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  @Override
  public void start()
    throws Exception
  {
    super.start();
    
    startClusterListener();
    
    ClusterServer selfServer = _selfServer.getData(ClusterServer.class);
    
    selfServer.notifyHeartbeatStart();
    
    validateTriad(_selfServer.getPod());
  }
  
  private void validateTriad(CloudPod pod)
  {
    CloudServer []servers = pod.getServerList();
    
    if (servers.length == 0) {
      return;
    }
    
    String address = servers[0].getAddress();
    boolean isMultipleAddress = false;
    
    for (int i = 0; i < servers.length; i++) {
      if (servers[i] == null) {
        continue;
      }
      
      if (! address.equals(servers[i].getAddress())) {
        isMultipleAddress = true;
      }
    }
    
    if (! isMultipleAddress) {
      return;
    }
    
    int triadMax = Math.min(servers.length, 3);
    
    for (int i = 0; i < triadMax; i++) {
      for (int j = i + 1; j < triadMax; j++) {
        CloudServer serverA = servers[i];
        CloudServer serverB = servers[j];
        
        if (serverA == null || serverB == null) {
          continue;
        }
        
        if (serverA.getAddress().equals(serverB.getAddress())) {
          log.warning(L.l("Triad servers should be on separate machines for better reliability.\n{0}\n{1}",
                          serverA, serverB));
          return;
        }
      }
    }
  }

  /**
   * Closes the server.
   */
  @Override
  public void stop()
    throws Exception
  {
    super.stop();
    
    try {
      if (_clusterListener != null) {
        _clusterListener.close();
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the local ip address for a server configured with
   * external-address.
   */
  public NetworkAddressResult getLocalSocketAddress(ClusterServer clusterServer)
  {
    return null;
  }

  /**
   * Configures the server.
   */
  private void configureServer(CloudServer cloudServer)
  {
    ClusterServer server = new ClusterServer(this, cloudServer);
    
    configServer(server, cloudServer);

    server.init();
  }
  
  public static void configServer(Object server, CloudServer cloudServer)
  {
    String oldServerId = (String) Config.getCurrentVar("rvar0");
    String oldClusterId = (String) Config.getCurrentVar("rvar1");
    
    try {
      Config.setProperty("rvar0", cloudServer.getDisplayId());
      Config.setProperty("rvar1", cloudServer.getCluster().getId());
      
      CloudCluster cluster = cloudServer.getCluster();
      CloudPod pod = cloudServer.getPod();

      ClusterServerProgram clusterServerProgram
      = cluster.getData(ClusterServerProgram.class);

      ClusterServerProgram podServerProgram
      = pod.getData(ClusterServerProgram.class);

      ClusterServerProgram serverProgram
      = cloudServer.getData(ClusterServerProgram.class);

      if (clusterServerProgram != null)
        clusterServerProgram.getProgram().configure(server);

      if (podServerProgram != null)
        podServerProgram.getProgram().configure(server);

      if (serverProgram != null)
        serverProgram.getProgram().configure(server);

      if (cloudServer.putDataIfAbsent(server) != null) {
        throw new IllegalStateException(L.l("{0} cannot be configured twice.",
                                            server));
      }
    } finally {
      Config.setProperty("rvar0", oldServerId);
      Config.setProperty("rvar1", oldClusterId);
    }
  }
 
  /**
   * Start the cluster port
   */
  private void startClusterListener()
    throws Exception
  {
    TcpPort listener = _clusterListener;
    
    if (listener != null) {
      ClusterServer clusterServer = _selfServer.getData(ClusterServer.class);
      
      long idleTime = clusterServer.getClusterIdleTime() + CLUSTER_IDLE_PADDING;
      
      listener.setKeepaliveConnectionTimeMaxMillis(CLUSTER_IDLE_TIME_MAX);
      listener.setKeepaliveTimeoutMillis(idleTime);
      listener.setSocketTimeoutMillis(clusterServer.getClusterSocketTimeout());
      
      listener.setProtocol(_clusterProtocol);
      listener.init();
      
      validateClusterServer(listener, clusterServer);
      
      log.info("");
      listener.bind();
      listener.start();
      log.info("");
      
      if (clusterServer.getPort() == 0) {
        clusterServer.setPort(listener.getLocalPort());
      }
    }
  }

  private void validateClusterServer(TcpPort listener,
                                     ClusterServer server)
  {
    if (listener == null || server == null)
      return;
    

    if (listener.getSocketTimeout() <= server.getLoadBalanceIdleTime()) {
      throw new ConfigException(L.l("{0}: load-balance-idle-time {1} must be less than socket-timeout {2}",
                                    server, 
                                    server.getLoadBalanceIdleTime(),
                                    listener.getSocketTimeout()));
    }

    // server/26r0
    /*
    if (server.getLoadBalanceSocketTimeout() <= listener.getSocketTimeout()) {
      throw new ConfigException(L.l("{0}: load-balance-socket-timeout {1} must be greater than socket-timeout {2}",
                                    server, 
                                    server.getLoadBalanceSocketTimeout(),
                                    listener.getSocketTimeout()));
    }
    */

    if (listener.getKeepaliveTimeout() <= server.getLoadBalanceIdleTime()) {
      throw new ConfigException(L.l("{0}: load-balance-idle-time {1} must be less than keepalive-timeout {2}",
                                    server, 
                                    server.getLoadBalanceIdleTime(),
                                    listener.getKeepaliveTimeout()));
    }
  }
  
  public static ArrayList<InetAddress> getLocalAddresses()
  {
    return HostUtil.getLocalAddresses();
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _clusterListener + "]");
  }
  
  private class NetworkClusterListener extends AbstractCloudClusterListener {
    @Override
    public void onClusterAdd(CloudCluster cluster)
    {
      cluster.addPodListener(new PodListener());
    }
    
  }
  
  private class PodListener extends AbstractCloudPodListener {
    @Override
    public void onPodAdd(CloudPod pod)
    {
      pod.addServerListener(new ServerListener());
    }
    
  }
  
  private class ServerListener extends AbstractCloudServerListener {
    @Override
    public void onServerAdd(CloudServer server)
    {
      configureServer(server);
    }
  }
}
