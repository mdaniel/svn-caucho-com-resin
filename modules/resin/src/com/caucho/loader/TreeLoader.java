/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import javax.annotation.*;

import com.caucho.util.CharBuffer;

import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.make.DependencyContainer;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class TreeLoader extends Loader implements Dependency
{
  private static final Logger log
    = Logger.getLogger(TreeLoader.class.getName());
  
  // Directory which may have jars dynamically added
  private Path _dir;

  // When the directory was last modified
  private long _lastModified;

  private String []_fileNames;

  // list of the jars in the directory
  private ArrayList<JarEntry> _jarList;

  private HashSet<Path> _files = new HashSet<Path>();
  private HashSet<Path> _tempFiles = new HashSet<Path>();
  
  // list of dependencies
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
  public void init()
  {
    _lastModified = _dir.getLastModified();
    
    try {
      _fileNames = _dir.list();
    } catch (IOException e) {
    }

    _jarList = new ArrayList<JarEntry>();
    _dependencyList = new DependencyContainer();

    fillJars();
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

  private void addJar(Path jar)
  {
    JarPath jarPath = JarPath.create(jar);
    JarEntry jarEntry = new JarEntry(jarPath);

    if (_jarList.contains(jarEntry))
      return;

    _jarList.add(jarEntry);
    _dependencyList.add(jarPath.getDepend());
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

      if (path.canRead() || path.isDirectory()) {
	try {
	  vector.add(new URL(path.getURL()));
	  return;
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }
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
        cb.append(Path.getPathSeparatorChar());
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
                                          filePath,
					  jarEntry.getCodeSource(pathName));

        ClassPackage classPackage = jarEntry.getPackage(pkg);

        entry.setClassPackage(classPackage);

        return entry;
      }
    }

    return null;
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
    return _dir;
  }

  /**
   * Destroys the loader, closing the jars.
   */
  protected void destroy()
  {
    clearJars();
  }

  /**
   * Closes the jars.
   */
  private void clearJars()
  {
    ArrayList<JarEntry> jars = new ArrayList<JarEntry>(_jarList);
    _jarList.clear();
    
    for (int i = 0; i < jars.size(); i++) {
      JarEntry jarEntry = jars.get(i);

      JarPath jarPath = jarEntry.getJarPath();

      jarPath.closeJar();
    }
  }

  public String toString()
  {
    return "TreeLoader[" + _dir + "]";
  }
}
