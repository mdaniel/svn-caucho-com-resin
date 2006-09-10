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

package com.caucho.server.resin;

import com.caucho.config.*;
import com.caucho.server.cluster.*;
import com.caucho.util.*;

/**
 * Compatiblity configuration for Resin 3.0-style configuration.
 */
public class ServerCompatConfig {
  private static final L10N L = new L10N(ServerCompatConfig.class);

  private final Resin _resin;

  private BuilderProgramContainer _program
    = new BuilderProgramContainer();

  /**
   * Creates a new resin server.
   */
  public ServerCompatConfig(Resin resin)
  {
    if (resin == null)
      throw new NullPointerException();
    
    _resin = resin;
  }

  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Creates a cluster compat.
   */
  public ClusterCompatConfig createCluster()
  {
    return new ClusterCompatConfig(_resin);
  }

  public void init()
  {
    try {
      String serverId = _resin.getServerId();
    
      ClusterServer clusterServer = _resin.findClusterServer(serverId);

      if (clusterServer != null) {
      }
      else if (_resin.getClusterList().size() > 0 || ! "".equals(serverId)) {
	throw new ConfigException(L.l("-server '{0}' does not match any defined servers",
				      serverId));
      }
      else {
	Cluster cluster = _resin.createCluster();
	_resin.addCluster(cluster);

	clusterServer = cluster.createServer();
	
	clusterServer.setPort(0);

	cluster.addServer(clusterServer);
      }

      _program.configure(clusterServer.getCluster());
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
