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

package com.caucho.osgi;

import com.caucho.config.ConfigException;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.Loader;
import com.caucho.vfs.Path;
import com.caucho.vfs.JarPath;
import com.caucho.server.util.*;

import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads resources.
 */
public class OsgiLoader extends Loader implements EnvironmentListener
{
  private static final Logger log
    = Logger.getLogger(OsgiLoader.class.getName());

  private OsgiManager _manager;

  private ArrayList<OsgiBundle> _bundleList = new ArrayList<OsgiBundle>();

  private ArrayList<OsgiBundle> _pendingInstallList
     = new ArrayList<OsgiBundle>();

  private ArrayList<ExportBundleClassLoader> _exportList
    = new ArrayList<ExportBundleClassLoader>();

  public OsgiLoader()
  {
    this(OsgiManager.getCurrent());
  }

  public OsgiLoader(OsgiManager manager)
  {
    _manager = manager;

    Environment.addEnvironmentListener(this);
  }

  public void addInstall(Path path)
  {
    OsgiBundle bundle = _manager.addPath(path, null, false);

    addBundle(bundle);

    _pendingInstallList.add(bundle);
  }

  public void addPath(Path path)
  {
    OsgiBundle bundle = _manager.addPath(path, null, false);

    addBundle(bundle);
  }

  public void addBundle(OsgiBundle bundle)
  {
    _bundleList.add(bundle);

    _exportList.addAll(bundle.getExports());
  }
  
  /**
   * Adds the classpath of this loader.
   */
  @Override
  protected void buildClassPath(ArrayList<String> list)
  {
    for (OsgiBundle bundle : _bundleList) {
      Path path = bundle.getPath();

      if (path != null) {
	String pathName = path.getNativePath();

	if (! list.contains(pathName))
	  list.add(pathName);
      }
    }
  }

  @Override
  protected Class loadClass(String name)
  {
    for (ExportBundleClassLoader loader : _exportList) {
      try {
	Class cl = loader.findClassImpl(name);

	if (cl != null)
	  return cl;
      } catch (Exception e) {
      }
    }
    
    return null;
  }
  
  /**
   * Handles the case where the environment is configuring and
   * registering beans
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
    throws ConfigException
  {
  }
  
  /**
   * Handles the case where the environment is binding injection targets
   */
  public void environmentBind(EnvironmentClassLoader loader)
    throws ConfigException
  {
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    ArrayList<OsgiBundle> installList = new ArrayList<OsgiBundle>();

    synchronized (_pendingInstallList) {
      installList.addAll(_pendingInstallList);
      _pendingInstallList.clear();
    }

    for (OsgiBundle bundle : installList) {
      bundle.startImpl();
    }
  }
  
  /**
   * Handles the case where the environment is stopping (after init).
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  @Override
  public String toString()
  {
    return "OsgiLoader[" + _manager + "]";
  }
}
