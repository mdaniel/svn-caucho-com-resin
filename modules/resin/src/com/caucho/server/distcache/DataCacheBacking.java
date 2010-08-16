/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.env.distcache.CacheBacking;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * Manages the distributed cache
 */
public class DataCacheBacking implements CacheBacking {
  private static final Logger log
    = Logger.getLogger(DataCacheBacking.class.getName());
  
  private DataStore _dataStore;
  private MnodeStore _mnodeStore;
  
  public DataCacheBacking()
  {
  }
  
  public void setDataStore(DataStore dataStore)
  {
    _dataStore = dataStore;
  }
  
  public void setMnodeStore(MnodeStore mnodeStore)
  {
    _mnodeStore = mnodeStore;
  }
  
  @Override
  public DataStore getDataStore()
  {
    return _dataStore;
  }

  /**
   * Returns the local value from the database
   */
  @Override
  public MnodeValue loadLocalEntryValue(HashKey key)
  {
    return _mnodeStore.load(key);
  }

  /**
   * Sets a cache entry
   */
  @Override
  public MnodeValue insertLocalValue(HashKey key, 
                                     MnodeValue mnodeValue,
                                     MnodeValue oldEntryValue,
                                     long timeout)
  {
    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeValue.NULL) {
      if (_mnodeStore.insert(key, mnodeValue.getValueHashKey(),
                             mnodeValue.getCacheHashKey(),
                             mnodeValue.getFlags(),
                             mnodeValue.getVersion(),
                             mnodeValue.getExpireTimeout(),
                             mnodeValue.getIdleTimeout(),
                             mnodeValue.getLeaseTimeout(),
                             mnodeValue.getLocalReadTimeout())) {
        return mnodeValue;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ")");

        return oldEntryValue;
      }
    } else {
      if (_mnodeStore.updateSave(key, mnodeValue.getValueHashKey(),
                                      mnodeValue.getVersion(), timeout)) {
        return mnodeValue;
      }
      else if (_mnodeStore.insert(key, mnodeValue.getValueHashKey(),
                                  mnodeValue.getCacheHashKey(),
                                  mnodeValue.getFlags(),
                                  mnodeValue.getVersion(),
                                  mnodeValue.getExpireTimeout(),
                                  mnodeValue.getIdleTimeout(),
                                  mnodeValue.getLeaseTimeout(),
                                  mnodeValue.getLocalReadTimeout())) {
        return mnodeValue;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ")");

        return oldEntryValue;
      }
    }
  }

  public MnodeValue putLocalValue(MnodeValue mnodeValue,
                                  HashKey key,
                                  MnodeValue oldEntryValue,
                                  long version,
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
    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeValue.NULL) {
      if (_mnodeStore.insert(key, valueHash, cacheHash,
                             flags, version,
                             expireTimeout,
                             idleTimeout,
                             leaseTimeout,
                             localReadTimeout)) {
        return mnodeValue;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ", version=" + version + ")");
      }
    } else {
      if (_mnodeStore.updateSave(key, valueHash, version, idleTimeout)) {
        return mnodeValue;
      }
      else if (_mnodeStore.insert(key, valueHash, cacheHash,
                                  flags, version,
                                  expireTimeout,
                                  idleTimeout,
                                  leaseTimeout,
                                  localReadTimeout)) {
        return mnodeValue;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ", version=" + version + ")");
      }
    }

    return null;
  }
  
  @Override
  public  MnodeValue saveLocalUpdateTime(HashKey keyHash,
                                         MnodeValue mnodeValue,
                                         MnodeValue oldMnodeValue)
  {
    if (_mnodeStore.updateUpdateTime(keyHash,
                                     mnodeValue.getVersion(),
                                     mnodeValue.getIdleTimeout(),
                                     mnodeValue.getLastUpdateTime())) {
      return mnodeValue;
    } else {
      log.fine(this + " db updateTime failed due to timing conflict"
               + "(key=" + keyHash + ", version=" + mnodeValue.getVersion() + ")");

      return oldMnodeValue;
    }
  }

  @Override
  public boolean loadData(HashKey valueHash, WriteStream os)
    throws IOException
  {
    return _dataStore.load(valueHash, os);
  }
  
  @Override
  public boolean saveData(HashKey valueHash, StreamSource source, int length)
    throws IOException
  {
    return _dataStore.save(valueHash, source, length);
  }

  @Override
  public boolean isDataAvailable(HashKey valueKey)
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      return false;

    return _dataStore.isDataAvailable(valueKey);
  }

  /**
   * Returns the last update time on server startup.
   */
  @Override
  public long getStartupLastUpdateTime()
  {
    return _mnodeStore.getStartupLastUpdateTime();
  }

  /**
   * Returns a set of entries since an access time.
   */
  @Override
  public ArrayList<CacheData> getUpdates(long accessTime, int offset)
  {
    return _mnodeStore.getUpdates(accessTime, offset);
  }

  /**
   * Returns a set of entries since an access time.
   */
  @Override
  public ArrayList<CacheData> getGlobalUpdates(long accessTime, int offset)
  {
    return _mnodeStore.getGlobalUpdates(accessTime, offset);
  }

  @Override
  public <E> MnodeValue loadClusterValue(E entry, CacheConfig config)
  {
    return null;
    // return _cacheService.get(entry, config);
  }

  @Override
  public void putCluster(HashKey key, HashKey value, HashKey cacheKey,
                         MnodeValue mnodeValue)
  {
  }

  @Override
  public void removeCluster(HashKey key,
                            HashKey cacheKey,
                            MnodeValue mnodeValue)
  {
  }
}
