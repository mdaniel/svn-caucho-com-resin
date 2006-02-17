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

package com.caucho.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Enumeration;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import com.caucho.loader.EnvironmentLocal;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Library of static config functions.
 */
public class ConfigLibrary {
  private static final L10N L = new L10N(ConfigLibrary.class);
  private static final Logger log = Log.open(ConfigLibrary.class);

  private static EnvironmentLocal<ConfigLibrary> _localLibrary =
    new EnvironmentLocal<ConfigLibrary>();

  private HashMap<String,Method> _methodMap = new HashMap<String,Method>();

  private ConfigLibrary()
  {
    configureLibrary();
  }

  public static ConfigLibrary getLocal()
  {
    return getLocal(Thread.currentThread().getContextClassLoader());
  }

  public static ConfigLibrary getLocal(ClassLoader loader)
  {
    loader = ClassLoader.getSystemClassLoader();
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);
      
      ConfigLibrary lib = _localLibrary.getLevel(loader);

      if (lib == null) {
	lib = new ConfigLibrary();

	_localLibrary.set(lib, loader);
      }

      return lib;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the method map.
   */
  public HashMap<String,Method> getMethodMap()
  {
    return _methodMap;
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusModule
   */ 
  private void configureLibrary()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      String library = "META-INF/services/com.caucho.config.ConfigLibrary";
      Enumeration<URL> urls = loader.getResources(library);

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        InputStream is = null;
        ReadStream rs =  null;
        try {
          is = url.openStream();
          rs = Vfs.openRead(is);

          parseServicesModule(rs);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
          if (is != null)
            is.close();
        }
      }

    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Parses the services file, looking for PHP services.
   */
  private void parseServicesModule(ReadStream in)
    throws IOException, ClassNotFoundException,
	   IllegalAccessException, InstantiationException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String line;

    while ((line = in.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
        String className = line;

        Class cl = Class.forName(className, false, loader);

        introspectLibraryClass(cl);
      }
    }
  }

  /**
   * Introspects the module class for functions.
   *
   * @param cl the class to introspect.
   */
  private void introspectLibraryClass(Class cl)
    throws IllegalAccessException, InstantiationException
  {
    log.fine("Config loading library " + cl.getName());

    for (Method method : cl.getMethods()) {
      if (! Modifier.isPublic(method.getModifiers()))
        continue;
      
      if (! Modifier.isStatic(method.getModifiers()))
        continue;

      _methodMap.put(method.getName(), method);
    }
  }
}
