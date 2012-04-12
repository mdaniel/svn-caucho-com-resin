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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.service.ResinSystem;
import com.caucho.inject.Module;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeList;
import com.caucho.util.HashKey;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.StreamSource;

/**
 * Manages the distributed cache
 */
@Module
public final class CacheStoreManager
{
  private static final Logger log
    = Logger.getLogger(CacheStoreManager.class.getName());

  private static final L10N L = new L10N(CacheStoreManager.class);
  
  private final ResinSystem _resinSystem;
  private static final Object NULL_OBJECT = new Object();
  
  private FreeList<KeyHashStream> _keyStreamFreeList
    = new FreeList<KeyHashStream>(32);
  
  private LruCache<CacheKey,HashKey> _keyCache;
  
  private CacheDataBackingImpl _dataBacking;
  
  private final LocalMnodeManager _localMnodeManager;
  private final LocalDataManager _localDataManager;
  private final LocalStoreManager _localStoreManager;
  
  private ConcurrentHashMap<HashKey,CacheMnodeListener> _cacheListenMap
    = new ConcurrentHashMap<HashKey,CacheMnodeListener>();
  
  private boolean _isCacheListen;
  
  private final LruCache<HashKey, DistCacheEntry> _entryCache
    = new LruCache<HashKey, DistCacheEntry>(64 * 1024);
  
  private boolean _isClosed;
  
  private CacheEngine _cacheEngine = new AbstractCacheEngine();
  
  private AdminCacheStore _admin = new AdminCacheStore(this);
  
  public CacheStoreManager(ResinSystem resinSystem)
  {
    _resinSystem = resinSystem;
    // new AdminPersistentStore(this);
    
    _localMnodeManager = new LocalMnodeManager(this);
    _localDataManager = new LocalDataManager(this);
    
    _localStoreManager = new LocalStoreManager(this);
  }

  public void setCacheEngine(CacheEngine cacheEngine)
  {
    if (cacheEngine == null)
      throw new NullPointerException();
    
    _cacheEngine = cacheEngine;
  }
  
  public CacheEngine getCacheEngine()
  {
    return _cacheEngine;
  }
  
  public CacheDataBacking getDataBacking()
  {
    return _dataBacking;
  }
  
  public LocalMnodeManager getLocalMnodeManager()
  {
    return _localMnodeManager;
  }
  
  public LocalDataManager getLocalDataManager()
  {
    return _localDataManager;
  }
  
  public LocalStoreManager getLocalStoreManager()
  {
    return _localStoreManager;
  }
  
  public void addCacheListener(HashKey cacheKey, CacheMnodeListener listener)
  {
    _cacheListenMap.put(cacheKey, listener);
    _isCacheListen = true;
  }

  /**
   * Returns the key entry.
   */
  public final DistCacheEntry getCacheEntry(Object key, CacheConfig config)
  {
    HashKey hashKey = createHashKey(key, config);

    DistCacheEntry cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(key, hashKey);

      cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns the key entry.
   */
  public final DistCacheEntry getCacheEntry(HashKey hashKey, 
                                            CacheConfig config)
  {
    DistCacheEntry cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(null, hashKey);

      cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns the key entry.
   */
  public DistCacheEntry createCacheEntry(Object key, HashKey hashKey)
  {
    TriadOwner owner = TriadOwner.getHashOwner(hashKey.getHash());

    return new DistCacheEntry(this, key, hashKey, owner);
  }

  /**
   * Returns the key entry.
   */
  final public DistCacheEntry getCacheEntry(HashKey hashKey)
  {
    DistCacheEntry cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(null, hashKey);

      if (! _entryCache.compareAndPut(null,
                                      cacheEntry.getKeyHash(),
                                      cacheEntry)) {
        cacheEntry = _entryCache.get(hashKey);
      }
    }

    return cacheEntry;
  }
  
  final public boolean load(DistCacheEntry entry,
                            CacheConfig config,
                            long now)
  {
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);
    
    return mnodeValue != null;
  }

  final public Object get(DistCacheEntry entry,
                          CacheConfig config,
                          long now)
  {
    return get(entry, config, now, false);
  }

