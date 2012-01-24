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

package javax.cache;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.WeakHashMap;

import javax.cache.spi.CacheManagerFactory;
import javax.cache.spi.CachingProvider;


/**
 * Provides the capability of dynamically creating a cache.
 *
 * See  the  default implementation of this interface in {@link com.caucho.cluster.CacheTemplate}
 * for additional methods.
 */
public final class Caching {
  public static final String DEFAULT_CACHE_MANAGER_NAME = "__default__";
  
  private Caching()
  {
  }
  
  public static CacheManagerFactory getCacheManagerFactory()
  {
    throw new UnsupportedOperationException();
  }
  
  public static CacheManager getCacheManager()
  {
    return getCacheManager(DEFAULT_CACHE_MANAGER_NAME);
  }
  
  public static CacheManager getCacheManager(ClassLoader classLoader)
  {
    return getCacheManager(classLoader, DEFAULT_CACHE_MANAGER_NAME);
  }
  
  public static CacheManager getCacheManager(String name)
  {
    return CachingSingleton.INSTANCE.getCacheManager(name);
  }
  
  public static CacheManager getCacheManager(ClassLoader classLoader,
                                             String name)
  {
    return CachingSingleton.INSTANCE.getCacheManager(classLoader, name);
  }
  
  public static void close()
    throws CachingShutdownException
  {
    throw new UnsupportedOperationException();
  }
  
  public static void close(ClassLoader classLoader)
    throws CachingShutdownException
  {
    throw new UnsupportedOperationException();
  }
  
  public static void close(ClassLoader classLoader, String name)
    throws CachingShutdownException
  {
      throw new UnsupportedOperationException();
  }
  
  public static boolean isSupported(OptionalFeature optionalFeature)
  {
    throw new UnsupportedOperationException();
  }
  
  public static boolean isAnnotationsSupported()
  {
    throw new UnsupportedOperationException();
  }

  private static final class CachingSingleton {
    private static CachingSingleton INSTANCE = new CachingSingleton();
    
    private final CachingProvider _cachingProvider;
    
    private CachingSingleton()
    {
      ServiceLoader<CachingProvider> serviceLoader
        = ServiceLoader.load(CachingProvider.class);
      
      Iterator<CachingProvider> iter = serviceLoader.iterator();
      
      _cachingProvider = iter.hasNext() ? iter.next() : null;
    }
    
    public CacheManager getCacheManager(String name)
    {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      return getCacheManager(loader, name);
    }
    
    public CacheManager getCacheManager(ClassLoader classLoader,
                                        String name)
    {
      if (classLoader == null)
        throw new NullPointerException();
      
      if (name == null)
        throw new NullPointerException();
      
      if (_cachingProvider == null)
        throw new IllegalStateException("Cannot find a CachingProvider"); 
    
      CacheManagerFactory factory;
      
      factory = _cachingProvider.getCacheManagerFactory();
      //System.out.println("FACTOR: " + factory);
      return factory.getCacheManager(classLoader, name);
    }
  }
}