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

package com.caucho.env.deploy;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The generator for the deploy
 */
class ExpandVersionManager
{
  private final String _id;
  
  private final boolean _isVersioning;
  
  private final TreeMap<String,ExpandVersion> _versionMap;
  
  private final TreeMap<String,ExpandVersionGroup> _baseVersionMap;
  
  /**
   * Creates the deploy.
   */
  public ExpandVersionManager(String id, 
                              TreeSet<String> keySet,
                              boolean isVersioning)
  {
    _id = id;
    
    _isVersioning = isVersioning;
    
    _versionMap = buildVersionMap(keySet);
    _baseVersionMap = buildVersionGroupMap(_versionMap);
  }

  /**
   * Returns the version by the full key.
   */
  public ExpandVersion getVersion(String key)
  {
    if (key != null)
      return _versionMap.get(key);
    else
      return null;
  }
  
  /**
   * Returns the version group by the base key.
   */
  public ExpandVersionGroup getBaseVersionGroup(String baseKey)
  {
    if (baseKey != null)
      return _baseVersionMap.get(baseKey);
    else
      return null;
  }
  
  /**
   * Returns the primary version for a base key.
   */
  public ExpandVersion getPrimaryVersion(String baseKey)
  {
    ExpandVersionGroup versionGroup = getBaseVersionGroup(baseKey);
    
    if (versionGroup != null)
      return versionGroup.getPrimaryVersion();
    else
      return null;
  }
  
  /**
   * Returns the set of base keys.
   */
  public Set<String> getBaseKeySet()
  {
    return _baseVersionMap.keySet();
  }
  
  /**
   * Returns the set of full versioned keys.
   */
  public Set<String> getKeySet()
  {
    return _versionMap.keySet();
  }
  
  /**
   * Version map uses the repository
   */
  private TreeMap<String,ExpandVersion> buildVersionMap(TreeSet<String> keySet)
  {
    TreeMap<String,ExpandVersion> versionMap
      = new TreeMap<String,ExpandVersion>();

    for (String key : keySet) {
      ExpandVersion version = createVersion(key);
        
      versionMap.put(key, version);
    }

    return versionMap;
  }

  private ExpandVersion createVersion(String key)
  {
    String baseKey = key;
    String version = "";

    int p = baseKey.lastIndexOf('-');
    if (p > 0) {
      version = key.substring(p + 1);
      
      if (isValidVersion(version)) {
        baseKey = key.substring(0, p);
      }
      else {
        version = "";
      }
    }

    return new ExpandVersion(key, baseKey, version);
  }
  
  private boolean isValidVersion(String version)
  {
    if (! _isVersioning)
      return false;
    
    int length = version.length();
    
    boolean isDigit = false;
    
    for (int i = 0; i < length; i++) {
      char ch = version.charAt(i);
      
      if ('0' <= ch && ch <= '9')
        isDigit = true;
      else if (ch == '.')
        return isDigit;
      else
        return false;
    }

    return isDigit;
  }
  
  /**
   * Builds the base to group map.
   */
  private TreeMap<String,ExpandVersionGroup> 
  buildVersionGroupMap(TreeMap<String,ExpandVersion> versionMap)
  {
    TreeMap<String,ExpandVersionGroup> baseVersionMap
      = new TreeMap<String,ExpandVersionGroup>();

    for (ExpandVersion version : versionMap.values()) {
      String baseKey = version.getBaseKey();
      
      ExpandVersionGroup versionGroup = baseVersionMap.get(baseKey);
        
      if (versionGroup == null) {
        versionGroup = new ExpandVersionGroup(version);
        baseVersionMap.put(baseKey, versionGroup);
      }
      else {
        versionGroup.addVersion(version);
      }
    }

    return baseVersionMap;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
