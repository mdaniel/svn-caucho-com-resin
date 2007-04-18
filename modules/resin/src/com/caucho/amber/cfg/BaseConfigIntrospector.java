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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import com.caucho.amber.entity.Listener;

import com.caucho.amber.field.*;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.L10N;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Base concrete introspector for orm.xml and annotations.
 */
public class BaseConfigIntrospector extends AbstractConfigIntrospector {
  private static final Logger log
    = Logger.getLogger(BaseConfigIntrospector.class.getName());
  private static final L10N L = new L10N(BaseConfigIntrospector.class);

  AmberPersistenceUnit _persistenceUnit;

  ArrayList<Completion> _linkCompletions = new ArrayList<Completion>();
  ArrayList<Completion> _depCompletions = new ArrayList<Completion>();

  HashMap<RelatedType, ArrayList<OneToOneCompletion>> _oneToOneCompletions
    = new HashMap<RelatedType, ArrayList<OneToOneCompletion>>();

  HashMap<String, EmbeddableConfig> _embeddableConfigMap
    = new HashMap<String, EmbeddableConfig>();

  HashMap<String, EntityConfig> _entityConfigMap
    = new HashMap<String, EntityConfig>();

  HashMap<String, MappedSuperclassConfig> _mappedSuperclassConfigMap
    = new HashMap<String, MappedSuperclassConfig>();

  /**
   * Creates the introspector.
   */
  public BaseConfigIntrospector()
  {
  }

  /**
   * Creates the introspector.
   */
  public BaseConfigIntrospector(AmberPersistenceUnit persistenceUnit)
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
   * Sets the mapped superclass config map.
   */
  public void setMappedSuperclassConfigMap(HashMap<String, MappedSuperclassConfig>
                                           mappedSuperclassConfigMap)
  {
    _mappedSuperclassConfigMap = mappedSuperclassConfigMap;
  }

  /**
   * Initializes the persistence unit meta data:
   * default listeners and so on.
   */
  public void initMetaData(EntityMappingsConfig entityMappings,
                           AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    PersistenceUnitMetaDataConfig metaData;
    metaData = entityMappings.getPersistenceUnitMetaData();

    if (metaData == null)
      return;

    PersistenceUnitDefaultsConfig defaults;

    defaults = metaData.getPersistenceUnitDefaults();

    if (defaults == null)
      return;

    EntityListenersConfig entityListeners;
    entityListeners = defaults.getEntityListeners();

    if (entityListeners == null)
      return;

    ArrayList<EntityListenerConfig> listeners;
    listeners = entityListeners.getEntityListeners();

    for (EntityListenerConfig listener : listeners)
      introspectDefaultListener(listener, persistenceUnit);
  }

  public void introspectDefaultListener(EntityListenerConfig listener,
                                        AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    JClassLoader loader = persistenceUnit.getJClassLoader();

    String className = listener.getClassName();

    JClass type = loader.forName(className);

    if (type == null)
      throw new ConfigException(L.l("'{0}' is an unknown type for <entity-listener> in orm.xml",
                                    className));

    ListenerType listenerType = persistenceUnit.addDefaultListener(type);

    introspectListener(type, listenerType);
  }

  public void introspectEntityListeners(JClass type,
                                        RelatedType relatedType,
                                        AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    getInternalEntityListenersConfig(type);
    JAnnotation entityListenersAnn = _annotationCfg.getAnnotation();
    EntityListenersConfig entityListenersCfg
      = _annotationCfg.getEntityListenersConfig();

    Object listeners[] = null;

    // XML mapping takes higher priority than annotations.
    if (entityListenersCfg != null)
      listeners = entityListenersCfg.getEntityListeners().toArray();
    else if (entityListenersAnn != null)
      listeners = (Object []) entityListenersAnn.get("value");
    else
      return;

    String relatedTypeName = relatedType.getBeanClass().getName();

    for (int i=0; i < listeners.length; i++) {

      JClass cl;

      // Introspects annotation or xml.
      if (listeners[i] instanceof JClass)
        cl = (JClass) listeners[i];
      else {
        JClassLoader loader = persistenceUnit.getJClassLoader();

        EntityListenerConfig listenerConfig
          = (EntityListenerConfig) listeners[i];

        String className = listenerConfig.getClassName();

        cl = loader.forName(className);

        if (cl == null)
          throw new ConfigException(L.l("'{0}' is an unknown type for <entity-listener> in orm.xml",
                                        className));
      }

      if (persistenceUnit.getDefaultListener(cl.getName()) != null)
        continue;

      introspectEntityListener(cl,
                               persistenceUnit,
                               relatedType,
                               relatedTypeName);
    }
  }

  public void introspectEntityListener(JClass type,
                                       AmberPersistenceUnit persistenceUnit,
                                       RelatedType sourceType,
                                       String sourceClassName)
    throws ConfigException
  {
    if (type == null) {
      throw new ConfigException(L.l("'{0}' is an unknown type for @EntityListeners annotated at class '{1}'",
                                    type.getName(),
                                    sourceClassName));
    }

    JClass parentClass = type.getSuperClass();

    if (parentClass == null) {
      // java.lang.Object
      return;
    }
    else {
      // XXX: entity listener super-classes in a hierarchy might
      // not be annotated as entity listeners but they might have
      // @PreXxx or @PostXxx annotated methods. On the other hand,
      // needs to filter regular classes out.

      introspectEntityListener(parentClass, persistenceUnit,
                               sourceType, sourceClassName);
    }

    // jpa/0r42

    ListenerType listenerType
      = persistenceUnit.getEntityListener(type.getName());

    ListenerType newListenerType
      = persistenceUnit.addEntityListener(sourceClassName, type);

    if (listenerType == null) {
      listenerType = newListenerType;
      introspectListener(type, listenerType);
    }

    sourceType.addListener(listenerType);
  }

