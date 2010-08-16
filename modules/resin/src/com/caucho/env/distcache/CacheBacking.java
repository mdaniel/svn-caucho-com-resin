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

package com.caucho.env.distcache;

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.CacheData;
import com.caucho.server.distcache.DataStore;
import com.caucho.server.distcache.MnodeValue;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * The local cache repository.
 */
public interface CacheBacking {
  /**
   * Returns the underlying DataStore, used for serialization.
   */
  DataStore getDataStore();
  
  public MnodeValue loadLocalEntryValue(HashKey key);
  
  public MnodeValue insertLocalValue(HashKey key, 
                                     MnodeValue mnodeValue,
                                     MnodeValue oldEntryValue,
                                     long timeout);
  
  public <E> MnodeValue loadClusterValue(E entry, CacheConfig config);
  
  public MnodeValue saveLocalUpdateTime(HashKey keyHash,
                                        MnodeValue mnodeValue,
                                        MnodeValue oldMnodeValue);

  /**
   * Sets a cache entry
   */
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
                                  int leaseOwner);
  
  public boolean loadData(HashKey valueHash, WriteStream os)
    throws IOException;

  public boolean saveData(HashKey valueHash, StreamSource source, int length)
    throws IOException;

  public boolean isDataAvailable(HashKey valueKey);

  /**
   * Returns the last update time on server startup.
   */
  public long getStartupLastUpdateTime();

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getUpdates(long accessTime, int offset);

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getGlobalUpdates(long accessTime, int offset);

  public void putCluster(HashKey key, HashKey value, HashKey cacheKey,
                         MnodeValue mnodeValue);

  public void removeCluster(HashKey key,
                            HashKey cacheKey,
                            MnodeValue mnodeValue);

}
