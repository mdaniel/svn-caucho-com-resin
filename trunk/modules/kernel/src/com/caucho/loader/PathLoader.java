/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.loader;

import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;

import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Class loader which loads specific paths.
 */
public class PathLoader extends Loader implements Dependency {
  private HashMap<String,Path> _pathMap = new HashMap<String,Path>();

  /**
   * Creates a new directory loader.
   */
  public PathLoader()
  {
  }

  /**
   * Initialize
   */
  public void init()
  {
  }

  /**
   * Returns the modified
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Returns the modified
   */
  public boolean logModified(Logger log)
  {
    return false;
  }

  /**
   * Adds a new path.
   */
  public void put(String name, Path path)
  {
    _pathMap.put(name, path);
  }

  /**
   * Returns the class entry.
   *
   * @param name name of the class
   */
  @Override
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    Path path = _pathMap.get(name);

    if (path != null && path.canRead() && path.getLength() > 0) {
      ClassEntry entry = new ClassEntry(getClassLoader(), name, path,
                                        path, getCodeSource(path));

      /*
      int p = name.lastIndexOf('.');
      String pkg;
      if (p > 0)
        pkg = name.substring(0, p);
      else
        pkg = "";

      ClassPackage classPackage = jarEntry.getPackage(pkg);

      entry.setClassPackage(classPackage);
      */

      return entry;
    }

    return null;
  }
  
  /**
   * Adds resources to the enumeration.
   */
  public void getResources(Vector<URL> vector, String name)
  {
  }

  /**
   * Find a given path somewhere in the classpath
   *
   * @param pathName the relative resourceName
   *
   * @return the matching path or null
   */
  public Path getPath(String pathName)
  {
    return null;
  }

  public Path getCodePath()
  {
    return null;
  }

  public String toString()
  {
    return "PathLoader[]";
  }
}
