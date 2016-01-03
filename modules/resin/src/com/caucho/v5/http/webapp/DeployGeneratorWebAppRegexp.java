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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.caucho.v5.config.types.PathBuilder;
import com.caucho.v5.deploy.DeployContainerService;
import com.caucho.v5.deploy.DeployGenerator;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.vfs.PathImpl;

/**
 * The generator for the web-app deploy
 */
public class DeployGeneratorWebAppRegexp
  extends DeployGenerator<WebApp,WebAppController>
{
  private static final Logger log
    = Logger.getLogger(DeployGeneratorWebAppSingle.class.getName());

  private WebAppContainer _container;
  
  private WebAppController _parent;

  private WebAppConfig _config;
  
  private ArrayList<WebAppConfig> _webAppDefaults =
    new ArrayList<WebAppConfig>();

  private ArrayList<WebAppController> _entries =
    new ArrayList<WebAppController>();

  /**
   * Creates the new host deploy.
   */
  public DeployGeneratorWebAppRegexp(DeployContainerService<WebApp,WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new host deploy.
   */
  public DeployGeneratorWebAppRegexp(DeployContainerService<WebApp,WebAppController> deployContainer,
                            WebAppContainer container,
                            WebAppConfig config)
  {
    super(deployContainer);
    
    setContainer(container);

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
  }
  /**
   * Sets the parent webApp.
   */
  public void setParent(WebAppController parent)
  {
    _parent = parent;
  }
  
  /**
   * Returns the current array of webApp entries.
   */
  @Override
  public void generateController(String name, ArrayList<WebAppController> list)
  {
    Pattern regexp = _config.getURLRegexp();
    Matcher matcher = regexp.matcher(name);

    if (! matcher.find() || matcher.start() != 0) {
      return;
    }

    int length = matcher.end() - matcher.start();

    String contextPath = matcher.group();
        
    ArrayList<String> vars = new ArrayList<String>();

    //WebAppDeployControlleryController entry = new WebAppDeployControlleryController(this, contextPath);
    HashMap<String,Object> varMap = new HashMap<String,Object>();
    // entry.getVariableMap();
        
    for (int j = 0; j <= matcher.groupCount(); j++) {
      vars.add(matcher.group(j));
      varMap.put("app" + j, matcher.group(j));
    }

    varMap.put("regexp", vars);

    PathImpl appDir = null;
    
    try {
      String appDirPath = _config.getRootDirectory();

      if (appDirPath == null)
        appDirPath = "./" + matcher.group(0);
      
      appDir = PathBuilder.lookupPath(appDirPath, varMap);

      if (! appDir.isDirectory() || ! appDir.canRead()) {
        return;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return;
    }

    WebAppController controller = null;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      synchronized (_entries) {
        for (int i = 0; i < _entries.size(); i++) {
          controller = _entries.get(i);

          if (appDir.equals(controller.getRootDirectory())) {
            list.add(controller);
            return;
          }
        }
        
        String stage = _container.getHttpContainer().getClusterName();
        String hostId = _container.getHost().getIdTail();
        
        String id;
        
        if (name.startsWith("/")) {
          id = "webapps/" + hostId + name;
        }
        else {
          id = "webapps/" + hostId + "/" + name;
        }

        // DeployHandle<WebApp> handle = _container.createHandle(id);
        
        controller = new WebAppController(id, appDir, _container, name);

        // XXX: not dynamic-deploy in the sense that the mappings are known
        //controller.setDynamicDeploy(true);
        controller.getVariableMap().putAll(varMap);
        controller.setRegexpValues(vars);
        controller.setConfig(_config);
        // _controller.setJarPath(_archivePath);

        for (int i = 0; i < _webAppDefaults.size(); i++)
          controller.addConfigDefault(_webAppDefaults.get(i));
      
        _entries.add(controller);
        
        
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    controller.setSourceType("regexp");
    
    //controller.deploy();

    list.add(controller);
  }

  public String toString()
  {
    if (_config == null)
      return "WebAppRegexpDeployGenerator[]";
    else
      return "WebAppRegexpDeployGenerator[" + _config.getURLRegexp().pattern() + "]";
  }
}
