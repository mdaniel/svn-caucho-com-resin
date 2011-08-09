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

package com.caucho.server.distcache;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.env.service.ResinSystem;
import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
public class FileCacheManager
  extends AbstractDataCacheManager<FileCacheEntry> {
  public FileCacheManager(ResinSystem resinSystem)
  {
    super(resinSystem);
  }

  /**
   * Returns the key entry.
   */
  @Override
  public FileCacheEntry createCacheEntry(Object key, HashKey hashKey)
  {
    TriadOwner owner = TriadOwner.A_B;
    
    return new FileCacheEntry(key, hashKey, owner, this);
  }

  /**
   * Sets a cache entry
   */
  @Override
  protected HashKey getAndPut(DistCacheEntry entry,
                              HashKey valueHash,
                              Object value,
                              HashKey cacheHash,
                              int flags,
                              long expireTimeout,
                              long idleTimeout,
                              long leaseTimeout,
                              long localReadTimeout,
                              int leaseOwner)
  {
    return getAndPutLocal(entry,
                          valueHash, 
                          value,
                          cacheHash,
                          flags,
                          expireTimeout,
                          idleTimeout,
                          leaseTimeout,
                          localReadTimeout,
                          leaseOwner);
  }

  @Override
  public HashKey compareAndPut(FileCacheEntry entry,
                               HashKey testValue,
                               HashKey valueHash,
                               Object value,
                               CacheConfig config)
  {
    int leaseOwner = -1;
    
    HashKey oldValueKey
      = compareAndPutLocal(entry, testValue, valueHash, value,
                           config.getCacheKey(),
                           config.getFlags(),
                           config.getExpireTimeout(),
                           config.getIdleTimeout(),
                           config.getLeaseTimeout(),
                           config.getLocalReadTimeout(),
                           leaseOwner);
    
    return oldValueKey;
  }

}
