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

import com.caucho.config.ConfigException;

import com.caucho.vfs.Path;

/**
 * Top <entity-mappings> tag in the orm.xml
 */
public class EntityMappingsConfig {
  private Path _root;

  // attributes
  private String _version;

  // elements
  private String _description;
  private String _package;
  private String _schema;
  private String _catalog;
  public enum AccessType { PROPERTY, FIELD };
  private AccessType _access;
  private HashMap<String, EntityConfig> _entityMap
    = new HashMap<String, EntityConfig>();

  // XXX: to do ...
  /*
  private PersistenceUnitMetaDataConfig _PersistenceUnitMetaDataConfig;
  private ArrayList<SequenceGeneratorConfig> _sequenceGeneratorList
    = new ArrayList<SequenceGeneratorConfig>();
  private ArrayList<TableGeneratorConfig> _tableGeneratorList
    = new ArrayList<TableGeneratorConfig>();
  private ArrayList<NamedQueryConfig> _namedQueryList
    = new ArrayList<NamedQueryConfig>();
  private ArrayList<NamedNativeQueryConfig> _namedNativeQueryList
    = new ArrayList<NamedNativeQueryConfig>();
  private ArrayList<SqlResultSetMappingConfig> _sqlResultSetMappingList
    = new ArrayList<SqlResultSetMappingConfig>();
  private ArrayList<MappedSuperclassConfig> _mappedSuperclassList
    = new ArrayList<MappedSuperclassConfig>();
  private ArrayList<EmbeddableConfig> _embeddableList
    = new ArrayList<EmbeddableConfig>();
  */

  public AccessType getAccess()
  {
    return _access;
  }

  public String getCatalog()
  {
    return _catalog;
  }

  public String getDescription()
  {
    return _description;
  }

  public String getPackage()
  {
    return _package;
  }

  public Path getRoot()
  {
    return _root;
  }

  public String getSchema()
  {
    return _schema;
  }

  public String getVersion()
  {
    return _version;
  }

  public void setAccess(AccessType access)
  {
    _access = access;
  }

  public void setCatalog(String catalog)
  {
    _catalog = catalog;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public void setPackage(String packageName)
  {
    _package = packageName;
  }

  public void setRoot(Path root)
  {
    _root = root;
  }

  public void setSchema(String schema)
  {
    _schema = schema;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  /**
   * Adds a new <entity>.
   */
  public void addEntity(EntityConfig entity)
  {
    _entityMap.put(entity.getClassName(), entity);
  }

  /**
   * Returns an entity config.
   */
  public EntityConfig getEntityConfig(String name)
  {
    return _entityMap.get(name);
  }

  /**
   * Returns the entity map.
   */
  public HashMap<String, EntityConfig> getEntityMap()
  {
    return _entityMap;
  }

  public String toString()
  {
    return _entityMap.toString();
  }
}
