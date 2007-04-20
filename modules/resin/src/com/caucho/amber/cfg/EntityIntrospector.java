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

package com.caucho.amber.cfg;

import com.caucho.amber.AmberTableCache;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyPropertyField;
import com.caucho.amber.field.PropertyField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;

import javax.persistence.AttributeOverrides;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
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

  // EntityType or MappedSuperclassType.
  HashMap<String, RelatedType> _relatedTypeMap
    = new HashMap<String, RelatedType>();

  /**
   * Creates the introspector.
   */
  public EntityIntrospector(AmberPersistenceUnit persistenceUnit)
  {
    super(persistenceUnit);
  }

  /**
   * Returns true for entity type.
   */
  public boolean isEntity(JClass type)
  {
    getInternalEntityConfig(type);
    JAnnotation entityAnn = _annotationCfg.getAnnotation();
    EntityConfig entityConfig = _annotationCfg.getEntityConfig();

    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  public RelatedType introspect(JClass type)
    throws ConfigException, SQLException
  {
    RelatedType entityType = null;

    try {
      getInternalEntityConfig(type);
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
        getInternalMappedSuperclassConfig(type);
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

      // Adds named queries, if any.
      introspectNamedQueries(type, typeName);
      introspectNamedNativeQueries(type, typeName);

      // Validates the type
      String entityName;
      RelatedType parentType = null;
      JAnnotation inheritanceAnn = null;
      InheritanceConfig inheritanceConfig = null;
      JClass rootClass = type;
      JAnnotation rootEntityAnn = null;
      EntityConfig rootEntityConfig = null;

      if (isEntity || isMappedSuperclass) {
        validateType(type, isEntity);

        // Inheritance annotation/configuration is specified
        // on the entity class that is the root of the entity
        // class hierarachy.

        getInternalInheritanceConfig(type);
        inheritanceAnn = _annotationCfg.getAnnotation();
        inheritanceConfig = _annotationCfg.getInheritanceConfig();

        boolean hasInheritance = ! _annotationCfg.isNull();

        for (JClass parentClass = type.getSuperClass();
             parentClass != null;
             parentClass = parentClass.getSuperClass()) {

          getInternalEntityConfig(parentClass);

          if (_annotationCfg.isNull())
            break;

          rootEntityAnn = _annotationCfg.getAnnotation();
          rootEntityConfig = _annotationCfg.getEntityConfig();

          rootClass = parentClass;

          if (hasInheritance)
            throw new ConfigException(L.l("'{0}' cannot have @Inheritance. It must be specified on the entity class that is the root of the entity class hierarchy.",
                                          type));

          getInternalInheritanceConfig(rootClass);
          inheritanceAnn = _annotationCfg.getAnnotation();
          inheritanceConfig = _annotationCfg.getInheritanceConfig();

          hasInheritance = ! _annotationCfg.isNull();
        }

        // jpa/0ge2
        // if (hasInheritance) {

        for (JClass parentClass = type.getSuperClass();
             parentClass != null;
             parentClass = parentClass.getSuperClass()) {

          getInternalEntityConfig(parentClass);

          if (! _annotationCfg.isNull()) {
            parentType = introspect(parentClass);

            if (parentClass.isAbstract() && ! type.isAbstract()) {
              // jpa/0gg0, jpa/0gg1
              // XXX entityType.addAbstractEntityParentFields();
            }

            break;
          }

          // jpa/0ge2
          getInternalMappedSuperclassConfig(parentClass);

          if (! _annotationCfg.isNull()) {
            parentType = introspect(parentClass);
            break;
          }
        }

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
        entityName = type.getName();
        int p = entityName.lastIndexOf('.');
        if (p > 0)
          entityName = entityName.substring(p + 1);
      }

      entityType = _relatedTypeMap.get(entityName);

      if (entityType != null)
        return entityType;

      if (isEntity) {
        entityType = _persistenceUnit.createEntity(entityName, type);
      }
      else {
        entityType = _persistenceUnit.createMappedSuperclass(entityName, type);
      }

      _relatedTypeMap.put(entityName, entityType);

      // Adds entity listeners, if any.
      introspectEntityListeners(type, entityType, _persistenceUnit);

      // Adds sql result set mappings, if any.
      introspectSqlResultSetMappings(type, entityType, typeName);

      boolean isField = isField(type, mappedSuperOrEntityConfig, false);

      if (isField)
        entityType.setFieldAccess(true);

      // jpa/0ge2
      entityType.setInstanceClassName(type.getName() + "__ResinExt");
      entityType.setEnhanced(true);

      Table table = null;

      getInternalTableConfig(type);
      JAnnotation tableAnn = _annotationCfg.getAnnotation();
      TableConfig tableConfig = _annotationCfg.getTableConfig();

      String tableName = null;

      if (tableAnn != null)
        tableName = (String) tableAnn.get("name");
      else if (tableConfig != null)
        tableName = tableConfig.getName();

      if (tableName == null || tableName.equals(""))
        tableName = toSqlName(entityName);

      // jpa/0gg0
      if (isEntity && ! type.isAbstract()) {

        InheritanceType strategy = null;

        if (inheritanceAnn != null)
          strategy = (InheritanceType) inheritanceAnn.get("strategy");
        else if (inheritanceConfig != null)
          strategy = inheritanceConfig.getStrategy();

        if ((parentType == null) ||
            (parentType instanceof MappedSuperclassType)) {

          entityType.setTable(_persistenceUnit.createTable(tableName));
        }
        else if (strategy == InheritanceType.JOINED) {
          entityType.setTable(_persistenceUnit.createTable(tableName));

          getInternalTableConfig(rootClass);
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
        else
          entityType.setTable(parentType.getTable());
      }

      JAnnotation tableCache = type.getAnnotation(AmberTableCache.class);

      if (tableCache != null) {
        entityType.getTable().setReadOnly(tableCache.getBoolean("readOnly"));

        long cacheTimeout = Period.toPeriod(tableCache.getString("timeout"));
        entityType.getTable().setCacheTimeout(cacheTimeout);
      }

      getInternalSecondaryTableConfig(type);
      JAnnotation secondaryTableAnn = _annotationCfg.getAnnotation();
      SecondaryTableConfig secondaryTableConfig = _annotationCfg.getSecondaryTableConfig();

      Table secondaryTable = null;

      if ((inheritanceAnn != null) || (inheritanceConfig != null))
        introspectInheritance(_persistenceUnit, entityType, type,
                              inheritanceAnn, inheritanceConfig);

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

      getInternalIdClassConfig(type);
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

        // jpa/0i49
        embeddable.setFieldAccess(isField);
      }

      if (entityType.getId() != null) {
      }
      else if (isField)
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

      if (isEntity && (entityType.getId() == null) && ((idMap == null) || (idMap.size() == 0)))
        throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id or exactly one @EmbeddedId field.",
                                      entityType.getName()));

      // Introspect overridden attributes. (jpa/0ge2)
      introspectAttributeOverrides(entityType, type);

      if (isField)
        introspectFields(_persistenceUnit, entityType, parentType, type,
                         mappedSuperOrEntityConfig, false);
      else
        introspectMethods(_persistenceUnit, entityType, parentType, type,
                          mappedSuperOrEntityConfig);

      if (isEntity) {
        introspectCallbacks(type, entityType);

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

  private void introspectAttributeOverrides(RelatedType relatedType,
                                            JClass type)
  {
    RelatedType parent = relatedType.getParentType();

    if (parent == null)
      return;

    boolean isAbstract = parent.getBeanClass().isAbstract();

    if (parent instanceof EntityType && ! isAbstract)
      return;

    _depCompletions.add(new AttributeOverrideCompletion(relatedType, type));
  }
}
