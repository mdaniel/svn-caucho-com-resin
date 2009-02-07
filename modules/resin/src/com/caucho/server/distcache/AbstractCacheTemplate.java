/**
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.server.distcache;

import com.caucho.cluster.AbstractCache;
import com.caucho.cluster.ClusterCache;

import javax.cache.CacheFactory;
import javax.cache.Cache;
import java.util.Map;

/**
 * This implementation of the {@link CacheFactory} interface
 * provides the means to obtain new instances of caches.
 */
public class AbstractCacheTemplate
  extends AbstractCache
  implements CacheFactory
{

  // The key to be used when using a map to provide the chache name as its value.
  //public static final String CACHE_NAME = "com.caucho.cluser.name";

  /**
   * Returns the requested cache.
   *
   * @param settings is optional parameter that contains the name of the
   *        cache as the value of the key CACHE_NAME
   * @return
   */
  public Cache createCache(Map settings)
  {
    String cacheName = ((settings != null) && settings.containsKey(CACHE_NAME))
      ? (String) settings.get(CACHE_NAME) : getName();

    return new ClusterCache(cacheName);
  }

  /**
   * Create a new cache directly by name.
   * @param cacheName
   * @return
   */
  public Cache createCache(String cacheName)
  {
    if (isOpen(cacheName))
      duplicateCacheNameException(cacheName);

    ClusterCache result = new ClusterCache();
    result.setName(cacheName);
    result.init();

    return result;
  }

  /**
   * Tests to see if the cacheName is currently open
   * in this environment.
   */
  protected boolean isOpen(String cacheName)
  {
    return getLocalCacheNameSet().contains(cacheName);
  }

}
