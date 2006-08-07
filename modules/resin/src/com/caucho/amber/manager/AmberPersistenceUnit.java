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

package com.caucho.amber.manager;

import java.io.IOException;

import java.lang.ref.SoftReference;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import javax.sql.DataSource;

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.cfg.EntityIntrospector;

import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.EntityKey;

import com.caucho.amber.gen.AmberEnhancer;
import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.gen.AmberGeneratorImpl;

import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;

import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;

import com.caucho.amber.table.Table;

import com.caucho.amber.type.*;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.JavaClassGenerator;

import com.caucho.jdbc.JdbcMetaData;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.enhancer.EnhancingClassLoader;

import com.caucho.naming.Jndi;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Main interface between Resin and the connector.  It's the
 * top-level SPI class for creating the SPI ManagedConnections.
 *
 * The resource configuration in Resin's web.xml will use bean-style
 * configuration to configure the ManagecConnectionFactory.
 */
public class AmberPersistenceUnit {
  private static final L10N L = new L10N(AmberPersistenceUnit.class);
  private static final Logger log
    = Logger.getLogger(AmberPersistenceUnit.class.getName());

  private String _name;

  private AmberContainer _amberContainer;

  // Actual class is EntityManagerProxy, but EntityManager is JDK 1.5 dependent
  private Object _entityManagerProxy;

  // basic data source
  private DataSource _dataSource;

  // data source for read-only requests
  private DataSource _readDataSource;

  // data source for requests in a transaction
  private DataSource _xaDataSource;

  private JdbcMetaData _jdbcMetaData;

  private boolean _createDatabaseTables;
  private boolean _validateDatabaseTables = true;

  // private long _tableCacheTimeout = 250;
  private long _tableCacheTimeout = 2000;

  private TypeManager _typeManager = new TypeManager();

  private HashMap<String,Table> _tableMap =
    new HashMap<String,Table>();

  private HashMap<String,AmberEntityHome> _entityHomeMap =
    new HashMap<String,AmberEntityHome>();

  private HashMap<String,IdGenerator> _tableGenMap =
    new HashMap<String,IdGenerator>();

  private HashMap<String,SequenceIdGenerator> _sequenceGenMap =
    new HashMap<String,SequenceIdGenerator>();

  private LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>> _queryCache =
    new LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>>(1024);

  private LruCache<EntityKey,SoftReference<EntityItem>> _entityCache =
    new LruCache<EntityKey,SoftReference<EntityItem>>(32 * 1024);

  private EntityKey _entityKey = new EntityKey();

  private ArrayList<EntityType> _lazyConfigure = new ArrayList<EntityType>();

  private ArrayList<EntityType> _lazyGenerate = new ArrayList<EntityType>();
  private ArrayList<AmberEntityHome> _lazyHomeInit =
    new ArrayList<AmberEntityHome>();
  private ArrayList<Table> _lazyTable = new ArrayList<Table>();

  private HashMap<String,String> _namedQueryMap =
    new HashMap<String,String>();

  private EntityIntrospector _introspector;
  private AmberGenerator _generator;

  private boolean _supportsGetGeneratedKeys;

  private ThreadLocal<AmberConnection> _threadConnection
    = new ThreadLocal<AmberConnection>();

  private volatile boolean _isInit;

  private long _xid = 1;

