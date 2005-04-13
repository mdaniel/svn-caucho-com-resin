/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.amber;

import java.io.IOException;

import java.lang.ref.SoftReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;

import com.caucho.loader.DynamicClassLoader;

import com.caucho.loader.enhancer.EnhancerManager;

import com.caucho.jdbc.JdbcMetaData;

import com.caucho.amber.connection.AmberConnectionImpl;
import com.caucho.amber.connection.CacheConnectionImpl;

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityKey;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.AmberCompletion;

import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.gen.AmberGeneratorImpl;
import com.caucho.amber.gen.AmberEnhancer;

import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;

import com.caucho.amber.table.Table;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.GeneratorTableType;
import com.caucho.amber.type.SubEntityType;
import com.caucho.amber.type.TypeManager;

import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.JavaClassGenerator;

import com.caucho.loader.enhancer.EnhancingClassLoader;

import com.caucho.log.Log;

import com.caucho.util.LruCache;
import com.caucho.util.L10N;

/**
 * Main interface between Resin and the connector.  It's the
 * top-level SPI class for creating the SPI ManagedConnections.
 *
 * The resource configuration in Resin's web.xml will use bean-style
 * configuration to configure the ManagecConnectionFactory.
 */
public class EnvAmberManager {
  private static final Logger log = Log.open(AmberManager.class);
  private static final L10N L = new L10N(AmberManager.class);

  private ClassLoader _parentLoader;

  private AmberEnhancer _enhancer;

  private long _tableCacheTimeout = 250;

  private JClassLoader _jClassLoader;

  private TypeManager _typeManager = new TypeManager();

  private HashMap<String,Table> _tableMap = new HashMap<String,Table>();

  private HashMap<String,AmberEntityHome> _entityHomeMap
    = new HashMap<String,AmberEntityHome>();

  private HashMap<String,IdGenerator> _tableGenMap
    = new HashMap<String,IdGenerator>();

  private HashMap<String,SequenceIdGenerator> _sequenceGenMap
    =  new HashMap<String,SequenceIdGenerator>();

  private LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>> _queryCache
    = new LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>>(1024);

  private LruCache<EntityKey,SoftReference<EntityItem>> _entityCache
    = new LruCache<EntityKey,SoftReference<EntityItem>>(32 * 1024);

  private EntityKey _entityKey = new EntityKey();

  private AmberGenerator _generator;

  private CacheConnectionImpl _cacheConn;

  private volatile boolean _isInit;

  private long _xid = 1;

  public EnvAmberManager()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader();
    _jClassLoader = EnhancerManager.create(_parentLoader).getJavaClassLoader();

    try {
      if (_parentLoader instanceof DynamicClassLoader)
	((DynamicClassLoader) _parentLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the default table cache time.
   */
  public void setTableCacheTimeout(long timeout)
  {
    _tableCacheTimeout = timeout;
  }

  /**
   * Get the default table cache time.
   */
  public long getTableCacheTimeout()
  {
    return _tableCacheTimeout;
  }

  /**
   * Returns a new xid.
   */
  public long getXid()
  {
    synchronized (this) {
      return _xid++;
    }
  }

  /**
   * Returns the enhanced loader.
   */
  public ClassLoader getEnhancedLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the enhanced loader.
   */
  public JClassLoader getJClassLoader()
  {
    return _jClassLoader;
  }

  /**
   * Creates a table.
   */
  public Table createTable(String tableName)
  {
    Table table = _tableMap.get(tableName);

    if (table == null) {
      table = new Table(tableName);
      table.setCacheTimeout(getTableCacheTimeout());
      
      _tableMap.put(tableName, table);

      // XXX: _lazyTable.add(table);
    }

    return table;
  }
  
  /**
   * Adds an entity.
   */
  public EntityType createEntity(JClass beanClass, AmberManager manager)
  {
    return createEntity(beanClass.getName(), beanClass, manager);
  }
  
  /**
   * Adds an entity.
   */
  public EntityType createEntity(String name,
				 JClass beanClass,
				 AmberManager manager)
  {
    EntityType entityType = (EntityType) _typeManager.get(name);

    if (entityType != null)
      return entityType;

    // ejb/0al2
    // entityType = (EntityType) _typeManager.get(beanClass.getName());
    
    if (entityType == null) {
      EntityType parentType = null;
      
      for (JClass parentClass = beanClass.getSuperClass();
	   parentType == null && parentClass != null;
	   parentClass = parentClass.getSuperClass()) {
	parentType = (EntityType) _typeManager.get(parentClass.getName());
      }

      if (parentType != null)
	entityType = new SubEntityType(manager, parentType);
      else
	entityType = new EntityType(manager);
    }
    
    // _typeManager.put(name, entityType);
    _typeManager.put(name, entityType);
    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), entityType);
    
    entityType.setName(name);
    entityType.setBeanClass(beanClass);

    // XXX: _lazyConfigure.add(entityType);

    AmberEntityHome entityHome = _entityHomeMap.get(beanClass.getName());

    if (entityHome == null) {
      entityHome = new AmberEntityHome(manager, entityType);
      // XXX: _lazyHomeInit.add(entityHome);
      _isInit = false;
    }

    _entityHomeMap.put(name, entityHome);
    // XXX: some confusion about the double entry, related to the EJB 3.0
    // confuction of named instances.
    _entityHomeMap.put(beanClass.getName(), entityHome);

    return entityType;
  }
  
