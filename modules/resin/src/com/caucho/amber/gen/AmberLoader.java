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

package com.caucho.amber.gen;

import java.util.logging.Logger;

import java.security.CodeSource;

import com.caucho.java.WorkDir;

import com.caucho.log.Log;

import com.caucho.loader.Loader;
import com.caucho.vfs.Path;

import com.caucho.amber.AmberManager;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class AmberLoader extends Loader {
  private static final Logger log = Log.open(AmberLoader.class);

  private AmberManager _manager;
  
  // The class directory
  private Path _path;
  private String _prefix;
  private String _pathPrefix;

  private CodeSource _codeSource;

  /**
   * Null constructor for the simple loader.
   */
  public AmberLoader(AmberManager manager)
  {
    _manager = manager;
  }

  /**
   * Given a class or resource name, returns a patch to that resource.
   *
   * @param name the class or resource name.
   *
   * @return the path representing the class or resource.
   */
  public Path getPath(String name)
  {
    if (! name.endsWith(".class"))
      return null;

    int p = name.lastIndexOf('$');
    if (p < 0)
      p = name.lastIndexOf('-');
    if (p < 0)
      p = name.lastIndexOf('.');
    
    String className = name.substring(0, p);
    className = className.replace('/', '.');

    if (_manager.getEntityByInstanceClass(className) == null)
      return null;

    Path workPath = WorkDir.getLocalWorkDir();

    return workPath.lookup(name);
  }

  /**
   * Returns the code source for the directory.
   */
  /*
  protected CodeSource getCodeSource(Path path)
  {
    return _codeSource;
  }
  */

  /**
   * Returns a printable representation of the loader.
   */
  public String toString()
  {
    return "AmberLoader[]";
  }
}
