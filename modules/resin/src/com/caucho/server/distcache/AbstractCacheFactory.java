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

import javax.cache.CacheFactory;
import javax.cache.Cache;
import java.util.Map;

public class AbstractCacheFactory
  extends AbstractCache
  implements CacheFactory
{
  public final String CACHE_NAME = "com.caucho.cluser.name";

  /**
   * Returns the requested cache.
   *
   * @param request is optional parameter that contains the name of the
   *        cache
   * @return
   */
  public Cache createCache(Map request)
  {
    String cacheName = ((request != null) && request.containsKey(CACHE_NAME))
      ? (String) request.get(CACHE_NAME) : getName();
    Cache result = getInstance(cacheName);
    if (result == null) {
      setName(cacheName);
      this.init();
      return this;
    } else
      return result;
  }

  public void init()
  {
    setTemplate(true);
  }
}
