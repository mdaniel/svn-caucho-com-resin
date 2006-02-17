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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.hibernate;

import java.util.ArrayList;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.EntityManyToOneField;

import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;

/**
 * configuration for an entity
 */
public class HibernateManyToOne extends HibernateField {
  private static final L10N L = new L10N(HibernateManyToOne.class);

  private ArrayList<HibernateColumn> _columns =
    new ArrayList<HibernateColumn>();

  private EntityManyToOneField _manyToOne;

  HibernateManyToOne(EntityType type)
  {
    super(type);

    _manyToOne = new EntityManyToOneField(type);
    
    setField(_manyToOne);
  }

  /**
   * Adds a column.
   */
  public void addColumn(HibernateColumn column)
  {
    _columns.add(column);
  }

  public void init()
    throws ConfigException
  {
    super.init();

    _manyToOne.setType(getType());

    if (_columns.size() > 0) {
      ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();
      
      for (int i = 0; i < _columns.size(); i++) {
	HibernateColumn hColumn = _columns.get(i);

	/* XXX:
	ForeignColumn column = new ForeignColumn(getOwnerType().getTable(),
						 hColumn.getName(),
						 hColumn.getType());
	
	columns.add(column);
	*/
      }

      LinkColumns link = new LinkColumns(getOwnerType().getTable(),
					 ((EntityType) getType()).getTable(),
					 columns);
      _manyToOne.setLinkColumns(link);
    }
    /*
    else {
      EntityColumn entityColumn = new EntityColumn(getOwnerType(), _manyToOne);
      _manyToOne.setColumn(entityColumn);
    }
    */

    // getProperty().init();
    _manyToOne.init();
    
    getOwnerType().addField(_manyToOne);
  }
}
