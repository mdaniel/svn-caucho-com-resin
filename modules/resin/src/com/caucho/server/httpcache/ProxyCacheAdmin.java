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

package com.caucho.server.httpcache;

import java.util.regex.Pattern;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.CacheItem;
import com.caucho.management.server.ProxyCacheMXBean;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.InvocationServer;

public class ProxyCacheAdmin extends AbstractManagedObject
  implements ProxyCacheMXBean
{
  private final ProxyCache _cache;

  ProxyCacheAdmin(ProxyCache cache)
  {
    _cache = cache;
    
    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public String getType()
  {
    return "ProxyCache";
  }
  
  //
  // Statistics
  //
  
  /**
   * Returns the proxy cache hit count.
   */
  @Override
  public long getHitCountTotal()
  {
    return _cache.getHitCount();
  }

  /**
   * Returns the proxy cache miss count.
   */
  @Override
  public long getMissCountTotal()
  {
    return _cache.getMissCount();
  }
  
  @Override
  public double getMissRate()
  {
    long hitCount = getHitCountTotal();
    long missCount = getMissCountTotal();
    
    double accessCount = hitCount + missCount;
    
    if (accessCount == 0)
      accessCount = 1;
    
    return missCount / accessCount;
  }
  
  @Override
  public long getInvocationHitCountTotal()
  {
    InvocationServer server = _cache.getServer().getInvocationServer();
    
    return server.getInvocationCacheHitCount();
  }
  
  @Override
  public long getInvocationMissCountTotal()
  {
    InvocationServer server = _cache.getServer().getInvocationServer();
    
    return server.getInvocationCacheMissCount();
  }
  
  @Override
  public double getCacheableRate()
  {
    long hitCount = getHitCountTotal();
    long missCount = getMissCountTotal();
    
    double cacheableCount = hitCount + missCount;
    
    long invocationHitCount = getInvocationHitCountTotal();
    long invocationMissCount = getInvocationMissCountTotal();

    double accessCount = invocationHitCount + invocationMissCount;
    
    if (accessCount == 0)
      accessCount = 1;
    
    return cacheableCount / accessCount;
  }

  //
  // Operations
  //

  /**
   * Clears the cache.
   */
  @Override
  public void clearCache()
  {
    ServletService server = _cache.getServer();
    if (server != null)
      server.clearCache();
    
    _cache.clear();
  }

  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  @Override
  public void clearCacheByPattern(String hostRegexp,
                                  String urlRegexp)
  {
    try {
      Pattern hostPattern = null;
      Pattern urlPattern = null;

      if (hostRegexp != null)
        hostPattern = Pattern.compile(hostRegexp);
    

      if (urlRegexp != null)
        urlPattern = Pattern.compile(urlRegexp);

      _cache.clearCacheByPattern(hostPattern, urlPattern);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Clears the cache expires.
   */
  @Override
  public void clearExpires()
  {
    _cache.clearExpires();
  }

  @Override
  public CacheItem []getCacheableEntries(int max)
  {
    return _cache.getCacheableEntries(max);
  }

  @Override
  public CacheItem []getUncacheableEntries(int max)
  {
    return _cache.getUncacheableEntries(max);
  }

  @Override
  public CacheItem []getCachedEntries(int max)
  {
    return _cache.getCachedEntries(max);
  }

  @Override
  public CacheItem []getUncachedEntries(int max)
  {
    return _cache.getUncachedEntries(max);
  }
}
