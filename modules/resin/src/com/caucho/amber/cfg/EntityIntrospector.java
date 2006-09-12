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

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.persistence.*;

import com.caucho.amber.AmberTableCache;

import com.caucho.amber.field.*;

import com.caucho.amber.idgen.IdGenerator;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;

import com.caucho.amber.type.*;

import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.ejb.EjbServerManager;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EntityIntrospector.class.getName());

  private static HashSet<String> _propertyAnnotations
    = new HashSet<String>();

  // types allowed with a @Basic annotation
  private static HashSet<String> _basicTypes = new HashSet<String>();

  // annotations allowed with a @Basic annotation
  private static HashSet<String> _basicAnnotations = new HashSet<String>();

  // types allowed with an @Id annotation
  private static HashSet<String> _idTypes = new HashSet<String>();

  // annotations allowed with an @Id annotation
  private static HashSet<String> _idAnnotations = new HashSet<String>();

  // annotations allowed with a @ManyToOne annotation
  private static HashSet<String> _manyToOneAnnotations = new HashSet<String>();

  // annotations allowed with a @OneToMany annotation
  private static HashSet<String> _oneToManyAnnotations = new HashSet<String>();

  // types allowed with a @OneToMany annotation
  private static HashSet<String> _oneToManyTypes = new HashSet<String>();

  // annotations allowed with a @ManyToMany annotation
  private static HashSet<String> _manyToManyAnnotations = new HashSet<String>();

  // types allowed with a @ManyToMany annotation
  private static HashSet<String> _manyToManyTypes = new HashSet<String>();

  // annotations allowed with a @OneToOne annotation
  private static HashSet<String> _oneToOneAnnotations = new HashSet<String>();

  // annotations allowed with a @Embedded annotation
  private static HashSet<String> _embeddedAnnotations = new HashSet<String>();

  // annotations allowed with a @EmbeddedId annotation
  private static HashSet<String> _embeddedIdAnnotations = new HashSet<String>();

  // annotations allowed with a @Version annotation
  private static HashSet<String> _versionAnnotations = new HashSet<String>();

  // types allowed with an @Version annotation
  private static HashSet<String> _versionTypes = new HashSet<String>();

  private AmberPersistenceUnit _persistenceUnit;

  private HashMap<String,EntityType> _entityMap
    = new HashMap<String,EntityType>();

  private HashMap<String,EntityConfig> _entityConfigMap
    = new HashMap<String,EntityConfig>();

  private ArrayList<Completion> _linkCompletions = new ArrayList<Completion>();
  private ArrayList<Completion> _depCompletions = new ArrayList<Completion>();

  /**
   * Creates the introspector.
   */
  public EntityIntrospector(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
  }

  /**
   * Sets the entity config map.
   */
  public void setEntityConfigMap(HashMap<String, EntityConfig> entityConfigMap)
  {
    _entityConfigMap = entityConfigMap;
  }

  /**
   * Introspects.
   */
  public EntityType introspect(JClass type)
    throws ConfigException, SQLException
  {
    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    JAnnotation entityAnn = type.getAnnotation(Entity.class);
    JAnnotation embeddableAnn = type.getAnnotation(Embeddable.class);
    JAnnotation mappedSuperclassAnn = type.getAnnotation(MappedSuperclass.class);

    boolean isEntity = (entityAnn != null) || (entityConfig != null);
    boolean isEmbeddable = embeddableAnn != null;
    boolean isMappedSuperclass = mappedSuperclassAnn != null;
    if (! (isEntity || isEmbeddable || isMappedSuperclass)) {
      throw new ConfigException(L.l("'{0}' is not an @Entity, @Embeddable or @MappedSuperclass.",
                                    type));
    }

    // Adds named queries, if any.

    JAnnotation namedQueryAnn = null;
    JAnnotation namedQueriesAnn = null;

    namedQueryAnn = type.getAnnotation(NamedQuery.class);
    namedQueriesAnn = type.getAnnotation(NamedQueries.class);

    if (! ((namedQueryAnn == null) && (namedQueriesAnn == null))) {

      if (isEntity || isMappedSuperclass) {

        Object namedQueryArray[];

        if ((namedQueryAnn != null) && (namedQueriesAnn != null)) {
          throw new ConfigException(L.l("{0} may not have both @NamedQuery and @NamedQueries",
                                        isEntity ? entityAnn.getString("name") :
                                        mappedSuperclassAnn.getString("name")));
        }
        else if (namedQueriesAnn != null) {
          namedQueryArray = (Object []) namedQueriesAnn.get("value");
        }
        else {
          namedQueryArray = new Object[] { namedQueryAnn };
        }

        for (int i=0; i < namedQueryArray.length; i++) {
          namedQueryAnn = (JAnnotation) namedQueryArray[i];
          _persistenceUnit.addNamedQuery(namedQueryAnn.getString("name"),
                                         namedQueryAnn.getString("query"));
        }
      }
      else {
        throw new ConfigException(L.l("'{0}' is not an @Entity or @MappedSuperclass.",
                                      type));
      }
    }

    // Validates the type

    String entityName;
    EntityType parentType = null;
    JAnnotation inheritanceAnn = null;

    if (isEntity) {
      validateType(type);

      inheritanceAnn = type.getAnnotation(Inheritance.class);

      if (inheritanceAnn != null) {
        for (JClass parentClass = type.getSuperClass();
             parentClass != null;
             parentClass = parentClass.getSuperClass()) {
          JAnnotation parentEntity = parentClass.getAnnotation(Entity.class);

          if (parentEntity != null) {
            parentType = introspect(parentClass);
            break;
          }
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
      entityType = _persistenceUnit.createEntity(entityName, type, isEmbeddable);
      _entityMap.put(entityName, entityType);

      boolean isField = isField(type);

      if (isField)
        entityType.setFieldAccess(true);

      entityType.setInstanceClassName(type.getName() + "__ResinExt");
      entityType.setEnhanced(true);

      Table table = null;
      JAnnotation tableAnn = type.getAnnotation(javax.persistence.Table.class);

      String tableName = null;
      if (tableAnn != null)
        tableName = (String) tableAnn.get("name");

      if (tableName == null || tableName.equals(""))
        tableName = entityName.toUpperCase();

      if (isEntity) {
        if (parentType == null)
          entityType.setTable(_persistenceUnit.createTable(tableName));
        else if (parentType.isJoinedSubClass())
          entityType.setTable(_persistenceUnit.createTable(tableName));
        else
          entityType.setTable(parentType.getTable());
      }

      JAnnotation tableCache = type.getAnnotation(AmberTableCache.class);
      if (tableCache != null) {
        entityType.getTable().setReadOnly(tableCache.getBoolean("readOnly"));

        long cacheTimeout = Period.toPeriod(tableCache.getString("timeout"));
        entityType.getTable().setCacheTimeout(cacheTimeout);
      }

      JAnnotation secondaryTableAnn = type.getAnnotation(SecondaryTable.class);

      Table secondaryTable = null;

      if (inheritanceAnn != null)
        introspectInheritance(_persistenceUnit, entityType, type);

      if (secondaryTableAnn != null) {
        String secondaryName = secondaryTableAnn.getString("name");

        secondaryTable = _persistenceUnit.createTable(secondaryName);

        entityType.addSecondaryTable(secondaryTable);

        // XXX: pk
      }

      JAnnotation idClassAnn = type.getAnnotation(IdClass.class);

      JClass idClass = null;
      if (idClassAnn != null)
        idClass = idClassAnn.getClass("value");

      if (entityType.getId() != null) {
      }
      else if (isField)
        introspectIdField(_persistenceUnit, entityType, parentType,
                          type, idClass, entityConfig);
      else
        introspectIdMethod(_persistenceUnit, entityType, parentType,
                           type, idClass, entityConfig);

      if (isEntity && (entityType.getId() == null))
        throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id or exactly one @EmbeddedId field.",
                                      entityType.getName()));

      if (isField)
        introspectFields(_persistenceUnit, entityType, parentType, type, entityConfig);
      else
        introspectMethods(_persistenceUnit, entityType, parentType, type, entityConfig);

      if (isEntity) {
        for (JMethod method : type.getMethods()) {
          introspectCallbacks(entityType, method);
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

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(EntityType type, JMethod method)
    throws ConfigException
  {
    JClass []param = method.getParameterTypes();

    if (method.getAnnotation(PostLoad.class) != null) {
      validateCallback("PostLoad", method);

      type.addPostLoadCallback(method);
    }

    if (method.getAnnotation(PrePersist.class) != null) {
      validateCallback("PrePersist", method);

      type.addPrePersistCallback(method);
    }

    if (method.getAnnotation(PostPersist.class) != null) {
      validateCallback("PostPersist", method);

      type.addPostPersistCallback(method);
    }

    if (method.getAnnotation(PreUpdate.class) != null) {
      validateCallback("PreUpdate", method);

      type.addPreUpdateCallback(method);
    }

    if (method.getAnnotation(PostUpdate.class) != null) {
      validateCallback("PostUpdate", method);

      type.addPostUpdateCallback(method);
    }

    if (method.getAnnotation(PreRemove.class) != null) {
      validateCallback("PreRemove", method);

      type.addPreRemoveCallback(method);
    }

    if (method.getAnnotation(PostRemove.class) != null) {
      validateCallback("PostRemove", method);

      type.addPostRemoveCallback(method);
    }
  }

  /**
   * Validates the bean
   */
  public void validateType(JClass type)
    throws ConfigException
  {
    if (type.isFinal())
      throw new ConfigException(L.l("'{0}' must not be final.  Entity beans may not be final.",
                                    type.getName()));

    if (type.isAbstract())
      throw new ConfigException(L.l("'{0}' must not be abstract.  Entity beans may not be abstract.",
                                    type.getName()));

    validateConstructor(type);

    for (JMethod method : type.getMethods()) {
      if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
      }
      else if (method.isFinal())
        throw error(method, L.l("'{0}' must not be final.  Entity beans methods may not be final.",
                                method.getFullName()));
    }
  }

  /**
   * Validates a callback method
   */
  private void validateCallback(String callbackName, JMethod method)
    throws ConfigException
  {
    if (method.isFinal())
      throw new ConfigException(L.l("'{0}' must not be final.  @{1} methods may not be final.",
                                    method.getFullName(),
                                    callbackName));

    if (method.isStatic())
      throw new ConfigException(L.l("'{0}' must not be static.  @{1} methods may not be static.",
                                    method.getFullName(),
                                    callbackName));

    if (method.getParameterTypes().length != 0) {
      throw new ConfigException(L.l("'{0}' must not have any arguments.  @{1} methods have zero arguments.",
                                    method.getFullName(),
                                    callbackName));
    }
  }

  /**
   * Checks for a valid constructor.
   */
  public void validateConstructor(JClass type)
    throws ConfigException
  {
    for (JMethod ctor : type.getConstructors()) {
      JClass []param = ctor.getParameterTypes();

      if (param.length == 0 && ctor.isPublic())
        return;
    }

    throw new ConfigException(L.l("'{0}' needs a public, no-arg constructor.  Entity beans must have public, no-arg constructors.",
                                  type.getName()));
  }

  /**
   * Validates a non-getter method.
   */
  public void validateNonGetter(JMethod method)
    throws ConfigException
  {
    JAnnotation ann = isAnnotatedMethod(method);

    if ((ann != null) && (! ann.getType().equals("javax.persistence.Version")))  {
      throw error(method,
                  L.l("'{0}' is not a valid annotation for {1}.  Only public getters and fields may have property annotations.",
                      ann.getType(), method.getFullName()));
    }
  }

  /**
   * Validates a non-getter method.
   */
  private JAnnotation isAnnotatedMethod(JMethod method)
    throws ConfigException
  {
    for (JAnnotation ann : method.getDeclaredAnnotations()) {
      if (_propertyAnnotations.contains(ann.getType())) {
        return ann;
      }
    }

    return null;
  }

  /**
   * Completes all partial bean introspection.
   */
  public void configure()
    throws ConfigException
  {
    ConfigException exn = null;

    while (_depCompletions.size() > 0 || _linkCompletions.size() > 0) {
      while (_linkCompletions.size() > 0) {
        Completion completion = _linkCompletions.remove(0);

        try {
          completion.complete();
        } catch (Exception e) {
          completion.getEntityType().setConfigException(e);

          if (exn == null)
            exn = new ConfigException(e);
          else
            log.log(Level.WARNING, e.toString(), e);
        }
      }

      if (_depCompletions.size() > 0) {
        Completion completion = _depCompletions.remove(0);


        try {
          completion.complete();
        } catch (Exception e) {
          completion.getEntityType().setConfigException(e);

          log.log(Level.WARNING, e.toString(), e);

          if (exn == null)
            exn = new ConfigException(e);
          else
            log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    if (exn != null)
      throw exn;
  }

  /**
   * Introspects the Inheritance
   */
  private void introspectInheritance(AmberPersistenceUnit persistenceUnit,
                                     EntityType entityType,
                                     JClass type)
    throws ConfigException, SQLException
  {
    JAnnotation inheritanceAnn = type.getAnnotation(Inheritance.class);

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

    if (entityType instanceof SubEntityType) {
      SubEntityType subType = (SubEntityType) entityType;

      subType.getParentType().addSubClass(subType);

      JAnnotation joinAnn = type.getAnnotation(PrimaryKeyJoinColumn.class);

      if (subType.isJoinedSubClass()) {
        linkInheritanceTable(subType.getRootType().getTable(),
                             subType.getTable(),
                             joinAnn);

        subType.setId(new SubId(subType, subType.getRootType()));
      }

      return;
    }

    switch ((InheritanceType) inheritanceAnn.get("strategy")) {
    case JOINED:
      entityType.setJoinedSubClass(true);
      break;
    }

    JAnnotation discriminatorAnn =
      type.getAnnotation(DiscriminatorColumn.class);

    String columnName = null;

    if (discriminatorAnn != null)
      columnName = discriminatorAnn.getString("name");

    if (columnName == null || columnName.equals(""))
      columnName = "TYPE";

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

    Column column = entityType.getTable().createColumn(columnName,
                                                       columnType);

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

  /**
   * Introspects the fields.
   */
  private void introspectIdMethod(AmberPersistenceUnit persistenceUnit,
                                  EntityType entityType,
                                  EntityType parentType,
                                  JClass type,
                                  JClass idClass,
                                  EntityConfig entityConfig)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    IdField idField = null;

    AttributesConfig attributesConfig = null;

    if (entityConfig != null)
      attributesConfig = entityConfig.getAttributes();

    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().getName().equals("java.lang.Object"))
        continue;

      if (! methodName.startsWith("get") || paramTypes.length != 0) {
        continue;
      }

      String fieldName = toFieldName(methodName.substring(3));

      if (parentType != null && parentType.getField(fieldName) != null)
        continue;

      JAnnotation id = method.getAnnotation(javax.persistence.Id.class);

      IdConfig idConfig = null;

      if (attributesConfig != null)
        idConfig = attributesConfig.getId(fieldName);

      if ((id != null) || (idConfig != null)) {
        idField = introspectId(persistenceUnit,
                               entityType,
                               method,
                               fieldName,
                               method.getReturnType(),
                               idConfig);

        if (idField != null)
          keys.add(idField);
      }
      else {
        JAnnotation embeddedId
          = method.getAnnotation(javax.persistence.EmbeddedId.class);

        if (embeddedId != null) {
          idField = introspectEmbeddedId(persistenceUnit,
                                         entityType,
                                         method,
                                         fieldName,
                                         method.getReturnType());
          break;
        }
        else {
          continue;
        }
      }
    }

    if (keys.size() == 0) {
      if (idField != null) {
        // @EmbeddedId was used.
        CompositeId id = new CompositeId(entityType, (EmbeddedIdField) idField);
        entityType.setId(id);
      }
    }
    else if (keys.size() == 1)
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id methods, but no @IdClass.  Compound primary keys require either an @IdClass or exactly one @EmbeddedId field or property.",
                                    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);

      entityType.setId(id);
    }
  }

  /**
   * Introspects the fields.
   */
  private void introspectIdField(AmberPersistenceUnit persistenceUnit,
                                 EntityType entityType,
                                 EntityType parentType,
                                 JClass type,
                                 JClass idClass,
                                 EntityConfig entityConfig)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    AttributesConfig attributesConfig = null;

    if (entityConfig != null)
      attributesConfig = entityConfig.getAttributes();

    for (JField field : type.getFields()) {
      String fieldName = field.getName();

      if (parentType != null && parentType.getField(fieldName) != null)
        continue;

      JAnnotation id = field.getAnnotation(javax.persistence.Id.class);
      JAnnotation embeddedId
        = field.getAnnotation(javax.persistence.EmbeddedId.class);

      IdConfig idConfig = null;

      if (attributesConfig != null)
        idConfig = attributesConfig.getId(fieldName);

      if ((id == null) && (embeddedId == null) && (idConfig == null))
        continue;

      IdField idField = introspectId(persistenceUnit,
                                     entityType,
                                     field,
                                     fieldName,
                                     field.getType(),
                                     idConfig);

      if (idField != null)
        keys.add(idField);
    }

    if (keys.size() == 0) {
    }
    else if (keys.size() == 1)
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id fields, but no @IdClass.  Compound primary keys require an @IdClass.",
                                    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);

      entityType.setId(id);
    }
  }

  /**
   * Check if it's field
   */
  private boolean isField(JClass type)
    throws ConfigException
  {
    if (type == null)
      return false;

    for (JField field : type.getDeclaredFields()) {
      JAnnotation id = field.getAnnotation(javax.persistence.Id.class);

      if (id != null)
        return true;

      id = field.getAnnotation(javax.persistence.EmbeddedId.class);

      if (id != null)
        return true;
    }

    return isField(type.getSuperClass());
  }

  private IdField introspectId(AmberPersistenceUnit persistenceUnit,
                               EntityType entityType,
                               JAccessibleObject field,
                               String fieldName,
                               JClass fieldType,
                               IdConfig idConfig)
    throws ConfigException, SQLException
  {
    JAnnotation id = field.getAnnotation(javax.persistence.Id.class);
    JAnnotation column = field.getAnnotation(javax.persistence.Column.class);
    JAnnotation gen = field.getAnnotation(javax.persistence.GeneratedValue.class);

    ColumnConfig columnConfig = null;

    if (idConfig != null)
      columnConfig = idConfig.getColumn();

    Type amberType = persistenceUnit.createType(fieldType);

    Column keyColumn = createColumn(entityType,
                                    field,
                                    fieldName,
                                    column,
                                    amberType,
                                    columnConfig);

    KeyPropertyField idField;
    idField = new KeyPropertyField(entityType, fieldName, keyColumn);

    JdbcMetaData metaData = persistenceUnit.getMetaData();

    if (gen == null) {
    }
    else if (GenerationType.IDENTITY.equals(gen.get("strategy"))) {
      if (! metaData.supportsIdentity())
        throw new ConfigException(L.l("'{0}' does not support identity.",
                                      metaData.getDatabaseName()));

      keyColumn.setGeneratorType("identity");
      idField.setGenerator("identity");
    }
    else if (GenerationType.SEQUENCE.equals(gen.get("strategy"))) {
      if (! metaData.supportsSequences())
        throw new ConfigException(L.l("'{0}' does not support sequence.",
                                      metaData.getDatabaseName()));

      addSequenceIdGenerator(persistenceUnit, idField, gen);
    }
    else if (GenerationType.TABLE.equals(gen.get("strategy"))) {
      addTableIdGenerator(persistenceUnit, idField, id);
    }
    else if (GenerationType.AUTO.equals(gen.get("strategy"))) {
      if (metaData.supportsIdentity()) {
        keyColumn.setGeneratorType("identity");
        idField.setGenerator("identity");
      }
      else if (metaData.supportsSequences()) {
        addSequenceIdGenerator(persistenceUnit, idField, gen);
      }
      else
        addTableIdGenerator(persistenceUnit, idField, id);
    }

    return idField;
  }

  private IdField introspectEmbeddedId(AmberPersistenceUnit persistenceUnit,
                                       EntityType entityType,
                                       JAccessibleObject field,
                                       String fieldName,
                                       JClass fieldType)
    throws ConfigException, SQLException
  {
    IdField idField;

    idField = new EmbeddedIdField(entityType, fieldName);

    return idField;
  }

  private void addSequenceIdGenerator(AmberPersistenceUnit persistenceUnit,
                                      KeyPropertyField idField,
                                      JAnnotation genAnn)
    throws ConfigException
  {
    idField.setGenerator("sequence");
    idField.getColumn().setGeneratorType("sequence");

    String name = genAnn.getString("generator");

    if (name == null || "".equals(name))
      name = idField.getSourceType().getTable().getName() + "_cseq";

    IdGenerator gen = persistenceUnit.createSequenceGenerator(name, 1);

    idField.getSourceType().setGenerator(idField.getName(), gen);
  }

  private void addTableIdGenerator(AmberPersistenceUnit persistenceUnit,
                                   KeyPropertyField idField,
                                   JAnnotation idAnn)
    throws ConfigException
  {
    idField.setGenerator("table");
    idField.getColumn().setGeneratorType("table");

    String name = idAnn.getString("generator");
    if (name == null || "".equals(name))
      name = "caucho";

    IdGenerator gen = persistenceUnit.getTableGenerator(name);

    if (gen == null) {
      String genName = "GEN_TABLE";

      GeneratorTableType genTable;
      genTable = persistenceUnit.createGeneratorTable(genName);

      gen = genTable.createGenerator(name);

      persistenceUnit.putTableGenerator(name, gen);
    }

    idField.getSourceType().setGenerator(idField.getName(), gen);
  }

  /**
   * Links a secondary table.
   */
  private void linkSecondaryTable(Table primaryTable,
                                  Table secondaryTable,
                                  JAnnotation []joinColumnsAnn)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (Column column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      JAnnotation joinAnn = getJoinColumn(joinColumnsAnn, column.getName());
      String name;

      if (joinAnn == null)
        name = column.getName();
      else
        name = joinAnn.getString("name");

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
                                       primaryTable,
                                       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);
  }

  /**
   * Links a secondary table.
   */
  private void linkInheritanceTable(Table primaryTable,
                                    Table secondaryTable,
                                    JAnnotation joinAnn)
    throws ConfigException
  {
    if (joinAnn != null)
      linkInheritanceTable(primaryTable, secondaryTable,
                           new JAnnotation[] { joinAnn });
    else
      linkInheritanceTable(primaryTable, secondaryTable,
                           (JAnnotation []) null);
  }

  /**
   * Links a secondary table.
   */
  private void linkInheritanceTable(Table primaryTable,
                                    Table secondaryTable,
                                    JAnnotation []joinColumnsAnn)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (Column column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      JAnnotation join;
      join = getJoinColumn(joinColumnsAnn, column.getName());
      String name;

      if (join == null)
        name = column.getName();
      else
        name = join.getString("name");

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
                                       primaryTable,
                                       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);
  }

  /**
   * Introspects the methods.
   */
  private void introspectMethods(AmberPersistenceUnit persistenceUnit,
                                 EntityType entityType,
                                 EntityType parentType,
                                 JClass type,
                                 EntityConfig entityConfig)
    throws ConfigException
  {
    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().getName().equals("java.lang.Object"))
        continue;

      introspectCallbacks(entityType, method);

      String propName;

      if (paramTypes.length != 0) {
        validateNonGetter(method);
        continue;
      }
      else if (methodName.startsWith("get")) {
        propName = methodName.substring(3);
      }
      else if (methodName.startsWith("is") &&
               (method.getReturnType().getName().equals("boolean") ||
                method.getReturnType().getName().equals("java.lang.Boolean"))) {
        propName = methodName.substring(2);
      }
      else {
        validateNonGetter(method);
        continue;
      }

      if (method.getAnnotation(Version.class) != null) {
        validateNonGetter(method);
      }
      else {

        JMethod setter = type.getMethod("set" + propName,
                                        new JClass[] { method.getReturnType() });
        if ((! method.isPublic()) ||
            (setter == null) || (! setter.isPublic())) {

          JAnnotation ann = isAnnotatedMethod(method);

          if (ann == null)
            ann = isAnnotatedMethod(setter);

          if (ann != null) {
            throw new ConfigException(L.l("'{0}' is not a valid annotation for {1}.  Only public persistent property getters with matching setters may have property annotations.",
                                          ann.getType(), method.getFullName()));
          }

          continue;
        }

        // ejb/0g03 for private
        if (method.isStatic()) { // || ! method.isPublic()) {
          validateNonGetter(method);
          continue;
        }
      }

      String fieldName = toFieldName(propName);

      if (parentType != null && parentType.getField(fieldName) != null)
        continue;

      JClass fieldType = method.getReturnType();

      introspectField(persistenceUnit, entityType, method, fieldName, fieldType, entityConfig);
    }
  }

  /**
   * Introspects the fields.
   */
  private void introspectFields(AmberPersistenceUnit persistenceUnit,
                                EntityType entityType,
                                EntityType parentType,
                                JClass type,
                                EntityConfig entityConfig)
    throws ConfigException
  {
    if (entityType.getId() == null)
      throw new IllegalStateException(L.l("{0} has no key", entityType));

    for (JField field : type.getFields()) {
      String fieldName = field.getName();

      if (parentType != null && parentType.getField(fieldName) != null)
        continue;

      if (field.isStatic() || field.isTransient())
        continue;

      JClass fieldType = field.getType();

      introspectField(persistenceUnit, entityType, field, fieldName, fieldType, entityConfig);
    }
  }

  private void introspectField(AmberPersistenceUnit persistenceUnit,
                               EntityType sourceType,
                               JAccessibleObject field,
                               String fieldName,
                               JClass fieldType,
                               EntityConfig entityConfig)
    throws ConfigException
  {
    AttributesConfig attributesConfig = null;
    IdConfig idConfig = null;
    BasicConfig basicConfig = null;

    if (entityConfig != null) {
      attributesConfig = entityConfig.getAttributes();

      idConfig = attributesConfig.getId(field.getName());
      basicConfig = attributesConfig.getBasic(field.getName());
    }

    if ((idConfig != null) ||
        field.isAnnotationPresent(javax.persistence.Id.class)) {
      validateAnnotations(field, _idAnnotations);

      if (! _idTypes.contains(fieldType.getName())) {
        throw error(field, L.l("{0} is an invalid @Id type for {1}.",
                               fieldType.getName(), field.getName()));
      }
    }
    else if ((basicConfig != null) ||
             field.isAnnotationPresent(javax.persistence.Basic.class)) {
      validateAnnotations(field, _basicAnnotations);

      addBasic(sourceType, field, fieldName, fieldType, basicConfig);
    }
    else if (field.isAnnotationPresent(javax.persistence.Version.class)) {
      validateAnnotations(field, _versionAnnotations);

      addVersion(sourceType, field, fieldName, fieldType);
    }
    else if (field.isAnnotationPresent(javax.persistence.ManyToOne.class)) {
      JAnnotation ann = field.getAnnotation(javax.persistence.ManyToOne.class);

      validateAnnotations(field, _manyToOneAnnotations);

      JClass targetEntity = ann.getClass("targetEntity");

      if (targetEntity == null ||
          targetEntity.getName().equals("void")) {
        targetEntity = fieldType;
      }

      if (! targetEntity.isAnnotationPresent(javax.persistence.Entity.class)) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne relations must target a valid @Entity.",
                               targetEntity.getName(), field.getName()));
      }

      if (! fieldType.isAssignableFrom(targetEntity)) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne targetEntity must be assignable to the field type '{2}'.",
                               targetEntity.getName(),
                               field.getName(),
                               fieldType.getName()));
      }

      _linkCompletions.add(new ManyToOneCompletion(sourceType,
                                                   field,
                                                   fieldName,
                                                   fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.OneToMany.class)) {
      validateAnnotations(field, _oneToManyAnnotations);

      if (field.isAnnotationPresent(javax.persistence.MapKey.class)) {
        if (!fieldType.getName().equals("java.util.Map")) {
          throw error(field, L.l("'{0}' is an illegal @OneToMany/@MapKey type for {1}. @MapKey must be a java.util.Map",
                                 fieldType.getName(),
                                 field.getName()));
        }
      }
      else if (! _oneToManyTypes.contains(fieldType.getName())) {
        throw error(field, L.l("'{0}' is an illegal @OneToMany type for {1}.  @OneToMany must be a java.util.Collection, java.util.List or java.util.Map",
                               fieldType.getName(),
                               field.getName()));
      }

      _depCompletions.add(new OneToManyCompletion(sourceType,
                                                  field,
                                                  fieldName,
                                                  fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.OneToOne.class)) {
      validateAnnotations(field, _oneToOneAnnotations);
      _depCompletions.add(new OneToOneCompletion(sourceType,
                                                 field,
                                                 fieldName,
                                                 fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.ManyToMany.class)) {

      if (field.isAnnotationPresent(javax.persistence.MapKey.class)) {
        if (!fieldType.getName().equals("java.util.Map")) {
          throw error(field, L.l("'{0}' is an illegal @ManyToMany/@MapKey type for {1}. @MapKey must be a java.util.Map",
                                 fieldType.getName(),
                                 field.getName()));
        }
      }

      Completion completion = new ManyToManyCompletion(sourceType,
                                                       field,
                                                       fieldName,
                                                       fieldType);

      JAnnotation ann = field.getAnnotation(ManyToMany.class);

      if ("".equals(ann.getString("mappedBy")))
        _linkCompletions.add(completion);
      else
        _depCompletions.add(completion);
    }
    else if (field.isAnnotationPresent(javax.persistence.Embedded.class)) {
      validateAnnotations(field, _embeddedAnnotations);
      _depCompletions.add(new EmbeddedCompletion(sourceType,
                                                 field,
                                                 fieldName,
                                                 fieldType,
                                                 false));
    }
    else if (field.isAnnotationPresent(javax.persistence.EmbeddedId.class)) {
      validateAnnotations(field, _embeddedIdAnnotations);
      _depCompletions.add(new EmbeddedCompletion(sourceType,
                                                 field,
                                                 fieldName,
                                                 fieldType,
                                                 true));
    }
    else if (field.isAnnotationPresent(javax.persistence.Transient.class)) {
    }
    else {
      addBasic(sourceType, field, fieldName, fieldType, basicConfig);
    }
  }

  private void addBasic(EntityType sourceType,
                        JAccessibleObject field,
                        String fieldName,
                        JClass fieldType,
                        BasicConfig basicConfig)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation basicAnn = field.getAnnotation(javax.persistence.Basic.class);
    JAnnotation columnAnn = field.getAnnotation(javax.persistence.Column.class);

    if (_basicTypes.contains(fieldType.getName())) {
    }
    else if (fieldType.isAssignableTo(java.io.Serializable.class)) {
    }
    else
      throw error(field, L.l("{0} is an invalid @Basic type for {1}.",
                             fieldType.getName(), field.getName()));

    Type amberType = persistenceUnit.createType(fieldType);

    ColumnConfig columnConfig = null;

    if (basicConfig != null)
      columnConfig = basicConfig.getColumn();

    Column fieldColumn = createColumn(sourceType, field, fieldName,
                                      columnAnn, amberType, columnConfig);

    PropertyField property = new PropertyField(sourceType, fieldName);
    property.setColumn(fieldColumn);

    if (basicAnn != null)
      property.setLazy(basicAnn.get("fetch") == FetchType.LAZY);
    else if (basicConfig != null)
      property.setLazy(basicConfig.getFetchType() == FetchType.LAZY);

    /*
      field.setInsertable(insertable);
      field.setUpdateable(updateable);
    */

    sourceType.addField(property);
  }

  private void addVersion(EntityType sourceType,
                          JAccessibleObject field,
                          String fieldName,
                          JClass fieldType)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation columnAnn = field.getAnnotation(javax.persistence.Column.class);

    if (! _versionTypes.contains(fieldType.getName())) {
      throw error(field, L.l("{0} is an invalid @Version type for {1}.",
                             fieldType.getName(), field.getName()));
    }

    Type amberType = persistenceUnit.createType(fieldType);

    ColumnConfig columnConfig = null;

    // if (versionConfig != null)
    //   columnConfig = Config.getColumn();

    Column fieldColumn = createColumn(sourceType, field, fieldName,
                                      columnAnn, amberType, columnConfig);

    VersionField version = new VersionField(sourceType, fieldName);
    version.setColumn(fieldColumn);

    sourceType.setVersionField(version);
  }

  private Column createColumn(EntityType entityType,
                              JAccessibleObject field,
                              String fieldName,
                              JAnnotation columnAnn,
                              Type amberType,
                              ColumnConfig columnConfig)
    throws ConfigException
  {
    String name;

    if (columnAnn != null && ! columnAnn.get("name").equals(""))
      name = (String) columnAnn.get("name");
    else if (columnConfig != null && ! columnConfig.getName().equals(""))
      name = columnConfig.getName();
    else
      name = toSqlName(fieldName);

    Column column = null;

    if (columnAnn != null && ! columnAnn.get("table").equals("")) {
      String tableName = columnAnn.getString("table");
      Table table;

      table = entityType.getSecondaryTable(tableName);

      if (table == null)
        throw error(field, L.l("{0} @Column(table='{1}') is an unknown secondary table.",
                               fieldName,
                               tableName));

      column = table.createColumn(name, amberType);
    }
    else if (!entityType.isEmbeddable()) {
      column = entityType.getTable().createColumn(name, amberType);
    }

    if ((column != null) && (columnAnn != null)) {
      // primaryKey = column.primaryKey();
      column.setUnique(columnAnn.getBoolean("unique"));
      column.setNotNull(! columnAnn.getBoolean("nullable"));
      //insertable = column.insertable();
      //updateable = column.updatable();
      if (! "".equals(columnAnn.getString("columnDefinition")))
        column.setSQLType(columnAnn.getString("columnDefinition"));
      column.setLength(columnAnn.getInt("length"));
      int precision = columnAnn.getInt("precision");
      if (precision < 0) {
        throw error(field, L.l("{0} @Column precision cannot be less than 0.",
                               fieldName));
      }

      int scale = columnAnn.getInt("scale");
      if (scale < 0) {
        throw error(field, L.l("{0} @Column scale cannot be less than 0.",
                               fieldName));
      }

      // this test implicitly works for case where
      // precision is not set explicitly (ie: set to 0 by default)
      // and scale is set
      if (scale > precision) {
        throw error(field, L.l("{0} @Column scale cannot be greater than precision. Must set precision to a non-zero value before setting scale.",
                               fieldName));
      }

      if (precision > 0) {
        column.setPrecision(precision);
        column.setScale(scale);
      }
    }

    return column;
  }

  private void addManyToOne(EntityType sourceType,
                            JAccessibleObject field,
                            String fieldName,
                            JClass fieldType)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation manyToOneAnn = field.getAnnotation(ManyToOne.class);

    if (manyToOneAnn == null) {
      // XXX: ejb/0o03
      manyToOneAnn = field.getAnnotation(OneToOne.class);
    }

    if (manyToOneAnn.get("fetch") == FetchType.EAGER) {
      if (sourceType.getBeanClass().getName().equals(fieldType.getName())) {
        throw error(field, L.l("'{0}': '{1}' is an illegal recursive type for @OneToOne/@ManyToOne with EAGER fetching. You should specify FetchType.LAZY for this relationship.",
                               field.getName(),
                               fieldType.getName()));
      }
    }

    JAnnotation joinColumns = field.getAnnotation(JoinColumns.class);
    Object []joinColumnsAnn = null;

    if (joinColumns != null)
      joinColumnsAnn = (Object []) joinColumns.get("value");
    JAnnotation joinColumnAnn = field.getAnnotation(JoinColumn.class);

    if (joinColumnsAnn != null && joinColumnAnn != null) {
      throw error(field, L.l("{0} may not have both @JoinColumn and @JoinColumns",
                             field.getName()));
    }

    if (joinColumnAnn != null)
      joinColumnsAnn = new Object[] { joinColumnAnn };

    JClass targetClass = manyToOneAnn.getClass("targetEntity");
    String targetName = "";
    if (targetClass != null)
      targetName = targetClass.getName();

    if (targetName.equals("") || targetName.equals("void"))
      targetName = fieldType.getName();

    EntityManyToOneField manyToOneField;
    manyToOneField = new EntityManyToOneField(sourceType, fieldName);

    EntityType targetType = persistenceUnit.createEntity(targetName, fieldType);

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(manyToOneAnn.get("fetch") == FetchType.LAZY);

    sourceType.addField(manyToOneField);

    Table sourceTable = sourceType.getTable();

    validateJoinColumns(field, joinColumnsAnn, targetType);

    ArrayList<ForeignColumn> foreignColumns = new ArrayList<ForeignColumn>();
    for (Column keyColumn : targetType.getId().getColumns()) {
      JAnnotation joinAnn = getJoinColumn(joinColumnsAnn, keyColumn.getName());

      String columnName = fieldName.toUpperCase() + '_' + keyColumn.getName();
      if (joinAnn != null)
        columnName = joinAnn.getString("name");

      ForeignColumn foreignColumn;

      foreignColumn = sourceTable.createForeignColumn(columnName, keyColumn);

      if (joinAnn != null) {
        foreignColumn.setNotNull(! joinAnn.getBoolean("nullable"));
        foreignColumn.setUnique(joinAnn.getBoolean("unique"));
      }

      foreignColumns.add(foreignColumn);
    }

    LinkColumns linkColumns = new LinkColumns(sourceType.getTable(),
                                              targetType.getTable(),
                                              foreignColumns);

    manyToOneField.setLinkColumns(linkColumns);

    manyToOneField.init();
  }

  private JAnnotation getJoinColumn(JAnnotation joinColumns, String keyName)
  {
    if (joinColumns == null)
      return null;

    return getJoinColumn((Object []) joinColumns.get("value"), keyName);
  }

  private void validateJoinColumns(JAccessibleObject field,
                                   Object []columnsAnn,
                                   EntityType targetType)
    throws ConfigException
  {
    if (columnsAnn == null) // || columnsAnn.length == 0)
      return;

    com.caucho.amber.field.Id id = targetType.getId();

    if (id.getColumns().size() != columnsAnn.length) {
      throw error(field, L.l("Number of @JoinColumns for '{1}' ({0}) does not match the number of primary key columns for '{3}' ({2}).",
                             "" + columnsAnn.length,
                             field.getName(),
                             id.getColumns().size(),
                             targetType.getName()));

    }

    for (int i = 0; i < columnsAnn.length; i++) {
      JAnnotation ann = (JAnnotation) columnsAnn[i];

      String ref = ann.getString("referencedColumnName");

      if (ref.equals("") && columnsAnn.length > 1)
        throw error(field, L.l("referencedColumnName is required when more than one @JoinColumn is specified."));

      Column column = findColumn(id.getColumns(), ref);

      if (column == null)
        throw error(field, L.l("referencedColumnName '{0}' does not match any key column in '{1}'.",
                               ref, targetType.getName()));
    }
  }

  private Column findColumn(ArrayList<Column> columns, String ref)
  {
    if (ref.equals("") && columns.size() == 1)
      return columns.get(0);

    for (Column column : columns) {
      if (column.getName().equals(ref))
        return column;
    }

    return null;
  }

  private JAnnotation getJoinColumn(Object []columnsAnn, String keyName)
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return null;

    for (int i = 0; i < columnsAnn.length; i++) {
      JAnnotation ann = (JAnnotation) columnsAnn[i];

      String ref = ann.getString("referencedColumnName");

      if (ref.equals("") || ref.equals(keyName))
        return ann;
    }

    return null;
  }

  /* XXX: inheritance
     private JAnnotation getJoinColumn(JAnnotation []columnsAnn, String keyName)
     {
     if (columns == null || columns.length == 0)
     return null;

     for (int i = 0; i < columns.length; i++) {
     String ref = columns[i].getString("referencedColumnName");

     if (ref.equals("") || ref.equals(keyName))
     return columns[i];
     }

     return null;
     }
  */

  private void addManyToMany(EntityType sourceType,
                             JAccessibleObject field,
                             String fieldName,
                             JClass fieldType)
    throws ConfigException
  {
    JAnnotation manyToManyAnn = field.getAnnotation(ManyToMany.class);

    JType retType;

    if (field instanceof JField)
      retType = ((JField) field).getGenericType();
    else
      retType = ((JMethod) field).getGenericReturnType();

    JType []typeArgs = retType.getActualTypeArguments();

    JClass targetEntity = manyToManyAnn.getClass("targetEntity");
    String targetName = "";

    if (targetEntity != null)
      targetName = targetEntity.getName();

    if (! targetName.equals("") && ! targetName.equals("void")) {
    }
    else if (typeArgs.length > 0)
      targetName = typeArgs[0].getName();
    else
      throw error(field, L.l("Can't determine targetEntity for {0}.  @OneToMany properties must target @Entity beans.",
                             field.getName()));

    EntityType targetType = _persistenceUnit.getEntity(targetName);
    if (targetType == null)
      throw error(field,
                  L.l("targetEntity '{0}' is not an @Entity bean for {1}.  The targetEntity of a @ManyToMany collection must be an @Entity bean.",
                      targetName,
                      field.getName()));

    String mappedBy = manyToManyAnn.getString("mappedBy");

    if (! "".equals(mappedBy)) {
      EntityManyToManyField sourceField
        = (EntityManyToManyField) targetType.getField(mappedBy);

      EntityManyToManyField manyToManyField;

      manyToManyField = new EntityManyToManyField(sourceType,
                                                  fieldName, sourceField);
      manyToManyField.setType(targetType);
      sourceType.addField(manyToManyField);

      return;
    }

    EntityManyToManyField manyToManyField;

    manyToManyField = new EntityManyToManyField(sourceType, fieldName);
    manyToManyField.setType(targetType);

    String sqlTable = sourceType.getTable().getName() + "_" + targetType.getTable().getName();

    JAnnotation joinTableAnn = field.getAnnotation(JoinTable.class);

    Table mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;

    if (joinTableAnn != null) {
      if (! joinTableAnn.getString("name").equals(""))
        sqlTable = joinTableAnn.getString("name");

      mapTable = _persistenceUnit.createTable(sqlTable);

      sourceColumns = calculateColumns(field,
                                       mapTable,
                                       sourceType.getTable().getName() + "_",
                                       sourceType,
                                       (Object []) joinTableAnn.get("joinColumns"));

      targetColumns = calculateColumns(field,
                                       mapTable,
                                       targetType.getTable().getName() + "_",
                                       targetType,
                                       (Object []) joinTableAnn.get("inverseJoinColumns"));
    }
    else {
      mapTable = _persistenceUnit.createTable(sqlTable);

      sourceColumns = calculateColumns(mapTable,
                                       sourceType.getTable().getName() + "_",
                                       sourceType);

      targetColumns = calculateColumns(mapTable,
                                       targetType.getTable().getName() + "_",
                                       targetType);
    }

    manyToManyField.setAssociationTable(mapTable);
    manyToManyField.setTable(sqlTable);

    manyToManyField.setSourceLink(new LinkColumns(mapTable,
                                                  sourceType.getTable(),
                                                  sourceColumns));

    manyToManyField.setTargetLink(new LinkColumns(mapTable,
                                                  targetType.getTable(),
                                                  targetColumns));

    JAnnotation mapKeyAnn = field.getAnnotation(MapKey.class);

    if (mapKeyAnn != null) {

      String key = mapKeyAnn.getString("name");

      String getter = "get" +
        Character.toUpperCase(key.charAt(0)) + key.substring(1);

      JMethod method = targetType.getGetter(getter);

      if (method == null) {
        throw error(field,
                    L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @ManyToMany targetEntity is incorrect.",
                        targetName, key));
      }

      manyToManyField.setMapKey(key);
    }

    sourceType.addField(manyToManyField);
  }

  private void validateAnnotations(JAccessibleObject field,
                                   HashSet<String> validAnnotations)
    throws ConfigException
  {
    for (JAnnotation ann : field.getDeclaredAnnotations()) {
      String name = ann.getType();

      if (! name.startsWith("javax.persistence"))
        continue;

      if (! validAnnotations.contains(name)) {
        throw error(field, L.l("{0} may not have a @{1} annotation.",
                               field.getName(),
                               name));
      }
    }
  }

  private ConfigException error(JAccessibleObject field,
                                String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + ": " + msg);
  }

  static String toFieldName(String name)
  {
    if (Character.isLowerCase(name.charAt(0)))
      return name;
    else if (name.length() == 1 ||
             Character.isLowerCase(name.charAt(1)))
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    else
      return name;
  }

  static ArrayList<ForeignColumn>
    calculateColumns(Table mapTable,
                     EntityType type,
                     Object []joinColumnsAnn)
  {
    if (joinColumnsAnn == null || joinColumnsAnn.length == 0)
      return calculateColumns(mapTable, type);

    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    for (int i = 0; i < joinColumnsAnn.length; i++) {
      ForeignColumn foreignColumn;
      JAnnotation joinColumnAnn = (JAnnotation) joinColumnsAnn[i];

      foreignColumn =
        mapTable.createForeignColumn(joinColumnAnn.getString("name"),
                                     type.getId().getKey().getColumns().get(0));

      columns.add(foreignColumn);
    }

    return columns;
  }

  ArrayList<ForeignColumn>
    calculateColumns(JAccessibleObject field,
                     Table mapTable,
                     String prefix,
                     EntityType type,
                     Object []joinColumnsAnn)
    throws ConfigException
  {
    if (joinColumnsAnn == null || joinColumnsAnn.length == 0)
      return calculateColumns(mapTable, prefix, type);

    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    ArrayList<IdField> idFields = type.getId().getKeys();

    if (joinColumnsAnn.length != idFields.size()) {
      throw error(field, L.l("@JoinColumns for {0} do not match number of the primary key columns in {1}.  The foreign key columns must match the primary key columns.",
                             field.getName(),
                             type.getName()));
    }

    for (int i = 0; i < joinColumnsAnn.length; i++) {
      ForeignColumn foreignColumn;
      JAnnotation joinColumnAnn = (JAnnotation) joinColumnsAnn[i];

      foreignColumn =
        mapTable.createForeignColumn(joinColumnAnn.getString("name"),
                                     idFields.get(i).getColumns().get(0));

      columns.add(foreignColumn);
    }

    return columns;
  }

  static ArrayList<ForeignColumn> calculateColumns(Table mapTable,
                                                   EntityType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    for (Column key : type.getId().getColumns()) {
      columns.add(mapTable.createForeignColumn(key.getName(), key));
    }

    return columns;
  }

  static ArrayList<ForeignColumn> calculateColumns(Table mapTable,
                                                   String prefix,
                                                   EntityType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    for (Column key : type.getId().getColumns()) {
      columns.add(mapTable.createForeignColumn(prefix + key.getName(), key));
    }

    return columns;
  }

  static String toSqlName(String name)
  {
    return name.toUpperCase();
    /*
      CharBuffer cb = new CharBuffer();

      for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (! Character.isUpperCase(ch))
      cb.append(ch);
      else if (i > 0 && ! Character.isUpperCase(name.charAt(i - 1))) {
      cb.append("_");
      cb.append(Character.toLowerCase(ch));
      }
      else if (i > 0 &&
      i + 1 < name.length() &&
      ! Character.isUpperCase(name.charAt(i + 1))) {
      cb.append("_");
      cb.append(Character.toLowerCase(ch));
      }
      else
      cb.append(Character.toLowerCase(ch));
      }

      return cb.toString();
    */
  }

  private EntityManyToOneField getSourceField(EntityType targetType,
                                              String mappedBy)
  {
    for (AmberField field : targetType.getFields()) {
      if (field.getName().equals(mappedBy)) {
        return (EntityManyToOneField) field;
      }
    }

    return null;
  }

  /**
   * completes for dependent
   */
  class Completion {
    protected EntityType _entityType;

    protected Completion(EntityType entityType)
    {
      _entityType = entityType;
    }

    EntityType getEntityType()
    {
      return _entityType;
    }

    void complete()
      throws ConfigException
    {
    }
  }

  /**
   * completes for dependent
   */
  class OneToManyCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToManyCompletion(EntityType type,
                        JAccessibleObject field,
                        String fieldName,
                        JClass fieldType)
    {
      super(type);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToManyAnn = _field.getAnnotation(OneToMany.class);

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      JType retType;

      if (_field instanceof JField)
        retType = ((JField) _field).getGenericType();
      else
        retType = ((JMethod) _field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      JClass targetEntity = oneToManyAnn.getClass("targetEntity");
      String targetName = "";

      if (targetEntity != null)
        targetName = targetEntity.getName();

      if (! targetName.equals("") && ! targetName.equals("void")) {
      }
      else if (typeArgs.length > 0)
        targetName = typeArgs[0].getName();
      else
        throw error(_field, L.l("Can't determine targetEntity for {0}.  @OneToMany properties must target @Entity beans.",
                                _field.getName()));

      EntityType targetType = persistenceUnit.getEntity(targetName);
      if (targetType == null)
        throw error(_field,
                    L.l("targetEntity '{0}' is not an @Entity bean for {1}.  The targetEntity of a @OneToMany collection must be an @Entity bean.",
                        targetName,
                        _field.getName()));

      String mappedBy = oneToManyAnn.getString("mappedBy");

      if (mappedBy != null && ! mappedBy.equals("")) {
        oneToManyBidirectional(targetType, targetName, mappedBy);
      }
      else {
        oneToManyUnidirectional(targetType, targetName);
      }
    }

    private void oneToManyBidirectional(EntityType targetType,
                                        String targetName,
                                        String mappedBy)
      throws ConfigException
    {
      if (_field.getAnnotation(JoinTable.class) != null) {
        throw error(_field,
                    L.l("Bidirectional @ManyToOne property {0} may not have a @JoinTable annotation.",
                        _field.getName()));
      }

      EntityManyToOneField sourceField = getSourceField(targetType,
                                                        mappedBy);

      if (sourceField == null)
        throw error(_field, L.l("'{0}' does not have matching field for @ManyToOne(mappedBy={1}).",
                                targetType.getName(),
                                mappedBy));

      EntityOneToManyField oneToMany;

      oneToMany = new EntityOneToManyField(_entityType, _fieldName);
      oneToMany.setSourceField(sourceField);

      JAnnotation mapKeyAnn = _field.getAnnotation(MapKey.class);

      if (mapKeyAnn != null) {

        String key = mapKeyAnn.getString("name");

        String getter = "get" +
          Character.toUpperCase(key.charAt(0)) + key.substring(1);

        JMethod method = targetType.getGetter(getter);

        if (method == null) {
          throw error(_field,
                      L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @OneToMany targetEntity is incorrect.",
                          targetName, key));
        }

        oneToMany.setMapKey(key);
      }

      _entityType.addField(oneToMany);
    }

    private void oneToManyUnidirectional(EntityType targetType,
                                         String targetName)
      throws ConfigException
    {
      EntityManyToManyField manyToManyField;

      manyToManyField = new EntityManyToManyField(_entityType, _fieldName);
      manyToManyField.setType(targetType);

      String sqlTable = _entityType.getTable().getName() + "_" + targetType.getTable().getName();

      JAnnotation joinTableAnn =
        _field.getAnnotation(JoinTable.class);

      Table mapTable = null;

      ArrayList<ForeignColumn> sourceColumns = null;
      ArrayList<ForeignColumn> targetColumns = null;

      if (joinTableAnn != null) {
        if (! joinTableAnn.getString("name").equals(""))
          sqlTable = joinTableAnn.getString("name");

        mapTable = _persistenceUnit.createTable(sqlTable);

        sourceColumns = calculateColumns(_field,
                                         mapTable,
                                         _entityType.getTable().getName() + "_",
                                         _entityType,
                                         (Object []) joinTableAnn.get("joinColumns"));

        targetColumns = calculateColumns(_field,
                                         mapTable,
                                         targetType.getTable().getName() + "_",
                                         targetType,
                                         (Object []) joinTableAnn.get("inverseJoinColumns"));
      }
      else {
        mapTable = _persistenceUnit.createTable(sqlTable);

        sourceColumns = calculateColumns(mapTable,
                                         _entityType.getTable().getName() + "_",
                                         _entityType);

        targetColumns = calculateColumns(mapTable,
                                         targetType.getTable().getName() + "_",
                                         targetType);
      }

      manyToManyField.setAssociationTable(mapTable);
      manyToManyField.setTable(sqlTable);

      manyToManyField.setSourceLink(new LinkColumns(mapTable,
                                                    _entityType.getTable(),
                                                    sourceColumns));

      manyToManyField.setTargetLink(new LinkColumns(mapTable,
                                                    targetType.getTable(),
                                                    targetColumns));

      JAnnotation mapKeyAnn = _field.getAnnotation(MapKey.class);

      if (mapKeyAnn != null) {

        String key = mapKeyAnn.getString("name");

        String getter = "get" +
          Character.toUpperCase(key.charAt(0)) + key.substring(1);

        JMethod method = targetType.getGetter(getter);

        if (method == null) {
          throw error(_field,
                      L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @ManyToMany targetEntity is incorrect.",
                          targetName, key));
        }

        manyToManyField.setMapKey(key);
      }

      _entityType.addField(manyToManyField);
    }
  }

  /**
   * completes for dependent
   */
  class OneToOneCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToOneCompletion(EntityType type,
                       JAccessibleObject field,
                       String fieldName,
                       JClass fieldType)
    {
      super(type);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToOneAnn = _field.getAnnotation(OneToOne.class);

      if (oneToOneAnn.get("fetch") == FetchType.EAGER) {
        if (_entityType.getBeanClass().getName().equals(_fieldType.getName())) {
          throw error(_field, L.l("'{0}': '{1}' is an illegal recursive type for @OneToOne with EAGER fetching. You should specify FetchType.LAZY for this relationship.",
                                  _field.getName(),
                                  _fieldType.getName()));
        }
      }

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      JClass targetEntity = oneToOneAnn.getClass("targetEntity");

      if (targetEntity == null || targetEntity.getName().equals("void"))
        targetEntity = _fieldType;

      if (! targetEntity.isAnnotationPresent(javax.persistence.Entity.class)) {
        throw error(_field, L.l("'{0}' is an illegal targetEntity for {1}.  @OneToOne relations must target a valid @Entity.",
                                targetEntity.getName(), _field.getName()));
      }

      if (! _fieldType.isAssignableFrom(targetEntity)) {
        throw error(_field, L.l("'{0}' is an illegal targetEntity for {1}.  @OneToOne targetEntity must be assignable to the field type '{2}'.",
                                targetEntity.getName(),
                                _field.getName(),
                                _fieldType.getName()));
      }

      String targetName = targetEntity.getName();

      String mappedBy =  oneToOneAnn.getString("mappedBy");

      EntityType targetType = null;

      if (targetName != null && ! targetName.equals("")) {
        targetType = persistenceUnit.getEntity(targetName);

        if (targetType == null)
          throw new ConfigException(L.l("{0}: '{1}' is an unknown entity for '{2}'.",
                                        _field.getDeclaringClass().getName(),
                                        targetName,
                                        _field.getName()));
      }
      else {
        targetType = persistenceUnit.getEntity(_field.getReturnType().getName());

        if (targetType == null)
          throw new ConfigException(L.l("{0} can't determine target name for '{1}'",
                                        _field.getDeclaringClass().getName(),
                                        _field.getName()));
      }

      if (mappedBy == null || mappedBy.equals("")) {
        // ejb/0o03
        addManyToOne(_entityType, _field, _fieldName, _field.getReturnType());

        // XXX: set unique
      }
      else {
        EntityManyToOneField sourceField
          = getSourceField(targetType, mappedBy);

        if (sourceField == null)
          throw new ConfigException(L.l("{0}: OneToOne target '{1}' does not have a matching ManyToOne relation.",
                                        _field.getDeclaringClass().getName(),
                                        targetType.getName()));

        DependentEntityOneToOneField oneToOne;

        oneToOne = new DependentEntityOneToOneField(_entityType, _fieldName);
        oneToOne.setTargetField(sourceField);
        sourceField.setTargetField(oneToOne);
        oneToOne.setLazy(oneToOneAnn.get("fetch") == FetchType.LAZY);

        _entityType.addField(oneToOne);
      }
    }
  }

  /**
   * completes for dependent
   */
  class ManyToManyCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToManyCompletion(EntityType type,
                         JAccessibleObject field,
                         String fieldName,
                         JClass fieldType)
    {
      super(type);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToMany(_entityType, _field, _fieldName, _fieldType);
    }
  }

  /**
   * completes for link
   */
  class ManyToOneCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToOneCompletion(EntityType type,
                        JAccessibleObject field,
                        String fieldName,
                        JClass fieldType)
    {
      super(type);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToOne(_entityType, _field, _fieldName, _fieldType);
    }
  }

  /**
   * completes for dependent
   */
  class EmbeddedCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;
    // The same completion is used for both:
    // @Embedded or @EmbeddedId
    private boolean _embeddedId;

    EmbeddedCompletion(EntityType type,
                       JAccessibleObject field,
                       String fieldName,
                       JClass fieldType,
                       boolean embeddedId)
    {
      super(type);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
      _embeddedId = embeddedId;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation attributeOverrideAnn = _field.getAnnotation(AttributeOverride.class);
      JAnnotation attributeOverridesAnn = _field.getAnnotation(AttributeOverrides.class);

      if (attributeOverrideAnn != null && attributeOverridesAnn != null) {
        throw error(_field, L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
                                _field.getName()));
      }

      Object attOverridesAnn[];

      if (attributeOverrideAnn != null) {
        attOverridesAnn = new Object[] { attributeOverrideAnn };
      }
      else if (attributeOverridesAnn != null) {
        attOverridesAnn = (Object []) attributeOverridesAnn.get("value");
      }
      else {
        return;
      }

      EntityEmbeddedField embeddedField;

      if (_embeddedId) {
        embeddedField = _entityType.getId().getEmbeddedIdField();
      } else {
        embeddedField = new EntityEmbeddedField(_entityType, _fieldName);
      }

      embeddedField.setEmbeddedId(_embeddedId);

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      EntityType type = persistenceUnit.createEntity(_fieldType.getName(), _fieldType, true);

      embeddedField.setType(type);

      _entityType.addField(embeddedField);

      // XXX: todo ...
      // validateAttributeOverrides(_field, attributeOverridesAnn, type);

      Table sourceTable = _entityType.getTable();

      HashMap<String, Column> embeddedColumns = new HashMap<String, Column>();
      HashMap<String, String> fieldNameByColumn = new HashMap<String, String>();
      JField fields[] = _fieldType.getDeclaredFields();

      for (int i=0; i<attOverridesAnn.length; i++) {
        String embeddedFieldName = ((JAnnotation) attOverridesAnn[i]).getString("name");

        JAnnotation columnAnn = ((JAnnotation) attOverridesAnn[i]).getAnnotation("column");
        String columnName = columnAnn.getString("name");

        Type amberType = StringType.create();

        for (int j=0; j < fields.length; j++) {
          if (embeddedFieldName.equals(fields[j].getName()))
            amberType = _persistenceUnit.createType(fields[j].getType());
        }

        Column column = sourceTable.createColumn(columnName, amberType);

        column.setNotNull(! columnAnn.getBoolean("nullable"));
        column.setUnique(columnAnn.getBoolean("unique"));

        embeddedColumns.put(columnName, column);
        fieldNameByColumn.put(columnName, embeddedFieldName);
      }

      embeddedField.setEmbeddedColumns(embeddedColumns);
      embeddedField.setFieldNameByColumn(fieldNameByColumn);

      embeddedField.init();
    }
  }

  static {
    // annotations allowed with a @Basic annotation
    _basicAnnotations.add("javax.persistence.Basic");
    _basicAnnotations.add("javax.persistence.Column");
    _basicAnnotations.add("javax.persistence.Enumerated");
    _basicAnnotations.add("javax.persistence.Lob");
    _basicAnnotations.add("javax.persistence.Temporal");

    // non-serializable types allowed with a @Basic annotation
    _basicTypes.add("boolean");
    _basicTypes.add("byte");
    _basicTypes.add("char");
    _basicTypes.add("short");
    _basicTypes.add("int");
    _basicTypes.add("long");
    _basicTypes.add("float");
    _basicTypes.add("double");
    _basicTypes.add("[byte");
    _basicTypes.add("[char");
    _basicTypes.add("[java.lang.Byte");
    _basicTypes.add("[java.lang.Character");

    // annotations allowed with an @Id annotation
    _idAnnotations.add("javax.persistence.Column");
    _idAnnotations.add("javax.persistence.GeneratedValue");
    _idAnnotations.add("javax.persistence.Id");
    _idAnnotations.add("javax.persistence.SequenceGenerator");
    _idAnnotations.add("javax.persistence.TableGenerator");

    // allowed with a @Id annotation
    _idTypes.add("boolean");
    _idTypes.add("byte");
    _idTypes.add("char");
    _idTypes.add("short");
    _idTypes.add("int");
    _idTypes.add("long");
    _idTypes.add("float");
    _idTypes.add("double");
    _idTypes.add("java.lang.Boolean");
    _idTypes.add("java.lang.Byte");
    _idTypes.add("java.lang.Character");
    _idTypes.add("java.lang.Short");
    _idTypes.add("java.lang.Integer");
    _idTypes.add("java.lang.Long");
    _idTypes.add("java.lang.Float");
    _idTypes.add("java.lang.Double");
    _idTypes.add("java.lang.String");
    _idTypes.add("java.util.Date");
    _idTypes.add("java.sql.Date");

    // annotations allowed with a @ManyToOne annotation
    _manyToOneAnnotations.add("javax.persistence.ManyToOne");
    _manyToOneAnnotations.add("javax.persistence.JoinColumn");
    _manyToOneAnnotations.add("javax.persistence.JoinColumns");

    // annotations allowed with a @OneToMany annotation
    _oneToManyAnnotations.add("javax.persistence.OneToMany");
    _oneToManyAnnotations.add("javax.persistence.JoinTable");
    _oneToManyAnnotations.add("javax.persistence.MapKey");

    // types allowed with a @OneToMany annotation
    _oneToManyTypes.add("java.util.Collection");
    _oneToManyTypes.add("java.util.List");
    _oneToManyTypes.add("java.util.Set");
    _oneToManyTypes.add("java.util.Map");

    // annotations allowed with a @ManyToMany annotation
    _manyToManyAnnotations.add("javax.persistence.ManyToMany");
    _manyToManyAnnotations.add("javax.persistence.JoinTable");
    _manyToManyAnnotations.add("javax.persistence.MapKey");

    // types allowed with a @ManyToMany annotation
    _manyToManyTypes.add("java.util.Collection");
    _manyToManyTypes.add("java.util.List");
    _manyToManyTypes.add("java.util.Set");
    _manyToManyTypes.add("java.util.Map");

    // annotations allowed with a @OneToOne annotation
    _oneToOneAnnotations.add("javax.persistence.OneToOne");
    _oneToOneAnnotations.add("javax.persistence.JoinColumn");
    _oneToOneAnnotations.add("javax.persistence.JoinColumns");

    // annotations allowed with a @Embedded annotation
    _embeddedAnnotations.add("javax.persistence.Embedded");
    _embeddedAnnotations.add("javax.persistence.AttributeOverride");
    _embeddedAnnotations.add("javax.persistence.AttributeOverrides");

    // annotations allowed with a @EmbeddedId annotation
    _embeddedIdAnnotations.add("javax.persistence.EmbeddedId");
    _embeddedIdAnnotations.add("javax.persistence.AttributeOverride");
    _embeddedIdAnnotations.add("javax.persistence.AttributeOverrides");

    // annotations allowed for a property
    _propertyAnnotations.add("javax.persistence.Basic");
    _propertyAnnotations.add("javax.persistence.Column");
    _propertyAnnotations.add("javax.persistence.Id");
    _propertyAnnotations.add("javax.persistence.Transient");
    _propertyAnnotations.add("javax.persistence.OneToOne");
    _propertyAnnotations.add("javax.persistence.ManyToOne");
    _propertyAnnotations.add("javax.persistence.OneToMany");
    _propertyAnnotations.add("javax.persistence.ManyToMany");
    _propertyAnnotations.add("javax.persistence.JoinColumn");
    _propertyAnnotations.add("javax.persistence.Embedded");
    _propertyAnnotations.add("javax.persistence.EmbeddedId");
    _propertyAnnotations.add("javax.persistence.Version");

    // annotations allowed with a @Version annotation
    _versionAnnotations.add("javax.persistence.Version");
    _versionAnnotations.add("javax.persistence.Column");
    _versionAnnotations.add("javax.persistence.Temporal");

    // types allowed with a @Version annotation
    _versionTypes.add("short");
    _versionTypes.add("int");
    _versionTypes.add("long");
    _versionTypes.add("java.lang.Short");
    _versionTypes.add("java.lang.Integer");
    _versionTypes.add("java.lang.Long");
    _versionTypes.add("java.sql.Timestamp");
  }
}
