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

package com.caucho.loader;

import java.io.*;

import java.util.*;

import java.util.logging.*;

import java.util.jar.Manifest;
import java.util.jar.Attributes;

import java.net.URL;

import com.caucho.util.CauchoSystem;
import com.caucho.util.CharBuffer;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.LogStream;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Depend;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.make.Dependency;
import com.caucho.make.DependencyContainer;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class JarLoader extends Loader implements Dependency {
  private static final Logger log = Log.open(JarLoader.class);

  // list of the jars in the directory
  private ArrayList<JarEntry> _jarList = new ArrayList<JarEntry>();
  
  // list of dependencies
  private DependencyContainer _dependencyList = new DependencyContainer();

  /**
   * Creates a new directory loader.
   */
  public JarLoader()
  {
  }

  /**
   * Initialize
   */
  public void init()
  {
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
   * Validates the loader.
   */
  public void validate()
    throws ConfigException
  {
    for (int i = 0; i < _jarList.size(); i++) {
      _jarList.get(i).validate();
    }
  }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  public boolean isModified()
  {
    return _dependencyList.isModified();
  }

  /**
   * Adds a new jar.
   */
  public void addJar(Path jar)
  {
    JarPath jarPath = JarPath.create(jar);
    JarEntry jarEntry = new JarEntry(jarPath);

    if (_jarList.contains(jarEntry))
      return;
    
    _jarList.add(jarEntry);

    _dependencyList.add(new Depend(jarPath));

    if (getLoader() != null)
      getLoader().addURL(jarPath);
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  protected String getClassPath(String head)
  {
    CharBuffer cb = new CharBuffer();

    cb.append(head);

    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      JarPath jar = jarEntry.getJarPath();

      if (cb.length() > 0)
        cb.append(CauchoSystem.getPathSeparatorChar());
      cb.append(jar.getContainer().getNativePath());
    }

    return cb.close();
  }

  /**
   * Returns the class entry.
   *
   * @param name name of the class
   */
  protected ClassEntry getClassEntry(String name)
    throws ClassNotFoundException
  {
    String pathName = name.replace('.', '/');
    
    String pkg = "";
    int p = pathName.lastIndexOf('/');
    if (p > 0)
      pkg = pathName.substring(0, p + 1);
         
    pathName = pathName + ".class";

    Path classPath = null;
    
    // Find the path corresponding to the class
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      Path path = jarEntry.getJarPath();

      Path filePath = path.lookup(pathName);
      
      if (filePath.canRead() && filePath.getLength() > 0) {
        ClassEntry entry = new ClassEntry(getLoader(), name, filePath,
                                          filePath, getCodeSource(filePath));

        ClassPackage classPackage = jarEntry.getPackage(pkg);

        entry.setClassPackage(classPackage);

        return entry;
      }
    }

    return null;
  }
  
  /**
   * Adds resources to the enumeration.
   */
  public void getResources(Vector<URL> vector, String name)
  {
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      Path path = jarEntry.getJarPath();

      path = path.lookup(name);

      if (path.canRead()) {
	try {
	  vector.add(new URL(path.getURL()));
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }
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
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      Path path = jarEntry.getJarPath();

      Path filePath = path.lookup(pathName);

      if (filePath.canRead())
	return filePath;
    }

    return null;
  }

  public Path getCodePath()
  {
    return null;
  }

  public String toString()
  {
    return "JarLoader[]";
  }
}
