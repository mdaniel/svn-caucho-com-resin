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

package com.caucho.amber.hibernate;

import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.AbstractField;
import com.caucho.amber.field.PropertyField;

import com.caucho.amber.table.Column;

/**
 * configuration for an entity
 */
public class HibernateProperty extends HibernateField {
  private static final L10N L = new L10N(HibernateProperty.class);

  private PropertyField _field;
  private HibernateColumn _column = new HibernateColumn();
  
  HibernateProperty(EntityType type)
  {
    super(type);

    _field = new PropertyField(type);

    setField(_field);
  }

  /**
   * Sets the name of the property.
   */
  public void setName(String name)
    throws ConfigException
  {
    super.setName(name);

    _column.setName(name);

    JMethod getter = _field.getGetterMethod();

    if (getter != null && getter.getReturnType().isPrimitive())
      _column.setNotNull(true);
  }

  /**
   * Sets the field
   */
  protected void setField(AbstractField field)
  {
    super.setField(field);

    _field = (PropertyField) field;
  }

  public HibernateColumn createColumn()
  {
    return _column;
  }

  /**
   * Sets the not-null column property.
   */
  public void setNotNull(boolean isNotNull)
  {
    _column.setNotNull(isNotNull);
  }

  /**
   * Sets the length column property.
   */
  public void setLength(int length)
  {
    _column.setLength(length);
  }

  /**
   * Sets the sql-type column property.
   */
  public void setSQLType(String sqlType)
  {
    _column.setSQLType(sqlType);
  }

  /**
   * Sets the unique column property.
   */
  public void setUnique(boolean isUnique)
  {
    _column.setUnique(isUnique);
  }

  /**
   * Sets the unique-key column property.
   */
  public void setUniqueKey(String uniqueKey)
  {
    _column.setUniqueKey(uniqueKey);
  }

  /**
   * Sets the index column property.
   */
  public void setIndex(String index)
  {
    _column.setIndex(index);
  }

  /**
   * Set true if the property should be saved on an insert.
   */
  public void setInsert(boolean isInsert)
  {
    _field.setInsert(isInsert);
  }

  /**
   * Set true if the property should be saved on an update.
   */
  public void setUpdate(boolean isUpdate)
  {
    _field.setUpdate(isUpdate);
  }

  public void init()
    throws ConfigException
  {
    super.init();

    _field.setType(getType());
    
    Column column =
      getOwnerType().getTable().createColumn(_column.getName(), getType());
    column.setNotNull(_column.getNotNull());
    column.setUnique(_column.getUnique());
    column.setLength(_column.getLength());
    column.setSQLType(_column.getSQLType());

    _field.setColumn(column);

    getOwnerType().addField(_field);
  }
}
