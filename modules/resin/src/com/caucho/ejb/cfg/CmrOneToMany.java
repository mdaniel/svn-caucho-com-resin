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

package com.caucho.ejb.cfg;

import java.util.ArrayList;

import java.io.IOException;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.java.JavaWriter;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.EntityOneToManyField;
import com.caucho.amber.field.KeyManyToOneField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.AmberField;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.LinkColumns;

import com.caucho.ejb.ql.QLParser;

/**
 * one-to-many relation
 */
public class CmrOneToMany extends CmrRelation {
  private static final L10N L = new L10N(CmrOneToMany.class);

  private EjbEntityBean _targetBean;
  private String _targetField;

  private ArrayList<String> _orderByFields;
  private ArrayList<Boolean> _orderByAscending;
  
  private SqlRelation []_sqlColumns;

  private EntityOneToManyField _amberOneToMany;

  /**
   * Creates a new cmp-relation
   */
  public CmrOneToMany(EjbEntityBean entityBean,
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
   * Returns the target type.
   */
  public JClass getTargetType()
  {
    return _targetBean.getLocal();
  }

  /**
   * Sets the column.
   */
  public void setSQLColumns(SqlRelation []columns)
  {
    _sqlColumns = columns;
  }

  /**
   * Gets the column.
   */
  public SqlRelation []getSQLColumns()
  {
    return _sqlColumns;
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
   * Create any bean methods.
   */
  public EjbMethod createGetter(EjbView view,
				JMethod apiMethod,
				JMethod implMethod)
    throws ConfigException
  {
    return new EjbOneToManyMethod(view, apiMethod, implMethod, this);
  }

  /**
   * Creates the amber type.
   */
  public AmberField assembleAmber(EntityType type)
    throws ConfigException
  {
    EntityOneToManyField oneToMany = new EntityOneToManyField(type, getName());

    AmberPersistenceUnit manager = type.getPersistenceUnit();
    
    EntityType targetType = _targetBean.getEntityType();
    oneToMany.setType(targetType);

    // if bi-directional, then other side handles it
    // if (! (getTargetRelation() instanceof CmrManyToOne)) {
  
    oneToMany.setOrderBy(_orderByFields, _orderByAscending);
    
    _amberOneToMany = oneToMany;

    return oneToMany;
  }

  /**
   * Link amber.
   */
  public void linkAmber()
    throws ConfigException
  {
    CmrManyToOne manyToOne = (CmrManyToOne) getTargetRelation();

    _amberOneToMany.setSourceField(manyToOne.getAmberManyToOne());
    _amberOneToMany.setLinkColumns(manyToOne.getAmberManyToOne().getLinkColumns());
    
    _amberOneToMany.init();
  }

  /**
   * Generates the destroy method.
   */
  public void generateAfterCommit(JavaWriter out)
    throws IOException
  {
    if (getHasGetter())
      out.println("__caucho_" + getName() + " = null;");
  }

  private LinkColumns calculateColumn(EntityType parentType,
				      EntityType childType,
				      String fieldName,
				      SqlRelation []sqlColumns)
    throws ConfigException
  {
    Id id = parentType.getId();
    ArrayList<Column> keys = new ArrayList<Column>(id.getColumns());
    ArrayList<ForeignColumn> columns = new ArrayList();

    // XXX: need to remove self from the keys if identifying
    /*
    for (int i = keys.size() - 1; i >= 0; i--) {
      IdField key = keys.get(i);

      if (key instanceof KeyManyToOneField) {
	KeyManyToOneField manyToOne = (KeyManyToOneField) key;

	if (manyToOne.getEntityType() == sourceType)
	  keys.remove(i);
      }
    }
    */

    if (_sqlColumns != null && _sqlColumns.length == keys.size()) {
      for (int i = 0; i < keys.size(); i++) {
	Column key = keys.get(i);

	String sqlColumn = getColumn(_sqlColumns, key.getName());
	ForeignColumn column =
	  childType.getTable().createForeignColumn(sqlColumn, key);
	
	columns.add(column);
      }
    }
    else if (_sqlColumns != null && _sqlColumns.length == 1) {
      String baseSqlColumn = _sqlColumns[0].getSQLColumn();
      
      for (Column key : keys) {
	String sqlColumn;

	sqlColumn = baseSqlColumn + "_" + key.getName();

	ForeignColumn column =
	  childType.getTable().createForeignColumn(sqlColumn, key);
	columns.add(column);
      }
    }
    else if (_sqlColumns != null && _sqlColumns.length > 0) {
      throw new IllegalStateException("Mismatched SQL columns");
    }
    else if (keys.size() == 1) {
      Column key = keys.get(0);
      
      String sqlColumn = CmpField.toSqlName(fieldName);
	
      ForeignColumn column =
	childType.getTable().createForeignColumn(sqlColumn, key);

      columns.add(column);
    }
    else {
      String baseSqlColumn = CmpField.toSqlName(fieldName);

      for (Column key : keys) {
	String sqlColumn = baseSqlColumn + "_" + key.getName();

	ForeignColumn foreignColumn =
	  childType.getTable().createForeignColumn(sqlColumn, key);

	columns.add(foreignColumn);
      }
    }

    return new LinkColumns(childType.getTable(),
			   parentType.getTable(),
			   columns);
  }

  private String getColumn(SqlRelation []sqlColumns, String fieldName)
    throws ConfigException
  {
    if (sqlColumns.length == 1)
      return sqlColumns[0].getSQLColumn();

    for (int i = 0; i < sqlColumns.length; i++) {
      String ref = sqlColumns[i].getReferences();

      if (ref == null)
	throw new ConfigException(L.l("sql-column '{0}' needs a references attribute.",
				      sqlColumns[i].getSQLColumn()));

      if (ref.equals(fieldName))
	return sqlColumns[i].getSQLColumn();
    }
    
    throw new ConfigException(L.l("key '{0}' has no matching sql-column",
				  fieldName));
  }
}
