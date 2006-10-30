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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.persistence.*;

import com.caucho.bytecode.*;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.AbstractEnhancedType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.ListenerType;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.util.L10N;


/**
 * Base introspector for orm.xml and annotations.
 */
public class BaseConfigIntrospector {
  private static final Logger log = Logger.getLogger(BaseConfigIntrospector.class.getName());
  private static final L10N L = new L10N(BaseConfigIntrospector.class);

  AnnotationConfig _annotationCfg = new AnnotationConfig();

  HashMap<String,EntityConfig> _entityConfigMap
    = new HashMap<String,EntityConfig>();

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

    if (metaData != null) {
      PersistenceUnitDefaultsConfig defaults;

      defaults = metaData.getPersistenceUnitDefaults();

      if (defaults != null) {
        EntityListenersConfig entityListeners;
        entityListeners = defaults.getEntityListeners();

        if (entityListeners != null) {
          ArrayList<EntityListenerConfig> listeners;
          listeners = entityListeners.getEntityListeners();

          for (EntityListenerConfig listener : listeners)
            introspectListener(listener, persistenceUnit);
        }
      }
    }
  }

  public void introspectListener(EntityListenerConfig listener,
                                 AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    JClassLoader loader = persistenceUnit.getJClassLoader();

    String className = listener.getClassName();

    JClass type = loader.forName(className);

    if (type == null) {
      throw new ConfigException(L.l("'{0}' is an unknown type for <entity-listener> in orm.xml",
                                    className));
    }

    ListenerType listenerType = persistenceUnit.addDefaultListener(type);

    listenerType.setInstanceClassName(listenerType.getName() + "__ResinExt");

    for (JMethod method : type.getMethods()) {
      introspectCallbacks(listenerType, method);
    }
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(AbstractEnhancedType type, JMethod method)
    throws ConfigException
  {
    JClass []param = method.getParameterTypes();

    String methodName = method.getName();
    JClass jClass = type.getBeanClass();

    boolean isListener = ! (type instanceof EntityType);

    getInternalPostLoadConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PostLoad", method, isListener);

      type.addPostLoadCallback(method);
    }

    getInternalPrePersistConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PrePersist", method, isListener);

      type.addPrePersistCallback(method);
    }

    getInternalPostPersistConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PostPersist", method, isListener);

      type.addPostPersistCallback(method);
    }

    getInternalPreUpdateConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PreUpdate", method, isListener);

      type.addPreUpdateCallback(method);
    }

    getInternalPostUpdateConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PostUpdate", method, isListener);

      type.addPostUpdateCallback(method);
    }

    getInternalPreRemoveConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PreRemove", method, isListener);

      type.addPreRemoveCallback(method);
    }

    getInternalPostRemoveConfig(jClass, method, methodName);

    if (! _annotationCfg.isNull()) {
      validateCallback("PostRemove", method, isListener);

      type.addPostRemoveCallback(method);
    }
  }

  /**
   * Validates a callback method
   */
  protected void validateCallback(String callbackName,
                                  JMethod method,
                                  boolean isListener)
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

    JClass params[] = method.getParameterTypes();

    if (isListener) {
      if (params.length != 1) {
        throw new ConfigException(L.l("'{0}' must have the <METHOD>(Object) signature for entity listeners.",
                                      method.getFullName()));
      }
    }
    else if (params.length != 0) {
      throw new ConfigException(L.l("'{0}' must not have any arguments.  @{1} methods have zero arguments for entities or mapped superclasses.",
                                    method.getFullName(),
                                    callbackName));
    }
  }

  void getInternalEntityConfig(JClass type)
  {
    _annotationCfg.reset(type, Entity.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    _annotationCfg.setConfig(entityConfig);
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

  void getInternalTableConfig(JClass type)
  {
    _annotationCfg.reset(type, Table.class);

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

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {
      _annotationCfg.setConfig(entityConfig.getIdClass());
    }
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

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {

      AttributesConfig attributes = entityConfig.getAttributes();

      if (attributes != null) {
        OneToOneConfig oneToOne = attributes.getOneToOne(fieldName);

        _annotationCfg.setConfig(oneToOne);
      }
    }
  }

  void getInternalOneToManyConfig(JClass type,
                                  JAccessibleObject field,
                                  String fieldName)
  {
    _annotationCfg.reset(field, OneToMany.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {

      AttributesConfig attributes = entityConfig.getAttributes();

      if (attributes != null) {
        OneToManyConfig oneToMany = attributes.getOneToMany(fieldName);

        _annotationCfg.setConfig(oneToMany);
      }
    }
  }

  void getInternalManyToOneConfig(JClass type,
                                  JAccessibleObject field,
                                  String fieldName)
  {
    _annotationCfg.reset(field, ManyToOne.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {

      AttributesConfig attributes = entityConfig.getAttributes();

      if (attributes != null) {
        ManyToOneConfig manyToOne = attributes.getManyToOne(fieldName);

        _annotationCfg.setConfig(manyToOne);
      }
    }
  }

  void getInternalManyToManyConfig(JClass type,
                                   JAccessibleObject field,
                                   String fieldName)
  {
    _annotationCfg.reset(field, ManyToMany.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {

      AttributesConfig attributes = entityConfig.getAttributes();

      if (attributes != null) {
        ManyToManyConfig manyToMany = attributes.getManyToMany(fieldName);

        _annotationCfg.setConfig(manyToMany);
      }
    }
  }

  void getInternalIdConfig(JClass type,
                           JAccessibleObject method,
                           String fieldName)
  {
    _annotationCfg.reset(method, Id.class);

    EntityConfig entityConfig = null;

    if (_entityConfigMap != null)
      entityConfig = _entityConfigMap.get(type.getName());

    if (entityConfig != null) {

      AttributesConfig attributes = entityConfig.getAttributes();

      if (attributes != null) {
        IdConfig id = attributes.getId(fieldName);

        _annotationCfg.setConfig(id);
      }
    }
  }

  void getInternalPostLoadConfig(JClass type,
                                 JAccessibleObject method,
                                 String fieldName)
  {
    _annotationCfg.reset(method, PostLoad.class);
  }

  void getInternalPrePersistConfig(JClass type,
                                   JAccessibleObject method,
                                   String fieldName)
  {
    _annotationCfg.reset(method, PrePersist.class);
  }

  void getInternalPostPersistConfig(JClass type,
                                    JAccessibleObject method,
                                    String fieldName)
  {
    _annotationCfg.reset(method, PostPersist.class);
  }

  void getInternalPreUpdateConfig(JClass type,
                                  JAccessibleObject method,
                                  String fieldName)
  {
    _annotationCfg.reset(method, PreUpdate.class);
  }

  void getInternalPostUpdateConfig(JClass type,
                                   JAccessibleObject method,
                                   String fieldName)
  {
    _annotationCfg.reset(method, PostUpdate.class);
  }

  void getInternalPreRemoveConfig(JClass type,
                                  JAccessibleObject method,
                                  String fieldName)
  {
    _annotationCfg.reset(method, PreRemove.class);
  }

  void getInternalPostRemoveConfig(JClass type,
                                   JAccessibleObject method,
                                   String fieldName)
  {
    _annotationCfg.reset(method, PostRemove.class);
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

  void getInternalAttributeOverrideConfig(JClass type,
                                          JAccessibleObject field,
                                          String fieldName)
  {
    _annotationCfg.reset(field, AttributeOverride.class);
  }

  class AnnotationConfig {
    private JAnnotation _annotation;
    private Object _config;

    public JAnnotation getAnnotation()
    {
      return _annotation;
    }

    public Object getConfig()
    {
      return _config;
    }

    public void setAnnotation(JAnnotation annotation)
    {
      _annotation = annotation;
    }

    public void setConfig(Object config)
    {
      _config = config;
    }

    public boolean isNull()
    {
      return (_annotation == null) && (_config == null);
    }

    public void reset()
    {
      _annotation = null;
      _config = null;
    }

    public void reset(JClass type, Class cl)
    {
      _annotation = type.getAnnotation(cl);
      _config = null;
    }

    public void reset(JAccessibleObject field, Class cl)
    {
      _annotation = field.getAnnotation(cl);
      _config = null;
    }

    public EntityConfig getEntityConfig()
    {
      return (EntityConfig) _config;
    }

    public TableConfig getTableConfig() {
      return (TableConfig) _config;
    }

    public SecondaryTableConfig getSecondaryTableConfig() {
      return (SecondaryTableConfig) _config;
    }

    public IdClassConfig getIdClassConfig() {
      return (IdClassConfig) _config;
    }

    public PostLoadConfig getPostLoadConfig() {
      return (PostLoadConfig) _config;
    }

    public PrePersistConfig getPrePersistConfig() {
      return (PrePersistConfig) _config;
    }

    public PostPersistConfig getPostPersistConfig() {
      return (PostPersistConfig) _config;
    }

    public PreUpdateConfig getPreUpdateConfig() {
      return (PreUpdateConfig) _config;
    }

    public PostUpdateConfig getPostUpdateConfig() {
      return (PostUpdateConfig) _config;
    }

    public PreRemoveConfig getPreRemoveConfig() {
      return (PreRemoveConfig) _config;
    }

    public PostRemoveConfig getPostRemoveConfig() {
      return (PostRemoveConfig) _config;
    }

    public InheritanceConfig getInheritanceConfig() {
      return (InheritanceConfig) _config;
    }

    public NamedQueryConfig getNamedQueryConfig() {
      return (NamedQueryConfig) _config;
    }

    public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumnConfig() {
      return (PrimaryKeyJoinColumnConfig) _config;
    }

    public DiscriminatorColumnConfig getDiscriminatorColumnConfig() {
      return (DiscriminatorColumnConfig) _config;
    }

    public IdConfig getIdConfig() {
      return (IdConfig) _config;
    }

    public EmbeddedIdConfig getEmbeddedIdConfig() {
      return (EmbeddedIdConfig) _config;
    }

    public ColumnConfig getColumnConfig() {
      return (ColumnConfig) _config;
    }

    public GeneratedValueConfig getGeneratedValueConfig() {
      return (GeneratedValueConfig) _config;
    }

    public BasicConfig getBasicConfig() {
      return (BasicConfig) _config;
    }

    public VersionConfig getVersionConfig() {
      return (VersionConfig) _config;
    }

    public ManyToOneConfig getManyToOneConfig() {
      return (ManyToOneConfig) _config;
    }

    public OneToOneConfig getOneToOneConfig() {
      return (OneToOneConfig) _config;
    }

    public ManyToManyConfig getManyToManyConfig() {
      return (ManyToManyConfig) _config;
    }

    public OneToManyConfig getOneToManyConfig() {
      return (OneToManyConfig) _config;
    }

    public MapKeyConfig getMapKeyConfig() {
      return (MapKeyConfig) _config;
    }

    public JoinTableConfig getJoinTableConfig() {
      return (JoinTableConfig) _config;
    }

    public JoinColumnConfig getJoinColumnConfig() {
      return (JoinColumnConfig) _config;
    }

    public AttributeOverrideConfig getAttributeOverrideConfig() {
      return (AttributeOverrideConfig) _config;
    }

    // public AttributeOverridesConfig getAttributeOverridesConfig() {
    //   return (AttributeOverridesConfig) _config;
    // }

    public AssociationOverrideConfig getAssociationOverrideConfig() {
      return (AssociationOverrideConfig) _config;
    }

    // public AssociationOverridesConfig getAssociationOverridesConfig() {
    //   return (AssociationOverridesConfig) _config;
    // }
  }
}
