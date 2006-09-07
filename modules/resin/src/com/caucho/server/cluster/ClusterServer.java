/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.log.Log;
import com.caucho.server.http.HttpProtocol;
import com.caucho.server.port.Port;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.management.ObjectName;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Defines a member of the cluster.
 *
 * A {@link ServerConnector} obtained with {@link #getServerConnector} is used to actually
 * communicate with this ClusterServer when it is active in another instance of
 * Resin .
 */
public class ClusterServer {
  private static final Logger log = Log.open(ClusterServer.class);
  private static final L10N L = new L10N(ClusterServer.class);

  private ObjectName _objectName;

  private Cluster _cluster;
  private String _id = "";

  private int _index;

  private ClusterPort _clusterPort;
  private ServerConnector _serverConnector;

  private long _loadBalanceMaxIdleTime = 30000L;
  private long _loadBalanceFailRecoverTime = 15000L;
  private long _loadBalanceWarmupTime = 60000L;
  
  private long _loadBalanceReadTimeout = 60000L;
  private long _loadBalanceWriteTimeout = 60000L;
  private long _loadBalanceConnectTimeout = 5000L;
  
  private int _loadBalanceWeight = 100;

  private BuilderProgramContainer _serverProgram
    = new BuilderProgramContainer();

  private ArrayList<Port> _ports = new ArrayList<Port>();

  public ClusterServer(Cluster cluster)
  {
    _cluster = cluster;

    _clusterPort = new ClusterPort(this);
    _ports.add(_clusterPort);
    
    _serverConnector = new ServerConnector(this);
  }

  public ClusterServer(Cluster cluster, boolean isTest)
  {
    _cluster = cluster;

    _clusterPort = new ClusterPort(this);
    
    _serverConnector = new ServerConnector(this);
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
   * Returns the cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the server index.
   */
  void setIndex(int index)
  {
    _index = index;
  }

  /**
   * Returns the server index.
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveMax(int max)
  {
  }

  /**
   * Sets the keepalive timeout.
   */
  public void setKeepaliveTimeout(Period timeout)
  {
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
   * Sets the loadBalance max-idle-time.
   */
  public void setLoadBalanceMaxIdleTime(Period period)
  {
    _loadBalanceMaxIdleTime = period.getPeriod();
  }

  /**
   * Sets the loadBalance max-idle-time.
   */
  public long getLoadBalanceMaxIdleTime()
  {
    return _loadBalanceMaxIdleTime;
  }

  /**
   * Sets the loadBalance fail-recover-time.
   */
  public void setLoadBalanceFailRecoverTime(Period period)
  {
    _loadBalanceFailRecoverTime = period.getPeriod();
  }

  /**
   * Gets the loadBalance fail-recover-time.
   */
  public long getLoadBalanceFailRecoverTime()
  {
    return _loadBalanceFailRecoverTime;
  }

  /**
   * Sets the loadBalance read/write timeout
   */
  public void setLoadBalanceTimeout(Period period)
  {
    long timeout = period.getPeriod();
    
    _loadBalanceReadTimeout = timeout;
    _loadBalanceWriteTimeout = timeout;
  }

  /**
   * Gets the loadBalance read/write timeout
   */
  public long getLoadBalanceReadTimeout()
  {
    return _loadBalanceReadTimeout;
  }

  /**
   * Gets the loadBalance read/write timeout
   */
  public long getLoadBalanceWriteTimeout()
  {
    return _loadBalanceWriteTimeout;
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
  public void addJvmArgs(String args)
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
   * Adds a http.
   */
  public void addHttp(Port port)
    throws ConfigException
  {
    if (port.getProtocol() == null) {
      HttpProtocol protocol = new HttpProtocol();
      protocol.setParent(port);
      port.setProtocol(protocol);
    }

    _ports.add(port);
  }

  /**
   * Adds a custom-protocol port.
   */
  public void addProtocol(Port port)
    throws ConfigException
  {
    if (port.getProtocol() == null) {
      HttpProtocol protocol = new HttpProtocol();
      protocol.setParent(port);
      port.setProtocol(protocol);
    }

    _ports.add(port);
  }

  /**
   * Pre-binding of ports.
   */
  public void bind(String address, int port, QServerSocket ss)
    throws Exception
  {
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
   * Returns the server connector.
   */
  public ServerConnector getServerConnector()
  {
    return _serverConnector;
  }

  /**
   * Adds a program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _serverProgram.addProgram(program);
  }

  /**
   * Adds a program.
   */
  public BuilderProgram getServerProgram()
  {
    return _serverProgram;
  }

  /**
   * Initialize
   */
  public void init()
    throws Exception
  {
    _clusterPort.init();

    _serverConnector.init();
  }

  /**
   * Starts the server.
   */
  public Server startServer()
    throws Throwable
  {
    return _cluster.startServer(this);
  }

  /**
   * Close any ports.
   */
  public void close()
  {
  }

  public String toString()
  {
    return ("ClusterServer[id=" + getId() + "]");
  }
}
