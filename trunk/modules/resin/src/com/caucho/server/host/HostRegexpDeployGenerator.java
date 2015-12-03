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
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The generator for the web-app deploy
 */
public class HostRegexpDeployGenerator extends DeployGenerator<HostController> {
  private static final Logger log
    = Logger.getLogger(HostSingleDeployGenerator.class.getName());

  private HostContainer _container;

  private HostConfig _config;
  
  private ArrayList<HostConfig> _hostDefaults
    = new ArrayList<HostConfig>();

  private ArrayList<HostController> _entries
    = new ArrayList<HostController>();

  /**
   * Creates the new host deploy.
   */
  public HostRegexpDeployGenerator(DeployContainer<HostController> container)
  {
    super(container);
  }

  /**
   * Creates the new host deploy.
   */
  public HostRegexpDeployGenerator(DeployContainer<HostController> container,
                                   HostContainer hostContainer,
                                   HostConfig config)
  {
    super(container);
    
    setContainer(hostContainer);

    _config = config;
  }

  /**
   * Gets the application container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the application container.
   */
  public void setContainer(HostContainer container)
  {
    _container = container;
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedNames(Set<String> names)
  {
    Pattern regexp = _config.getRegexp();
    
    String pattern = regexp.pattern();
    
    String staticPattern = toStaticPattern(pattern);
    
    if (staticPattern != null) {
      names.add(staticPattern);
    }
  }
  
  /**
   * Returns the current array of application entries.
   */
  @Override
  public void generateController(String name, ArrayList<HostController> list)
  {
    Pattern regexp = _config.getRegexp();
    Matcher matcher = regexp.matcher(name);

    if (! matcher.matches()) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      String hostName = matcher.group();

      ArrayList<String> vars = new ArrayList<String>();

      HashMap<String,Object> varMap = new HashMap<String,Object>();
        
      for (int j = 0; j <= matcher.groupCount(); j++) {
        vars.add(matcher.group(j));
        varMap.put("host" + j, matcher.group(j));
      }

      varMap.put("regexp", vars);
      
      varMap.put("host", new HostRegexpVar(hostName, vars));

      if (_config.getHostName() != null) {
        try {
          hostName = Config.evalString(_config.getHostName(), varMap);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      String id = _container.getServer().getStage() + "/host/" + name;
      Path rootDirectory = _config.calculateRootDirectory(varMap);
      
      HostController controller
        = new HostController(id, rootDirectory, name,
                             _config, _container, varMap);

      controller.setRegexpName(name);

      controller.setRegexp(regexp);
      controller.setRootDirectoryPattern(_config.getRootDirectory());

      // XXX: not dynamic-deploy in the sense that the mappings are known
      //controller.setDynamicDeploy(true);
      //controller.setRegexpValues(vars);
      //controller.setHostConfig(_config);
      // _controller.setJarPath(_archivePath);

      for (int i = 0; i < _hostDefaults.size(); i++)
        controller.addConfigDefault(_hostDefaults.get(i));

      controller.init();
    
      Path rootDir = controller.getRootDirectory();

      if (rootDir == null || ! rootDir.isDirectory()) {
        // server/0522
        controller.destroy();
        return;
      }

      synchronized (_entries) {
        for (int i = 0; i < _entries.size(); i++) {
          HostController oldController = _entries.get(i);

          if (rootDir.equals(oldController.getRootDirectory())) {
            list.add(oldController);
          }
        }
      
        _entries.add(controller);
      }

      // registers mbean
      /*
      try {
        controller.deployHost();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      list.add(controller);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private static String toStaticPattern(String pattern)
  {
    StringBuilder sb = new StringBuilder();
    
    int len = pattern.length();
    
    for (int i = 0; i < len; i++) {
      char ch = pattern.charAt(i);
      
      switch (ch) {
      case '\\':
        if (i + 1 < len && pattern.charAt(i + 1) == '.') {
          sb.append('.');
          i++;
        }
        else {
          return null;
        }
        
      case '*': case '?': case '+': case '|': 
      case '(': case ')':
      case '[': case ']': 
      case '{': case '}':
        return null;
        
      case '^': case '$':
        break;
        
      default:
        sb.append(ch);
        break;
      }
    }
    
    return sb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _config + "]";
  }
}
