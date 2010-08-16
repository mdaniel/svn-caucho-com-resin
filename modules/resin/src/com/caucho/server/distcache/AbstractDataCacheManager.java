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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.cache.CacheLoader;

import com.caucho.config.ConfigException;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.CacheBacking;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.service.RootDirectoryService;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.Sha256OutputStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Manages the distributed cache
 */
abstract public class AbstractDataCacheManager<E extends DistCacheEntry>
  extends AbstractCacheManager<E>
{
  private MnodeStore _mnodeStore;
  private DataStore _dataStore;
  
  private DataCacheBacking _dataCacheBacking;
  
  public AbstractDataCacheManager(ResinSystem resinSystem)
  {
    super(resinSystem);
    
    _dataCacheBacking = new DataCacheBacking();
    
    setCacheBacking(_dataCacheBacking);
  }

  protected MnodeStore getMnodeStore()
  {
    return _mnodeStore;
  }

  abstract protected E createCacheEntry(Object key, HashKey hashKey);

  @Override
  protected void requestClusterData(HashKey valueKey, int flags)
  {
    // _cacheService.requestData(valueKey, flags);
  }

  @Override
  public void start()
  {
    try {
      Path dataDirectory = RootDirectoryService.getCurrentDataDirectory();
    
      String serverId = ResinSystem.getCurrentId();
    
      if (serverId.isEmpty())
        serverId = "default";

      _mnodeStore = new MnodeStore(dataDirectory, serverId);
      _dataStore = new DataStore(serverId, _mnodeStore);
      
      _dataCacheBacking.setDataStore(_dataStore);
      _dataCacheBacking.setMnodeStore(_mnodeStore);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  
    new AdminPersistentStore(this);
  }

  /**
   * Closes the manager.
   */
  @Override
  public void close()
  {
    super.close();
    
    _mnodeStore.close();
  }
}
