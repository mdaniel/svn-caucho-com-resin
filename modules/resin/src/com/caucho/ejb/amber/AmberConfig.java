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

package com.caucho.ejb.amber;

import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;

import javax.sql.DataSource;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.JavaClassGenerator;

import com.caucho.make.PersistentDependency;

import com.caucho.amber.field.Id;
import com.caucho.amber.field.CompositeId;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.PropertyField;
import com.caucho.amber.field.StubMethod;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;

import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.Type;

import com.caucho.ejb.cfg.EjbConfig;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.ejb.cfg.CmpField;
import com.caucho.ejb.cfg.CmpProperty;
import com.caucho.ejb.cfg.CmrRelation;
import com.caucho.ejb.cfg.CmrManyToOne;
import com.caucho.ejb.cfg.CmrOneToMany;
import com.caucho.ejb.cfg.CmrManyToMany;

/**
 * Configuration manager for amber.
 */
public class AmberConfig {
  private static final L10N L = new L10N(AmberConfig.class);

  private EjbConfig _ejbConfig;
  private AmberPersistenceUnit _manager;

  private ArrayList<EjbEntityBean> _beans = new ArrayList<EjbEntityBean>();
  
  private HashMap<String,EntityType> _entityMap =
    new HashMap<String,EntityType>();

  /**
   * Sets the data source.
   */
  public AmberConfig(EjbConfig ejbConfig)
  {
    _ejbConfig = ejbConfig;
    _manager = _ejbConfig.getEJBManager().getAmberManager();
  }

  public void init()
    throws ConfigException, IOException
  {
  }

  /**
   * Returns the manager.
   */
  public AmberPersistenceUnit getManager()
  {
    return _manager;
  }
  
  /**
   * Adds a new bean config.
   */
  public void addBean(EjbEntityBean bean)
    throws ConfigException
  {
    _beans.add(bean);

    EntityType type = bean.getEntityType();

    type.setInstanceClassName(bean.getFullImplName());
    type.setProxyClass(bean.getLocal());

    String sqlTable;

    if (bean.getAbstractSchemaName() != null) {
      type.setName(bean.getAbstractSchemaName());
      sqlTable = bean.getAbstractSchemaName();
    }
    else {
      String name = bean.getEJBName();
      int p = name.lastIndexOf('/');
      if (p > 0)
	sqlTable = name.substring(p + 1);
      else
	sqlTable = name;
    }

    if (bean.getSQLTable() != null)
      sqlTable = bean.getSQLTable();

    Table table = _manager.createTable(sqlTable);
    table.setConfigLocation(bean.getLocation());
			    
    type.setTable(table);

    type.setReadOnly(bean.isReadOnly());
    table.setReadOnly(bean.isReadOnly());
    type.setCacheTimeout(bean.getCacheTimeout());

    if (hasMethod(bean.getEJBClassWrapper(), "ejbLoad", new JClass[0])) {
      type.setHasLoadCallback(true);
    }
    
    _entityMap.put(bean.getEJBName(), type);

    ArrayList<CmpProperty> ids = new ArrayList<CmpProperty>();

    for (CmpField cmpField : bean.getCmpFields()) {
      if (cmpField.isId())
	ids.add(cmpField);
      else
	configureField(type, cmpField);
    }

    for (CmrRelation cmrRelation : bean.getRelations()) {
      if (cmrRelation.isId())
	ids.add(cmrRelation);
    }

    configureId(bean, type, ids);

    for (JMethod method : bean.getStubMethods()) {
      type.addStubMethod(new StubMethod(method));
    }

    for (PersistentDependency depend : bean.getDependList()) {
      type.addDependency(depend);
    }
  }

  /**
   * Configure the field.
   */
  private void configureField(EntityType type, CmpField cmpField)
    throws ConfigException
  {
    String fieldName = cmpField.getName();
    String sqlName = cmpField.getSQLColumn();

    if (sqlName == null)
      sqlName = fieldName;
      
    JClass dataType = cmpField.getJavaType();

    if (dataType == null)
      throw new NullPointerException(L.l("'{0}' is an unknown field",
					 fieldName));

    Type amberType = _manager.createType(dataType);
    Column column = type.getTable().createColumn(sqlName, amberType);

    column.setConfigLocation(cmpField.getLocation());
      
    PropertyField field = new PropertyField(type, fieldName);
    field.setColumn(column);

    type.addField(field);
  }

  /**
   * Configure the field.
   */
  private void configureId(EjbEntityBean bean,
			   EntityType type,
			   ArrayList<CmpProperty> fields)
    throws ConfigException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();
    
    for (CmpProperty cmpProperty : fields) {
      IdField idField = cmpProperty.createId(_manager, type);
      if (fields.size() > 1 || bean.getCompositeKeyClass() != null)
	idField.setKeyField(true);

      keys.add(idField);
    }

    if (keys.size() == 1 && bean.getCompositeKeyClass() == null) {
      Id id = new Id(type, keys.get(0));
      type.setId(id);
    }
    else {
      CompositeId id = new CompositeId(type, keys);
      id.setKeyClass(bean.getPrimKeyClass());
      type.setId(id);
    }
  }
  
  public void configureRelations()
    throws ConfigException
  {
    for (EjbEntityBean bean : _beans) {
      configureRelations(bean);
    }
    
    for (EjbEntityBean bean : _beans) {
      linkRelations(bean);
    }
  }
  
  private void configureRelations(EjbEntityBean bean)
    throws ConfigException
  {
    EntityType type = bean.getEntityType();

    for (CmrRelation rel : bean.getRelations()) {
      if (! rel.isId())
	configureRelation(type, rel);
    }
  }
  
  private void linkRelations(EjbEntityBean bean)
    throws ConfigException
  {
    EntityType type = _entityMap.get(bean.getEJBName());
    
    for (CmrRelation rel : bean.getRelations()) {
      rel.linkAmber();
    }
  }

  /**
   * Configure the relation rolen.
   */
  private void configureRelation(EntityType type, CmrRelation rel)
    throws ConfigException
  {
    String fieldName = rel.getName();
    String targetName = rel.getTargetBean().getEJBName();

    EntityType targetType = _entityMap.get(targetName);

    if (targetType == null)
      throw new ConfigException(L.l("'{0}' is an unknown entity type",
				    targetName));

    AmberField field = rel.assembleAmber(type);

    if (field != null)
      type.addField(field);
  }

  /**
   * Generates the beans.
   */
  public void generate(JavaClassGenerator javaGen)
    throws Exception
  {
    _manager.generate(javaGen);
  }

  private static boolean hasMethod(JClass cl, String name, JClass []param)
  {
    try {
      return cl.getMethod(name, param) != null;
    } catch (Throwable e) {
      return false;
    }
  }
}
