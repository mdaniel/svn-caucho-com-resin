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

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.spi.AnnotationProvider;
import javax.cache.spi.CachingProvider;


/**
 * Provides the capability of dynamically creating a cache.
 *
 * See  the  default implementation of this interface in {@link com.caucho.cluster.CacheTemplate}
 * for additional methods.
 */
public final class Caching {
  private static final Logger log = Logger.getLogger(Caching.class.getName());
  
  public static final String DEFAULT_CACHE_MANAGER_NAME = "__default__";
  
  private static final WeakHashMap<ClassLoader,CachingSingleton> _cacheMap
    = new WeakHashMap<ClassLoader,CachingSingleton>();
  
  private static final ClassLoader _systemClassLoader;
  
  private Caching()
  {
  }
  
  public static CacheManagerFactory getCacheManagerFactory()
  {
    return getCachingSingleton().getFactory();
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
    return getCacheManagerFactory().getCacheManager(name);
  }
  
  public static CacheManager getCacheManager(ClassLoader classLoader,
                                             String name)
  {
    return getCacheManagerFactory().getCacheManager(classLoader, name);
  }
  
  public static void close()
    throws CachingShutdownException
  {
    getCacheManagerFactory().close();
  }
  
  public static void close(ClassLoader classLoader)
    throws CachingShutdownException
  {
      getCacheManagerFactory().close(classLoader);
  }
  
  public static void close(ClassLoader classLoader, String name)
    throws CachingShutdownException
  {
    getCacheManagerFactory().close(classLoader, name);
  }
  
  public static boolean isSupported(OptionalFeature feature)
  {
    return getCachingSingleton().getProvider().isSupported(feature);
  }
  
  public static boolean isAnnotationsSupported()
  {
    return getCachingSingleton().isAnnotationsSupported();
  }
  
  private static CachingSingleton getCachingSingleton()
  {
    CachingSingleton caching = null;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    if (loader == null)
      loader = _systemClassLoader;
    
    synchronized (_cacheMap) {
      caching =_cacheMap.get(loader);
    
      if (caching == null) {
        caching = new CachingSingleton(loader);
      
        _cacheMap.put(loader, caching);
      }
    }

    return caching;
  }

  private static final class CachingSingleton {
    private WeakReference<ClassLoader> _loaderRef;
    private SoftReference<CachingProvider> _providerRef;
    private SoftReference<AnnotationProvider> _annProviderRef;
    
    CachingSingleton(ClassLoader loader)
    {
      _loaderRef = new WeakReference<ClassLoader>(loader);
    }
    
    CacheManagerFactory getFactory()
    {
      return getProvider().getCacheManagerFactory();
    }
    
    public boolean isAnnotationsSupported()
    {
      return getAnnProvider() != null;
    }
    
    private synchronized CachingProvider getProvider()
    {
      CachingProvider provider = null;
      
      if (_providerRef != null)
        provider = _providerRef.get();
      
      if (provider == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();
        
        try {
          thread.setContextClassLoader(_loaderRef.get());
          
          ServiceLoader<CachingProvider> serviceLoader
            = ServiceLoader.load(CachingProvider.class);
  
          Iterator<CachingProvider> iter = serviceLoader.iterator();
  
          provider = iter.hasNext() ? iter.next() : null;
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
        
        if (provider == null)
          throw new IllegalStateException("Cannot find a CachingProvider: "
                                          + _loaderRef.get());
        
        if (log.isLoggable(Level.FINE)) {
          log.fine("Caching: using provider " + provider);
        }
        
        _providerRef = new SoftReference<CachingProvider>(provider);
      }
      
      return provider;
    }
    
    private synchronized AnnotationProvider getAnnProvider()
    {
      AnnotationProvider provider = null;
      
      if (_annProviderRef != null)
        provider = _annProviderRef.get();
      
      if (provider == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();
        
        try {
          thread.setContextClassLoader(_loaderRef.get());
          
          ServiceLoader<AnnotationProvider> serviceLoader
            = ServiceLoader.load(AnnotationProvider.class);
  
          Iterator<AnnotationProvider> iter = serviceLoader.iterator();
  
          provider = iter.hasNext() ? iter.next() : null;
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
        
        if (provider == null)
          return null;
        
        if (log.isLoggable(Level.FINE)) {
          log.fine("Caching: using provider " + provider);
        }
        
        _annProviderRef = new SoftReference<AnnotationProvider>(provider);
      }
      
      return provider;
    }
  }
  
  static {
    ClassLoader systemClassLoader = null;
    
    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _systemClassLoader = systemClassLoader;
  }
}