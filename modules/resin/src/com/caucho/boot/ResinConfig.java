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

package com.caucho.boot;

import java.util.*;

import com.caucho.config.*;
import com.caucho.vfs.*;

public class ResinConfig {
  private ArrayList<ClusterConfig> _clusterList
    = new ArrayList<ClusterConfig>();

  private Path _resinHome;
  private Path _rootDirectory;

  ResinConfig(Path resinHome, Path serverRoot)
  {
    _resinHome = resinHome;
    _rootDirectory = serverRoot;
  }

  public Path getResinHome()
  {
    return _resinHome;
  }

  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  public void setRootDirector(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public ClusterConfig createCluster()
  {
    ClusterConfig cluster = new ClusterConfig(this);
    
    _clusterList.add(cluster);

    return cluster;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
  }

  /**
   * Finds a server.
   */
  public ServerWatchdog findServer(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      ClusterConfig cluster = _clusterList.get(i);

      ServerWatchdog server = cluster.findServer(id);

      if (server != null)
	return server;
    }

    return null;
  }
}