  final public Object getExact(DistCacheEntry entry,
                               CacheConfig config,
                               long now)
  {
    return get(entry, config, now, true);
  }

  final public Object get(DistCacheEntry entry,
                          CacheConfig config,
                          long now,
                          boolean isExact)
  {
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now, isExact);

    if (mnodeValue == null) {
      return null;
    }

    Object value = mnodeValue.getValue();

    if (value != null) {
      return value;
    }

    long valueHash = mnodeValue.getValueHash();

    if (valueHash == 0) {
      return null;
    }

    updateAccessTime(entry, mnodeValue, now);

    value = _localDataManager.readData(entry.getKeyHash(),
                                       valueHash,
                                       mnodeValue.getValueDataId(),
                                       config.getValueSerializer(),
                                       config);
    
    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data in get for " 
                  + mnodeValue + " " + entry);
      remove(entry, config);
    }

    mnodeValue.setObjectValue(value);

    return value;
  }

  /**
   * Gets a cache entry as a stream
   */
  final public boolean getStream(DistCacheEntry entry,
                                 OutputStream os,
                                 CacheConfig config)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();

    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    if (mnodeValue == null)
      return false;

    updateAccessTime(entry, mnodeValue, now);

    long valueHash = mnodeValue.getValueHash();

    if (valueHash == 0) {
      return false;
    }

    boolean isData = _localDataManager.readData(entry.getKeyHash(), mnodeValue, os, config);
    
    if (! isData) {
      log.warning("Missing or corrupted data for getStream " + mnodeValue
                  + " " + entry);
      // Recovery from dropped or corrupted data
      remove(entry, config);
    }

    return isData;
  }

  /**
   * Gets a cache entry as a stream
   */
  final public StreamSource getValueStream(DistCacheEntry entry)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();

    MnodeEntry mnodeValue = entry.getMnodeEntry();

    if (mnodeValue == null)
      return null;

    // updateAccessTime(entry, mnodeValue, now);

    return _localDataManager.createDataSource(mnodeValue.getValueDataId());
  }

  final public MnodeEntry getMnodeValue(DistCacheEntry entry,
                                        CacheConfig config,
                                        long now)
  {
    return getMnodeValue(entry, config, now, false);
  }

  final public MnodeEntry getMnodeValue(DistCacheEntry entry,
                                        CacheConfig config,
                                        long now,
                                        boolean isExact)
  {
    MnodeEntry mnodeValue = _localMnodeManager.loadMnodeValue(entry);

    if (mnodeValue == null) {
      reloadValue(entry, config, now);
    }
    else if (isExact 
             || isLocalExpired(config, entry.getKeyHash(), mnodeValue, now)) {
      reloadValue(entry, config, now);
    }

    mnodeValue = entry.getMnodeEntry();

    // server/016q
    if (mnodeValue != null) {
      updateAccessTime(entry, mnodeValue);
    }

    mnodeValue = entry.getMnodeEntry();
    

    return mnodeValue;
  }

  private void reloadValue(DistCacheEntry entry,
                           CacheConfig config,
                           long now)
  {
    // only one thread may update the expired data
    if (entry.startReadUpdate()) {
      try {
        loadExpiredValue(entry, config, now);
      } finally {
        entry.finishReadUpdate();
      }
    }
  }
  
  // XXX: needs to be moved
  protected void lazyValueUpdate(DistCacheEntry entry, CacheConfig config)
  {
    reloadValue(entry, config, CurrentTime.getCurrentTime());
  }

  protected boolean isLocalExpired(CacheConfig config,
                                  HashKey key,
                                  MnodeEntry mnodeValue,
                                  long now)
  {
    return config.getEngine().isLocalExpired(config, key, mnodeValue, now);
  }

  private void updateAccessTime(DistCacheEntry entry,
                                MnodeEntry mnodeValue,
                                long now)
  {
    if (mnodeValue != null) {
      long idleTimeout = mnodeValue.getAccessedExpireTimeout();
      long updateTime = mnodeValue.getLastModifiedTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
          && updateTime + mnodeValue.getAccessExpireTimeoutWindow() < now) {
        // XXX:
        mnodeValue.setLastAccessTime(now);

        saveUpdateTime(entry, mnodeValue);
      }
    }
  }

  private void loadExpiredValue(DistCacheEntry entry,
                                CacheConfig config,
                                long now)
  {
    MnodeEntry mnodeEntry = entry.getMnodeEntry();
    
    entry.addLoadCount();

    if (mnodeEntry == null || mnodeEntry.isExpired(now)) {
      CacheLoader loader = config.getCacheLoader();

      if (loader != null && config.isReadThrough() && entry.getKey() != null) {
        Object arg = null;
        
        Cache.Entry loaderEntry = loader.load(entry.getKey());

        if (entry != null) {
          put(entry, loaderEntry.getValue(), config, now, mnodeEntry);

          return;
        }
      }

      MnodeEntry nullMnodeValue = new MnodeEntry(0, 0, 0, 0, null, null,
                                                 0,
                                                 config.getAccessedExpireTimeout(),
                                                 config.getModifiedExpireTimeout(),
                                                 config.getLeaseExpireTimeout(),
                                                 now, now,
                                                 true, true);

      entry.compareAndSet(mnodeEntry, nullMnodeValue);
    }
    else {
      mnodeEntry.setLastAccessTime(now);
    }
  }
  
 /**
   * Sets a cache entry
   */
  final public void put(DistCacheEntry entry,
                        Object value,
                        CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    put(entry, value, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final void put(DistCacheEntry entry,
                           Object value,
                           CacheConfig config,
                           long now,
                           MnodeEntry mnodeValue)
  {
    // long idleTimeout = config.getIdleTimeout() * 5L / 4;
    HashKey key = entry.getKeyHash();

    MnodeUpdate update = _localDataManager.writeValue(mnodeValue, value, config);
    
    mnodeValue = putLocalValue(entry, update, value);

    if (mnodeValue == null)
      return;

    config.getEngine().put(key, update, mnodeValue);
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      writer.write(entry);
    }

    return;
  }

  public final ExtCacheEntry putStream(DistCacheEntry entry,
                                       InputStream is,
                                       CacheConfig config,
                                       long accessedExpireTime,
                                       long modifiedExpireTime,
                                       int userFlags)
    throws IOException
  {
    HashKey key = entry.getKeyHash();
    MnodeEntry mnodeEntry = _localMnodeManager.loadMnodeValue(entry);

    DataItem valueItem = _localDataManager.writeData(is);
    
    long valueHash = valueItem.getValueHash();
    long valueDataId = valueItem.getValueDataId();
    
    long valueLength = valueItem.getLength();
    long newVersion = getNewVersion(mnodeEntry);
    
    long flags = config.getFlags() | ((long) userFlags) << 32;
    
    if (accessedExpireTime < 0)
      accessedExpireTime = config.getAccessedExpireTimeout();
    
    if (modifiedExpireTime < 0)
      modifiedExpireTime = config.getModifiedExpireTimeout();

    
    int leaseOwner = (mnodeEntry != null) ? mnodeEntry.getLeaseOwner() : -1;
    long leaseTimeout = config.getLeaseExpireTimeout();
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(valueHash,
                                              valueDataId,
                                              valueLength,
                                              newVersion,
                                              HashKey.getHash(config.getCacheKey()),
                                              flags,
                                              accessedExpireTime,
                                              modifiedExpireTime,
                                              leaseOwner,
                                              leaseTimeout);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;
    
    MnodeValue mnodeValue = putLocalValue(entry, mnodeUpdate, null);

    if (mnodeEntry == null)
      return null;
    
    config.getEngine().put(key, mnodeUpdate, mnodeValue);

    return mnodeEntry;
  }

  public final boolean putIfNew(DistCacheEntry entry,
                                MnodeUpdate update,
                                InputStream is)
    throws IOException
  {
    MnodeValue newValue = putLocalValue(entry, update, is);

    return newValue.getValueHash() == update.getValueHash();
  }

  /**
   * Sets a cache entry
   */
  final public Object getAndPut(DistCacheEntry entry,
                                Object value,
                                CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    return getAndPut(entry, value, config, now, mnodeValue);
  }
  
  /**
   * Sets a cache entry
   */
  final public Object getAndRemove(DistCacheEntry entry,
                                   CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    return getAndPut(entry, null, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final Object getAndPut(DistCacheEntry entry,
                                   Object value,
                                   CacheConfig config,
                                   long now,
                                   MnodeEntry mnodeValue)
  {
    MnodeUpdate mnodeUpdate
      = _localDataManager.writeValue(mnodeValue, value, config);
    
    Object oldValue = mnodeValue != null ? mnodeValue.getValue() : null;

    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;

    long oldHash = getAndPut(entry, 
                             mnodeUpdate, value,
                             config.getLeaseExpireTimeout(),
                             leaseOwner,
                             config);

    if (oldHash == 0)
      return null;
    
    if (oldHash == mnodeUpdate.getValueHash() && oldValue != null)
      return oldValue;
    
    oldValue = _localDataManager.readData(entry.getKeyHash(),
                                          oldHash,
                                          mnodeValue.getValueDataId(),
                                          config.getValueSerializer(),
                                          config);

    return oldValue;
  }

  /**
   * Sets a cache entry
   */
  protected long getAndPut(DistCacheEntry entry,
                           MnodeUpdate mnodeUpdate,
                           Object value,
                           long leaseTimeout,
                           int leaseOwner,
                           CacheConfig config)
  {
    return config.getEngine().getAndPut(entry, mnodeUpdate, value);
  }
  
  public long getAndPutLocal(DistCacheEntry entry,
                             MnodeUpdate mnodeUpdate,
                             Object value)
  {
    long oldValueHash = entry.getValueHash();

    MnodeEntry mnodeValue = putLocalValue(entry, 
                                          mnodeUpdate, value);

    return oldValueHash;
  }

  public Object getAndReplace(DistCacheEntry entry, 
                              long testValue,
                              Object value, 
                              CacheConfig config)
  {
    if (compareAndPut(entry, testValue, value, config)) {
      long result = -1;
      
      return _localDataManager.readData(entry.getKeyHash(),
                                        result,
                                        entry.getMnodeEntry().getValueDataId(),
                                        config.getValueSerializer(),
                                        config);
    }
    else {
      return null;
    }
  }
  
  public boolean compareAndPut(DistCacheEntry entry, 
                               long testValue,
                               Object value, 
                               CacheConfig config)
  {
    MnodeUpdate update
      = _localDataManager.writeValue(entry.getMnodeEntry(), value, config);
    
    try {
      return _localMnodeManager.compareAndPut(entry, testValue, update, value);
    } finally {
      MnodeValue newMnodeValue = entry.getMnodeEntry();
      
      if (newMnodeValue == null
          || newMnodeValue.getValueDataId() != update.getValueDataId()) {
        _dataBacking.removeData(update.getValueDataId());
      }
    }
  }
  
  public final boolean compareAndPutLocal(DistCacheEntry entry,
                                          long testValue,
                                          MnodeUpdate update,
                                          StreamSource source)
  {
    long version = getNewVersion(entry.getMnodeEntry());
    
    MnodeUpdate newUpdate
      = _localDataManager.writeData(update, version, source);
    
    Object value = null;
    
    try {
      return _localMnodeManager.compareAndPut(entry, testValue, newUpdate, value);
    } finally {
      MnodeValue newMnodeValue = entry.getMnodeEntry();
      
      if (newMnodeValue == null
          || newMnodeValue.getValueDataId() != newUpdate.getValueDataId()) {
        _dataBacking.removeData(newUpdate.getValueDataId());
      }
    }
  }

  protected boolean compareAndPut(DistCacheEntry entry,
                                  long testValue,
                                  MnodeUpdate mnodeUpdate,
                                  Object value,
                                  CacheConfig config)
  {
    CacheEngine engine = config.getEngine();
    
    return engine.compareAndPut(entry, testValue, mnodeUpdate, value);
  }

  /*
  protected boolean compareAndPut(DistCacheEntry entry,
                                  long testValue,
                                  MnodeUpdate mnodeUpdate,
                                  Object value)
  {
    CacheEngine engine = config.getEngine();
    
    return engine.compareAndPut(entry, testValue, mnodeUpdate, value, config);
  }
  */

  /*
  public boolean compareAndPut(DistCacheEntry entry,
                               long version,
                               long valueHash,
                               long valueIndex,
                               long valueLength,
                               CacheConfig config)
  {
    HashKey key = entry.getKeyHash();
    MnodeEntry mnodeValue = _localMnodeManager.loadMnodeValue(entry);

    long oldValueHash = (mnodeValue != null
                         ? mnodeValue.getValueHash()
                         : null);
    long oldVersion = mnodeValue != null ? mnodeValue.getVersion() : 0;

    if (version <= oldVersion)
      return false;

    if (valueHash == oldValueHash) {
      return true;
    }
    
    long newVersion = version;
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(valueHash, valueIndex, valueLength,
                                              newVersion,
                                              config);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;

    mnodeValue = putLocalValue(entry, mnodeUpdate, null);
    
    if (mnodeValue == null)
      return false;
    
    config.getEngine().put(key, mnodeUpdate, mnodeValue);

    return true;
  }
  */

  final DistCacheEntry getLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    DistCacheEntry entry = getCacheEntry(key);

    return entry;
  }

  public final DistCacheEntry loadLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    DistCacheEntry entry = getCacheEntry(key);

    long now = CurrentTime.getCurrentTime();

    if (entry.getMnodeEntry() == null
        || entry.getMnodeEntry().isExpired(now)) {
      _localMnodeManager.forceLoadMnodeValue(entry);
    }

    return entry;
  }

  public final DistCacheEntry getLocalEntryAndUpdateIdle(HashKey key)
  {
    DistCacheEntry entry = getLocalEntry(key);

    MnodeEntry mnodeValue = entry.getMnodeEntry();

    if (mnodeValue != null) {
      updateAccessTime(entry, mnodeValue);
    }

    return entry;
  }

  final protected void updateAccessTime(DistCacheEntry entry,
                                        MnodeEntry mnodeValue)
  {
    long accessedExpireTimeout = mnodeValue.getAccessedExpireTimeout();
    long accessedTime = mnodeValue.getLastAccessedTime();

    long now = CurrentTime.getCurrentTime();
                       
    if (accessedExpireTimeout < CacheConfig.TIME_INFINITY
        && accessedTime + mnodeValue.getAccessExpireTimeoutWindow() < now) {
      mnodeValue.setLastAccessTime(now);

      saveUpdateTime(entry, mnodeValue);
    }
  }

  /**
   * Gets a cache entry
   */
  /*
  final public MnodeEntry loadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeEntry mnodeValue = cacheEntry.getMnodeEntry();

    if (mnodeValue == null || mnodeValue.isImplicitNull()) {
      MnodeEntry newMnodeValue = getDataBacking().loadLocalEntryValue(key);

      // cloud/6811
      cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

      mnodeValue = cacheEntry.getMnodeEntry();
    }

    return mnodeValue;
  }
  */

  /**
   * Gets a cache entry
   */
  /*
  private MnodeEntry forceLoadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeEntry mnodeValue = cacheEntry.getMnodeEntry();

    MnodeEntry newMnodeValue = getDataBacking().loadLocalEntryValue(key);

    cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

    mnodeValue = cacheEntry.getMnodeEntry();

    return mnodeValue;
  }
  */

  /**
   * Sets a cache entry
   */
  final MnodeEntry putLocalValue(HashKey key, MnodeEntry mnodeValue)
  {
    DistCacheEntry entry = getCacheEntry(key);

    long timeout = 60000L;

    MnodeEntry oldEntryValue = entry.getMnodeEntry();

    if (oldEntryValue != null && mnodeValue.compareTo(oldEntryValue) <= 0) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()
    if (! entry.compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue update failed due to timing conflict"
        + " (key=" + key + ")");

      return entry.getMnodeEntry();
    }

    return getDataBacking().insertLocalValue(key, mnodeValue,
                                             oldEntryValue);
  }
  
  /**
   * Sets a cache entry
   */
  final MnodeEntry saveUpdateTime(DistCacheEntry entryKey,
                                  MnodeEntry mnodeValue)
  {
    MnodeEntry newEntryValue = saveLocalUpdateTime(entryKey, mnodeValue);

    if (newEntryValue.getVersion() != mnodeValue.getVersion())
      return newEntryValue;

    getCacheEngine().updateTime(entryKey.getKeyHash(), mnodeValue);

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  public final void saveLocalUpdateTime(HashKey key,
                                        long version,
                                        long accessTimeout,
                                        long updateTime)
  {
    DistCacheEntry entry = _entryCache.get(key);

    if (entry == null)
      return;

    MnodeEntry oldEntryValue = entry.getMnodeEntry();

    if (oldEntryValue == null || version != oldEntryValue.getVersion())
      return;

    MnodeEntry mnodeValue
      = new MnodeEntry(oldEntryValue, accessTimeout, updateTime);

    saveLocalUpdateTime(entry, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry saveLocalUpdateTime(DistCacheEntry entry,
                                       MnodeEntry mnodeValue)
  {
    MnodeEntry oldEntryValue = entry.getMnodeEntry();

    if (oldEntryValue != null
        && mnodeValue.getVersion() < oldEntryValue.getVersion()) {
      return oldEntryValue;
    }
    
    if (oldEntryValue != null
        && mnodeValue.getLastAccessedTime() == oldEntryValue.getLastAccessedTime()
        && mnodeValue.getLastModifiedTime() == oldEntryValue.getLastModifiedTime()) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! entry.compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue updateTime failed due to timing conflict"
               + " (key=" + entry.getKeyHash() + ")");

      return entry.getMnodeEntry();
    }

    return getDataBacking().saveLocalUpdateTime(entry.getKeyHash(),
                                                 mnodeValue,
                                                 oldEntryValue);
  }

  /**
   * Sets a cache entry
   */
  public final boolean remove(DistCacheEntry entry, CacheConfig config)
  {
    HashKey key = entry.getKeyHash();

    MnodeEntry mnodeEntry = _localMnodeManager.loadMnodeValue(entry);
    long oldValueHash = mnodeEntry != null ? mnodeEntry.getValueHash() : 0;

    long newVersion = getNewVersion(mnodeEntry);

    /*
    long leaseTimeout = (mnodeEntry != null
                         ? mnodeEntry.getLeaseTimeout()
                         : config.getLeaseExpireTimeout());
    int leaseOwner = (mnodeEntry != null ? mnodeEntry.getLeaseOwner() : -1);
    */
    
    MnodeUpdate mnodeUpdate;
    
    if (mnodeEntry != null)
      mnodeUpdate = MnodeUpdate.createNull(newVersion, mnodeEntry);
    else
      mnodeUpdate = MnodeUpdate.createNull(newVersion, config);

    MnodeValue mnodeValue = putLocalValue(entry, mnodeUpdate, null);

    if (mnodeEntry == null)
      return oldValueHash != 0;

    config.getEngine().remove(key, mnodeUpdate, mnodeEntry);
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      writer.delete(entry.getKey());
    }

    return oldValueHash != 0;
  }

  /**
   * Sets a cache entry
   */
  public final boolean remove(HashKey key, CacheConfig config)
  {
    DistCacheEntry entry = getCacheEntry(key);
    MnodeValue mnodeValue = entry.getMnodeEntry();

    long oldValueHash = mnodeValue != null ? mnodeValue.getValueHash() : 0;

    long newVersion = getNewVersion(mnodeValue);

    MnodeUpdate mnodeUpdate = MnodeUpdate.createNull(newVersion,
                                                     mnodeValue);
    
    mnodeValue = putLocalValue(entry,
                               mnodeUpdate,
                               null);

    if (mnodeValue == null) {
      return oldValueHash != 0;
    }
    
    config.getEngine().put(key, null, mnodeValue);

    return oldValueHash != 0;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(DistCacheEntry entry,
                                        MnodeUpdate mnodeUpdate,
                                        Object value)
  {
    HashKey key = entry.getKeyHash();
    
    // long valueHash = mnodeUpdate.getValueHash();
    // long version = mnodeUpdate.getVersion();
    
    MnodeEntry mnodeValue = _localMnodeManager.putLocalValue(entry,
                                                             mnodeUpdate,
                                                             value);
    
    notifyPutListeners(key, mnodeUpdate, mnodeValue);
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeValue putLocalValue(DistCacheEntry entry,
                                        MnodeUpdate mnodeUpdate,
                                        InputStream is)
  {
    HashKey key = entry.getKeyHash();
    
    // long valueHash = mnodeUpdate.getValueHash();
    // long version = mnodeUpdate.getVersion();
    
    MnodeValue mnodeValue = localUpdate(entry, mnodeUpdate, is);

    notifyPutListeners(key, mnodeUpdate, mnodeValue);
    
    return mnodeValue;
  }

  /**
   * localPut updates the local copy based on a CachePut message
   */
  public MnodeUpdate localPut(byte []keyHash,
                              MnodeUpdate update,
                              StreamSource source)
  {
    try {
      HashKey key = new HashKey(keyHash);
      long valueHash = update.getValueHash();

      DistCacheEntry cacheEntry = loadLocalEntry(key);
    
      return localUpdate(cacheEntry, update, source.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public MnodeUpdate localUpdate(DistCacheEntry cacheEntry,
                                 MnodeUpdate update,
                                 InputStream is)
  {
    MnodeEntry oldEntryValue = cacheEntry.getMnodeEntry();
    long oldEntryHash = (oldEntryValue != null
                         ? oldEntryValue.getValueHash()
                         : 0);
    
    if (update.getValueHash() == 0) {
    }
    else if (oldEntryValue == null
             || (oldEntryValue.getVersion() <= update.getVersion()
                 && update.getValueHash() != oldEntryHash)) {
      try {
        if (is != null) {
          update = _localDataManager.writeData(update,
                                               update.getVersion(),
                                               is);
        }
      } finally {
        IoUtil.close(is);
      }
    }
    
    _localMnodeManager.putLocalValue(cacheEntry, update, null);
    
    /*
    int server = 0;
    CacheUpdateWithSource result = loadCacheSource(key,
                                                   update.getVersion(),
                                                   server,
                                                   false);
                                                   */
    
    return cacheEntry.getMnodeEntry().getRemoteUpdate();
  }

  private void notifyPutListeners(HashKey key,
                                  MnodeUpdate update,
                                  MnodeValue mnodeValue)
  {
    
    if (mnodeValue != null
        && mnodeValue.getValueHash() == update.getValueHash()
        && mnodeValue.getCacheHash() != null
        && _isCacheListen) {
      HashKey cacheKey = HashKey.create(mnodeValue.getCacheHash());
      
      CacheMnodeListener listener = _cacheListenMap.get(cacheKey);

      if (listener != null)
        listener.onPut(key, mnodeValue);
    }
  }
  
  private long getNewVersion(MnodeValue mnodeValue)
  {
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;
    
    return getNewVersion(version);
  }
  
  private long getNewVersion(long version)
  {
    long newVersion = version + 1;

    long now = CurrentTime.getCurrentTime();
  
    if (newVersion < now)
      return now;
    else
      return newVersion;
  }

  /**
   * Load the cluster data from the triad.
   */
  /*
  protected boolean loadClusterData(HashKey key,
                                    HashKey valueKey,
                                    long valueIndex,
                                    CacheConfig config)
  {
    return config.getEngine().loadData(key, 
                                       valueKey, valueIndex,
                                       config.getFlags());
  }
  */

  /**
   * Clears leases on server start/stop
   */
  final public void clearLeases()
  {
    Iterator<DistCacheEntry> iter = _entryCache.values();

    while (iter.hasNext()) {
      DistCacheEntry entry = iter.next();

      entry.clearLease();
    }
  }

  /**
   * Clears ephemeral data on startup.
   */
  public void clearEphemeralEntries()
  {
  }
  
  public Iterator<DistCacheEntry> getEntries()
  {
    return _entryCache.values();
  }
  
  public void start()
  {
    _keyCache = new LruCache<CacheKey,HashKey>(64 * 1024);
    
    if (_dataBacking == null)
      _dataBacking = new CacheDataBackingImpl();
    
    if (getDataBacking() == null)
      throw new NullPointerException();
    
    _dataBacking.start();
    
    _cacheEngine.start();
  }
  public boolean readData(HashKey keyHash, MnodeEntry mnodeEntry,
                          OutputStream os, CacheConfig config)
    throws IOException
  {
    return _localDataManager.readData(keyHash,  mnodeEntry, os, config);
  }

  public void closeCache(String guid)
  {
    _keyCache.clear();
  }

  protected HashKey createHashKey(Object key, CacheConfig config)
  {
    CacheKey cacheKey = new CacheKey(config.getGuid(),
                                     config.getGuidHash(), 
                                     key);
    
    HashKey hashKey = _keyCache.get(cacheKey);
    
    if (hashKey == null) {
      hashKey = createHashKeyImpl(key, config);
      
      _keyCache.put(cacheKey, hashKey);
    }
    
    return hashKey;
  }

  /**
   * Sets a cache entry
   */
  public void put(HashKey hashKey,
                  Object value,
                  CacheConfig config)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(HashKey hashKey,
                           InputStream is,
                           CacheConfig config,
                           long idleTimeout)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Called when a cache initializes.
   */
  public void initCache(CacheImpl cache)
  {
    // XXX: engine.initCache
  }

  /**
   * Called when a cache is removed.
   */
  public void destroyCache(CacheImpl cache)
  {
    
  }

  /**
   * Returns the key hash
   */
  protected HashKey createHashKeyImpl(Object key, CacheConfig config)
  {
    try {
      KeyHashStream dOut = _keyStreamFreeList.allocate();
      
      if (dOut == null) {
        MessageDigest digest
          = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);
      
        dOut = new KeyHashStream(digest);
      }
      
      dOut.init();

      CacheSerializer keySerializer = config.getKeySerializer();
      
      keySerializer.serialize(config.getGuid(), dOut);
      keySerializer.serialize(key, dOut);

      HashKey hashKey = new HashKey(dOut.digest());
      
      _keyStreamFreeList.free(dOut);

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the key hash
   */
  public HashKey createSelfHashKey(Object key, CacheSerializer keySerializer)
  {
    try {
      MessageDigest digest
        = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      KeyHashStream dOut = new KeyHashStream(digest);

      keySerializer.serialize(key, dOut);

      HashKey hashKey = new HashKey(dOut.digest());

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes the manager.
   */
  public void close()
  {
    _isClosed = true;

    if (getDataBacking() != null)
      getDataBacking().close();
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  //
  // QA
  //

  public long calculateValueHash(Object value, CacheConfig config)
  {
    return _localDataManager.calculateValueHash(value, config);
  }
  
  public MnodeStore getMnodeStore()
  {
    return _dataBacking.getMnodeStore();
  }
  
  public DataStore getDataStore()
  {
    return _dataBacking.getDataStore();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _resinSystem.getId() + "]";
  }
  
  public static class DataItem {
    private long _valueHash;
    private long _dataIndex;
    private long _length;
    
    DataItem(long valueHash, long dataIndex, long length)
    {
      _valueHash = valueHash;
      _dataIndex = dataIndex;
      _length = length;
    }
    
    /**
     * @return
     */
    public long getValueDataId()
    {
      return _dataIndex;
    }

    public long getValueHash()
    {
      return _valueHash;
    }
    
    public long getLength()
    {
      return _length;
    }
  }
  
  static final class CacheKey {
    private final String _guid;
    private final Object _key;
    private final int _hashCode;
    
    CacheKey(String guid, int guidHash, Object key)
    {
      _guid = guid;
      
      if (key == null)
        key = NULL_OBJECT;
      
      _key = key;
      
      _hashCode = 65521 * (17 + guidHash) + key.hashCode();
    }
    
    @Override
    public final int hashCode()
    {
      return _hashCode;
    }
    
    @Override
    public boolean equals(Object o)
    {
      CacheKey key = (CacheKey) o;
      
      if (! key._key.equals(_key))
        return false;
      
      return key._guid.equals(_guid);
    }
  }

  static class KeyHashStream extends OutputStream {
    private MessageDigest _digest;

    KeyHashStream(MessageDigest digest)
    {
      _digest = digest;
    }
    
    void init()
    {
      _digest.reset();
    }

    @Override
    public void write(int value)
    {
      _digest.update((byte) value);
    }

    @Override
    public void write(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    public byte []digest()
    {
      byte []digest = _digest.digest();
      
      return digest;
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close()
    {
    }
  }
}
