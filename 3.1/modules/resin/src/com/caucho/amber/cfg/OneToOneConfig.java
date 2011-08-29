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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import java.util.HashMap;


/**
 * <one-to-one> tag in orm.xml
 */
public class OneToOneConfig extends AbstractRelationConfig {

  // attributes
  private boolean _isOptional;
  private String _mappedBy;

  // elements
  private HashMap<String, PrimaryKeyJoinColumnConfig> _primaryKeyJoinColumnMap
    = new HashMap<String, PrimaryKeyJoinColumnConfig>();

  private HashMap<String, JoinColumnConfig> _joinColumnMap
    = new HashMap<String, JoinColumnConfig>();

  public boolean getOptional()
  {
    return _isOptional;
  }

  public void setOptional(boolean isOptional)
  {
    _isOptional = isOptional;
  }

  public String getMappedBy()
  {
    return _mappedBy;
  }

  public void setMappedBy(String mappedBy)
  {
    _mappedBy = mappedBy;
  }

  public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumn(String columnName)
  {
    return _primaryKeyJoinColumnMap.get(columnName);
  }

  public void addPrimaryKeyJoinColumn(PrimaryKeyJoinColumnConfig primaryKeyJoinColumn)
  {
    _primaryKeyJoinColumnMap.put(primaryKeyJoinColumn.getName(),
                                 primaryKeyJoinColumn);
  }

  public HashMap<String, PrimaryKeyJoinColumnConfig> getPrimaryKeyJoinColumnMap()
  {
    return _primaryKeyJoinColumnMap;
  }

  public JoinColumnConfig getJoinColumn(String name)
  {
    return _joinColumnMap.get(name);
  }

  public void addJoinColumn(JoinColumnConfig joinColumn)
  {
    _joinColumnMap.put(joinColumn.getName(),
                       joinColumn);
  }

  public HashMap<String, JoinColumnConfig> getJoinColumnMap()
  {
    return _joinColumnMap;
  }
}
