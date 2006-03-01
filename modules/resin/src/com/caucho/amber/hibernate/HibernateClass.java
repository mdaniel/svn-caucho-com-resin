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

package com.caucho.amber.hibernate;

import java.util.ArrayList;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;
import com.caucho.bytecode.JClassWrapper;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.AmberManager;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyPropertyField;
import com.caucho.amber.field.CompositeId;
import com.caucho.amber.field.Discriminator;

import com.caucho.amber.table.Column;

/**
 * configuration for an entity
 */
public class HibernateClass {
  private static final L10N L = new L10N(HibernateClass.class);

  private HibernateMapping _mapping;
  protected AmberManager _amberManager;
  private EntityType _entityType;

  private String _table;
  private String _abstractSchema;

  HibernateClass(HibernateMapping mapping)
  {
    _mapping = mapping;
    _amberManager = mapping.getManager();
  }

  protected EntityType getEntityType()
  {
    return _entityType;
  }

  /**
   * Sets the class name.
   */
  public void setName(Class name)
  {
    _entityType = createEntity(name);
    _entityType.startConfigure();

    JClass jClass = new JClassWrapper(name, _amberManager.getJClassLoader());
    _entityType.setBeanClass(jClass);

    String className = name.getName();
    int p = className.lastIndexOf('.');

    String sqlTable = null;
    if (p > 0)
      sqlTable = className.substring(p + 1);
    else
      sqlTable = className;
    
    _entityType.setTable(_amberManager.createTable(sqlTable));

    _entityType.addDependency(_mapping.getDependency());
  }

  protected EntityType createEntity(Class cl)
  {
    JClass jClass = new JClassWrapper(cl, _amberManager.getJClassLoader());
    
    return _amberManager.createEntity(jClass);
  }

  /**
   * Sets the entity's abstract schema
   */
  public void setAbstractSchema(String schema)
  {
    _abstractSchema = schema;
  }

  /**
   * Sets the entity's table
   */
  public void setTable(String table)
  {
    _table = table;
  }

  /**
   * Sets the id.
   */
  public HibernateId createId()
  {
    return new HibernateId(_entityType);
  }

  /**
   * Sets the discriminator
   */
  public HibernateDiscriminator createDiscriminator()
  {
    return new HibernateDiscriminator(_entityType);
  }


  /**
   * Sets the id.
   */
  public HibernateCompositeId createCompositeId()
  {
    return new HibernateCompositeId(_entityType);
  }

  /**
   * Sets the property
   */
  public HibernateProperty createProperty()
  {
    return new HibernateProperty(_entityType);
  }

  /**
   * Sets the many-to-one
   */
  public HibernateManyToOne createManyToOne()
  {
    return new HibernateManyToOne(_entityType);
  }

  /**
   * Adds a bag
   */
  public HibernateBag createBag()
  {
    return new HibernateBag(_entityType);
  }

  /**
   * Adds a set
   */
  public HibernateBag createSet()
  {
    return new HibernateBag(_entityType);
  }

  /**
   * Adds a map
   */
  public HibernateMap createMap()
  {
    return new HibernateMap(_entityType);
  }

  /**
   * Adds a load group
   */
  public LoadGroup createLoadGroup()
  {
    return new LoadGroup(_entityType);
  }

  /**
   * Adds a subclass
   */
  public HibernateSubClass createSubclass()
  {
    return new HibernateSubClass(_mapping, this);
  }

  /**
   * Complete the class.
   */
  public void init()
  {
    if (_table != null)
      _entityType.setTable(_amberManager.createTable(_table));
    
    if (_abstractSchema != null)
      _entityType.setName(_abstractSchema);
  }

  public class LoadGroup {
    private EntityType _type;

    LoadGroup(EntityType type)
    {
      _type = type;
      type.nextDefaultLoadGroupIndex();
    }
    
    /**
     * Sets the property
     */
    public HibernateProperty createProperty()
    {
      return new HibernateProperty(_entityType);
    }
  }
  
  public static class HibernateId extends HibernateField {
    private KeyPropertyField _id;
    private HibernateGenerator _gen;
    
    HibernateId(EntityType type)
    {
      super(type);

      _id = new KeyPropertyField(type);
      _id.setGenerator("identity");
      
      setField(_id);
    }

    IdField getId()
    {
      return _id;
    }

    public void addGenerator(HibernateGenerator gen)
    {
      _id.setGenerator(gen.getGeneratorType());
    }

    public void init()
      throws ConfigException
    {
      super.init();
      
      _id.setColumn(getOwnerType().getTable().createColumn(_id.getName(),
							   getType()));
      _id.getColumn().setGeneratorType(_id.getGenerator());

      ArrayList<IdField> keys = new ArrayList<IdField>();
      keys.add(_id);

      getOwnerType().setId(new Id(getOwnerType(), keys));
    }
  }

  public static class HibernateDiscriminator extends HibernateProperty {
    private Discriminator _discriminator;
    
    HibernateDiscriminator(EntityType entityType)
    {
      super(entityType);

      _discriminator = new Discriminator(entityType);

      setField(_discriminator);
    }

    public void init()
      throws ConfigException
    {
      super.init();

      // getOwnerType().setDiscriminator(_discriminator);
      throw new UnsupportedOperationException();
    }
  }

  public static class HibernateCompositeId {
    EntityType _type;

    private Class _keyClass;

    ArrayList<IdField> _ids = new ArrayList<IdField>();
    ArrayList<Column> _columns = new ArrayList<Column>();
    
    HibernateCompositeId(EntityType type)
    {
      _type = type;
    }

    public void setClass(Class keyClass)
    {
      _keyClass = keyClass;
    }

    public HibernateId createKeyProperty()
    {
      return new HibernateId(_type);
    }

    public void addKeyProperty(HibernateId id)
    {
      _ids.add(id.getId());
      _columns.add(id.getId().getColumns().get(0));
    }

    public void init()
      throws ConfigException
    {
      CompositeId id = new CompositeId(_type, _ids);

      JClassLoader jClassLoader = _type.getAmberManager().getJClassLoader();
      
      JClass jKeyClass = new JClassWrapper(_keyClass, jClassLoader);
      
      id.setKeyClass(jKeyClass);

      //id.setColumn(new CompositeColumn(_columns));

      _type.setId(id);
    }
  }

  public static class HibernateGenerator {
    private String _generator;

    public HibernateGenerator()
    {
    }

    public void setClass(String type)
      throws ConfigException
    {
      if (! "identity".equals(type) && ! "max".equals(type))
	throw new ConfigException(L.l("'{0}' is an unknown generator",
				      type));

      _generator = type;
    }

    String getGeneratorType()
    {
      return _generator;
    }
  }
}
