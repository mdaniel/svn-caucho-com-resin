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

import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.DependentEntityOneToOneField;
import com.caucho.amber.field.EntityManyToOneField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyManyToOneField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * many-to-one relation
 */
public class CmrManyToOne extends CmrRelation {
  private static final L10N L = new L10N(CmrManyToOne.class);
  private static final Logger log = Log.open(CmrManyToOne.class);

  private final EjbEntityBean _targetBean;

  private SqlRelation []_sqlColumns;

  private EntityManyToOneField _amberManyToOne;
  private DependentEntityOneToOneField _amberDependentOneToOne;

  private boolean _isDependent;
  private boolean _isSourceCascadeDelete;
  private boolean _isTargetCascadeDelete;
  
  /**
   * Creates a new cmp-relation
   */
  public CmrManyToOne(EjbEntityBean entityBean,
		      String fieldName,
		      EjbEntityBean targetBean)
    throws ConfigException
  {
    super(entityBean, fieldName);

    _targetBean = targetBean;

    ApiMethod getter = getGetter();
    
    if (! getter.getReturnType().equals(_targetBean.getLocal()))
      throw new ConfigException(L.l("{0}: '{1}' must return the local interface '{2}' of the EJB bean '{3}'.",
				    entityBean.getEJBClass().getName(),
				    getter.getFullName(),
				    _targetBean.getLocal().getName(),
				    _targetBean.getEJBName()));
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
  public ApiClass getTargetType()
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
   * Set true for a dependent one-to-one
   */
  public void setDependent(boolean isDependent)
  {
    _isDependent = isDependent;
  }

  /**
   * Set true for a cascade-delete many-to-one
   */
  public void setSourceCascadeDelete(boolean isCascadeDelete)
  {
    _isSourceCascadeDelete = isCascadeDelete;
  }

  /**
   * Set true for a cascade-delete many-to-one
   */
  public void setTargetCascadeDelete(boolean isCascadeDelete)
  {
    _isTargetCascadeDelete = isCascadeDelete;
  }

  /**
   * Return true for a cascade-delete many-to-one
   */
  public boolean isCascadeDelete()
  {
    return _isSourceCascadeDelete;
  }

  /**
   * Returns the amber many-to-one.
   */
  public EntityManyToOneField getAmberManyToOne()
  {
      
    if (_amberManyToOne == null) {
      try {
	EntityType sourceType = getEntity().getEntityType();
	
	_amberManyToOne = new EntityManyToOneField(sourceType, getName());

	EntityType targetType = _targetBean.getEntityType();
	_amberManyToOne.setType(targetType);

	_amberManyToOne.setLinkColumns(calculateColumns(sourceType,
							targetType));

	if (! _isDependent) {
	  _amberManyToOne.setSourceCascadeDelete(_isSourceCascadeDelete);
	  _amberManyToOne.setTargetCascadeDelete(_isTargetCascadeDelete);
	}

	_amberManyToOne.init();
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
      
    return _amberManyToOne;
  }

  /**
   * Returns the amber one-to-one.
   */
  public DependentEntityOneToOneField getAmberOneToOne()
  {
    return _amberDependentOneToOne;
  }

  /**
   * Amber creating the id field.
   */
  public IdField createId(AmberPersistenceUnit amberPersistenceUnit, EntityType type)
    throws ConfigException
  {
    String fieldName = getName();

    EntityType sourceType = getEntity().getEntityType();
    EntityType targetType = getTargetBean().getEntityType();

    /*
    columns.add(new ForeignColumn(sqlName, targetType.getId().getKeys().get(0)));
    */

    KeyManyToOneField keyField = new KeyManyToOneField(type, fieldName);
    keyField.setType(targetType);
    
    keyField.setLinkColumns(calculateColumns(sourceType, targetType));

    keyField.init();
      
    return keyField;
  }

  /**
   * Creates the amber type.
   */
  public AmberField assembleAmber(EntityType type)
    throws ConfigException
  {
    AmberPersistenceUnit manager = type.getPersistenceUnit();
    EntityType targetType = getTargetBean().getEntityType();

    if (_isDependent) {
      DependentEntityOneToOneField oneToOne;
      oneToOne = new DependentEntityOneToOneField(type, getName());

      _amberDependentOneToOne = oneToOne;
      
      // oneToOne.setType(_targetBean.getEntityType());
      //oneToOne.setCascadeDelete(_isCascadeDelete);

      return oneToOne;
    }
    else {
      EntityManyToOneField manyToOne;
      manyToOne = new EntityManyToOneField(type, getName());
      
      _amberManyToOne = manyToOne;

      manyToOne.setType(_targetBean.getEntityType());

      manyToOne.setLinkColumns(calculateColumns(type, targetType));

      manyToOne.setSourceCascadeDelete(_isSourceCascadeDelete);
      manyToOne.setTargetCascadeDelete(_isTargetCascadeDelete);

      return manyToOne;
    }
  }

  private LinkColumns calculateColumns(EntityType type,
				       EntityType targetType)
    throws ConfigException
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    Id id = targetType.getId();
    
    if (id == null || id.getColumns() == null)
      throw new IllegalStateException(L.l("Entity '{0}' has invalid id columns.",
					  targetType.getName()));
    
    ArrayList<Column> keys = id.getColumns();

    
    if (_sqlColumns != null && _sqlColumns.length == keys.size()) {
      for (Column key : keys) {
	String sqlColumn = getColumn(_sqlColumns, key.getName());
	ForeignColumn column =
	  type.getTable().createForeignColumn(sqlColumn, key);
	
	columns.add(column);
      }
    }
    else if (_sqlColumns != null && _sqlColumns.length == 1) {
      String baseSqlColumn = _sqlColumns[0].getSQLColumn();
      
      for (Column key : keys) {
	String sqlColumn = baseSqlColumn + "_" + key.getName();

	ForeignColumn column =
	  type.getTable().createForeignColumn(sqlColumn, key);
	columns.add(column);
      }
    }
    else if (_sqlColumns != null && _sqlColumns.length > 0) {
      throw new IllegalStateException("Mismatched SQL columns");
    }
    else if (keys.size() == 1) {
      String sqlColumn = toSqlName(getName());
      
      ForeignColumn column =
	type.getTable().createForeignColumn(sqlColumn, keys.get(0));
      columns.add(column);
    }
    else {
      String baseSqlColumn = toSqlName(getName());
      for (Column key : keys) {
	String sqlColumn = baseSqlColumn + "_" + key.getName();
	  
	ForeignColumn column =
	  type.getTable().createForeignColumn(sqlColumn, key);
	columns.add(column);
      }
    }

    return new LinkColumns(type.getTable(), targetType.getTable(), columns);
  }

  private String getColumn(SqlRelation []sqlColumns, String sqlName)
    throws ConfigException
  {
    String fieldName = getFieldName(_targetBean.getEntityType(), sqlName);
    
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

  private String getFieldName(EntityType type, String sqlName)
    throws ConfigException
  {
    for (IdField field : type.getId().getKeys()) {
      ArrayList<Column> columns = field.getColumns();

      for (int i = 0; i < columns.size(); i++)
	if (columns.get(i).getName().equals(sqlName))
	  return field.getName();
    }

    return sqlName;
  }
  
  /**
   * Link amber.
   */
  public void linkAmber()
    throws ConfigException
  {
    CmrRelation targetRelation = getTargetRelation();

    if (targetRelation == null)
      return;
    else if (! (targetRelation instanceof CmrManyToOne))
      return;
    else if (_isDependent) {
      CmrManyToOne targetManyToOne = (CmrManyToOne) targetRelation;

      EntityManyToOneField amberTarget = targetManyToOne.getAmberManyToOne();

      if (_amberDependentOneToOne == null) {
	EntityType type = getEntity().getEntityType();
	
	DependentEntityOneToOneField oneToOne;
	oneToOne = new DependentEntityOneToOneField(type, getName());

	_amberDependentOneToOne = oneToOne;
      }
      
      _amberDependentOneToOne.setTargetField(amberTarget);
    }
    else {
      CmrManyToOne targetManyToOne = (CmrManyToOne) targetRelation;

      DependentEntityOneToOneField amberTarget;
      amberTarget = targetManyToOne.getAmberOneToOne();

      EntityManyToOneField manyToOne = getAmberManyToOne();

      if (amberTarget != null && manyToOne != null) {
	manyToOne.setTargetField(amberTarget);
	amberTarget.setTargetField(manyToOne);
      }
    }
  }
}
