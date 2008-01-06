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

package com.caucho.amber.type;

import com.caucho.amber.entity.MappedSuperclass;
import com.caucho.amber.gen.*;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.*;
import com.caucho.util.L10N;

import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * Represents a non-persistent class with abstract O/R mapping information.
 */
public class MappedSuperclassType extends RelatedType {
  private static final Logger log = Logger.getLogger(MappedSuperclassType.class.getName());
  private static final L10N L = new L10N(MappedSuperclassType.class);

  public MappedSuperclassType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
  }

  /**
   * Gets the instance class.
   */
  public Class getInstanceClass()
  {
    return getInstanceClass(MappedSuperclass.class);
  }

  /**
   * Returns the component interface name.
   */
  public String getComponentInterfaceName()
  {
    return "com.caucho.amber.entity.MappedSuperclass";
  }

  /**
   * Gets a component generator.
   */
  public AmberMappedComponent getComponentGenerator()
  {
    return new MappedSuperclassComponent();
  }

  /**
   * Returns the root type.
   */
  public RelatedType getRootType()
  {
    return null;
  }

  /**
   * Returns the table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns the columns.
   */
  public ArrayList<Column> getColumns()
  {
    // jpa/0ge2
    if (getTable() == null)
      return null;

    return getTable().getColumns();
  }

  /**
   * Sets the table.
   */
  public void setTable(Table table)
  {
  }

  /**
   * Printable version of the mapped superclass.
   */
  public String toString()
  {
    return "MappedSuperclassType[" + _beanClass.getName() + "]";
  }
}
