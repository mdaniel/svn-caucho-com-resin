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

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberObjectNotFoundException;
import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.EntityResultConfig;
import com.caucho.amber.cfg.NamedNativeQueryConfig;
import com.caucho.amber.cfg.SqlResultSetMappingConfig;
import com.caucho.amber.collection.AmberCollection;
import com.caucho.amber.entity.*;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.ResultSetCacheChunk;
import com.caucho.amber.query.UserQuery;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.jca.BeginResource;
import com.caucho.jca.CloseResource;
import com.caucho.jca.UserTransactionProxy;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.PersistenceException;
import javax.persistence.EntityExistsException;
import javax.persistence.TransactionRequiredException;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The entity manager from a entity manager proxy.
 */
public class AmberConnection
  implements BeginResource, CloseResource, Synchronization
{
  private static final L10N L = new L10N(AmberConnection.class);
  private static final Logger log
    = Logger.getLogger(AmberConnection.class.getName());

  private AmberPersistenceUnit _persistenceUnit;

  private boolean _isRegistered;
  private boolean _isThreadConnection;

  private ArrayList<Entity> _entities = new ArrayList<Entity>();

  private ArrayList<Entity> _txEntities = new ArrayList<Entity>();

  private ArrayList<AmberCompletion> _completionList
    = new ArrayList<AmberCompletion>();

  private ArrayList<AmberCollection> _queries
    = new ArrayList<AmberCollection>();

  private EntityTransaction _trans;

  private long _xid;
  private boolean _isInTransaction;
  private boolean _isXA;

  private Connection _conn;
  private Connection _readConn;

  private boolean _isAutoCommit = true;

  private int _depth;

  private LruCache<String,PreparedStatement> _preparedStatementMap
    = new LruCache<String,PreparedStatement>(32);

  private ArrayList<Statement> _statements = new ArrayList<Statement>();

  private QueryCacheKey _queryKey = new QueryCacheKey();

  /**
   * Creates a manager instance.
   */
  AmberConnection(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
  }

  /**
   * Returns the persistence unit.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return _persistenceUnit;
  }

  /**
   * Set true for a threaded connection.
   */
  public void initThreadConnection()
  {
    _isThreadConnection = true;

    initJta();
  }

  public void initJta()
  {
    // non-jta connections do not register with the local transaction
    if (_persistenceUnit.isJta())
      register();
  }

  /**
   * @PrePersist callback for default listeners and
   * entity listeners.
   */
  public void prePersist(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.PRE_PERSIST, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PostPersist callback for default listeners and
   * entity listeners.
   */
  public void postPersist(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.POST_PERSIST, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PreRemove callback for default listeners and
   * entity listeners.
   */
  public void preRemove(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.PRE_REMOVE, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PostRemove callback for default listeners and
   * entity listeners.
   */
  public void postRemove(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.POST_REMOVE, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PreUpdate callback for default listeners and
   * entity listeners.
   */
  public void preUpdate(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.PRE_UPDATE, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PostUpdate callback for default listeners and
   * entity listeners.
   */
  public void postUpdate(Entity entity)
  {

    try {
      _persistenceUnit.callListeners(Listener.POST_UPDATE, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * @PostLoad callback for default listeners and
   * entity listeners.
   */
  public void postLoad(Entity entity)
  {
    try {
      _persistenceUnit.callListeners(Listener.POST_LOAD, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Makes the instance managed.
   */
  public void persist(Object entityObject)
  {
    try {
      if (entityObject == null)
        return;

      Entity entity = checkEntityType(entityObject, "persist");

      checkTransactionRequired("persist");

      persistInternal(entity, false);
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Makes the instance managed called
   * from cascading operations.
   */
  public void persistFromCascade(Object entity)
  {
    try {
      if (entity == null)
        return;

      persistInternal(entity, true);
    } catch (EntityExistsException e) {
      log.log(Level.FINER, e.toString(), e);
      // This is not an issue. It is the cascading
      // operation trying to persist the source
      // entity from the destination end.
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entityT)
  {
    try {
      if (entityT == null)
        return null;

      Entity entity = checkEntityType(entityT, "merge");

      flushInternal();

      EntityState state = entity.__caucho_getEntityState();

      Object pk = entity.__caucho_getPrimaryKey();

      log.finest(L.l("merge(class: '{0}' PK: '{1}' state: '{2}')",
                     entity.getClass().getName(), pk, state));

      if (EntityState.P_DELETING.ordinal() <= state.ordinal()) {
        // removed entity instance
        throw new IllegalArgumentException(L.l("Merge operation cannot be applied to a removed entity instance"));
      }

      try {
        Entity managedEntity = null;

        try {
          managedEntity = (Entity) load(entity.getClass(), pk);
        } catch (AmberObjectNotFoundException e) {
          log.log(Level.FINEST, e.toString(), e);
          // JPA: should not throw at all, returns null only.
        }

        // jpa/0s2k
        setTransactionalState(entity);

        if (managedEntity == null) {
          // new entity instance

          managedEntity = entity.getClass().newInstance();
          entity.__caucho_copyTo(managedEntity, this);

          // XXX: needs to do a pre-persist anyways at the beginning of merge,
          // but needs to check if the dependent entities exist.

          // cascade children
          entity.__caucho_cascadePrePersist(this);

          persist(managedEntity);

          log.finest(L.l("merged to a new entity (persisted)"));
        }
        else {
          // jpa/0ga3: existing entity
          entity.__caucho_copyTo(managedEntity, this);

          // XXX: needs to update the managed entity load mask in the copyTo???

          // jpa/0h08: sets the corresponding load/dirty masks to the argument entity.
          entity.__caucho_copyDirtyMaskFrom(managedEntity);
          entity.__caucho_copyLoadMaskFrom(managedEntity);

          entityT = (T) managedEntity;

          log.finest(L.l("merged to an existing entity"));
        }

        // jpa/0i5g
        entity.__caucho_cascadePostPersist(this);

      } catch (Exception e) {
        if (e.getCause() instanceof SQLException) {
          // OK: entity exists in the database
          log.log(Level.FINER, e.toString(), e);
        }
        else {
          throw e;
        }
      }

      // XXX: merge recursively for
      // cascade=MERGE or cascade=ALL

      return entityT;

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Remove the instance.
   */
  public void remove(Object entity)
  {
    try {
      if (entity == null)
        return;

      Entity instance = checkEntityType(entity, "remove");

      checkTransactionRequired("remove");

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("removing entity class " + instance.getClass().getName() +
                       " PK: " + instance.__caucho_getPrimaryKey()));

      // jpa/0k12
      if (instance.__caucho_getConnection() == null) {
        if (instance.__caucho_getEntityType() == null) {
          if (log.isLoggable(Level.FINEST))
            log.finest(L.l("remove is ignoring entity; performing only cascade post-remove"));

          // Ignore this entity; only post-remove child entities.
          instance.__caucho_cascadePostRemove(this);

          // jpa/0ga7
          return;
        }
        else
          throw new IllegalArgumentException(L.l("remove() operation can only be applied to a managed entity. This entity instance '{0}' PK: '{1}' is detached which means it was probably removed or needs to be merged.", instance.getClass().getName(), instance.__caucho_getPrimaryKey()));
      }

      EntityState state = instance.__caucho_getEntityState();

      if (EntityState.P_DELETING.ordinal() <= state.ordinal()) {
        if (log.isLoggable(Level.FINEST))
          log.finest(L.l("remove is ignoring entity in state " + state));

        return;
      }

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("remove is flushing any lazy cascading operation"));

      // jpa/1620
      // In particular, required for cascading persistence, since the cascade
      // is lazy until flush
      // jpa/0h25 flushInternal();
      // Only flushes this entity.
      instance.__caucho_flush();

      Object oldEntity;

      int index = getEntity(instance.getClass().getName(),
                            instance.__caucho_getPrimaryKey());

      // jpa/0ga4
      if (index < 0)
        throw new IllegalArgumentException(L.l("remove() operation can only be applied to a managed entity instance."));

      oldEntity = _entities.get(index);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("remove is performing cascade pre-remove"));

      // Pre-remove child entities.
      instance.__caucho_cascadePreRemove(this);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("remove is performing delete on the target entity"));

      delete(instance);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("remove is performing cascade post-remove"));

      // jpa/0o30
      // Post-remove child entities.
      instance.__caucho_cascadePostRemove(this);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("DONE successful remove for entity class " +
                       instance.getClass().getName() +
                       " PK: " + instance.__caucho_getPrimaryKey()));

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Find by the primary key.
   */
  /*
    public Object find(String entityName, Object primaryKey)
    {
    try {
    return load(entityName, primaryKey);
    } catch (RuntimeException e) {
    throw e;
    } catch (Exception e) {
    throw new EJBExceptionWrapper(e);
    }
    }
  */

  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass,
                    Object primaryKey)
  {
    try {
      AmberEntityHome entityHome
        = _persistenceUnit.getEntityHome(entityClass.getName());

      if (entityHome == null) {
        throw new IllegalArgumentException(L.l("find() operation can only be applied if the entity class is specified in the scope of a persistence unit."));
      }

      return (T) load(entityClass, primaryKey);
    } catch (AmberObjectNotFoundException e) {
      if (_persistenceUnit.isJPA()) {
        // JPA: should not throw at all, returns null only.
        // log.log(Level.FINER, e.toString(), e);
        return null;
      }

      // ejb/0604
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Find by the primary key.
   */
  public <T> T getReference(Class<T> entityClass, Object primaryKey)
    throws EntityNotFoundException, IllegalArgumentException
  {
    T reference = null;

    try {
      // XXX: only needs to get a reference.

      reference = find(entityClass, primaryKey);

      if (reference == null)
        throw new EntityNotFoundException(L.l("entity with primary key {0} not found in getReference()", primaryKey));

      if (! (entityClass.isAssignableFrom(Entity.class)))
        throw new IllegalArgumentException(L.l("getReference() operation can only be applied to an entity class"));

      return reference;

    } catch (EntityNotFoundException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Clears the connection
   */
  public void clear()
  {
    _entities.clear();
    _txEntities.clear();
  }

  /**
   * Creates a query.
   */
  public Query createQuery(String sql)
  {
    try {
      AbstractQuery queryProgram = parseQuery(sql, false);

      return new QueryImpl(queryProgram, this);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String name)
  {
    String sql = _persistenceUnit.getNamedQuery(name);

    if (sql != null)
      return createQuery(sql);

    NamedNativeQueryConfig nativeQuery
      = _persistenceUnit.getNamedNativeQuery(name);

    sql = nativeQuery.getQuery();

    String resultSetMapping = nativeQuery.getResultSetMapping();

    if (! ((resultSetMapping == null) || "".equals(resultSetMapping)))
      return createNativeQuery(sql, resultSetMapping);

    String resultClass = nativeQuery.getResultClass();

    AmberEntityHome entityHome
      = _persistenceUnit.getEntityHome(resultClass);

    EntityType entityType = entityHome.getEntityType();

    try {
      return createNativeQuery(sql, entityType.getInstanceClass());
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql)
  {
    try {
      QueryImpl query = new QueryImpl(this);

      query.setNativeSql(sql);

      return query;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql, String map)
  {
    // jpa/0y1-

    SqlResultSetMappingConfig resultSet;

    resultSet = _persistenceUnit.getSqlResultSetMapping(map);

    if (resultSet == null)
      throw new IllegalArgumentException(L.l("createNativeQuery() cannot create a native query for a result set named '{0}'", map));

    return createInternalNativeQuery(sql, resultSet);
  }

  /**
   * Creates an instance of the native query
   */
  public Query createNativeQuery(String sql, Class type)
  {
    SqlResultSetMappingConfig resultSet
      = new SqlResultSetMappingConfig();

    EntityResultConfig entityResult
      = new EntityResultConfig();

    entityResult.setEntityClass(type.getName());

    resultSet.addEntityResult(entityResult);

    return createInternalNativeQuery(sql, resultSet);
  }

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity)
  {
    try {
      if (entity == null)
        return;

      if (! (entity instanceof Entity))
        throw new IllegalArgumentException(L.l("refresh() operation can only be applied to an entity instance. This object is of class '{0}'", entity.getClass().getName()));

      checkTransactionRequired("refresh");

      Entity instance = (Entity) entity;

      String className = instance.getClass().getName();
      Object pk = instance.__caucho_getPrimaryKey();

      int index = getEntity(className, pk);

      if (index >= 0) {
        EntityState state = instance.__caucho_getEntityState();

        if (state.ordinal() <= EntityState.TRANSIENT.ordinal()
            || EntityState.P_DELETING.ordinal() <= state.ordinal()) {
          throw new IllegalArgumentException(L.l("refresh() operation can only be applied to a managed entity instance. The entity state is '{0}' for object of class '{0}' with PK '{1}'", className, pk, state == EntityState.TRANSIENT ? "TRANSIENT" : "DELETING or DELETED"));
        }
      }
      else
        throw new IllegalArgumentException(L.l("refresh() operation can only be applied to a managed entity instance. There was no managed instance of class '{0}' with PK '{1}'", className, pk));

      // Reset and refresh state.
      instance.__caucho_expire();
      instance.__caucho_makePersistent(this, (EntityType) null);
      instance.__caucho_retrieve(this);
    } catch (SQLException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Returns the flush mode.
   */
  public FlushModeType getFlushMode()
  {
    return FlushModeType.AUTO;
  }

  /**
   * Returns the flush mode.
   */
  public void setFlushMode(FlushModeType mode)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Locks the object.
   */
  public void lock(Object entity, LockModeType lockMode)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the transaction.
   */
  public EntityTransaction getTransaction()
  {
    if (_isXA)
      throw new IllegalStateException(L.l("Cannot call EntityManager.getTransaction() inside a distributed transaction."));

    if (_trans == null)
      _trans = new EntityTransactionImpl();

    return _trans;
  }

  /**
   * Returns true if open.
   */
  public boolean isOpen()
  {
    return _persistenceUnit != null;
  }

  /**
   * Registers with the local transaction.
   */
  void register()
  {
    if (! _isRegistered) {
      UserTransactionProxy.getInstance().enlistCloseResource(this);
      UserTransactionProxy.getInstance().enlistBeginResource(this);
    }

    _isRegistered = true;
  }

  /**
   * Joins the transaction.
   */
  public void joinTransaction()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the delegate.
   */
  public Object getDelegate()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the context.
   */
  public void close()
  {
    if (_persistenceUnit == null)
      return;

    try {
      if (_isThreadConnection)
        _persistenceUnit.removeThreadConnection();

      _isRegistered = false;

      cleanup();
    } finally {
      _persistenceUnit = null;
    }
  }

  /**
   * Returns the amber manaber.
   */
  public AmberPersistenceUnit getAmberManager()
  {
    return _persistenceUnit;
  }

  /**
   * Registers a collection.
   */
  public void register(AmberCollection query)
  {
    _queries.add(query);
  }

  /**
   * Adds a completion
   */
  public void addCompletion(AmberCompletion completion)
  {
    if (! _completionList.contains(completion))
      _completionList.add(completion);
  }

  /**
   * Returns true if a transaction is active.
   */
  public boolean isInTransaction()
  {
    return _isInTransaction;
  }

  /**
   * Returns the cache chunk size.
   */
  public int getCacheChunkSize()
  {
    return 25;
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl,
                     Object key)
    throws AmberException
  {
    log.finest(L.l("loading entity class " + cl.getName() + " PK: " + key));

    Entity entity = null;

    if (key == null)
      return null;

    // ejb/0d01, jpa/0gh0, jpa/0g0k
    // if (shouldRetrieveFromCache())
    int index = getEntity(cl.getName(), key);

    if (index >= 0) {
      entity = _entities.get(index);

      if (! isInTransaction()) {
        log.finest(L.l("load returning existing entity not in transaction"));

        return entity;
      }

      // jpa/0g0k setTransactionalState(entity);
      /*
        if (entity.__caucho_getEntityState() == EntityState.P_TRANSACTIONAL) {
        return entity;
        }
      */
      if (entity.__caucho_getEntityState().isTransactional()) {
        log.finest(L.l("load returning existing entity in transactional state"));

        return entity;
      }

      // jpa/0j5f
    }

    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;
    else {
      try {
        entityHome.init();
      } catch (ConfigException e) {
        throw new AmberException(e);
      }

      entity = entityHome.load(this, key);

      if (entity == null)
        return null;

      // jpa/0o36: eager loading can change the corresponding entity index.
      index = getEntity(cl.getName(), key);

      if (index >= 0) {
        // jpa/0ga9: replace with managed entity.
        _entities.remove(index);
      }

      addEntity(entity);

      index = getTransactionEntity(entity.getClass().getName(),
                                   entity.__caucho_getPrimaryKey());

      // XXX: jpa/0v33
      if (index >= 0) {
        setTransactionalState(_txEntities.get(index));
      }

      return entity;
    }
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(String entityName,
                     Object key)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(entityName);

    if (entityHome == null)
      return null;

    Entity entity = null;

    // XXX: ejb/0d01
    // jpa/0y14 if (shouldRetrieveFromCache())
    int index = getEntity(entityName, key);

    if (index >= 0)
      return _entities.get(index);

    try {
      entityHome.init();
    } catch (ConfigException e) {
      throw new AmberException(e);
    }

    entity = entityHome.load(this, key);

    addEntity(entity);

    return entity;
  }

  /**
   * Returns the entity for the connection.
   */
  public Entity getEntity(EntityItem item)
  {
    Entity itemEntity = item.getEntity();
    EntityType entityType = itemEntity.__caucho_getEntityType();

    int index = getEntity(entityType.getBeanClass().getName(),
                          itemEntity.__caucho_getPrimaryKey());

    if (index >= 0)
      return _entities.get(index);
    else {
      Entity entity = item.copy(this);

      addEntity(entity);

      return entity;
    }
  }

  /**
   * Loads the object based on itself.
   */
  public Object makePersistent(Object obj)
    throws SQLException
  {
    Entity entity = (Entity) obj;

    // check to see if exists

    if (entity == null)
      throw new NullPointerException();

    Class cl = entity.getClass();

    // Entity oldEntity = getEntity(cl, entity.__caucho_getPrimaryKey());

    AmberEntityHome entityHome;
    entityHome = _persistenceUnit.getEntityHome(entity.getClass().getName());

    if (entityHome == null)
      throw new AmberException(L.l("entity has no matching home"));

    entityHome.makePersistent(entity, this, false);

    return entity;
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(Class cl, String name, Object key)
  {
    return loadLazy(cl.getName(), name, key);
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(String className, String name, Object key)
  {
    if (key == null)
      return null;

    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", className));

      home.init();

      Object obj = home.loadLazy(this, key);

      Entity entity = (Entity) obj;

      addEntity(entity);

      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } catch (ConfigException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public EntityItem findEntityItem(String name, Object key)
  {
    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return home.findEntityItem(_persistenceUnit.getCacheConnection(), key, false);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public EntityItem setEntityItem(String name, Object key, EntityItem item)
  {
    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return home.setEntityItem(key, item);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(String name,
                          Object key)
  {
    if (key == null)
      return null;

    AmberEntityHome home = _persistenceUnit.getEntityHome(name);

    if (home == null)
      throw new RuntimeException(L.l("no matching home for {0}", name));

    return loadProxy(home.getEntityType(), key);
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(EntityType type,
                          Object key)
  {
    if (key == null)
      return null;

    try {
      AmberEntityHome home = type.getHome();

      EntityItem item = home.findEntityItem(this, key, false);

      if (item == null)
        return null;

      EntityFactory factory = home.getEntityFactory();

      Object entity = factory.getEntity(this, item);

      if (_persistenceUnit.isJPA()) {
        // jpa/0h29: eager loading.
        Entity instance = (Entity) entity;
        setTransactionalState(instance);
      }

      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return load(cl, key);
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object loadLazy(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return loadLazy(cl, cl.getName(), key);
  }

  /**
   * Matches the entity.
   */
  public int getEntity(String className, Object key)
  {
    for (int i = _entities.size() - 1; i >= 0; i--) {
      Entity entity = _entities.get(i);

      if (entity.__caucho_match(className, key)) {
        return i;
      }
    }

    return -1;
  }

  public int getTransactionEntity(String className, Object key)
  {
    for (int i = _txEntities.size() - 1; i >= 0; i--) {
      Entity entity = _txEntities.get(i);

      if (entity.__caucho_match(className, key)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Adds an entity.
   */
  public boolean addEntity(Entity entity)
  {
    boolean added = false;

    Object pk = entity.__caucho_getPrimaryKey();

    int index = getEntity(entity.getClass().getName(), pk);

    // jpa/0s2d: if (! _entities.contains(entity)) {
    if (index < 0) {
      _entities.add(entity);
      added = true;
    }

    // jpa/0g06
    if (_isInTransaction) {
      index = getTransactionEntity(entity.getClass().getName(), pk);

      // jpa/0s2d: if (! _txEntities.contains(entity)) {
      if (index < 0) {
        _txEntities.add(entity);
        added = true;
      }
    }

    return added;
  }

  /**
   * Removes an entity.
   */
  public boolean removeEntity(Entity entity)
  {
    _entities.remove(entity);

    if (_isInTransaction)
      _txEntities.remove(entity);

    return true;
  }

  /**
   * Loads the object based on itself.
   */
  public boolean contains(Object obj)
  {
    if (obj == null)
      return false;

    if (! (obj instanceof Entity))
      throw new IllegalArgumentException(L.l("contains() operation can only be applied to an entity instance."));

    Entity entity = (Entity) obj;

    // jpa/11a8
    if (! _entities.contains(entity))
      return false;

    /*
      entity = getEntity(entity.getClass().getName(),
      entity.__caucho_getPrimaryKey());
    */

    if (entity == null)
      return false;

    EntityState state = entity.__caucho_getEntityState();
    if (isInTransaction()) {
      if (! state.isTransactional()) {
        // jpa/11a6
        return false;
      }
    }

    // jpa/0j5f
    if (EntityState.P_DELETING.ordinal() <= state.ordinal())
      return false;

    return true;
  }

  /**
   * Callback when the user transaction begins
   */
  public void begin(Transaction xa)
  {
    try {
      xa.registerSynchronization(this);

      _isInTransaction = true;
      _isXA = true;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Starts a transaction.
   */
  public void beginTransaction()
    throws SQLException
  {
    _isInTransaction = true;

    if (_conn != null && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }

    // _xid = _factory.getXid();
  }

  /**
   * Sets XA.
   */
  public void setXA(boolean isXA)
  {
    _isXA = isXA;
    _isInTransaction = isXA;
  }

  /**
   * Commits a transaction.
   */
  public void commit()
    throws SQLException
  {
    try {
      flushInternal();

      _xid = 0;
      if (_conn != null) {
        _conn.commit();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      if (! _isXA)
        _isInTransaction = false;

      for (int i = 0; i < _txEntities.size(); i++) {
        Entity entity = _txEntities.get(i);

        entity.__caucho_afterCommit();
      }

      _txEntities.clear();
    }
  }

  /**
   * Callback before a utrans commit.
   */
  public void beforeCompletion()
  {
    try {
      beforeCommit();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Callback after a utrans commit.
   */
  public void afterCompletion(int status)
  {
    afterCommit(status == Status.STATUS_COMMITTED);
    _isXA = false;
    _isInTransaction = false;
  }

  /**
   * Called before the commit phase
   */
  public void beforeCommit()
    throws SQLException
  {
    try {
      flushInternal();
    } catch (SQLException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    /*
    // jpa/0gh0
    for (int i = _txEntities.size() - 1; i >= 0; i--) {
    Entity entity = _txEntities.get(i);

    // jpa/1500
    if (entity.__caucho_getEntityState() == EntityState.P_DELETED) {
    EntityType entityType = entity.__caucho_getEntityType();
    Object key = entity.__caucho_getPrimaryKey();
    EntityItem item = _persistenceUnit.getEntity(entityType, key);

    if (item == null) {
    // jpa/0ga8: entity has been removed and DELETE SQL was already flushed.
    continue;
    }
    }

    entity.__caucho_flush();
    }
    */
  }

  /**
   * Commits a transaction.
   */
  public void afterCommit(boolean isCommit)
  {
    if (! _isXA)
      _isInTransaction = false;

    if (isCommit) {
      if (_completionList.size() > 0) {
        _persistenceUnit.complete(_completionList);
      }
    }
    _completionList.clear();

    for (int i = 0; i < _txEntities.size(); i++) {
      Entity entity = _txEntities.get(i);

      try {
        if (isCommit)
          entity.__caucho_afterCommit();
        else
          entity.__caucho_afterRollback();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    _txEntities.clear();

    // jpa/0s2k
    for (int i = _entities.size() - 1; i >= 0; i--) {
      // XXX: needs to check EXTENDED type.
      // jpa/0h07: persistence context TRANSACTION type.
      _entities.get(i).__caucho_detach();
    }

    if (! isCommit) {
      // jpa/0j5c
      _entities.clear();

      /* XXX: jpa/0k11 - avoids double rollback()
         Rollback is done from com.caucho.transaction.TransactionImpl
         to the pool item com.caucho.jca.PoolItem
         try {
         if (_conn != null)
         _conn.rollback();
         } catch (SQLException e) {
         throw new IllegalStateException(e);
         }
      */
    }
  }

  /**
   * Rollbacks a transaction.
   */
  public void rollback()
    throws SQLException
  {
    try {
      flushInternal();

      _xid = 0;
      if (_conn != null) {
        _conn.rollback();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      if (! _isXA)
        _isInTransaction = false;

      _completionList.clear();

      for (int i = 0; i < _txEntities.size(); i++) {
        Entity entity = _txEntities.get(i);

        entity.__caucho_afterRollback();
      }

      _txEntities.clear();
    }
  }

  /**
   * Flushes managed entities.
   */
  public void flush()
  {
    try {
      checkTransactionRequired("flush");

      flushInternal();
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Flushes managed entities.
   */
  public void flushNoChecks()
  {
    try {
      flushInternal();
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Expires the entities
   */
  public void expire()
    throws SQLException
  {
    for (int i = 0; i < _entities.size(); i++) {
      Entity entity = _entities.get(i);

      entity.__caucho_expire();
    }
  }

  /**
   * Returns the connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    DataSource readDataSource = _persistenceUnit.getReadDataSource();

    if (! _isXA && ! _isInTransaction && readDataSource != null) {
      if (_readConn == null) {
        _readConn = readDataSource.getConnection();
      }
      else if (_readConn.isClosed()) {
        closeConnectionImpl();
        _readConn = _persistenceUnit.getDataSource().getConnection();
      }

      return _readConn;
    }

    if (_conn == null) {
      _conn = _persistenceUnit.getDataSource().getConnection();
      _isAutoCommit = true;
    }
    else if (_conn.isClosed()) {
      closeConnectionImpl();
      _conn = _persistenceUnit.getDataSource().getConnection();
      _isAutoCommit = true;
    }

    if (_isXA) {
    }
    else if (_isInTransaction && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }
    else if (! _isInTransaction && ! _isAutoCommit) {
      _isAutoCommit = true;
      _conn.setAutoCommit(true);
    }

    return _conn;
  }

  /**
   * Prepares a statement.
   */
  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    try {
      PreparedStatement pstmt = _preparedStatementMap.get(sql);

      if (pstmt == null) {
        Connection conn = getConnection();

        // XXX: avoids locking issues.
        if (_statements.size() > 0) {
          conn = _statements.get(0).getConnection();
        }

        // XXX: avoids locking issues.
        // See com.caucho.sql.UserConnection
        pstmt = conn.prepareStatement(sql,
                                      ResultSet.TYPE_FORWARD_ONLY,
                                      ResultSet.CONCUR_READ_ONLY);

        _statements.add(pstmt);

        _preparedStatementMap.put(sql, pstmt);
      }

      return pstmt;
    } catch (SQLException e) {
      closeConnectionImpl();

      throw e;
    }
  }

  /**
   * Closes a statement.
   */
  public void closeStatement(String sql)
    throws SQLException
  {
    PreparedStatement pstmt = _preparedStatementMap.remove(sql);

    if (pstmt != null) {
      _statements.remove(pstmt);

      pstmt.close();
    }
  }

  /**
   * Prepares an insert statement.
   */
  public PreparedStatement prepareInsertStatement(String sql)
    throws SQLException
  {
    PreparedStatement pstmt = null;

    try {
      pstmt = _preparedStatementMap.get(sql);

      if (pstmt == null) {
        Connection conn = getConnection();

        // XXX: avoids locking issues.
        if (_statements.size() > 0) {
          conn = _statements.get(0).getConnection();
        }

        if (_persistenceUnit.hasReturnGeneratedKeys())
          pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        else {
          // XXX: avoids locking issues.
          // See com.caucho.sql.UserConnection
          pstmt = conn.prepareStatement(sql,
                                        ResultSet.TYPE_FORWARD_ONLY,
                                        ResultSet.CONCUR_READ_ONLY);
        }

        _statements.add(pstmt);

        _preparedStatementMap.put(sql, pstmt);
      }
    } catch (SQLException e) {
      closeStatement(sql);
    }

    return pstmt;
  }

  /**
   * Makes the object transactional.
   *
   * @param obj the object to save
   *
   * @return the proxy for the saved object
   */
  public void makeTransactional(Entity entity)
  {
    // ejb/0600
    if (! _persistenceUnit.isJPA())
      return;

    EntityState state = entity.__caucho_getEntityState();

    if (EntityState.TRANSIENT.ordinal() < state.ordinal()
        && state.ordinal() < EntityState.P_DELETING.ordinal()) {
      // jpa/0g06
      addEntity(entity);
    }

    /*
      if (! isInTransaction())
      throw new AmberRuntimeException(L.l("makePersistent must be called from within a transaction."));

      if (! (obj instanceof Entity)) {
      throw new AmberRuntimeException(L.l("`{0}' is not a known entity class.",
      obj.getClass().getName()));
      }
    */
  }

  /**
   * Updates the database with the values in object.  If the object does
   * not exist, throws an exception.
   *
   * @param obj the object to update
   */
  public void update(Object obj)
  {
    /*
      for (int i = _entities.size() - 1; i >= 0; i--) {
      Entity entity = _entities.get(i);

      if (entity.__caucho_match(obj)) {
      entity.__caucho_load(obj);

      return entity;
      }
      }
    */

    /*
      Class cl = obj.getClass();

      EntityHome home = _factory.getHome(cl);

      if (home == null)
      throw new AmberException(L.l("no matching home for {0}", cl.getName()));

      Object key = home.getKeyFromEntity(obj);

      Entity entity = getEntity(cl, key);

      if (entity == null) {
      entity = home.load(this, key);

      addEntity(entity);
      }

      entity.__caucho_loadFromObject(obj);

      return entity;
    */
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(String homeName, Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(homeName, obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(AmberEntityHome home, Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(home, obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Updates the object.
   */
  public void update(Entity entity)
  {
    if (entity == null)
      return;

    // jpa/0g0i
    if (entity.__caucho_getEntityType() == null)
      return;

    // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.
    // jpa/0k10
    if (! isInTransaction())
      return;

    Table table = entity.__caucho_getEntityType().getTable();

    Object key = entity.__caucho_getPrimaryKey();

    addCompletion(new RowInvalidateCompletion(table.getName(), key));

    // jpa/0ga8, jpa/0s2d if (! _txEntities.contains(entity)) {
    int index = getTransactionEntity(entity.getClass().getName(), key);

    if (index < 0) {
      _txEntities.add(entity);
    }
    else {
      // jpa/0s2d
      Entity oldEntity = _txEntities.get(index);
      _txEntities.set(index, entity);
    }
  }

  /**
   * Deletes the object.
   *
   * @param obj the object to delete
   */
  public void delete(Entity entity)
    throws SQLException
  {
    int index = getEntity(entity.getClass().getName(),
                          entity.__caucho_getPrimaryKey());

    if (index < 0) {
      throw new IllegalStateException(L.l("AmberEntity[{0}:{1}} cannot be deleted since it is not managed"));
      /*
        EntityType entityType = entity.__caucho_getEntityType();

        if (entityType == null)
        return;
        // throw new AmberException(L.l("entity has no entityType"));

        AmberEntityHome entityHome = entityType.getHome();
        //entityHome = _persistenceUnit.getEntityHome(entity.getClass().getName());

        if (entityHome == null)
        throw new AmberException(L.l("entity has no matching home"));

        // XXX: this makes no sense
        entityHome.makePersistent(entity, this, true);

        addEntity(entity);
      */
    }
    else {
      Entity oldEntity = _entities.get(index);

      // XXX: jpa/0k12
      oldEntity.__caucho_setConnection(this);

      entity = oldEntity;
    }

    entity.__caucho_delete();
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, false);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareLazyQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareUpdate(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  private AmberQuery prepareQuery(String queryString, boolean isLazy)
    throws AmberException
  {
    AbstractQuery queryProgram = parseQuery(queryString, isLazy);

    UserQuery query = new UserQuery(queryProgram);

    query.setSession(this);

    return query;
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AbstractQuery parseQuery(String queryString, boolean isLazy)
    throws AmberException
  {
    try {
      _persistenceUnit.initEntityHomes();
    } catch (Exception e) {
      throw AmberRuntimeException.create(e);
    }

    QueryParser parser = new QueryParser(queryString);

    parser.setPersistenceUnit(_persistenceUnit);
    parser.setLazyResult(isLazy);

    return parser.parse();
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public ResultSet query(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.executeQuery();
  }

  /**
   * Returns the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   */
  public ResultSetCacheChunk getQueryCacheChunk(String sql,
                                                Object []args,
                                                int startRow)
  {
    _queryKey.init(sql, args, startRow);

    return _persistenceUnit.getQueryChunk(_queryKey);
  }

  /**
   * Returns the result set meta data from cache.
   */
  public ResultSetMetaData getQueryMetaData()
  {
    return _persistenceUnit.getQueryMetaData(_queryKey);
  }

  /**
   * Sets the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   * @param cacheChunk the new value of the cache chunk
   */
  public void putQueryCacheChunk(String sql,
                                 Object []args,
                                 int startRow,
                                 ResultSetCacheChunk cacheChunk,
                                 ResultSetMetaData cacheMetaData)
  {
    QueryCacheKey key = new QueryCacheKey();
    Object []newArgs = new Object[args.length];

    System.arraycopy(args, 0, newArgs, 0, args.length);

    key.init(sql, newArgs, startRow);

    _persistenceUnit.putQueryChunk(key, cacheChunk);
    _persistenceUnit.putQueryMetaData(key, cacheMetaData);
  }

  /**
   * Updates the database with a query
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public int update(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareUpdate(hsql);

    return query.executeUpdate();
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public List find(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.list();
  }

  /**
   * Cleans up the connection.
   */
  public void cleanup()
  {
    try {
      // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.
      // jpa/0g04
      if (isInTransaction()) {
        flushInternal();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      _depth = 0;

      for (int i = _entities.size() - 1; i >= 0; i--) {
        _entities.get(i).__caucho_detach();
      }

      _entities.clear();
      _txEntities.clear();
      _completionList.clear();

      freeConnection();
    }
  }

  /**
   * Pushes the depth.
   */
  public void pushDepth()
  {
    // these aren't necessary because the AmberConnection is added as
    // a close callback to the UserTransaction
  }

  /**
   * Pops the depth.
   */
  public void popDepth()
  {
  }

  /**
   * Frees the connection.
   */
  public void freeConnection()
  {
    closeConnectionImpl();
  }

  /**
   * Frees the connection.
   */
  private void closeConnectionImpl()
  {
    Connection conn = _conn;
    _conn = null;

    Connection readConn = _readConn;
    _readConn = null;

    boolean isAutoCommit = _isAutoCommit;
    _isAutoCommit = true;

    try {
      if (conn != null && ! isAutoCommit)
        conn.setAutoCommit(true);
    } catch (SQLException e) {
    }

    for (Statement stmt : _statements) {
      try {
        stmt.close();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    try {
      _preparedStatementMap.clear();
      _statements.clear();

      if (conn != null)
        conn.close();

      if (readConn != null)
        readConn.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    if (_persistenceUnit != null)
      return "AmberConnection[" + _persistenceUnit.getName() + "]";
    else
      return "AmberConnection[closed]";
  }

  /**
   * Finalizer.
   */
  public void finalize()
  {
    cleanup();
  }

  /**
   * Returns true when cache items can be used.
   */
  public boolean shouldRetrieveFromCache()
  {
    // ejb/0d01
    return (! isInTransaction());
  }

  public void setTransactionalState(Entity entity)
  {
    if (isInTransaction()) {
      // jpa/0ga8
      entity.__caucho_setConnection(this);

      // jpa/0j5f
      EntityState state = entity.__caucho_getEntityState();
      //if (state.ordinal() < EntityState.P_DELETING.ordinal())
      if (state == EntityState.P_NON_TRANSACTIONAL)
        entity.__caucho_setEntityState(EntityState.P_TRANSACTIONAL);
    }
  }

  //
  // private
  //
  // throws Exception (for jpa)
  //
  // ejb/0g22 (cmp) expects exception handling in
  // the public methods. See public void create(Object) above.

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(Object obj)
    throws Exception
  {
    AmberEntityHome home = null;

    Class cl = obj.getClass();

    for (; home == null && cl != null; cl = cl.getSuperclass()) {
      home = _persistenceUnit.getHome(cl);
    }

    if (home == null)
      throw new AmberException(L.l("`{0}' is not a known entity class.",
                                   obj.getClass().getName()));

    createInternal(home, obj);
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(String homeName, Object obj)
    throws Exception
  {
    AmberEntityHome home = _persistenceUnit.getEntityHome(homeName);

    if (home == null)
      throw new AmberException(L.l("`{0}' is not a known entity class.",
                                   obj.getClass().getName()));

    createInternal(home, obj);
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(AmberEntityHome home, Object obj)
    throws Exception
  {
    // XXX: flushing things like delete might be useful?
    // XXX: the issue is a flush can break FK constraints and
    //      fail prematurely (jpa/0h26).
    // commented out: flushInternal();

    if (contains(obj))
      return;

    Entity entity = (Entity) obj;

    // jpa/0g0k: cannot call home.save because of jpa exception handling.
    if (_persistenceUnit.isJPA()) {
      // See persistInternal(): entity.__caucho_cascadePrePersist(this);

      entity.__caucho_create(this, home.getEntityType());

      // See persistInternal(): entity.__caucho_cascadePostPersist(this);
    }
    else
      home.save(this, entity);

    addEntity(entity);

    // jpa/0h25
    // XXX: not correct, since we need to keep the P_PERSIST state around
    // and P_PERSIST is a transactional state
    // setTransactionalState(entity);

    Table table = home.getEntityType().getTable();
    addCompletion(new TableInvalidateCompletion(table.getName()));
  }

  private void checkTransactionRequired(String operation)
    throws TransactionRequiredException
  {
    // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.

    if (! (_isXA || _isInTransaction))
      throw new TransactionRequiredException(L.l("{0}() operation can only be executed in the scope of a transaction.", operation));
  }

  private Entity checkEntityType(Object entity, String operation)
  {
    if (! (entity instanceof Entity))
      throw new IllegalArgumentException(L.l(operation + "() operation can only be applied to an entity instance. If the argument is an entity, the corresponding class must be specified in the scope of a persistence unit."));

    if (_persistenceUnit.isJPA()) {
      String className = entity.getClass().getName();

      EntityType entityType = (EntityType) _persistenceUnit.getEntity(className);

      // jpa/0m08
      if (entityType == null) {
        throw new IllegalArgumentException(L.l(operation + "() operation can only be applied to an entity instance. If the argument is an entity, the class '{0}' must be specified in the orm.xml or annotated with @Entity and must be in the scope of a persistence unit.", className));
      }
    }

    return (Entity) entity;
  }

  /**
   * Flush managed entities.
   */
  private void flushInternal()
    throws Exception
  {
    /* XXX: moved into __caucho_flush
       for (int i = _txEntities.size() - 1; i >= 0; i--) {
       Entity entity = _txEntities.get(i);

       EntityState state = entity.__caucho_getEntityState();

       // jpa/0i60
       // jpa/0h27: for all entities Y referenced by a *managed*
       // entity X, where the relationship has been annotated
       // with cascade=PERSIST/ALL, the persist operation is
       // applied to Y. It is a lazy cascade as the relationship
       // is not always initialized at the time persist(X) was
       // called but must be at flush time.

       if (state == EntityState.P_PERSIST) {
       entity.__caucho_cascadePrePersist(this);
       entity.__caucho_cascadePostPersist(this);
       }
       }
    */

    for (int i = _txEntities.size() - 1; i >= 0; i--) {
      Entity entity = _txEntities.get(i);

      entity.__caucho_flush();
    }

    if (! isInTransaction()) {
      if (_completionList.size() > 0) {
        _persistenceUnit.complete(_completionList);
      }
      _completionList.clear();

      for (int i = 0; i < _txEntities.size(); i++) {
        Entity entity = _txEntities.get(i);

        entity.__caucho_afterCommit();
      }

      _txEntities.clear();
    }
  }

  /**
   * Persists the entity.
   */
  private void persistInternal(Object entity, boolean isCascade)
    throws Exception
  {
    Entity instance = (Entity) entity;

    EntityState state = instance.__caucho_getEntityState();

    switch (state) {
    case TRANSIENT:
      {
        // jpa/0h24
        // Pre-persist child entities.
        instance.__caucho_cascadePrePersist(this);

        try {
          createInternal(instance);
        } catch (SQLException e) {
          log.log(Level.FINEST, e.toString(), e);

          // jpa/0ga3
          throw new EntityExistsException(L.l("Trying to persist an entity of class '{0}' with PK '{1}' that already exists. Entity state '{2}'", instance.getClass().getName(), instance.__caucho_getPrimaryKey(), state));
        }
      }
      break;

    case P_DELETING:
    case P_DELETED:
      {
        // jpa/0i60, jpa/1510

        /* jpa/0h25: should always flush from cascade,
           but only flushes this entity. Related entities
           are flushed with the cascading operations.

           if (! isCascade) {
             flushInternal();
        */
        instance.__caucho_flush();

        // jpa/0h26
        instance.__caucho_cascadePrePersist(this);

        // removed entity instance, reset state and persist.
        instance.__caucho_makePersistent(null, (EntityType) null);
        createInternal(instance);
      }
      break;

    case P_PERSIST:
      {
        // jpa/0h26
        // Pre-persist child entities.
        instance.__caucho_cascadePrePersist(this);
      }
      break;

    default:
      if (instance.__caucho_getConnection() == this)
        return;
      else {
        // jpa/0ga5 (tck):
        // See entitytest.persist.basic.persistBasicTest4 vs.
        //     callback.inheritance.preUpdateTest
        throw new EntityExistsException(L.l("Trying to persist an entity that is detached or already exists. Entity state '{0}'", state));
      }
    }

    // jpa/0h27
    // Post-persist child entities.
    instance.__caucho_cascadePostPersist(this);
  }

  /**
   * Creates an instance of the native query.
   */
  private Query createInternalNativeQuery(String sql,
                                          SqlResultSetMappingConfig map)
  {
    Query query = createNativeQuery(sql);

    QueryImpl queryImpl = (QueryImpl) query;

    queryImpl.setSqlResultSetMapping(map);

    return query;
  }

  private class EntityTransactionImpl implements EntityTransaction {
    /**
     * Starts a resource transaction.
     */
    public void begin()
    {
      try {
        AmberConnection.this.beginTransaction();
      } catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }

    /**
     * Commits a resource transaction.
     */
    public void commit()
    {
      try {
        // jpa/11a7
        AmberConnection.this.beforeCommit();

        _isInTransaction = false;

        // jpa/11a7 AmberConnection.this.commit();
        if (AmberConnection.this._conn != null) {
          AmberConnection.this._conn.commit();
        }

        // XXX: missing finally issues if _conn.commit fails

        // jpa/11a7
        AmberConnection.this.afterCommit(true);

        if (AmberConnection.this._conn != null) {
          closeConnectionImpl();
        }
      } catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }

    /**
     * Rolls the current transaction back.
     */
    public void rollback()
    {
      try {
        AmberConnection.this.rollback();
      } catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }

    /**
     * Marks the current transaction for rollback only.
     */
    public void setRollbackOnly()
    {
    }

    /**
     * Returns true if the transaction is for rollback only.
     */
    public boolean getRollbackOnly()
    {
      return false;
    }

    /**
     * Test if a transaction is in progress.
     */
    public boolean isActive()
    {
      return _isInTransaction;
    }
  }
}
