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

import java.util.logging.Logger;

import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;

/**
 * Manages the distributed cache
 */
final class LocalMnodeManager
{
  private final CacheStoreManager _storeManager;
  
  LocalMnodeManager(CacheStoreManager storeManager)
  {
    _storeManager = storeManager;
  }
  
  private CacheStoreManager getStoreManager()
  {
    return _storeManager;
  }
  
  private CacheDataBacking getDataBacking()
  {
    return getStoreManager().getDataBacking();
  }

  /**
   * Gets a cache entry
   */
  final MnodeEntry loadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeEntry mnodeValue = cacheEntry.getMnodeEntry();

    if (mnodeValue == null || mnodeValue.isImplicitNull()) {
      MnodeEntry newMnodeValue = getDataBacking().loadLocalEntryValue(key);

      // cloud/6811
      cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

      mnodeValue = cacheEntry.getMnodeEntry();
    }

    return mnodeValue;
  }

  /**
   * Gets a cache entry
   */
  MnodeEntry forceLoadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeEntry mnodeValue = cacheEntry.getMnodeEntry();

    MnodeEntry newMnodeValue = getDataBacking().loadLocalEntryValue(key);

    cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

    mnodeValue = cacheEntry.getMnodeEntry();

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry putLocalValue(DistCacheEntry entry,
                                 MnodeUpdate mnodeUpdate,
                                 Object value)
  {
    HashKey key = entry.getKeyHash();
    
    long valueHash = mnodeUpdate.getValueHash();
    long version = mnodeUpdate.getVersion();
    
    MnodeEntry oldEntryValue;
    MnodeEntry mnodeValue;

    do {
      oldEntryValue = loadMnodeValue(entry);
    
      long oldValueHash
        = oldEntryValue != null ? oldEntryValue.getValueHash() : 0;

      long oldVersion = oldEntryValue != null ? oldEntryValue.getVersion() : 0;
      long now = CurrentTime.getCurrentTime();
      
      if (version < oldVersion
          || (version == oldVersion
              && valueHash != 0
              && valueHash <= oldValueHash)) {
        // lease ownership updates even if value doesn't
        if (oldEntryValue != null) {
          oldEntryValue.setLeaseOwner(mnodeUpdate.getLeaseOwner(), now);

          // XXX: access time?
          oldEntryValue.setLastAccessTime(now);
        }

        return oldEntryValue;
      }

      long accessTime = now;
      long updateTime = accessTime;
      long leaseTimeout = mnodeUpdate.getLeaseTimeout();

      mnodeValue = new MnodeEntry(mnodeUpdate,
                                  value,
                                  leaseTimeout,
                                  accessTime,
                                  updateTime,
                                  true,
                                  false);
    } while (! entry.compareAndSet(oldEntryValue, mnodeValue));

    //MnodeValue newValue
    getDataBacking().putLocalValue(mnodeValue, key,  
                                   oldEntryValue,
                                   mnodeUpdate);
    
    return mnodeValue;
  }

  public boolean compareAndPut(DistCacheEntry entry,
                               long testValueHash,
                               MnodeUpdate update,
                               Object value)
  {
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    long oldValueHash = (mnodeValue != null
                         ? mnodeValue.getValueHash()
                         : 0);
    
    if (oldValueHash != testValueHash) {
      return false;
    }
    
    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;

    mnodeValue = putLocalValue(entry, update, value);
    
    return (mnodeValue != null);
  }
}
