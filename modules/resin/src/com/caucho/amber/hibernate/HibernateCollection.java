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

import java.util.ArrayList;

import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.CollectionField;
import com.caucho.amber.field.AssociationField;
import com.caucho.amber.field.EntityOneToManyField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;

/**
 * configuration for an entity
 */
public class HibernateCollection {
  private static final L10N L = new L10N(HibernateCollection.class);

  private String _name;
  private EntityType _type;

  private CollectionField _field;
  
  private Type _resultType;
  private String _table;

  private LinkColumns _keyColumns;
  
  HibernateCollection(EntityType type)
  {
    _type = type;
  }

  public void setField(CollectionField field)
  {
    _field = field;

    getOwnerType().addField(_field);
  }

  protected CollectionField getField()
  {
    return _field;
  }

  public void setName(String name)
    throws ConfigException
  {
    _name = name;
  }

  String getName()
    throws ConfigException
  {
    return _name;
  }

  void setKeyColumns(LinkColumns columns)
    throws ConfigException
  {
    _keyColumns = columns;
  }

  public void setType(String type)
    throws ConfigException
  {
    _resultType = _type.getAmberManager().createType(type);
  }

  Type getType()
    throws ConfigException
  {
    return _resultType;
  }

  EntityType getOwnerType()
  {
    return _type;
  }

  public void setTable(String table)
  {
    _table = table;
  }

  public Key createKey()
  {
    return new Key();
  }

  public Element createElement()
  {
    AssociationField field;
    field = new AssociationField(getOwnerType());
  
    setField(field);
    
    return new Element(field, getOwnerType());
  }

  public OneToMany createOneToMany()
  {
    setField(new EntityOneToManyField(getOwnerType()));
    
    return new OneToMany(_field, getOwnerType());
  }

  public ManyToMany createManyToMany()
  {
    AssociationField field = new AssociationField(getOwnerType());
    
    setField(field);
    
    return new ManyToMany(field, getOwnerType());
  }
  
  public void init()
    throws ConfigException
  {
    if (_resultType == null) {
      JClass resultClass = _field.getGetterMethod().getReturnType();
      _resultType = _type.getAmberManager().createType(resultClass);
    }

    _field.setName(_name);
    _field.setType(_resultType);
    if (_keyColumns != null)
      _field.setLinkColumns(_keyColumns);
    _field.setTable(_table);
  }

  public class Key {
    private ArrayList<HibernateColumn> _columns =
      new ArrayList<HibernateColumn>();
    
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
      if (_columns.size() == 0) {
	HibernateColumn column = new HibernateColumn();

	column.setName(getName());
	_columns.add(column);
      }
      
      Id id = getOwnerType().getId();

      ArrayList<Column> keys = id.getColumns();

      if (keys.size() != _columns.size())
	throw new ConfigException(L.l("The number of columns must match the number of keys"));

      Table ownerTable = getOwnerType().getTable();
      ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();
      
      for (int i = 0; i < _columns.size(); i++) {
	HibernateColumn hColumn = _columns.get(i);

	ForeignColumn column;
	column = ownerTable.createForeignColumn(hColumn.getName(),
						keys.get(i));

	columns.add(column);
      }

      /*
      setColumns(new LinkColumns(ownerTable,
				 resultType.getTable(),
				 columns));
      */
    }
  }

  public class Target {
    private CollectionField _field;
    private EntityType _entityType;
    
    Target(CollectionField field, EntityType type)
    {
      _entityType = type;
      _field = field;
    }
  }

  public class Association {
    private EntityType _entityType;
    private AssociationField _field;
    
    private ArrayList<HibernateColumn> _columns =
      new ArrayList<HibernateColumn>();
    
    Association(AssociationField field, EntityType type)
    {
      _entityType = type;
      _field = field;
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
      if (_columns.size() > 0) {
	Id id = _field.getSourceType().getId();

	ArrayList<IdField> keys = id.getKeys();

	if (keys.size() != _columns.size())
	  throw new ConfigException(L.l("The number of columns must match the number of keys"));

	ArrayList<Column> columns = new ArrayList<Column>();
      
	for (int i = 0; i < _columns.size(); i++) {
	  HibernateColumn hColumn = _columns.get(i);

	  Column column =
	    getOwnerType().getTable().createColumn(hColumn.getName(),
						   keys.get(i).getType());

	  columns.add(column);
	}

	// _field.setSourceColumns(columns);
      }
    }
  }

  public class Element extends Association {
    Element(AssociationField field, EntityType type)
    {
      super(field, type);
    }

    public void setType(String type)
      throws ConfigException
    {
      HibernateCollection.this.setType(type);
    }
  }
  
  public class OneToMany extends Target {
    OneToMany(CollectionField field, EntityType type)
    {
      super(field, type);
    }

    public void setClass(String type)
      throws ConfigException
    {
      HibernateCollection.this.setType(type);
    }
  }
  
  public class ManyToMany extends Association {
    ManyToMany(AssociationField field, EntityType type)
    {
      super(field, type);
    }

    public void setClass(String type)
      throws ConfigException
    {
      HibernateCollection.this.setType(type);
    }
  }
}
