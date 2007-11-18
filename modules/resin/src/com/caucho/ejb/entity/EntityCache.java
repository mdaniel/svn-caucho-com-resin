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

package com.caucho.ejb.entity;

import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;

/**
 * Caches entities
 */
public class EntityCache
{
  private static final L10N L = new L10N(EntityCache.class);
  protected static final Logger log
    = Logger.getLogger(EntityCache.class.getName());

  private int _entityCacheSize = 32 * 1024;

  private long _entityCacheTimeout = 5000L;

  private LruCache<EntityKey,QEntityContext> _entityCache;

  private EntityKey _entityKey = new EntityKey();

  /**
   * Caches entity beans
   */
  public EntityCache()
  {
  }

  /**
   * Gets the cache timeout.
   */
  public long getCacheTimeout()
  {
    return _entityCacheTimeout;
  }

  /**
   * Sets the cache timeout.
   */
  public void setCacheTimeout(long cacheTimeout)
  {
    _entityCacheTimeout = cacheTimeout;
    // _amberPersistenceUnitenceUnitenceUnit.setTableCacheTimeout(cacheTimeout);
  }

  /**
   * Gets the cache size.
   */
  public int getCacheSize()
  {
    return _entityCacheSize;
  }

  /**
   * Sets the cache size.
   */
  public void setCacheSize(int cacheSize)
  {
    _entityCacheSize = cacheSize;
  }

  /**
   * Initialize the manager after all the configuration files have been read.
   */
  public void start()
  {
    if (_entityCache == null)
      _entityCache = new LruCache<EntityKey,QEntityContext>(_entityCacheSize);
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext getEntity(EntityServer server, Object key)
  {
    synchronized (_entityKey) {
      _entityKey.init(server, key);

      return _entityCache.get(_entityKey);
    }
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext putEntityIfNew(EntityServer server,
				       Object key,
                                       QEntityContext context)
  {
    return _entityCache.putIfNew(new EntityKey(server, key), context);
  }

  /**
   * Adds a new entity.
   */
  public void removeEntity(EntityServer server, Object key)
  {
    if (_entityCache == null)
      return;
    
    synchronized (_entityKey) {
      _entityKey.init(server, key);
      _entityCache.remove(_entityKey);
    }
  }

  /**
   * Adds a new entity.
   */
  public void removeBeans(ArrayList<QEntityContext> beans, EntityServer server)
  {
    if (_entityCache == null)
      return;
    
    synchronized (_entityCache) {
      Iterator<LruCache.Entry<EntityKey,QEntityContext>> iter;

      iter = _entityCache.iterator();

      while (iter.hasNext()) {
        LruCache.Entry<EntityKey,QEntityContext> entry = iter.next();

        beans.add(entry.getValue());

        iter.remove();
      }
    }
  }

  /**
   * Invalidates the caches for all the beans.
   */
  public void invalidateCache()
  {
  }
}

