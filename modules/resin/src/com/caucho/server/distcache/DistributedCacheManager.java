/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.distcache;

import com.caucho.cache.CacheEntry;
import com.caucho.cache.CacheSerializer;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.util.LruCache;

/**
 * Manages the distributed cache
 */
abstract public class DistributedCacheManager
{
  private final Server _server;

  protected DistributedCacheManager(Server server)
  {
    _server = server;
  }

  /**
   * Returns the owning cluster
   */
  protected Cluster getCluster()
  {
    return _server.getCluster();
  }

  /**
   * Gets a cache entry
   */
  abstract public Object get(HashKey hashKey, CacheSerializer serializer);

  /**
   * Sets a cache entry
   */
  abstract public void put(HashKey hashKey,
			   Object value,
			   CacheSerializer serializer);

  /**
   * Removes a cache entry
   */
  abstract public boolean remove(HashKey hashKey);

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server.getServerId() + "]";
  }
}
