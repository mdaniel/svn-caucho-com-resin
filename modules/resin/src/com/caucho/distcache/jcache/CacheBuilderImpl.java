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

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.Configuration.Duration;
import javax.cache.Configuration.ExpiryType;
import javax.cache.event.CacheEntryListener;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

import com.caucho.distcache.ClusterCache;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.CacheManagerImpl;

/**
 * Caching Provider for jcache
 */
public class CacheBuilderImpl<K,V> implements CacheBuilder<K,V>
{
  private String _name;
  private String _guid;
  private CacheManagerFacade _manager;
  private ClusterCache _cache;
  
  CacheBuilderImpl(String name, CacheManagerFacade manager)
  {
    _name = name;
    _manager = manager;
    
    _guid = _manager.getGuid() + ":" + name;
    
    _cache = new ClusterCache();
    _cache.setName(name);
    _cache.setCacheManager(manager);
    
  }

  @Override
  public Cache<K,V> build()
  {
    _cache.init();
    
    _manager.addCache(_name, _cache);
    
    return _manager.getCache(_name);
  }

  @Override
  public CacheBuilder<K,V> registerCacheEntryListener(CacheEntryListener<K,V> listener)
  {
    return this;
  }

  @Override
  public CacheBuilder<K,V> setCacheLoader(CacheLoader<K,? extends V> cacheLoader)
  {
    _cache.getConfig().setCacheLoader(cacheLoader);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setReadThrough(boolean readThrough)
  {
    _cache.getConfig().setReadThrough(readThrough);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setCacheWriter(CacheWriter<? super K,? super V> cacheWriter)
  {
    _cache.getConfig().setCacheWriter(cacheWriter);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setWriteThrough(boolean isWriteThrough)
  {
    _cache.getConfig().setWriteThrough(isWriteThrough);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setExpiry(ExpiryType type, Duration timeToLive)
  {
    _cache.getConfig().setExpiry(type, timeToLive);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setStatisticsEnabled(boolean isEnable)
  {
    _cache.getConfig().setStatisticsEnabled(isEnable);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setStoreByValue(boolean storeByValue)
  {
    _cache.getConfig().setStoreByValue(storeByValue);
    
    return this;
  }

  @Override
  public CacheBuilder<K,V> setTransactionEnabled(IsolationLevel isolationLevel,
                                                 Mode mode)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }
}
