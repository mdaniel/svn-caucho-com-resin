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

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.config.ObjectAttributeProgram;
import com.caucho.config.types.RawString;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.DeployManagementService;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.HostConfig;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.resource.spi.ResourceAdapter;
import java.util.logging.Logger;

/**
 * Configuration for management.
 */
public class Management
{
  private static L10N L = new L10N(Management.class);
  private static Logger log = Logger.getLogger(Management.class.getName());

  public static final String HOST_NAME = "admin.caucho";

  private Cluster _cluster;
  private Server _server;
  private Path _path;

  private HostConfig _hostConfig;

  private DeployManagementService _deployService;
  protected TransactionManager _transactionManager;

  public void setCluster(Cluster cluster)
  {
    _cluster = cluster;
  }
  
  public void setServer(Server server)
  {
    _server = server;
  }

  public String getServerId()
  {
    return Cluster.getServerId();
  }

  /**
   * Sets the path for storing managment related logs and files,
   * default is "admin".
   */
  public void setPath(Path path)
  {
    _path = path;
  }
  
  public Path getPath()
  {
    return _path;
  }

  /**
   * Create and configure the j2ee deploy service.
   */
  public DeployManagementService createDeployService()
  {
    if (_deployService == null)
      _deployService = new DeployManagementService(this);

    return _deployService;
  }

  /**
   * Create and configure the jmx service.
   */
  public Object createJmxService()
  {
    throw new ConfigException(L.l("jmx-service requires Resin Professional"));
  }

  /**
   * Create and configure the ping monitor.
   */
  public ResourceAdapter createPing()
  {
    throw new ConfigException(L.l("ping requires Resin Professional"));
  }

  public void addPing(ResourceAdapter ping)
  {
    throw new ConfigException(L.l("ping requires Resin Professional"));
  }

  /**
   * Create and configure the persistent logger.
   */
  public Object createPersistentLog()
  {
    log.fine(L.l("'{0}' management requires Resin Professional",
                 "persistent-logger"));

    return null;
  }

  /**
   * Create and configure the transaction log.
   */
  public TransactionLog createXaLog()
  {
    return createTransactionManager().createTransactionLog();
  }

  /**
   * backwards compat
   */
  @Deprecated
  public void setManagementPath(Path managementPath)
  {
    if (_path == null)
      _path = managementPath;
  }

  /**
   * backwards compat
   */
  @Deprecated
  public TransactionManager createTransactionManager()
    throws ConfigException
  {
    if (_transactionManager == null)
      _transactionManager = new TransactionManager(this);

    return _transactionManager;
  }

  public void init()
  {
    start();
  }
  
  public void start()
  {
    try {
      if (getPath() != null)
        getPath().mkdirs();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    // the start is necessary for the qa tests
    if (_transactionManager != null)
      _transactionManager.start();

    if (_deployService != null)
      _deployService.start();
  }

  public HostConfig getHostConfig()
  {
    if (_hostConfig == null) {
      HostConfig hostConfig = new HostConfig();
      hostConfig.setId(HOST_NAME);
      hostConfig.setRootDirectory(new RawString("bogus-root"));

      hostConfig.init();

      try {
	_server.addHost(hostConfig);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw ConfigException.create(e);
      }

      _hostConfig = hostConfig;
    }

    return _hostConfig;
  }

  protected Cluster getCluster()
  {
    if (_cluster == null)
      _cluster = Cluster.getLocal();

    return _cluster;
  }

  public void destroy()
  {
    TransactionManager transactionManager = _transactionManager;
    _transactionManager = null;

    DeployManagementService deployService = _deployService;
    _deployService = null;

    if (transactionManager != null)
      transactionManager.destroy();
  }
}
