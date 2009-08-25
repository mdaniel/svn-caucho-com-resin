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

package com.caucho.server.cluster;

import com.caucho.config.Configurable;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.Period;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.server.http.HttpProtocol;
import com.caucho.server.port.*;
import com.caucho.server.resin.*;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.QServerSocket;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private static final long DEFAULT = 0xcafebabe;
  private static final int DECODE[];

  private final Cluster _cluster;
  private final ClusterPod _pod;
  private final int _index;
  
  private String _id = "";

  private Machine _machine;

  private boolean _isDynamic;

  // unique identifier for the server within the cluster
  private String _serverClusterId;
  // unique identifier for the server within all Resin clusters
  private String _serverDomainId;
  // the bam admin name
  private String _bamJid;
  
  private ClusterPort _clusterPort;
  private boolean _isClusterPortConfig;
  
  //
  // config parameters
  //

  private long _socketTimeout = 90000L;
  private long _keepaliveTimeout = 75000L;
  
  private int _loadBalanceConnectionMin = 0;
  private long _loadBalanceIdleTime = DEFAULT;
  private long _loadBalanceRecoverTime = 15000L;
  private long _loadBalanceSocketTimeout = DEFAULT;
  private long _loadBalanceWarmupTime = 60000L;
  
  private long _loadBalanceConnectTimeout = 5000L;
  
  private int _loadBalanceWeight = 100;

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  private ArrayList<ConfigProgram> _portDefaults
    = new ArrayList<ConfigProgram>();
  private ArrayList<Port> _ports = new ArrayList<Port>();

  private boolean _isSelf;

  // runtime

  private ServerPool _serverPool;

  private boolean _isActive;
  private long _stateTimestamp;

  // admin

  private ClusterServerAdmin _admin = new ClusterServerAdmin(this);

  public ClusterServer(ClusterPod pod, int index)
  {
    _pod = pod;
    _cluster = pod.getCluster();
    _index = index;

    _clusterPort = new ClusterPort(this);
    _ports.add(_clusterPort);
    
    StringBuilder sb = new StringBuilder();

    sb.append(convert(getIndex()));
    sb.append(convert(getClusterPod().getIndex()));
    sb.append(convert(getClusterPod().getIndex() / 64));

    _serverClusterId = sb.toString();
      
    String clusterId = _cluster.getId();
    if (clusterId.equals(""))
      clusterId = "default";
      
    _serverDomainId = _serverClusterId + "." + clusterId.replace('.', '_');

    _bamJid = _serverDomainId + ".admin.resin";
  }

  /**
   * Gets the server identifier.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the server identifier.
   */
  public void setId(String id)
  {
    _id = id;
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
    return _bamJid;
  }

  /**
   * Returns the cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the machine.
   */
  public Machine getMachine()
  {
    return _machine;
  }

  /**
   * Returns the machine.
   */
  protected void setMachine(Machine machine)
  {
    _machine = machine;
  }

  /**
   * Returns the owning pod
   */
  public ClusterPod getClusterPod()
  {
    return _pod;
  }

  /**
   * Returns true if this server is a triad.
   */
  public boolean isTriad()
  {
    ClusterPod pod = getClusterPod();

    return pod.isTriad(this);
  }

  /**
   * Returns the pod owner
   */
  public ClusterPod.Owner getTriadOwner()
  {
    return getClusterPod().getOwner(getIndex());
  }

  /**
   * Returns the server index.
   */
  /*
  void setIndex(int index)
  {
    _index = index;
  }
  */

  /**
   * Returns the server index within the pod.
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Sets the address
   */
  public void setAddress(String address)
    throws UnknownHostException
  {
    _clusterPort.setAddress(address);
  }

  /**
   * Gets the address
   */
  public String getAddress()
  {
    return _clusterPort.getAddress();
  }

  /**
   * Sets true for backups
   */
  public void setBackup(boolean isBackup)
  {
    if (isBackup)
      setLoadBalanceWeight(1);
  }

  /**
   * True for a dynamic server
   */
  public void setDynamic(boolean isDynamic)
  {
    _isDynamic = isDynamic;
  }

  /**
   * True for a dynamic server
   */
  public boolean isDynamic()
  {
    return _isDynamic;
  }

  /**
   * Sets the keepalive timeout.
   */
  public void setKeepaliveTimeout(Period timeout)
  {
    _keepaliveTimeout = timeout.getPeriod();
  }

  /**
   * Gets the keepalive timeout.
   */
  public long getKeepaliveTimeout()
  {
    return _keepaliveTimeout;
  }

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
    if (_loadBalanceSocketTimeout != DEFAULT)
      return _loadBalanceSocketTimeout;
    else
      return _socketTimeout + 600000L;
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
    if (_loadBalanceIdleTime != DEFAULT)
      return _loadBalanceIdleTime;
    else if (_keepaliveTimeout < 15000L)
      return _keepaliveTimeout - 5000L;
    else
      return _keepaliveTimeout - 10000L;
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
   * Sets the loadBalance read/write timeout
   */
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Gets the loadBalance read/write timeout
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
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
   * Arguments on boot
   */
  public void addJavaExe(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmClasspath(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPassword(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPort(int port)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogAddress(String addr)
  {
  }

  /**
   * Sets a port.
   */
  public void setPort(int port)
  {
    _clusterPort.setPort(port);
  }

  /**
   * Gets the port.
   */
  public int getPort()
  {
    return _clusterPort.getPort();
  }

  /**
   * Adds a port default
   */
  public void addPortDefault(ContainerProgram program)
  {
    _portDefaults.add(program);
  }

  /**
   * Adds a http.
   */
  public Port createHttp()
    throws ConfigException
  {
    Port port = new Port(this);

    HttpProtocol protocol = new HttpProtocol();
    protocol.setParent(port);
    port.setProtocol(protocol);

    addProtocolPort(port);

    applyPortDefaults(port);
    
    return port;
  }

  public Port createProtocol()
  {
    ProtocolPortConfig port = new ProtocolPortConfig(this);

    _ports.add(port);

    return port;
  }

  void addProtocolPort(Port port)
  {
    _ports.add(port);
  }

  public void add(ProtocolPort protocolPort)
  {
    Port port = new Port(this);

    Protocol protocol = protocolPort.getProtocol();
    protocol.setParent(port);
    port.setProtocol(protocol);

    applyPortDefaults(port);

    protocolPort.getConfigProgram().configure(port);
    
    addProtocolPort(port);
  }

  private void applyPortDefaults(Port port)
  {
    for (ConfigProgram program : _portDefaults) {
      program.configure(port);
    }
  }

  /**
   * Pre-binding of ports.
   */
  public void bind(String address, int port, QServerSocket ss)
    throws Exception
  {
    if ("null".equals(address))
      address = null;

    for (int i = 0; i < _ports.size(); i++) {
      Port serverPort = _ports.get(i);

      if (port != serverPort.getPort())
	continue;

      if ((address == null) != (serverPort.getAddress() == null))
	continue;
      else if (address == null || address.equals(serverPort.getAddress())) {
	serverPort.bind(ss);

	return;
      }
    }

    throw new IllegalStateException(L.l("No matching port for {0}:{1}",
					address, port));
  }

  /**
   * Sets the user name.
   */
  public void setUserName(String userName)
  {
  }

  /**
   * Sets the group name.
   */
  public void setGroupName(String groupName)
  {
  }

  /**
   * Returns the ports.
   */
  public ArrayList<Port> getPorts()
  {
    return _ports;
  }

  /**
   * Sets the ClusterPort.
   */
  public ClusterPort createClusterPort()
  {
    applyPortDefaults(_clusterPort);

    _isClusterPortConfig = true;
    
    return _clusterPort;
  }

  /**
   * Sets the ClusterPort.
   */
  public ClusterPort getClusterPort()
  {
    return _clusterPort;
  }

  /**
   * Returns true for the self server
   */
  public boolean isSelf()
  {
    return _isSelf;
  }

  /**
   * Returns true for secure.
   */
  public boolean isSSL()
  {
    return getClusterPort().isSSL();
  }

  /**
   * Returns the server connector.
   */
  public final ServerPool getServerPool()
  {
    return _serverPool;
  }

  /**
   * Returns true if the server is remote and active.
   */
  public final boolean isActiveRemote()
  {
    ServerPool pool = _serverPool;

    return pool != null && pool.isActive();
  }

  /**
   * Adds a program.
   */
  public void addContentProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }

  /**
   * Adds a program.
   */
  public ConfigProgram getServerProgram()
  {
    return _serverProgram;
  }
  
  /**
   * Initialize
   */
  public void init()
  {
    if (! _isClusterPortConfig)
      applyPortDefaults(_clusterPort);
    
    _clusterPort.init();

    if (! getId().equals(Resin.getCurrent().getServerId())) {
      _serverPool = new ServerPool(Resin.getCurrent().getServerId(), this);
      _serverPool.init();
    }

    _admin.register();
  }

  /**
   * Test if the server is active, i.e. has received an active message.
   */
  public boolean isActive()
  {
    return _isActive;
  }

  /**
   * Returns the last state change timestamp.
   */
  public long getStateTimestamp()
  {
    return _stateTimestamp;
  }

  /**
   * Notify that a start event has been received.
   */
  public void notifyStart(long timestamp)
  {
    if (_serverPool != null)
      _serverPool.notifyStart();

    synchronized (this) {
      if (timestamp < _stateTimestamp)
        return;

      if (log.isLoggable(Level.FINER) && ! _isActive)
        log.finer(this + " notify-start");
    
      _isActive = true;
      _stateTimestamp = timestamp;
    }

    Server server = Server.getCurrent();

    if (server != null)
      server.notifyServerStart(this);
  }

  /**
   * Notify that a start event has been received.
   */
  public void notifyStop(long timestamp)
  {
    if (_serverPool != null)
      _serverPool.notifyStop();

    synchronized (this) {
      if (timestamp < _stateTimestamp)
        return;

      if (log.isLoggable(Level.FINER) && _isActive)
        log.finer(this + " notify-stop");
    
      _isActive = false;
      _stateTimestamp = timestamp;
    }

    Server server = Server.getCurrent();

    if (server != null)
      server.notifyServerStop(this);
  }

  /**
   * Starts the server.
   */
  public Server startServer()
    throws StartLifecycleException
  {
    _isSelf = true;
    _isActive = true;
    _stateTimestamp = Alarm.getCurrentTime();
    
    return _cluster.startServer(this);
  }

  /**
   * Starts the server.
   */
  public void stopServer()
  {
    _isActive = false;
    _stateTimestamp = Alarm.getCurrentTime();

    if (_serverPool != null)
      _serverPool.notifyStop();
  }

  /**
   * Generate the primary, secondary, tertiary, returning the value encoded
   * in a long.
   */
  /*
  public long generateBackupCode()
  {
    return _cluster.generateBackupCode(_index);
  }
  */

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
    if (_serverPool != null)
      _serverPool.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + getId() + "]";
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
  
  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
