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

import com.caucho.loader.EnvironmentLocal;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Defines a group of clusters.  Normally, the group is the entire
 * set of clusters in the server.
 * subgroups of servers.
 */
public class ClusterGroup {
  private static final L10N L = new L10N(ClusterGroup.class);
  private static final Logger log = Log.open(ClusterGroup.class);

  static protected final EnvironmentLocal<ClusterGroup> _clusterGroupLocal
    = new EnvironmentLocal<ClusterGroup>();

  private final ArrayList<Cluster> _clusterList
    = new ArrayList<Cluster>();

  private ClusterGroup()
  {
  }

  /**
   * Returns the cluster group for the current level.
   */
  public static ClusterGroup getClusterGroup()
  {
    return _clusterGroupLocal.get();
  }
    
  /**
   * Returns the cluster group for the current level.
   */
  public static ClusterGroup createClusterGroup()
  {
    ClusterGroup group = _clusterGroupLocal.getLevel();

    if (group == null) {
      group = new ClusterGroup();
      _clusterGroupLocal.set(group);
    }

    return group;
  }

  /**
   * Adds a cluster.
   */
  public void addCluster(Cluster cluster)
  {
    if (! _clusterList.contains(cluster))
      _clusterList.add(cluster);
  }

  /**
   * Returns all the clusters.
   */
  public ArrayList<Cluster> getClusterList()
  {
    return _clusterList;
  }

  /**
   * Returns the cluster with the matching name.
   */
  public Cluster findCluster(String id)
  {
    for (int i = _clusterList.size() - 1; i >= 0; i--) {
      Cluster cluster = _clusterList.get(i);

      if (cluster.getId().equals(id))
	return cluster;
    }
    
    return null;
  }

  /**
   * Finds the srun client port matching the host and port.
   */
  public ServerConnector findClient(String host, int port)
  {
    for (int i = _clusterList.size() - 1; i >= 0; i--) {
      Cluster cluster = _clusterList.get(i);

      ServerConnector serverConnector = cluster.findConnector(host, port);

      if (serverConnector != null)
	return serverConnector;
    }
    
    return null;
  }
}
