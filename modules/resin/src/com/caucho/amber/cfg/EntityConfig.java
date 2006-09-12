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

import javax.sql.DataSource;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.config.ConfigException;


/**
 * <entity> tag in the orm.xml
 */
public class EntityConfig {
  // attributes
  private String _name;
  private String _className;
  private EntityMappingsConfig.AccessType _access;
  private boolean _isMetadataComplete;

  // elements
  /* XXX: TO DO...
  private String _description;
  private TableConfig _table;
  private SecondaryTableConfig _secondaryTable;
  private PrimaryKeyJoinColumnConfig _primaryKeyJoinColumn;
  private IdClassConfig _idClass;
  private InheritanceConfig _inheritance;
  private DiscriminatorValueConfig _discriminatorValue;
  private DiscriminatorColumnConfig _discriminatorColumn;
  private SequenceGeneratorConfig _sequenceGenerator;
  private TableGeneratorConfig _tableGenerator;
  private NamedQueryConfig _namedQuery;
  private NamedNativeQueryConfig _namedNativeQuery;
  private SqlResultSetMappingConfig _sqlResultSetMapping;
  private EmptyTypeConfig _excludeDefaultListeners;
  private EmptyTypeConfig _excludeSuperclassListeners;
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
  */

  private AttributesConfig _attributes;

  /**
   * Returns the access type.
   */
  public EntityMappingsConfig.AccessType getAccess()
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
  public void setAccess(EntityMappingsConfig.AccessType access)
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

  public String toString()
  {
    return "EntityConfig[" + _name + ", " + _className + "]";
  }
}
