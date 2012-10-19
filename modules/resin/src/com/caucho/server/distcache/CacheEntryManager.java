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

package com.caucho.server.distcache;

import java.util.Iterator;

import com.caucho.env.service.ResinSystem;
import com.caucho.inject.Module;
import com.caucho.util.HashKey;
import com.caucho.util.LruCache;

/**
 * Manages the server entries for the distributed cache
 */
@Module
public final class CacheEntryManager
{
  private final String _serverId;
  
  private final CacheEntryFactory _cacheEntryFactory;
  
  private final LruCache<HashKey, DistCacheEntry> _entryCache
    = new LruCache<HashKey, DistCacheEntry>(64 * 1024);
  
  public CacheEntryManager(CacheEntryFactory cacheEntryFactory)
  {
    _serverId = ResinSystem.getCurrentId();
    _cacheEntryFactory = cacheEntryFactory;
  }

  /**
   * Returns the cache entry.
   */
  public final DistCacheEntry getCacheEntry(HashKey key)
  {
    return _entryCache.get(key);
  }

  /**
   * Returns the key entry, creating on if necessary.
   */
  public final DistCacheEntry createCacheEntry(HashKey key,
                                               CacheHandle cache)
  {
    DistCacheEntry cacheEntry = _entryCache.get(key);
    
    if (cacheEntry != null) {
      return cacheEntry;
    }
    
    cacheEntry = _cacheEntryFactory.createCacheEntry(key, cache);

    cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    
    // cloud/60n2
    cacheEntry.loadLocalMnodeValue();

    return cacheEntry;
  }
  
  public Iterator<DistCacheEntry> getEntries()
  {
    return _entryCache.values();
  }

  /**
   * Clears leases on server start/stop
   */
  final public void clearLeases()
  {
    Iterator<DistCacheEntry> iter = _entryCache.values();

    while (iter.hasNext()) {
      DistCacheEntry entry = iter.next();

      entry.clearLease();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _serverId + "]";
  }
}
