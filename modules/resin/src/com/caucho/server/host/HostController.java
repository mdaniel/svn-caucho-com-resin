
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

package com.caucho.server.host;

import java.util.*;
import java.util.LinkedHashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import javax.management.ObjectName;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.CompileException;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Depend;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.config.NodeBuilder;
import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.Config;
import com.caucho.config.types.PathBuilder;

import com.caucho.make.Dependency;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.server.webapp.WebAppConfig;

import com.caucho.server.deploy.ExpandDeployController;

import com.caucho.server.host.mbean.HostMBean;

/**
 * A configuration entry for a host
 */
class HostController extends ExpandDeployController<Host> {
  private static final Logger log = Log.open(HostController.class);
  private static final L10N L = new L10N(HostController.class);
  
  private HostContainer _container;

  // The host name is the canonical name
  private String _hostName;
  // The regexp name is the matching name of the regexp
  private String _regexpName;

  // Any host aliases.
  private ArrayList<String> _entryHostAliases = new ArrayList<String>();
  
  private ArrayList<String> _hostAliases = new ArrayList<String>();

  // The configuration
  private HostConfig _config;

  private ArrayList<HostConfig> _hostDefaults = new ArrayList<HostConfig>();

  private boolean _isInit;
  private BuilderProgram _initProgram;

  private ObjectName _mbeanName;
  private Object _mbean;

  private VariableResolver _variableResolver;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // The host variables.
  private Var _hostVar = new Var();

  // root-dir as set by the resin.conf
  private Path _cfgRootDir;
  // root-dir as set by the resin.conf
  private Path _jarRootDir;
    
  // private Host _host;
  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();

  private long _startTime;

  HostController(HostContainer container, HostConfig config)
  {
    super(container != null ? container.getClassLoader() : null);
    
    _container = container;
    _config = config;
    
    if (config != null)
      setStartupMode(config.getStartupMode());

    VariableResolver parentResolver = Config.getEnvironment();
    _variableResolver = new MapVariableResolver(_variableMap, parentResolver);

    _variableMap.put("host", _hostVar);
  }

  /**
   * Returns the Resin host name.
   */
  public String getName()
  {
    String name = super.getName();
    
    if (name != null)
      return name;
    else
      return getHostName();
  }

  /**
   * Sets the Resin host name.
   */
  public void setName(String name)
  {
    name = name.toLowerCase();
    
    _variableMap.put("name", name);

    super.setName(name);
  }

  /**
   * Returns the host's canonical name
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Sets the host's canonical name
   */
  public void setHostName(String name)
  {
    if (name != null)
      name = name.trim();
    
    if (name == null || name.equals("*"))
      name = "";
    
    name = name.toLowerCase();

    _hostName = name;
  }

  /**
   * Returns the host's canonical name
   */
  public void setRegexpName(String name)
  {
    _regexpName = name.toLowerCase();
  }
  
  /**
   * Adds a host alias.
   */
  public void addHostAlias(String name)
  {
    if (name != null)
      name = name.trim();
    
    if (name == null || name.equals("*"))
      name = ""; // XXX: default?
    
    name = name.toLowerCase();

    if (! _entryHostAliases.contains(name))
      _entryHostAliases.add(name);

    addExtHostAlias(name);
  }

  /**
   * Adds an extension host alias, e.g. from a resin:import
   */
  public void addExtHostAlias(String name)
  {
    if (! _hostAliases.contains(name))
      _hostAliases.add(name);
  }

  /**
   * Returns the host aliases.
   */
  public ArrayList<String> getHostAliases()
  {
    return _hostAliases;
  }

  /**
   * Gets the HostConfig
   */
  public HostConfig getHostConfig()
  {
    return _config;
  }

  /**
   * Returns the path variable map.
   */
  public HashMap<String,Object> getVariableMap()
  {
    return _variableMap;
  }

  /**
   * Adds to the var map
   */
  public void addVariableMap(HashMap<String,Object> map)
  {
    _variableMap.putAll(map);
  }

  /**
   * Returns the path variable map.
   */
  public VariableResolver getVariableResolver()
  {
    return _variableResolver;
  }

  /**
   * Returns the host's resin.conf configuration node.
   */
  public BuilderProgram getInitProgram()
  {
    return _initProgram;
  }

  /**
   * Sets the host's init program
   */
  public void setInitProgram(BuilderProgram initProgram)
  {
    _initProgram = initProgram;
  }

  /**
   * Returns the mbean object.
   */
  public Object getMBean()
  {
    return _mbean;
  }

  /**
   * Adds host defaults.
   */
  public void addHostDefault(HostConfig config)
  {
    _hostDefaults.add(config);
  }
  
  /**
   * Returns the host directory set by the resin.conf.
   */
  public Path getCfgRootDirectory()
  {
    return _cfgRootDir;
  }