  /**
   * Returns a table generator.
   */
  public IdGenerator getTableGenerator(String name)
  {
    return _tableGenMap.get(name);
  }
  
  /**
   * Sets a table generator.
   */
  public IdGenerator putTableGenerator(String name, IdGenerator gen)
  {
    synchronized (_tableGenMap) {
      IdGenerator oldGen = _tableGenMap.get(name);

      if (oldGen != null)
	return oldGen;
      else {
	_tableGenMap.put(name, gen);
	return gen;
      }
    }
  }
  
  /**
   * Adds a generator table.
   */
  public GeneratorTableType createGeneratorTable(String name,
						 AmberManager manager)
  {
    Type type = _typeManager.get(name);

    if (type instanceof GeneratorTableType)
      return (GeneratorTableType) type;

    if (type != null)
      throw new RuntimeException(L.l("'{0}' is a duplicate generator table.",
				     type));

    GeneratorTableType genType = new GeneratorTableType(manager, name);

    _typeManager.put(name, genType);

    // _lazyGenerate.add(genType);

    return genType;
  }
  
  /**
   * Returns a sequence generator.
   */
  public SequenceIdGenerator createSequenceGenerator(String name,
						     int size,
						     AmberManager manager)
    throws ConfigException
  {
    synchronized (_sequenceGenMap) {
      SequenceIdGenerator gen = _sequenceGenMap.get(name);

      if (gen == null) {
	gen = new SequenceIdGenerator(manager, name, size);

	_sequenceGenMap.put(name, gen);
      }

      return gen;
    }
  }
  
  /**
   * Adds an entity.
   */
  public SubEntityType createSubEntity(JClass beanClass,
				       EntityType parent,
				       AmberManager manager)
  {
    SubEntityType entityType;
    entityType = (SubEntityType) _typeManager.get(beanClass.getName());

    if (entityType != null)
      return entityType;

    entityType = new SubEntityType(manager, parent);
    entityType.setBeanClass(beanClass);

    _typeManager.put(entityType.getName(), entityType);
    
    _entityHomeMap.put(entityType.getName(), parent.getHome());

    return entityType;
  }

  /**
   * Returns the entity home.
   */
  public AmberEntityHome getEntityHome(String name)
  {
    if (! _isInit) {
      /* XXX:
      try {
	initEntityHomes();
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new AmberRuntimeException(e);
      }
      */
    }
    
    return _entityHomeMap.get(name);
  }

