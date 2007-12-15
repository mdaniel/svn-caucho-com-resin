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

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class LibraryLoader extends Loader implements Dependency {
  private static final Logger log
    = Logger.getLogger(DirectoryLoader.class.getName());
  
  // Configured path.
  private Path _path;
  
  private FileSetType _fileSet;

  // When the directory was last modified
  private long _lastModified;

  private String []_fileNames;

  // list of the matching paths
  private ArrayList<Path> _pathList = new ArrayList<Path>();

  // list of the matching paths
  private ArrayList<Path> _newPathList = new ArrayList<Path>();
  
  // list of the jars in the directory
  private ArrayList<JarEntry> _jarList;
  
  // list of dependencies
  private DependencyContainer _dependencyList = new DependencyContainer();

  /**
   * Creates a new directory loader.
   */
  public LibraryLoader()
  {
  }

  /**
   * Creates a new directory loader.
   */
  public LibraryLoader(Path path)
  {
    _path = path;

    try {
      init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The library loader's path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * The library loader's path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets a file set.
   */
  public void setFileSet(FileSetType fileSet)
  {
    _fileSet = fileSet;
  }

  /**
   * Create a new class loader
   *
   * @param parent parent class loader
   * @param dir directories which can handle dynamic jar addition
   */
  public static DynamicClassLoader create(ClassLoader parent, Path path)
  {
    DynamicClassLoader loader = new DynamicClassLoader(parent);

    LibraryLoader dirLoader = new LibraryLoader(path);

    loader.addLoader(dirLoader);

    loader.init();
    
    return loader;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      if (_fileSet != null) {
      }
      else if (_path.getPath().endsWith(".jar") ||
	       _path.getPath().endsWith(".zip")) {
	_fileSet = new FileSetType();
	_fileSet.setDir(_path.getParent());
	_fileSet.addInclude(new PathPatternType(_path.getTail()));
      }
      else {
	_fileSet = new FileSetType();
	_fileSet.setDir(_path);
	_fileSet.addInclude(new PathPatternType("*.jar"));
	_fileSet.addInclude(new PathPatternType("*.zip"));
      }

      _jarList = new ArrayList<JarEntry>();
      _dependencyList = new DependencyContainer();

      fillJars();
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
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
    _newPathList.clear();

    _fileSet.getPaths(_newPathList);

    return ! _newPathList.equals(_pathList);
  }
  
  /**
   * True if the classes in the directory have changed.
   */
  public boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(_path.getNativePath() + " has modified jar files");
      return true;
    }
    else
      return false;
  }

  /**
   * Find all the jars in this directory and add them to jarList.
   */
  private void fillJars()
  {
    _pathList.clear();
    _jarList.clear();

    _fileSet.getPaths(_pathList);

    for (int i = 0; i < _pathList.size(); i++) {
      Path jar = _pathList.get(i);

      addJar(jar);
    }
  }

  private void addJar(Path jar)
  {
    JarPath jarPath = JarPath.create(jar);
    _jarList.add(new JarEntry(jarPath));

    _dependencyList.add(new Depend(jarPath));

    if (getLoader() != null)
      getLoader().addURL(jarPath);
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  @Override
  protected void buildClassPath(ArrayList<String> pathList)
  {
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      JarPath jar = jarEntry.getJarPath();
      
      String path = jar.getContainer().getNativePath();

      if (! pathList.contains(path))
	pathList.add(path);
    }
  }

  /**
   * Returns the class entry.
   *
   * @param name name of the class
   */
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    String pkg = "";
    int p = pathName.lastIndexOf('/');
    if (p > 0)
      pkg = pathName.substring(0, p + 1);

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
   * Adds resources to the enumeration.
   */
  public void getResources(Vector<URL> vector, String name)
  {
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      Path path = jarEntry.getJarPath();

      path = path.lookup(name);

      if (path.exists()) {
	try {
	  URL url = new URL(path.getURL());

	  if (! vector.contains(url))
	    vector.add(url);
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

      if (filePath.exists())
	return filePath;
    }

    return null;
  }

  public Path getCodePath()
  {
    return _fileSet.getDir();
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
    return "LibraryLoader[" + _fileSet + "]";
  }
}
