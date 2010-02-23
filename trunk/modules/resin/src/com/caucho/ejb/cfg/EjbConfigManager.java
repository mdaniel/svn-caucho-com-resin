/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.cfg;

import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Manages the EJB configuration files.
 */
public class EjbConfigManager extends EjbConfig {
  private static final L10N L = new L10N(EjbConfigManager.class);
  private static final Logger log
    = Logger.getLogger(EjbConfigManager.class.getName());

  private HashMap<Path,EjbRootConfig> _rootConfigMap
    = new HashMap<Path,EjbRootConfig>();

  private ArrayList<EjbRootConfig> _rootPendingList
    = new ArrayList<EjbRootConfig>();
  
  private ArrayList<Path> _pathPendingList = new ArrayList<Path>();

  public EjbConfigManager(EjbContainer ejbContainer)
  {
    super(ejbContainer);
  }

  /**
   * Returns an EjbRootConfig for a class-loader root.
   */
  public EjbRootConfig createRootConfig(Path root)
  {
    EjbRootConfig rootConfig = _rootConfigMap.get(root);

    if (rootConfig == null) {
      rootConfig = new EjbRootConfig(root);
      _rootConfigMap.put(root, rootConfig);
      _rootPendingList.add(rootConfig);

      Path ejbJar = root.lookup("META-INF/ejb-jar.xml");
      if (ejbJar.canRead())
	addEjbPath(ejbJar);
    }

    return rootConfig;
  }

  public void start()
  {
    ArrayList<EjbRootConfig> pendingList
      = new ArrayList<EjbRootConfig>(_rootPendingList);
    _rootPendingList.clear();

    for (EjbRootConfig rootConfig : pendingList) {
      for (String className : rootConfig.getClassNameList()) {
	addIntrospectableClass(className);
      }
    }

    configurePaths();

    configure();

    deploy();
  }

  /**
   * Adds a path for an EJB config file to the config list.
   */
  @Override
  public void addEjbPath(Path path)
  {
    if (_pathPendingList.contains(path))
      return;

    _pathPendingList.add(path);
  }

  private void configurePaths()
  {
    ArrayList<Path> pathList = new ArrayList<Path>(_pathPendingList);
    _pathPendingList.clear();

    for (Path path : pathList) {
      if (path.getScheme().equals("jar"))
	path.setUserPath(path.getURL());

      Environment.addDependency(path);

      String ejbModuleName;

      if (path instanceof JarPath) {
	ejbModuleName = ((JarPath) path).getContainer().getPath();
      }
      else {
	ejbModuleName = path.getPath();
      }

      EjbJar ejbJar = new EjbJar(this, ejbModuleName);

      try {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " reading " + path.getURL());

	new Config().configure(ejbJar, path, getSchema());
      } catch (ConfigException e) {
	throw e;
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
  }
}
