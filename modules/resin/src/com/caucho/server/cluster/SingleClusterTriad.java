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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * The ClusterTriad is a reliable triplicate for clustered data.
 * 
 * For small clusters, the triad may only have 1 or 2 servers, so
 * server B and server C may return null.
 */
public class SingleClusterTriad extends ClusterTriad
{
  private static final L10N L = new L10N(SingleClusterTriad.class);
  private static final Logger log
    = Logger.getLogger(SingleClusterTriad.class.getName());

  private ClusterServer _serverA;

  private ClusterServer []_serverList = new ClusterServer[0];

  /**
   * Creates a new triad for the cluster.
   * 
   * @param cluster the owning cluster
   * @param serverA the triad's first server
   * @param serverB the triad's second server
   * @param serverC the triad's third server
   */
  SingleClusterTriad(Cluster cluster)
  {
    super(cluster, 0);
  }

  /**
   * Return the servers in the triad
   */
  public ClusterServer []getServerList()
  {
    return _serverList;
  }
  
  /**
   * Returns the triad's first server
   * 
   * @return the first server.
   */
  public ClusterServer getServerA()
  {
    return _serverA;
  }
  
  /**
   * Returns the triad's second server
   * 
   * @return the second server.
   */
  public ClusterServer getServerB()
  {
    return null;
  }
  
  /**
   * Returns the triad's third server
   * 
   * @return the third server.
   */
  public ClusterServer getServerC()
  {
    return null;
  }

  /**
   * Returns the OwnerServerTriad for the given owner.
   */
  @Override
  public OwnerServerTriad getOwnerServerTriad(Owner owner)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // configuration
  //

  /**
   * Creates the server
   */
  public ClusterServer createServer()
  {
    if (_serverA != null)
      throw new ConfigException(L.l("Multiple servers requires Resin Professional"));
    
    _serverA = new ClusterServer(this, 0);
    _serverList = new ClusterServer[] { _serverA };

    getCluster().configureServerDefault(_serverA);

    return _serverA;
  }
}
