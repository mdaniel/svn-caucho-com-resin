/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.distcache;

import com.caucho.cluster.CacheEntry;
import com.caucho.cluster.CacheLoader;
import com.caucho.cluster.CacheSerializer;
import com.caucho.cluster.HessianSerializer;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.LruCache;

/**
 * Manages the distributed cache
 */
public class CacheConfig
{
  public static final long TIME_INFINITY  = Long.MAX_VALUE / 2;
  public static final int FLAG_EPHEMERAL  = 0x01;
  public static final int FLAG_BACKUP     = 0x02;
  public static final int FLAG_TRIPLICATE = 0x04;

  private String _guid;
  
  private int _flags = (FLAG_BACKUP
			| FLAG_TRIPLICATE);

  private long _expireTimeout = TIME_INFINITY;
  
  private long _idleTimeout = TIME_INFINITY;
  
  private long _localReadTimeout
    = Alarm.isTest() ? -1 : 10L; // 10ms default timeout, except for QA

  private long _leaseTimeout = 5 * 60 * 1000; // 5 min lease timeout

  private CacheLoader _cacheLoader;

  private CacheSerializer _keySerializer;
  private CacheSerializer _valueSerializer;

  /**
   * The Cache will use a CacheLoader to populate cache misses.
   */
  public CacheLoader getCacheLoader()
  {
    return _cacheLoader;
  }

  /**
   * The Cache will use a CacheLoader to populate cache misses.
   */
  public void setCacheLoader(CacheLoader cacheLoader)
  {
    _cacheLoader = cacheLoader;
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public String getGuid()
  {
    return _guid;
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public void setGuid(String guid)
  {
    _guid = guid;
  }

  /**
   * Returns the flags
   */
  public int getFlags()
  {
    return _flags;
  }

  /**
   * Sets the flags
   */
  public void setFlags(int flags)
  {
    _flags = flags;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   *
   * Default is infinite.
   */
  public long getExpireTimeout()
  {
    return _expireTimeout;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   *
   * Default is infinite.
   */
  public void setExpireTimeout(long expireTimeout)
  {
    if (expireTimeout < 0 || TIME_INFINITY <= expireTimeout)
      expireTimeout = TIME_INFINITY;
    else
      _expireTimeout = expireTimeout;
  }

  /**
   * Returns the expire check window, i.e. the precision of the expire
   * check.  Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  public long getExpireCheckWindow()
  {
    return _expireTimeout / 4;
  }

  /**
   * The maximum idle time for an item.  For example, session
   * data might be removed if idle over 30 minutes.
   *
   * Cached data would typically have infinite idle time because
   * it doesn't depend on how often it's accessed.
   *
   * Default is infinite.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  /**
   * The maximum idle time for an item.  For example, session
   * data might be removed if idle over 30 minutes.
   *
   * Cached data would typically have infinite idle time because
   * it doesn't depend on how often it's accessed.
   */
  public void setIdleTimeout(long idleTimeout)
  {
    if (idleTimeout < 0 || TIME_INFINITY <= idleTimeout)
      idleTimeout = TIME_INFINITY;
    else
      _idleTimeout = idleTimeout;
  }

  /**
   * Returns the idle check window, i.e. the precision of the idle
   * check.
   */
  public long getIdleCheckWindow()
  {
    return _idleTimeout / 4;
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseTimeout()
  {
    return _leaseTimeout;
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public void setLeaseTimeout(long timeout)
  {
    _leaseTimeout = timeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public long getLocalReadTimeout()
  {
    return _localReadTimeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public void setLocalReadTimeout(long timeout)
  {
    _localReadTimeout = timeout;
  }

  /**
   * Returns the key serializer
   */
  public CacheSerializer getKeySerializer()
  {
    return _keySerializer;
  }

  /**
   * Returns the value serializer
   */
  public CacheSerializer getValueSerializer()
  {
    return _valueSerializer;
  }

  /**
   * Sets the value serializer
   */
  public void setValueSerializer(CacheSerializer serializer)
  {
    _valueSerializer = serializer;
  }

  public void init()
  {
    if (_keySerializer == null)
      _keySerializer = new HessianSerializer();
    
    if (_valueSerializer == null)
      _valueSerializer = new HessianSerializer();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
