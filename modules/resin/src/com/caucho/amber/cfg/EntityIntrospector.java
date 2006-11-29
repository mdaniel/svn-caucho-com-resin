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
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;

import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EntityIntrospector.class.getName());

  HashMap<String, EntityType> _entityMap
    = new HashMap<String, EntityType>();

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
  public EntityType introspect(JClass type)
    throws ConfigException, SQLException
  {
    getInternalEntityConfig(type);
    JAnnotation entityAnn = _annotationCfg.getAnnotation();
    EntityConfig entityConfig = _annotationCfg.getEntityConfig();

    JAnnotation mappedSuperclassAnn = type.getAnnotation(MappedSuperclass.class);

    boolean isEntity = (entityAnn != null) || (entityConfig != null);
    boolean isMappedSuperclass = mappedSuperclassAnn != null;
    if (! (isEntity || isMappedSuperclass)) {
      throw new ConfigException(L.l("'{0}' is not an @Entity or @MappedSuperclass.",
                                    type));
    }

    String typeName = isEntity ? entityAnn.getString("name") :
      mappedSuperclassAnn.getString("name");

    // Adds named queries, if any.
    introspectNamedQueries(type, typeName);

    // Validates the type
    String entityName;
    EntityType parentType = null;
    JAnnotation inheritanceAnn = null;
    InheritanceConfig inheritanceConfig = null;
    JClass rootClass = type;
    JAnnotation rootEntityAnn = null;
    EntityConfig rootEntityConfig = null;

    if (isEntity) {
      validateType(type);

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

      if (hasInheritance) {

        for (JClass parentClass = type.getSuperClass();
             parentClass != null;
             parentClass = parentClass.getSuperClass()) {

          getInternalEntityConfig(parentClass);
          JAnnotation parentEntity = _annotationCfg.getAnnotation();
          EntityConfig superEntityConfig = _annotationCfg.getEntityConfig();

          if (! _annotationCfg.isNull()) {
            parentType = introspect(parentClass);
            break;
          }

          // getInternalMappedSuperclassConfig(parentClass);
          // JAnnotation superclassAnn = _annotationCfg.getAnnotation();
          // EntityConfig superclassConfig = _annotationCfg.getMappedSuperclassConfig();
          //
          // if (! _annotationCfg.isNull()) {
          //   parentType = introspect(parentClass);
          //   break;
          // }
        }
      }

      if (entityAnn != null)
        entityName = entityAnn.getString("name");
      else {
        entityName = entityConfig.getClassName();

        int p = entityName.lastIndexOf('.');

        if (p > 0)
          entityName = entityName.substring(p + 1);
      }
    }
    else {
      entityName = type.getName();
    }

    if (entityName.equals("")) {
      entityName = type.getName();
      int p = entityName.lastIndexOf('.');
      if (p > 0)
        entityName = entityName.substring(p + 1);
    }

    EntityType entityType = _entityMap.get(entityName);

    if (entityType != null)
      return entityType;

    try {
      entityType = _persistenceUnit.createEntity(entityName, type);
      _entityMap.put(entityName, entityType);

      // Adds entity listeners, if any.
      introspectEntityListeners(type, entityType, _persistenceUnit);

      boolean isField = isField(type, entityConfig, false);

      if (isField)
        entityType.setFieldAccess(true);

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
        tableName = entityName.toUpperCase();

      if (isEntity) {

        InheritanceType strategy = null;

        if (inheritanceAnn != null)
          strategy = (InheritanceType) inheritanceAnn.get("strategy");
        else if (inheritanceConfig != null)
          strategy = inheritanceConfig.getStrategy();

        if (parentType == null)
          entityType.setTable(_persistenceUnit.createTable(tableName));
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

            rootTableName = rootEntityName.toUpperCase();
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
      }

      if (entityType.getId() != null) {
      }
      else if (isField)
        introspectIdField(_persistenceUnit, entityType, parentType,
                          type, idClass, entityConfig);
      else
        introspectIdMethod(_persistenceUnit, entityType, parentType,
                           type, idClass, entityConfig);

      HashMap<String, IdConfig> idMap = null;

      AttributesConfig attributes = null;

      if (entityConfig != null) {
        attributes = entityConfig.getAttributes();

        if (attributes != null)
          idMap = attributes.getIdMap();
      }

      // if ((idMap == null) || (idMap.size() == 0)) {
      //   idMap = entityType.getSuperClass();
      // }

      if (isEntity && (entityType.getId() == null) && ((idMap == null) || (idMap.size() == 0)))
        throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id or exactly one @EmbeddedId field.",
                                      entityType.getName()));

      if (isField)
        introspectFields(_persistenceUnit, entityType, parentType, type, entityConfig, false);
      else
        introspectMethods(_persistenceUnit, entityType, parentType, type, entityConfig);

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
      entityType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      entityType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      entityType.setConfigException(e);

      throw e;
    }

    return entityType;
  }
}
