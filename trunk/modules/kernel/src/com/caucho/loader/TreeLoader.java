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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader;

import com.caucho.config.*;
import com.caucho.make.DependencyContainer;
import com.caucho.util.*;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class TreeLoader extends JarListLoader implements Dependency
{
  private static final L10N L = new L10N(TreeLoader.class);
  
  private static final Logger log
    = Logger.getLogger(TreeLoader.class.getName());
  
  // Directory which may have jars dynamically added
  private Path _dir;

  // When the directory was last modified
  private long _lastModified;

  private String []_fileNames;

  /**
   * Creates a new directory loader.
   */
  public TreeLoader()
  {
  }

  /**
   * Creates a new directory loader.
   */
  public TreeLoader(Path dir)
  {
    _dir = dir;

    init();
  }

  /**
   * The directory loader's path.
   */
  public void setPath(Path path)
  {
    _dir = path;
  }

  /**
   * The directory loader's path.
   */
  public Path getPath()
  {
    return _dir;
  }

  /**
   * Create a new class loader
   *
   * @param parent parent class loader
   * @param dir directories which can handle dynamic jar addition
   */
  public static DynamicClassLoader create(ClassLoader parent, Path dir)
  {
    DynamicClassLoader loader = new DynamicClassLoader(parent);

    TreeLoader treeLoader = new TreeLoader(dir);

    loader.addLoader(treeLoader);

    loader.init();
    
    return loader;
  }

  /**
   * Initialize
   */
  @PostConstruct
  @Override
  public void init()
  {
    super.init();

    if (_dir == null)
      throw new ConfigException(L.l("<tree-loader> requires a 'path' attribute"));
    
    _lastModified = _dir.getLastModified();
    
    try {
      _fileNames = _dir.list();
    } catch (IOException e) {
    }

    fillJars();
  }
  
  /**
   * True if the classes in the directory have changed.
   */
  public boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(_dir.getNativePath() + " has modified jar files");
      return true;
    }
    else
      return false;
  }

  /**
   * Sets the owning class loader.
   */
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);

    for (int i = 0; i < _jarList.size(); i++)
      loader.addURL(_jarList.get(i).getJarPath());
  }

  /**
   * Find all the jars in this directory and add them to jarList.
   */
  private void fillJars()
  {
    clearJars();

    fillJars(_dir);
  }
  
  /**
   * Find all the jars in this directory and add them to jarList.
   */
  private void fillJars(Path dir)
  {
    try {
      String []list = dir.list();

      for (int j = 0; list != null && j < list.length; j++) {
	Path path = dir.lookup(list[j]);

	if (list[j].endsWith(".jar") || list[j].endsWith(".zip")) {
	  addJar(path);
	}
	else if (path.isDirectory()) {
	  fillJars(path);
	}
      }
      
    } catch (IOException e) {
    }
  }

  public Path getCodePath()
  {
    return _dir;
  }

  /**
   * Destroys the loader, closing the jars.
   */
  protected void destroy()
  {
    clearJars();
  }

  public String toString()
  {
    return "TreeLoader[" + _dir + "]";
  }
}
