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

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.network.port.PortTcp;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ServerSocketBar;

/**
 * Configuration for a server instance.
 */
public class ServerConfigBoot
{
  private String _id;
  
  private String _displayName;
  
  private String _address = "";
  private int _port = -1;
  private int _portBartender = -1;

  private boolean _isSSL;
  
  private ClusterConfigBoot _cluster;
  
  private ContainerProgram _program = new ContainerProgram();
  private ContainerProgram _programPort = new ContainerProgram();
  
  private boolean _isRequireExplicitId;
  private String _clusterSystemKey;
  private int _watchdogPort;
  
  private boolean _isDynamic;
  private boolean _isEphemeral;

  private ServerSocketBar _ss;

  private ServerSocketBar _ssBartender;

  private boolean _isRemoveDataOnStart;

  private long _clusterPortTimeout;

  ServerConfigBoot(ClusterConfigBoot cluster,
                   String id,
                   String address,
                   int port)
  {
    _cluster = cluster;
    
    _id = id;
    _address = address;
    _port = port;
    
    if (! "".equals(id)) {
      _displayName = id;
    }
    else if (port >= 0) {
      _displayName = cluster.getId() + '-' + port;
      //_displayName = address + ':' + port;
    }
    else {
      _displayName = cluster.getId() + "-embed";
    }
  }
  
  /**
   * id: display name for the server  
   */
  @Configurable
  public void setId(String id)
  {
    if (id == null || "".equals(id)) {
      id = "default";
    }
    
    _displayName = id;
  }

  public String getId()
  {
    return _id;
  }

  public String getDisplayName()
  {
    if (_displayName != null) {
      return _displayName;
    }
    else if (_port >= 0) {
      return _cluster.getId() + '-' + _port;
    }
    else {
      return _cluster.getId() + "-embed";
    }
  }

  public boolean matchId(String serverId)
  {
    return getDisplayName().equals(serverId);
  }
  
  public ClusterConfigBoot getCluster()
  {
    return _cluster;
  }

  public RootConfigBoot getRoot()
  {
    return getCluster().getRoot();
  }

  public PathImpl getRootDirectory(ArgsDaemon args)
  {
    return getRoot().getRootDirectory(args);
  }

  /*
  public Path getLogDirectory(ArgsDaemon args)
  {
    return getRoot().getLogDirectory(args);
  }
  */

  /**
   * address: configured IP address for a server
   */
  @ConfigArg(0)
  public void setAddress(String address)
  {
    int p = address.lastIndexOf(':');
    
    if (p > 0) {
      int port = Integer.parseInt(address.substring(p + 1));
      address = address.substring(0, p);
      
      setPort(port);
    }
    
    if (address == null || "*".equals(address)) {
      address = "";
    }
    
    _address = address;
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  /**
   * cluster-system-key: internal key to identify cluster servers
   */
  @Configurable
  public void setClusterSystemKey(String key)
  {
    _clusterSystemKey = key;
  }

  public String getClusterSystemKey()
  {
    return _clusterSystemKey;
  }

  /**
   * port: the public HTTP/JAMP port. 
   */
  @Configurable
  public void setPort(int port)
  {
    _port = port;
  }
  
  public int getPort()
  {
    return _port;
  }

  public boolean isSSL()
  {
    return _isSSL;
  }

  /**
   * bartender-port: the bartender system port. 
   */
  @Configurable
  public void setBartenderPort(int port)
  {
    _portBartender = port;
  }
  
  public int getPortBartender()
  {
    return _portBartender;
  }
  
  public String getFullAddress()
  {
    return getAddress() + ":" + getPort();
  }

  /**
   * require-explicit-id: used for QA; disables CLI default server search
   */
  @Configurable
  public void setRequireExplicitId(boolean isExplicit)
  {
    _isRequireExplicitId = isExplicit;
  }

  public boolean isRequireExplicitId()
  {
    return _isRequireExplicitId;
  }

  public String getWatchdogAddress(ArgsDaemon args)
  {
    return "127.0.0.1";
  }
  
  /**
   * watchdog-port: TCP port for the watchdog 
   */
  @Configurable
  public void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }

  public int getWatchdogPort(ArgsDaemon args)
  {
    if (args.getWatchdogPort() > 0) {
      return args.getWatchdogPort();
    }
    else if (_watchdogPort > 0) {
      return _watchdogPort;
    }
    else {
      return args.getWatchdogPortDefault();
    }
  }

  public String getUserName(ArgsDaemon args)
  {
    return null;
  }

  public String getGroupName(ArgsDaemon args)
  {
    return null;
  }

  public String getCluster(ArgsDaemon args)
  {
    String clusterId = args.getClusterId();
    
    if (clusterId != null) {
      return clusterId;
    }
    
    return _cluster.getId();
  }

