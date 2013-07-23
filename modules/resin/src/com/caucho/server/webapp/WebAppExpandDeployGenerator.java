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

package com.caucho.server.webapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployControllerType;
import com.caucho.env.deploy.DeployMode;
import com.caucho.env.deploy.ExpandDeployGenerator;
import com.caucho.env.deploy.ExpandVersion;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.vfs.Path;

/**
 * The generator for the web-app deploy
 */
public class WebAppExpandDeployGenerator
  extends ExpandDeployGenerator<WebAppController>
  implements EnvironmentListener
{
  private static final Logger log
    = Logger.getLogger(WebAppExpandDeployGenerator.class.getName());

  private final WebAppExpandDeployGeneratorAdmin _admin;

  private WebAppContainer _container;

  private WebAppController _parent;

  private String _urlPrefix = "";

  private ArrayList<WebAppConfig> _webAppDefaults
    = new ArrayList<WebAppConfig>();

  private HashMap<Path,WebAppConfig> _webAppConfigMap
    = new HashMap<Path,WebAppConfig>();

  // Maps from the context-path to the webapps directory
  private HashMap<String,Path> _contextPathMap
    = new HashMap<String,Path>();
  
  private HashMap<String,String> _nameToKeyMap
    = new HashMap<String,String>();

  private ClassLoader _parentLoader;

  /**
   * Creates the new expand deploy.
   */
  public WebAppExpandDeployGenerator(String id,
                                     DeployContainer<WebAppController> container,
                                     WebAppContainer webAppContainer)
  {
    super(id, container, webAppContainer.getRootDirectory());

    _container = webAppContainer;

    _parentLoader = webAppContainer.getClassLoader();

    try {
      setExtension(".war");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    setEntryNamePrefix("/");

    _admin = new WebAppExpandDeployGeneratorAdmin(this);
  }
  
  @Override
  public String getId()
  {
    return super.getId() + _urlPrefix; 
  }

  /**
   * Gets the webApp container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the parent webApp.
   */
  public void setParent(WebAppController parent)
  {
    _parent = parent;
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
    if (prefix.equals("")) {
    }

    while (prefix.endsWith("/"))
      prefix = prefix.substring(0, prefix.length() - 1);

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
   * Sets true for a lazy-init.
   */
  public void setLazyInit(boolean lazyInit)
    throws ConfigException
  {
    log.config("lazy-init is deprecated.  Use <startup>lazy</startup> instead.");
    if (lazyInit)
      setStartupMode(DeployMode.LAZY);
    else
      setStartupMode(DeployMode.AUTOMATIC);
  }

  /**
   * Adds an overriding web-app
   */
  public void addWebApp(WebAppConfig config)
  {
    String docDir = config.getRootDirectory();

    Path appDir = getExpandDirectory().lookup(docDir);

    _webAppConfigMap.put(appDir, config);

    if (config.getContextPath() != null) {
      _contextPathMap.put(config.getContextPath(), appDir);
      
      String tail = appDir.getTail();
      
      _nameToKeyMap.put(config.getContextPath(), tail);
    }
  }

  /**
   * Adds a default.
   */
  public void addWebAppDefault(WebAppConfig config)
  {
    _webAppDefaults.add(config);
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
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedNames(Set<String> keys)
  {
    super.fillDeployedNames(keys);

    for (WebAppConfig cfg : _webAppConfigMap.values()) {
      if (cfg.getContextPath() != null)
        keys.add(cfg.getContextPath());
    }
  }

  /**
   * Start the deploy.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();

    Environment.addEnvironmentListener(this, _parentLoader);

    _admin.register();
  }

  /**
   * Returns the new controller.
   */
  @Override
  protected WebAppController createController(ExpandVersion version)
  {
    String key = version.getKey();

    String baseKey = version.getBaseKey();
    String contextPath = keyToName(baseKey);
    
    Path rootDirectory = getExpandPath(key);
    
    if (rootDirectory == null)
      throw new NullPointerException();
    
    if (isVersioning()) {
      WebAppVersioningController controller
        = new WebAppVersioningController(getId() + "/" + baseKey,
                                         baseKey,
                                         contextPath,
                                         this,
                                         _container);
      controller.setControllerType(DeployControllerType.VERSIONING);
    
      return controller;
    }
    else
      return createVersionController(version);
  }
  
  
  /**
   * Returns the new controller.
   */
  public WebAppController createVersionController(ExpandVersion version)
  {
    String key = version.getKey();

    String baseKey = version.getBaseKey();
    String contextPath = keyToName(baseKey);
    
    Path rootDirectory = getExpandPath(key);
    Path archivePath = getArchivePath(key);
 
    String id = getId() + "/" + key;

    WebAppController controller
      = new WebAppController(id, rootDirectory, _container,
                             contextPath);

    controller.setArchivePath(archivePath);

    controller.setWarName(key);

    controller.setParentWebApp(_parent);

    controller.setDynamicDeploy(true);
    controller.setSourceType("expand");
    controller.setControllerType(DeployControllerType.DYNAMIC);
    
    controller.setStartupMode(getStartupMode());
    controller.setRedeployMode(getRedeployMode());

    controller.setVersion(version.getVersion());
    
    // server/1h82 vs server/1h20
    // controller.init();

    return controller;
  }
  
  @Override
  protected void afterUpdate()
  {
    _container.clearCache();
  }

  /**
   * Returns the current array of webApp entries.
   */
  @Override
  protected void mergeController(WebAppController controller,
                                             String key)
  {
    try {
      Path expandDirectory = getExpandDirectory();
      Path rootDirectory = controller.getRootDirectory();

      if (! expandDirectory.equals(rootDirectory.getParent())) {
        return;
      }

      super.mergeController(controller, key);

      if (controller.getArchivePath() == null) {
        String archiveName = rootDirectory.getTail() + ".war";

        Path jarPath = getArchiveDirectory().lookup(archiveName);

        if (! jarPath.isDirectory()) {
          controller.setArchivePath(jarPath);
          controller.addDepend(jarPath);
        }
      }

      controller.setStartupMode(getStartupMode());
      // controller.setRedeployMode(getRedeployMode());

      for (int i = 0; i < _webAppDefaults.size(); i++) {
        controller.addConfigDefault(_webAppDefaults.get(i));
      }

      WebAppConfig cfg = _webAppConfigMap.get(rootDirectory);

      if (cfg != null) {
        // server/1h11
        if (cfg.getContextPath() != null)
          controller.setContextPath(cfg.getContextPath());

        controller.addConfigDefault(cfg);
      }
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINEST, e.toString(), e);
      controller.setConfigException(e);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      controller.setConfigException(e);
    }
  }
  
  @Override
  protected String keyToName(String key)
  {
    if (key.equalsIgnoreCase("root"))
      return _urlPrefix;
    else
      return _urlPrefix + "/" + key;
  }
  
  @Override
  protected String nameToKey(String name)
  {
    if (! name.startsWith(_urlPrefix))
      return null;
    
    // server/1h86
    String key = _nameToKeyMap.get(name);
    
    if (key != null)
      return key;
    
    String tail = name.substring(_urlPrefix.length());
    
    if (tail.startsWith("/"))
      tail = tail.substring(1);
    
    if (tail.equals(""))
      key = "ROOT";
    else
      key = tail;
    
    return key;
  }

  /**
   * Destroy the deployment.
   */
  @Override
  protected void destroyImpl()
  {
    _admin.unregister();

    _container.removeWebAppDeploy(this);

    Environment.removeEnvironmentListener(this, _parentLoader);

    super.destroyImpl();
  }
}
