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

import java.io.*;
import java.net.*;

import com.caucho.util.L10N;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.server.port.Port;
import com.caucho.server.port.Protocol;

import com.caucho.server.hmux.HmuxProtocol;

/**
 * Represents a protocol connection.
 */
public class ClusterPort extends Port {
  private static final L10N L = new L10N(ClusterPort.class);

  private ClusterServer _server;
  private ServerConnector _conn;
  
  private int _clientWeight = 100;

  public ClusterPort(ClusterServer server)
  {
    _server = server;
    _conn = new ServerConnector(this);
    
    try {
      setAddress("127.0.0.1");
      setPort(6800);
      
      setProtocol(new HmuxProtocol());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the cluster server.
   */
  public ClusterServer getClusterServer()
  {
    return _server;
  }

  /**
   * Returns the server connector.
   */
  public ServerConnector getServerConnector()
  {
    return _conn;
  }

  /**
   * Returns the session index for the srun.
   */
  public int getIndex()
  {
    return _server.getIndex();
  }

  /**
   * Set the client weight.
   */
  public void setClientWeight(int weight)
  {
    _clientWeight = weight;
  }

  /**
   * Return the client weight.
   */
  public int getClientWeight()
  {
    return _clientWeight;
  }

  public void init()
  {
    try {
      _conn.init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toString()
  {
    if (getAddress() != null)
      return "ClusterPort[address=" + getAddress() + ",port=" + getPort() + "]";
    else
      return "ClusterPort[address=*,port=" + getPort() + "]";
  }
}
