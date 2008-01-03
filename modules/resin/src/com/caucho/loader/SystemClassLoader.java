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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.File;
import java.net.URLClassLoader;

/**
 * ClassLoader that initalizes the environment and allows byte code
 * enhancement of classes in the system classpath.
 * <pre>
 * java -Djava.system.class.loader=com.caucho.loader.SystemClassLoader ...
 * </pre>
 * If the system property "system.conf" is defined, it is used as a path
 * to a configuration file that initializes the enviornment.  Relative paths
 * are relative to the current directory (See {@link com.caucho.vfs.Vfs#getPwd()}.
 * <p/>
 * Resources defined in system.conf are available to all classes loaded within the jvm.
 * <pre>
 * java -Dsystem.conf=tests/system.conf -Djava.system.class.loader=com.caucho.loader.SystemClassLoader ...
 * </pre>
 */
public class SystemClassLoader
  extends EnvironmentClassLoader
  implements EnvironmentBean
{
  private boolean _isInit;
  private boolean _hasBootClassPath;

  private URLClassLoader _loader;

  /**
   * Creates a new SystemClassLoader.
   */
  public SystemClassLoader(ClassLoader parent)
  {
    super(parent);

    setId("system");
  }

  public ClassLoader getClassLoader()
  {
    return this;
  }

  public void init()
  {
    if (_isInit)
      return;

    _isInit = true;

    initClasspath();

    super.init();

    String systemConf = System.getProperty("system.conf");

    if (systemConf != null) {
      try {
        Path path = Vfs.lookup(systemConf);

        Config config = new Config();

        config.configure(this, path, getSchema());
      }
      catch (Exception ex) {
        ex.printStackTrace();

        throw new RuntimeException(ex.toString());
      }
    }
  }

  private void initClasspath()
  {
    String boot = System.getProperty("sun.boot.class.path");
    if (boot != null) {
      initClasspath(boot);
      _hasBootClassPath = true;
    }
    
    initClasspath(System.getProperty("java.class.path"));
  }

  private void initClasspath(String classpath)
  {
    String[] classpathElements = classpath.split(File.pathSeparator, 512);

    for (String classpathElement : classpathElements) {
      Path root = Vfs.lookup(classpathElement);

      try {
	if (root.exists())
	  addRoot(root);
      } catch (Throwable e) {
	e.printStackTrace();
      }
    }
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname to load
   * @param resolve if true, resolve the class
   *
   * @return the loaded classes
   */

  protected Class loadClassImpl(String name, boolean resolve)
    throws ClassNotFoundException
  {
    // The JVM has already cached the classes, so we don't need to
    Class cl = findLoadedClass(name);

    if (cl != null) {
      if (resolve)
        resolveClass(cl);
      return cl;
    }

    // This causes problems with JCE
    if (false && _hasBootClassPath) {
      String className = name.replace('.', '/') + ".class";

      if (findPath(className) == null)
	return null;
    }

    return super.loadClassImpl(name, resolve);
  }

  protected String getSchema()
  {
    return "com/caucho/loader/system.rnc";
  }

  public String toString()
  {
    return "SystemClassLoader[]";
  }
}


