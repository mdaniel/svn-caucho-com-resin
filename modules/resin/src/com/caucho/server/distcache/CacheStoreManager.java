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
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.db.blob.BlobInputStream;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.service.ResinSystem;
import com.caucho.inject.Module;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeList;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.NullOutputStream;
import com.caucho.util.Sha256OutputStream;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

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

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return null;

    updateAccessTime(entry, mnodeValue, now);

    value = readData(valueHash,
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

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return false;

    boolean isData = readData(mnodeValue, os, config);
    
    if (! isData) {
      log.warning("Missing or corrupted data for getStream " + mnodeValue
                  + " " + entry);
      // Recovery from dropped or corrupted data
      remove(entry, config);
    }

    return isData;
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
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    if (mnodeValue == null) {
      reloadValue(entry, config, now);
    }
    else if (! isExact 
             && ! isLocalExpired(config, entry.getKeyHash(), mnodeValue, now)) {
    }
    else { // if (! isLazy) {
      reloadValue(entry, config, now);
    }/*
    else {
      lazyValueUpdate(entry, config);
    }
    */

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
    MnodeEntry mnodeValue;
    
    mnodeValue = config.getEngine().get(entry, config);
    
    entry.addLoadCount();

    if (mnodeValue == null || mnodeValue.isExpired(now)) {
      CacheLoader loader = config.getCacheLoader();

      if (loader != null && config.isReadThrough() && entry.getKey() != null) {
        Object arg = null;
        
        Cache.Entry loaderEntry = loader.load(entry.getKey());

        if (entry != null) {
          put(entry, loaderEntry.getValue(), config, now, mnodeValue);

          return;
        }
      }

      MnodeEntry nullMnodeValue = new MnodeEntry(null, 0, 0, null, null,
                                                 0,
                                                 config.getAccessedExpireTimeout(),
                                                 config.getModifiedExpireTimeout(),
                                                 config.getLeaseExpireTimeout(),
                                                 now, now,
                                                 true, true);

      entry.compareAndSet(mnodeValue, nullMnodeValue);
    }
    else {
      mnodeValue.setLastAccessTime(now);
    }
  }
  
  public DataItem getValueHash(Object value, CacheConfig config)
  {
    return writeData(null, value, config.getValueSerializer());
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

    DataItem dataItem = writeData(mnodeValue, value,
                                  config.getValueSerializer());
    
    HashKey valueHash = dataItem.getValue();
    long valueLength = dataItem.getLength();

    long newVersion = getNewVersion(mnodeValue);
    
    MnodeUpdate mnodeUpdate
      = new MnodeUpdate(key, valueHash, valueLength, newVersion, config);

    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;
    
    mnodeValue = putLocalValue(entry, 
                               mnodeUpdate, value,
                               config.getLeaseExpireTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return;

    config.getEngine().put(key, mnodeUpdate, mnodeValue);
    
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
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    HashKey oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHashKey()
                            : null);

    DataItem valueItem = writeData(oldValueHash, is);
    
    HashKey valueHash = valueItem.getValue();

    if (valueHash != null && valueHash.equals(oldValueHash)) {
      return mnodeValue;
    }
    
    long valueLength = valueItem.getLength();
    long newVersion = getNewVersion(mnodeValue);
    
    long flags = config.getFlags() | ((long) userFlags) << 32;
    
    if (accessedExpireTime < 0)
      accessedExpireTime = config.getAccessedExpireTimeout();
    
    if (modifiedExpireTime < 0)
      modifiedExpireTime = config.getModifiedExpireTimeout();

    MnodeUpdate mnodeUpdate = new MnodeUpdate(key.getHash(),
                                              valueHash.getHash(),
                                              valueLength,
                                              newVersion,
                                              HashKey.getHash(config.getCacheKey()),
                                              flags,
                                              accessedExpireTime,
                                              modifiedExpireTime);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;
    
    int leaseOwner = (mnodeValue != null) ? mnodeValue.getLeaseOwner() : -1;
    
    mnodeValue = putLocalValue(entry, mnodeUpdate, null,
                               config.getLeaseExpireTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return null;
    
    config.getEngine().put(key, mnodeUpdate, mnodeValue);

    return mnodeValue;
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
    DataItem dataItem = writeData(mnodeValue, value,
                                  config.getValueSerializer());
    
    HashKey valueHash = dataItem.getValue();
    long valueLength = dataItem.getLength();
    
    long version = getNewVersion(mnodeValue);
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(entry.getKeyHash(),
                                              valueHash, valueLength, version, 
                                              config);
    
    Object oldValue = mnodeValue != null ? mnodeValue.getValue() : null;

    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;

    HashKey oldHash = getAndPut(entry, 
                                mnodeUpdate, value,
                                config.getLeaseExpireTimeout(),
                                leaseOwner,
                                config);

    if (oldHash == null)
      return null;
    
    if (oldHash.equals(valueHash) && oldValue != null)
      return oldValue;
    
    oldValue = readData(oldHash,
                        config.getValueSerializer(),
                        config);

    return oldValue;
  }

  /**
   * Sets a cache entry
   */
  protected HashKey getAndPut(DistCacheEntry entry,
                              MnodeUpdate mnodeUpdate,
                              Object value,
                              long leaseTimeout,
                              int leaseOwner,
                              CacheConfig config)
  {
    return config.getEngine().getAndPut(entry, mnodeUpdate, value,
                                        leaseTimeout, leaseOwner);
  }
  
  public HashKey getAndPutLocal(DistCacheEntry entry,
                                MnodeUpdate mnodeUpdate,
                                Object value,
                                long leaseTimeout,
                                int leaseOwner)
  {
    HashKey oldValueHash = entry.getValueHashKey();

    MnodeEntry mnodeValue = putLocalValue(entry, 
                                          mnodeUpdate, value,
                                          leaseTimeout,
                                          leaseOwner);

    return oldValueHash;
  }

  public Object getAndReplace(DistCacheEntry entry, 
                               HashKey testValue,
                               Object value, 
                               CacheConfig config)
  {
    HashKey result = compareAndPut(entry, testValue, value, config);
    
    if (result == null || result.isNull()) {
      return null;
    }
    else {
      return readData(result,
                      config.getValueSerializer(),
                      config);
    }
  }
  
  public HashKey compareAndPut(DistCacheEntry entry, 
                                 HashKey testValue,
                                 Object value, 
                                 CacheConfig config)
    {
    DataItem dataItem = writeData(entry.getMnodeEntry(), 
                                  value,
                                  config.getValueSerializer());
    
    HashKey valueHash = dataItem.getValue();
    long valueLength = dataItem.getLength();
    
    long version = getNewVersion(entry.getMnodeEntry());
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(entry.getKeyHash(),
                                              valueHash,
                                              valueLength,
                                              version,
                                              config);
    
    return compareAndPut(entry, testValue, mnodeUpdate, value, config);
  }

  protected HashKey compareAndPut(DistCacheEntry entry,
                                  HashKey testValue,
                                  MnodeUpdate mnodeUpdate,
                                  Object value,
                                  CacheConfig config)
  {
    CacheEngine engine = config.getEngine();
    
    return engine.compareAndPut(entry, testValue, mnodeUpdate, value, config);
  }
  
  public final HashKey compareAndPutLocal(DistCacheEntry entry,
                                          HashKey testValue,
                                          MnodeUpdate mnodeUpdate,
                                          Object value,
                                          long leaseTimeout,
                                          int leaseOwner)
  {
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    HashKey oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHashKey()
                            : null);

    if (testValue == null) {
    }
    else if (testValue.equals(oldValueHash)) {
      
    }
    else if (testValue.isNull()) {
      if (oldValueHash != null && ! oldValueHash.isNull())
        return null;
      
      oldValueHash = MnodeEntry.NULL_KEY;
    }
    else if (testValue.isAny()) {
      if (oldValueHash == null || oldValueHash.isNull())
        return null;
    }
    else {
      return null;
    }
    
    // long newVersion = getNewVersion(mnodeValue);

    mnodeValue = putLocalValue(entry, 
                               mnodeUpdate, null,
                               leaseTimeout,
                               leaseOwner);

    if (mnodeValue != null)
      return oldValueHash;
    else
      return null;
  }

  public boolean compareAndPut(DistCacheEntry entry,
                               long version,
                               HashKey valueHash, 
                               long valueLength,
                               CacheConfig config)
  {
    HashKey key = entry.getKeyHash();
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    HashKey oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHashKey()
                            : null);
    long oldVersion = mnodeValue != null ? mnodeValue.getVersion() : 0;

    if (version <= oldVersion)
      return false;

    if (valueHash != null && valueHash.equals(oldValueHash)) {
      return true;
    }
    
    long newVersion = version;
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(key, valueHash, valueLength,
                                              newVersion,
                                              config);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;

    int leaseOwner = (mnodeValue != null) ? mnodeValue.getLeaseOwner() : -1;
    
    mnodeValue = putLocalValue(entry,
                               mnodeUpdate, null,
                               config.getLeaseExpireTimeout(),
                               leaseOwner);
    
    if (mnodeValue == null)
      return false;
    
    config.getEngine().put(key, mnodeUpdate, mnodeValue);

    return true;
  }

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
      forceLoadMnodeValue(entry);
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

  /**
   * Gets a cache entry
   */
  private MnodeEntry forceLoadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeEntry mnodeValue = cacheEntry.getMnodeEntry();

    MnodeEntry newMnodeValue = getDataBacking().loadLocalEntryValue(key);

    cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

    mnodeValue = cacheEntry.getMnodeEntry();

    return mnodeValue;
  }

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

    MnodeEntry mnodeEntry = loadMnodeValue(entry);
    HashKey oldValueHash = mnodeEntry != null ? mnodeEntry.getValueHashKey() : null;

    long newVersion = getNewVersion(mnodeEntry);

    long leaseTimeout = (mnodeEntry != null
                         ? mnodeEntry.getLeaseTimeout()
                         : config.getLeaseExpireTimeout());
    int leaseOwner = (mnodeEntry != null ? mnodeEntry.getLeaseOwner() : -1);
    
    MnodeUpdate mnodeUpdate;
    
    if (mnodeEntry != null)
      mnodeUpdate = new MnodeUpdate(key.getHash(), (byte[]) null, 0, newVersion, mnodeEntry);
    else
      mnodeUpdate = new MnodeUpdate(key.getHash(),
                                    (byte[]) null, 0, newVersion, config);

    mnodeEntry = putLocalValue(entry, 
                               mnodeUpdate, null,
                               leaseTimeout,
                               leaseOwner);

    if (mnodeEntry == null)
      return oldValueHash != null;

    config.getEngine().remove(key, mnodeUpdate, mnodeEntry);
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      writer.delete(entry.getKey());
    }

    return oldValueHash != null;
  }

  /**
   * Sets a cache entry
   */
  public final boolean remove(HashKey key, CacheConfig config)
  {
    DistCacheEntry entry = getCacheEntry(key);
    MnodeEntry mnodeValue = entry.getMnodeEntry();

    HashKey oldValueHash = mnodeValue != null ? mnodeValue.getValueHashKey() : null;

    long newVersion = getNewVersion(mnodeValue);

    long leaseTimeout = mnodeValue != null ? mnodeValue.getLeaseTimeout() : -1;
    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;

    MnodeUpdate mnodeUpdate = new MnodeUpdate(key.getHash(),
                                              (byte[]) null, 0, newVersion,
                                              mnodeValue);
    
    mnodeValue = putLocalValue(entry,
                               mnodeUpdate,
                               null,
                               leaseTimeout, leaseOwner);

    if (mnodeValue == null)
      return oldValueHash != null;
    
    config.getEngine().put(key, null, mnodeValue);

    return oldValueHash != null;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(DistCacheEntry entry,
                                        MnodeUpdate mnodeUpdate,
                                        Object value,
                                        long leaseTimeout,
                                        int leaseOwner)
  {
    HashKey key = entry.getKeyHash();
    
    HashKey valueHash = HashKey.create(mnodeUpdate.getValueHash());
    long version = mnodeUpdate.getVersion();

    MnodeEntry oldEntryValue;
    MnodeEntry mnodeValue;

    do {
      oldEntryValue = loadMnodeValue(entry);
    
      HashKey oldValueHash
        = oldEntryValue != null ? oldEntryValue.getValueHashKey() : null;

      long oldVersion = oldEntryValue != null ? oldEntryValue.getVersion() : 0;
      long now = CurrentTime.getCurrentTime();
      
      if (version < oldVersion
          || (version == oldVersion
              && valueHash != null
              && valueHash.compareTo(oldValueHash) <= 0)) {
        // lease ownership updates even if value doesn't
        if (oldEntryValue != null) {
          oldEntryValue.setLeaseOwner(leaseOwner, now);

          // XXX: access time?
          oldEntryValue.setLastAccessTime(now);
        }

        return oldEntryValue;
      }

      long accessTime = now;
      long updateTime = accessTime;

      mnodeValue = new MnodeEntry(mnodeUpdate,
                                  value,
                                  leaseTimeout,
                                  accessTime,
                                  updateTime,
                                  true,
                                  false);
    } while (! entry.compareAndSet(oldEntryValue, mnodeValue));

    //MnodeValue newValue
    getDataBacking().putLocalValue(mnodeValue, key,  
                                   oldEntryValue,
                                   mnodeUpdate);

    if (mnodeValue.getCacheHash() != null && _isCacheListen) {
      HashKey cacheKey = HashKey.create(mnodeValue.getCacheHash());
      
      CacheMnodeListener listener = _cacheListenMap.get(cacheKey);

      if (listener != null)
        listener.onPut(key, mnodeValue);
    }
    
    return mnodeValue;
  }

  final public DataItem writeData(MnodeEntry mnodeValue,
                                  Object value,
                                  CacheSerializer serializer)
  {
    byte [] oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHash()
                            : null);
    
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      byte[] hash = writeDataStream(os, value, serializer);

      HashKey valueHash = new HashKey(hash);

      int length = os.getLength();

      if (HashKey.equals(hash, oldValueHash))
        return new DataItem(valueHash, length);

      StreamSource source = new StreamSource(os);
      if (! getDataBacking().saveData(valueHash, source, length)) {
        throw new IllegalStateException(L.l("Can't save the data '{0}'",
                                       valueHash));
      }

      return new DataItem(valueHash, length);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.destroy();
    }
  }

  /**
   * Used by QA
   */
  final public byte[] calculateValueHash(Object value,
                                         CacheConfig config)
  {
    // TempOutputStream os = null;
    
    try {
      NullOutputStream os = NullOutputStream.NULL;

      return writeDataStream(os, value, config.getValueSerializer());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private byte []writeDataStream(OutputStream os, 
                                 Object value, 
                                 CacheSerializer serializer)
    throws IOException
  {
    Sha256OutputStream mOut = new Sha256OutputStream(os);
    //DeflaterOutputStream gzOut = new DeflaterOutputStream(mOut);
    //ResinDeflaterOutputStream gzOut = new ResinDeflaterOutputStream(mOut);

    //serializer.serialize(value, gzOut);
    serializer.serialize(value, mOut);

    //gzOut.finish();
    //gzOut.close();
    mOut.close();
    
    byte []hash = mOut.getDigest();
    
    return hash;
  }

  final public DataItem writeData(HashKey oldValueHash,
                                  InputStream is)
    throws IOException
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      Sha256OutputStream mOut = new Sha256OutputStream(os);

      WriteStream out = Vfs.openWrite(mOut);

      out.writeStream(is);

      out.close();

      mOut.close();

      byte[] hash = mOut.getDigest();

      HashKey valueHash = new HashKey(hash);

      int length = os.getLength();
      
      if (valueHash.equals(oldValueHash)) {
        return new DataItem(valueHash, length);
      }
      
      StreamSource source = new StreamSource(os);

      if (! getDataBacking().saveData(valueHash, source, length))
        throw new RuntimeException(L.l("Can't save the data '{0}'",
                                       valueHash));
      
      return new DataItem(valueHash, length);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.destroy();
    }
  }

  final protected Object readData(HashKey valueKey,
                                  CacheSerializer serializer,
                                  CacheConfig config)
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      return null;

    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      WriteStream out = Vfs.openWrite(os);

      if (! getDataBacking().loadData(valueKey, out)) {
        if (! loadClusterData(valueKey, config)) {
          log.warning(this + " cannot load data for " + valueKey + " from triad");
          
          out.close();
        
          return null;
        }

        if (! getDataBacking().loadData(valueKey, out)) {
          out.close();
        
          return null;
        }
      }

      out.close();

      InputStream is = os.openInputStream();

      try {
        // InflaterInputStream gzIn = new InflaterInputStream(is);

        // Object value = serializer.deserialize(gzIn);
        Object value = serializer.deserialize(is);

        // gzIn.close();

        return value;
      } finally {
        is.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    } finally {
      if (os != null)
        os.destroy();
    }
  }

  final protected boolean readData(MnodeEntry mnodeValue,
                                   OutputStream os,
                                   CacheConfig config)
    throws IOException
  {
    HashKey valueKey = mnodeValue.getValueHashKey();
    
    if (valueKey == null || valueKey == HashManager.NULL)
      throw new IllegalStateException(L.l("readData may not be called with a null value"));

    WriteStream out = Vfs.openWrite(os);

    try {
      Blob blob = mnodeValue.getBlob();
      
      if (blob == null) {
        blob = getDataBacking().loadBlob(valueKey);
        
        if (blob != null)
          mnodeValue.setBlob(blob);
      }

      if (blob != null) {
        loadData(blob, out);

        return true;
      }

      if (! loadClusterData(valueKey, config)) {
        log.warning(this + " cannot load cluster value " + valueKey);

        // XXX: error?  since we have the value key, it should exist

        // server/0180
        // return false;
      }

      if (getDataBacking().loadData(valueKey, out)) {
        return true;
      }

      log.warning(this + " unexpected load failure in readValue " + valueKey);

      // XXX: error?  since we have the value key, it should exist

      return false;
    } finally {
      if (out != os)
        out.close();
    }
  }
  
  private void loadData(Blob blob, WriteStream out)
    throws IOException
  {
    try {
      InputStream is = blob.getBinaryStream();
      
      if (is instanceof BlobInputStream) {
        BlobInputStream blobIs = (BlobInputStream) is;
        
        blobIs.readToOutput(out);
      }
      else {
        out.writeStream(blob.getBinaryStream());
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
  
  private long getNewVersion(MnodeEntry mnodeValue)
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
  protected boolean loadClusterData(HashKey valueKey, 
                                    CacheConfig config)
  {
    return config.getEngine().loadData(valueKey, config.getFlags());
  }

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
    private HashKey _value;
    private long _length;
    
    DataItem(HashKey value, long length)
    {
      _value = value;
      _length = length;
    }
    
    public HashKey getValue()
    {
      return _value;
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
