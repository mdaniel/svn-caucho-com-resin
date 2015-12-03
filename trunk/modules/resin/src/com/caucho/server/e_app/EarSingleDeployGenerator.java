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

package com.caucho.server.e_app;

import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.server.webapp.WebAppController;
import com.caucho.vfs.Path;

/**
 * The generator for the ear-deploy
 */
public class EarSingleDeployGenerator extends DeployGenerator<EarDeployController> {
  private String _urlPrefix = "";

  private WebAppContainer _parentContainer;
  
  private EarDeployController _controller;

  public EarSingleDeployGenerator(DeployContainer<EarDeployController> deployContainer,
                         WebAppContainer parentContainer,
                         EarConfig config)
  {
    super(deployContainer);
    
    _parentContainer = parentContainer;
    
    String id = "production/entapp/default/default";
    Path rootDirectory = config.calculateRootDirectory();

    _controller = new EarDeployController(id, rootDirectory, "",
                                          parentContainer, config);
  }

  /**
   * Returns the parent container;
   */
  WebAppContainer getContainer()
  {
    return _parentContainer;
  }

  /**
   * Returns any matching web-app entry.
   */
  public WebAppController findWebAppEntry(String name)
  {
    WebAppController entry = _controller.findWebAppController(name);

    return entry;
  }
  
  /**
   * Returns the current array of webApp entries.
   */
  public EarDeployController createEntry(String name)
    throws Exception
  {
    return null;
  }

  public Throwable getConfigException()
  {
    Throwable configException =  super.getConfigException();

    if (configException == null && _controller != null)
      configException = _controller.getConfigException();

    return configException;
  }
}
