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


/**
 * <entity> tag in the orm.xml
 */
public class EntityConfig {
  // attributes
  private String _name;
  private String _className;
  private String _access;
  private boolean _isMetadataComplete;

  // elements
  private String _description;
  private TableConfig _table;
  private SecondaryTableConfig _secondaryTable;
  private PrimaryKeyJoinColumnConfig _primaryKeyJoinColumn;
  private IdClassConfig _idClass;
  private InheritanceConfig _inheritance;
  private String _discriminatorValue;
  private DiscriminatorColumnConfig _discriminatorColumn;
  private SequenceGeneratorConfig _sequenceGenerator;
  private TableGeneratorConfig _tableGenerator;
  private NamedQueryConfig _namedQuery;
  private NamedNativeQueryConfig _namedNativeQuery;
  private SqlResultSetMappingConfig _sqlResultSetMapping;
  private boolean _excludeDefaultListeners;
  private boolean _excludeSuperclassListeners;
  private EntityListenersConfig _entityListeners;
  private PrePersistConfig _prePersist;
  private PostPersistConfig _postPersist;
  private PreRemoveConfig _preRemove;
  private PostRemoveConfig _postRemove;
  private PreUpdateConfig _preUpdate;
  private PostUpdateConfig _postUpdate;
  private PostLoadConfig _postLoad;
  private AttributeOverrideConfig _attributeOverride;
  private AssociationOverrideConfig _associationOverride;
  private AttributesConfig _attributes;

  /**
   * Returns the access type.
   */
  public String getAccess()
  {
    return _access;
  }

