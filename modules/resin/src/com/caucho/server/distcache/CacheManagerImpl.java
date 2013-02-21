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

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;

import com.caucho.distcache.jcache.CacheManagerFacade;
import com.caucho.loader.Environment;

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

public class CacheManagerImpl implements Closeable
{
  private DistCacheSystem _cacheSystem;
  private String _name;
  private String _guid;
  private ConcurrentHashMap<String,CacheImpl> _cacheMap;
  
  private CacheManagerFacade _facade;

  public CacheManagerImpl(DistCacheSystem cacheSystem,
                          String name,
                          String guid, 
                          ClassLoader loader)
  {
    _cacheSystem = cacheSystem;
    _name = name;
    _guid = guid;

    _cacheMap = new ConcurrentHashMap<String,CacheImpl>();
    
    _facade = new CacheManagerFacade(_name, loader, this);
    
    Environment.addCloseListener(this, loader);
  }
  
  public String getName()
  {
    return _name;
  }
  
  public String getGuid()
  {
    return _guid;
  }
  
  public CacheImpl getCache(String name)
  {
    return _cacheMap.get(name);
  }

  public CacheImpl createIfAbsent(String name, CacheConfig config)
  {
    CacheImpl cache = _cacheMap.get(name);
    
    if (cache == null) {
      String guid = name + ":" + _guid;
   
      config.setGuid(guid);
      cache = new CacheImpl(this, name, getName(), guid, config);

      _cacheMap.putIfAbsent(name, cache);
    }
    
    return _cacheMap.get(name);
  }
  
  public void remove(String name)
  {
    _cacheMap.remove(name);
  }
  
  @Override
  public void close()
  {
    _cacheMap.clear();
    
    _cacheSystem.removeCacheManager(_guid);
  }
}
