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
import java.util.*;
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
public class JarCache
{
  private static final Logger log
    = Logger.getLogger(JarCache.class.getName());
  private static final L10N L = new L10N(JarCache.class);

  private final HashMap<String,JarNode> _map
    = new HashMap<String,JarNode>();
  private final JarNode _root;

  private Boolean _isSigned;

  JarCache()
  {
    _root = new JarNode("", "");
    _map.put("/", _root);
  }

  JarNode get(String name)
  {
    JarNode node = _map.get(name);

    if (node != null)
      return node;

    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);

    return _map.get(name);
  }

  void add(ZipEntry entry)
  {
    String name = entry.getName();

    JarNode node = addNode(name);

    node.fill(entry);

    if (node.isDirectory())
      _map.put(name + "/", node);
  }

  Boolean isSigned()
  {
    return _isSigned;
  }

  void setSigned(Boolean isSigned)
  {
    _isSigned = isSigned;
  }

  private JarNode addNode(String childName)
  {
    if (childName.endsWith("/"))
      childName = childName.substring(0, childName.length() - 1);
        
    String mapName = childName;

    if (! mapName.startsWith("/"))
      mapName = "/" + mapName;

    JarNode node = _map.get(mapName);

    if (node != null)
      return node;
    
    JarNode parent;
    int len = childName.length();
    int p = childName.lastIndexOf('/', len - 1);
    
    if (p < 0) {
      parent = _root;

      node = new JarNode(mapName, childName);
    }
    else {
      String parentName = childName.substring(0, p + 1);
      parent = addNode(parentName);

      node = new JarNode(childName, childName.substring(p + 1));
    }
    
    _map.put(mapName, node);

    parent.addChild(node);

    return node;
  }
}
