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

import java.net.URL;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.TimedCache;
import com.caucho.vfs.Path;

/**
 * Root class loader.
 */
public class RootDynamicClassLoader
  extends DynamicClassLoader
{
  private static final URL NULL_URL;
  
  private static final ClassLoader _systemClassLoader;
  private static final DynamicClassLoader _systemRootClassLoader;
  
  private TimedCache<String,String> _classNotFoundCache
    = new TimedCache<String,String>(8192, 60 * 1000);
  
  private TimedCache<String,URL> _resourceCache
    = new TimedCache<String,URL>(8192, 60 * 1000);
  
  private Path _libexec;

  /**
   * Creates a new RootDynamicClassLoader.
   */
  private RootDynamicClassLoader(ClassLoader parent)
  {
    super(parent, false, true);
    
    if (parent instanceof DynamicClassLoader) {
      throw new IllegalStateException();
    }
  }
  
  static DynamicClassLoader create(ClassLoader parent)
  {
    if (parent instanceof DynamicClassLoader)
      return (DynamicClassLoader) parent;
    
    if (parent == _systemClassLoader)
      return _systemRootClassLoader;
    
    return new RootDynamicClassLoader(parent);
  }
  
  public static DynamicClassLoader getSystemRootClassLoader()
  {
    return _systemRootClassLoader;
  }
  
  @Override
  public boolean isRoot()
  {
    return true;
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname to load
   * @param resolve if true, resolve the class
   *
   * @return the loaded classes
   */
  @Override
  public Class<?> loadClassImpl(String name, boolean resolve)
    throws ClassNotFoundException
  {
    // The JVM has already cached the classes, so we don't need to
    Class<?> cl = findLoadedClass(name);

    if (cl != null) {
      if (resolve)
        resolveClass(cl);
      return cl;
    }
    // System.out.println("ROOT: " + name);
    if (_classNotFoundCache.get(name) != null) {
      return null;
    }
    
    try {
      cl = super.loadClassImpl(name, resolve);
    } catch (ClassNotFoundException e) {
      _classNotFoundCache.put(name, name);
      
      throw e;
    }
    
    if (cl == null) {
      _classNotFoundCache.put(name, name);
    }
    
    return cl;
  }
  
  @Override
  public URL getResource(String name)
  {
    URL url = _resourceCache.get(name);
    
    if (url == null) {
      url = super.getResource(name);
      
      if (url != null)
        _resourceCache.put(name, url);
      else
        _resourceCache.put(name, NULL_URL);
    }
    else if (url == NULL_URL) {
      url = null;
    }
    
    return url;
  }
  
  private Path getLibexec()
  {
    if (_libexec == null) {
      if (CauchoSystem.isWindows()) {
        if (CauchoSystem.is64Bit()) {
          _libexec = CauchoSystem.getResinHome().lookup("win64");
        }
        else {
          _libexec = CauchoSystem.getResinHome().lookup("win32");
        }
      }
      else {
        if (CauchoSystem.is64Bit()) {
          _libexec = CauchoSystem.getResinHome().lookup("libexec64");
        }
        else {
          _libexec = CauchoSystem.getResinHome().lookup("libexec");
        }
      }
    }
    
    return _libexec;
  }

  /**
   * Returns the full library path for the name.
   */
  @Override
  public String findLibrary(String name)
  {
    Path path = getLibexec().lookup("lib" + name + ".so");

    if (path.canRead()) {
      return path.getNativePath();
    }
    
    path = getLibexec().lookup("lib" + name + ".jnilib");

    if (path.canRead()) {
      return path.getNativePath();
    }
    
    path = getLibexec().lookup(name + ".dll");

    return super.findLibrary(name);
  }

  /*
  private void initSecurity()
  {
    addPermission(new AllPermission());
  }
  */
  static {
    URL nullUrl = null;
    
    try {
      nullUrl = new URL("file:///caucho.com/null");
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    NULL_URL = nullUrl;
    
    ClassLoader systemClassLoader = null;
    
    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
    }
    
    try {
      if (systemClassLoader == null) {
        systemClassLoader = RootDynamicClassLoader.class.getClassLoader();
      }
    } catch (Exception e) {
    }
    
    _systemClassLoader = systemClassLoader;
    
    if (_systemClassLoader instanceof DynamicClassLoader)
      _systemRootClassLoader = (DynamicClassLoader) _systemClassLoader;
    else
      _systemRootClassLoader = new RootDynamicClassLoader(_systemClassLoader);
  }
}

