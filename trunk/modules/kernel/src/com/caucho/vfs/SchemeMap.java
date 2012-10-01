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

package com.caucho.vfs;

import java.util.HashMap;

import com.caucho.loader.EnvironmentLocal;

/**
 * The top-level filesystem schemes are collected into a single map.
 *
 * <p>The default scheme has a number of standard filesystems, file:, mailto:,
 * jndi:, http:.
 *
 * <p>Applications can add schemes in the configuration file.  When first
 * accessed, the SchemeMap will look in the Registry to match the scheme.
 * If the new scheme exists, it will instantiate a single root instance and
 * use that for the remainder of the application.
 * <code><pre>
 * &lt;caucho.com>
 *  &lt;vfs scheme="foo" class-name="test.vfs.FooPath"/>
 * &lt;/caucho.com>
 * </pre></code>
 */
public class SchemeMap {
  // Constant null scheme map for protected filesystems.
  private static SchemeMap _nullSchemeMap;

  private final EnvironmentLocal<HashMap<String,SchemeRoot>> _schemeMap
    = new EnvironmentLocal<HashMap<String,SchemeRoot>>();

  /**
   * Create an empty SchemeMap.
   */
  public SchemeMap()
  {
    _schemeMap.set(new HashMap<String,SchemeRoot>(), null);
  }

  /**
   * Create an empty SchemeMap.
   */
  private SchemeMap(HashMap<String,SchemeRoot> map)
  {
    getUpdateMap().putAll(map);
  }

  /**
   * The null scheme map is useful for protected filesystems as used
   * in createRoot().  That way, no dangerous code can get access to
   * files using, for example, the file: scheme.
   */
  static SchemeMap getNullSchemeMap()
  {
    synchronized (SchemeMap.class) {
      if (_nullSchemeMap == null) {
        _nullSchemeMap = new SchemeMap();
      }
      
      return _nullSchemeMap;
    }
  }

  /**
   * Gets the scheme from the schemeMap.
   */
  public Path get(String scheme)
  {
    SchemeRoot root = getMap().get(scheme);
    
    Path path = null;
    
    if (root != null)
      path = root.getRoot();
    
    if (path != null) {
      return path;
    }
    else {
      return new NotFoundPath(this, scheme + ":");
    }
  }
  
  public SchemeRoot getSchemeRoot(String scheme)
  {
    return getMap().get(scheme);
  }

  /**
   * Puts a new value in the schemeMap.
   */
  public Path put(String scheme, Path path)
  {
    SchemeRoot oldRoot = getUpdateMap().put(scheme, new SchemeRoot(path));
    
    return oldRoot != null ? oldRoot.getRoot() : null;
  }

  /**
   * Puts a new value in the schemeMap.
   */
  public SchemeRoot put(String scheme, SchemeRoot root)
  {
    return getUpdateMap().put(scheme, root);
  }

  public SchemeMap copy()
  {
    return new SchemeMap(getMap());
  }

  /**
   * Removes value from the schemeMap.
   */
  public Path remove(String scheme)
  {
    SchemeRoot oldRoot = getUpdateMap().remove(scheme);
    
    return oldRoot != null ? oldRoot.getRoot() : null;
  }
  
  private HashMap<String,SchemeRoot> getMap()
  {
    HashMap<String,SchemeRoot> map = _schemeMap.get();
    
    return map;
  }
  
  private HashMap<String,SchemeRoot> getUpdateMap()
  {
    HashMap<String,SchemeRoot> map = _schemeMap.getLevel();
    
    if (map == null) {
      HashMap<String,SchemeRoot> oldMap = getMap();
      
      if (oldMap != null)
        map = new HashMap<String,SchemeRoot>(oldMap);
      else
        map = new HashMap<String,SchemeRoot>();
      
      _schemeMap.set(map);
    }
    
    return map;
  }
}
