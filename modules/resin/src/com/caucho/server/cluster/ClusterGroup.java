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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.log.Log;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.loader.EnvironmentLocal;

/**
 * Defines a group of cluster servers.  The group is an advanced
 * configuration in the case where the cluster is comprised of several
 * subgroups of servers.
 */
public class ClusterGroup {
  static protected final L10N L = new L10N(ClusterGroup.class);
  static protected final Logger log = Log.open(ClusterGroup.class);

  private Cluster _cluster;
  private ClusterServer []_serverList = new ClusterServer[0];

  public ClusterGroup()
  {
  }

  /**
   * Sets the group's server.
   */
  public void setCluster(Cluster cluster)
  {
    _cluster = cluster;
  }

  /**
   * Gets the group's server.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Adds a srun server.
   */
  public void addPort(ClusterPort port)
    throws Exception
  {
    ClusterServer server = new ClusterServer();
    server.setCluster(getCluster());
    server.setGroup(this);
    server.setPort(port);

    if (port.getIndex() < 0)
      port.setIndex(getCluster().getServerList().length + 1);
    else if (port.getIndex() != getCluster().getServerList().length) {
      log.config(L.l("srun index '{0}' for port 'id={1}' does not match expected cluster index '{2}'",
		     port.getIndex(),
		     port.getServerId(),
		     getCluster().getServerList().length));
    }
    
    server.init();

    add(server);
  }

  /**
   * Adds a srun server.
   */
  public void addSrun(ClusterPort port)
    throws Exception
  {
    addPort(port);
  }

  /**
   * Returns the array of servers in the group.
   */
  public ClusterServer []getServerList()
  {
    return _serverList;
  }
  
  /**
   * Adds a new server to the cluster.
   */
  void add(ClusterServer server)
    throws ConfigException
  {
    int newLength = _serverList.length + 1;
    ClusterServer []newList = new ClusterServer[newLength];

    System.arraycopy(_serverList, 0, newList, 0, _serverList.length);
    _serverList = newList;

    int i = newLength - 1;
    for (; i > 0; i--) {
      ClusterServer oldServer = _serverList[i - 1];

      if (oldServer.getIndex() == server.getIndex())
	throw new ConfigException(L.l("Cluster server `{0}' conflicts with a previous server.", server.getIndex()));

      else if (oldServer.getIndex() < server.getIndex()) {
	break;
      }
      else {
	_serverList[i] = oldServer;
	oldServer.setGroupIndex(i);
      }
    }

    _serverList[i] = server;
    server.setGroup(this);
    server.setGroupIndex(i);

    getCluster().addServer(server);
  }
}
