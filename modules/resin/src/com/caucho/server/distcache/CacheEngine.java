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

import java.io.InputStream;

import com.caucho.server.distcache.LocalDataManager.DataItemLocal;
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

  public void initCache(CacheImpl cache);

  public int getServerIndex();

  public boolean isLocalExpired(CacheConfig config,
                                HashKey key,
                                MnodeEntry mnodeEntry, 
                                long now);
  
  public MnodeValue get(DistCacheEntry entry);
  
  /*
  public boolean loadData(HashKey key, 
                          HashKey valueKey, long valueIndex,
                          int flags);
                          */

  public void put(HashKey key, 
                  HashKey cacheKey,
                  MnodeUpdate mnodeUpdate,
                  long valueDataId,
                  long valueDataTime);

  public void updateTime(HashKey key, HashKey cacheKey, MnodeEntry mnodeValue);

  public void remove(HashKey key, 
                     HashKey cacheKey,
                     MnodeUpdate mnodeUpdate);

  public InputStream getAndPut(DistCacheEntry entry, 
                               MnodeUpdate mnodeValue,
                               long valueDataId,
                               long valueDataTime);

  public boolean compareAndPut(DistCacheEntry entry, 
                               long testValue,
                               MnodeUpdate update,
                               long valueDataId,
                               long valueDataTime);

  public void notifyLease(HashKey key, HashKey cacheKey, int leaseOwner);
}
