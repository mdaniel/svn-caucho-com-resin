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

import com.caucho.inject.Module;
import com.caucho.util.HashKey;

/**
 * Manages the server entries for the distributed cache
 */
@Module
final class CacheRegionManager
{
  private final ConcurrentHashMap<HashKey, CacheHandle> _regionCache
    = new ConcurrentHashMap<HashKey, CacheHandle>();
  
  public CacheHandle getCache(HashKey key)
  {
    if (key == null) {
      return null;
    }
    
    return _regionCache.get(key);
  }
  
  public CacheHandle createCache(HashKey key, CacheConfig config)
  {
    if (key == null) {
      throw new NullPointerException();
    }
    
    CacheHandle cache = _regionCache.get(key);
    
    if (cache == null) {
      cache = new CacheHandle(key, config);
      
      cache.setConfig(config);
      
      _regionCache.putIfAbsent(key, cache);
      
      cache = _regionCache.get(key);
    }
    
    return cache;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
