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

import java.util.*;
import java.util.logging.*;
import com.caucho.util.*;

/**
 * Represents a machine in a cluster.  Contains multiple servers.
 */
public class Machine {
  private static final Logger log
    = Logger.getLogger(Machine.class.getName());
  private static final L10N L = new L10N(Machine.class);

  private ClusterTriad _triad;
  private String _id = "";

  private ArrayList<ClusterServer> _serverList
    = new ArrayList<ClusterServer>();

  public Machine(ClusterTriad triad)
  {
    _triad = triad;
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
  public ClusterTriad getClusterTriad()
  {
    return _triad;
  }

  /**
   * Creates a new ClusterServer.
   */
  public ClusterServer createServer()
  {
    ClusterServer server = _triad.createServer();

    server.setMachine(this);

    _serverList.add(server);

    return server;
  }

  /**
   * Creates a new ClusterServer.
   */
  /*
  public ClusterServer createDynamicServer()
  {
    ClusterServer server = new ClusterServer(this);

    _serverList.add(server);

    return _cluster.createDynamicServer(server);
  }
  */

  /**
   * Creates a new ClusterServer.
   */
  public void addServer(ClusterServer server)
  {
    _triad.addServer(server);
  }

  /**
   * Returns the list of servers.
   */
  public ArrayList<ClusterServer> getServerList()
  {
    return _serverList;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + getId() + "]";
  }
}