  public void introspectListener(JClass type,
                                 ListenerType listenerType)
    throws ConfigException
  {
    listenerType.setInstanceClassName(listenerType.getName() + "__ResinExt");

    for (JMethod method : type.getMethods()) {
      introspectCallbacks(listenerType, method);
    }
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(JClass type,
                                  RelatedType entityType)
    throws ConfigException
  {
    getInternalExcludeDefaultListenersConfig(type);

    if (! _annotationCfg.isNull())
      entityType.setExcludeDefaultListeners(true);

    getInternalExcludeSuperclassListenersConfig(type);

    if (! _annotationCfg.isNull())
      entityType.setExcludeSuperclassListeners(true);

    for (JMethod method : type.getMethods()) {
      introspectCallbacks(entityType, method);
    }
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(AbstractEnhancedType type,
                                  JMethod method)
    throws ConfigException
  {
    JClass []param = method.getParameterTypes();

    String methodName = method.getName();
    JClass jClass = type.getBeanClass();

    boolean isListener = type instanceof ListenerType;

    int n = ListenerType.CALLBACK_CLASS.length;

    for (int i=1; i < n; i++) {
      getInternalCallbackConfig(i, jClass, method, methodName);

      if (! _annotationCfg.isNull()) {
        validateCallback(ListenerType.CALLBACK_CLASS[i].getName(),
                         method, isListener);

        type.addCallback(i, method);
      }
    }
  }

  /**
   * Introspects named queries.
   */
  void introspectNamedQueries(JClass type, String typeName)
  {
    // jpa/0y0-

    getInternalNamedQueryConfig(type);
    JAnnotation namedQueryAnn = _annotationCfg.getAnnotation();
    NamedQueryConfig namedQueryConfig = _annotationCfg.getNamedQueryConfig();

    // getInternalNamedQueriesConfig(type);
    JAnnotation namedQueriesAnn = type.getAnnotation(NamedQueries.class);
    // NamedQueriesConfig namedQueriesConfig = _annotationCfg.getNamedQueriesConfig();

    if ((namedQueryAnn == null) && (namedQueriesAnn == null))
      return;

    Object namedQueryArray[];

    if ((namedQueryAnn != null) && (namedQueriesAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @NamedQuery and @NamedQueries",
                                    typeName));
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

  /**
   * Introspects named native queries.
   */
  void introspectNamedNativeQueries(JClass type, String typeName)
  {
    // jpa/0y2-

    getInternalNamedNativeQueryConfig(type);
    JAnnotation namedNativeQueryAnn = _annotationCfg.getAnnotation();
    NamedNativeQueryConfig namedNativeQueryConfig = _annotationCfg.getNamedNativeQueryConfig();

    JAnnotation namedNativeQueriesAnn = type.getAnnotation(NamedNativeQueries.class);

    if ((namedNativeQueryAnn == null) && (namedNativeQueriesAnn == null))
      return;

    Object namedNativeQueryArray[];

    if ((namedNativeQueryAnn != null) && (namedNativeQueriesAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @NamedNativeQuery and @NamedNativeQueries",
                                    typeName));
    }
    else if (namedNativeQueriesAnn != null) {
      namedNativeQueryArray = (Object []) namedNativeQueriesAnn.get("value");
    }
    else {
      namedNativeQueryArray = new Object[] { namedNativeQueryAnn };
    }

    for (int i=0; i < namedNativeQueryArray.length; i++) {
      namedNativeQueryAnn = (JAnnotation) namedNativeQueryArray[i];

      NamedNativeQueryConfig nativeQueryConfig = new NamedNativeQueryConfig();

      nativeQueryConfig.setQuery(namedNativeQueryAnn.getString("query"));
      nativeQueryConfig.setResultClass(namedNativeQueryAnn.getClass("resultClass").getName());
      nativeQueryConfig.setResultSetMapping(namedNativeQueryAnn.getString("resultSetMapping"));

      _persistenceUnit.addNamedNativeQuery(namedNativeQueryAnn.getString("name"),
                                           nativeQueryConfig);
    }
  }

  /**
   * Introspects sql result set mappings.
   */
  void introspectSqlResultSetMappings(JClass type,
                                      RelatedType relatedType,
                                      String typeName)
  {
    // jpa/0y1-

    getInternalSqlResultSetMappingConfig(type);
    JAnnotation sqlResultSetMappingAnn = _annotationCfg.getAnnotation();
    SqlResultSetMappingConfig sqlResultSetMappingConfig
      = _annotationCfg.getSqlResultSetMappingConfig();

    JAnnotation sqlResultSetMappingsAnn = type.getAnnotation(SqlResultSetMappings.class);

    if ((sqlResultSetMappingAnn == null) && (sqlResultSetMappingsAnn == null))
      return;

    Object sqlResultSetMappingArray[];

    if ((sqlResultSetMappingAnn != null) && (sqlResultSetMappingsAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @SqlResultSetMapping and @SqlResultSetMappings",
                                    typeName));
    }
    else if (sqlResultSetMappingsAnn != null) {
      sqlResultSetMappingArray = (Object []) sqlResultSetMappingsAnn.get("value");
    }
    else {
      sqlResultSetMappingArray = new Object[] { sqlResultSetMappingAnn };
    }

    if (sqlResultSetMappingConfig != null) {
      _persistenceUnit.addSqlResultSetMapping(sqlResultSetMappingConfig.getName(),
                                              sqlResultSetMappingConfig);
      return;
    }

    for (int i=0; i < sqlResultSetMappingArray.length; i++) {
      sqlResultSetMappingAnn = (JAnnotation) sqlResultSetMappingArray[i];

      String name = sqlResultSetMappingAnn.getString("name");
      Object entities[] = (Object []) sqlResultSetMappingAnn.get("entities");
      Object columns[] = (Object []) sqlResultSetMappingAnn.get("columns");

      SqlResultSetMappingCompletion completion
        = new SqlResultSetMappingCompletion(relatedType, name,
                                            entities, columns);

      _depCompletions.add(completion);
    }
  }

  /**
   * Completion callback for sql result set mappings.
   */
  void addSqlResultSetMapping(String resultSetName,
                              Object entities[],
                              Object columns[])
    throws ConfigException
  {
    // jpa/0y1-

    SqlResultSetMappingConfig sqlResultSetMapping
      = new SqlResultSetMappingConfig();

    // Adds @EntityResult.
    for (int i=0; i < entities.length; i++) {
      JAnnotation entityResult = (JAnnotation) entities[i];

      String className = entityResult.getClass("entityClass").getName();

      EntityType resultType = _persistenceUnit.getEntity(className);

      if (resultType == null)
        throw new ConfigException(L.l("entityClass '{0}' is not an @Entity bean for @SqlResultSetMapping '{1}'. The entityClass of an @EntityResult must be an @Entity bean.",
                                      className,
                                      resultSetName));

      EntityResultConfig entityResultConfig = new EntityResultConfig();

      entityResultConfig.setEntityClass(className);

      // @FieldResult annotations.
      Object fields[] = (Object []) entityResult.get("fields");

      for (int j=0; j < fields.length; j++) {
        JAnnotation fieldResult = (JAnnotation) fields[j];

        String fieldName = fieldResult.getString("name");

        AmberField field = resultType.getField(fieldName);

        if (field == null)
          throw new ConfigException(L.l("@FieldResult with field name '{0}' is not a field for @EntityResult bean '{1}' in @SqlResultSetMapping '{2}'",
                                        fieldName,
                                        className,
                                        resultSetName));

        String columnName = fieldResult.getString("column");

        if (columnName == null || columnName.length() == 0)
          throw new ConfigException(L.l("@FieldResult must have a column name defined and it must not be empty for '{0}' in @EntityResult '{1}' @SqlResultSetMapping '{2}'",
                                        fieldName,
                                        className,
                                        resultSetName));

        FieldResultConfig fieldResultConfig = new FieldResultConfig();

        fieldResultConfig.setName(fieldName);
        fieldResultConfig.setColumn(columnName);

        entityResultConfig.addFieldResult(fieldResultConfig);
      }

      sqlResultSetMapping.addEntityResult(entityResultConfig);
    }

    // Adds @ColumnResult.
    for (int i=0; i < columns.length; i++) {
      JAnnotation columnResult = (JAnnotation) columns[i];

      String columnName = columnResult.getString("name");

      if (columnName == null || columnName.length() == 0)
        throw new ConfigException(L.l("@ColumnResult must have a column name defined and it must not be empty in @SqlResultSetMapping '{0}'",
                                      resultSetName));

      ColumnResultConfig columnResultConfig = new ColumnResultConfig();

      columnResultConfig.setName(columnName);

      sqlResultSetMapping.addColumnResult(columnResultConfig);
    }

    // Adds a global sql result set mapping to the persistence unit.
    _persistenceUnit.addSqlResultSetMapping(resultSetName,
                                            sqlResultSetMapping);
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
          completion.getRelatedType().setConfigException(e);

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
          completion.getRelatedType().setConfigException(e);

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
  void introspectInheritance(AmberPersistenceUnit persistenceUnit,
                             RelatedType entityType,
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

      getInternalPrimaryKeyJoinColumnConfig(type);
      JAnnotation joinAnn = _annotationCfg.getAnnotation();
      PrimaryKeyJoinColumnConfig primaryKeyJoinColumnConfig = _annotationCfg.getPrimaryKeyJoinColumnConfig();

      // if (subType.isJoinedSubClass()) {
      if (strategy == InheritanceType.JOINED) {
        linkInheritanceTable(subType.getRootType().getTable(),
                             subType.getTable(),
                             joinAnn,
                             primaryKeyJoinColumnConfig);

        subType.setId(new SubId(subType, subType.getRootType()));
      }

      return;
    }

    switch (strategy) {
    case JOINED:
      entityType.setJoinedSubClass(true);
      break;
    }

    getInternalDiscriminatorColumnConfig(type);
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
  void introspectIdMethod(AmberPersistenceUnit persistenceUnit,
                          RelatedType entityType,
                          RelatedType parentType,
                          JClass type,
                          JClass idClass,
                          MappedSuperclassConfig config)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    IdField idField = null;

    AttributesConfig attributesConfig = null;

    if (config != null)
      attributesConfig = config.getAttributes();

    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().getName().equals("java.lang.Object"))
        continue;

      if (! methodName.startsWith("get") || paramTypes.length != 0) {
        continue;
      }

      String fieldName = toFieldName(methodName.substring(3));

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      getInternalIdConfig(type, method, fieldName);
      JAnnotation id = _annotationCfg.getAnnotation();
      IdConfig idConfig = _annotationCfg.getIdConfig();

      if (! _annotationCfg.isNull()) {
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
        getInternalEmbeddedIdConfig(type, method, fieldName);
        JAnnotation embeddedId = _annotationCfg.getAnnotation();
        EmbeddedIdConfig embeddedIdConfig = _annotationCfg.getEmbeddedIdConfig();

        if (! _annotationCfg.isNull()) {
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
    else if (keys.size() == 1) {
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    }
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
  void introspectIdField(AmberPersistenceUnit persistenceUnit,
                         RelatedType entityType,
                         RelatedType parentType,
                         JClass type,
                         JClass idClass,
                         MappedSuperclassConfig config)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    AttributesConfig attributesConfig = null;

    if (config != null)
      attributesConfig = config.getAttributes();

    for (JField field : type.getFields()) {
      String fieldName = field.getName();

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      getInternalIdConfig(type, field, fieldName);
      JAnnotation id = _annotationCfg.getAnnotation();
      IdConfig idConfig = _annotationCfg.getIdConfig();

      if (_annotationCfg.isNull()) {
        getInternalEmbeddedIdConfig(type, field, fieldName);
        JAnnotation embeddedId = _annotationCfg.getAnnotation();
        EmbeddedIdConfig embeddedIdConfig = _annotationCfg.getEmbeddedIdConfig();

        if (_annotationCfg.isNull())
          continue;
      }

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
  boolean isField(JClass type,
                  AbstractEnhancedConfig typeConfig,
                  boolean isEmbeddable)
    throws ConfigException
  {
    if (type == null)
      return false;

    if (isEmbeddable) {

      for (JMethod method : type.getDeclaredMethods()) {

        JAnnotation ann[] = method.getDeclaredAnnotations();

        if ((ann != null) && (ann.length > 0))
          return false;
      }

      // jpa/0gh0
      for (JField field : type.getDeclaredFields()) {

        if ((! field.isTransient()) && field.isPrivate())
          return false;
      }

      return true;
    }

    if (typeConfig != null) {

      String access = typeConfig.getAccess();

      if (access != null)
        return access.equals("FIELD");

      JClass parentClass = type.getSuperClass();

      if (parentClass == null)
        return false;
      else {
        getInternalEntityConfig(parentClass);
        EntityConfig superEntityConfig = _annotationCfg.getEntityConfig();

        if (superEntityConfig == null)
          return false;

        return isField(parentClass, superEntityConfig, false);
      }
    }

    for (JField field : type.getDeclaredFields()) {

      JAnnotation id = field.getAnnotation(javax.persistence.Id.class);

      if (id != null)
        return true;

      id = field.getAnnotation(EmbeddedId.class);

      if (id != null)
        return true;
    }

    return isField(type.getSuperClass(), null, false);
  }

  private IdField introspectId(AmberPersistenceUnit persistenceUnit,
                               RelatedType entityType,
                               JAccessibleObject field,
                               String fieldName,
                               JClass fieldType,
                               IdConfig idConfig)
    throws ConfigException, SQLException
  {
    JAnnotation id = field.getAnnotation(javax.persistence.Id.class);
    JAnnotation column = field.getAnnotation(javax.persistence.Column.class);

    ColumnConfig columnConfig = null;
    GeneratedValueConfig generatedValueConfig = null;

    if (idConfig != null) {
      columnConfig = idConfig.getColumn();
      generatedValueConfig = idConfig.getGeneratedValue();
    }

    JAnnotation gen = field.getAnnotation(GeneratedValue.class);

    Type amberType = persistenceUnit.createType(fieldType);

    KeyPropertyField idField;

    Column keyColumn = null;

    if (entityType.getTable() != null) {
      keyColumn = createColumn(entityType,
                               field,
                               fieldName,
                               column,
                               amberType,
                               columnConfig);

      idField = new KeyPropertyField(entityType, fieldName, keyColumn);
    }
    else {
      idField = new KeyPropertyField(entityType, fieldName);
      return idField;
    }

    if (gen == null) {
    }
    else {
      JdbcMetaData metaData = null;

      try {
        metaData = persistenceUnit.getMetaData();
      } catch (Exception e) {
        throw new ConfigException(L.l("Unable to get meta data for database. Meta data is needed for generated values."));
      }

      if (GenerationType.IDENTITY.equals(gen.get("strategy"))) {
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
        else {
          addTableIdGenerator(persistenceUnit, idField, id);
        }
      }
    }

    return idField;
  }

  private IdField introspectEmbeddedId(AmberPersistenceUnit persistenceUnit,
                                       RelatedType entityType,
                                       JAccessibleObject field,
                                       String fieldName,
                                       JClass fieldType)
    throws ConfigException, SQLException
  {
    IdField idField;

    idField = new EmbeddedIdField(entityType, fieldName);

    return idField;
  }

  void addSequenceIdGenerator(AmberPersistenceUnit persistenceUnit,
                              KeyPropertyField idField,
                              JAnnotation genAnn)
    throws ConfigException
  {
    idField.setGenerator("sequence");
    idField.getColumn().setGeneratorType("sequence");

    String name = genAnn.getString("generator");

    if (name == null || "".equals(name))
      name = idField.getEntitySourceType().getTable().getName() + "_cseq";

    IdGenerator gen = persistenceUnit.createSequenceGenerator(name, 1);

    idField.getEntitySourceType().setGenerator(idField.getName(), gen);
  }

  void addTableIdGenerator(AmberPersistenceUnit persistenceUnit,
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

      // jpa/0g60
      genTable.init();

      persistenceUnit.putTableGenerator(name, gen);
    }

    idField.getEntitySourceType().setGenerator(idField.getName(), gen);
  }

  /**
   * Links a secondary table.
   */
  void linkSecondaryTable(Table primaryTable,
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
  void linkInheritanceTable(Table primaryTable,
                            Table secondaryTable,
                            JAnnotation joinAnn,
                            PrimaryKeyJoinColumnConfig pkJoinColumnCfg)
    throws ConfigException
  {
    JAnnotation joinAnns[] = null;

    if (joinAnn != null)
      joinAnns = new JAnnotation[] { joinAnn };

    linkInheritanceTable(primaryTable,
                         secondaryTable,
                         joinAnns,
                         pkJoinColumnCfg);
  }

  /**
   * Links a secondary table.
   */
  void linkInheritanceTable(Table primaryTable,
                            Table secondaryTable,
                            JAnnotation []joinColumnsAnn,
                            PrimaryKeyJoinColumnConfig pkJoinColumnCfg)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (Column column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      String name;

      if (joinColumnsAnn == null) {

        if (pkJoinColumnCfg == null)
          name = column.getName();
        else
          name = pkJoinColumnCfg.getName();
      }
      else {
        JAnnotation join;

        join = getJoinColumn(joinColumnsAnn, column.getName());

        if (join == null)
          name = column.getName();
        else
          name = join.getString("name");
      }

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

    // jpa/0l48
    //    link = new LinkColumns(primaryTable,
    //                           secondaryTable,
    //                           linkColumns);
    //
    //    link.setSourceCascadeDelete(true);
    //
    //    primaryTable.setDependentIdLink(link);
  }

  /**
   * Introspects the methods.
   */
  void introspectMethods(AmberPersistenceUnit persistenceUnit,
                         AbstractStatefulType entityType,
                         AbstractStatefulType parentType,
                         JClass type,
                         AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().getName().equals("java.lang.Object"))
        continue;

      // jpa/0r38
      // Callbacks are introspected in the main introspect() block.
      // introspectCallbacks(entityType, method);

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

      getInternalVersionConfig(type, method, propName);
      JAnnotation versionAnn = _annotationCfg.getAnnotation();
      VersionConfig versionConfig = _annotationCfg.getVersionConfig();

      if (! _annotationCfg.isNull()) {
        validateNonGetter(method);
      }
      else {

        JMethod setter = type.getMethod("set" + propName,
                                        new JClass[] { method.getReturnType() });
        if (method.isPrivate() ||
            (setter == null) || setter.isPrivate()) {

          JAnnotation ann = isAnnotatedMethod(method);

          if (ann == null) {
            if (setter != null)
              ann = isAnnotatedMethod(setter);
          }
          else if (ann.getType().equals("javax.persistence.Transient"))
            continue;

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

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      JClass fieldType = method.getReturnType();

      introspectField(persistenceUnit, entityType, method,
                      fieldName, fieldType, typeConfig);
    }
  }

  /**
   * Introspects the fields.
   */
  void introspectFields(AmberPersistenceUnit persistenceUnit,
                        AbstractStatefulType entityType,
                        AbstractStatefulType parentType,
                        JClass type,
                        AbstractEnhancedConfig typeConfig,
                        boolean isEmbeddable)
    throws ConfigException
  {
    if (! isEmbeddable)
      if (((RelatedType) entityType).getId() == null)
        throw new IllegalStateException(L.l("{0} has no key", entityType));

    for (JField field : type.getFields()) {
      String fieldName = field.getName();

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      if (field.isStatic() || field.isTransient())
        continue;

      JClass fieldType = field.getType();

      introspectField(persistenceUnit, entityType, field,
                      fieldName, fieldType, typeConfig);
    }
  }

  void introspectField(AmberPersistenceUnit persistenceUnit,
                       AbstractStatefulType sourceType,
                       JAccessibleObject field,
                       String fieldName,
                       JClass fieldType,
                       AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    EmbeddableConfig embeddableConfig = null;
    MappedSuperclassConfig mappedSuperOrEntityConfig = null;

    if (typeConfig instanceof EmbeddableConfig)
      embeddableConfig = (EmbeddableConfig) typeConfig;
    else if (typeConfig instanceof MappedSuperclassConfig)
      mappedSuperOrEntityConfig = (MappedSuperclassConfig) typeConfig;

    // jpa/0r37: interface fields must not be considered.

    JClass jClass = field.getDeclaringClass();

    if (jClass.isInterface())
      return;

    // jpa/0r37: fields declared in non-entity superclasses
    // must not be considered.

    AbstractStatefulType declaringType;

    declaringType = _persistenceUnit.getEntity(jClass.getName());

    if (declaringType == null)
      declaringType = _persistenceUnit.getEmbeddable(jClass.getName());

    if (declaringType == null)
      declaringType = _persistenceUnit.getMappedSuperclass(jClass.getName());

    if (declaringType == null)
      return;

    AttributesConfig attributesConfig = null;
    IdConfig idConfig = null;
    BasicConfig basicConfig = null;
    OneToOneConfig oneToOneConfig = null;
    OneToManyConfig oneToManyConfig = null;
    ManyToOneConfig manyToOneConfig = null;
    ManyToManyConfig manyToManyConfig = null;
    VersionConfig versionConfig = null;

    if (mappedSuperOrEntityConfig != null) {
      attributesConfig = mappedSuperOrEntityConfig.getAttributes();

      if (attributesConfig != null) {
        idConfig = attributesConfig.getId(fieldName);

        basicConfig = attributesConfig.getBasic(fieldName);

        oneToOneConfig = attributesConfig.getOneToOne(fieldName);

        oneToManyConfig = attributesConfig.getOneToMany(fieldName);

        manyToOneConfig = attributesConfig.getManyToOne(fieldName);

        manyToManyConfig = attributesConfig.getManyToMany(fieldName);

        versionConfig = attributesConfig.getVersion(fieldName);
      }
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
    else if ((versionConfig != null) ||
             field.isAnnotationPresent(javax.persistence.Version.class)) {
      validateAnnotations(field, _versionAnnotations);

      addVersion((RelatedType) sourceType, field,
                 fieldName, fieldType, versionConfig);
    }
    else if ((manyToOneConfig != null) ||
             field.isAnnotationPresent(javax.persistence.ManyToOne.class)) {
      validateAnnotations(field, _manyToOneAnnotations);

      JAnnotation ann = field.getAnnotation(ManyToOne.class);

      JClass targetEntity = null;

      if (ann != null)
        targetEntity = ann.getClass("targetEntity");
      else {

        String s = manyToOneConfig.getTargetEntity();

        if ((s != null) && (s.length() > 0))
          targetEntity = _persistenceUnit.getJClassLoader().forName(s);
      }

      if (targetEntity == null ||
          targetEntity.getName().equals("void")) {
        targetEntity = fieldType;
      }

      getInternalEntityConfig(targetEntity);
      JAnnotation targetEntityAnn = _annotationCfg.getAnnotation();
      EntityConfig targetEntityConfig = _annotationCfg.getEntityConfig();

      if (_annotationCfg.isNull()) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne relations must target a valid @Entity.",
                               targetEntity.getName(), field.getName()));
      }

      if (! fieldType.isAssignableFrom(targetEntity)) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne targetEntity must be assignable to the field type '{2}'.",
                               targetEntity.getName(),
                               field.getName(),
                               fieldType.getName()));
      }

      RelatedType relatedType = (RelatedType) sourceType;

      relatedType.setHasDependent(true);

      _linkCompletions.add(new ManyToOneCompletion(relatedType,
                                                   field,
                                                   fieldName,
                                                   fieldType));
    }
    else if ((oneToManyConfig != null) ||
             field.isAnnotationPresent(javax.persistence.OneToMany.class)) {
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

      RelatedType relatedType = (RelatedType) sourceType;

      _depCompletions.add(new OneToManyCompletion(relatedType,
                                                  field,
                                                  fieldName,
                                                  fieldType,
                                                  oneToManyConfig));
    }
    else if ((oneToOneConfig != null) ||
             field.isAnnotationPresent(javax.persistence.OneToOne.class)) {
      validateAnnotations(field, _oneToOneAnnotations);

      RelatedType relatedType = (RelatedType) sourceType;

      OneToOneCompletion oneToOne = new OneToOneCompletion(relatedType,
                                                           field,
                                                           fieldName,
                                                           fieldType);

      // jpa/0o03 and jpa/0o06 (check also jpa/0o07 with no mappedBy at all)
      // @OneToOne with mappedBy should be completed first
      String mappedBy;

      if (oneToOneConfig != null)
        mappedBy = oneToOneConfig.getMappedBy();
      else {
        JAnnotation oneToOneAnn = field.getAnnotation(OneToOne.class);
        mappedBy = oneToOneAnn.getString("mappedBy");
      }

      boolean isOwner = (mappedBy == null || mappedBy.equals(""));

      if (isOwner)
        _depCompletions.add(oneToOne);
      else {
        _depCompletions.add(0, oneToOne);
        relatedType.setHasDependent(true);
      }

      ArrayList<OneToOneCompletion> oneToOneList
        = _oneToOneCompletions.get(relatedType);

      if (oneToOneList == null) {
        oneToOneList = new ArrayList<OneToOneCompletion>();
        _oneToOneCompletions.put(relatedType, oneToOneList);
      }

      oneToOneList.add(oneToOne);
    }
    else if ((manyToManyConfig != null) ||
             field.isAnnotationPresent(javax.persistence.ManyToMany.class)) {

      if (field.isAnnotationPresent(javax.persistence.MapKey.class)) {
        if (! fieldType.getName().equals("java.util.Map")) {
          throw error(field, L.l("'{0}' is an illegal @ManyToMany/@MapKey type for {1}. @MapKey must be a java.util.Map",
                                 fieldType.getName(),
                                 field.getName()));
        }
      }

      RelatedType relatedType = (RelatedType) sourceType;

      Completion completion = new ManyToManyCompletion(relatedType,
                                                       field,
                                                       fieldName,
                                                       fieldType);

      JAnnotation ann = field.getAnnotation(ManyToMany.class);

      String mappedBy;

      if (ann != null)
        mappedBy = ann.getString("mappedBy");
      else
        mappedBy = manyToManyConfig.getMappedBy();

      if ("".equals(mappedBy))
        _linkCompletions.add(completion);
      else
        _depCompletions.add(completion);
    }
    else if (field.isAnnotationPresent(javax.persistence.Embedded.class)) {
      validateAnnotations(field, _embeddedAnnotations);

      RelatedType relatedType = (RelatedType) sourceType;

      relatedType.setHasDependent(true);

      _depCompletions.add(new EmbeddedCompletion(relatedType,
                                                 field,
                                                 fieldName,
                                                 fieldType,
                                                 false));
    }
    else if (field.isAnnotationPresent(javax.persistence.EmbeddedId.class)) {
      validateAnnotations(field, _embeddedIdAnnotations);

      _depCompletions.add(new EmbeddedCompletion((RelatedType) sourceType,
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

  void addBasic(AbstractStatefulType sourceType,
                JAccessibleObject field,
                String fieldName,
                JClass fieldType,
                BasicConfig basicConfig)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation basicAnn = field.getAnnotation(Basic.class);
    JAnnotation columnAnn = field.getAnnotation(javax.persistence.Column.class);
    JAnnotation enumeratedAnn = field.getAnnotation(Enumerated.class);

    ColumnConfig columnConfig = null;

    if (basicConfig != null)
      columnConfig = basicConfig.getColumn();

    if (_basicTypes.contains(fieldType.getName())) {
    }
    else if (fieldType.isAssignableTo(java.io.Serializable.class)) {
    }
    else
      throw error(field, L.l("{0} is an invalid @Basic type for {1}.",
                             fieldType.getName(), field.getName()));

    Type amberType;

    if (enumeratedAnn == null)
      amberType = persistenceUnit.createType(fieldType);
    else {
      com.caucho.amber.type.EnumType enumType;

      enumType = persistenceUnit.createEnum(fieldType.getName(),
                                            fieldType);

      enumType.setOrdinal(enumeratedAnn.get("value") ==
                          javax.persistence.EnumType.ORDINAL);

      amberType = enumType;
    }

    Column fieldColumn = null;

    if (sourceType instanceof RelatedType)
      fieldColumn = createColumn((RelatedType) sourceType, field, fieldName,
                                 columnAnn, amberType, columnConfig);

    PropertyField property = new PropertyField(sourceType, fieldName);
    property.setColumn(fieldColumn);

    // jpa/0w24
    property.setType(amberType);

    if (basicAnn != null)
      property.setLazy(basicAnn.get("fetch") == FetchType.LAZY);
    else if (basicConfig != null)
      property.setLazy(basicConfig.getFetch() == FetchType.LAZY);
    else
      property.setLazy(false);

    /*
      field.setInsertable(insertable);
      field.setUpdateable(updateable);
    */

    sourceType.addField(property);
  }

  void addVersion(RelatedType sourceType,
                  JAccessibleObject field,
                  String fieldName,
                  JClass fieldType,
                  VersionConfig versionConfig)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation columnAnn = field.getAnnotation(javax.persistence.Column.class);

    ColumnConfig columnConfig = null;

    if (versionConfig != null)
      columnConfig = versionConfig.getColumn();

    if (! _versionTypes.contains(fieldType.getName())) {
      throw error(field, L.l("{0} is an invalid @Version type for {1}.",
                             fieldType.getName(), field.getName()));
    }

    Type amberType = persistenceUnit.createType(fieldType);

    Column fieldColumn = createColumn(sourceType, field, fieldName,
                                      columnAnn, amberType, columnConfig);

    VersionField version = new VersionField(sourceType, fieldName);
    version.setColumn(fieldColumn);

    sourceType.setVersionField(version);
  }

  private Column createColumn(RelatedType entityType,
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
    else if (entityType.getTable() == null) // jpa/0ge2: MappedSuperclassType
      column = new Column(null, name, amberType);
    else
      column = entityType.getTable().createColumn(name, amberType);

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

  void addManyToOne(RelatedType sourceType,
                    JAccessibleObject field,
                    String fieldName,
                    JClass fieldType)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    getInternalManyToOneConfig(sourceType.getBeanClass(), field, fieldName);
    JAnnotation manyToOneAnn = _annotationCfg.getAnnotation();
    Object manyToOneConfig = _annotationCfg.getManyToOneConfig();
    HashMap<String, JoinColumnConfig> joinColumnMap = null;
    CascadeType cascadeTypes[] = null;

    JClass parentClass = sourceType.getBeanClass();

    do {
      getInternalEntityConfig(parentClass);
      JAnnotation parentEntity = _annotationCfg.getAnnotation();
      EntityConfig superEntityConfig = _annotationCfg.getEntityConfig();

      if (superEntityConfig != null) {
        AttributesConfig attributesConfig = superEntityConfig.getAttributes();

        if (attributesConfig != null) {
          if (manyToOneConfig == null)
            manyToOneConfig = attributesConfig.getManyToOne(fieldName);
        }
      }

      parentClass = parentClass.getSuperClass();
    }
    while ((parentClass != null) && (manyToOneConfig == null));

    FetchType fetchType = FetchType.EAGER;
    JClass targetClass = null;

    if ((manyToOneAnn == null) && (manyToOneConfig == null)) {
      // jpa/0o03

      getInternalOneToOneConfig(sourceType.getBeanClass(), field, fieldName);
      manyToOneAnn = _annotationCfg.getAnnotation();
      manyToOneConfig = _annotationCfg.getOneToOneConfig();

      if (manyToOneConfig != null) {
        fetchType = ((OneToOneConfig) manyToOneConfig).getFetch();
        joinColumnMap = ((OneToOneConfig) manyToOneConfig).getJoinColumnMap();

        String s = ((OneToOneConfig) manyToOneConfig).getTargetEntity();
        if (s != null)
          targetClass = _persistenceUnit.getJClassLoader().forName(s);
      }
    }
    else {
      if (manyToOneConfig != null) {
        fetchType = ((ManyToOneConfig) manyToOneConfig).getFetch();
        joinColumnMap = ((ManyToOneConfig) manyToOneConfig).getJoinColumnMap();
        CascadeConfig cascade = ((ManyToOneConfig) manyToOneConfig).getCascade();

        if (cascade != null) {
          cascadeTypes = cascade.getCascadeTypes();
        }

        String s = ((ManyToOneConfig) manyToOneConfig).getTargetEntity();
        if (s != null)
          targetClass = _persistenceUnit.getJClassLoader().forName(s);
      }
    }

    if (manyToOneAnn != null) {
      fetchType = (FetchType) manyToOneAnn.get("fetch");

      targetClass = manyToOneAnn.getClass("targetEntity");

      // XXX: runtime does not cast this
      // cascadeType = (CascadeType []) manyToOneAnn.get("cascade");
      Object cascade[] = (Object []) manyToOneAnn.get("cascade");

      cascadeTypes = new CascadeType[cascade.length];

      for (int i=0; i < cascade.length; i++)
        cascadeTypes[i] = (CascadeType) cascade[i];
    }

    if (fetchType == FetchType.EAGER) {
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

    String targetName = "";
    if (targetClass != null)
      targetName = targetClass.getName();

    if (targetName.equals("") || targetName.equals("void"))
      targetName = fieldType.getName();

    EntityManyToOneField manyToOneField;
    manyToOneField = new EntityManyToOneField(sourceType, fieldName, cascadeTypes);

    EntityType targetType = persistenceUnit.createEntity(targetName, fieldType);

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(fetchType == FetchType.LAZY);

    manyToOneField.setJoinColumns(joinColumnsAnn);
    manyToOneField.setJoinColumnMap(joinColumnMap);

    sourceType.addField(manyToOneField);

    // jpa/0ge3
    if (sourceType instanceof MappedSuperclassType)
      return;

    validateJoinColumns(field, joinColumnsAnn, joinColumnMap, targetType);

    manyToOneField.init();
  }

  public static JAnnotation getJoinColumn(JAnnotation joinColumns,
                                          String keyName)
  {
    if (joinColumns == null)
      return null;

    return getJoinColumn((Object []) joinColumns.get("value"), keyName);
  }

  void validateJoinColumns(JAccessibleObject field,
                           Object []columnsAnn,
                           HashMap<String, JoinColumnConfig> joinColumnMap,
                           RelatedType targetType)
    throws ConfigException
  {
    if ((joinColumnMap == null) && (columnsAnn == null))
      return;

    com.caucho.amber.field.Id id = targetType.getId();

    RelatedType parentType = targetType;

    int idCols;

    // XXX: jpa/0l48
    while ((idCols = id.getColumns().size()) == 0) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      id = parentType.getId();
    }

    int size;
    Object joinColumnCfg[] = null;

    if (columnsAnn != null)
      size = columnsAnn.length;
    else {
      size = joinColumnMap.size();
      joinColumnCfg = joinColumnMap.values().toArray();
    }

    if (idCols != size) {
      throw error(field, L.l("Number of @JoinColumns for '{1}' ({0}) does not match the number of primary key columns for '{3}' ({2}).",
                             "" + size,
                             field.getName(),
                             idCols,
                             targetType.getName()));
    }

    for (int i = 0; i < size; i++) {
      String ref;

      if (joinColumnCfg != null) {
        ref = ((JoinColumnConfig) joinColumnCfg[i]).getReferencedColumnName();
      }
      else {
        JAnnotation ann = (JAnnotation) columnsAnn[i];

        ref = ann.getString("referencedColumnName");
      }

      if (((ref == null) || ref.equals("")) && size > 1)
        throw error(field, L.l("referencedColumnName is required when more than one @JoinColumn is specified."));

      Column column = findColumn(id.getColumns(), ref);

      if (column == null)
        throw error(field, L.l("referencedColumnName '{0}' does not match any key column in '{1}'.",
                               ref, targetType.getName()));
    }
  }

  private Column findColumn(ArrayList<Column> columns, String ref)
  {
    if (((ref == null) || ref.equals("")) && columns.size() == 1)
      return columns.get(0);

    for (Column column : columns) {
      if (column.getName().equals(ref))
        return column;
    }

    return null;
  }

  public static JAnnotation getJoinColumn(Object []columnsAnn,
                                          String keyName)
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

  void addManyToMany(RelatedType sourceType,
                     JAccessibleObject field,
                     String fieldName,
                     JClass fieldType)
    throws ConfigException
  {
    getInternalManyToManyConfig(sourceType.getBeanClass(), field, fieldName);
    JAnnotation manyToManyAnn = _annotationCfg.getAnnotation();
    ManyToManyConfig manyToManyConfig = _annotationCfg.getManyToManyConfig();

    JType retType;

    if (field instanceof JField)
      retType = ((JField) field).getGenericType();
    else
      retType = ((JMethod) field).getGenericReturnType();

    JType []typeArgs = retType.getActualTypeArguments();

    String targetName = "";

    if (manyToManyAnn != null) {
      JClass targetEntity = manyToManyAnn.getClass("targetEntity");

      if (targetEntity != null)
        targetName = targetEntity.getName();
    }
    else
      targetName = manyToManyConfig.getTargetEntity();

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

    // XXX: introspect cascade types
    CascadeType[] cascadeTypes = null;

    String mappedBy;

    if (manyToManyAnn != null) {
      mappedBy = manyToManyAnn.getString("mappedBy");

      // XXX: runtime does not cast this
      // cascadeType = (CascadeType []) manyToManyAnn.get("cascade");
      Object cascade[] = (Object []) manyToManyAnn.get("cascade");

      cascadeTypes = new CascadeType[cascade.length];

      for (int i=0; i < cascade.length; i++)
        cascadeTypes[i] = (CascadeType) cascade[i];
    }
    else {
      mappedBy = manyToManyConfig.getMappedBy();

      CascadeConfig cascade = ((ManyToManyConfig) manyToManyConfig).getCascade();

      if (cascade != null) {
        cascadeTypes = cascade.getCascadeTypes();
      }
    }

    if (! ((mappedBy == null) || "".equals(mappedBy))) {
      EntityManyToManyField sourceField
        = (EntityManyToManyField) targetType.getField(mappedBy);

      EntityManyToManyField manyToManyField;

      if (sourceField == null)
        throw error(field,
                    L.l("Unable to find the associated field in '{0}' for a @ManyToMany relationship from '{1}'",
                        targetName,
                        field.getName()));

      manyToManyField = new EntityManyToManyField(sourceType,
                                                  fieldName,
                                                  sourceField,
                                                  cascadeTypes);
      manyToManyField.setType(targetType);
      sourceType.addField(manyToManyField);

      // jpa/0i5-
      // Update column names for bidirectional many-to-many

      if (! sourceField.hasJoinColumns()) {
        LinkColumns sourceLink = sourceField.getSourceLink();
        ArrayList<ForeignColumn> columns = sourceLink.getColumns();
        for (ForeignColumn column : columns) {
          String columnName = column.getName();
          columnName = columnName.substring(columnName.indexOf('_'));
          columnName = manyToManyField.getName().toUpperCase() + columnName;
          column.setName(columnName);
        }
      }

      if (! sourceField.hasInverseJoinColumns()) {
        LinkColumns targetLink = sourceField.getTargetLink();
        ArrayList<ForeignColumn> columns = targetLink.getColumns();
        for (ForeignColumn column : columns) {
          String columnName = column.getName();
          columnName = columnName.substring(columnName.indexOf('_'));
          columnName = sourceField.getName().toUpperCase() + columnName;
          column.setName(columnName);
        }
      }

      return;
    }

    EntityManyToManyField manyToManyField;

    manyToManyField = new EntityManyToManyField(sourceType, fieldName, cascadeTypes);
    manyToManyField.setType(targetType);

    String sqlTable = sourceType.getTable().getName() + "_" +
      targetType.getTable().getName();

    JAnnotation joinTableAnn = field.getAnnotation(javax.persistence.JoinTable.class);

    JoinTableConfig joinTableConfig = null;

    if (manyToManyConfig != null)
      joinTableConfig = manyToManyConfig.getJoinTable();

    Table mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;

    if ((joinTableAnn != null) || (joinTableConfig != null)) {

      Object joinColumns[] = null;
      Object inverseJoinColumns[] = null;

      HashMap<String, JoinColumnConfig> joinColumnsConfig = null;
      HashMap<String, JoinColumnConfig> inverseJoinColumnsConfig = null;

      String joinTableName;

      if (joinTableAnn != null) {
        joinTableName = joinTableAnn.getString("name");
        joinColumns = (Object []) joinTableAnn.get("joinColumns");
        inverseJoinColumns = (Object []) joinTableAnn.get("inverseJoinColumns");

        if ((joinColumns != null) &&
            (joinColumns.length > 0))
          manyToManyField.setJoinColumns(true);

        if ((inverseJoinColumns != null) &&
            (inverseJoinColumns.length > 0))
          manyToManyField.setInverseJoinColumns(true);
      }
      else {
        joinTableName = joinTableConfig.getName();
        joinColumnsConfig = joinTableConfig.getJoinColumnMap();
        inverseJoinColumnsConfig = joinTableConfig.getInverseJoinColumnMap();

        if ((joinColumnsConfig != null) &&
            (joinColumnsConfig.size() > 0))
          manyToManyField.setJoinColumns(true);

        if ((inverseJoinColumnsConfig != null) &&
            (inverseJoinColumnsConfig.size() > 0))
          manyToManyField.setInverseJoinColumns(true);
      }

      if (! joinTableName.equals(""))
        sqlTable = joinTableName;

      mapTable = _persistenceUnit.createTable(sqlTable);

      sourceColumns = AbstractConfigIntrospector.calculateColumns(field,
                                                                  mapTable,
                                                                  sourceType.getTable().getName() + "_",
                                                                  sourceType,
                                                                  joinColumns,
                                                                  joinColumnsConfig);

      targetColumns = AbstractConfigIntrospector.calculateColumns(field,
                                                                  mapTable,
                                                                  targetType.getTable().getName() + "_",
                                                                  targetType,
                                                                  inverseJoinColumns,
                                                                  inverseJoinColumnsConfig);
    }
    else {
      mapTable = _persistenceUnit.createTable(sqlTable);

      sourceColumns = AbstractConfigIntrospector.calculateColumns(mapTable,
                                                                  sourceType.getTable().getName() + "_",
                                                                  sourceType);

      targetColumns = AbstractConfigIntrospector.calculateColumns(mapTable,
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

    getInternalMapKeyConfig(sourceType.getBeanClass(), field, fieldName);
    JAnnotation mapKeyAnn = _annotationCfg.getAnnotation();
    MapKeyConfig mapKeyConfig = _annotationCfg.getMapKeyConfig();

    if (! _annotationCfg.isNull()) {

      String key;

      if (mapKeyAnn != null)
        key = mapKeyAnn.getString("name");
      else
        key = mapKeyConfig.getName();

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

  EntityManyToOneField getSourceField(RelatedType targetType,
                                      String mappedBy,
                                      RelatedType sourceType)
  {
    do {
      ArrayList<AmberField> fields = targetType.getFields();

      for (AmberField field : fields) {
        // jpa/0o07: there is no mappedBy at all on any sides.
        if ("".equals(mappedBy) || mappedBy == null) {
          if (field.getJavaType().isAssignableFrom(sourceType.getBeanClass()))
            return (EntityManyToOneField) field;
        }
        else if (field.getName().equals(mappedBy))
          return (EntityManyToOneField) field;
      }

      // jpa/0ge4
      targetType = targetType.getParentType();
    }
    while (targetType != null);

    return null;
  }

  OneToOneCompletion getSourceCompletion(RelatedType targetType,
                                         String mappedBy)
  {
    do {
      ArrayList<OneToOneCompletion> sourceCompletions
        = _oneToOneCompletions.get(targetType);

      if (sourceCompletions == null) {
      } // jpa/0o07
      else if (sourceCompletions.size() == 1)
        return sourceCompletions.get(0);
      else {
        for (OneToOneCompletion oneToOne : sourceCompletions) {
          if (oneToOne.getFieldName().equals(mappedBy)) {
            return oneToOne;
          }
        }
      }

      // jpa/0ge4
      targetType = targetType.getParentType();
    }
    while (targetType != null);

    return null;
  }

  /**
   * completes for dependent
   */
  class Completion {
    protected RelatedType _relatedType;

    protected Completion(RelatedType relatedType,
                         String fieldName)
    {
      _relatedType = relatedType;
      _relatedType.addCompletionField(fieldName);
    }

    protected Completion(RelatedType relatedType)
    {
      _relatedType = relatedType;
    }

    RelatedType getRelatedType()
    {
      return _relatedType;
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
    private OneToManyConfig _oneToManyConfig;

    OneToManyCompletion(RelatedType type,
                        JAccessibleObject field,
                        String fieldName,
                        JClass fieldType,
                        OneToManyConfig oneToManyConfig)
    {
      super(type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
      _oneToManyConfig = oneToManyConfig;
    }

    void complete()
      throws ConfigException
    {
      getInternalOneToManyConfig(_relatedType.getBeanClass(), _field, _fieldName);
      JAnnotation oneToManyAnn = _annotationCfg.getAnnotation();
      OneToManyConfig oneToManyConfig = _annotationCfg.getOneToManyConfig();

      AmberPersistenceUnit persistenceUnit = _relatedType.getPersistenceUnit();

      JType retType;

      if (_field instanceof JField)
        retType = ((JField) _field).getGenericType();
      else
        retType = ((JMethod) _field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      JClass targetEntity = null;

      if (oneToManyAnn != null)
        targetEntity = oneToManyAnn.getClass("targetEntity");
      else {
        String s = oneToManyConfig.getTargetEntity();

        if (s != null)
          targetEntity = _persistenceUnit.getJClassLoader().forName(s);
      }

      String targetName = "";

      if (targetEntity != null)
        targetName = targetEntity.getName();

      if (! targetName.equals("") && ! targetName.equals("void")) {
      }
      else if (typeArgs.length > 0)
        targetName = typeArgs[typeArgs.length-1].getName();
      else
        throw error(_field, L.l("Can't determine targetEntity for {0}.  @OneToMany properties must target @Entity beans.",
                                _field.getName()));

      EntityType targetType = persistenceUnit.getEntity(targetName);
      if (targetType == null) {

        EntityConfig entityConfig = null;

        if (_entityConfigMap != null) {
          entityConfig = _entityConfigMap.get(targetName);
        }

        if (entityConfig == null)
          throw error(_field,
                      L.l("targetEntity '{0}' is not an @Entity bean for {1}.  The targetEntity of a @OneToMany collection must be an @Entity bean.",
                          targetName,
                          _field.getName()));
      }

      CascadeType[] cascadeTypes = null;

      String mappedBy;

      if (oneToManyAnn != null) {
        mappedBy = oneToManyAnn.getString("mappedBy");

        // XXX: runtime does not cast this
        // cascadeType = (CascadeType []) oneToManyAnn.get("cascade");
        Object cascade[] = (Object []) oneToManyAnn.get("cascade");

        cascadeTypes = new CascadeType[cascade.length];

        for (int i=0; i < cascade.length; i++)
          cascadeTypes[i] = (CascadeType) cascade[i];
      }
      else {
        mappedBy = oneToManyConfig.getMappedBy();

        CascadeConfig cascade = ((OneToManyConfig) oneToManyConfig).getCascade();

        if (cascade != null) {
          cascadeTypes = cascade.getCascadeTypes();
        }
      }

      if (mappedBy != null && ! mappedBy.equals("")) {
        oneToManyBidirectional(targetType, targetName, mappedBy, cascadeTypes);
      }
      else {
        oneToManyUnidirectional(targetType, targetName, cascadeTypes);
      }
    }

    private void oneToManyBidirectional(RelatedType targetType,
                                        String targetName,
                                        String mappedBy,
                                        CascadeType[] cascadeTypes)
      throws ConfigException
    {
      JAnnotation joinTableAnn = _field.getAnnotation(javax.persistence.JoinTable.class);

      JoinTableConfig joinTableConfig = null;

      if (_oneToManyConfig != null)
        joinTableConfig = _oneToManyConfig.getJoinTable();

      if ((joinTableAnn != null) || (joinTableConfig != null)) {
        throw error(_field,
                    L.l("Bidirectional @ManyToOne property {0} may not have a @JoinTable annotation.",
                        _field.getName()));
      }

      EntityManyToOneField sourceField = getSourceField(targetType,
                                                        mappedBy,
                                                        null);

      if (sourceField == null)
        throw error(_field, L.l("'{0}' does not have matching field for @ManyToOne(mappedBy={1}).",
                                targetType.getName(),
                                mappedBy));

      JAnnotation orderByAnn = _field.getAnnotation(javax.persistence.OrderBy.class);

      String orderBy = null;
      ArrayList<String> orderByFields = null;
      ArrayList<Boolean> orderByAscending = null;

      if (orderByAnn != null) {
        orderBy = (String) orderByAnn.get("value");

        if (orderBy == null)
          orderBy = "";

        if ("".equals(orderBy)) {
          if (targetType instanceof RelatedType) {
            RelatedType targetRelatedType = (RelatedType) targetType;
            orderBy = targetRelatedType.getId().generateJavaSelect(null);
          }
        }

        orderByFields = new ArrayList<String>();
        orderByAscending = new ArrayList<Boolean>();

        int len = orderBy.length();

        int i = 0;

        while (i < len) {

          int index = orderBy.indexOf(",", i);

          if (index < 0)
            index = len;

          String orderByField = orderBy.substring(i, index);

          i += index;

          // ASC or DESC
          index = orderByField.toUpperCase().lastIndexOf("SC");

          Boolean asc = Boolean.TRUE;

          if (index > 1) {
            if (orderByField.charAt(index - 1) != 'E') {
              // field ASC or default
              if (orderByField.charAt(index - 1) == 'A' &&
                  Character.isSpaceChar(orderByField.charAt(index - 2))) {
                index -= 2;
              }
            }
            else if (index > 2 &&
                     orderByField.charAt(index - 2) == 'D' &&
                     Character.isSpaceChar(orderByField.charAt(index - 3))) {

                asc = Boolean.FALSE;
                index -= 3;
            }
          }

          if (index > 0)
            orderByField = orderByField.substring(0, index).trim();

          AmberField amberField = targetType.getField(orderByField);

          if (amberField == null)
            throw error(_field, L.l("'{0}' has no field named '{1}' in @OrderBy",
                                    targetType.getName(),
                                    orderByField));

          /*
          if (amberField instanceof PropertyField) {
            PropertyField property = (PropertyField) amberField;
            orderByField = property.getColumn().getName();
          }
          */

          orderByFields.add(orderByField);
          orderByAscending.add(asc);
        }
      }

      EntityOneToManyField oneToMany;

      oneToMany = new EntityOneToManyField(_relatedType, _fieldName, cascadeTypes);
      oneToMany.setSourceField(sourceField);
      oneToMany.setOrderBy(orderByFields, orderByAscending);

      getInternalMapKeyConfig(_relatedType.getBeanClass(), _field, _fieldName);
      JAnnotation mapKeyAnn = _annotationCfg.getAnnotation();
      MapKeyConfig mapKeyConfig = _annotationCfg.getMapKeyConfig();

      if (! _annotationCfg.isNull()) {

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

      _relatedType.addField(oneToMany);
    }

    private void oneToManyUnidirectional(RelatedType targetType,
                                         String targetName,
                                         CascadeType[] cascadeTypes)
      throws ConfigException
    {
      EntityManyToManyField manyToManyField;

      manyToManyField = new EntityManyToManyField(_relatedType, _fieldName, cascadeTypes);
      manyToManyField.setType(targetType);

      String sqlTable = _relatedType.getTable().getName() + "_" + targetType.getTable().getName();

      JAnnotation joinTableAnn = _field.getAnnotation(javax.persistence.JoinTable.class);

      JoinTableConfig joinTableConfig = null;

      if (_oneToManyConfig != null)
        joinTableConfig = _oneToManyConfig.getJoinTable();

      Table mapTable = null;

      ArrayList<ForeignColumn> sourceColumns = null;
      ArrayList<ForeignColumn> targetColumns = null;

      if ((joinTableAnn != null) || (joinTableConfig != null)) {

        Object joinColumns[] = null;
        Object inverseJoinColumns[] = null;

        HashMap<String, JoinColumnConfig> joinColumnsConfig = null;
        HashMap<String, JoinColumnConfig> inverseJoinColumnsConfig = null;

        if (joinTableAnn != null) {
          if (! joinTableAnn.getString("name").equals(""))
            sqlTable = joinTableAnn.getString("name");

          joinColumns = (Object []) joinTableAnn.get("joinColumns");
          inverseJoinColumns = (Object []) joinTableAnn.get("inverseJoinColumns");
        }
        else {
          if (! joinTableConfig.getName().equals(""))
            sqlTable = joinTableConfig.getName();

          joinColumnsConfig = joinTableConfig.getJoinColumnMap();
          inverseJoinColumnsConfig = joinTableConfig.getInverseJoinColumnMap();
        }

        mapTable = _persistenceUnit.createTable(sqlTable);

        sourceColumns = AbstractConfigIntrospector.calculateColumns(_field,
                                                                    mapTable,
                                                                    _relatedType.getTable().getName() + "_",
                                                                    _relatedType,
                                                                    joinColumns,
                                                                    joinColumnsConfig);

        targetColumns = AbstractConfigIntrospector.calculateColumns(_field,
                                                                    mapTable,
                                                                    targetType.getTable().getName() + "_",
                                                                    targetType,
                                                                    inverseJoinColumns,
                                                                    inverseJoinColumnsConfig);
      }
      else {
        mapTable = _persistenceUnit.createTable(sqlTable);

        sourceColumns = AbstractConfigIntrospector.calculateColumns(mapTable,
                                                                    _relatedType.getTable().getName() + "_",
                                                                    _relatedType);

        targetColumns = AbstractConfigIntrospector.calculateColumns(mapTable,
                                                                    // jpa/0j40
                                                                    _fieldName.toUpperCase() + "_",
                                                                    targetType);
      }

      manyToManyField.setAssociationTable(mapTable);
      manyToManyField.setTable(sqlTable);

      manyToManyField.setSourceLink(new LinkColumns(mapTable,
                                                    _relatedType.getTable(),
                                                    sourceColumns));

      manyToManyField.setTargetLink(new LinkColumns(mapTable,
                                                    targetType.getTable(),
                                                    targetColumns));

      getInternalMapKeyConfig(_relatedType.getBeanClass(), _field, _field.getName());
      JAnnotation mapKeyAnn = _annotationCfg.getAnnotation();
      MapKeyConfig mapKeyConfig = _annotationCfg.getMapKeyConfig();

      if (! _annotationCfg.isNull()) {

        String key;

        if (mapKeyAnn != null)
          key = mapKeyAnn.getString("name");
        else
          key = mapKeyConfig.getName();

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

      _relatedType.addField(manyToManyField);
    }
  }

  /**
   * completes for dependent
   */
  class OneToOneCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToOneCompletion(RelatedType type,
                       JAccessibleObject field,
                       String fieldName,
                       JClass fieldType)
    {
      super(type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    String getFieldName()
    {
      return _fieldName;
    }

    void complete()
      throws ConfigException
    {
      getInternalOneToOneConfig(_relatedType.getBeanClass(), _field, _fieldName);
      JAnnotation oneToOneAnn = _annotationCfg.getAnnotation();
      OneToOneConfig oneToOneConfig = _annotationCfg.getOneToOneConfig();

      boolean isLazy;

      if (oneToOneAnn != null)
        isLazy = oneToOneAnn.get("fetch") == FetchType.LAZY;
      else
        isLazy = oneToOneConfig.getFetch() == FetchType.LAZY;

      if (! isLazy) {
        if (_relatedType.getBeanClass().getName().equals(_fieldType.getName())) {
          throw error(_field, L.l("'{0}': '{1}' is an illegal recursive type for @OneToOne with EAGER fetching. You should specify FetchType.LAZY for this relationship.",
                                  _field.getName(),
                                  _fieldType.getName()));
        }
      }

      AmberPersistenceUnit persistenceUnit = _relatedType.getPersistenceUnit();

      JClass targetEntity = null;
      String targetName = "";

      if (oneToOneAnn != null) {
        targetEntity = oneToOneAnn.getClass("targetEntity");

        if (targetEntity != null)
          targetEntity.getName();
      }
      else {
        targetName = oneToOneConfig.getTargetEntity();

        if (! ((targetName == null) || "".equals(targetName)))
          targetEntity = _persistenceUnit.getJClassLoader().forName(targetName);
      }

      if (targetEntity == null || targetEntity.getName().equals("void"))
        targetEntity = _fieldType;

      getInternalEntityConfig(targetEntity);
      JAnnotation targetEntityAnn = _annotationCfg.getAnnotation();
      EntityConfig targetEntityConfig = _annotationCfg.getEntityConfig();

      if (_annotationCfg.isNull()) {
        throw error(_field, L.l("'{0}' is an illegal targetEntity for {1}.  @OneToOne relations must target a valid @Entity.",
                                targetEntity.getName(), _field.getName()));
      }

      if (! _fieldType.isAssignableFrom(targetEntity)) {
        throw error(_field, L.l("'{0}' is an illegal targetEntity for {1}.  @OneToOne targetEntity must be assignable to the field type '{2}'.",
                                targetEntity.getName(),
                                _field.getName(),
                                _fieldType.getName()));
      }

      String mappedBy;

      if (oneToOneAnn != null)
        mappedBy = oneToOneAnn.getString("mappedBy");
      else
        mappedBy = oneToOneConfig.getMappedBy();

      RelatedType targetType = null;

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

      // jpa/0o00, jpa/0o03, jpa/0o06, jpa/0o07, jpa/10ca, jpa/0s2d

      // jpa/0o06
      boolean isManyToOne = (mappedBy == null) || "".equals(mappedBy);

      // jpa/0o07
      if (isManyToOne) {
        getInternalJoinColumnConfig(_relatedType.getBeanClass(), _field, _fieldName);
        JAnnotation joinColumnAnn = _annotationCfg.getAnnotation();
        JoinColumnConfig joinColumnConfig = _annotationCfg.getJoinColumnConfig();

        if (! _annotationCfg.isNull()) {
          // jpa/0o07: DstBean.getParent()
          // OK: isManyToOne = true;
        }
        else {
          // jpa/10ca
          OneToOneCompletion otherSide
            = getSourceCompletion(targetType, mappedBy);

          if (otherSide != null) {
            getInternalJoinColumnConfig(targetType.getBeanClass(),
                                        otherSide._field,
                                        otherSide._fieldName);

            // jpa/0o07, jpa/0s2d
            if (! _annotationCfg.isNull())
              isManyToOne = false;
          }
        }
      }

      // XXX: jpa/0ge4
      if (targetType.getParentType() != null) {
        if (targetType.getParentType() instanceof MappedSuperclassType) {
          isManyToOne = false;

          OneToOneCompletion otherSide
            = getSourceCompletion(targetType.getParentType(), mappedBy);

          if (otherSide != null) {
            // jpa/0ge4
            if (_depCompletions.remove(otherSide)) {
              otherSide.complete();
            }
          }
        }
      }

      if (isManyToOne) {

        addManyToOne(_relatedType, _field, _fieldName, _field.getReturnType());

        // XXX: set unique
      }
      else {

        if (! (mappedBy == null || "".equals(mappedBy))) {

          // jpa/0o06

          OneToOneCompletion otherSide
            = getSourceCompletion(targetType, mappedBy);

          if (otherSide != null) {
            // jpa/0o00
            if (_depCompletions.remove(otherSide)) {
              otherSide.complete();
            }
          }
        }

        // Owner
        EntityManyToOneField sourceField
          = getSourceField(targetType, mappedBy, _relatedType);

        if (sourceField == null) {
          throw new ConfigException(L.l("{0}: OneToOne target '{1}' does not have a matching ManyToOne relation.",
                                        _field.getDeclaringClass().getName(),
                                        targetType.getName()));
        }

        DependentEntityOneToOneField oneToOne;

        CascadeType cascadeTypes[] = null;

        if (oneToOneAnn != null) {
          // jpa/0o33

          // XXX: runtime does not cast this
          // cascadeType = (CascadeType []) manyToOneAnn.get("cascade");
          Object cascade[] = (Object []) oneToOneAnn.get("cascade");

          cascadeTypes = new CascadeType[cascade.length];

          for (int i=0; i < cascade.length; i++)
            cascadeTypes[i] = (CascadeType) cascade[i];
        }

        oneToOne = new DependentEntityOneToOneField(_relatedType, _fieldName, cascadeTypes);
        oneToOne.setTargetField(sourceField);
        sourceField.setTargetField(oneToOne);
        oneToOne.setLazy(isLazy);

        _relatedType.addField(oneToOne);
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

    ManyToManyCompletion(RelatedType type,
                         JAccessibleObject field,
                         String fieldName,
                         JClass fieldType)
    {
      super(type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToMany(_relatedType, _field, _fieldName, _fieldType);
    }
  }

  /**
   * completes for link
   */
  class ManyToOneCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToOneCompletion(RelatedType type,
                        JAccessibleObject field,
                        String fieldName,
                        JClass fieldType)
    {
      super(type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToOne(_relatedType, _field, _fieldName, _fieldType);
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

    EmbeddedCompletion(RelatedType type,
                       JAccessibleObject field,
                       String fieldName,
                       JClass fieldType,
                       boolean embeddedId)
    {
      super(type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
      _embeddedId = embeddedId;
    }

    void complete()
      throws ConfigException
    {
      getInternalAttributeOverrideConfig(_relatedType.getBeanClass());
      JAnnotation attributeOverrideAnn = _annotationCfg.getAnnotation();
      AttributeOverrideConfig attributeOverrideConfig = _annotationCfg.getAttributeOverrideConfig();

      boolean hasAttributeOverride = ! _annotationCfg.isNull();

      JAnnotation attributeOverridesAnn = _field.getAnnotation(AttributeOverrides.class);

      boolean hasAttributeOverrides = (attributeOverridesAnn != null);

      if (hasAttributeOverride && hasAttributeOverrides) {
        throw error(_field, L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
                                _field.getName()));
      }

      Object attOverridesAnn[] = null;

      if (attributeOverrideAnn != null) {
        attOverridesAnn = new Object[] { attributeOverrideAnn };
      }
      else if (attributeOverridesAnn != null) {
        attOverridesAnn = (Object []) attributeOverridesAnn.get("value");
      }

      EntityEmbeddedField embeddedField;

      if (_embeddedId) {
        embeddedField = _relatedType.getId().getEmbeddedIdField();
      } else {
        embeddedField = new EntityEmbeddedField(_relatedType, _fieldName);
      }

      embeddedField.setEmbeddedId(_embeddedId);

      embeddedField.setLazy(false);

      AmberPersistenceUnit persistenceUnit = _relatedType.getPersistenceUnit();

      EmbeddableType type = persistenceUnit.createEmbeddable(_fieldType.getName(), _fieldType);

      embeddedField.setType(type);

      _relatedType.addField(embeddedField);

      // XXX: todo ...
      // validateAttributeOverrides(_field, attributeOverridesAnn, type);

      Table sourceTable = _relatedType.getTable();

      HashMap<String, Column> embeddedColumns = new HashMap<String, Column>();
      HashMap<String, String> fieldNameByColumn = new HashMap<String, String>();

      ArrayList<AmberField> fields = type.getFields();

      for (int i=0; i < fields.size(); i++) {

        String embeddedFieldName = fields.get(i).getName();

        String columnName = toSqlName(embeddedFieldName);
        boolean notNull = false;
        boolean unique = false;

        if (attOverridesAnn != null) {
          for (int j=0; j<attOverridesAnn.length; j++) {

            if (embeddedFieldName.equals(((JAnnotation) attOverridesAnn[j]).getString("name"))) {

              JAnnotation columnAnn = ((JAnnotation) attOverridesAnn[j]).getAnnotation("column");

              if (columnAnn != null) {
                columnName = columnAnn.getString("name");
                notNull = ! columnAnn.getBoolean("nullable");
                unique = columnAnn.getBoolean("unique");
              }
            }
          }
        }

        Type amberType = _persistenceUnit.createType(fields.get(i).getJavaType().getName());

        Column column = sourceTable.createColumn(columnName, amberType);

        column.setNotNull(notNull);
        column.setUnique(unique);

        embeddedColumns.put(columnName, column);
        fieldNameByColumn.put(columnName, embeddedFieldName);
      }

      embeddedField.setEmbeddedColumns(embeddedColumns);
      embeddedField.setFieldNameByColumn(fieldNameByColumn);

      embeddedField.init();
    }
  }

  /**
   * completes for dependent
   */
  class SqlResultSetMappingCompletion extends Completion {
    private String _name;
    private Object _entities[];
    private Object _columns[];

    SqlResultSetMappingCompletion(RelatedType type,
                                  String name,
                                  Object entities[],
                                  Object columns[])
    {
      super(type);

      _name = name;
      _entities = entities;
      _columns = columns;
    }

    void complete()
      throws ConfigException
    {
      addSqlResultSetMapping(_name,
                             _entities,
                             _columns);
    }
  }

  /**
   * completes for link
   */
  class AttributeOverrideCompletion extends Completion {
    private JClass _type;

    AttributeOverrideCompletion(RelatedType relatedType,
                                JClass type)
    {
      super(relatedType);

      _type = type;
    }

    void complete()
      throws ConfigException
    {
      getInternalAttributeOverrideConfig(_type);
      JAnnotation attributeOverrideAnn = _annotationCfg.getAnnotation();

      boolean hasAttributeOverride = (attributeOverrideAnn != null);

      JAnnotation attributeOverridesAnn
        = _type.getAnnotation(AttributeOverrides.class);

      ArrayList<AttributeOverrideConfig> attributeOverrideList = null;

      if (_entityConfigMap != null) {
        EntityConfig entityConfig = _entityConfigMap.get(_type.getName());

        if (entityConfig != null)
          attributeOverrideList = entityConfig.getAttributeOverrideList();
      }

      boolean hasAttributeOverrides = false;

      if ((attributeOverrideList != null) &&
          (attributeOverrideList.size() > 0)) {
        hasAttributeOverrides = true;
      }
      else if (attributeOverridesAnn != null)
        hasAttributeOverrides = true;

      if (hasAttributeOverride && hasAttributeOverrides)
        throw new ConfigException(L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
                                      _type));

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

      // jpa/0ge8, jpa/0ge9, jpa/0gea
      // Fields which have not been overridden are added to the
      // entity subclass. This makes the columns to be properly
      // created at each entity table -- not the mapped superclass
      // table, even because the parent might not have a valid table.

      RelatedType parent = _relatedType.getParentType();

      ArrayList<AmberField> fields = parent.getFields();

      for (AmberField field : fields) {
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

        if (field instanceof PropertyField) {
          Column column = ((PropertyField) field).getColumn();

          // jpa/0ge8, jpa/0gea
          // Creates the missing attribute override config.
          attOverrideConfig = createAttributeOverrideConfig(fieldName,
                                                            column.getName(),
                                                            ! column.isNotNull(),
                                                            column.isUnique());

          attributeOverrideList.add(attOverrideConfig);
        }
      }

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
              if (_relatedType.isFieldAccess())
                introspectIdField(_persistenceUnit, _relatedType, null,
                                  parent.getBeanClass(), null, null);
              else
                introspectIdMethod(_persistenceUnit, _relatedType, null,
                                   parent.getBeanClass(), null, null);
            } catch (SQLException e) {
              throw new ConfigException(e);
            }

            field = _relatedType.getId().getKeys().get(0);

            Column column = ((KeyPropertyField) field).getColumn();

            // jpa/0ge8, jpa/0ge9, jpa/0gea
            // Creates the missing attribute override config.
            attOverrideConfig = createAttributeOverrideConfig(fieldName,
                                                              column.getName(),
                                                              ! column.isNotNull(),
                                                              column.isUnique());

            attributeOverrideList.add(attOverrideConfig);
          }
        }
      }

      // Overrides fields from MappedSuperclass.

      Table sourceTable = _relatedType.getTable();

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
              _relatedType.addMappedSuperclassField(field);
            }

            continue;
          }

          entityFieldName = field.getName();

          columnName = toSqlName(entityFieldName);

          if (entityFieldName.equals(attOverrideConfig.getName())) {

            ColumnConfig columnConfig = attOverrideConfig.getColumn();

            if (columnConfig != null) {
              columnName = columnConfig.getName();
              notNull = ! columnConfig.getNullable();
              unique = columnConfig.getUnique();
              amberType = _persistenceUnit.createType(field.getJavaType().getName());

              Column column = sourceTable.createColumn(columnName, amberType);

              column.setNotNull(notNull);
              column.setUnique(unique);

              PropertyField overriddenField
                = new PropertyField(field.getSourceType(), field.getName());

              overriddenField.setType(((PropertyField) field).getType());
              overriddenField.setLazy(field.isLazy());
              overriddenField.setColumn(column);

              _relatedType.addMappedSuperclassField(overriddenField);
            }
          }
        }

        if (_relatedType.getId() != null) {
          ArrayList<IdField> keys = _relatedType.getId().getKeys();

          for (int j = 0; j < keys.size(); j++) {

            IdField field = keys.get(j);

            entityFieldName = field.getName();

            columnName = toSqlName(entityFieldName);

            if (entityFieldName.equals(attOverrideConfig.getName())) {

              ColumnConfig columnConfig = attOverrideConfig.getColumn();

              if (columnConfig != null) {
                columnName = columnConfig.getName();
                notNull = ! columnConfig.getNullable();
                unique = columnConfig.getUnique();
                amberType = _persistenceUnit.createType(field.getJavaType().getName());

                Column column = sourceTable.createColumn(columnName, amberType);

                column.setNotNull(notNull);
                column.setUnique(unique);

                if (field instanceof KeyPropertyField) {
                  KeyPropertyField overriddenField
                    = new KeyPropertyField((RelatedType) field.getSourceType(),
                                           field.getName());

                  overriddenField.setGenerator(field.getGenerator());
                  overriddenField.setColumn(column);

                  // XXX: needs to handle compound pk with @AttributeOverride ???
                  if (keys.size() == 1) {
                    keys.remove(0);
                    keys.add(overriddenField);
                    _relatedType.setId(new com.caucho.amber.field.Id(_relatedType, keys));
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  void getInternalEmbeddableConfig(JClass type)
  {
    _annotationCfg.reset(type, Embeddable.class);

    EmbeddableConfig embeddableConfig = null;

    if (_embeddableConfigMap != null)
      embeddableConfig = _embeddableConfigMap.get(type.getName());

    _annotationCfg.setConfig(embeddableConfig);
  }

  void getInternalEntityConfig(JClass type)
  {
    _annotationCfg.reset(type, Entity.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    _annotationCfg.setConfig(entityConfig);
  }

  void getInternalMappedSuperclassConfig(JClass type)
  {
    _annotationCfg.reset(type, MappedSuperclass.class);

    if (_mappedSuperclassConfigMap == null)
      return;

    MappedSuperclassConfig mappedSuperConfig = null;

    mappedSuperConfig = _mappedSuperclassConfigMap.get(type.getName());

    _annotationCfg.setConfig(mappedSuperConfig);
  }

  void getInternalEntityListenersConfig(JClass type)
  {
    _annotationCfg.reset(type, EntityListeners.class);

    if (_entityConfigMap == null)
      return;

    EntityConfig entityConfig;

    entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig == null)
      return;

    _annotationCfg.setConfig(entityConfig.getEntityListeners());
  }

  void getInternalExcludeDefaultListenersConfig(JClass type)
  {
    _annotationCfg.reset(type, ExcludeDefaultListeners.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    if (entityConfig.getExcludeDefaultListeners())
      _annotationCfg.setConfig(entityConfig.getExcludeDefaultListeners());
  }

  void getInternalExcludeSuperclassListenersConfig(JClass type)
  {
    _annotationCfg.reset(type, ExcludeSuperclassListeners.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    if (entityConfig.getExcludeSuperclassListeners())
      _annotationCfg.setConfig(entityConfig.getExcludeSuperclassListeners());
  }

  void getInternalInheritanceConfig(JClass type)
  {
    _annotationCfg.reset(type, Inheritance.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getInheritance());
    }
  }

  void getInternalNamedQueryConfig(JClass type)
  {
    _annotationCfg.reset(type, NamedQuery.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getNamedQuery());
    }
  }

  void getInternalNamedNativeQueryConfig(JClass type)
  {
    _annotationCfg.reset(type, NamedNativeQuery.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getNamedNativeQuery());
    }
  }

  void getInternalSqlResultSetMappingConfig(JClass type)
  {
    _annotationCfg.reset(type, SqlResultSetMapping.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getSqlResultSetMapping());
    }
  }

  void getInternalTableConfig(JClass type)
  {
    _annotationCfg.reset(type, javax.persistence.Table.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getTable());
    }
  }

  void getInternalSecondaryTableConfig(JClass type)
  {
    _annotationCfg.reset(type, SecondaryTable.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getSecondaryTable());
    }
  }

  void getInternalIdClassConfig(JClass type)
  {
    _annotationCfg.reset(type, IdClass.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    _annotationCfg.setConfig(entityConfig.getIdClass());
  }

  void getInternalPrimaryKeyJoinColumnConfig(JClass type)
  {
    _annotationCfg.reset(type, PrimaryKeyJoinColumn.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getPrimaryKeyJoinColumn());
    }
  }

  void getInternalDiscriminatorColumnConfig(JClass type)
  {
    _annotationCfg.reset(type, DiscriminatorColumn.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getDiscriminatorColumn());
    }
  }

  void getInternalOneToOneConfig(JClass type,
                                 JAccessibleObject field,
                                 String fieldName)
  {
    _annotationCfg.reset(field, OneToOne.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      OneToOneConfig oneToOne = attributes.getOneToOne(fieldName);

      _annotationCfg.setConfig(oneToOne);
    }
  }

  void getInternalOneToManyConfig(JClass type,
                                  JAccessibleObject field,
                                  String fieldName)
  {
    _annotationCfg.reset(field, OneToMany.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      OneToManyConfig oneToMany = attributes.getOneToMany(fieldName);

      _annotationCfg.setConfig(oneToMany);
    }
  }

  void getInternalManyToOneConfig(JClass type,
                                  JAccessibleObject field,
                                  String fieldName)
  {
    _annotationCfg.reset(field, ManyToOne.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      ManyToOneConfig manyToOne = attributes.getManyToOne(fieldName);

      _annotationCfg.setConfig(manyToOne);
    }
  }

  void getInternalManyToManyConfig(JClass type,
                                   JAccessibleObject field,
                                   String fieldName)
  {
    _annotationCfg.reset(field, ManyToMany.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      ManyToManyConfig manyToMany = attributes.getManyToMany(fieldName);

      _annotationCfg.setConfig(manyToMany);
    }
  }

  void getInternalIdConfig(JClass type,
                           JAccessibleObject method,
                           String fieldName)
  {
    _annotationCfg.reset(method, javax.persistence.Id.class);

    MappedSuperclassConfig mappedSuperclassOrEntityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (mappedSuperclassOrEntityConfig == null)
      return;

    AttributesConfig attributes
      = mappedSuperclassOrEntityConfig.getAttributes();

    if (attributes != null) {
      IdConfig id = attributes.getId(fieldName);

      _annotationCfg.setConfig(id);
    }
  }

  void getInternalCallbackConfig(int callback,
                                 JClass type,
                                 JAccessibleObject method,
                                 String fieldName)
  {
    _annotationCfg.reset(method, ListenerType.CALLBACK_CLASS[callback]);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AbstractListenerConfig callbackConfig;

    switch (callback) {
    case Listener.PRE_PERSIST:
      callbackConfig = entityConfig.getPrePersist();
      break;
    case Listener.POST_PERSIST:
      callbackConfig = entityConfig.getPostPersist();
      break;
    case Listener.PRE_REMOVE:
      callbackConfig = entityConfig.getPreRemove();
      break;
    case Listener.POST_REMOVE:
      callbackConfig = entityConfig.getPostRemove();
      break;
    case Listener.PRE_UPDATE:
      callbackConfig = entityConfig.getPreUpdate();
      break;
    case Listener.POST_UPDATE:
      callbackConfig = entityConfig.getPostUpdate();
      break;
    case Listener.POST_LOAD:
      callbackConfig = entityConfig.getPostLoad();
      break;
    default:
      return;
    }

    if (callbackConfig == null)
      return;

    if (callbackConfig.getMethodName().equals(method.getName()))
      _annotationCfg.setConfig(callbackConfig);
  }

  void getInternalEmbeddedIdConfig(JClass type,
                                   JAccessibleObject method,
                                   String fieldName)
  {
    _annotationCfg.reset(method, EmbeddedId.class);
  }

  void getInternalVersionConfig(JClass type,
                                JAccessibleObject method,
                                String fieldName)
  {
    _annotationCfg.reset(method, Version.class);
  }

  void getInternalJoinColumnConfig(JClass type,
                                   JAccessibleObject field,
                                   String fieldName)
  {
    _annotationCfg.reset(field, JoinColumn.class);
  }

  void getInternalJoinTableConfig(JClass type,
                                  JAccessibleObject field,
                                  String fieldName)
  {
    _annotationCfg.reset(field, JoinTable.class);
  }

  void getInternalMapKeyConfig(JClass type,
                               JAccessibleObject field,
                               String fieldName)
  {
    _annotationCfg.reset(field, MapKey.class);
  }

  void getInternalAttributeOverrideConfig(JClass type)
  {
    _annotationCfg.reset(type, AttributeOverride.class);
  }

  private MappedSuperclassConfig getInternalMappedSuperclassOrEntityConfig(String name)
  {
    MappedSuperclassConfig mappedSuperclassConfig = null;

    if (_entityConfigMap != null)
      mappedSuperclassConfig = _entityConfigMap.get(name);

    if (mappedSuperclassConfig != null)
      return mappedSuperclassConfig;

    if (_mappedSuperclassConfigMap != null)
      mappedSuperclassConfig = _mappedSuperclassConfigMap.get(name);

    return mappedSuperclassConfig;
  }

  private static AttributeOverrideConfig convertAttributeOverrideAnnotationToConfig(JAnnotation attOverrideAnn)
  {
    JAnnotation columnAnn = attOverrideAnn.getAnnotation("column");

    return createAttributeOverrideConfig(attOverrideAnn.getString("name"),
                                         columnAnn.getString("name"),
                                         columnAnn.getBoolean("nullable"),
                                         columnAnn.getBoolean("unique"));
  }

  private static AttributeOverrideConfig createAttributeOverrideConfig(String name,
                                                                       String columnName,
                                                                       boolean isNullable,
                                                                       boolean isUnique)
  {
    AttributeOverrideConfig attOverrideConfig
      = new AttributeOverrideConfig();

    attOverrideConfig.setName(name);

    ColumnConfig columnConfig = new ColumnConfig();

    columnConfig.setName(columnName);
    columnConfig.setNullable(isNullable);
    columnConfig.setUnique(isUnique);

    attOverrideConfig.setColumn(columnConfig);

    return attOverrideConfig;
  }
}
