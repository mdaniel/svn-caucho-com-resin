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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import java.lang.reflect.Method;

import java.lang.ref.SoftReference;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;
import java.sql.ResultSet;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.amber.AmberManager;
import com.caucho.amber.AmberException;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.connection.AmberConnectionImpl;

import com.caucho.amber.query.CacheUpdate;

/**
 * Manages the set of persistent beans.
 */
public class AmberEntityHome {
  private static final L10N L = new L10N(AmberEntityHome.class);
  private static final Logger log = Log.open(AmberEntityHome.class);

  private AmberManager _manager;
  private EntityType _entityType;

  private EntityFactory _entityFactory = new EntityFactory();

  private Entity _homeBean;
  
  private ArrayList<SoftReference<CacheUpdate>> _cacheUpdates =
    new ArrayList<SoftReference<CacheUpdate>>();
  
  private EntityKey _cacheKey = new EntityKey();

  private volatile boolean _isInit;

  private ConfigException _configException;
  
  private Method _cauchoGetBeanMethod;

  public AmberEntityHome(AmberManager manager, EntityType type)
  {
    _manager = manager;
    _entityType = type;

    try {
      Class cl = Class.forName("com.caucho.ejb.entity.EntityObject");
      _cauchoGetBeanMethod = cl.getMethod("_caucho_getBean", new Class[0]);
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  /**
   * Returns the manager.
   */
  public AmberManager getManager()
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
    return _entityType.getRootType();
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
      _homeBean = (Entity) _entityType.getInstanceClass().newInstance();
    } catch (Exception e) {
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
  
  /**
   * Finds by the primary key.
   */
  public Entity load(AmberConnectionImpl aConn, Object key)
    throws AmberException
  {
    return find(aConn, key, true);
  }
  
  /**
   * Finds by the primary key.
   */
  public Entity loadLazy(AmberConnectionImpl aConn, Object key)
    throws AmberException
  {
    return find(aConn, key, false);
  }
  
  /**
   * Finds by the primary key.
   */
  public EntityItem findItem(AmberConnectionImpl aConn,
			     ResultSet rs, int index)
    throws SQLException
  {
    return _homeBean.__caucho_home_find(aConn, this, rs, index);
  }
  
  /**
   * Finds by the primary key.
   */
  public Object loadFull(AmberConnectionImpl aConn, ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    Entity entity = null;

    Object value = _entityFactory.getEntity(aConn, item);

    if (aConn.isInTransaction()) {
      if (value instanceof Entity)
	entity = (Entity) value;
      else if (_cauchoGetBeanMethod != null) {
	try {
	  entity = (Entity) _cauchoGetBeanMethod.invoke(value, new Object[0]);
	  entity.__caucho_makePersistent(aConn, item);
	} catch (Throwable e) {
	  log.log(Level.FINEST, e.toString(), e);
	}
      }

      if (entity == null)
	entity = aConn.getEntity(item);
    }
    else
      entity = item.getEntity();
    
    int keyLength = _entityType.getId().getKeyCount();

    entity.__caucho_load(aConn, rs, index + keyLength);

    return value;
  }
  
  /**
   * Finds by the primary key.
   */
  public Object loadLazy(AmberConnectionImpl aConn, ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    return _entityFactory.getEntity(aConn, item);
  }
  
  /**
   * Finds an entity based on the primary key.
   *
   * @param key the primary key
   * @param aConn the Amber connection to associate with the loaded item
   * @param isLoad if true, try to load the bean
   */
  public Entity find(AmberConnectionImpl aConn, Object key, boolean isLoad)
    throws AmberException
  {
    EntityItem item = findEntityItem(aConn, key, isLoad);

    return item.copy(aConn);
  }
  
  /**
   * Loads an entity based on the primary key.
   *
   * @param aConn the Amber connection to associate with the loaded item
   * @param key the primary key
   * @param isLoad if true, try to load the bean
   */
  public EntityItem findEntityItem(AmberConnectionImpl aConn,
				   Object key,
				   boolean isLoad)
    throws AmberException
  {
    if (key == null)
      return null; // ejb/0a06 throw new NullPointerException("primaryKey");

    try {
      EntityItem item = _manager.getEntity(getRootType(), key);

      if (item == null) {
	if (_homeBean == null && _configException != null)
	  throw _configException;
	
	Entity cacheEntity;
	cacheEntity = (Entity) _homeBean.__caucho_home_new(aConn, this, key);
	
	if (isLoad)
	  cacheEntity.__caucho_retrieve(aConn);

	item = new CacheableEntityItem(this, cacheEntity);
	item = _manager.putEntity(getRootType(), key, item);
      }
      else if (isLoad) {
	item.loadEntity(0);
      }

      return item;
    } catch (Exception e) {
      throw AmberException.create(e);
    }
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
  public EntityItem findDiscriminatorEntityItem(AmberConnectionImpl aConn,
						Object key,
						String discriminator)
    throws SQLException
  {
    EntityItem item = _manager.getEntity(getRootType(), key);

    if (item == null) {
      EntityType subEntity = _entityType.getSubClass(discriminator);
    
      Entity cacheEntity = subEntity.createBean();

      cacheEntity.__caucho_setPrimaryKey(key);
      cacheEntity.__caucho_makePersistent(_manager.getCacheConnection(),
					  subEntity);

      item = new CacheableEntityItem(this, cacheEntity);
      item = _manager.putEntity(getRootType(), key, item);
    }

    return item;
  }
  
  /**
   * Finds by the primary key.
   */
  public Entity makePersistent(Entity entity,
			       AmberConnectionImpl aConn,
			       boolean isLazy)
    throws SQLException
  {
    entity.__caucho_makePersistent(aConn, _entityType);

    return entity;
  }
  
  /**
   * Saves based on the object.
   */
  public void save(AmberConnectionImpl aConn, Entity entity)
    throws AmberException
  {
    try {
      entity.__caucho_create(aConn, _entityType);
    } catch (Exception e) {
      throw AmberException.create(e);
    }
  }
  
  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnectionImpl aConn, Object key)
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
  public void delete(AmberConnectionImpl aConn, long primaryKey)
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
