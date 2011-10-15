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

import java.io.IOException;
import java.io.InputStream;

import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
public interface CacheEngine
{
  /**
   * Starts the service
   */
  public void start();

  /**
   * Called when a cache initializes.
   */
  public void initCache(CacheImpl cache);

  /**
   * Called when a cache is removed.
   */
  public void destroyCache(CacheImpl cache);

  public void closeCache(String name);
  
  public HashKey createSelfHashKey(Object key, CacheSerializer keySerializer);

  /**
   * Gets a cache key entry
   */
  public DistCacheEntry getCacheEntry(Object key, CacheConfig config);

  /**
   * Gets a cache key entry
   */
  public DistCacheEntry getCacheEntry(HashKey key, CacheConfig config);

  /**
   * Sets a cache entry
   */
  public void put(HashKey hashKey,
                  Object value,
                  CacheConfig config);

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(HashKey hashKey,
                           InputStream is,
                           CacheConfig config,
                           long idleTimeout)
    throws IOException;

  /**
   * Removes a cache entry
   */
  public boolean remove(HashKey hashKey);

  public CacheDataBacking getDataBacking();
  
  /**
   * For QA
   */
  public byte[] calculateValueHash(Object value, CacheConfig config);
  
  /**
   * Closes the manager
   */
  public void close();

  /**
   * @param mnodeUpdate
   * @param value
   * @param leaseTimeout
   * @param leaseOwner
   * @return
   */
  public HashKey getAndPut(MnodeUpdate mnodeUpdate, Object value,
                           long leaseTimeout, int leaseOwner);

  /**
   * @param testValue
   * @param mnodeUpdate
   * @param value
   * @param config
   * @return
   */
  public HashKey compareAndPut(HashKey testValue, MnodeUpdate mnodeUpdate,
                               Object value, CacheConfig config);

  /**
   * @param config
   * @param mnodeEntry
   * @param now
   * @return
   */
  boolean isLocalReadValid(CacheConfig config, MnodeEntry mnodeEntry, long now);

  /**
   * @param key
   * @param mnodeValue
   */
  void updateCacheTime(HashKey key, MnodeEntry mnodeValue);

  /**
   * @param valueKey
   * @param flags
   * @return
   */
  boolean loadClusterData(HashKey valueKey, int flags);

  /**
   * @param entry
   * @param mnodeUpdate
   * @param value
   * @param leaseTimeout
   * @param leaseOwner
   * @return
   */
  HashKey getAndPut(DistCacheEntry entry, MnodeUpdate mnodeUpdate,
                    Object value, long leaseTimeout, int leaseOwner);

  /**
   * @param entry
   * @param testValue
   * @param mnodeUpdate
   * @param value
   * @param config
   * @return
   */
  HashKey compareAndPut(DistCacheEntry entry, HashKey testValue,
                        MnodeUpdate mnodeUpdate, Object value,
                        CacheConfig config);

  /**
   * @return
   */
  CacheDataBacking createDataBacking();

  /**
   * @param entry
   * @param config
   * @return
   */
  public MnodeEntry loadClusterValue(DistCacheEntry entry, CacheConfig config);

  /**
   * @param key
   * @param mnodeUpdate
   * @param mnodeValue
   */
  public void putCluster(HashKey key, MnodeUpdate mnodeUpdate,
                         MnodeEntry mnodeValue);

  /**
   * @param key
   * @param mnodeUpdate
   * @param mnodeEntry
   */
  public void removeCluster(HashKey key, MnodeUpdate mnodeUpdate,
                            MnodeEntry mnodeEntry);
}
