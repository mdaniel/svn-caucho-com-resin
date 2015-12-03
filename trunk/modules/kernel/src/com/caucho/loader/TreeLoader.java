/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights - reserved
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.make.DependencyContainer;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
@Configurable
public class TreeLoader extends JarListLoader implements Dependency
{
  private static final L10N L = new L10N(TreeLoader.class);
  
  // Directory which may have jars dynamically added
  private Path _dir;
  
  private DependencyContainer _dependencyList = new DependencyContainer();

  /**
   * Creates a new directory loader.
   */
  public TreeLoader()
  {
  }

  /**
   * Creates a new directory loader.
   */
  public TreeLoader(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Creates a new directory loader.
   */
  public TreeLoader(ClassLoader loader, Path dir)
  {
    super(loader);
    
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

    TreeLoader treeLoader = new TreeLoader(loader, dir);

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
    
    _dir.getLastModified();
    
    try {
      _dir.list();
    } catch (IOException e) {
    }

    fillJars();

    for (int i = 0; i < _jarList.size(); i++) { 
      getClassLoader().addURL(_jarList.get(i).getJarPath());
    }
  }
  
  /**
   * True if the classes in the directory have changed.
   */
  @Override
  public boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(_dir.getNativePath() + " has modified jar files");
      return true;
    }
    else
      return false;
  }
  
  @Override
  public boolean isModified()
  {
    // server/6p44
    if (super.isModified()) {
      return true;
    }
    else {
      return _dependencyList.isModified();
    }
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
    ArrayList<Path> paths = new ArrayList<Path>();
    
    fillJars(paths, dir);
    
    Collections.sort(paths);
    
    for (Path path : paths) {
      addJar(path);
    }
  }
  
  /**
   * Find all the jars in this directory and add them to jarList.
   */
  private void fillJars(ArrayList<Path> paths, Path dir)
  {
    try {
      addDependency(dir);

      String []list = dir.list();
      
      for (int j = 0; list != null && j < list.length; j++) {
        Path path = dir.lookup(list[j]);
        
        if (list[j].endsWith(".jar") || list[j].endsWith(".zip")) {
          paths.add(path);
        }
        else if (path.isDirectory()) {
          fillJars(paths, path);
        }
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private void addDependency(Path path)
  {
    _dependencyList.add(new Depend(path));
  }

  public Path getCodePath()
  {
    return _dir;
  }

  /**
   * Destroys the loader, closing the jars.
   */
  @Override
  protected void destroy()
  {
    super.destroy();
    
    clearJars();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _dir + "]";
  }
}
