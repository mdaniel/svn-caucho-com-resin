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
 * @author Emil Ong
 */

package com.caucho.ant.jaxb;

import java.io.*;

import java.util.*;

import java.net.URL;

import com.caucho.util.*;
import com.caucho.vfs.*;


/**
 * Class loader which loads all the classes in a directory tree.
 *
 * The JAXB TCK has tests with package access classes.  This loader
* deals with this irregularity.
 */
public class GreedyClassLoader extends java.net.URLClassLoader
{
  private final static URL[] NULL_URL_ARRAY = new URL[0];

  private ClassLoader _parent;
  private Hashtable<String,Class> _classes = new Hashtable<String,Class>();
  private ArrayList<Class> _jaxbClasses = new ArrayList<Class>();
  private ByteBuffer _buffer = new ByteBuffer();
  private HashMap<File,String> _problemFiles = new HashMap<File,String>();

  public GreedyClassLoader(ClassLoader parent)
  {
    super(NULL_URL_ARRAY);
    _parent = parent;
  }

  public Class<?> loadClass(String name) 
    throws ClassNotFoundException
  {
    if (_classes.containsKey(name))
      return _classes.get(name);

    return _parent.loadClass(name);
  }

  public Class<?> loadClassFile(File file, String packagePrefix)
    throws IOException
  {
    String name = file.getName();
    String className = name.substring(0, name.length() - ".class".length());

    _buffer.clear();
    _buffer.ensureCapacity((int) file.length());

    Path path = Vfs.lookup(file.getPath());
    ReadStream rs = path.openRead();

    try {
      rs.readAll(_buffer.getBuffer(), 0, (int) file.length());

      Class cl = null;

      if (packagePrefix == null) {
        cl = defineClass(packagePrefix + "." + className, 
                         _buffer.getBuffer(), 0, (int) file.length());
        
        if (! "package-info".equals(className))
          _jaxbClasses.add(cl);

        _classes.put(className, cl);
      } 
      else {
        cl = defineClass(className, 
                         _buffer.getBuffer(), 0, (int) file.length());

        if (! "package-info".equals(className))
          _jaxbClasses.add(cl);

        _classes.put(packagePrefix + "." + className, cl);
      }

      return cl;
    }
    finally {
      rs.close();
    }
  }

  public void loadClassFiles(File root)
    throws IOException
  {
    loadClassFiles(root, null);

    int startSize;

    // XXX
    // Very crude method to deal with dependencies
    do {
      startSize = _problemFiles.size();

      Iterator<Map.Entry<File,String>> i = _problemFiles.entrySet().iterator();

      while (i.hasNext()) {
        try {
          Map.Entry<File,String> problem = i.next();

          loadClassFile(problem.getKey(), problem.getValue());

          i.remove();
        }
        catch (NoClassDefFoundError e) {
        }
      }
    }
    while (_problemFiles.size() < startSize);
  }

  public void loadClassFiles(File root, String packagePrefix)
    throws IOException
  {
    if (root.isDirectory()) {
      for (File child : root.listFiles()) {
        // if we have a root like /x/y/z with a class /x/y/z/a/b.class
        // then we need to produce a.b.  When packagePrefix == null, we 
        // are at /x/y/z, so we don't want to pick up z.  When 
        // packagePrefix == "", that means the current root is a 
        // subpackage, so we can append their name to the packagePrefix.
        // Basically, this is an off-by-one fix.
        
        if (packagePrefix == null)
          loadClassFiles(child, "");
        else if ("".equals(packagePrefix))
          loadClassFiles(child, root.getName());
        else
          loadClassFiles(child, packagePrefix + "." + root.getName());
      }
    }
    else {
      if (root.getPath().endsWith(".class")) {
        try {
          loadClassFile(root, packagePrefix);
        }
        catch (NoClassDefFoundError e) {
          _problemFiles.put(root, packagePrefix);
        }
      }
    }
  }

  public Class[] getLoadedClasses()
  {
    return _jaxbClasses.toArray(new Class[0]);
  }
}
