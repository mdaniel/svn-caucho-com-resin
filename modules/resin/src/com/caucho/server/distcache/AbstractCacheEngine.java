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
public class AbstractCacheEngine implements CacheEngine
{
  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#calculateValueHash(java.lang.Object, com.caucho.server.distcache.CacheConfig)
   */
  @Override
  public byte[] calculateValueHash(Object value, CacheConfig config)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#close()
   */
  @Override
  public void close()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#closeCache(java.lang.String)
   */
  @Override
  public void closeCache(String name)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#compareAndPut(com.caucho.server.distcache.DistCacheEntry, com.caucho.util.HashKey, com.caucho.server.distcache.MnodeUpdate, java.lang.Object, com.caucho.server.distcache.CacheConfig)
   */
  @Override
  public HashKey compareAndPut(DistCacheEntry entry, HashKey testValue,
                               MnodeUpdate mnodeUpdate, Object value,
                               CacheConfig config)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#createDataBacking()
   */
  @Override
  public CacheDataBacking createDataBacking()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#createSelfHashKey(java.lang.Object, com.caucho.distcache.CacheSerializer)
   */
  @Override
  public HashKey createSelfHashKey(Object key, CacheSerializer keySerializer)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#destroyCache(com.caucho.server.distcache.CacheImpl)
   */
  @Override
  public void destroyCache(CacheImpl cache)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#getAndPut(com.caucho.server.distcache.DistCacheEntry, com.caucho.server.distcache.MnodeUpdate, java.lang.Object, long, int)
   */
  @Override
  public HashKey getAndPut(DistCacheEntry entry, MnodeUpdate mnodeUpdate,
                           Object value, long leaseTimeout, int leaseOwner)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#getCacheEntry(com.caucho.util.HashKey, com.caucho.server.distcache.CacheConfig)
   */
  @Override
  public DistCacheEntry getCacheEntry(HashKey key, CacheConfig config)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#getDataBacking()
   */
  @Override
  public CacheDataBacking getDataBacking()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#initCache(com.caucho.server.distcache.CacheImpl)
   */
  @Override
  public void initCache(CacheImpl cache)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isLocalReadValid(CacheConfig config, MnodeEntry mnodeEntry,
                                  long now)
  {
    return ! mnodeEntry.isEntryExpired(now);
  }

  @Override
  public boolean loadClusterData(HashKey valueKey, int flags)
  {
    return true;
  }

  @Override
  public MnodeEntry loadClusterValue(DistCacheEntry entry, CacheConfig config)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#put(com.caucho.util.HashKey, java.lang.Object, com.caucho.server.distcache.CacheConfig)
   */
  @Override
  public void put(HashKey hashKey, Object value, CacheConfig config)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#put(com.caucho.util.HashKey, java.io.InputStream, com.caucho.server.distcache.CacheConfig, long)
   */
  @Override
  public ExtCacheEntry put(HashKey hashKey, InputStream is, CacheConfig config,
                           long idleTimeout) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#putCluster(com.caucho.util.HashKey, com.caucho.server.distcache.MnodeUpdate, com.caucho.server.distcache.MnodeEntry)
   */
  @Override
  public void putCluster(HashKey key, MnodeUpdate mnodeUpdate,
                         MnodeEntry mnodeValue)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#remove(com.caucho.util.HashKey)
   */
  @Override
  public boolean remove(HashKey hashKey)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#removeCluster(com.caucho.util.HashKey, com.caucho.server.distcache.MnodeUpdate, com.caucho.server.distcache.MnodeEntry)
   */
  @Override
  public void removeCluster(HashKey key, MnodeUpdate mnodeUpdate,
                            MnodeEntry mnodeEntry)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#start()
   */
  @Override
  public void start()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.server.distcache.CacheEngine#updateCacheTime(com.caucho.util.HashKey, com.caucho.server.distcache.MnodeEntry)
   */
  @Override
  public void updateCacheTime(HashKey key, MnodeEntry mnodeValue)
  {
    // TODO Auto-generated method stub
    
  }
}