  public AmberPersistenceUnit(AmberContainer container, String name)
  {
    _amberContainer = container;
    _name = name;

    _dataSource = container.getDataSource();
    _xaDataSource = container.getXADataSource();
    _readDataSource = container.getReadDataSource();

    _createDatabaseTables = container.getCreateDatabaseTables();

    _introspector = new EntityIntrospector(this);

    // needed to support JDK 1.4 compatibility
    try {
      bindProxy();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  private void bindProxy()
    throws Exception
  {
    Jndi.bindDeep(_amberContainer.getPersistenceUnitJndiPrefix() + getName(),
                  new FactoryProxy(this));

    Jndi.bindDeep(_amberContainer.getPersistenceContextJndiPrefix() + getName(),
                  new EntityManagerNamingProxy(this));
  }

  public EntityManager getEntityManager()
  {
    // return (EntityManager) _entityManagerProxy;

    return null;
  }

  public ClassLoader getEnhancedLoader()
  {
    return _amberContainer.getEnhancedLoader();
  }

  /**
   * Sets the data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the read data source.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _readDataSource = dataSource;
  }

  /**
   * Gets the read data source.
   */
  public DataSource getReadDataSource()
  {
    return _readDataSource;
  }

  /**
   * Sets the XA data source.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _xaDataSource = dataSource;
  }

  /**
   * Gets the xa data source.
   */
  public DataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * Returns the jdbc meta data.
   */
  public JdbcMetaData getMetaData()
  {
    if (_jdbcMetaData == null) {
      if (getDataSource() == null)
        throw new NullPointerException("No data-source specified for PersistenceUnit");

      _jdbcMetaData = JdbcMetaData.create(getDataSource());
    }

    return _jdbcMetaData;
  }

  /**
   * Set true if database tables should be created automatically.
   */
  public void setCreateDatabaseTables(boolean create)
  {
    _createDatabaseTables = create;
  }

  /**
   * Set true if database tables should be created automatically.
   */
  public boolean getCreateDatabaseTables()
  {
    return _createDatabaseTables;
  }

  /**
   * Set true if database tables should be validated automatically.
   */
  public void setValidateDatabaseTables(boolean validate)
  {
    _validateDatabaseTables = validate;
  }

  /**
   * Set true if database tables should be validated automatically.
   */
  public boolean getValidateDatabaseTables()
  {
    return _validateDatabaseTables;
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
   * Set false for EJB-style generation.
   */
  public void setBytecodeGenerator(boolean isBytecodeGenerator)
  {
    if (isBytecodeGenerator)
      _generator = _amberContainer.getGenerator();
    else
      _generator = new AmberGeneratorImpl(this);
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
  public JClassLoader getJClassLoader()
  {
    return _amberContainer.getJClassLoader();
  }

  /**
   * Creates a table.
   */
  public Table createTable(String tableName)
  {
    Table table = _tableMap.get(tableName);

    if (table == null) {
      table = new Table(this, tableName);
      table.setCacheTimeout(getTableCacheTimeout());

      _tableMap.put(tableName, table);

      _lazyTable.add(table);
    }

    return table;
  }

  public Throwable getConfigException()
  {
    return _amberContainer.getConfigException();
  }

  /**
   * Add an entity.
   */
  public void addEntityClass(String className)
    throws ConfigException
  {
    JClass type = getJClassLoader().forName(className);

    if (type == null) {
      throw new ConfigException(L.l("'{0}' is an unknown type",
                                    className));
    }

    boolean isEntity = type.getAnnotation(javax.persistence.Entity.class) != null;
    boolean isEmbeddable = type.getAnnotation(javax.persistence.Embeddable.class) != null;
    boolean isMappedSuperclass = type.getAnnotation(javax.persistence.MappedSuperclass.class) != null;
    if (! (isEntity || isEmbeddable || isMappedSuperclass)) {
      throw new ConfigException(L.l("'{0}' must implement javax.persistence.Entity, javax.persistence.Embeddable or javax.persistence.MappedSuperclass",
                                    className));
    }

    try {
      _introspector.introspect(type);
    } catch (Throwable e) {
      _amberContainer.addEntityException(className, e);

      throw new ConfigException(e);
    }

    EntityType entity = createEntity(type);

    _amberContainer.addEntity(className, entity);
  }

  /**
   * Adds a named query.
   */
  public void addNamedQuery(String name,
                            String query)
    throws ConfigException
  {
    if (_namedQueryMap.containsKey(name)) {
      throw new ConfigException(L.l("Named query '{0}': '{1}' is already defined.",
                                    name, query));
    }

    _namedQueryMap.put(name, query);
  }

  /**
   * Returns the named query statement.
   */
  public String getNamedQuery(String name)
  {
    return (String) _namedQueryMap.get(name);
  }

  /**
   * Adds an entity.
   */
  public EntityType createEntity(JClass beanClass)
  {
    return createEntity(beanClass.getName(), beanClass);
  }

  /**
   * Adds an entity.
   */
  public EntityType createEntity(String name, JClass beanClass)
  {
    return createEntity(name, beanClass, false);
  }

  /**
   * Adds an entity.
   */
  public EntityType createEntity(String name,
                                 JClass beanClass,
                                 boolean embeddable)
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
        entityType = new SubEntityType(this, parentType);
      else
        entityType = new EntityType(this);
    }

    // _typeManager.put(name, entityType);
    _typeManager.put(name, entityType);
    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), entityType);

    entityType.setName(name);
    entityType.setBeanClass(beanClass);

    entityType.setEmbeddable(embeddable);

    if (!embeddable) {
      _lazyConfigure.add(entityType);
      // getEnvManager().addLazyConfigure(entityType);

      AmberEntityHome entityHome = _entityHomeMap.get(beanClass.getName());

      if (entityHome == null) {
        entityHome = new AmberEntityHome(this, entityType);
        _lazyHomeInit.add(entityHome);
        _isInit = false;
      }

      addEntityHome(name, entityHome);
      // XXX: some confusion about the double entry, related to the EJB 3.0
      // confuction of named instances.
      addEntityHome(beanClass.getName(), entityHome);
    }

    return entityType;
  }