  /**
   * Returns the entity home by the schema name.
   */
  public AmberEntityHome getHomeBySchema(String name, AmberManager manager)
  {
    for (AmberEntityHome home : _entityHomeMap.values()) {
      if (name.equals(home.getEntityType().getName()))
	return home;
    }

    try {
      createType(name, manager);
    } catch (Throwable e) {
    }

    return _entityHomeMap.get(name);
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntity(String className)
  {
    Type type = _typeManager.get(className);

    if (type instanceof EntityType)
      return (EntityType) type;
    else
      return null;
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntityByInstanceClass(String className)
  {
    return _typeManager.getEntityByInstanceClass(className);
  }

  /**
   * Creates a type.
   */
  public Type createType(String typeName, AmberManager manager)
    throws ConfigException
  {
    Type type = _typeManager.get(typeName);
    
    if (type != null)
      return type;

    JClass cl = _jClassLoader.forName(typeName);

    if (cl == null)
      throw new ConfigException(L.l("'{0}' is an unknown type", typeName));

    return createType(cl, manager);
  }

  /**
   * Creates a type.
   */
  public Type createType(JClass javaType, AmberManager manager)
    throws ConfigException
  {
    Type type = _typeManager.create(javaType);

    if (type != null)
      return type;

    return createEntity(javaType, manager);
  }

  /**
   * Sets the generator.
   */
  public void setGenerator(AmberGenerator generator)
  {
    _generator = generator;
  }

  /**
   * Sets the generator.
   */
  public AmberGenerator getGenerator()
  {
    if (_generator == null) {
      _generator = new AmberGeneratorImpl();
      // XXX: _generator.setAmberManager(this);
    }

    return _generator;
  }

  /**
   * Initialize the resource.
   */
  public void initLoaders()
    throws ConfigException, IOException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (_enhancer == null) {
      // ejb/0880
      /*
      if (envLoader.getOwner() == null)
	return;
      */
      
      _enhancer = new AmberEnhancer();
      // XXX: _enhancer.setAmberManager(this);

      EnhancerManager.create().addClassEnhancer(_enhancer);
    }
  }

  /**
   * Initialize the resource.
   */
  public void init()
    throws ConfigException, IOException
  {
    initLoaders();

    // XXX: getGenerator().setAmberManager(this);
  }

  /**
   * Returns an EntityHome.
   */
  public AmberEntityHome getHome(Class cl)
  {
    return getEntityHome(cl.getName());
  }

  /**
   * Returns the query result.
   */
  public ResultSetCacheChunk getQueryChunk(QueryCacheKey key)
  {
    SoftReference<ResultSetCacheChunk> ref = _queryCache.get(key);

    if (ref == null)
      return null;
    else {
      ResultSetCacheChunk chunk = ref.get();

      if (chunk != null && chunk.isValid())
	return chunk;
      else
	return null;
    }
  }

  /**
   * Sets the query result.
   */
  public void putQueryChunk(QueryCacheKey key, ResultSetCacheChunk chunk)
  {
    _queryCache.put(key, new SoftReference<ResultSetCacheChunk>(chunk));
  }

  /**
   * Returns the entity item.
   */
  public EntityItem getEntityItem(String homeName, Object key)
    throws AmberException
  {
    AmberEntityHome home = getEntityHome(homeName);

    // return home.findEntityItem(getCacheConnection(), key, false);

    return null; // XXX:
  }

  /**
   * Returns the query result.
   */
  public EntityItem getEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType, key);
      ref = _entityCache.get(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Sets the entity result.
   */
  public EntityItem putEntity(EntityType rootType,
			      Object key,
			      EntityItem entity)
  {
    SoftReference<EntityItem> ref = new SoftReference<EntityItem>(entity);
    EntityKey entityKey = new EntityKey(rootType, key);
    
    ref = _entityCache.putIfNew(entityKey, ref);

    return ref.get();
  }

  /**
   * Remove the entity result.
   */
  public EntityItem removeEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType, key);
      ref = _entityCache.remove(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Completions affecting the cache.
   */
  public void complete(ArrayList<AmberCompletion> completions)
  {
    int size = completions.size();
    if (size == 0)
      return;

    synchronized (_entityCache) {
      Iterator<LruCache.Entry<EntityKey,SoftReference<EntityItem>>> iter;

      iter = _entityCache.iterator();
      while (iter.hasNext()) {
	LruCache.Entry<EntityKey,SoftReference<EntityItem>> entry;
	entry = iter.next();

	EntityKey key = entry.getKey();
	SoftReference<EntityItem> valueRef = entry.getValue();
	EntityItem value = valueRef.get();

	if (value == null)
	  continue;

	EntityType entityRoot = key.getEntityType();
	Object entityKey = key.getKey();

	for (int i = 0; i < size; i++) {
	  if (completions.get(i).complete(entityRoot, entityKey, value)) {
	    // XXX: delete
	  }
	}
      }
    }

    synchronized (_queryCache) {
      Iterator<SoftReference<ResultSetCacheChunk>> iter;

      iter = _queryCache.values();
      while (iter.hasNext()) {
	SoftReference<ResultSetCacheChunk> ref = iter.next();

	ResultSetCacheChunk chunk = ref.get();

	if (chunk != null) {
	  for (int i = 0; i < size; i++) {
	    if (completions.get(i).complete(chunk)) {
	      // XXX: delete
	    }
	  }
	}
      }
    }
  }

  /**
   * destroys the manager.
   */
  public void destroy()
  {
    _typeManager = null;
    _queryCache = null;
    _entityCache = null;
    _parentLoader = null;
  }

  public String toString()
  {
    return "EnvAmberManager[]";
  }
}
