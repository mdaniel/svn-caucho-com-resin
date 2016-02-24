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
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.types.PathBuilder;
import com.caucho.v5.deploy.DeployContainerService;
import com.caucho.v5.deploy.DeployGenerator;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvLoaderListener;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

/**
 * The generator for the web-app deploy
 */
public class DeployGeneratorWebAppSingle
  extends DeployGenerator<WebAppResinBase,WebAppController>
  implements EnvLoaderListener
{
  private static final L10N L = new L10N(DeployGeneratorWebAppSingle.class);
  private static final Logger log
    = Logger.getLogger(DeployGeneratorWebAppSingle.class.getName());

  private WebAppContainer _container;
  
  // private WebAppController _parentWebApp;

  private String _urlPrefix = "";

  private PathImpl _archivePath;
  private PathImpl _rootDirectory;

  private ArrayList<WebAppConfig> _defaultList = new ArrayList<WebAppConfig>();
  private WebAppConfig _config;
  
  private ClassLoader _parentLoader;

  private WebAppController _controller;

  /**
   * Creates the new host deploy.
   */
  public DeployGeneratorWebAppSingle(DeployContainerService<WebAppResinBase,WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new web-app deploy.
   */
  public DeployGeneratorWebAppSingle(DeployContainerService<WebAppResinBase,WebAppController> deployContainer,
                                     WebAppContainer container,
                                     WebAppConfig config)
  {
    super(deployContainer);
    
    setContainer(container);

    String contextPath = config.getContextPath();

    if (contextPath.equals("/")) {
      contextPath = "";
    }
    
    setURLPrefix(config.getContextPath());

    _config = config;
  }

  /**
   * Gets the webApp container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the webApp container.
   */
  public void setContainer(WebAppContainer container)
  {
    _container = container;

    if (_parentLoader == null)
      _parentLoader = container.getClassLoader();
  }
  
  /**
   * Sets the parent webApp.
   */
  /*
  public void setParentWebApp(WebAppController parent)
  {
    _parentWebApp = parent;
  }
  */

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
  public void setRootDirectory(PathImpl rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Adds a default.
   */
  public void addWebAppDefault(WebAppConfig config)
  {
    _defaultList.add(config);
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
   * Initializes the controller.
   */
  @Override
  protected void initImpl()
  {
    super.initImpl();

    if (_controller != null)
      return;

    String rootDir = _config.getRootDirectory();
    String archivePath = _config.getArchivePath();

    if (archivePath != null) {
      _archivePath = PathBuilder.lookupPath(archivePath, null,
                                            _container.getRootDirectory());
    }

    if (_rootDirectory == null) {
      if (rootDir != null) {
      }
      else if (_archivePath != null
               && (_urlPrefix.equals("/") || _urlPrefix.equals(""))
               && _container != null) {
        log.warning(L.l("web-app's root-directory '{0}' must be outside of the '{1}' root-directory when using 'archive-path",
                        _rootDirectory, _container));

        rootDir = "./ROOT";
      }
      else
        rootDir = "./" + _urlPrefix;
      
      _rootDirectory = PathBuilder.lookupPath(rootDir, null,
                                              _container.getRootDirectory());
    }
    
    String id = WebAppController.calculateId(_container, _urlPrefix);
    
    _controller = _container.createWebAppController(id,
                                                    _rootDirectory, 
                                                    _urlPrefix);

    _controller.setArchivePath(_archivePath);

    if (_archivePath != null)
      _controller.addDepend(_archivePath);
    
    // _controller.setParentWebApp(_parentWebApp);

    for (WebAppConfig config : _defaultList) {
      _controller.addConfigDefault(config);
    }
    
    // server/1h13 vs server/2e00
    _controller.setConfig(_config);
    
    _controller.initConfig(_config);
    // _controller.addConfigDefault(_config);

    _controller.setSourceType("single");

    EnvLoader.addEnvironmentListener(this, _parentLoader);

    // server/1d02
    //_controller.init();
    
    if (! isDeployed()) {
      log.warning(_controller + " does not have an active root-directory "
                  + _controller.getRootDirectory());
    }
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedNames(Set<String> keys)
  {
    if (! isDeployed())
      return;
    
    if (_controller != null)
      keys.add(_controller.getContextPath());
  }
  
  private boolean isDeployed()
  {
    if (_controller == null)
      return false;
    
    // server/1d--
    return true;
    
    /*
    if (_controller.getRootDirectory().exists())
      return true;
    else if (_controller.getArchivePath() == null)
      return false;
    else
      return _controller.getArchivePath().canRead();
      */
  }
  
  /**
   * Creates a controller given the name
   */
  @Override
  public void generateController(String name, ArrayList<WebAppController> list)
  {
    if (! isDeployed()) {
      return;
    }
    
    if (name.equals(_controller.getContextPath())) {
      WebAppController webApp;
      
      webApp = new WebAppController(_controller.getId(),
                                    _rootDirectory, 
                                    _container,
                                    _urlPrefix);

      //webApp.setArchivePath(_controller.getArchivePath());
      //webApp.setStartupPriority(_controller.getStartupPriority());
      
      // server/12ab
      webApp.merge(_controller);
      webApp.setControllerType(_config.getControllerType());

      list.add(webApp);
    }
  }
  
  /**
   * Merges the controllers.
   */
  @Override
  public void mergeController(WebAppController controller, String name)
  {
    if (controller.getRootDirectory().equals(_controller.getRootDirectory())
        || _controller.isNameMatch(name)) {
      // if directory matches, merge the two controllers.  The
      // last controller has priority.
      // server/1h10, server/1d90
      controller.setContextPath(_controller.getContextPath());
      
      controller.setDynamicDeploy(false);
      
      // server/1h12
      //controller.merge(_controller);
    }
    else if (! _controller.isNameMatch(name)) {
      // else if the names don't match, return the new controller
    }
    else {
      // otherwise, the single deploy overrides
      // return _controller;
    }
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

  @Override
  public Throwable getConfigException()
  {
    Throwable configException =   super.getConfigException();

    if (configException == null && _controller != null)
      configException = _controller.getConfigException();

    return configException;
  }

  /**
   * Destroy the deployment.
   */
  @Override
  protected void destroyImpl()
  {
    EnvLoader.removeEnvironmentListener(this, _parentLoader);

    _container.removeWebAppDeploy(this);

    super.destroyImpl();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _urlPrefix + "]";
  }
}
