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

package com.caucho.distcache.jdbc;

import java.util.logging.Logger;

import javax.sql.DataSource;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.config.ConfigException;
import com.caucho.env.service.ResinSystem;
import com.caucho.server.distcache.AbstractCacheManager;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.MnodeValue;
import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
public final class JdbcCacheManager extends AbstractCacheManager<JdbcCacheEntry>
{
  private static final Logger log
    = Logger.getLogger(JdbcCacheManager.class.getName());
  
  private DataSource _dataSource;
  private JdbcMnodeStore _mnodeStore;
  private JdbcDataStore _dataStore;
  
  public JdbcCacheManager(ResinSystem resinSystem, DataSource dataSource)
  {
    super(resinSystem);
    
    _dataSource = dataSource;
  }

  @Override
  public void start()
  {
    super.start();

    try {
      String mnodeTable = "resin_mnode";
      String dataTable = "resin_data";
      
      String serverName = ResinSystem.getCurrent().getId();
      _mnodeStore = new JdbcMnodeStore(_dataSource, mnodeTable, serverName);
      _dataStore = new JdbcDataStore(_mnodeStore, dataTable, serverName);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    /*
    
    _dataManager = new CacheDataManager(selfServer, getDataBacking());
    _mnodeManager = new CacheMnodeManager(selfServer, this, _dataManager);
    
    CacheClusterBackingImpl cacheBacking
      = new CacheClusterBackingImpl(_mnodeManager);
    
    setClusterBacking(cacheBacking);
    */
    
    JdbcClusterBacking jdbcBacking = new JdbcClusterBacking(this);
    
    setClusterBacking(jdbcBacking);
  }
  
  protected JdbcMnodeStore getJdbcMnodeStore()
  {
    return _mnodeStore;
  }
  
  protected JdbcDataStore getJdbcDataStore()
  {
    return _dataStore;
  }

  @Override
  protected JdbcCacheEntry createCacheEntry(Object key, HashKey hashKey)
  {
    TriadOwner owner = TriadOwner.A_B;
    
    return new JdbcCacheEntry(key, hashKey, owner, this);
  }

  public MnodeValue put(HashKey key, 
                        HashKey valueHash,
                        HashKey cacheHash,
                        MnodeValue mnodeValue)
  {
    int flags = mnodeValue.getFlags();
    long version = mnodeValue.getVersion();
    long expireTimeout = mnodeValue.getExpireTimeout();
    long idleTimeout = mnodeValue.getIdleTimeout();
    long leaseTimeout = mnodeValue.getLeaseTimeout();
    long localReadTimeout = mnodeValue.getLocalReadTimeout();
    
    saveData(valueHash);
    
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

    return null;
  }
  
  private void saveData(HashKey valueHash)
  {
    if (valueHash == null)
      return;
    
    _dataStore.save(valueHash, getDataBacking());
  }

  public MnodeValue get(JdbcCacheEntry entry, CacheConfig config)
  {
    MnodeValue value = _mnodeStore.load(entry.getKeyHash());
    
    entry.compareAndPut(value.getVersion(), value.getValueHashKey(), config);
    
    return value;
  }

  /**
   * Load the cluster data from the triad.
   */
  @Override
  protected boolean loadClusterData(HashKey valueKey, int flags)
  {
    return _dataStore.load(valueKey, getDataBacking());
  }
}
