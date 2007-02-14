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

package com.caucho.amber.entity;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.util.Alarm;

import java.sql.SQLException;
import java.util.Map;

/**
 * An entity item handles the living entities.
 */
public class CacheableEntityItem extends EntityItem {
  private AmberEntityHome _home;
  private Entity _cacheItem;
  private long _expireTime;

  public CacheableEntityItem(AmberEntityHome home, Entity cacheItem)
  {
    _home = home;
    _cacheItem = cacheItem;

    _expireTime = Alarm.getCurrentTime() + _home.getCacheTimeout();
  }

  /**
   * Returns the entity home.
   */
  AmberEntityHome getEntityHome()
  {
    return _home;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  public Entity getEntity()
  {
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      _expireTime = now + _home.getCacheTimeout();

      _cacheItem.__caucho_expire();
    }

    return _cacheItem;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  public Entity loadEntity(int loadGroup)
  {
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      _expireTime = now + _home.getCacheTimeout();
      _cacheItem.__caucho_expire();
    }

    AmberConnection aConn = _home.getManager().getCacheConnection();

    try {
      _cacheItem.__caucho_setConnection(aConn);
      _cacheItem.__caucho_retrieve(aConn);
    } catch (SQLException e) {
      // XXX: item is dead
      throw new RuntimeException(e);
    } finally {
      aConn.freeConnection();
    }

    return _cacheItem;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  public Entity loadEntity(AmberConnection aConn,
                           int loadGroup)
  {
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      _expireTime = now + _home.getCacheTimeout();
      _cacheItem.__caucho_expire();
    }

    // jpa/0v33

    try {
      _cacheItem.__caucho_setConnection(aConn);
      _cacheItem.__caucho_retrieve(aConn);
    } catch (SQLException e) {
      // XXX: item is dead
      throw new RuntimeException(e);
    }

    return _cacheItem;
  }

  /**
   * Creates a bean instance
   */
  public Entity copy(AmberConnection aConn)
  {
    return _cacheItem.__caucho_copy(aConn, this);
  }

  /**
   * Saves the item values into the cache.
   */
  public void save(Entity item)
  {
    /*
      long now = Alarm.getCurrentTime();

      synchronized (_cacheItem) {
      _expireTime = now + _home.getCacheTimeout();

      _cacheItem.__caucho_loadFromObject(item);
      }
    */
  }

  /**
   * Saves the item values into the cache.
   */
  public void savePart(Entity item)
  {
    /*
      synchronized (_cacheItem) {
      _cacheItem.__caucho_loadFromObject(item);
      }
    */
  }

  /**
   * Expire the value from the cache.
   */
  public void expire()
  {
    _cacheItem.__caucho_expire();
  }

  Class getInstanceClass()
  {
    return _cacheItem.getClass();
  }

  public String toString()
  {
    return "CacheableEntityItem[" + _cacheItem + "]";
  }
}