  /**
   * Sets the host directory by the resin.conf
   */
  public void setCfgRootDirectory(Path rootDir)
  {
    _cfgRootDir = rootDir;
  }

  /**
   * Returns the host directory set by the hosts-directory.
   */
  public Path getJarRootDir()
  {
    return _jarRootDir;
  }

  /**
   * Sets the host directory by the resin.conf
   */
  public void setJarRootDir(Path rootDir)
  {
    _jarRootDir = rootDir;
  }

  /**
   * Adds a dependent file.
   */
  public void addDepend(Path depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(new Depend(depend));
  }

  /**
   * Initialize the entry.
   */
  public boolean init()
  {
    if (! super.init())
      return false;
    
    try {
      try {
	if (_config == null || getHostName() != null) {
	}
	else if (_config.getHostName() != null)
	  setHostName(EL.evalString(_config.getHostName(), _variableResolver));
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      if (_regexpName != null && _hostName == null)
	_hostName = _regexpName;

      if (_hostName == null)
	_hostName = "";

      if (super.getName() == null)
	setName(getHostName());

      ArrayList<String> aliases = null;

      if (_config != null)
	aliases = _config.getHostAliases();
      for (int i = 0; aliases != null && i < aliases.size(); i++) {
	String alias = aliases.get(i);

	alias = EL.evalString(alias, _variableResolver);
	
	addHostAlias(alias);
      }
      
      setRootDirectory(calculateRootDirectory());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return true;
  }

  /**
   * Deploys the DeployController after any merging.  Does not deploy the instance.
   */
  protected void deployEntry()
  {
    try {
      LinkedHashMap<String,String> properties = Jmx.copyContextProperties();
      properties.put("type", "Host");

      String name = _hostName;
      if (name == null)
	name = "";
      else if (name.indexOf(':') >= 0)
	name = name.replace(':', '-');

      if (name.equals(""))
	properties.put("name", "default");
      else
	properties.put("name", name);

      _mbeanName = Jmx.getObjectName("resin", properties);

      _mbean = new IntrospectionMBean(new Admin(), HostMBean.class);

      // Must wait for actual registration because regexp creates
      // host entry before checking for duplicates.
      if (_regexpName == null)
	Jmx.register(_mbean, _mbeanName);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns true for a matching name.
   */
  public boolean isNameMatch(String name)
  {
    if (_hostName.equalsIgnoreCase(name))
      return true;

    for (int i = _hostAliases.size() - 1; i >= 0; i--) {
      if (name.equalsIgnoreCase(_hostAliases.get(i)))
	return true;
    }
    
    return false;
  }

  /**
   * Merges two entries.
   */
  protected HostController merge(HostController newEntry)
  {
    if (_config.getRegexp() != null)
      return newEntry;
    else if (newEntry._config.getRegexp() != null)
      return this;
    else {
      // XXX: not quite correct.
      return newEntry;
    }
  }

  /**
   * Creates a new instance of the host object.
   */
  protected Host instantiateDeployInstance()
  {
    return new Host(_container, this, _hostName);
  }

  /**
   * Creates the host.
   */
  protected void configureInstance(Host host)
    throws Throwable
  {
    _hostAliases.clear();
    _hostAliases.addAll(_entryHostAliases);
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    Path rootDir = null;
    try {
      if (_container != null) {
        thread.setContextClassLoader(_container.getClassLoader());
      }
      
      Map<String,Object> varMap = host.getVariableMap();
      varMap.putAll(_variableMap);

      rootDir = calculateRootDirectory();
      if (rootDir == null)
	throw new NullPointerException("Null root-directory");

      /*
        if (! rootDir.isDirectory()) {
	throw new ConfigException(L.l("root-directory `{0}' must specify a directory.",
	rootDir.getPath()));
        }
      */

      host.setRootDirectory(rootDir);
      varMap.put("host-root", rootDir);

      ArrayList<HostConfig> initList = new ArrayList<HostConfig>();
      
      if (_container != null) {
	initList.addAll(_container.getHostDefaultList());
	  
	ArrayList<WebAppConfig> appDefaults;
	appDefaults = _container.getWebAppDefaultList();

	for (int i = 0; i < appDefaults.size(); i++) {
	  WebAppConfig init = appDefaults.get(i);

	  host.addWebAppDefault(init);
	}
      }

      // deployHost();
      /*
	if (_initProgram != null)
	_initProgram.configure(host);
      */
        
      MergePath schemaPath = new MergePath();
      schemaPath.addClassPath();

      initList.addAll(_hostDefaults);

      if (_config != null)
	initList.add(_config);

      thread.setContextClassLoader(host.getClassLoader());
      Vfs.setPwd(rootDir);

      for (int i = 0; i < _dependList.size(); i++) {
	Dependency depend = _dependList.get(i);

	Environment.addDependency(depend);
      }

      for (int i = 0; i < initList.size(); i++) {
	HostConfig config = initList.get(i);
	BuilderProgram program = config.getBuilderProgram();

	if (program != null)
	  program.configure(host);
      }

      host.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected Path calculateRootDirectory()
    throws ELException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_container.getClassLoader());
      
      Path rootDir = getRootDirectory();
 
      if (rootDir == null && _config != null) {
	String path = _config.getRootDirectory();

	if (path != null)
	  rootDir = PathBuilder.lookupPath(path, _variableResolver);
      }
     
      if (rootDir == null)
	rootDir = _cfgRootDir;
 
      Host host = getDeployInstance();
      
      if (rootDir == null && host != null)
	rootDir = host.getRootDirectory();

      if (rootDir == null)
	rootDir = Vfs.lookup();

      return rootDir;
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof HostController))
      return false;

    HostController entry = (HostController) o;

    return _hostName.equals(entry._hostName);
  }

  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "HostController[" + getName() + "]";
  }

  /**
   * EL variables for the host.
   */
  public class Var {
    public String getName()
    {
      return HostController.this.getName();
    }
    
    public String getHostName()
    {
      return HostController.this.getHostName();
    }
    
    public String getURL()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getURL();
      else if (_hostName.equals(""))
	return "";
      else if (_hostName.startsWith("http:") ||
	       _hostName.startsWith("https:"))
	return _hostName;
      else
	return "http://" + _hostName;
    }

    public ArrayList getRegexp()
    {
      return (ArrayList) _variableMap.get("regexp");
    }
    
    public Path getRootDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getRootDirectory();
      else
	return HostController.this.getRootDirectory();
    }
    
