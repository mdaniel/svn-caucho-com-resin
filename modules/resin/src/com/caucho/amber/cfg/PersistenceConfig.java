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

package com.caucho.amber.cfg;

import java.util.ArrayList;

import com.caucho.vfs.Path;

/**
 * Top <persistence> tag for the persistence.xml
 */
public class PersistenceConfig {
  private Path _root;
  
  private ArrayList<PersistenceUnitConfig> _unitList
    = new ArrayList<PersistenceUnitConfig>();

  public void setRoot(Path root)
  {
    _root = root;
  }

  public Path getRoot()
  {
    return _root;
  }

  public void setVersion(String version)
  {
  }

  public void setSchemaLocation(String location)
  {
  }

  /**
   * Adds a new <persistence-unit>.
   */
  public void addPersistenceUnit(PersistenceUnitConfig unit)
  {
    _unitList.add(unit);
  }

  /**
   * Returns the unit list.
   */
  public ArrayList<PersistenceUnitConfig> getUnitList()
  {
    return _unitList;
  }
}
