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

package com.caucho.amber.cfg;

import com.caucho.amber.AmberTableCache;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.amber.field.SubId;
import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;

import javax.persistence.InheritanceType;
import javax.persistence.AttributeOverrides;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EntityIntrospector.class.getName());

  /**
   * Creates the introspector.
   */
  EntityIntrospector(AmberConfigManager manager)
  {
    super(manager);
  }

  /**
   * Returns true for entity type.
   */
  public boolean isEntity(JClass type)
  {
    getInternalEntityConfig(type, _annotationCfg);
 
    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  public EntityType introspect(JClass type)
    throws ConfigException, SQLException
  {
    EntityType entityType = null;

    try {
      EntityType parentType = introspectParent(type.getSuperClass());

      entityType = introspectEntityType(type);

      MappedSuperclassConfig mappedSuperOrEntityConfig
	= introspectEntityConfig(type);
				 
      entityType.setParentType(parentType);

      introspectInheritance(type, entityType, parentType);
      
      introspectTable(type, entityType, parentType);
    
      introspectTableCache(entityType, type);

      introspectSecondaryTable(entityType, type);

      getInternalIdClassConfig(type, _annotationCfg);
      JAnnotation idClassAnn = _annotationCfg.getAnnotation();
      IdClassConfig idClassConfig = _annotationCfg.getIdClassConfig();

      JClass idClass = null;
      if (! _annotationCfg.isNull()) {
        if (idClassAnn != null)
          idClass = idClassAnn.getClass("value");
        else {
          String s = idClassConfig.getClassName();
          idClass = _persistenceUnit.getJClassLoader().forName(s);
        }

        // XXX: temp. introspects idClass as an embeddable type.
        EmbeddableType embeddable
          = _persistenceUnit.getEmbeddableIntrospector().introspect(idClass);

        // jpa/0i49 vs jpa/0i40
        // embeddable.setFieldAccess(isField);
      }

      if (entityType.getId() != null) {
      }
      else if (entityType.isFieldAccess())
        introspectIdField(_persistenceUnit, entityType, parentType,
                          type, idClass, mappedSuperOrEntityConfig);
      else {
        introspectIdMethod(_persistenceUnit, entityType, parentType,
                           type, idClass, mappedSuperOrEntityConfig);
      }

      HashMap<String, IdConfig> idMap = null;

      AttributesConfig attributes = null;

      if (mappedSuperOrEntityConfig != null) {
        attributes = mappedSuperOrEntityConfig.getAttributes();

        if (attributes != null)
          idMap = attributes.getIdMap();
      }

      // if ((idMap == null) || (idMap.size() == 0)) {
      //   idMap = entityType.getSuperClass();
      // }

      if (entityType.isEntity() && (entityType.getId() == null) && ((idMap == null) || (idMap.size() == 0)))
        throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id or exactly one @EmbeddedId field.",
                                      entityType.getName()));

      // Introspect overridden attributes. (jpa/0ge2)
      introspectAttributeOverrides(entityType, type);

      if (entityType.isFieldAccess())
        introspectFields(_persistenceUnit, entityType, parentType, type,
                         mappedSuperOrEntityConfig, false);
      else
        introspectMethods(_persistenceUnit, entityType, parentType, type,
                          mappedSuperOrEntityConfig);

      introspectCallbacks(type, entityType);
      

      // Adds entity listeners, if any.
      introspectEntityListeners(type, entityType, _persistenceUnit);

      // Adds sql result set mappings, if any.
      introspectSqlResultSetMappings(type, entityType, entityType.getName());

      // Adds named queries, if any.
      introspectNamedQueries(type, entityType.getName());
      introspectNamedNativeQueries(type, entityType.getName());
    } catch (ConfigException e) {
      if (entityType != null)
        entityType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      if (entityType != null)
        entityType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      if (entityType != null)
        entityType.setConfigException(e);

      throw e;
    }

    return entityType;
  }

  private EntityType introspectEntityType(JClass type)
    throws SQLException
  {
    EntityType entityType;
    
    getInternalEntityConfig(type, _annotationCfg);
      
    JAnnotation entityAnn = _annotationCfg.getAnnotation();
    EntityConfig entityConfig = _annotationCfg.getEntityConfig();

    boolean isEntity = ! _annotationCfg.isNull();

    boolean isMappedSuperclass = false;
    JAnnotation mappedSuperAnn = null;
    MappedSuperclassConfig mappedSuperConfig = null;

    String typeName;

    MappedSuperclassConfig mappedSuperOrEntityConfig = null;

    if (isEntity) {
      mappedSuperOrEntityConfig = entityConfig;

      if (entityConfig != null)
	typeName = entityConfig.getClassName();
      else
	typeName = entityAnn.getString("name");
    }
    else {
      getInternalMappedSuperclassConfig(type, _annotationCfg);
      mappedSuperAnn = _annotationCfg.getAnnotation();
      mappedSuperConfig = _annotationCfg.getMappedSuperclassConfig();

      isMappedSuperclass = ! _annotationCfg.isNull();

      if (isMappedSuperclass) {
	mappedSuperOrEntityConfig = mappedSuperConfig;

	if (mappedSuperConfig != null)
	  typeName = mappedSuperConfig.getClassName();
	else
	  typeName = mappedSuperAnn.getString("name");
      }
      else
	throw new ConfigException(L.l("'{0}' is not an @Entity or @MappedSuperclass.",
				      type));
    }

    // Validates the type
    String entityName;
    JAnnotation inheritanceAnn = null;
    InheritanceConfig inheritanceConfig = null;
    JClass rootClass = type;
    JAnnotation rootEntityAnn = null;
    EntityConfig rootEntityConfig = null;

    if (isEntity || isMappedSuperclass) {
      validateType(type, isEntity);

      // jpa/0ge2
      // if (hasInheritance) {

      if (isEntity) {
	if (entityConfig == null)
	  entityName = entityAnn.getString("name");
	else {
	  entityName = entityConfig.getClassName();

	  int p = entityName.lastIndexOf('.');

	  if (p > 0)
	    entityName = entityName.substring(p + 1);
	}
      }
      else { // jpa/0ge2
	if (mappedSuperConfig == null)
	  entityName = mappedSuperAnn.getString("name");
	else {
	  entityName = mappedSuperConfig.getClassName();

	  int p = entityName.lastIndexOf('.');

	  if (p > 0)
	    entityName = entityName.substring(p + 1);
	}
      }
    }
    else {
      entityName = type.getName();
    }

    if ((entityName == null) || "".equals(entityName)) {
      entityName = type.getSimpleName();
    }

    if (isEntity) {
      entityType = _persistenceUnit.createEntity(entityName, type);
    }
    else {
      entityType = _persistenceUnit.createMappedSuperclass(entityName, type);
    }

    _configManager.addType(type, new EntityConfig(type.getName(), this, entityType));

    boolean isField = isField(type, mappedSuperOrEntityConfig, false);

    if (isField)
      entityType.setFieldAccess(true);

    // jpa/0ge2
    entityType.setInstanceClassName(type.getName() + "__ResinExt");
    entityType.setEnhanced(true);

    return entityType;
  }

  private MappedSuperclassConfig introspectEntityConfig(JClass type)
    throws SQLException
  {
    getInternalEntityConfig(type, _annotationCfg);
      
    if (! _annotationCfg.isNull())
      return _annotationCfg.getEntityConfig();
    
    getInternalMappedSuperclassConfig(type, _annotationCfg);

    return _annotationCfg.getMappedSuperclassConfig();
  }

  private EntityType introspectParent(JClass parentClass)
    throws SQLException
  {
    if (parentClass == null)
      return null;

    getInternalEntityConfig(parentClass, _annotationCfg);

    if (! _annotationCfg.isNull())
      return _configManager.introspectEntity(parentClass);
    else if (parentClass.isAnnotationPresent(javax.persistence.Entity.class))
      return _configManager.introspectEntity(parentClass);
    else if (parentClass.isAnnotationPresent(javax.persistence.MappedSuperclass.class))
      return _configManager.introspectEntity(parentClass);
    else
      return null;
  }



  private void introspectInheritance(JClass type,
				     EntityType entityType,
				     EntityType parentType)
    throws SQLException
  {
    // Inheritance annotation/configuration is specified
    // on the entity class that is the root of the entity
    // class hierarachy.
    InheritanceConfig inheritanceConfig;
    JAnnotation inheritanceAnn;

    getInternalInheritanceConfig(type, _annotationCfg);
    inheritanceAnn = _annotationCfg.getAnnotation();
    inheritanceConfig = _annotationCfg.getInheritanceConfig();

    boolean hasInheritance = ! _annotationCfg.isNull();

    if (! hasInheritance &&
	(parentType == null || ! parentType.isEntity()))
      return;

    introspectDiscriminatorValue(type, entityType);
    
    if (parentType != null) {
      if (hasInheritance)
	throw new ConfigException(L.l("'{0}' cannot have @Inheritance. It must be specified on the entity class that is the root of the entity class hierarchy.",
				      type));
      entityType.getRootType().addSubClass(entityType);

      getInternalPrimaryKeyJoinColumnConfig(type, _annotationCfg);
      JAnnotation joinAnn = _annotationCfg.getAnnotation();
      PrimaryKeyJoinColumnConfig primaryKeyJoinColumnConfig
	= _annotationCfg.getPrimaryKeyJoinColumnConfig();

      // if (subType.isJoinedSubClass()) {
      /*
      if (strategy == InheritanceType.JOINED) {
        linkInheritanceTable(entityType.getRootType().getTable(),
                             entityType.getTable(),
                             joinAnn,
                             primaryKeyJoinColumnConfig);

        entityType.setId(new SubId(entityType, entityType.getRootType()));
      }
      */

      return;
    }
    
    if (! hasInheritance)
      return;

    if ((inheritanceAnn != null) || (inheritanceConfig != null))
      introspectInheritance(_persistenceUnit, entityType, type,
			    inheritanceAnn, inheritanceConfig);
  }

  private void introspectDiscriminatorValue(JClass type, EntityType entityType)
  {
    JAnnotation discValueAnn = type.getAnnotation(DiscriminatorValue.class);

    String discriminatorValue = null;

    if (discValueAnn != null)
      discriminatorValue = discValueAnn.getString("value");

    if (discriminatorValue == null || discriminatorValue.equals("")) {
      String name = entityType.getBeanClass().getName();
      int p = name.lastIndexOf('.');
      if (p > 0)
        name = name.substring(p + 1);

      discriminatorValue = name;
    }

    entityType.setDiscriminatorValue(discriminatorValue);
  }

  /**
   * Introspects the Inheritance
   */
  void introspectInheritance(AmberPersistenceUnit persistenceUnit,
                             EntityType entityType,
                             JClass type,
                             JAnnotation inheritanceAnn,
                             InheritanceConfig inheritanceConfig)
    throws ConfigException, SQLException
  {
    InheritanceType strategy;

    if (inheritanceAnn != null)
      strategy = (InheritanceType) inheritanceAnn.get("strategy");
    else
      strategy = inheritanceConfig.getStrategy();

    switch (strategy) {
    case JOINED:
      entityType.setJoinedSubClass(true);
      break;
    }

    getInternalDiscriminatorColumnConfig(type, _annotationCfg);
    JAnnotation discriminatorAnn = _annotationCfg.getAnnotation();
    DiscriminatorColumnConfig discriminatorConfig = _annotationCfg.getDiscriminatorColumnConfig();

    String columnName = null;

    if (discriminatorAnn != null)
      columnName = discriminatorAnn.getString("name");

    if (columnName == null || columnName.equals(""))
      columnName = "DTYPE";

    Type columnType = null;
    DiscriminatorType discType = DiscriminatorType.STRING;

    if (discriminatorAnn != null)
      discType = (DiscriminatorType) discriminatorAnn.get("discriminatorType");

    switch (discType) {
    case STRING:
      columnType = StringType.create();
      break;
    case CHAR:
      columnType = PrimitiveCharType.create();
      break;
    case INTEGER:
      columnType = PrimitiveIntType.create();
      break;
    default:
      throw new IllegalStateException();
    }

    Table table = entityType.getTable();

    // jpa/0gg0
    if (table == null)
      return;

    com.caucho.amber.table.Column column
      = table.createColumn(columnName, columnType);

    if (discriminatorAnn != null) {
      column.setNotNull(! discriminatorAnn.getBoolean("nullable"));

      column.setLength(discriminatorAnn.getInt("length"));

      if (! "".equals(discriminatorAnn.get("columnDefinition")))
        column.setSQLType(discriminatorAnn.getString("columnDefinition"));
    }
    else {
      column.setNotNull(true);
      column.setLength(10);
    }

    entityType.setDiscriminator(column);
  }

  private void introspectTable(JClass type,
                               EntityType entityType,
			       EntityType parentType)
  {
    // XXX: need better test
    boolean isEntity = ! (entityType instanceof MappedSuperclassType);
    
    Table table = null;

    getInternalTableConfig(type, _annotationCfg);
    JAnnotation tableAnn = _annotationCfg.getAnnotation();
    TableConfig tableConfig = _annotationCfg.getTableConfig();

    String tableName = null;

    if (tableAnn != null)
      tableName = (String) tableAnn.get("name");
    else if (tableConfig != null)
      tableName = tableConfig.getName();

    // jpa/0gg0, jpa/0gg2
    if (isEntity) { // && ! type.isAbstract()) {

      boolean hasTableConfig = true;

      if (tableName == null || tableName.equals("")) {
	hasTableConfig = false;
	tableName = toSqlName(entityType.getName());
      }

      /*
      InheritanceType strategy = null;
      
      if (parentType != null)
        strategy = parentType.getInheritanceStrategy();

      if (inheritanceAnn != null)
	strategy = (InheritanceType) inheritanceAnn.get("strategy");
      else if (inheritanceConfig != null)
	strategy = inheritanceConfig.getStrategy();
       */

      // jpa/0gg2
      if (! entityType.isEntity())
        return;
      /*
      if (type.isAbstract()
	  && strategy != InheritanceType.JOINED
	  && ! hasTableConfig) {
	// jpa/0gg0
      }
       */
      else if (parentType == null || parentType.getTable() == null) {
	entityType.setTable(_persistenceUnit.createTable(tableName));
      }
      /*
      else if (strategy == InheritanceType.JOINED) {
	entityType.setTable(_persistenceUnit.createTable(tableName));

	getInternalTableConfig(rootClass, _annotationCfg);
	JAnnotation rootTableAnn = _annotationCfg.getAnnotation();
	TableConfig rootTableConfig = _annotationCfg.getTableConfig();

	String rootTableName = null;

	if (rootTableAnn != null)
	  rootTableName = (String) rootTableAnn.get("name");
	else if (rootTableConfig != null)
	  rootTableName = rootTableConfig.getName();

	if (rootTableName == null || rootTableName.equals("")) {

	  String rootEntityName;

	  if (rootEntityAnn != null)
	    rootEntityName = rootEntityAnn.getString("name");
	  else {
	    rootEntityName = rootEntityConfig.getClassName();

	    int p = rootEntityName.lastIndexOf('.');

	    if (p > 0)
	      rootEntityName = rootEntityName.substring(p + 1);
	  }

	  if (rootEntityName.equals("")) {
	    rootEntityName = rootClass.getName();

	    int p = rootEntityName.lastIndexOf('.');

	    if (p > 0)
	      rootEntityName = rootEntityName.substring(p + 1);
	  }

	  rootTableName = toSqlName(rootEntityName);
	}

	entityType.setRootTableName(rootTableName);
      }
       */
      else
	entityType.setTable(parentType.getTable());
    }
  }

  private void introspectSecondaryTable(EntityType entityType, JClass type)
  {
    getInternalSecondaryTableConfig(type, _annotationCfg);
    JAnnotation secondaryTableAnn = _annotationCfg.getAnnotation();
    SecondaryTableConfig secondaryTableConfig = _annotationCfg.getSecondaryTableConfig();

    Table secondaryTable = null;

    if ((secondaryTableAnn != null) || (secondaryTableConfig != null)) {
      String secondaryName;

      if (secondaryTableAnn != null)
	secondaryName = secondaryTableAnn.getString("name");
      else
	secondaryName = secondaryTableConfig.getName();

      secondaryTable = _persistenceUnit.createTable(secondaryName);

      entityType.addSecondaryTable(secondaryTable);

      // XXX: pk
    }

    if (secondaryTableAnn != null) {
      Object []join = (Object []) secondaryTableAnn.get("pkJoinColumns");

      JAnnotation []joinAnn = null;

      if (join != null) {
	joinAnn = new JAnnotation[join.length];
	System.arraycopy(join, 0, joinAnn, 0, join.length);
      }

      linkSecondaryTable(entityType.getTable(),
			 secondaryTable,
			 joinAnn);
    }
  }

  private void introspectTableCache(EntityType entityType, JClass type)
  {
    JAnnotation tableCache = type.getAnnotation(AmberTableCache.class);

    if (tableCache != null) {
      entityType.getTable().setReadOnly(tableCache.getBoolean("readOnly"));

      long cacheTimeout = Period.toPeriod(tableCache.getString("timeout"));
      entityType.getTable().setCacheTimeout(cacheTimeout);
    }
  }

  private void introspectAttributeOverrides(EntityType entityType,
                                            JClass type)
  {
    EntityType parent = entityType.getParentType();

    if (parent == null)
      return;

    boolean isAbstract = parent.getBeanClass().isAbstract();

    if (parent.isEntity() && ! isAbstract)
      return;

    HashMap<String,ColumnConfig> overrideMap
      = new HashMap<String,ColumnConfig>();
    
    getInternalAttributeOverrideConfig(type, _annotationCfg);
    JAnnotation attributeOverrideAnn = _annotationCfg.getAnnotation();

    boolean hasAttributeOverride = (attributeOverrideAnn != null);

    JAnnotation attributeOverridesAnn
      = type.getAnnotation(AttributeOverrides.class);

    ArrayList<AttributeOverrideConfig> attributeOverrideList = null;

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null)
      attributeOverrideList = entityConfig.getAttributeOverrideList();

    boolean hasAttributeOverrides = false;

    if ((attributeOverrideList != null) &&
	(attributeOverrideList.size() > 0)) {
      hasAttributeOverrides = true;
    }
    else if (attributeOverridesAnn != null)
      hasAttributeOverrides = true;

    if (hasAttributeOverride && hasAttributeOverrides)
      throw new ConfigException(L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
				    type));

    if (attributeOverrideList == null)
      attributeOverrideList = new ArrayList<AttributeOverrideConfig>();

    if (hasAttributeOverride) {
      // Convert annotation to configuration.

      AttributeOverrideConfig attOverrideConfig
	= convertAttributeOverrideAnnotationToConfig(attributeOverrideAnn);

      attributeOverrideList.add(attOverrideConfig);
    }
    else if (hasAttributeOverrides) {
      if (attributeOverrideList.size() > 0) {
	// OK: attributeOverrideList came from orm.xml
      }
      else {
	// Convert annotations to configurations.

	Object attOverridesAnn[]
	  = (Object []) attributeOverridesAnn.get("value");

	AttributeOverrideConfig attOverrideConfig;

	for (int i = 0; i < attOverridesAnn.length; i++) {
	  attOverrideConfig
	    = convertAttributeOverrideAnnotationToConfig((JAnnotation) attOverridesAnn[i]);

	  attributeOverrideList.add(attOverrideConfig);
	}
      }
    }

    for (AttributeOverrideConfig override : attributeOverrideList) {
      overrideMap.put(override.getName(), override.getColumn());
    }

    _depCompletions.add(new AttributeOverrideCompletion(this, entityType, type,
							overrideMap));
  }
}
