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

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Container of clusters.
 */
public class ClusterContainer {
  static protected final Logger log = Log.open(ClusterContainer.class);

  static final EnvironmentLocal<ClusterContainer> _clusterContainerLocal =
  new EnvironmentLocal<ClusterContainer>("caucho.cluster.container");
  
  private ClusterContainer _parent;
  
  private ArrayList<Cluster> _clusterList = new ArrayList<Cluster>();

  private ClusterContainer()
  {
  }

  /**
   * Creates the cluster for the current level.
   */
  public static ClusterContainer create()
  {
    ClusterContainer container = _clusterContainerLocal.getLevel();

    if (container == null) {
      container = new ClusterContainer();
      ClusterContainer parent = _clusterContainerLocal.get();

      container.setParent(parent);
      _clusterContainerLocal.set(container);
    }

    return container;
  }

  /**
   * Creates the cluster for the current level.
   */
  public static ClusterContainer getLocal()
  {
    return _clusterContainerLocal.get();
  }

  /**
   * Creates the cluster for the current level.
   */
  public static ClusterContainer getLocal(ClassLoader loader)
  {
    return _clusterContainerLocal.get(loader);
  }

  /**
   * Sets the parent container.
   */
  private void setParent(ClusterContainer parent)
  {
    _parent = parent;
  }

  /**
   * Gets the parent container.
   */
  private ClusterContainer getParent()
  {
    return _parent;
  }

  /**
   * Adds a cluster.
   */
  public void addCluster(Cluster cluster)
  {
    _clusterList.add(cluster);
  }

  /**
   * Returns the cluster list.
   */
  public ArrayList<Cluster> getClusterList()
  {
    return _clusterList;
  }

  /**
   * Finds the named cluster.
   */
  public Cluster findCluster(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      Cluster cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
        return cluster;
    }

    if (_parent != null)
      return _parent.findCluster(id);
    else
      return null;
  }

  /**
   * Returns the matching ports.
   */
  public ArrayList<ClusterPort> getServerPorts(String serverId)
  {
    ArrayList<ClusterPort> ports = new ArrayList<ClusterPort>();
    
    for (int i = 0; i < _clusterList.size(); i++) {
      Cluster cluster = _clusterList.get(i);

      ports.addAll(cluster.getServerPorts(serverId));
    }

    return ports;
  }

  public String toString()
  {
    return ("ClusterContainer[]");
  }
}
