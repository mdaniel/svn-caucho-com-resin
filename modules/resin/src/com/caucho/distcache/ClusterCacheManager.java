/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.distcache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.UserTransaction;

import com.caucho.config.Configurable;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;

/**
 * Cache which stores consistent copies on the cluster segment.
 *
 * Using the cache is like using java.util.Map.  To add a new entry,
 * call <code>cache.put(key, value)</code>.  To get the entry, call
 * <code>cache.get(key)</code>.
 *
 * The cache configuration affects the lifetime, local caching timeouts
 * and consistency.
 */

@ApplicationScoped
@Configurable
public class ClusterCacheManager implements CacheManager
{
  private static final L10N L = new L10N(ClusterCacheManager.class);
  
  public static final String NAME = "name";

  private String _contextId;
  
  private ConcurrentHashMap<String,ClusterCache> _cacheMap;

  public ClusterCacheManager()
  {
    _cacheMap = new ConcurrentHashMap<String,ClusterCache>();
    
    _contextId = Environment.getEnvironmentName();
  }

  /*
   * @see javax.cache.CacheFactory#createCache(java.util.Map)
   */
  @Override
  public Cache getCache(String name) throws CacheException
  {
    if (name == null)
      throw new IllegalArgumentException(L.l("Cache 'name' is required."));
    
    ClusterCache cache = _cacheMap.get(name);
    
    if (cache == null) {
      cache = new ClusterCache();
      cache.setName(name);
      cache.setGuid(_contextId + ":" + name);
      cache.init();
      
      _cacheMap.putIfAbsent(name, cache);
      
      cache = _cacheMap.get(name);
    }
    
    return cache;
  }

  /* (non-Javadoc)
   * @see javax.cache.CacheManager#getUserTransaction()
   */
  @Override
  public UserTransaction getUserTransaction()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
 
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextId + "]";
  }
}
