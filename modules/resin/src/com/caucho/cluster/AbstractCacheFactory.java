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

package com.caucho.cluster;

import javax.cache.Cache;
import javax.cache.CacheFactory;
import java.util.Map;
import java.util.HashMap;

public class AbstractCacheFactory implements CacheFactory {

  /**
   * Creates a cache based on the parameters in the request map.
   * The map must contain String for the cache name, entered with a String
   * key of "javax.cache.cache.name"
   * @param request map
   * @return the requested Cache
   * @throws CacheException if paramenters are invalid.
   */
  public Cache createCache(Map request) throws CacheException
  {
    return AbstractCache.getInstance(request);
  }

  /**
   * Provides a convenience method.
   * @param cacheName
   * @return
   */
  public Cache createCache(String cacheName)
  {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(AbstractCache.CACHE_NAME, cacheName);
    return AbstractCache.getInstance(request);
  }
}
