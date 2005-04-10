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

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.Alarm;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.el.EL;

import com.caucho.config.ConfigException;

import com.caucho.config.types.RawString;

import com.caucho.server.deploy.ExpandDeployGenerator;
import com.caucho.server.deploy.DeployContainer;

/**
 * The generator for the web-app deploy
 */
public class WebAppExpandDeployGenerator extends ExpandDeployGenerator<WebAppController>
  implements EnvironmentListener {
  private static final Logger log = Log.open(WebAppExpandDeployGenerator.class);

  private ApplicationContainer _container;
  
  private WebAppController _parent;

  private String _urlPrefix = "";

  private ArrayList<WebAppConfig> _webAppDefaults =
    new ArrayList<WebAppConfig>();

  private HashMap<Path,WebAppConfig> _webAppConfigMap =
    new HashMap<Path,WebAppConfig>();

  private ClassLoader _parentLoader;

  private boolean _isActive;

  /**
   * Creates the new expand deploy.
   */
  public WebAppExpandDeployGenerator(DeployContainer<WebAppController> container)
  {
    super(container);
    
    try {
      setExtension(".war");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
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
      setStartupMode("lazy");
    else
      setStartupMode("automatic");
  }

  /**
   * Adds an overriding web-app
   */
  public void addWebApp(WebAppConfig config)
  {
    String docDir = config.getDocumentDirectory();

    Path appDir = getExpandDirectory().lookup(docDir);

    _webAppConfigMap.put(appDir, config);
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
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Start the deploy.
   */
  public void start()
  {
    Environment.addEnvironmentListener(this, _parentLoader);
    
    super.start();
  }
  
  /**
   * Returns the current array of application entries.
   */
  protected WebAppController createController(String name)
  {
    if (! name.startsWith(_urlPrefix))
      return null;

    String segmentName = name.substring(_urlPrefix.length());

    if (segmentName.equals(""))
      segmentName = "/ROOT";

    String expandName = getExpandPrefix() + segmentName.substring(1);

    String archiveName = segmentName + ".war";
    Path jarPath = getArchiveDirectory().lookup("." + archiveName);

    Path rootDirectory;

    if (jarPath.isDirectory()) {
      rootDirectory = getExpandDirectory().lookup("." + archiveName);
      jarPath = null;
    }
    else
      rootDirectory = getExpandDirectory().lookup(expandName);

    WebAppConfig cfg = _webAppConfigMap.get(rootDirectory);

    if (cfg != null && cfg.getContextPath() != null)
      name = cfg.getContextPath();

    WebAppController controller
      = new WebAppController(name, rootDirectory, _container);

    try {
      controller.setStartupMode(getStartupMode());
      // controller.setRedeployMode(getRedeployMode());

      controller.setParentWebApp(_parent);

      if (jarPath != null) {
	controller.setArchivePath(jarPath);
	controller.addDepend(jarPath);
      }

      controller.setDynamicDeploy(true);
      controller.setSourceType("expand");

      for (int i = 0; i < _webAppDefaults.size(); i++)
	controller.addConfigDefault(_webAppDefaults.get(i));

      if (cfg != null)
	controller.addConfigDefault(cfg);
    } catch (ConfigException e) {
      controller.setConfigException(e);

      log.log(Level.FINER, e.toString(), e);
      log.warning(e.toString());
    } catch (Throwable e) {
      controller.setConfigException(e);

      log.log(Level.WARNING, e.toString(), e);
    }

    return controller;
  }

  /**
   * Converts the name.
   */
  protected String pathNameToEntryName(String name)
  {
    String entryName = super.pathNameToEntryName(name);

    if (entryName == null)
      return null;

    if (entryName.equalsIgnoreCase("root"))
      return _urlPrefix;
    else
      return _urlPrefix + "/" + entryName;
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    _isActive = true;
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  /**
   * Destroy the deployment.
   */
  public void destroy()
  {
    _isActive = false;

    _container.removeWebAppDeploy(this);
    Environment.removeEnvironmentListener(this, _parentLoader);
    
    super.destroy();
  }

  public String toString()
  {
    return "WebAppExpandDeployGenerator[" + getExpandDirectory() + "]";
  }
}
