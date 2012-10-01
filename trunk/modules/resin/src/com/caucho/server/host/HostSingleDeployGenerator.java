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
 * @author Scott Ferguson
 */

package com.caucho.server.host;

import com.caucho.config.Config;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployControllerType;
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The generator for the host deploy
 */
public class HostSingleDeployGenerator
  extends DeployGenerator<HostController> {
  private static final Logger log
    = Logger.getLogger(HostSingleDeployGenerator.class.getName());

  private HostContainer _container;

  private HostConfig _config;

  private HostController _controller;

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
                                   HostContainer hostContainer,
                                   HostConfig config)
  {
    super(container);
    
    _container = hostContainer;

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
  @Override
  public ClassLoader getParentClassLoader()
  {
    return _container.getClassLoader();
  }

  /**
   * Returns the log.
   */
  @Override
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Initializes the entry.
   */
  @Override
  public void initImpl()
  {
    super.initImpl();
    
    if (_controller != null)
      return;

    String hostName = "";
    String hostId = null;
    
    String rawId = _config.getId();
    String rawHostName = _config.getHostName();

    if (rawId != null) {
      hostId = Config.evalString(rawId);

      if (hostId.startsWith("*"))  // server/1f20
        hostId = hostId.substring(1);
    }

    if (rawHostName != null) {
      hostName = Config.evalString(rawHostName);

      if (rawHostName.startsWith("*"))  // server/1f20
        hostName = rawHostName.substring(1);
    }
    
    String stage = _container.getServer().getStage();
    
    String id;
    
    if (hostName.equals(""))
      id = stage + "/host/default";
    else if (hostName.startsWith(":"))
      id = stage + "/host/default" + hostName;
    else
      id = stage + "/host/" + hostName;
      
    Path rootDirectory = _config.calculateRootDirectory();

    if (hostName != null) {
      _controller = new HostController(id, rootDirectory,
                                       hostName, _config, _container, null);
      _controller.setControllerType(DeployControllerType.STATIC);

      if (hostId != null)
        _controller.addHostAlias(hostId);
    }
    else {
      _controller = new HostController(id, rootDirectory,
                                       hostId, _config, _container, null);
      _controller.setControllerType(DeployControllerType.STATIC);
    }
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedNames(Set<String> names)
  {
    String key = _controller.getName();
    
    if (key.equals("default"))
      names.add("");
    else
      names.add(key);
  }
  
  /**
   * Returns the current array of application entries.
   */
  @Override
  public void generateController(String name, ArrayList<HostController> list)
  {
    if (_controller.isNameMatch(name)) {
      Path rootDirectory = _config.calculateRootDirectory();
      
      HostController host;
      host = new HostController(_controller.getId(), rootDirectory,
                                _controller.getHostName(), _config,
                                _container, null);
      host.setControllerType(DeployControllerType.STATIC);

      // host = host.merge(_controller);
      
      list.add(host);
    }
  }
  
  /**
   * Merges the controllers.
   */
  /*
  @Override
  public HostController mergeController(HostController controller,
                                        String name)
  {
    // if directory matches, merge the two controllers.  The
    // last controller has priority.
    if (controller.getRootDirectory().equals(_controller.getRootDirectory())) {
      // controller.setDynamicDeploy(false);
      
      return controller.merge(_controller);
    }
    else if (! _controller.isNameMatch(name)) {
      // else if the names don't match, return the new controller
      
      return controller;
    }
    else {
      // otherwise, the single deploy overrides
      // server/10v9
      return _controller;
    }
  }
  */

  @Override
  public Throwable getConfigException()
  {
    Throwable configException =  super.getConfigException();

    if (configException == null && _controller != null)
      configException = _controller.getConfigException();

    return configException;
  }

  @Override
  public String toString()
  {
    if (_config == null)
      return getClass().getSimpleName() + "[]";
    else
      return getClass().getSimpleName() + "[" + _config.getHostName() + "]";
  }
}
