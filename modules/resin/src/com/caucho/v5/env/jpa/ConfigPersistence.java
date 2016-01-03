/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.env.jpa;

import java.net.URL;
import java.util.ArrayList;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.vfs.PathImpl;

/**
 * Top <persistence> tag for the persistence.xml
 */
public class ConfigPersistence {
  private final PathImpl _root;
  
  private String _version;
  
  private ArrayList<ConfigPersistenceUnit> _unitList
    = new ArrayList<ConfigPersistenceUnit>();

  public ConfigPersistence(PathImpl root)
  {
    _root = root;
  }

  public PathImpl getRoot()
  {
    return _root;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setSchemaLocation(String location)
  {
  }

  /**
   * Adds a new <persistence-unit>.
   */
  public ConfigPersistenceUnit createPersistenceUnit()
  {
    try {
      ConfigPersistenceUnit unit = new ConfigPersistenceUnit(new URL(_root.getURL()));
      
      unit.setVersion(_version);
    
      _unitList.add(unit);
    
      return unit;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Returns the unit list.
   */
  public ArrayList<ConfigPersistenceUnit> getUnitList()
  {
    return _unitList;
  }
}
