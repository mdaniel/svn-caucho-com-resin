/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.host;

import java.util.ArrayList;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.config.types.PathBuilder;

import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.DeployContainer;

/**
 * The generator for the host deploy
 */
public class HostSingleDeployGenerator extends DeployGenerator<HostController> {
  private static final Logger log = Log.open(HostSingleDeployGenerator.class);

  private HostContainer _container;

  private Path _archivePath;
  private Path _rootDirectory;

  private HostConfig _config;

  private HostController _entry;

  /**
   * Creates the new host deploy.
   */
  public HostSingleDeployGenerator(DeployContainer<HostController> container)
  {
    super(container);
  }

  /**
   * Creates the new host deploy.
   */
  public HostSingleDeployGenerator(DeployContainer<HostController> container,
			  HostContainer hostContainer, HostConfig config)
    throws Exception
  {
    super(container);
    
    _container = hostContainer;

    _config = config;

    Path serverRoot = hostContainer.getRootDirectory();
    String rootDir = config.getRootDirectory();

    if (rootDir == null)
      _rootDirectory = serverRoot;
    else
      _rootDirectory = PathBuilder.lookupPath(rootDir, null, serverRoot);

    _config = config;

    init();
  }

  /**
   * Gets the host container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Gets the parent loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _container.getClassLoader();
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Initializes the entry.
   */
  public void init()
  {
    _entry = new HostController(_container, _config);

    _entry.setCfgRootDirectory(_rootDirectory);
    _entry.setArchivePath(_archivePath);

    if (_archivePath != null)
      _entry.addDepend(_archivePath);

    if (_config.getStartupMode() != null)
      _entry.setStartupMode(_config.getStartupMode());

    _entry.init();
    
    // _entry.initAdmin();
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    keys.add(_entry.getName());
  }
  
  /**
   * Returns the current array of application entries.
   */
  public HostController generateController(String name)
  {
    if (_entry.isNameMatch(name))
      return _entry;
    else
      return null;
  }
  
  /**
   * Merges the entries.
   */
  public HostController mergeEntry(HostController entry, String name)
  {
    if (! _entry.isNameMatch(name))
      return entry;
    else
      return entry.merge(_entry);
  }

  /**
   * Initialize the deployment.
   */
  public void deploy()
  {
    // _entry.deploy();
  }
  
  /**
   * Destroy the deployment.
   */
  public void destroy()
  {
  }

  public String toString()
  {
    return "HostDeploy[" + _config.getHostName() + "]";
  }
}
