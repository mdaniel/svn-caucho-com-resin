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

package com.caucho.make;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.LogStream;
import com.caucho.vfs.JarPath;

import com.caucho.log.Log;

import com.caucho.loader.Loader;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;

import com.caucho.config.DynamicBean;
import com.caucho.config.DynamicItem;

import com.caucho.make.task.JavacTask;
import com.caucho.make.task.DocletTask;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class MakeLoader extends Loader implements DynamicBean, Make {
  private static final Logger log = Log.open(MakeLoader.class);

  private static final EnvironmentLocal<DynamicItem[]> _localConfig =
    new EnvironmentLocal<DynamicItem[]>();

  private static DynamicItem []_configItems;
    
  // The class directory
  private Path _path;

  // The task list
  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();
  private ArrayList<Make> _makeList = new ArrayList<Make>();

  /**
   * Null constructor for the make loader.
   */
  public MakeLoader()
  {
  }

  public DynamicItem []getDynamicConfigurationElements()
  {
    DynamicItem []configItems = _localConfig.get();

    if (configItems == null) {
      ArrayList<DynamicItem> items = new ArrayList<DynamicItem>();

      items.add(new DynamicItem("javac", JavacTask.class, "make"));
      items.add(new DynamicItem("doclet", DocletTask.class, "make"));

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      try {
	Class taskClass = Class.forName("com.caucho.ejb.doclet.EjbDocletTask",
					false,
					loader);

	items.add(new DynamicItem("ejb-doclet", taskClass, "make"));
      } catch (Throwable e) {
	log.log(Level.FINEST, e.toString(), e);
      }

      configItems = items.toArray(new DynamicItem[items.size()]);

      _localConfig.set(configItems);
    }
    
    return configItems;
  }

  /**
   * Sets the resource directory.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the resource path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the owning class loader.
   */
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);

    loader.addURL(_path);
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
    for (int i = 0; i < _makeList.size(); i++) {
      Make make = _makeList.get(i);

      try {
        make.make();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    return _path.lookup(name);
  }

  /**
   * Sets the make config.
   */
  public void addTask(TaskConfig task)
  {
    Object obj = task.getTask();

    if (obj instanceof Make)
      _makeList.add((Make) obj);
  }

  /**
   * Sets the make config.
   */
  public void addMake(Make make)
  {
    _makeList.add(make);
  }

  /**
   * Makes the loader.
   */
  public void make()
    throws Exception
  {
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
    
    if (_path instanceof JarPath)
      cb.append(((JarPath) _path).getContainer().getNativePath());
    else if (_path.isDirectory())
      cb.append(_path.getNativePath());

    return cb.toString();
  }

  /**
   * Returns a printable representation of the loader.
   */
  public String toString()
  {
    return "MakeLoader[" + _path + "]";
  }

  static {
  };
}


