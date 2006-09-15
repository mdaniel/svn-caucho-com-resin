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

import java.util.HashMap;

import javax.persistence.*;

import com.caucho.bytecode.*;

/**
 * Base introspector for orm.xml and annotations.
 */
abstract public class AbstractConfigIntrospector {
  AnnotationConfig _annotationCfg = new AnnotationConfig();

  HashMap<String,EntityConfig> _entityConfigMap
    = new HashMap<String,EntityConfig>();

  void getInternalConfig(JClass type, Class cl)
  {
    _annotationCfg.reset();

    _annotationCfg.setAnnotation(type.getAnnotation(cl));

    if (cl.equals(Entity.class)) {

      EntityConfig entityConfig = null;

      if (_entityConfigMap != null)
        entityConfig = _entityConfigMap.get(type.getName());

      _annotationCfg.setConfig(entityConfig);
    }
  }

  void getInternalConfig(JAccessibleObject type, Class cl)
  {
    _annotationCfg.reset();

    _annotationCfg.setAnnotation(type.getAnnotation(cl));
  }

  void getInternalConfig(JField field, Class cl)
  {
    _annotationCfg.reset();

    _annotationCfg.setAnnotation(field.getAnnotation(cl));
  }

  void getInternalConfig(JMethod method, Class cl)
  {
    _annotationCfg.reset();

    _annotationCfg.setAnnotation(method.getAnnotation(cl));
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