  /**
   * dynamic servers are non-seed servers, i.e. servers not configured in the
   * baratine.cf.
   */
  public void setDynamic(boolean isDynamic)
  {
    _isDynamic = isDynamic;
  }
  
  public boolean isDynamic()
  {
    return _isDynamic;
  }

  /**
   * ephemeral servers are dynamic servers with ephemeral ports. Their data
   * directories are cleared on restart. 
   */
  public void setEphemeral(boolean isEphemeral)
  {
    _isEphemeral = isEphemeral;
    
    setRemoveDataOnStart(isEphemeral);
  }
  
  public boolean isEphemeral()
  {
    return _isEphemeral;
  }

  public void setRemoveDataOnStart(boolean isRemoveData)
  {
    _isRemoveDataOnStart = isRemoveData;
  }
  
  /*
  public boolean isRemoveDataOnStart()
  {
    return _isRemoveDataOnStart;
  }
  */

  public boolean isRemoveDataOnStart(ArgsDaemon args)
  {
    if (args.getArgBoolean("remove-data")) {
      return true;
    }
    else {
      return _isRemoveDataOnStart;
    }
  }

  public String getDebugId()
  {
    StringBuilder sb = new StringBuilder();
    
    String address = getAddress();
    
    if (address == null || "".equals(address)) {
      address = "*";
    }
    
    String displayName = getDisplayName();
    
    if (getPort() >= 0) {
      sb.append(address);
      sb.append(":");
      sb.append(getPort());
    }
    else {
      sb.append("embed");
    }
    
    sb.append(" (");
    sb.append(displayName);
    sb.append(")");
    
    return sb.toString();
  }

  public void setServerSocket(ServerSocketBar ss)
  {
    System.out.println("SS0: " + ss);
    Thread.dumpStack();
    _ss = ss;
  }
  
  public ServerSocketBar getServerSocket()
  {
    return _ss;
  }

  public void setSocketBartender(ServerSocketBar ssBartender)
  {
    _ssBartender = ssBartender;
  }

  public ServerSocketBar getSocketBartender()
  {
    return _ssBartender;
  }
  
  /**
   * Ignore items we can't understand.
   */
  /*
  @Configurable
  public void addProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  */
  
  public ConfigProgram getServerProgram()
  {
    return _program;
  }
  
  
  /**
   * allow-non-reserved-ip
   */
  @Configurable
  public void setAllowNonReservedIp(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  
  /**
   * http
   */
  @Configurable
  public void addHttp(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  /**
   * jvm-arg
   */
  @Configurable
  public void addJvmArg(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * keepalive-select-enable
   */
  @Configurable
  public void setKeepaliveSelectEnable(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * keepalive-select-thread-timeout
   */
  @Configurable
  public void setKeepaliveSelectThreadTimeout(ConfigProgram program)
  {
    _programPort.addProgram(program);
  }
  
  //
  // port config
  //

  /**
   * cluster-port-timeout
   */
  @Configurable
  public void setClusterPortTimeout(Period period)
  {
    _clusterPortTimeout = period.getPeriod();
  }

  /**
   * cluster-port-timeout
   */
  public long getClusterPortTimeout()
  {
    return _clusterPortTimeout;
  }

  /**
   * keepalive-timeout:
   */
  @Configurable
  public void setKeepaliveTimeout(ConfigProgram program)
  {
    _programPort.addProgram(program);
  }

  /**
   * liten
   */
  @Configurable
  public void setListen(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * openssl:
   */
  @Configurable
  public void setOpenssl(ConfigProgram program)
  {
    _isSSL = true;
    
    _programPort.addProgram(program);
  }

  /**
   * sendfile-enable:
   */
  @Configurable
  public void setSendfileEnable(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * sendfile-min-length:
   */
  @Configurable
  public void setSendfileMinLength(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  /**
   * cluster-idle-time
   */
  @Configurable
  public void setClusterIdleTime(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  /**
   * socket-timeout
   */
  @Configurable
  public void setSocketTimeout(ConfigProgram program)
  {
    _programPort.addProgram(program);
  }

  public void configure(Object bean)
  {
    getServerProgram().configure(bean);
  }

  public void configurePort(PortTcp port)
  {
    _programPort.configure(port);
  }
  
  @Override
  public int hashCode()
  {
    return getFullAddress().hashCode();
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof ServerConfigBoot)) {
      return false;
    }
    
    ServerConfigBoot server = (ServerConfigBoot) o;
    
    return getFullAddress().equals(server.getFullAddress());
  }
  

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "," + getAddress() + ":" + getPort() + "]";
  }
}
