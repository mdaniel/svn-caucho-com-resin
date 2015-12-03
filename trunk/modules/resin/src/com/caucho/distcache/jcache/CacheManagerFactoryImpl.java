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

import java.util.ArrayList;
import java.util.HashMap;

import javax.cache.CacheManager;
import javax.cache.CacheManagerFactory;
import javax.cache.CachingShutdownException;

import com.caucho.config.ConfigException;
import com.caucho.server.distcache.CacheManagerImpl;
import com.caucho.server.distcache.DistCacheSystem;
import com.caucho.util.L10N;

/**
 * Caching Provider for jcache
 */
public class CacheManagerFactoryImpl implements CacheManagerFactory
{
  private static final L10N L = new L10N(CacheManagerFactoryImpl.class);
  
  private HashMap<String,CacheManagerFacade> _cacheManagerMap
    = new HashMap<String,CacheManagerFacade>();
  
  @Override
  public CacheManager getCacheManager(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return getCacheManager(loader, name);
  }

  @Override
  public CacheManager getCacheManager(ClassLoader classLoader, String name)
  {
    synchronized (_cacheManagerMap) {
      CacheManagerFacade cm = _cacheManagerMap.get(name);
    
      if (cm == null) {
        DistCacheSystem cacheService = DistCacheSystem.getCurrent();

        if (cacheService == null)
          throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a Resin environment",
                                        getClass().getSimpleName()));
        
        CacheManagerImpl manager;
        
        if (name != null)
          manager = cacheService.getCacheManager(name);
        else
          manager = cacheService.getCacheManager();
        
        cm = new CacheManagerFacade(name, classLoader, manager);
        
        _cacheManagerMap.put(name, cm);
      }
      
      return cm;
    }
  }

  @Override
  public void close() throws CachingShutdownException
  {
    ArrayList<CacheManagerFacade> managerList
      = new ArrayList<CacheManagerFacade>();
  
    synchronized (_cacheManagerMap) {
      managerList.addAll(_cacheManagerMap.values());
    
      _cacheManagerMap.clear();
    }
  
    for (CacheManagerFacade manager : managerList) {
      manager.shutdown();
    }
  }

  @Override
  public void close(ClassLoader classLoader) throws CachingShutdownException
  {
    ArrayList<CacheManagerFacade> managerList
      = new ArrayList<CacheManagerFacade>();
  
    synchronized (_cacheManagerMap) {
      managerList.addAll(_cacheManagerMap.values());
    
      _cacheManagerMap.clear();
    }
  
    for (CacheManagerFacade manager : managerList) {
      manager.shutdown();
    }
  }

  @Override
  public void close(ClassLoader classLoader, String name)
      throws CachingShutdownException
  {
    CacheManagerFacade cm;
    
    synchronized (_cacheManagerMap) {
      cm = _cacheManagerMap.remove(name);
    }
    
    if (cm != null)
      cm.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
