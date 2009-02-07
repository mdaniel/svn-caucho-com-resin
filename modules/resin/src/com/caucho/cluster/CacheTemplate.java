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

import com.caucho.server.distcache.AbstractCacheTemplate;
import com.caucho.config.ConfigException;

import javax.cache.Cache;
import javax.cache.CacheFactory;
import java.util.Map;

/**
 * This implementation of the {@link CacheFactory} interface provide
 * additional capabilities to manage caches.
 *
 * @note To use as a template, the properties should be set in
 * a configuration file. 
 */
public class CacheTemplate
  extends AbstractCacheTemplate
  implements CacheFactory
{

  /**
   * Returns a cache with the requested name.
   *
   * @param cacheName
   * @return
   * @throws ConfigException if a cache with that name is open.
   */
  @Override
  public Cache createCache(String cacheName) throws ConfigException
  {
    return super.createCache(cacheName);
  }

  /**
   * Returns a new cache.
   *
   * @param settings of the cache configuration. In the current
   *                 implementation, only the name may be changed. Use
   *                 {@link #CACHE_NAME} as the key.
   * @return
   */
  @Override
  public Cache createCache(Map settings)
  {
    return super.createCache(settings);
  }
}
