
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

import java.util.regex.Pattern;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.JMException;

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

import com.caucho.server.e_app.EarConfig;

import com.caucho.server.deploy.EnvironmentDeployController;

import com.caucho.server.host.mbean.HostMBean;

/**
 * A configuration entry for a host
 */
class HostController extends EnvironmentDeployController<Host,HostConfig> {
  private static final Logger log = Log.open(HostController.class);
  private static final L10N L = new L10N(HostController.class);
  
  private HostContainer _container;

  // The host name is the canonical name
  private String _hostName;
  // The regexp name is the matching name of the regexp
  private String _regexpName;

  // Any host aliases.
  private ArrayList<String> _entryHostAliases
    = new ArrayList<String>();
  private ArrayList<Pattern> _entryHostAliasRegexps
    = new ArrayList<Pattern>();
  
  private ArrayList<String> _hostAliases = new ArrayList<String>();

  // The host variables.
  private Var _hostVar = new Var();

  // root-dir as set by the resin.conf
  private Path _cfgRootDir;
  // root-dir as set by the resin.conf
  private Path _jarRootDir;
    
  // private Host _host;
  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();

  HostController(HostContainer container, HostConfig config)
  {
    super("");
    
    _container = container;

    setConfig(config);

    if (_container != null) {
      for (HostConfig defaultConfig : _container.getHostDefaultList())
	addConfigDefault(defaultConfig);
    }
	
    getVariableMap().put("host", _hostVar);
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
    
    getVariableMap().put("name", name);

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
  protected void initBegin()
  {
    try {
      try {
	if (getConfig() == null || getHostName() != null) {
	}
	else if (getConfig().getHostName() != null)
	  setHostName(EL.evalString(getConfig().getHostName(),
				    getVariableResolver()));
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

      if (getConfig() != null) {
	aliases = getConfig().getHostAliases();

	_entryHostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
      }
      
      for (int i = 0; aliases != null && i < aliases.size(); i++) {
	String alias = aliases.get(i);

	alias = EL.evalString(alias, getVariableResolver());
	
	addHostAlias(alias);
      }
      
      setRootDirectory(calculateRootDirectory());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    super.initBegin();
  }

  /**
   * Creates the object name.  The default is to use getId() as
   * the 'name' property, and the classname as the 'type' property.
   */
  protected ObjectName createObjectName(Map<String,String> properties)
    throws MalformedObjectNameException
  {
    String name = _hostName;
    if (name == null)
      name = "";
    else if (name.indexOf(':') >= 0)
      name = name.replace(':', '-');

    if (name.equals(""))
      properties.put("name", "default");
    else
      properties.put("name", name);

    properties.put("type", "Host");
      
    return Jmx.getObjectName("resin", properties);
  }

  /**
   * Creates the managed object.
   */
  protected Object createMBean()
    throws JMException
  {
    return new IntrospectionMBean(new HostAdmin(this), HostMBean.class);
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

    for (int i = _entryHostAliasRegexps.size() - 1; i >= 0; i--) {
      Pattern alias = _entryHostAliasRegexps.get(i);

      if (alias.matcher(name).find())
	return true;
    }
    
    return false;
  }

  /**
   * Merges two entries.
   */
  protected HostController merge(HostController newEntry)
  {
    if (getConfig().getRegexp() != null)
      return newEntry;
    else if (newEntry.getConfig().getRegexp() != null)
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

    Map<String,Object> varMap = host.getVariableMap();
    varMap.put("host-root", getRootDirectory());

    if (_container != null) {
      for (EarConfig config : _container.getEarDefaultList())
	host.addEarDefault(config);
      
      for (WebAppConfig config : _container.getWebAppDefaultList())
	host.addWebAppDefault(config);
    }

    super.configureInstance(host);
  }
  
  protected Path calculateRootDirectory()
    throws ELException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_container.getClassLoader());
      
      Path rootDir = getRootDirectory();
 
      if (rootDir == null && getConfig() != null) {
	String path = getConfig().getRootDirectory();

	if (path != null)
	  rootDir = PathBuilder.lookupPath(path, getVariableResolver());
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
      return (ArrayList) getVariableMap().get("regexp");
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
}