  /**
   * Returns the attributes.
   */
  public AttributesConfig getAttributes()
  {
    return _attributes;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Returns the entity name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true if the metadata is complete.
   */
  public boolean isMetaDataComplete()
  {
    return _isMetadataComplete;
  }

  /**
   * Sets the access type.
   */
  public void setAccess(String access)
  {
    _access = access;
  }

  /**
   * Sets the attributes.
   */
  public void setAttributes(AttributesConfig attributes)
  {
    _attributes = attributes;
  }

  /**
   * Sets the class name.
   */
  public void addClass(String className)
  {
    _className = className;
  }

  /**
   * Sets the metadata is complete (true) or not (false).
   */
  public void setMetaDataComplete(boolean isMetaDataComplete)
  {
    _isMetadataComplete = isMetaDataComplete;
  }

  /**
   * Sets the entity name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public String getDescription()
  {
    return _description;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public TableConfig getTable()
  {
    return _table;
  }

  public void setTable(TableConfig table)
  {
    _table = table;
  }

  public SecondaryTableConfig getSecondaryTable()
  {
    return _secondaryTable;
  }

  public void setSecondaryTable(SecondaryTableConfig secondaryTable)
  {
    _secondaryTable = secondaryTable;
  }

  public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumn()
  {
    return _primaryKeyJoinColumn;
  }

  public void setPrimaryKeyJoinColumn(PrimaryKeyJoinColumnConfig primaryKeyJoinColumn)
  {
    _primaryKeyJoinColumn = primaryKeyJoinColumn;
  }

  public IdClassConfig getIdClass()
  {
    return _idClass;
  }

  public void setIdClass(IdClassConfig idClass)
  {
    _idClass = idClass;
  }

  public InheritanceConfig getInheritance()
  {
    return _inheritance;
  }

  public void setInheritance(InheritanceConfig inheritance)
  {
    _inheritance = inheritance;
  }

  public String getDiscriminatorValue()
  {
    return _discriminatorValue;
  }

  public void setDiscriminatorValue(String discriminatorValue)
  {
    _discriminatorValue = discriminatorValue;
  }

  public DiscriminatorColumnConfig getDiscriminatorColumn()
  {
    return _discriminatorColumn;
  }

  public void setDiscriminatorColumn(DiscriminatorColumnConfig discriminatorColumn)
  {
    _discriminatorColumn = discriminatorColumn;
  }

  public SequenceGeneratorConfig getSequenceGenerator()
  {
    return _sequenceGenerator;
  }

  public void setSequenceGenerator(SequenceGeneratorConfig sequenceGenerator)
  {
    _sequenceGenerator = sequenceGenerator;
  }

  public TableGeneratorConfig getTableGenerator()
  {
    return _tableGenerator;
  }

  public void setTableGenerator(TableGeneratorConfig tableGenerator)
  {
    _tableGenerator = tableGenerator;
  }

  public NamedQueryConfig getNamedQuery()
  {
    return _namedQuery;
  }

  public void setNamedQuery(NamedQueryConfig namedQuery)
  {
    _namedQuery = namedQuery;
  }

  public NamedNativeQueryConfig getNamedNativeQuery()
  {
    return _namedNativeQuery;
  }

  public void setNamedNativeQuery(NamedNativeQueryConfig namedNativeQuery)
  {
    _namedNativeQuery = namedNativeQuery;
  }

  public SqlResultSetMappingConfig getSqlResultSetMapping()
  {
    return _sqlResultSetMapping;
  }

  public void setSqlResultSetMapping(SqlResultSetMappingConfig sqlResultSetMapping)
  {
    _sqlResultSetMapping = sqlResultSetMapping;
  }

  public boolean getExcludeDefaultListeners()
  {
    return _excludeDefaultListeners;
  }

  public void setExcludeDefaultListeners(boolean excludeDefaultListeners)
  {
    _excludeDefaultListeners = excludeDefaultListeners;
  }

  public boolean getExcludeSuperclassListeners()
  {
    return _excludeSuperclassListeners;
  }

  public void setExcludeSuperclassListeners(boolean excludeSuperclassListeners)
  {
    _excludeSuperclassListeners = excludeSuperclassListeners;
  }

  public EntityListenersConfig getEntityListeners()
  {
    return _entityListeners;
  }

  public void setEntityListeners(EntityListenersConfig entityListeners)
  {
    _entityListeners = entityListeners;
  }

  public PrePersistConfig getPrePersist()
  {
    return _prePersist;
  }

  public void setPrePersist(PrePersistConfig prePersist)
  {
    _prePersist = prePersist;
  }

  public PostPersistConfig getPostPersist()
  {
    return _postPersist;
  }

  public void setPostPersist(PostPersistConfig postPersist)
  {
    _postPersist = postPersist;
  }

  public PreRemoveConfig getPreRemove()
  {
    return _preRemove;
  }

  public void setPreRemove(PreRemoveConfig preRemove)
  {
    _preRemove = preRemove;
  }

  public PostRemoveConfig getPostRemove()
  {
    return _postRemove;
  }

  public void setPostRemove(PostRemoveConfig postRemove)
  {
    _postRemove = postRemove;
  }

  public PreUpdateConfig getPreUpdate()
  {
    return _preUpdate;
  }

  public void setPreUpdate(PreUpdateConfig preUpdate)
  {
    _preUpdate = preUpdate;
  }

  public PostUpdateConfig getPostUpdate()
  {
    return _postUpdate;
  }

  public void setPostUpdate(PostUpdateConfig postUpdate)
  {
    _postUpdate = postUpdate;
  }

  public PostLoadConfig getPostLoad()
  {
    return _postLoad;
  }

  public void setPostLoad(PostLoadConfig postLoad)
  {
    _postLoad = postLoad;
  }

  public AttributeOverrideConfig getAttributeOverride()
  {
    return _attributeOverride;
  }

  public void setAttributeOverride(AttributeOverrideConfig attributeOverride)
  {
    _attributeOverride = attributeOverride;
  }

  public AssociationOverrideConfig getAssociationOverride()
  {
    return _associationOverride;
  }

  public void setAssociationOverride(AssociationOverrideConfig associationOverride)
  {
    _associationOverride = associationOverride;
  }

  public String toString()
  {
    return "EntityConfig[" + _name + ", " + _className + "]";
  }
}
