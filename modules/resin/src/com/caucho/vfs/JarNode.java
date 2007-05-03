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

package com.caucho.vfs;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.CachedDependency;
import com.caucho.util.Alarm;
import com.caucho.util.CacheListener;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Entry for a Jar.
 */
public class JarNode
{
  private static final Logger log
    = Logger.getLogger(JarNode.class.getName());
  private static final L10N L = new L10N(JarNode.class);

  private final String _name;
  private final String _segment;

  private long _size;
  private long _time;

  private boolean _isDirectory;
  private boolean _exists;

  private ArrayList<JarNode> _children;

  JarNode(String name, String segment)
  {
    _name = name;
    _segment = segment;
    _size = 0;
    _time = Alarm.getCurrentTime();
    _isDirectory = true;
  }

  void fill(ZipEntry entry)
  {
    _size = entry.getSize();
    _time = entry.getTime();
    _isDirectory = entry.isDirectory();
    _exists = true;
  }

  public boolean isDirectory()
  {
    return _isDirectory;
  }

  public boolean exists()
  {
    return _exists;
  }

  public long getSize()
  {
    return _size;
  }

  public long getTime()
  {
    return _time;
  }

  public void addChild(JarNode child)
  {
    if (_children == null)
      _children = new ArrayList<JarNode>();

    _children.add(child);
  }

  public ArrayList<JarNode> getChildren()
  {
    return _children;
  }
}
