/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.loader.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class OsgiManager
{
  private static final Logger log
    = Logger.getLogger(OsgiManager.class.getName());

  private static final EnvironmentLocal<OsgiManager> _localOsgi
    = new EnvironmentLocal<OsgiManager>();

  private ClassLoader _parentLoader;

  private ArrayList<OsgiBundle> _bundleList
    = new ArrayList<OsgiBundle>();

  private HashMap<String,ExportBundleClassLoader> _exportMap
    = new HashMap<String,ExportBundleClassLoader>();

  private OsgiManager()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader().getParent();
  }

  public static OsgiManager getCurrent()
  {
    return _localOsgi.getLevel();
  }

  public static OsgiManager create()
  {
    synchronized (_localOsgi) {
      OsgiManager manager = _localOsgi.getLevel();

      if (manager == null) {
	manager = new OsgiManager();
	_localOsgi.set(manager);
      }

      return manager;
    }
  }

  public ClassLoader getParentLoader()
  {
    return _parentLoader;
  }

  /**
   * Adds a new jar
   */
  public OsgiBundle addPath(Path path)
  {
    JarPath jar = JarPath.create(path);

    OsgiBundle bundle = new OsgiBundle(this, jar);

    _bundleList.add(bundle);

    return bundle;
  }

  public ExportBundleClassLoader getExportLoader(String name)
  {
    return _exportMap.get(name);
  }

  public void putExportLoader(String name,
			      ExportBundleClassLoader loader)
  {
    _exportMap.put(name, loader);
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }
}
