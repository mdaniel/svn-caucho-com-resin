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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.TriadOwner;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.Period;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.network.listen.AbstractProtocol;
import com.caucho.network.listen.TcpPort;
import com.caucho.server.cluster.ProtocolPort;
import com.caucho.server.cluster.ProtocolPortConfig;
import com.caucho.server.http.HttpProtocol;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * Defines a member of the cluster, corresponds to <server> in the conf file.
 *
 * A {@link ServerConnector} obtained with {@link #getServerConnector} is used to actually
 * communicate with this ClusterServer when it is active in another instance of
 * Resin .
 */
public final class ClusterServer {
  private static final L10N L = new L10N(ClusterServer.class);
  private static final Logger log
    = Logger.getLogger(ClusterServer.class.getName());

  private static final int DECODE[];

  private final NetworkClusterSystem _clusterSystem;
  private final CloudServer _cloudServer;

  // unique identifier for the server within the cluster
  private String _serverClusterId;
  // unique identifier for the server within all Resin clusters
  private String _serverDomainId;
  // the bam admin name
  private String _bamAddress;
  
  private boolean _isRemotePod;

  //
  // config parameters
  //

  private int _loadBalanceConnectionMin = 0;
  private long _loadBalanceIdleTime = 60000L;
  private long _loadBalanceBusyRecoverTime = 15000L;
  private long _loadBalanceRecoverTime = 15000L;
  private long _loadBalanceSocketTimeout = 600000L;
  private long _loadBalanceWarmupTime = 60000L;

  private long _loadBalanceConnectTimeout = 5000L;

  private int _loadBalanceWeight = 100;
  private boolean _isBackup;
  
  private long _clusterIdleTime = 3 * 60000L;
  private long _clusterSocketTimeout = 10 * 60000L;
  
  private ConfigProgram _portDefaults = new ContainerProgram();

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  private String _stage;
  private ArrayList<String> _pingUrls = new ArrayList<String>();
  
  private ArrayList<TcpPort> _listeners
    = new ArrayList<TcpPort>();

  // runtime
  
  private String _address;
  private int _port;

  private AtomicReference<SocketPool> _clusterSocketPool
    = new AtomicReference<SocketPool>();
  
  private AtomicReference<SocketPool> _loadBalanceSocketPool
    = new AtomicReference<SocketPool>();

  private final ServerHeartbeatState _heartbeatState;

  // admin

  private ClusterServerAdmin _admin = new ClusterServerAdmin(this);

  ClusterServer(NetworkClusterSystem networkService,
                CloudServer cloudServer)
  {
    _clusterSystem = networkService;
    
    if (networkService == null)
      throw new NullPointerException();
    
    _cloudServer = cloudServer;
    cloudServer.getIndex();

    
    if (_clusterSystem == null)
      throw new NullPointerException();
    
    // XXX: active isn't quite right here
    if (cloudServer.getPod() != networkService.getSelfServer().getPod()) {
      _isRemotePod = true;
      // _isHeartbeatActive.set(true);
    }
    
    _heartbeatState = new ServerHeartbeatState(this);

    _serverClusterId = getServerAddress(getIndex(), getCloudPod().getIndex());

    String clusterId = cloudServer.getCluster().getId();
    if (clusterId.equals(""))
      clusterId = "default";

    _serverDomainId = _serverClusterId + "." + clusterId.replace('.', '_');

    _bamAddress = getBamAdminName(cloudServer);
    _port = cloudServer.getPort();
    
    if (! isExternal()) {
      _address = cloudServer.getAddress();
    }
    else if (cloudServer.isSelf()) {
      _address = lookupLocalAddress();
    }
    else {
      _address = "127.0.0.2";
    }
  }
  
  public static CloudServer getSelfServer()
  {
    NetworkClusterSystem system = NetworkClusterSystem.getCurrent();
    
    if (system == null)
      throw new IllegalStateException();
    
    return system.getSelfServer();
  }
  
  public static String getBamAdminName(CloudServer server)
  {
    String serverClusterId = getServerAddress(server.getIndex(), 
                                              server.getPod().getIndex());

    String clusterId = server.getCluster().getId();
    if (clusterId.equals(""))
      clusterId = "default";

    String serverDomainId = serverClusterId + "." + clusterId.replace('.', '_');

    return serverDomainId + ".admin.resin";
  }
  
  public static ClusterServer getCurrent()
  {
    CloudServer cloudServer = NetworkClusterSystem.getCurrentSelfServer();
    
    return cloudServer.getData(ClusterServer.class);
  }

  /**
   * Gets the server identifier.
   */
  public String getId()
  {
    return _cloudServer.getId();
  }

  public String getDebugId()
  {
    if ("".equals(getId()))
      return "default";
    else
      return getId();
  }

  public CloudServer getCloudServer()
  {
    return _cloudServer;
  }
  
  /**
   * Returns the server's id within the cluster
   */
  public String getServerClusterId()
  {
    return _serverClusterId;
  }

  /**
   * Returns the server's id within all Resin clusters
   */
  public String getServerDomainId()
  {
    return _serverDomainId;
  }

  /**
   * Returns the bam name
   */
  public String getBamAdminName()
  {
    return _bamAddress;
  }

  /**
   * Returns the cluster.
   */
  public CloudCluster getCluster()
  {
    return _cloudServer.getCluster();
  }

  /**
   * Returns the owning pod
   */
  public CloudPod getCloudPod()
  {
    return _cloudServer.getPod();
  }

  /**
   * Returns true if this server is a triad.
   */
  public boolean isTriad()
  {
    return _cloudServer.isTriad();
  }

  /**
   * Returns the pod owner
   */
  public TriadOwner getTriadOwner()
  {
    return TriadOwner.getOwner(getIndex());
  }

  /**
   * Returns the server index within the pod.
   */
  public int getIndex()
  {
    return _cloudServer.getIndex();
  }

 
  /**
   * Gets the address
   */
  public String getAddress()
  {
    return _address;
  }
  
  public int getPort()
  {
    return _port;
  }
  
  public void setPort(int port)
  {
    _port = port;
  }
  
  private boolean isExternal()
  {
    return _cloudServer.isExternal();
  }
  
  public boolean isSSL()
  {
    return _cloudServer.isSSL();
  }

  /**
   * Sets true for backups
   */
  public void setBackup(boolean isBackup)
  {
    _isBackup = isBackup;

    if (isBackup)
      setLoadBalanceWeight(1);
  }
  
  public boolean isBackup()
  {
    return _isBackup;
  }

  /**
   * True for a dynamic server
   */
  public boolean isDynamic()
  {
    return ! _cloudServer.isStatic();
  }
  
  //
  // load balance configuration
  //

  /**
   * Sets the loadBalance connection time.
   */
  public void setLoadBalanceConnectTimeout(Period period)
  {
    _loadBalanceConnectTimeout = period.getPeriod();
  }

  /**
   * Gets the loadBalance connection time.
   */
  public long getLoadBalanceConnectTimeout()
  {
    return _loadBalanceConnectTimeout;
  }

  /**
   * The minimum number of load balance connections for green load balancing.
   */
  @Configurable
  public void setLoadBalanceConnectionMin(int min)
  {
    _loadBalanceConnectionMin = min;
  }

  /**
   * The minimum number of load balance connections for green load balancing.
   */
  public int getLoadBalanceConnectionMin()
  {
    return _loadBalanceConnectionMin;
  }

  /**
   * Sets the loadBalance socket time.
   */
  public void setLoadBalanceSocketTimeout(Period period)
  {
    _loadBalanceSocketTimeout = period.getPeriod();
  }

  /**
   * Gets the loadBalance socket time.
   */
  public long getLoadBalanceSocketTimeout()
  {
    return _loadBalanceSocketTimeout;
  }

  /**
   * Sets the loadBalance max-idle-time.
   */
  public void setLoadBalanceIdleTime(Period period)
  {
    _loadBalanceIdleTime = period.getPeriod();
  }

  /**
   * Sets the loadBalance idle-time.
   */
  public long getLoadBalanceIdleTime()
  {
    return _loadBalanceIdleTime;
  }

  /**
   * Sets the loadBalance fail-recover-time.
   */
  public void setLoadBalanceRecoverTime(Period period)
  {
    _loadBalanceRecoverTime = period.getPeriod();
  }

  /**
   * Gets the loadBalance fail-recover-time.
   */
  public long getLoadBalanceRecoverTime()
  {
    return _loadBalanceRecoverTime;
  }

  /**
   * Sets the loadBalance busy-recover-time.
   */
  public void setLoadBalanceBusyRecoverTime(Period period)
  {
    _loadBalanceBusyRecoverTime = period.getPeriod();
  }

  /**
   * Gets the loadBalance fail-recover-time.
   */
  public long getLoadBalanceBusyRecoverTime()
  {
    return _loadBalanceBusyRecoverTime;
  }

  /**
   * The cluster idle-time.
   */
  public long getClusterIdleTime()
  {
    return _clusterIdleTime;
  }
  
  @Configurable
  public void setClusterSocketTimeout(Period period)
  {
    _clusterSocketTimeout = period.getPeriod();
  }

  /**
   * The cluster socket-timeout
   */
  public long getClusterSocketTimeout()
  {
    return _clusterSocketTimeout;
  }

  
  //
  // port defaults
  //
  
  
  @Configurable
  public TcpPort createHttp()
    throws ConfigException
  {
    TcpPort listener = new TcpPort();
    
    applyPortDefaults(listener);

    HttpProtocol protocol = new HttpProtocol();
    listener.setProtocol(protocol);

    // getListenService().addListener(listener);

    return listener;
  }
  
  public void addHttp(TcpPort listener)
  {
    if (listener.getPort() <= 0) {
      log.fine(listener + " skipping because port is 0.");
      return;
    }
    
    _listeners.add(listener);
  }

  @Configurable
  public TcpPort createProtocol()
  {
    ProtocolPortConfig port = new ProtocolPortConfig();

    _listeners.add(port);

    return port;
  }

  @Configurable
  public TcpPort createListen()
  {
    ProtocolPortConfig listener = new ProtocolPortConfig();

    return listener;
  }
  
  public void addListen(TcpPort listener)
  {
    if (listener.getPort() <= 0) {
      log.fine(listener + " skipping because port is 0.");
      return;
    }
    
    _listeners.add(listener);
  }

  @Configurable
  public void add(ProtocolPort protocolPort)
  {
    TcpPort listener = new TcpPort();

    AbstractProtocol protocol = protocolPort.getProtocol();
    listener.setProtocol(protocol);

    applyPortDefaults(listener);

    protocolPort.getConfigProgram().configure(listener);

    _listeners.add(listener);
  }
  
  public ArrayList<TcpPort> getListeners()
  {
    return _listeners;
  }

  /**
   * Adds a port-default
   */
  @Configurable
  public void addPortDefault(ContainerProgram program)
  {
    addListenDefault(program);
  }

  /**
   * Adds a listen-default
   */
  @Configurable
  public void addListenDefault(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  private void applyPortDefaults(TcpPort port)
  {
    _portDefaults.configure(port);
    
    // port.setKeepaliveSelectEnable(isKeepaliveSelectEnable());
  }


  //
  // Configuration from <server>
  //

  /**
   * Sets the socket's listen property
   */
  public void setAcceptListenBacklog(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the minimum spare listen.
   */
  public void setAcceptThreadMin(ConfigProgram program)
    throws ConfigException
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the maximum spare listen.
   */
  public void setAcceptThreadMax(ConfigProgram program)
    throws ConfigException
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the maximum connections per port
   */
  public void setConnectionMax(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the maximum keepalive
   */
  public void setKeepaliveMax(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the keepalive timeout
   */
  public void setKeepaliveTimeout(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the keepalive connection timeout
   */
  public void setKeepaliveConnectionTimeMax(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectEnable(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectMax(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectThreadTimeout(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the suspend timeout
   */
  public void setSocketTimeout(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  /**
   * Sets the suspend timeout
   */
  public void setSuspendTimeMax(ConfigProgram program)
  {
    _portDefaults.addProgram(program);
  }

  public void setStage(String stage)
  {
    _stage = stage;
  }
  
  public String getStage()
  {
    return _stage;
  }
  
  /**
   * Adds a ping url for availability testing
   */
  public void addPingUrl(String url)
  {
    _pingUrls.add(url);
  }

  /**
   * Returns the ping url list
   */
  public ArrayList<String> getPingUrlList()
  {
    return _pingUrls;
  }

  /**
   * Sets the loadBalance warmup time
   */
  public void setLoadBalanceWarmupTime(Period period)
  {
    _loadBalanceWarmupTime = period.getPeriod();
  }

  /**
   * Gets the loadBalance warmup time
   */
  public long getLoadBalanceWarmupTime()
  {
    return _loadBalanceWarmupTime;
  }

  /**
   * Sets the loadBalance weight
   */
  public void setLoadBalanceWeight(int weight)
  {
    _loadBalanceWeight = weight;
  }

  /**
   * Gets the loadBalance weight
   */
  public int getLoadBalanceWeight()
  {
    return _loadBalanceWeight;
  }

  /**
   * Gets the ip.
   */
  public String getIp()
  {
    return getCloudServer().getIpAddress();
  }

  /**
   * Returns true for the self server
   */
  public boolean isSelf()
  {
    return getCloudServer().isSelf();
  }
  
  public boolean isRemote()
  {
    return ! isSelf();
  }
  
  /**
   * Looks up the local address when given an external address, e.g. for
   * cloud systems that dynamically allocate local addresses.
   */
  private String lookupLocalAddress()
  {
    long timeout = 120 * 1000L;
    
    long expireTime = CurrentTime.getCurrentTime() + timeout;
    
    String address;
    
    while ((address = allocateLocalAddress()) == null
           && CurrentTime.getCurrentTime() < expireTime) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    }
    
    if (address == null) {
      throw new ConfigException(L.l("Cannot find an internal local IP address for server {0}, external IP {1} within 120s."
                                    + " 'external-address=true' is used for cloud networks where the internal address is allocated dynamically."
                                    + " Check that the persistent address has been assigned in the cloud configuration or DNS.",
                                    _cloudServer.getId(),
                                    _cloudServer.getAddress()));
    }
    
    return address;
  }
  
  private String allocateLocalAddress()
  {
    ArrayList<String> addressNames = new ArrayList<String>();

    for (InetAddress addr : NetworkClusterSystem.getLocalAddresses()) {
      String localAddress = getLocalAddress(addr);
      
      if (localAddress != null)
        addressNames.add(localAddress);
    }
      
    Collections.sort(addressNames);
    
    String address = null;
    
    if (addressNames.size() > 0)
      address = addressNames.get(0);

    return address;
  }
  
  private String getLocalAddress(InetAddress addr)
  {
    String address = addr.getHostAddress();
    
    byte []bytes = addr.getAddress();
    
    if (address.equals(_cloudServer.getAddress())) {
      // the external address cannot be the local address
      return null;
    }
    
    if (bytes[0] == 127) {
      // loopback isn't valid
      return null;
    }
    
    if (isLocal(bytes))
      return address;
    else
      return null;
  }
  
  private boolean isLocal(byte []bytes)
  {
    if (bytes.length != 4) {
      return false;
    }
    else if (bytes[0] == 10) {
      return true;
    }
    else if ((bytes[0] & 0xff) == 192 && (bytes[1] & 0xff) == 168) {
      return true;
    }
    else if ((bytes[0] & 0xff) == 172 && (bytes[1] & 0xf0) == 0x10) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Returns the socket pool as a pod cluster connector.
   */
  public final ClientSocketFactory getClusterSocketPool()
  {
    return getClusterSocketFactory();
  }

  /**
   * Returns the socket pool as a load-balancer.
   */
  public final ClientSocketFactory getLoadBalanceSocketPool()
  {
    ClientSocketFactory factory = getLoadBalanceSocketFactory();
    
    if (factory == null)
      return null;
    
    if (_cloudServer.getState().isDisableSoft()) {
      factory.enableSessionOnly();
    }
    else if (_cloudServer.getState().isDisabled()){
      // server/269g
      factory.disable();
    }
    else {
      factory.enable();
    }
    
    return factory;
  }
  
  private ClientSocketFactory getClusterSocketFactory()
  {
    SocketPool socketPool = _clusterSocketPool.get();
    
    if (socketPool != null) {
      return socketPool.getFactory();
    }
    
    if (! isExternal()) {
      return null;
    }
    
    NetworkAddressResult result = _clusterSystem.getLocalSocketAddress(this);

    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " getLocalSocketAddress -> " + result);
    }
    
    if (result == null)
      return null;
    
    ClientSocketFactory factory
      = createClusterPool(_clusterSystem.getServerId(), 
                          result.getAddress(), result.getPort());
    
    factory.init();
    factory.start();
    
    socketPool = new SocketPool(factory);
    
    if (! _clusterSocketPool.compareAndSet(null, socketPool)) {
      factory.stop();
      
      socketPool = _clusterSocketPool.get();
    }
    
    if (socketPool != null)
      return socketPool.getFactory();
    else
      return null;
  }
  
  private ClientSocketFactory getLoadBalanceSocketFactory()
  {
    SocketPool socketPool = _loadBalanceSocketPool.get();
    
    if (socketPool != null)
      return socketPool.getFactory();
    
    if (! isExternal())
      return null;
    
    return null;
  }
  
  
  /**
   * Returns true if the server is remote and active.
   */
  public final boolean isActiveRemote()
  {
    ClientSocketFactory pool = getClusterSocketPool();

    return pool != null && pool.isActive();
  }
  
  /**
   * Adds a program.
   */
  @Configurable
  public void addContentProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }

  /**
   * Returns the configuration program for the Server.
   */
  public ConfigProgram getServerProgram()
  {
    return _serverProgram;
  }
  
  /**
   * Returns the port defaults for the Server
   */
  public ConfigProgram getPortDefaults()
  {
    return _portDefaults;
  }

  /**
   * Initialize
   */
  public void init()
  {
    /*
    if (! _isClusterPortConfig)
      applyPortDefaults(_clusterPort);

    _clusterPort.init();
    */

    if (! isSelf()
        && getCloudServer().getPort() >= 0
        && ! isExternal()) {
      ClientSocketFactory clusterFactory
      = createClusterPool(_clusterSystem.getServerId(), getAddress(), getPort());
      clusterFactory.init();
      
      _clusterSocketPool.set(new SocketPool(clusterFactory));
      
      ClientSocketFactory loadBalanceFactory
        = createLoadBalancePool(_clusterSystem.getServerId());
      loadBalanceFactory.init();
      
      _loadBalanceSocketPool.set(new SocketPool(loadBalanceFactory));
    }

    _admin.register();
  }

  private ClientSocketFactory createLoadBalancePool(String serverId)
  {
    ClientSocketFactory pool = new ClientSocketFactory(serverId,
                                                       getId(),
                                                       "Resin|LoadBalanceSocket",
                                                       getStatId(),
                                                       getAddress(),
                                                       getPort(),
                                                       isSSL());

    pool.setLoadBalanceConnectTimeout(getLoadBalanceConnectTimeout());
    pool.setLoadBalanceConnectionMin(getLoadBalanceConnectionMin());
    pool.setLoadBalanceSocketTimeout(getLoadBalanceSocketTimeout());
    pool.setLoadBalanceIdleTime(getLoadBalanceIdleTime());
    pool.setLoadBalanceRecoverTime(getLoadBalanceRecoverTime());
    pool.setLoadBalanceBusyRecoverTime(getLoadBalanceBusyRecoverTime());
    pool.setLoadBalanceWarmupTime(getLoadBalanceWarmupTime());
    pool.setLoadBalanceWeight(getLoadBalanceWeight());
    
    return pool;
  }

  private ClientSocketFactory createClusterPool(String serverId,
                                                String address,
                                                int port)
  {
    if (port <= 0)
      port = getPort();
    
    ClientSocketFactory pool = new ClientSocketFactory(serverId,
                                                       getId(),
                                                       "Resin|ClusterSocket",
                                                       getStatId(),
                                                       address,
                                                       port,
                                                       isSSL());
    
    pool.setLoadBalanceSocketTimeout(getClusterIdleTime());
    pool.setLoadBalanceIdleTime(getClusterIdleTime());
    
    if (getCloudServer().getPod() == _clusterSystem.getSelfServer().getPod())
      pool.setHeartbeatServer(true);
    
    return pool;
  }

  private String getStatId()
  {
    String targetCluster = getCluster().getId();

    if ("".equals(targetCluster))
      targetCluster = "default";

    int index = getIndex();

    return String.format("%02x:%s", index, targetCluster);
  }

  public boolean isRemotePod()
  {
    return _isRemotePod;
  }
  
  /**
   * Test if the server is active, i.e. has received an active message.
   */
  public boolean isHeartbeatActive()
  {
    return _heartbeatState.isHeartbeatActive();
  }

  /**
   * Returns the last state change timestamp.
   */
  public long getStateTimestamp()
  {
    return _heartbeatState.getStateTimestamp();
  }
  
  public long getLastHeartbeatTime()
  {
    return _heartbeatState.getLastHeartbeatTime();
  }
  
  public String getHeartbeatState()
  {
    return _heartbeatState.getHeartbeatState();
  }

  /**
   * Notify that a start event has been received.
   */
  public boolean notifyHeartbeatStart()
  {
    if (! _heartbeatState.notifyHeartbeatStart()) {
      return false;
    }
    
    _cloudServer.onHeartbeatStart();

    ClientSocketFactory clusterSocketPool = getClusterSocketPool();
    if (clusterSocketPool != null) {
      clusterSocketPool.notifyHeartbeatStart();
    }

    _clusterSystem.notifyHeartbeatStart(this);

    return true;
  }

  /**
   * Notify that a stop event has been received.
   */
  public boolean notifyHeartbeatStop()
  {
    SocketPool clusterSocketPool;
    
    if (isExternal())
      clusterSocketPool = _clusterSocketPool.getAndSet(null);
    else
      clusterSocketPool = _clusterSocketPool.get();

    if (clusterSocketPool != null)
      clusterSocketPool.getFactory().notifyHeartbeatStop();

    if (! _heartbeatState.notifyHeartbeatStop()) {
      return false;
    }
    
    if (_clusterSystem.isActive())
      log.warning(this + " notify-heartbeat-stop");
    else
      log.fine(this + " notify-heartbeat-stop");
    
    _cloudServer.onHeartbeatStop();

    _clusterSystem.notifyHeartbeatStop(this);

    return true;
  }
  
  public void updateHeartbeatTimeout(long timeout)
  {
    _heartbeatState.updateTimeout(timeout);
  }
  
  public void onHeartbeatTimeout()
  {
    if (_clusterSystem.isActive())
      log.warning(this + " notify-heartbeat-timeout (check peer for possible freeze)");
    else
      log.fine(this + " notify-heartbeat-timeout (check peer for possible freeze)");
    
    _cloudServer.onHeartbeatStop();
    _clusterSystem.notifyHeartbeatStop(this);
  }

  /**
   * Starts the server.
   */
  public void stopServer()
  {
    _heartbeatState.notifyHeartbeatStop();

    SocketPool pool = _clusterSocketPool.get();
    
    if (pool != null) {
      pool.getFactory().notifyHeartbeatStop();
    }
  }

  /**
   * Adds the primary/backup/third digits to the id.
   */
  public void generateIdPrefix(StringBuilder cb)
  {
    cb.append(getServerClusterId());
  }

  //
  // admin
  //

  /**
   * Returns the admin object
   */
  public ClusterServerMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Close any ports.
   */
  public void close()
  {
    SocketPool loadBalancePool = _loadBalanceSocketPool.get();
    
    if (loadBalancePool != null)
      loadBalancePool.getFactory().close();
    
    SocketPool clusterPool = _clusterSocketPool.get();
    
    if (clusterPool != null)
      clusterPool.getFactory().close();
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[id=" + getId()
            + "," + getAddress() + ":" + getPort() + "]");
  }
  
  public static String getServerAddress(int index, int podIndex)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(convert(index));
    sb.append(convert(podIndex));
    sb.append(convert(podIndex / 64));
    
    return sb.toString();
  }

  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }
  
  static class SocketPool {
    private final ClientSocketFactory _factory;
    
    SocketPool(ClientSocketFactory factory)
    {
      _factory = factory;
    }
    
    ClientSocketFactory getFactory()
    {
      return _factory;
    }
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