    public Path getRootDir()
    {
      return getRootDirectory();
    }
    
    public Path getDocumentDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getDocumentDirectory();
      else
	return null;
    }
    
    public Path getDocDir()
    {
      return getDocumentDirectory();
    }
    
    public Path getWarDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getWarDir();
      else
	return null;
    }
    
    public Path getWarDir()
    {
      return getWarDirectory();
    }
    
    public Path getWarExpandDirectory()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getWarExpandDir();
      else
	return null;
    }
    
    public Path getWarExpandDir()
    {
      return getWarExpandDirectory();
    }
    
    public String toString()
    {
      return "Host[" + getName() + "]";
    }
  }

  /**
   * Return the mbean info.
   */
  public class Admin implements HostMBean {
    public String getName()
    {
      return HostController.this.getName();
    }
    
    public String getHostName()
    {
      return HostController.this.getHostName();
    }

    /**
     * Returns the mbean object.
     */
    public ObjectName getObjectName()
    {
      return HostController.this._mbeanName;
    }
    
    public String getURL()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getURL();
      else
	return null;
    }
    
    public Date getStartTime()
    {
      return new Date(HostController.this.getStartTime());
    }
    
    public String getRootDirectory()
    {
      Path path;
      
      Host host = getDeployInstance();
      
      if (host != null)
	path = host.getRootDirectory();
      else
	path = HostController.this.getRootDirectory();

      if (path != null)
	return path.getNativePath();
      else
	return null;
    }

    /**
     * Returns the host's document directory.
     */
    public String getDocumentDirectory()
    {
      Path path = null;
      
      Host host = getDeployInstance();
      
      if (host != null)
	path = host.getDocumentDirectory();

      if (path != null)
	return path.getNativePath();
      else
	return null;
    }

    /**
     * Returns the host's war directory.
     */
    public String getWarDirectory()
    {
      Path path = null;
      
      Host host = getDeployInstance();
      
      if (host != null)
	path = host.getWarDir();

      if (path != null)
	return path.getNativePath();
      else
	return null;
    }
    
    public String getWarExpandDirectory()
    {
      Path path = null;
      
      Host host = getDeployInstance();
      
      if (host != null)
	path = host.getWarExpandDir();

      if (path != null)
	return path.getNativePath();
      else
	return null;
    }

    /**
     * Updates a .war deployment.
     */
    public void updateWebAppDeploy(String name)
    {
      Host host = getDeployInstance();
      
      if (host != null)
	host.updateWebAppDeploy(name);
    }

    /**
     * Updates a .ear deployment.
     */
    public void updateEarDeploy(String name)
    {
      Host host = getDeployInstance();
      
      if (host != null)
	host.updateEarDeploy(name);
    }

    /**
     * Expand a .ear deployment.
     */
    public void expandEarDeploy(String name)
    {
      Host host = getDeployInstance();
      
      if (host != null)
	host.expandEarDeploy(name);
    }

    /**
     * Start a .ear deployment.
     */
    public void startEarDeploy(String name)
    {
      Host host = getDeployInstance();
      
      if (host != null)
	host.startEarDeploy(name);
    }

    /**
     * Returns a string view.
     */
    public String toString()
    {
      return "MBean[" + HostController.this.toString() + "]";
    }
  }
}
