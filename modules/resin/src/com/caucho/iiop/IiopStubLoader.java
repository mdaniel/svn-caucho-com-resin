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

package com.caucho.iiop;

import java.util.HashSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import java.net.URL;

import java.security.CodeSource;

import java.security.cert.Certificate;

import com.caucho.log.Log;

import com.caucho.util.CharBuffer;
import com.caucho.util.CauchoSystem;

import com.caucho.config.ConfigException;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.JarPath;

import com.caucho.java.WorkDir;

import com.caucho.ejb.AbstractStubLoader;

import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class IiopStubLoader extends AbstractStubLoader {
  private static final Logger log = Log.open(IiopStubLoader.class);

  private HashSet<String> _stubClassNames = new HashSet<String>();

  private CodeSource _codeSource;

  /**
   * Null constructor for the simple loader.
   */
  public IiopStubLoader()
  {
  }

  /**
   * Creates the simple loader with the specified path.
   *
   * @param path specifying the root of the resources
   */
  public IiopStubLoader(Path path)
  {
    setPath(path);
  }

  /**
   * Sets the owning class loader.
   */
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);

    loader.addURL(getPath());
  }

  /**
   * Initializes the loader.
   */
  public void init()
    throws ConfigException
  {
    try {
      _codeSource = new CodeSource(new URL(getPath().getURL()),
				   (Certificate []) null);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Adds a stub class.
   */
  public void addStubClass(String className)
  {
    System.out.println("PATH: " + getPath());
    
    int p = className.lastIndexOf('.');

    if (p > 0) {
      String tail = className.substring(p + 1);

      tail = "_" + tail + "_Stub";

      className = className.substring(0, p) + '.' + tail;
    }
    else
      className = "_" + className + "_Stub";

    className = "org.omg.stub." + className;

    className = className.replace('.', '/') + ".class";
    
    System.out.println("CL: " + className);
    
    _stubClassNames.add(className);
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
    if (! _stubClassNames.contains(name))
      return null;

    Path stubClassPath = getPath().lookup(name);

    System.out.println("PATH: " + stubClassPath.getNativePath());

    if (stubClassPath.canRead())
      return stubClassPath;

    String fullClassName = name.substring(0, name.length() - ".class".length());
    fullClassName = fullClassName.replace('/', '.');
    
    String className = fullClassName.substring("org.omg.stub.".length());
    className = className.replace('/', '.');

    int p = className.lastIndexOf('.');
    if (p > 0) {
      String tail = className.substring(p + 1);

      tail = tail.substring(1, tail.length() - "_Stub".length());

      className = className.substring(0, p) + '.' + tail;
    }

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      System.out.println("ZOOM: " + className);
      
      Class cl = Class.forName(className, false, loader);
      System.out.println("CL: " + cl);

      IiopStubCompiler compiler = new IiopStubCompiler(cl);
      compiler.setFullClassName(fullClassName);
      compiler.setClassDir(getPath());
      
      compiler.generate();
      compiler.compileJava();
    
      System.out.println("OOK-OOK-OOK");
      
      if (stubClassPath.canRead())
	return stubClassPath;
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.WARNING, e.toString(), e);
    }

    return null;
  }

  /**
   * Returns the code source for the directory.
   */
  protected CodeSource getCodeSource(Path path)
  {
    return _codeSource;
  }

  /**
   * Adds the class of this resource.
   */
  protected String getClassPath(String head)
  {
    CharBuffer cb = new CharBuffer();

    if (! head.equals("")) {
      cb.append(head);
      cb.append(CauchoSystem.getPathSeparatorChar());
    }
    
    if (getPath().isDirectory())
      cb.append(getPath().getNativePath());

    return cb.toString();
  }

  /**
   * Returns a printable representation of the loader.
   */
  public String toString()
  {
    return "IiopStubLoader[" + getPath() + "]";
  }
}
