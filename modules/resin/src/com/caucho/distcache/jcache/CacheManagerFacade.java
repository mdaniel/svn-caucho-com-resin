/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.distcache.jcache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.UserTransaction;

import com.caucho.cache.Cache;
import com.caucho.cache.CacheManager;
import com.caucho.cache.Configuration;
import com.caucho.cache.OptionalFeature;
import com.caucho.cache.Status;
import com.caucho.config.ConfigException;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.CacheImpl;
import com.caucho.server.distcache.CacheManagerImpl;
import com.caucho.server.distcache.DistCacheSystem;
import com.caucho.transaction.UserTransactionProxy;

/**
 * Caching Provider for jcache
 */
public class CacheManagerFacade implements CacheManager
{
  private CacheManagerImpl _manager;
  
  private ConcurrentHashMap<String,Cache<?,?>> _cacheMap
    = new ConcurrentHashMap<String,Cache<?,?>>();
  
  public CacheManagerFacade(String name, 
                            ClassLoader loader,
                            CacheManagerImpl manager)
  {
    _manager = manager;
  }

  @Override
  public String getName()
  {
    return _manager.getName();
  }
  
  public String getGuid()
  {
    return _manager.getGuid();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getCache(String name)
  {
    return (Cache) _cacheMap.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<Cache<?, ?>> getCaches()
  {
    return new HashSet(_cacheMap.values());
  }
  
  void addCache(String name, Cache<?,?> cache)
  {
    _cacheMap.putIfAbsent(name, cache);
  }

  @Override
  public <K, V> Cache<K, V> configureCache(String cacheName,
                                           Configuration<K, V> configuration)
  {
    CacheConfig config = new CacheConfig(configuration);
    
    DistCacheSystem cacheService = DistCacheSystem.getCurrent();

    if (cacheService != null) {
      config.setEngine(cacheService.getDistCacheManager().getCacheEngine());
    }
    
    CacheImpl<K,V> cache = _manager.createIfAbsent(cacheName, config);
    
    _cacheMap.put(cacheName, cache);
    
    return cache;
  }

  @Override
  public void enableStatistics(String cacheName, boolean enabled)
  {

  }

  @Override
  public Status getStatus()
  {
    if (_manager != null)
      return Status.STARTED;
    else
      return Status.STOPPED;
  }

  @Override
  public UserTransaction getUserTransaction()
  {
    return UserTransactionProxy.getCurrent();
  }

  @Override
  public boolean isSupported(OptionalFeature optionalFeature)
  {
    return false;
  }

  @Override
  public boolean removeCache(String cacheName) throws IllegalStateException
  {
    return _cacheMap.remove(cacheName) != null;
  }

  @Override
  public void shutdown()
  {
    _manager = null;
  }

  @Override
  public <T> T unwrap(Class<T> cl)
  {
    return null;
  }
  
  public void close()
  {
    shutdown();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "," + getGuid() + "]";
  }
}
