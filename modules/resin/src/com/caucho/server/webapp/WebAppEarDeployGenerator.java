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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.config.types.PathBuilder;

import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.DeployContainer;

import com.caucho.server.e_app.EarDeployGenerator;

/**
 * The generator for the ear deploy
 */
public class WebAppEarDeployGenerator extends DeployGenerator<WebAppController> {
  private static final Logger log = Log.open(WebAppEarDeployGenerator.class);

  private ApplicationContainer _container;
  
  private String _urlPrefix = "";

  private ClassLoader _parentLoader;

  private EarDeployGenerator _earDeploy;

  /**
   * Creates the new ear deploy.
   */
  public WebAppEarDeployGenerator(DeployContainer<WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new host deploy.
   */
  public WebAppEarDeployGenerator(DeployContainer<WebAppController> deployContainer,
			 ApplicationContainer container,
			 EarDeployGenerator earDeploy)
    throws Exception
  {
    super(deployContainer);
    
    setContainer(container);

    _earDeploy = earDeploy;
  }

  /**
   * Gets the application container.
   */
  public ApplicationContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the application container.
   */
  public void setContainer(ApplicationContainer container)
  {
    _container = container;

    if (_parentLoader == null)
      _parentLoader = container.getClassLoader();
  }

  /**
   * Sets the parent loader.
   */
  public void setParentClassLoader(ClassLoader loader)
  {
    _parentLoader = loader;
  }

  /**
   * Sets the url prefix.
   */
  public void setURLPrefix(String prefix)
  {
    while (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    
    _urlPrefix = prefix;
  }

  /**
   * Gets the url prefix.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Return true if modified.
   */
  public boolean isModified()
  {
    return _earDeploy.isModified();
  }

  /**
   * Redeploys is modified.
   */
  public void redeployIfModified()
  {
    _earDeploy.redeployIfModified();
  }
  
  /**
   * Returns the current array of application entries.
   */
  public WebAppController generateController(String name)
  {
    return _earDeploy.findWebAppEntry(name);
  }

  /**
   * Initialize the deployment.
   */
  public void deploy()
  {
    try {
      _earDeploy.deploy();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Destroy the deployment.
   */
  public void destroy()
  {
  }

  public String toString()
  {
    return "WebAppEarDeployGenerator[" + _earDeploy + "]";
  }
}
