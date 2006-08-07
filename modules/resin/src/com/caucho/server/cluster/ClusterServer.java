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

import java.net.*;
import java.util.*;

import java.util.logging.*;

import java.io.IOException;

import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.config.types.*;

import com.caucho.log.Log;

import com.caucho.jmx.Jmx;

import com.caucho.management.server.ServerConnectorMXBean;

import com.caucho.server.port.*;
import com.caucho.server.http.*;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadWritePair;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * Defines a member of the cluster.
 *
 * A {@link ClusterClient} obtained with {@link #getClient} is used to actually
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

  private ArrayList<Port> _ports = new ArrayList<Port>();

  public ClusterServer(Cluster cluster)
  {
    _cluster = cluster;

    _clusterPort = new ClusterPort(this);
    _ports.add(_clusterPort);
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
    // port.setServer(this);

    /*
    if (_url.equals("") && port.matchesServerId(_serverId)) {
      if (port.getAddress() == null || port.getAddress().equals("") ||
          port.getAddress().equals("*"))
        _url = "http://localhost";
      else
        _url = "http://" + port.getAddress();

      if (port.getPort() != 0)
        _url += ":" + port.getPort();

      if (_hostContainer != null)
        _hostContainer.setURL(_url);
    }
    */

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
    return _clusterPort.getServerConnector();
  }

  /**
   * Initialize
   */
  public void init()
    throws Exception
  {
    _clusterPort.init();
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
