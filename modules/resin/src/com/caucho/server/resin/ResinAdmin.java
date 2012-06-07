/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.util.Collection;

import com.caucho.VersionFactory;
import com.caucho.cloud.topology.*;
import com.caucho.config.ConfigAdmin;
import com.caucho.management.server.*;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.util.CauchoSystem;

public class ResinAdmin extends AbstractManagedObject
  implements ResinMXBean
{
  private final Resin _resin;
  private ThreadPoolAdmin _threadPoolAdmin;

  /**
   * Creates the admin object and registers with JMX.
   */
  public ResinAdmin(Resin resin)
  {
    _resin = resin;

    registerSelf();
    
    _threadPoolAdmin = ThreadPoolAdmin.create();
    _threadPoolAdmin.register();
    
    MemoryAdmin.create();
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

  public ThreadPoolMXBean getThreadPoolAdmin()
  {
    return _threadPoolAdmin;
  }
  
  @Override
  public ConfigMXBean []getConfigs()
  {
    Collection<ConfigMXBean> beans = ConfigAdmin.getMBeans(_resin.getClassLoader());
    ConfigMXBean[] array = new ConfigMXBean[beans.size()];
    beans.toArray(array);
    
    return array;
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
  public String getConfigDirectory()
  {
    return _resin.getConfDirectory().getNativePath();
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
  public String getLogDirectory()
  {
    return _resin.getLogDirectory().getNativePath();
  }

  @Override
  public String getDataDirectory()
  {
    return _resin.getResinDataDirectory().getNativePath();
  }

  @Override
  public ServerMXBean getServer()
  {
    ServletService server = _resin.getServer();

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
  public boolean isRestart()
  {
    return _resin.isRestart();
  }
  
  @Override
  public String getWatchdogStartMessage()
  {
    return _resin.getRestartMessage();
  }

  @Override
  public String toString()
  {
    return "ResinAdmin[" + getObjectName() + "]";
  }
}
