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
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg21;

import com.caucho.ejb.cfg.*;
import com.caucho.ejb.cfg21.CmpField;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.EntityManyToManyField;
import com.caucho.amber.field.Id;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.SelfEntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.ql.QLParser;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * many-to-many relation
 */
public class CmrManyToMany extends CmrRelation {
  private static final L10N L = new L10N(CmrManyToMany.class);

  private String _sqlTable;
  
  private EjbEntityBean _targetBean;
  private String _targetField;

  // true if the target is only in a single instance
  private boolean _isTargetUnique;

  private ArrayList<String> _orderByFields;
  private ArrayList<Boolean> _orderByAscending;
  
  private SqlRelation []_keySQLColumns;
  private SqlRelation []_dstSQLColumns;

  private EntityManyToManyField _amberManyToMany;

  /**
   * Creates a new cmp-relation
   */
  public CmrManyToMany(EjbEntityBean entityBean,
		       String fieldName,
		       EjbEntityBean targetBean,
		       String targetField)
    throws ConfigException
  {
    super(entityBean, fieldName);

    _targetBean = targetBean;
    _targetField = targetField;
  }

  /**
   * Returns the target bean
   */
  public EjbEntityBean getTargetBean()
  {
    return _targetBean;
  }

  /**
   * Sets the sql table.
   */
  public void setSQLTable(String sqlTable)
  {
    _sqlTable = sqlTable;
  }

  /**
   * Gets the sql table.
   */
  public String getSQLTable()
  {
    if (_sqlTable != null)
      return _sqlTable;
    else
      return getRelationName();
  }

  /**
   * Sets true if the target is unique.
   */
  public void setTargetUnique(boolean isUnique)
  {
    _isTargetUnique = isUnique;
  }

  /**
   * Sets true if the target is unique.
   */
  public boolean isTargetUnique()
  {
    return _isTargetUnique;
  }

  /**
   * Returns the target type.
   */
  public ApiClass getTargetType()
  {
    return _targetBean.getLocal();
  }

  /**
   * Sets the column.
   */
  public void setKeySQLColumns(SqlRelation []columns)
  {
    _keySQLColumns = columns;
  }

  /**
   * Gets the column.
   */
  public SqlRelation []getKeySQLColumns()
  {
    return _keySQLColumns;
  }

  /**
   * Sets the column.
   */
  public void setDstSQLColumns(SqlRelation []columns)
  {
    _dstSQLColumns = columns;
  }

  /**
   * Gets the column.
   */
  public SqlRelation []getDstSQLColumns()
  {
    return _dstSQLColumns;
  }

  /**
   * Sets the order by.
   */
  public void setOrderBy(String orderBySQL)
    throws ConfigException
  {
    if (orderBySQL != null) {
      ArrayList<String> fields = new ArrayList<String>();
      ArrayList<Boolean> asc = new ArrayList<Boolean>();

      QLParser.parseOrderBy(_targetBean, orderBySQL, fields, asc);
      
      _orderByFields = fields;
      _orderByAscending = asc;
    }
  }

  /**
   * The OneToMany is a collection.
   */
  public boolean isCollection()
  {
    return true;
  }

  /**
   * Returns the amber field.
   */
  public EntityManyToManyField getAmberField()
  {
    return _amberManyToMany;
  }

  /**
   * Create any bean methods.
   */
  public EjbMethod createGetter(EjbView view,
				ApiMethod apiMethod,
				ApiMethod implMethod)
    throws ConfigException
  {
    return new EjbManyToManyMethod(view, apiMethod, implMethod, this);
    
  }

  /**
   * Creates the amber type.
   */
  public AmberField assembleAmber(SelfEntityType type)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = type.getPersistenceUnit();

    Table map = persistenceUnit.createTable(getSQLTable());

    map.setConfigLocation(getLocation());

    EntityManyToManyField manyToMany;

    manyToMany = new EntityManyToManyField(type, getName());
    _amberManyToMany = manyToMany;
    
    manyToMany.setAssociationTable(map);

    SelfEntityType targetType = _targetBean.getEntityType();
    manyToMany.setType(targetType);

    ArrayList<ForeignColumn> targetColumns =
      calculateColumns(map, targetType, _targetField, _dstSQLColumns);

    manyToMany.setTargetLink(new LinkColumns(map,
					     targetType.getTable(),
					     targetColumns));
    
    SelfEntityType sourceType = getBean().getEntityType();
    // manyToMany.setType(targetType);


    ArrayList<ForeignColumn> sourceColumns =
      calculateColumns(map, sourceType, getName(), _keySQLColumns);

    manyToMany.setSourceLink(new LinkColumns(map,
					     sourceType.getTable(),
					     sourceColumns));

    manyToMany.setOrderBy(_orderByFields, _orderByAscending);
      
    manyToMany.init();

    return manyToMany;
  }

  private ArrayList<ForeignColumn>
    calculateColumns(Table mapTable, SelfEntityType type, String fieldName,
		     SqlRelation []sqlColumns)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    Id id = type.getId();
    ArrayList<Column> keys = id.getColumns();

    if (sqlColumns != null && sqlColumns.length == keys.size()) {
      for (int i = 0; i < sqlColumns.length; i++) {
	ForeignColumn column =
	  mapTable.createForeignColumn(sqlColumns[i].getSQLColumn(),
				       keys.get(i));
	columns.add(column);
      }
    }
    else if (keys.size() == 1) {
      Column key = keys.get(0);

      String sqlColumn;

      /*
      if (fieldName != null)
	sqlColumn = CmpField.toSqlName(type.getName());
      else
      sqlColumn = key.getColumn().getName();
      */
      if (type.getTable().getName() != null)
	sqlColumn = type.getTable().getName();
      else
	sqlColumn = CmpField.toSqlName(type.getName());

      columns.add(mapTable.createForeignColumn(sqlColumn, key));
    }
    else {
      String baseSqlColumn;

      if (sqlColumns != null && sqlColumns.length == 1)
	baseSqlColumn = sqlColumns[0].getSQLColumn();
      else
	baseSqlColumn = type.getTable().getName();

      if (baseSqlColumn == null)
	baseSqlColumn = CmpField.toSqlName(type.getName());

      for (int i = 0; i < keys.size(); i++) {
	Column key = keys.get(i);
	
	String sqlColumn = baseSqlColumn + "_" + key.getName();
	
	ForeignColumn foreignColumn =
	  mapTable.createForeignColumn(sqlColumn, key);

	columns.add(foreignColumn);
      }
    }

    return columns;
  }
}
