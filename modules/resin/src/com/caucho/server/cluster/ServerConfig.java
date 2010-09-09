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

import javax.annotation.PostConstruct;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterService;
import com.caucho.cloud.network.NetworkListenService;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.admin.Management;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * ServletContainer configration from the <server> tags.
 */
@Configurable
public class ServerConfig 
{
  private static final L10N L = new L10N(ServerConfig.class);

  private ServletContainerConfig _config;

  /**
   * Creates a new servlet server.
   */
  public ServerConfig(ServletContainerConfig config)
  {
    _config = config;
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setMemoryFreeMin(Bytes min)
  {
    _config.setMemoryFreeMin(min);
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setPermGenFreeMin(Bytes min)
  {
    _config.setPermGenFreeMin(min);
  }

  /**
   * Sets the max wait time for shutdown.
   */
  @Configurable
  public void setShutdownWaitMax(Period waitTime)
  {
    _config.setShutdownWaitMax(waitTime);
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  @Configurable
  public void setThreadMax(int max)
  {
    _config.setThreadMax(max);
  }

  /**
   * Sets the maximum executor (background) thread.
   */
  @Configurable
  public void setThreadExecutorTaskMax(int max)
  {
    _config.setThreadExecutorTaskMax(max);
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
  }
}
