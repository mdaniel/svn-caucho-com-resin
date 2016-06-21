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

package com.caucho.env.distcache;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.caucho.server.distcache.CacheData;
import com.caucho.server.distcache.DataStore;
import com.caucho.server.distcache.DataStore.DataItem;
import com.caucho.server.distcache.MnodeEntry;
import com.caucho.server.distcache.MnodeStore;
import com.caucho.server.distcache.MnodeUpdate;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * The local cache repository.
 */
public interface CacheDataBacking {
  /**
   * Returns the underlying DataStore, used for serialization.
   */
  public DataStore getDataStore();
  
  public MnodeStore getMnodeStore();
  
  public void start();
  
  public MnodeEntry loadLocalEntryValue(HashKey key);
  
  public MnodeEntry insertLocalValue(HashKey key,
                                     HashKey cacheKey,
                                     MnodeEntry mnodeValue,
                                     MnodeEntry oldMnodeValue);
  
  public MnodeEntry saveLocalUpdateTime(HashKey keyHash,
                                        MnodeEntry mnodeValue,
                                        MnodeEntry oldMnodeValue);

  /**
   * Sets a cache entry
   */
  public boolean putLocalValue(MnodeEntry mnodeValue,
                               HashKey key,
                               HashKey cacheKey,
                               MnodeEntry oldEntryValue,
                               MnodeUpdate mnodeUpdate);
  
  public boolean loadData(long valueDataId, long valueDataTime, WriteStream os)
    throws IOException;

  public java.sql.Blob loadBlob(long valueDataId, long valueDataTime);

  public DataItem saveData(InputStream mIn, int length)
    throws IOException;

  public DataItem saveData(StreamSource source, int length);
  
  public boolean removeData(long valueDataId, long valueDataTime);
  
  public boolean isDataAvailable(long valueDataId, long valueDataTime);

  /**
   * Returns the last update time on server startup.
   */
  public long getStartupLastUpdateTime();

  /**
   * Returns the last update time on server startup.
   */
  public long getStartupLastUpdateTime(HashKey cacheKey);

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getUpdates(long accessTime, int offset);

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getUpdates(HashKey cacheKey,
                                         long accessTime, 
                                         int offset);

  /**
   * Close the backing.
   */
  public void close();


}
