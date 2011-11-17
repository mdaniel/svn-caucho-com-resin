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

package com.caucho.distcache.jcache;

import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheManager;
import javax.cache.OptionalFeature;
import javax.cache.Status;
import javax.transaction.UserTransaction;

import com.caucho.loader.Environment;

/**
 * Caching Provider for jcache
 */
public class CacheManagerFacade implements CacheManager
{
  private String _name;
  private String _guid;
  
  public CacheManagerFacade(String name, ClassLoader loader)
  {
    _name = name;

    _guid = Environment.getEnvironmentName(loader) + ":" + name;
  }

  @Override
  public String getName()
  {
    return _name;
  }
  
  public String getGuid()
  {
    return _guid;
  }

  @Override
  public void addImmutableClass(Class<?> immutableClass)
  {
  }

  @Override
  public <K, V> CacheBuilder<K, V> createCacheBuilder(String cacheName)
  {
    return new CacheBuilderImpl<K,V>(cacheName, this);
  }

  @Override
  public <K, V> Cache<K, V> getCache(String name)
  {
    return null;
  }

  @Override
  public <K, V> Set<Cache<K, V>> getCaches()
  {
    return null;
  }

  @Override
  public Status getStatus()
  {
    return null;
  }

  @Override
  public UserTransaction getUserTransaction()
  {
    return null;
  }

  @Override
  public boolean isSupported(OptionalFeature optionalFeature)
  {
    return false;
  }

  @Override
  public boolean removeCache(String cacheName) throws IllegalStateException
  {
    return false;
  }

  @Override
  public void shutdown()
  {
  }

  @Override
  public <T> T unwrap(Class<T> cl)
  {
    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "," + _guid + "]";
  }
}
