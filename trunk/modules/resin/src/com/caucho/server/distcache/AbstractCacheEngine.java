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

import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
public class AbstractCacheEngine implements CacheEngine
{
  @Override
  public void start()
  {
  }
  
  @Override
  public void initCache(CacheImpl cache)
  {
  }

  @Override
  public boolean isLocalExpired(CacheConfig config,
                                  HashKey key,
                                  MnodeEntry mnodeEntry,
                                  long now)
  {
    return mnodeEntry.isExpired(now);
  }

  @Override
  public boolean loadData(HashKey valueKey, int flags)
  {
    return true;
  }

  @Override
  public MnodeEntry get(DistCacheEntry entry, CacheConfig config)
  {
    return null;
  }

  @Override
  public void put(HashKey key, MnodeUpdate mnodeUpdate,
                         MnodeEntry mnodeValue)
  {
  }

  @Override
  public void remove(HashKey key, MnodeUpdate mnodeUpdate,
                            MnodeEntry mnodeEntry)
  {
  }

  @Override
  public void updateTime(HashKey key, MnodeEntry mnodeValue)
  {
  }
  @Override
  public HashKey compareAndPut(DistCacheEntry entry, HashKey testValue,
                               MnodeUpdate mnodeUpdate, Object value,
                               CacheConfig config)
  {
    return null;
  }

  @Override
  public HashKey getAndPut(DistCacheEntry entry, MnodeUpdate mnodeUpdate,
                           Object value, long leaseTimeout, int leaseOwner)
  {
    return null;
  }

}
