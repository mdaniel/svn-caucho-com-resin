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

package com.caucho.server.deploy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.JMException;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.config.Config;
import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;

import com.caucho.config.types.PathBuilder;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.loader.EnvironmentListener;

import com.caucho.util.L10N;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.server.deploy.ExpandDeployController;

import com.caucho.server.deploy.mbean.DeployControllerMBean;

/**
 * A deploy controller for an environment.
 */
abstract public class
  EnvironmentDeployController<I extends EnvironmentDeployInstance,
					C extends DeployConfig>
  extends ExpandDeployController<I>
  implements EnvironmentListener {
  
  private static final L10N L = new L10N(EnvironmentDeployController.class);
  private static final Logger log
    = Log.open(EnvironmentDeployController.class);
  
  // The JMX identity
  private LinkedHashMap<String,String> _jmxContext;

  private Object _mbean;

  private ObjectName _mbeanName;

  // The default configurations
  private ArrayList<C> _configDefaults =  new ArrayList<C>();
  
  // The primary configuration
  private C _config;

  // The configuration variable resolver
  private VariableResolver _parentVariableResolver;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // Config exception passed in from parent, e.g. .ear
  private Throwable _configException;

  public EnvironmentDeployController()
  {
    this("", (Path) null);
  }

  public EnvironmentDeployController(C config)
  {
    this(config.getId(), config.calculateRootDirectory());

    setConfig(config);
  }

  public EnvironmentDeployController(String id, C config)
  {
    this(id, config.calculateRootDirectory());

    setConfig(config);
  }

  public EnvironmentDeployController(String id, Path rootDirectory)
  {
    super(id, null, rootDirectory);
    
    _parentVariableResolver = EL.getEnvironment(getParentClassLoader());

    _jmxContext = Jmx.copyContextProperties(getParentClassLoader());
  }

  /**
   * Sets the primary configuration.
   */
  public void setConfig(C config)
  {
    if (config == null)
      return;

    if (_config != null && ! _configDefaults.contains(_config))
      addConfigDefault(_config);
    
    addConfigMode(config);
    
    _config = config;

    /* XXX: config is at the end
    if (config != null) {
      addConfigDefault(config);
    }
    */
  }

  /**
   * Gets the primary configuration
   */
  public C getConfig()
  {
    return _config;
  }

  /**
   * Returns the configure exception.
   */
  public Throwable getConfigException()
  {
    if (_configException != null)
      return _configException;

    DeployInstance deploy = getDeployInstance();

    if (deploy != null)
      return deploy.getConfigException();
    else
      return null;
  }

  /**
   * Adds a default config.
   */
  public void addConfigDefault(C config)
  {
    if (! _configDefaults.contains(config)) {
      _configDefaults.add(config);

      addConfigMode(config);
    }
  }

  private void addConfigMode(C config)
  {
    if (config.getStartupMode() != null)
      setStartupMode(config.getStartupMode());

    if (config.getRedeployMode() != null)
      setRedeployMode(config.getRedeployMode());
  }

  /**
   * Returns the path variable map.
   */
  public HashMap<String,Object> getVariableMap()
  {
    return _variableMap;
  }

  /**
   * Returns the mbean.
   */
  public Object getMBean()
  {
    return _mbean;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _mbeanName;
  }

  /**
   * Sets a parent config exception (e.g. from a .ear)
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Initialize the controller.
   */
  protected void initEnd()
  {
    super.initEnd();
    
    try {
      LinkedHashMap<String,String> properties;

      properties = Jmx.copyContextProperties(getParentClassLoader());

      _mbeanName = createObjectName(properties);

      if (_mbeanName != null) {
	_mbean = createMBean();
      
	Jmx.register(_mbean, _mbeanName);
      }
    } catch (Exception e) {
      // XXX: thrown?
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Creates the object name.  The default is to use getId() as
   * the 'name' property, and the classname as the 'type' property.
   */
  protected ObjectName createObjectName(Map<String,String> properties)
    throws MalformedObjectNameException
  {
    String type = getMBeanTypeName();

    properties.put("type", getMBeanTypeName());
    properties.put("name", getMBeanId());
      
    return Jmx.getObjectName("resin", properties);
  }

  /**
   * Creates the managed object.
   */
  protected Object createMBean()
    throws JMException
  {
    return new IntrospectionMBean(new DeployControllerAdmin(this),
				  DeployControllerMBean.class);
  }

  /**
   * Returns true if the entry matches.
   */
  public boolean isNameMatch(String url)
  {
    return url.equals(getId());
  }

  /**
   * Merges with the old controller.
   */
  protected void mergeController(DeployController oldControllerV)
  {
    super.mergeController(oldControllerV);

    EnvironmentDeployController<I,C> oldController;
    oldController = (EnvironmentDeployController) oldControllerV;
    // setId(oldController.getId());

    _configDefaults.addAll(oldController._configDefaults);

    if (getConfig() == null)
      setConfig(oldController.getConfig());
    else if (oldController.getConfig() != null) {
      _configDefaults.add(getConfig());
      
      setConfig(oldController.getConfig());
    }

    if (oldController.getArchivePath() != null)
      setArchivePath(oldController.getArchivePath());

    mergeStartupMode(oldController.getStartupMode());

    mergeRedeployMode(oldController.getRedeployMode());
  }
  
  /**
   * Returns the application object.
   */
  public boolean destroy()
  {
    if (! super.destroy())
      return false;
    
    Environment.removeEnvironmentListener(this, getParentClassLoader());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
    
      try {
	ObjectName mbeanName = _mbeanName;
	_mbeanName = null;

	if (mbeanName != null)
	  Jmx.unregister(mbeanName);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  /**
   * Configures the instance.
   */
  protected void configureInstance(I instance)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      ClassLoader classLoader = instance.getClassLoader();
      
      thread.setContextClassLoader(classLoader);

      log.fine(instance + " initializing");

      // set from external error, like .ear
      instance.setConfigException(_configException);
      
      HashMap<String,Object> varMap = new HashMap<String,Object>();
      varMap.putAll(_variableMap);

      VariableResolver variableResolver
	= new MapVariableResolver(varMap, _parentVariableResolver);

      EL.setVariableMap(varMap, classLoader);
      EL.setEnvironment(variableResolver, classLoader);

      configureInstanceVariables(instance);

      _jmxContext.put(getMBeanTypeName(), getMBeanId());
	  
      Jmx.setContextProperties(_jmxContext, classLoader);

      try {
	String typeName = "Current" + getMBeanTypeName();
	
	Jmx.register(getMBean(),
		     new ObjectName("resin:type=" + typeName),
		     classLoader);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }

      ArrayList<DeployConfig> initList = new ArrayList<DeployConfig>();

      fillInitList(initList);

      thread.setContextClassLoader(instance.getClassLoader());
      Vfs.setPwd(getRootDirectory());

      if (getArchivePath() != null)
	Environment.addDependency(getArchivePath());

      for (DeployConfig config : initList) {
	BuilderProgram program = config.getBuilderProgram();

	if (program != null)
	  program.configure(instance);
      }

      instance.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected void fillInitList(ArrayList<DeployConfig> initList)
  {
    initList.addAll(_configDefaults);

    if (_config != null && ! initList.contains(_config))
      initList.add(_config);
  }

  protected void configureInstanceVariables(I instance)
    throws Throwable
  {
    Path rootDirectory = getRootDirectory();
    
    if (rootDirectory == null)
      throw new NullPointerException("Null root directory");

    if (! rootDirectory.isFile()) {
    }
    else if (rootDirectory.getPath().endsWith(".jar") ||
	     rootDirectory.getPath().endsWith(".war")) {
      throw new ConfigException(L.l("root-directory `{0}' must specify a directory.  It may not be a .jar or .war.",
				    rootDirectory.getPath()));
    }
    else
      throw new ConfigException(L.l("root-directory `{0}' may not be a file.  root-directory must specify a directory.",
				    rootDirectory.getPath()));
    Vfs.setPwd(rootDirectory);

    if (log.isLoggable(Level.FINE))
      log.fine(instance + " root-directory=" + rootDirectory);

    instance.setRootDirectory(rootDirectory);
  }

  public Path getArchivePath()
  {
    Path path = super.getArchivePath();

    if (path != null)
      return path;
    
    if (_config != null) {
      String pathString = _config.getArchivePath();
      
      if (pathString != null) {
	try {
	  path = PathBuilder.lookupPath(pathString);
	} catch (ELException e) {
	  throw new RuntimeException(e);
	}
      }

      setArchivePath(path);
    }

    return path;
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }
  
  /**
   * Returns a printable view.
   */
  public String toString()
  {
    String name = getClass().getName();

    name = name.substring(name.lastIndexOf('.') + 1);
    
    return name + "" + System.identityHashCode(this) + "[" + getId() + "]";
  }
}
