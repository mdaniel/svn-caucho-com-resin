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

package com.caucho.memcached;

import com.caucho.server.distcache.AbstractCacheEngine;
import com.caucho.server.distcache.CacheStoreManager;
import com.caucho.server.distcache.DistCacheEntry;
import com.caucho.server.distcache.MnodeUpdate;
import com.caucho.server.distcache.MnodeValue;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.HashKey;

/**
 * Custom serialization for the cache
 */
public class MemcachedCacheEngine extends AbstractCacheEngine
{
  private CacheStoreManager _cacheService;
  private MemcachedClient _client;
  
  MemcachedCacheEngine(CacheStoreManager cacheService,
                      MemcachedClient client)
  {
    _cacheService = cacheService;
    _client = client;
  }
  

  /*
  @Override
  public boolean isLocalExpired(CacheConfig config,
                                  HashKey key,
                                  MnodeEntry mnodeEntry,
                                  long now)
  {
    return mnodeEntry.isLocalExpired(now, config);
  }
  */

  @Override
  public MnodeValue get(DistCacheEntry entry)
  {
    CharBuffer cb = new CharBuffer();
    
    Base64.encode(cb, entry.getKeyHash().getHash());
    
    String key = cb.toString();
    
    MnodeUpdate update = _client.getResinIfModified(key, 
                                                    entry.getMnodeEntry().getValueHash(),
                                                    entry);
    
    if (update != null)
      return entry.putLocalValue(update, null);
    else
      return entry.getMnodeEntry();
  }

  @Override
  public void put(HashKey hashKey, 
                  HashKey cacheKey,
                  MnodeUpdate mnodeUpdate,
                  long valueDataId)
  {
    CharBuffer cb = new CharBuffer();
    
    Base64.encode(cb, hashKey.getHash());
    
    String key = cb.toString();
    
    _client.putResin(key, mnodeUpdate, valueDataId);
  }
  
  @Override
  public void remove(HashKey hashKey,
                     HashKey cacheKey,
                     MnodeUpdate mnodeUpdate)
  {
    CharBuffer cb = new CharBuffer();
    
    Base64.encode(cb, hashKey.getHash());
    
    String key = cb.toString();
    
    _client.removeImpl(key);
  }
}
