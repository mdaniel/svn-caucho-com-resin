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


import com.caucho.amber.field.*;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;

import com.caucho.util.L10N;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Completion for overrides based on a parent map
 */
public class AttributeOverrideCompletion extends Completion {
  private static final L10N L = new L10N(AttributeOverrideCompletion.class);
  private JClass _type;
  private HashMap<String,ColumnConfig> _overrideMap;

  AttributeOverrideCompletion(BaseConfigIntrospector base,
                              EntityType entityType,
			      JClass type,
			      HashMap<String,ColumnConfig> overrideMap)
  {
    super(base, entityType);

    _type = type;
    _overrideMap = overrideMap;
  }

  @Override
  void complete()
    throws ConfigException
  {

    // jpa/0ge8, jpa/0ge9, jpa/0gea
    // Fields which have not been overridden are added to the
    // entity subclass. This makes the columns to be properly
    // created at each entity table -- not the mapped superclass
    // table, even because the parent might not have a valid table.

    EntityType parent = _entityType.getParentType();

    ArrayList<AmberField> fields = parent.getFields();

    for (AmberField field : fields) {
      String fieldName = field.getName();

      ColumnConfig column = _overrideMap.get(fieldName);

      Column oldColumn = field.getColumn();
      // XXX: deal with types
      AbstractField newField = (AbstractField) field.override(_entityType);

      if (column == null) {
	Table table = _entityType.getTable();
	Column newColumn = table.createColumn(oldColumn.getName(),
					      oldColumn.getType());

	newField.setColumn(newColumn);

	newField.init();
	
	_entityType.addField(newField);
	continue;
      }

      if (true) {
	_entityType.addField(newField);
	continue;
      }

      /*

      if (field instanceof PropertyField) {
	Column column = ((PropertyField) field).getColumn();

	// jpa/0ge8, jpa/0gea
	// Creates the missing attribute override config.
	attOverrideConfig = _base.createAttributeOverrideConfig(fieldName,
							  column.getName(),
							  ! column.isNotNull(),
							  column.isUnique());

	attributeOverrideList.add(attOverrideConfig);
      }

      */
    }


    /*
    // jpa/0ge8, jpa/0ge9
    // Similar code to create any missing configuration for keys.

    com.caucho.amber.field.Id parentId = parent.getId();

    // jpa/0ge6
    if (parentId != null) {
      ArrayList<IdField> keys = parentId.getKeys();

      for (IdField field : keys) {
	String fieldName = field.getName();

	AttributeOverrideConfig attOverrideConfig = null;

	int i = 0;

	for (; i < attributeOverrideList.size(); i++) {
	  attOverrideConfig = attributeOverrideList.get(i);

	  if (fieldName.equals(attOverrideConfig.getName())) {
	    break;
	  }
	}

	if (i < attributeOverrideList.size())
	  continue;

	if (field instanceof KeyPropertyField) {
	  try {
	    if (_entityType.isFieldAccess())
	      _base.introspectIdField(_base._persistenceUnit, _entityType, null,
				parent.getBeanClass(), null, null);
	    else
	      _base.introspectIdMethod(_base._persistenceUnit, _entityType, null,
				 parent.getBeanClass(), null, null);
	  } catch (SQLException e) {
	    throw ConfigException.create(e);
	  }

	  field = _entityType.getId().getKeys().get(0);

	  Column column = ((KeyPropertyField) field).getColumn();

	  // jpa/0ge8, jpa/0ge9, jpa/0gea
	  // Creates the missing attribute override config.
	  attOverrideConfig = _base.createAttributeOverrideConfig(fieldName,
							    column.getName(),
							    ! column.isNotNull(),
							    column.isUnique());

	  attributeOverrideList.add(attOverrideConfig);
	}
      }
    }

    // Overrides fields from MappedSuperclass.

    Table sourceTable = _entityType.getTable();

    for (int i = 0; i < attributeOverrideList.size(); i++) {

      AttributeOverrideConfig attOverrideConfig
	= attributeOverrideList.get(i);

      String entityFieldName;
      String columnName;
      boolean notNull = false;
      boolean unique = false;

      Type amberType = null;

      for (int j = 0; j < fields.size(); j++) {

	AmberField field = fields.get(j);

	if (! (field instanceof PropertyField)) {
	  // jpa/0ge3: relationship fields are fully mapped in the
	  // mapped superclass, i.e., are not included in @AttributeOverrides
	  // and can be added to the entity right away.

	  // Adds only once.
	  if (i == 0) {
	    _entityType.addMappedSuperclassField(field);
	  }

	  continue;
	}

	entityFieldName = field.getName();

	columnName = _base.toSqlName(entityFieldName);

	if (entityFieldName.equals(attOverrideConfig.getName())) {

	  ColumnConfig columnConfig = attOverrideConfig.getColumn();

	  if (columnConfig != null) {
	    columnName = columnConfig.getName();
	    notNull = ! columnConfig.getNullable();
	    unique = columnConfig.getUnique();
	    amberType = _base._persistenceUnit.createType(field.getJavaType().getName());

	    Column column = sourceTable.createColumn(columnName, amberType);

	    column.setNotNull(notNull);
	    column.setUnique(unique);

	    PropertyField overriddenField
	      = new PropertyField(field.getSourceType(), field.getName());

	    overriddenField.setType(((PropertyField) field).getType());
	    overriddenField.setLazy(field.isLazy());
	    overriddenField.setColumn(column);

	    _entityType.addMappedSuperclassField(overriddenField);
	  }
	}
      }

      if (_entityType.getId() != null) {
	ArrayList<IdField> keys = _entityType.getId().getKeys();

	for (int j = 0; j < keys.size(); j++) {

	  IdField field = keys.get(j);

	  entityFieldName = field.getName();

	  columnName = _base.toSqlName(entityFieldName);

	  if (entityFieldName.equals(attOverrideConfig.getName())) {

	    ColumnConfig columnConfig = attOverrideConfig.getColumn();

	    if (columnConfig != null) {
	      columnName = columnConfig.getName();
	      notNull = ! columnConfig.getNullable();
	      unique = columnConfig.getUnique();
	      amberType = _base._persistenceUnit.createType(field.getJavaType().getName());

	      Column column = sourceTable.createColumn(columnName, amberType);

	      column.setNotNull(notNull);
	      column.setUnique(unique);

	      if (field instanceof KeyPropertyField) {
		KeyPropertyField overriddenField
		  = new KeyPropertyField((EntityType) field.getSourceType(),
					 field.getName());

		overriddenField.setGenerator(field.getGenerator());
		overriddenField.setColumn(column);

		// XXX: needs to handle compound pk with @AttributeOverride ???
		if (keys.size() == 1) {
		  keys.remove(0);
		  keys.add(overriddenField);
		  _entityType.setId(new com.caucho.amber.field.Id(_entityType, keys));
		}
	      }
	    }
	  }
	}
      }
      }
    */
  }
}
