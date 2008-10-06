/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.zip.*;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class ExportLoader extends Loader implements Dependency
{
  private static final Logger log
    = Logger.getLogger(ExportLoader.class.getName());
  
  // The exported jar
  private JarPath _jar;
  private Depend _depend;

  private String _packageName;
  private String _packagePath;
  private OsgiVersion _version;
  
  private CodeSource _codeSource;

  /**
   * Creates a export loader.
   */
  ExportLoader(JarPath jar, String packageName, OsgiVersion version)
  {
    _jar = jar;
    _depend = new Depend(jar);
    
    _packageName = packageName;
    _packagePath = packageName.replace('.', '/');
    _version = version;
  }

  /**
   * Initialize
   */
  protected void init()
  {
    try {
      _codeSource = new CodeSource(new URL(_jar.getContainer().getURL()), (Certificate []) null);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Sets the owning class loader.
   */
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);

    loader.addURL(_jar);
  }
  
  /**
   * True if the jar has been modified.
   */
  public boolean isModified()
  {
    return _depend.isModified();
  }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  public boolean logModified(Logger log)
  {
    return _depend.logModified(log);
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  @Override
  protected void buildClassPath(ArrayList<String> pathList)
  {
    String path = _jar.getContainer().getNativePath();

    if (! pathList.contains(path))
      pathList.add(path);
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
    if (! pathName.startsWith(_packagePath))
      return null;

    Path path = _jar.lookup(pathName);

    if (path.exists())
      return path;

    return null;
  }

  /**
   * Returns the code source for the directory.
   */
  protected CodeSource getCodeSource(Path path)
  {
    return _codeSource;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _packageName + "," + _version + "]");
  }
}
