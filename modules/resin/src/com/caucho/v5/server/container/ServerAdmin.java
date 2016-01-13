/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.v5.server.container;

import java.util.ArrayList;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ClusterBartender;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.jmx.server.ConfigMXBean;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.ClusterMXBean;
import com.caucho.v5.management.server.ResinMXBean;
import com.caucho.v5.management.server.ServerMXBean;
import com.caucho.v5.management.server.ThreadPoolMXBean;
import com.caucho.v5.profile.MemoryUtil;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.Version;

public class ServerAdmin extends ManagedObjectBase
  implements ResinMXBean
{
  private final ServerBaseOld _server;
  private ThreadPoolAdmin _threadPoolAdmin;

  /**
   * Creates the admin object and registers with JMX.
   */
  public ServerAdmin(ServerBaseOld resin)
  {
    _server = resin;

    registerSelf();
    
    _threadPoolAdmin = ThreadPoolAdmin.create();
    _threadPoolAdmin.register();
    
    MemoryUtil.create();
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
    ServerBartender selfServer = BartenderSystem.getCurrentSelfServer();
    
    ArrayList<ClusterMXBean> mxClusterList = new ArrayList<>();
    
    for (ClusterBartender domain : selfServer.getRoot().getClusters()) {
      // ClusterMXBean mxCluster = cluster.getAdmin();
      // mxClusters.add(mxCluster);
    }
    
    ClusterMXBean []mxClusters = new ClusterMXBean[mxClusterList.size()];
    mxClusterList.toArray(mxClusters);
    
    return mxClusters;
  }

  public ThreadPoolMXBean getThreadPoolAdmin()
  {
    return _threadPoolAdmin;
  }
  
  @Override
  public ConfigMXBean []getConfigs()
  {
    /*
    Collection<ConfigMXBean> beans = ConfigAdmin.getMBeans(_resin.getClassLoader());
    ConfigMXBean[] array = new ConfigMXBean[beans.size()];
    beans.toArray(array);
    
    return array;
    */
    return null;
  }
  
  //
  // Configuration attributes
  //

  @Override
  public String getConfigFile()
  {
    return _server.getConfigPath().getNativePath();
  }

  @Override
  public String getConfigDirectory()
  {
    return _server.getConfigPath().getParent().getNativePath();
  }

  @Override
  public String getResinHome()
  {
    return _server.getHomeDirectory().getNativePath();
  }

  @Override
  public String getRootDirectory()
  {
    return _server.getRootDirectory().getNativePath();
  }
  
  @Override
  public String getLogDirectory()
  {
    return _server.getLogDirectory().getNativePath();
  }

  @Override
  public String getDataDirectory()
  {
    return RootDirectorySystem.getCurrentDataDirectory().getNativePath();
  }

  @Override
  public ServerMXBean getServer()
  {
    HttpContainer server = (HttpContainer) _server.getHttp();

    /*
    if (server != null)
      return server.getAdmin();
    else
      return null;
      */
    
    return null;
  }

  @Override
  public String getVersion()
  {
    return Version.getFullVersion();
  }
  
  @Override
  public boolean isProfessional()
  {
    return true;
  }

  @Override
  public String getLocalHost()
  {
    return CauchoUtil.getLocalHost();
  }

  @Override
  public String getUserName()
  {
    return System.getProperty("user.name");
  }
  
  @Override
  public boolean isRestart()
  {
    return _server.isRestart();
  }
  
  @Override
  public String getWatchdogStartMessage()
  {
    return _server.getRestartMessage();
  }

  @Override
  public String toString()
  {
    return "ResinAdmin[" + getObjectName() + "]";
  }
}
