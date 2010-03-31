/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import com.caucho.network.server.NetworkServer;
import com.caucho.server.resin.*;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Defines a cluster with a single server
 */
public class SingleCluster extends Cluster
{
  private static final L10N L = new L10N(SingleCluster.class);
  private static final Logger log
    = Logger.getLogger(SingleCluster.class.getName());

  private SingleClusterPod _pod;
  private ClusterPod [] _podList;

  public SingleCluster(Resin resin)
  {
    super(resin);

    _pod = new SingleClusterPod(this);
    _podList = new ClusterPod[] { _pod };
  }

  public SingleCluster()
  {
    this(null);
  }

  /**
   * Returns the pod as a list
   */
  @Override
  public ClusterPod []getPodList()
  {
    return _podList;
  }

  //
  // configuration
  //

  /**
   * Adds a new server to the cluster during configuration.
   */
  public ClusterServer createServer()
  {
    if (isActive())
      throw new IllegalStateException(L.l("{0}: can't create static server after initialization", this));

    return _pod.createServer();
  }

  @Override
  protected Server createResinServer(NetworkServer networkServer,
                                     ClusterServer clusterServer)
  {
    return new Server(networkServer, clusterServer);
  }
}
