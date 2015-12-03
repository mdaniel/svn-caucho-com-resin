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

import com.caucho.inject.Module;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;

/**
 * Manages the distributed cache
 */
@Module
public final class LocalStoreManager
{
  private static final Logger log
    = Logger.getLogger(LocalStoreManager.class.getName());
  
  private CacheDataBackingImpl _dataBacking;
  
  private final CacheStoreManager _cacheManager;
  private final LocalMnodeManager _localMnodeManager;
  private final LocalDataManager _localDataManager;
  
  LocalStoreManager(CacheStoreManager cacheStore)
  {
    _cacheManager = cacheStore;
    _localMnodeManager = cacheStore.getLocalMnodeManager();
    _localDataManager = cacheStore.getLocalDataManager();
  }

  public StreamSource loadDataSource(byte[] keyHash,
                                     byte []cacheHash)
  {
    HashKey key = HashKey.create(keyHash);
    HashKey cacheKey = HashKey.create(cacheHash);
    
    CacheConfig config = null;
    
    DistCacheEntry entry = _cacheManager.loadLocalEntry(key, cacheKey);

    return entry.getValueStream();
    /*
    MnodeEntry mnodeEntry = entryKey.getMnodeEntry();
    
    DataStreamSource dataSource
      = _localDataManager.createDataSource(mnodeEntry.getValueDataId());

    if (dataSource != null) {
      return new StreamSource(dataSource);
    }
    else {
      return null;
    }
    */
  }
}
