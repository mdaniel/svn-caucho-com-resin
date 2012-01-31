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

package com.caucho.server.distcache;

import java.util.concurrent.ConcurrentHashMap;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.sql.DataSource;

import com.caucho.distcache.jcache.CacheBuilderImpl;
import com.caucho.env.service.*;
import com.caucho.loader.Environment;

/**
 * The local cache repository.
 */
public class DistCacheSystem extends AbstractResinSubSystem 
{
  public static final int START_PRIORITY = START_PRIORITY_CACHE_SERVICE;

  private ConcurrentHashMap<String,CacheManagerImpl> _managerMap
    = new ConcurrentHashMap<String,CacheManagerImpl>(); 
  
  private CacheStoreManager _distCacheManager;
  
  private DataSource _jdbcDataSource;
  
  public DistCacheSystem(CacheStoreManager distCacheManager)
  {
    if (distCacheManager == null)
      throw new NullPointerException();

    _distCacheManager = distCacheManager;
  }
  
  public static DistCacheSystem 
    createAndAddService(CacheStoreManager distCacheManager)
  {
    ResinSystem system = preCreate(DistCacheSystem.class);

    DistCacheSystem service = new DistCacheSystem(distCacheManager);
    system.addService(DistCacheSystem.class, service);

    return service;
  }

  public static DistCacheSystem getCurrent()
  {
    return ResinSystem.getCurrentService(DistCacheSystem.class);
  }

  public static CacheImpl getMatchingCache(String name)
  {
    DistCacheSystem cacheService = getCurrent();
    
    if (cacheService == null)
      return null;
    
    CacheManagerImpl localManager = cacheService.getCacheManager();

    String contextId = Environment.getEnvironmentName();

    String guid = contextId + ":" + name;

    return (CacheImpl) localManager.getCache(guid);
  }

  public CacheStoreManager getDistCacheManager()
  {
    return _distCacheManager;
  }
  
  public CacheManagerImpl getCacheManager()
  {
    String name = "Resin";
    
    return getCacheManager(name);
  }
  
  public CacheManagerImpl getCacheManager(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return getCacheManager(name, loader);
  }
  
  public CacheManagerImpl getCacheManager(String name, ClassLoader loader)
  {
    String guid = name + ":" + Environment.getEnvironmentName(loader);
    
    CacheManagerImpl cacheManager = _managerMap.get(guid);
    
    if (cacheManager == null) {
      cacheManager = new CacheManagerImpl(this, name, guid, loader);
      
      _managerMap.putIfAbsent(guid, cacheManager);
    }
    
    return _managerMap.get(guid);
  }
  
  void removeCacheManager(String guid)
  {
    _managerMap.remove(guid);
  }
  
  public DataSource getJdbcDataSource()
  {
    return _jdbcDataSource;
  }
  
  public void setJdbcDataSource(DataSource dataSource)
  {
    _jdbcDataSource = dataSource;
  }

  /*
  public CacheBuilderImpl createBuilder(String name)
  {
    return getCacheManager()new CacheBuilderImpl(name, _cacheManager, _distCacheManager);
  }
  */
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    _distCacheManager.start();
  }
  
  @Override
  public void stop()
  {
    _distCacheManager.close();
    // _cacheManager.close();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _distCacheManager + "]";
  }
}
