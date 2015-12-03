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

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Name;

/**
 * Management interface for the proxy cache.
 *
 * <pre>
 * resin:type=ProxyCache
 * </pre>
 */
@Description("Resin's integrated proxy cache")
public interface ProxyCacheMXBean extends ManagedObjectMXBean {
  //
  // Statistics
  //
  
  /**
   * Returns the proxy cache hit count.
   */
  @Description("The number of cacheable requests that hit the cache")
  public long getHitCountTotal();

  /**
   * Returns the proxy cache miss count.
   */
  @Description("The number of cacheable requests that miss the cache")
  public long getMissCountTotal();
  
  /**
   * Returns the proxy cache miss count.
   */
  public double getMissRate();
  
  /**
   * Returns the invocation hit count
   */
  @Description("The total invocation hits")
  public long getInvocationHitCountTotal();
  
  /**
   * Returns the invocation miss count
   */
  @Description("The total invocation misses")
  public long getInvocationMissCountTotal();
  
  /**
   * Returns the cacheable rate
   */
  @Description("The cacheable rate")
  public double getCacheableRate();

  /**
   * Return most used cacheable connections.
   */
  public CacheItem []getCacheableEntries(int max);

  /**
   * Return most used uncacheable connections.
   */
  public CacheItem []getUncacheableEntries(int max);

  /**
   * Return most used cached connections.
   */
  public CacheItem []getCachedEntries(int max);

  /**
   * Return most used uncached connections.
   */
  public CacheItem []getUncachedEntries(int max);

  //
  // Operations
  //

  /**
   * Clears the cache.
   */
  @Description("Clear the cache")
  public void clearCache();

  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  @Description("Selectively clear the cache using patterns")
  public void clearCacheByPattern(
    @Name("hostRegexp")
    @Description("A regular expression that matches a host name, null to match all host names")
    String hostRegexp,

    @Name("urlRegexp")
    @Description("A regular expression that matches a url, null to match all urls")
    String urlRegexp);

  /**
   * Clears the expires timers for the cache.
   */
  @Description("Clear expires")
  public void clearExpires();
}
