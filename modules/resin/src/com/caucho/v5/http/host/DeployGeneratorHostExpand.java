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

package com.caucho.v5.http.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.deploy.DeployContainerService;
import com.caucho.v5.deploy.DeployControllerType;
import com.caucho.v5.deploy.DeployGeneratorExpand;
import com.caucho.v5.deploy.DeployMode;
import com.caucho.v5.vfs.PathImpl;

/**
 * The generator for the host deploy
 */
public class DeployGeneratorHostExpand
  extends DeployGeneratorExpand<Host,HostController>
{
  private static final Logger log
    = Logger.getLogger(DeployGeneratorHostExpand.class.getName());

  private final DeployGeneratorHostExpandAdmin _admin
    = new DeployGeneratorHostExpandAdmin(this);
  
  private HostDeployWebAppListener _webappListener;
  // private HostDeployServiceListener _serviceListener;

  private HostContainer _container;
  
  private ArrayList<HostConfig> _hostDefaults = new ArrayList<HostConfig>();

  private String _hostName;

  /**
   * Creates the new host deploy.
   */
  public DeployGeneratorHostExpand(String id,
                                   DeployContainerService<Host,HostController> container,
                                   HostContainer hostContainer)
  {
    super(id, container, hostContainer.getRootDirectory());

    _container = hostContainer;
  }

  /**
   * Gets the host container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the host name.
   */
  public void setHostName(RawString name)
  {
    _hostName = name.getValue();
  }

  /**
   * Gets the host name.
   */
  public String getHostName()
  {
    return _hostName;
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
   * Adds a default.
   */
  public void addHostDefault(HostConfig config)
  {
    _hostDefaults.add(config);
  }

  @Override
  protected void initImpl()
    throws ConfigException
  {
    super.initImpl();

    _webappListener = new HostDeployWebAppListener(this);
    // _serviceListener = new HostDeployServiceListener(this);
  }

  @Override
  protected void startImpl()
    throws ConfigException
  {
    super.startImpl();
    
    _admin.register();
  }

  /**
   * Returns the log.
   */
  @Override
  protected Logger getLog()
  {
    return log;
  }
  
  @Override
  protected void beforeUpdate()
  {
    _webappListener.update();
    // _serviceListener.update();
  }

  /**
   * Returns the current array of application entries.
   */
  @Override
  public HostController createController(String key)
  {
    // String key = version.getKey();
    
    /*
    // server/13g3
    if (name.equals(""))
      return null;
      */
    
    /*
    if (! isDeployedKey(name))
      return null;
    */
    
    PathImpl rootDirectory = getExpandPath(key);

    String hostName = keyToName(key);

    // String cluster = _container.getServer().getClusterName();
    String id = "host/" + key;
    
    String hostNamePattern = getHostName();
    
    HashMap<String,Object> varMap = null;

    if (hostNamePattern != null && ! key.equals(Host.DEFAULT_NAME)) {
      varMap = new HashMap<String,Object>();
      varMap.put("host", new HostRegexpVar(key));
      
      //ELResolver resolver = new MapVariableResolver(varMap);

      //ELContext env = new ConfigELContext(resolver);

      hostName = ConfigContext.evalString(hostNamePattern);
    }

    HostController controller
      = _container.createController(id, rootDirectory, hostName, null, varMap);
    
    controller.setControllerType(DeployControllerType.DYNAMIC);
    
    /*
    */
    
    for (HostConfig hostDefault : _hostDefaults) {
      controller.addConfigDefault(hostDefault);
    }

    PathImpl jarPath = getArchivePath(key);
    controller.setArchivePath(jarPath);
    
    /*
    if (rootDirectory.isDirectory()
        && ! isValidDirectory(rootDirectory, name))
      return null;
    else if (! rootDirectory.isDirectory()
             && ! jarPath.isFile())
      return null;
      */

    return controller;
  }

  
  /**
   * Adds configuration to the current controller
   */
  @Override
  protected void mergeController(HostController controller,
                                 String key)
  {
    try {
      PathImpl expandDirectory = getExpandDirectory();
      PathImpl rootDirectory = controller.getRootDirectory();

      if (! expandDirectory.equals(rootDirectory.getParent())) {
        return;
      }

      super.mergeController(controller, key);
      
      controller.setStartupMode(getStartupMode());
    
      for (int i = 0; i < _hostDefaults.size(); i++)
        controller.addConfigDefault(_hostDefaults.get(i));
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);
      
      controller.setConfigException(e);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      controller.setConfigException(e);
    }
  }
  
  @Override
  public String nameToKey(String name)
  {
    if (name.isEmpty())
      return Host.DEFAULT_NAME;
    else
      return name;
  }
  
  @Override
  public String keyToName(String key)
  {
    if (key.equals(Host.DEFAULT_NAME)) {
      return "";
    }
    else {
      return key;
    }
  }

  @Override
  protected void destroyImpl()
  {
    _admin.unregister();

    super.destroyImpl();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployGeneratorHostExpand deploy = (DeployGeneratorHostExpand) o;

    PathImpl expandPath = getExpandDirectory();
    PathImpl deployExpandPath = deploy.getExpandDirectory();
    if (expandPath != deployExpandPath &&
        (expandPath == null || ! expandPath.equals(deployExpandPath)))
      return false;

    return true;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpandDirectory() + "]";
  }
}
