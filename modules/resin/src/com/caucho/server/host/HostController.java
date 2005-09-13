
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

import com.caucho.make.Dependency;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.el.EL;

import com.caucho.server.webapp.WebAppConfig;

import com.caucho.server.e_app.EarConfig;

import com.caucho.server.deploy.EnvironmentDeployController;

import com.caucho.server.host.mbean.HostMBean;

/**
 * A configuration entry for a host
 */
public class HostController extends EnvironmentDeployController<Host,HostConfig> {
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

  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();

  HostController(String id,
		 HostConfig config,
		 HostContainer container,
		 Map<String,Object> varMap)
  {
    super(id, config);

    setHostName(id);

    if (varMap != null)
      getVariableMap().putAll(varMap);
    
    getVariableMap().put("host", _hostVar);

    setContainer(container);
    
    setRootDirectory(config.calculateRootDirectory(getVariableMap()));
  }

  HostController(String id, Path rootDirectory, HostContainer container)
  {
    super(id, rootDirectory);

    setHostName(id);

    getVariableMap().put("name", id);
    getVariableMap().put("host", _hostVar);
    
    setContainer(container);
  }

  public void setContainer(HostContainer container)
  {
    _container = container;
    
    if (_container != null) {
      for (HostConfig defaultConfig : _container.getHostDefaultList())
	addConfigDefault(defaultConfig);
    }
  }

  /**
   * Returns the Resin host name.
   */
  public String getName()
  {
    String name = super.getId();
    
    if (name != null)
      return name;
    else
      return getHostName();
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
				    EL.getEnvironment()));
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      if (_regexpName != null && _hostName == null)
	_hostName = _regexpName;

      if (_hostName == null)
	_hostName = "";

      ArrayList<String> aliases = null;

      if (getConfig() != null) {
	aliases = getConfig().getHostAliases();

	_entryHostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
      }
      
      for (int i = 0; aliases != null && i < aliases.size(); i++) {
	String alias = aliases.get(i);

	alias = EL.evalString(alias, EL.getEnvironment());
	
	addHostAlias(alias);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    super.initBegin();
  }

  /**
   * Returns the "name" property.
   */
  protected String getMBeanId()
  {
    String name = _hostName;
    
    if (name == null)
      name = "";
    else if (name.indexOf(':') >= 0)
      name = name.replace(':', '-');

    if (name.equals(""))
      return "default";
    else
      return name;
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
  protected HostController merge(HostController newController)
  {
    if (getConfig() != null && getConfig().getRegexp() != null)
      return newController;
    else if (newController.getConfig() != null &&
	     newController.getConfig().getRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(getParentClassLoader());

	HostController mergedController
	  = new HostController(newController.getHostName(),
			       getRootDirectory(),
			       _container);

	mergedController.mergeController(this);
	mergedController.mergeController(newController);

	return mergedController;
      } catch (Throwable e) {
	e.printStackTrace();
	return null;
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }
  }

  /**
   * Merges with the old controller.
   */
  protected void mergeController(HostController oldController)
  {
    super.mergeController(oldController);

    _entryHostAliases.addAll(oldController._entryHostAliases);
    _entryHostAliasRegexps.addAll(oldController._entryHostAliasRegexps);
    _hostAliases.addAll(oldController._hostAliases);
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

    getVariableMap().put("host-root", getRootDirectory());

    if (_container != null) {
      for (EarConfig config : _container.getEarDefaultList())
	host.addEarDefault(config);

      for (WebAppConfig config : _container.getWebAppDefaultList())
	host.addWebAppDefault(config);
    }

    super.configureInstance(host);
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
    
    public Path getRoot()
    {
      Host host = getDeployInstance();
      
      if (host != null)
	return host.getRootDirectory();
      else
	return HostController.this.getRootDirectory();
    }
    
    /**
     * @deprecated
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * @deprecated
     */
    public Path getRootDirectory()
    {
      return getRoot();
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
      return "Host[" + getId() + "]";
    }
  }
}
