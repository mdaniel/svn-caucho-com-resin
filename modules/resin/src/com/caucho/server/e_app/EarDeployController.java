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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.e_app;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.util.L10N;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;

import com.caucho.log.Log;

import com.caucho.loader.Environment;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.types.PathBuilder;

import com.caucho.make.Dependency;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.server.webapp.WebAppConfig;
import com.caucho.server.webapp.WebAppController;
import com.caucho.server.webapp.ApplicationContainer;

import com.caucho.server.deploy.ExpandDeployController;
import com.caucho.server.deploy.DeployContainer;

/**
 * A configuration entry for an Enterprise Application
 */
public class EarDeployController extends ExpandDeployController<EnterpriseApplication> {
  private static final Logger log = Log.open(EarDeployController.class);
  private static final L10N L = new L10N(EarDeployController.class);
  
  private ApplicationContainer _container;

  // The ear name
  private String _name = "";

  // The configuration
  private EarConfig _config;

  private BuilderProgram _initProgram;

  private VariableResolver _variableResolver;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // private Var _hostVar = new Var();

  // root-dir as set by the resin.conf
  private Path _earRootDir;

  private boolean _isInit;

  private ArrayList<EarConfig> _eAppDefaults = new ArrayList<EarConfig>();

  EarDeployController(ApplicationContainer container, EarConfig config)
  {
    _container = container;
    _config = config;

    VariableResolver parentResolver = EL.getEnvironment(getParentClassLoader());
    _variableResolver = new MapVariableResolver(_variableMap, parentResolver);
  }

  /**
   * Sets the Resin host name.
   */
  public void setName(String name)
  {
    _variableMap.put("name", name);

    super.setName(name);
  }

  /**
   * Gets the EarConfig
   */
  public EarConfig getEarConfig()
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
   * Returns the path variable map.
   */
  public VariableResolver getVariableResolver()
  {
    return _variableResolver;
  }

  /**
   * Adds the ear default.
   */
  public void addEarDefault(EarConfig earDefault)
  {
    if (earDefault != null)
      _eAppDefaults.add(earDefault);
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
   * Returns the ear directory set by the hosts-directory.
   */
  public Path getEarRootDir()
  {
    return _earRootDir;
  }

  /**
   * Sets the host directory by the resin.conf
   */
  public void setEarRootDir(Path rootDir)
  {
    _earRootDir = rootDir;
  }

  /**
   * Finds any web-app in the ear matching the contextPath.
   */
  public WebAppController findWebAppController(String name)
  {
    try {
      EnterpriseApplication eApp = request();

      if (eApp != null)
	return eApp.findWebAppEntry(name);
      else
	return null;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }

  /**
   * Creates the application.
   */
  protected EnterpriseApplication instantiateDeployInstance()
  {
    return new EnterpriseApplication(_container, this, getName());
  }

  /**
   * Creates the application.
   */
  protected void configureInstance(EnterpriseApplication eApp)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    Path rootDir = null;
    try {
      thread.setContextClassLoader(getParentClassLoader());

      Map<String,Object> varMap = eApp.getVariableMap();
      varMap.putAll(_variableMap);

      eApp.setEarPath(getArchivePath());

      rootDir = calculateRootDirectory();
      if (rootDir == null)
	throw new NullPointerException("Null root-directory");

      /*
        if (! rootDir.isDirectory()) {
	throw new ConfigException(L.l("root-directory `{0}' must specify a directory.",
	rootDir.getPath()));
        }
      */

      eApp.setRootDirectory(rootDir);

      ArrayList<EarConfig> initList = new ArrayList<EarConfig>();

      if (_container != null) {
	initList.addAll(_container.getEarDefaultList());
      }

      initList.addAll(_eAppDefaults);
	
      /*
	if (_initProgram != null)
	_initProgram.configure(host);
      */

      if (_config != null)
	initList.add(_config);
	
      thread.setContextClassLoader(eApp.getClassLoader());
      Vfs.setPwd(rootDir);

      addManifestClassPath();

      for (int i = 0; i < initList.size(); i++) {
	EarConfig config = initList.get(i);
	BuilderProgram program = config.getBuilderProgram();

	if (program != null)
	  program.configure(eApp);
      }

      eApp.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected Path calculateRootDirectory()
    throws ELException
  {
    Path rootDir = getRootDirectory();
    EnterpriseApplication eApp = getDeployInstance();
 
    if (rootDir == null && eApp != null)
      rootDir = eApp.getRootDirectory();

    return rootDir;
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EarDeployController))
      return false;

    EarDeployController entry = (EarDeployController) o;

    return getName().equals(entry.getName());
  }

  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "EarDeployController[" + getName() + "]";
  }
}