  /**
   * Adds a new home bean.
   */
  private void addEntityHome(String name, AmberEntityHome home)
  {
    _entityHomeMap.put(name, home);

    // getEnvManager().addEntityHome(name, home);
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
  public GeneratorTableType createGeneratorTable(String name)
  {
    Type type = _typeManager.get(name);

    if (type instanceof GeneratorTableType)
      return (GeneratorTableType) type;

    if (type != null)
      throw new RuntimeException(L.l("'{0}' is a duplicate generator table.",
                                     type));

    GeneratorTableType genType = new GeneratorTableType(this, name);

    _typeManager.put(name, genType);

    // _lazyGenerate.add(genType);

    return genType;
  }

  /**
   * Returns a sequence generator.
   */
  public SequenceIdGenerator createSequenceGenerator(String name, int size)
    throws ConfigException
  {
    synchronized (_sequenceGenMap) {
      SequenceIdGenerator gen = _sequenceGenMap.get(name);

      if (gen == null) {
        gen = new SequenceIdGenerator(this, name, size);

        _sequenceGenMap.put(name, gen);
      }

      return gen;
    }
  }

  /**
   * Configure lazy.
   */
  public void generate()
    throws Exception
  {
    configure();

    while (_lazyGenerate.size() > 0) {
      EntityType type = _lazyGenerate.remove(0);

      try {
        if (!type.isEmbeddable()) {
          type.init();

          getGenerator().generate(type);
        }
      } catch (Exception e) {
        type.setConfigException(e);

        _amberContainer.addEntityException(type.getBeanClass().getName(), e);

        throw e;
      }
    }

    try {
      initTables();

      getGenerator().compile();
    } catch (Exception e) {
      _amberContainer.addException(e);

      throw e;
    }
  }

  /**
   * Configure lazy.
   */
  public void generate(JavaClassGenerator javaGen)
    throws Exception
  {
    configure();

    while (_lazyGenerate.size() > 0) {
      EntityType type = _lazyGenerate.remove(0);

      type.init();

      if (type instanceof EntityType) {
        EntityType entityType = (EntityType) type;

        if (! entityType.isGenerated()) {
          if (entityType.getInstanceClassName() == null)
            throw new ConfigException(L.l("'{0}' does not have a configured instance class.",
                                          entityType));

          entityType.setGenerated(true);

          try {
            getGenerator().generateJava(javaGen, entityType);
          } catch (Throwable e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }

      configure();
    }

    for (SequenceIdGenerator gen : _sequenceGenMap.values())
      gen.init(this);
  }

  /**
   * Configure lazy.
   */
  public void configure()
    throws Exception
  {
    _introspector.configure();

    while (_lazyConfigure.size() > 0) {
      EntityType type = _lazyConfigure.remove(0);

      if (type.startConfigure()) {
        // getEnvManager().getGenerator().configure(type);
      }

      _introspector.configure();

      if (! _lazyGenerate.contains(type))
        _lazyGenerate.add(type);
    }
  }

  /**
   * Adds an entity.
   */
  public SubEntityType createSubEntity(JClass beanClass, EntityType parent)
  {
    SubEntityType entityType;
    entityType = (SubEntityType) _typeManager.get(beanClass.getName());

    if (entityType != null)
      return entityType;

    entityType = new SubEntityType(this, parent);
    entityType.setBeanClass(beanClass);

    _typeManager.put(entityType.getName(), entityType);

    addEntityHome(entityType.getName(), parent.getHome());

    return entityType;
  }

  /**
   * Returns the entity home.
   */
  public AmberEntityHome getEntityHome(String name)
  {
    if (! _isInit) {
      try {
        initEntityHomes();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new AmberRuntimeException(e);
      }
    }

    return _entityHomeMap.get(name);
  }

  /**
   * Returns the entity home by the schema name.
   */
  public AmberEntityHome getHomeBySchema(String name)
  {
    for (AmberEntityHome home : _entityHomeMap.values()) {
      if (name.equals(home.getEntityType().getName()))
        return home;
    }

    try {
      createType(name);
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
  public Type createType(String typeName)
    throws ConfigException
  {
    Type type = _typeManager.get(typeName);

    if (type != null)
      return type;

    JClass cl = _amberContainer.getJClassLoader().forName(typeName);

    if (cl == null)
      throw new ConfigException(L.l("'{0}' is an unknown type", typeName));

    return createType(cl);
  }

  /**
   * Creates a type.
   */
  public Type createType(JClass javaType)
    throws ConfigException
  {
    Type type = _typeManager.create(javaType);

    if (type != null)
      return type;

    return createEntity(javaType);
  }

  /**
   * Sets the generator.
   */
  public AmberGenerator getGenerator()
  {
    if (_generator != null)
      return _generator;
    /*
      else if (_enhancer != null)
      return _enhancer;
    */
    else {
      _generator = _amberContainer.getGenerator();

      return _generator;
    }
  }

  /**
   * Returns true if generated keys are allowed.
   */
  public boolean hasReturnGeneratedKeys()
  {
    return _supportsGetGeneratedKeys;
  }

  /**
   * Initialize the resource.
   */
  public void init()
    throws ConfigException, IOException
  {
    initLoaders();

    if (_dataSource == null)
      return;

    try {
      Connection conn = _dataSource.getConnection();

      try {
        DatabaseMetaData metaData = conn.getMetaData();

        try {
          _supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();
        } catch (Throwable e) {
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Initialize the resource.
   */
  public void initLoaders()
    throws ConfigException, IOException
  {
    // getEnvManager().initLoaders();
  }

  public void initEntityHomes()
    throws AmberException, ConfigException
  {
    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;
    }

    initTables();

    while (_lazyHomeInit.size() > 0) {
      AmberEntityHome home = _lazyHomeInit.remove(0);

      home.init();
    }
  }

  /**
   * Configure lazy.
   */
  public void initTables()
    throws ConfigException
  {
    while (_lazyTable.size() > 0) {
      Table table = _lazyTable.remove(0);

      if (getDataSource() == null)
        throw new ConfigException(L.l("No configured data-source found for <ejb-server>."));

      if (getCreateDatabaseTables())
        table.createDatabaseTable(this);

      if (getValidateDatabaseTables())
        table.validateDatabaseTable(this);
    }
  }

  /**
   * Returns the cache connection.
   */
  public CacheConnection getCacheConnection()
  {
    // cache connection cannot be reused (#998)
    return new CacheConnection(this);
  }

  /**
   * Returns the cache connection.
   */
  public AmberConnection createAmberConnection()
  {
    return new AmberConnection(this);
  }

  /**
   * Returns the thread's amber connection.
   */
  public AmberConnection getThreadConnection()
  {
    AmberConnection aConn = _threadConnection.get();

    if (aConn == null) {
      aConn = new AmberConnection(this);
      aConn.initThreadConnection();

      _threadConnection.set(aConn);
    }

    return aConn;
  }

  /**
   * Unset the thread's amber connection.
   */
  public void removeThreadConnection()
  {
    _threadConnection.set(null);
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

    return home.findEntityItem(getCacheConnection(), key, false);
  }

  /**
   * Returns the entity with the given key.
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
  }

  /**
   * New Version of getCreateTableSQL which returns
   * the SQL for the table with the given SQL type
   * but takes sqlType, length, precision, and scale.
   */
  public String getCreateColumnSQL(int sqlType, int length, int precision, int scale) {
    return getMetaData().getCreateColumnSQL(sqlType, length, precision, scale);
  }

  public String toString()
  {
    return "AmberPersistenceUnit[]";
  }
}
