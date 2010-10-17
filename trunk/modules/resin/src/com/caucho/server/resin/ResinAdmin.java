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
 * @author Sam
 */

package com.caucho.server.resin;

import com.caucho.VersionFactory;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.server.cluster.Server;
import com.caucho.server.util.CauchoSystem;

public class ResinAdmin extends AbstractManagedObject
  implements ResinMXBean
{
  private final Resin _resin;

  /**
   * Creates the admin object and registers with JMX.
   */
  public ResinAdmin(Resin resin)
  {
    _resin = resin;

    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  //
  // Hierarchy attributes
  //

  /**
   * Returns the Clusters known to Resin.
   */
  @Override
  public ClusterMXBean []getClusters()
  {
    CloudSystem system = TopologyService.getCurrentSystem();
    
    CloudCluster []clusters = system.getClusterList();
    
    ClusterMXBean []mxClusters = new ClusterMXBean[clusters.length];
    
    for (int i = 0; i < clusters.length; i++) {
      mxClusters[i] = clusters[i].getAdmin();
    }
    
    return mxClusters;
  }

  //
  // Configuration attributes
  //

  @Override
  public String getConfigFile()
  {
    return _resin.getResinConf().getNativePath();
  }

  @Override
  public String getResinHome()
  {
    return _resin.getResinHome().getNativePath();
  }

  @Override
  public String getRootDirectory()
  {
    return _resin.getRootDirectory().getNativePath();
  }

  @Override
  public ServerMXBean getServer()
  {
    Server server = _resin.getServer();

    if (server != null)
      return server.getAdmin();
    else
      return null;
  }

  @Override
  public String getVersion()
  {
    return VersionFactory.getFullVersion();
  }
  
  @Override
  public boolean isProfessional()
  {
    return _resin.isProfessional();
  }

  @Override
  public String getLocalHost()
  {
    return CauchoSystem.getLocalHost();
  }

  @Override
  public String getUserName()
  {
    return System.getProperty("user.name");
  }

  @Override
  public String toString()
  {
    return "ResinAdmin[" + getObjectName() + "]";
  }
}
