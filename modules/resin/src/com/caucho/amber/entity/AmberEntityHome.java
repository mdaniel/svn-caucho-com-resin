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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.AmberObjectNotFoundException;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.CacheUpdate;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the set of persistent beans.
 */
public class AmberEntityHome {
  private static final L10N L = new L10N(AmberEntityHome.class);
  private static final Logger log = Log.open(AmberEntityHome.class);

  private AmberPersistenceUnit _manager;
  private EntityType _entityType;

  private EntityFactory _entityFactory = new EntityFactory();

  private Entity _homeBean;

  private ArrayList<SoftReference<CacheUpdate>> _cacheUpdates
    = new ArrayList<SoftReference<CacheUpdate>>();

  private EntityKey _cacheKey = new EntityKey();

  private volatile boolean _isInit;

  private ConfigException _configException;

  private Method _cauchoGetBeanMethod;

  public AmberEntityHome(AmberPersistenceUnit manager, EntityType type)
  {
    _manager = manager;
    _entityType = type;

    try {
      Class cl = Class.forName("com.caucho.ejb.entity.EntityObject");
      _cauchoGetBeanMethod = cl.getMethod("_caucho_getBean", new Class[0]);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the getBean method to instantiate the Amber object.
   */
  public Method getCauchoGetBeanMethod()
  {
    return _cauchoGetBeanMethod;
  }

  /**
   * Returns the manager.
   */
  public AmberPersistenceUnit getManager()
  {
    return _manager;
  }

  /**
   * Returns the entity type
   */
  public EntityType getEntityType()
  {
    return _entityType;
  }

  /**
   * Returns the entity type
   */
  public EntityType getRootType()
  {
    return (EntityType) _entityType.getRootType();
  }

  /**
   * Returns the java class.
   */
  public Class getJavaClass()
  {
    return _entityType.getInstanceClass();
  }

  /**
   * Returns the entity factory.
   */
  public EntityFactory getEntityFactory()
  {
    return _entityFactory;
  }

  /**
   * Sets the entity factory.
   */
  public void setEntityFactory(EntityFactory factory)
  {
    _entityFactory = factory;
  }

  /**
   * Returns the cache timeout.
   */
  public long getCacheTimeout()
  {
    return _entityType.getCacheTimeout();
  }

  /**
   * Returns the instance class.
   */
  public Class getInstanceClass()
  {
    return _entityType.getInstanceClass();
  }

  /**
   * Link the classes.
   */
  void link()
    throws ConfigException
  {
    // _entityClass.link(_manager);
  }

  /**
   * Initialize the home.
   */
  public void init()
    throws ConfigException
  {
    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;
    }

    _entityType.init();

    try {
      Class instanceClass = _entityType.getInstanceClass();

      if (! Modifier.isAbstract(instanceClass.getModifiers()))
        _homeBean = (Entity) instanceClass.newInstance();
    } catch (Exception e) {
      _entityType.setConfigException(e);

      _configException = new ConfigException(e);
      throw _configException;
    }

    _entityType.start();
  }

  /**
   * Returns the entity from the key.
   */
  public Object getKeyFromEntity(Entity entity)
    throws AmberException
  {
    //    return _entityType.getId().getType().getValue(obj);
    return null;
  }

  /**
   * Converts a long key to the key.
   */
  public Object toObjectKey(long key)
  {
    return _entityType.getId().toObjectKey(key);
  }

  /*
  private Entity load(AmberConnection aConn,
                     Object key)
    throws AmberException
  {
    EntityItem existingItem = _manager.getEntity(getRootType(), key);
    
    return find(aConn, key, existingItem, true, 0, 0);
  }
  */

  /**
   * Finds by the primary key.
   */
  /*
  private Entity load(AmberConnection aConn,
                     Object key,
                     long notExpiringLoadMask,
                     int notExpiringGroup)
    throws AmberException
  {
    EntityItem existingItem = _manager.getEntity(getRootType(), key);
    
    return find(aConn, key, existingItem,
		true, notExpiringLoadMask, notExpiringGroup);
  }
  */

  /**
   * Finds by the primary key.
   */
  /*
  private Entity loadLazy(AmberConnection aConn, Object key)
    throws AmberException
  {
    EntityItem existingItem = _manager.getEntity(getRootType(), key);
    
    return find(aConn, key, existingItem, false, 0, 0);
  }
  */

  /**
   * Finds by the primary key.
   */
  public EntityItem findItem(AmberConnection aConn,
                             ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = _homeBean.__caucho_home_find(aConn, this, rs, index);

    return item;
  }

  /**
   * Finds by the primary key.
   */
  public Object loadFull(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    // jpa/0l43
    Entity entity = aConn.getSubEntity(_homeBean.getClass(),
                                       rs.getObject(index));

    if (entity != null) {
      if (entity.__caucho_getEntityState().isManaged())
        return entity;
    }

    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    entity = null;

    Object value = aConn.getEntityLazy(item);

    if (aConn.isActiveTransaction()) {
      if (value instanceof Entity)
        entity = (Entity) value;
      else if (_cauchoGetBeanMethod != null) {
        try {
          entity = (Entity) _cauchoGetBeanMethod.invoke(value, new Object[0]);
          entity.__caucho_makePersistent(aConn, item);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      if (entity == null)
        entity = aConn.getEntity(item);
    }
    else
      entity = item.getEntity();

    int keyLength = _entityType.getId().getKeyCount();

    entity.__caucho_load(aConn, rs, index + keyLength);

    return entity;
  }

  /**
   * Finds by the primary key.
   */
  public Object loadLazy(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    return aConn.getEntity(item);
  }

  /*
  private Entity find(AmberConnection aConn,
                     Object key,
                     boolean isLoad)
    throws AmberException
  {
    EntityItem existingItem = _manager.getEntity(getRootType(), key);
    
    return find(aConn, key, existingItem, isLoad, 0, 0);
  }
  */

  /**
   * Finds an entity based on the primary key.
   *
   * @param key the primary key
   * @param aConn the Amber connection to associate with the loaded item
   * @param isLoad if true, try to load the bean
   */
  /*
  public Entity find(AmberConnection aConn,
                     Object key,
		     EntityItem cacheItem,
                     boolean isLoad,
                     long notExpiringLoadMask,
                     int notExpiringGroup)
    throws AmberException
  {
    try {
      // jpa/0o01, jpa/0o41

      // jpa/0l48: checks if cacheItem is new or existing (below).
      // EntityItem existingItem = _manager.getEntity(getRootType(), key);

      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, L.l("AmberEntityHome.find class: {0} PK: {1} isLoad: {2} xa: {3} notExpiringLoadMask: " + notExpiringLoadMask + " notExpiringGroup: " + notExpiringGroup,
                                 _entityType.getClassName(), key,
                                 isLoad, aConn.isActiveTransaction()));

        String msg;

        if (existingItem == null)
          msg = "there is not a cache item for class: {0} PK: {1}. It will try to load it.";
        else
          msg = "found an existing cache item for class: {0} PK: {1}.";

        log.log(Level.FINER, L.l("AmberEntityHome.find: " + msg,
                                 _entityType.getClassName(), key));
      }

      Entity cacheEntity = cacheItem.getEntity();
      AmberEntityHome home = cacheItem.getEntityHome();

      Entity entity = cacheEntity.__caucho_home_new(home, key);
      entity.__caucho_makePersistent(aConn, cacheItem);

      // The entity is added for eagerly loading optimization.
      aConn.addEntity(entity);

      try {
        // jpa/0l48: inheritance loading optimization.
        // jpa/0h20: no transaction, copy from the existing cache item.

        // Copy as much as possible from a new cache item or
        // from the existing cache item if there is no transaction.
        if (existingItem == null || ! aConn.isActiveTransaction()) {
          if (log.isLoggable(Level.FINER)) {
            String msg;

            if (existingItem == null)
              msg = "a new cache item has been loaded and";
            else
              msg = "since there is no transaction, the existing cache item";

            log.log(Level.FINER, L.l( "AmberEntityHome.find: {0} will be copied to a context object class: {1} PK: {2}.",
                                      msg, _entityType.getClassName(), key));
          }

          // _manager.copyFromCacheItem(aConn, entity, cacheItem);
	  cacheItem.copyTo(entity, aConn);
        }

        long loadMask = entity.__caucho_getLoadMask(notExpiringGroup);
        loadMask |= notExpiringLoadMask;

        // jpa/0l42: loading optimization.
        entity.__caucho_setLoadMask(loadMask, notExpiringGroup);

        if (isLoad) {
          entity.__caucho_retrieve_eager(aConn);
        }
        else if (aConn.isActiveTransaction()) {
          // jpa/0v33: within a transaction, cannot copy from cache.
          entity.__caucho_retrieve_self(aConn);
        }

        // XXX: should be handled above.
        // else if (existingItem != null) {
        //   cacheItem.copyTo(entity, aConn);
        // }
      } catch (AmberObjectNotFoundException e) {
        // 0g0q: if the entity is not found, removes it from context.
        aConn.removeEntity(entity);

        if (_manager.isJPA())
          return null;

        throw e;
      }

      return entity;
    } catch (Exception e) {
      // jpa/0u0j
      if (_manager.isJPA() && (e instanceof RuntimeException))
        throw (RuntimeException) e;

      throw AmberException.create(e);
    }
  }
  */

  /**
   * Loads an entity based on the primary key.
   *
   * @param aConn the Amber connection to associate with the loaded item
   * @param key the primary key
   * @param isLoad if true, try to load the bean
   */
  /*
  public EntityItem findEntityItem(AmberConnection aConn,
                                   Object key,
                                   boolean isLoad,
                                   EntityItem item)
    throws AmberException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "findEntityItem: "+key+" "+isLoad);

    if (key == null)
      return null; // ejb/0a06 throw new NullPointerException("primaryKey");

    try {
      // XXX: ejb/0d01 should not check this.
      // jpa/0y14 if (aConn.shouldRetrieveFromCache())
      if (item == null)
        item = _manager.getEntity(getRootType(), key);

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "findEntityItem item is null? "+(item == null));

      if (item == null) {
        if (_homeBean == null && _configException != null)
          throw _configException;

        // jpa/0l00, jpa/0l32
        // XXX: this is an initial optimization and bug fix also for jpa/0s29
        // XXX: another point is inheritance with many-to-one (jpa/0l40 and jpa/0s29)
        //      still get twice the number of loading SQLs.
        boolean loadFromResultSet = false; // ! getEntityType().hasDependent();

        Entity cacheEntity;

        cacheEntity = (Entity) _homeBean.__caucho_home_find(aConn, this, key);

        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, "findEntityItem cacheEntity is null? "+(cacheEntity == null));

        // Object does not exist.
        if (cacheEntity == null) {
          if (_manager.isJPA())
            return null;

          // ejb/0604
          throw new AmberObjectNotFoundException("amber find: no matching object " + _entityType.getBeanClass().getName() + "[" + key + "]");
        }

        item = new CacheableEntityItem(this, cacheEntity);

        // The cache entity is added after commit.
        if (! aConn.isActiveTransaction()) {
          if (isLoad) {
            if (_manager.isJPA()) {
              // jpa/0o03
              item.loadEntity(aConn, 0);
            }
            else
              item.loadEntity(0);
          }

          // jpa/0g0p: only adds the cache entity if it is not within a transaction.
          item = _manager.putEntity(getRootType(), key, item);

          if (log.isLoggable(Level.FINER))
            log.log(Level.FINER, "findEntityItem after putEntity item is null? "+(item == null));
        }
      }
      else if (isLoad) {
        if (! aConn.isActiveTransaction()) {
          if (_manager.isJPA()) {
            // jpa/0v33
            item.loadEntity(aConn, 0);
          }
          else
            item.loadEntity(0);
        }
      }

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "returning item is null? "+(item == null));

      return item;
    } catch (Exception e) {
      // jpa/0u0j
      if (_manager.isJPA() && (e instanceof ClassCastException))
        throw new IllegalArgumentException(e);

      throw AmberException.create(e);
    }
  }
  */
  
  public EntityItem findEntityItem(AmberConnection aConn, Object key)
    throws AmberException
  {
    if (_homeBean == null && _configException != null)
      throw _configException;

    Entity cacheEntity;

    cacheEntity = (Entity) _homeBean.__caucho_home_find(aConn, this, key);

    if (cacheEntity != null)
      return new CacheableEntityItem(this, cacheEntity);
    else
      return null;
  }

  /**
   * Loads an entity based on the primary key.
   *
   * @param key the primary key
   * @param aConn the Amber connection to associate with the loaded item
   * @param isLoad if true, try to load the bean
   */
  public EntityItem setEntityItem(Object key, EntityItem item)
    throws AmberException
  {
    if (key == null)
      throw new NullPointerException("primaryKey");

    try {
      item.getEntity().__caucho_setConnection(_manager.getCacheConnection());

      return _manager.putEntity(getRootType(), key, item);
    } catch (Exception e) {
      throw AmberException.create(e);
    }
  }

  /**
   * Loads an entity where the type is determined by a discriminator
   *
   * @param aConn the connection to associate with the entity
   * @param key the primary key
   * @param discriminator the object's discriminator
   */
  public EntityItem findDiscriminatorEntityItem(AmberConnection aConn,
                                                Object key,
                                                String discriminator)
    throws SQLException
  {
    EntityItem item = null;

    // jpa/0l20
    // XXX: ejb/0d01
    // if (aConn.shouldRetrieveFromCache())
    item = _manager.getEntity(getRootType(), key);

    if (item == null) {
      EntityType subEntity
        = (EntityType) _entityType.getSubClass(discriminator);

      Entity cacheEntity = subEntity.createBean();

      cacheEntity.__caucho_setPrimaryKey(key);
      cacheEntity.__caucho_makePersistent(_manager.getCacheConnection(),
                                          subEntity);

      item = new CacheableEntityItem(this, cacheEntity);

      // The cache entity is added after commit.
      if (! aConn.isActiveTransaction()) {
        item = _manager.putEntity(getRootType(), key, item);
      }
    }

    return item;
  }

  /**
   * Instantiates a new entity for this home.
   */
  public Entity newEntity(Object key)
  {
    return (Entity) _homeBean.__caucho_home_new(this, key, null, null);
  }

  /**
   * Loads an entity where the type is determined by a discriminator
   *
   * @param aConn the connection to associate with the entity
   * @param key the primary key
   * @param discriminator the object's discriminator
   */
  public Entity newDiscriminatorEntity(Object key,
                                       String discriminator)
  {
    if (discriminator == null || key == null)
      throw new AmberRuntimeException(L.l("{0} is not a valid inheritance key.",
                                          key));

    EntityType subType = (EntityType) _entityType.getSubClass(discriminator);

    return subType.getHome().newEntity(key);
  }

  /**
   * Finds by the primary key.
   */
  public Entity makePersistent(Entity entity,
                               AmberConnection aConn,
                               boolean isLazy)
    throws SQLException
  {
    entity.__caucho_makePersistent(aConn, _entityType);

    return entity;
  }

  /**
   * Saves based on the object.
   */
  public void save(AmberConnection aConn, Entity entity)
    throws SQLException
  {
    // Common to JPA and CMP.
    entity.__caucho_lazy_create(aConn, _entityType);

    if (! _manager.isJPA())
      entity.__caucho_create(aConn, _entityType);
  }

  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, Object key)
    throws SQLException
  {
    _manager.removeEntity(getRootType(), key);

    /*
      _entityType.childDelete(aConn, key);

      // XXX: possibly move somewhere else?
      synchronized (_cacheUpdates) {
      for (int i = _cacheUpdates.size() - 1; i >= 0; i--) {
      SoftReference<CacheUpdate> ref = _cacheUpdates.get(i);
      CacheUpdate update = ref.get();

      if (update == null)
      _cacheUpdates.remove(i);
      else
      update.delete(primaryKey);
      }
      }
    */
  }

  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, long primaryKey)
    throws SQLException
  {
    /*
      _entityClass.childDelete(session, primaryKey);

      // XXX: possibly move somewhere else?
      synchronized (_cacheUpdates) {
      for (int i = _cacheUpdates.size() - 1; i >= 0; i--) {
      SoftReference<CacheUpdate> ref = _cacheUpdates.get(i);
      CacheUpdate update = ref.get();

      if (update == null)
      _cacheUpdates.remove(i);
      else
      update.delete(primaryKey);
      }
      }
    */
  }

  /**
   * Update for a modification.
   */
  public void update(Entity entity)
    throws SQLException
  {
  }

  /**
   * Adds a cache update.
   */
  public void addUpdate(CacheUpdate update)
  {
    _cacheUpdates.add(new SoftReference<CacheUpdate>(update));
  }
}
