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

package com.caucho.server.webapp;

import java.util.ArrayList;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.config.types.PathBuilder;

import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.DeployContainer;

/**
 * The generator for the web-app deploy
 */
public class WebAppSingleDeployGenerator
  extends DeployGenerator<WebAppController>
  implements EnvironmentListener {
  private static final Logger log = Log.open(WebAppSingleDeployGenerator.class);

  private ApplicationContainer _container;
  
  private WebAppController _parentWebApp;

  private String _urlPrefix = "";

  private Path _archivePath;
  private Path _rootDirectory;

  private WebAppConfig _config;

  private ClassLoader _parentLoader;

  private WebAppController _controller;

  /**
   * Creates the new host deploy.
   */
  public WebAppSingleDeployGenerator(DeployContainer<WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new host deploy.
   */
  public WebAppSingleDeployGenerator(DeployContainer<WebAppController> deployContainer,
			    ApplicationContainer container,
			    WebAppConfig config)
    throws Exception
  {
    super(deployContainer);
    
    setContainer(container);

    String contextPath = config.getContextPath();

    if (contextPath.equals("/"))
      contextPath = "";
    
    setURLPrefix(config.getContextPath());

    _config = config;
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
   * Sets the parent application.
   */
  public void setParentWebApp(WebAppController parent)
  {
    _parentWebApp = parent;
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
    if (! prefix.startsWith("/"))
      prefix = "/" + prefix;
    
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
   * Initializes the controller.
   */
  public void init()
    throws Exception
  {
    if (_controller != null)
      return;

    String appDir = _config.getDocumentDirectory();

    if (appDir == null)
      appDir = "./" + _urlPrefix;

    if (_rootDirectory == null) {
      _rootDirectory = PathBuilder.lookupPath(appDir, null,
					      _container.getDocumentDirectory());
    }
    
    _controller = new WebAppController(_urlPrefix, _rootDirectory, _container);

    _controller.setArchivePath(_archivePath);

    if (_archivePath != null)
      _controller.addDepend(_archivePath);
    
    _controller.setParentWebApp(_parentWebApp);
    
    _controller.setConfig(_config);

    _controller.setSourceType("single");

    Environment.addEnvironmentListener(this, _parentLoader);
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    keys.add(_controller.getContextPath());
  }
  
  /**
   * Returns the current array of application entries.
   */
  public WebAppController generateController(String name)
  {
    if (name.equals(_controller.getContextPath()))
      return _controller;
    else
      return null;
  }
  
  /**
   * Merges the controllers.
   */
  public WebAppController mergeController(WebAppController controller,
					  String name)
  {
    // if directory matches, merge them
    if (controller.getRootDirectory().equals(_controller.getRootDirectory()))
      return _controller.merge(controller);
    // else if the names don't match, return the new controller
    else if (! _controller.isNameMatch(name))
      return controller;
    // otherwise, the single deploy overrides
    else
      return _controller;
  }

  /**
   * Initialize the deployment.
   */
  public void deploy()
  {
    try {
      init();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Destroy the deployment.
   */
  public void destroy()
  {
    _container.removeWebAppDeploy(this);
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public String toString()
  {
    return "WebAppDeploy[" + _urlPrefix + "]";
  }
}
