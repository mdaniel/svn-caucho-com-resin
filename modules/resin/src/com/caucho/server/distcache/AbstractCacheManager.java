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
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.cache.CacheLoader;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.AbstractCacheClusterBacking;
import com.caucho.env.distcache.CacheClusterBacking;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.service.ResinSystem;
import com.caucho.inject.Module;
import com.caucho.util.Alarm;
import com.caucho.util.NullOutputStream;
import com.caucho.util.ResinDeflaterOutputStream;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.Sha256OutputStream;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Manages the distributed cache
 */
@Module
abstract public class AbstractCacheManager<E extends DistCacheEntry>
  extends DistributedCacheManager
{
  private static final Logger log
    = Logger.getLogger(AbstractCacheManager.class.getName());

  private static final L10N L = new L10N(AbstractCacheManager.class);
  
  private final ResinSystem _resinSystem;
  
  private CacheDataBacking _dataBacking;
  private CacheClusterBacking _clusterBacking;
  
  private ConcurrentHashMap<HashKey,CacheMnodeListener> _cacheListenMap
    = new ConcurrentHashMap<HashKey,CacheMnodeListener>();
  
  private boolean _isCacheListen;
  
  private final LruCache<HashKey, E> _entryCache
    = new LruCache<HashKey, E>(64 * 1024);
  
  private boolean _isClosed;
  
  public AbstractCacheManager(ResinSystem resinSystem)
  {
    _resinSystem = resinSystem;
    // new AdminPersistentStore(this);
    _clusterBacking = new AbstractCacheClusterBacking();
  }
  
  @Override
  public CacheDataBacking getDataBacking()
  {
    return _dataBacking;
  }
  
  protected void setClusterBacking(CacheClusterBacking clusterBacking)
  {
    _clusterBacking = clusterBacking;
  }
  
  protected CacheClusterBacking getClusterBacking()
  {
    return _clusterBacking;
  }
  
  protected CacheDataBacking createDataBacking()
  {
    return new CacheDataBackingImpl();
  }
  
  public void addCacheListener(HashKey cacheKey, CacheMnodeListener listener)
  {
    _cacheListenMap.put(cacheKey, listener);
    _isCacheListen = true;
  }

  /**
   * Returns the key entry.
   */
  @Override
  public final E getCacheEntry(Object key, CacheConfig config)
  {
    HashKey hashKey = createHashKey(key, config);

    E cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(key, hashKey);

      cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns the key entry.
   */
  @Override
  public final E getCacheEntry(HashKey hashKey, CacheConfig config)
  {
    E cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(null, hashKey);

      cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    }

    return cacheEntry;
  }

  abstract protected E createCacheEntry(Object key, HashKey hashKey);

  /**
   * Returns the key entry.
   */
  final public E getCacheEntry(HashKey hashKey)
  {
    E cacheEntry = _entryCache.get(hashKey);

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

  final public Object get(E entry,
                          CacheConfig config,
                          long now)
  {
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now);//, isLazy);

    if (mnodeValue == null)
      return null;

    Object value = mnodeValue.getValue();

    if (value != null)
      return value;

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return null;

    updateAccessTime(entry, mnodeValue, now);

    value = readData(valueHash,
                     config.getFlags(),
                     config.getValueSerializer());
    
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
  final public boolean getStream(E entry,
                                 OutputStream os,
                                 CacheConfig config)
    throws IOException
  {
    long now = Alarm.getCurrentTime();

    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    if (mnodeValue == null)
      return false;

    updateAccessTime(entry, mnodeValue, now);

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return false;

    boolean isData = readData(mnodeValue, config.getFlags(), os);
    
    if (! isData) {
      log.warning("Missing or corrupted data for getStream " + mnodeValue
                  + " " + entry);
      // Recovery from dropped or corrupted data
      remove(entry, config);
    }

    return isData;
  }

  final public MnodeEntry getMnodeValue(E entry,
                                        CacheConfig config,
                                        long now)
                                        // boolean isLazy)
  {
    MnodeEntry mnodeValue = loadMnodeValue(entry);

    if (mnodeValue == null) {
      reloadValue(entry, config, now);
    }
    else if (isLocalReadValid(config, mnodeValue, now)) {
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
      updateIdleTime(entry, mnodeValue);
    }

    return entry.getMnodeEntry();
  }

  private void reloadValue(E entry,
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
  protected void lazyValueUpdate(E entry, CacheConfig config)
  {
    reloadValue(entry, config, Alarm.getCurrentTime());
  }

  protected boolean isLocalReadValid(CacheConfig config,
                                     MnodeEntry mnodeValue,
                                     long now)
  {
    return ! mnodeValue.isEntryExpired(now);
  }

  private void updateAccessTime(E entry,
                                MnodeEntry mnodeValue,
                                long now)
  {
    if (mnodeValue != null) {
      long idleTimeout = mnodeValue.getIdleTimeout();
      long updateTime = mnodeValue.getLastUpdateTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
          && updateTime + mnodeValue.getIdleWindow() < now) {
        // XXX:
        mnodeValue.setLastAccessTime(now);

        saveUpdateTime(entry, mnodeValue);
      }
    }
  }

  private void loadExpiredValue(E entry,
                                CacheConfig config,
                                long now)
  {
    MnodeEntry mnodeValue = getClusterBacking().loadClusterValue(entry, config);
    
    if (mnodeValue == null || mnodeValue.isEntryExpired(now)) {
      CacheLoader loader = config.getCacheLoader();

      if (loader != null && entry.getKey() != null) {
        Object arg = null;
        
        Object value = loader.load(entry.getKey(), arg);

        if (value != null) {
          put(entry, value, config, now, mnodeValue);

          return;
        }
      }

      MnodeEntry nullMnodeValue = new MnodeEntry(null, 0, 0, null, null,
                                                 0,
                                                 config.getExpireTimeout(),
                                                 config.getIdleTimeout(),
                                                 config.getLeaseTimeout(),
                                                 now, now,
                                                 true, true);

      entry.compareAndSet(mnodeValue, nullMnodeValue);
    }
    else
      mnodeValue.setLastAccessTime(now);
  }
  
  public DataItem getValueHash(Object value, CacheConfig config)
  {
    return writeData(null, value, config.getValueSerializer());
  }
  
 /**
   * Sets a cache entry
   */
  final public void put(E entry,
                        Object value,
                        CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    put(entry, value, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final void put(E entry,
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
                               config.getLeaseTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return;

    getClusterBacking().putCluster(key, mnodeUpdate, mnodeValue);

    return;
  }

  public final ExtCacheEntry putStream(E entry,
                                       InputStream is,
                                       CacheConfig config,
                                       long idleTimeout)
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

    MnodeUpdate mnodeUpdate = new MnodeUpdate(key.getHash(),
                                              valueHash.getHash(),
                                              valueLength,
                                              newVersion,
                                              HashKey.getHash(config.getCacheKey()),
                                              config.getFlags(),
                                              config.getExpireTimeout(),
                                              idleTimeout);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;
    
    int leaseOwner = (mnodeValue != null) ? mnodeValue.getLeaseOwner() : -1;
    
    mnodeValue = putLocalValue(entry, mnodeUpdate, null,
                               config.getLeaseTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return null;

    getClusterBacking().putCluster(key, mnodeUpdate, mnodeValue);

    return mnodeValue;
  }
  
  /**
   * Sets a cache entry
   */
  final public Object getAndPut(E entry,
                                Object value,
                                CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = getMnodeValue(entry, config, now); // , false);

    return getAndPut(entry, value, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final Object getAndPut(E entry,
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
                                config.getLeaseTimeout(),
                                leaseOwner);

    if (oldHash == null)
      return null;
    
    if (oldHash.equals(valueHash) && oldValue != null)
      return oldValue;
    
    oldValue = readData(oldHash,
                        config.getFlags(),
                        config.getValueSerializer());

    return oldValue;
  }

  /**
   * Sets a cache entry
   */
  abstract protected HashKey getAndPut(DistCacheEntry entry,
                                       MnodeUpdate mnodeUpdate,
                                       Object value,
                                       long leaseTimeout,
                                       int leaseOwner);
  
  /**
   * Sets a cache entry
   */
  public final HashKey getAndPutLocal(DistCacheEntry entry,
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

  public HashKey compareAndPut(E entry, 
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

  abstract protected HashKey compareAndPut(E entry,
                                           HashKey testValue,
                                           MnodeUpdate mnodeUpdate,
                                           Object value,
                                           CacheConfig config);
  
  public final HashKey compareAndPutLocal(E entry,
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

  public boolean compareAndPut(E entry,
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
                               config.getLeaseTimeout(),
                               leaseOwner);
    
    if (mnodeValue == null)
      return false;

    getClusterBacking().putCluster(key, mnodeUpdate, mnodeValue);

    return true;
  }

  final E getLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    E entry = getCacheEntry(key);

    return entry;
  }

  public final E loadLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    E entry = getCacheEntry(key);

    long now = Alarm.getCurrentTime();

    if (entry.getMnodeEntry() == null
        || entry.getMnodeEntry().isEntryExpired(now)) {
      forceLoadMnodeValue(entry);
    }

    return entry;
  }

  public final E getLocalEntryAndUpdateIdle(HashKey key)
  {
    E entry = getLocalEntry(key);

    MnodeEntry mnodeValue = entry.getMnodeEntry();

    if (mnodeValue != null) {
      updateIdleTime(entry, mnodeValue);
    }

    return entry;
  }

  final protected void updateIdleTime(E entry, MnodeEntry mnodeValue)
  {
    long idleTimeout = mnodeValue.getIdleTimeout();
    long updateTime = mnodeValue.getLastUpdateTime();

    long now = Alarm.getCurrentTime();

    if (idleTimeout < CacheConfig.TIME_INFINITY
        && updateTime + mnodeValue.getIdleWindow() < now) {
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
    E entry = getCacheEntry(key);

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
  final MnodeEntry saveUpdateTime(E entryKey,
                                  MnodeEntry mnodeValue)
  {
    MnodeEntry newEntryValue = saveLocalUpdateTime(entryKey, mnodeValue);

    if (newEntryValue.getVersion() != mnodeValue.getVersion())
      return newEntryValue;

    updateCacheTime(entryKey.getKeyHash(), mnodeValue);

    return mnodeValue;
  }

  // XXX:
  protected void updateCacheTime(HashKey key, MnodeEntry mnodeValue)
  {
    // _cacheService.updateTime(entryKey.getKeyHash(), mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  public final void saveLocalUpdateTime(HashKey key,
                                        long version,
                                        long idleTimeout,
                                        long updateTime)
  {
    DistCacheEntry entry = _entryCache.get(key);

    if (entry == null)
      return;

    MnodeEntry oldEntryValue = entry.getMnodeEntry();

    if (oldEntryValue == null || version != oldEntryValue.getVersion())
      return;

    MnodeEntry mnodeValue
      = new MnodeEntry(oldEntryValue, idleTimeout, updateTime);

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
        && mnodeValue.getLastAccessTime() == oldEntryValue.getLastAccessTime()
        && mnodeValue.getLastUpdateTime() == oldEntryValue.getLastUpdateTime()) {
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
  public final boolean remove(E entry, CacheConfig config)
  {
    HashKey key = entry.getKeyHash();

    MnodeEntry mnodeEntry = loadMnodeValue(entry);
    HashKey oldValueHash = mnodeEntry != null ? mnodeEntry.getValueHashKey() : null;

    long newVersion = getNewVersion(mnodeEntry);

    long leaseTimeout = (mnodeEntry != null
                         ? mnodeEntry.getLeaseTimeout()
                         : config.getLeaseTimeout());
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

    getClusterBacking().removeCluster(key, mnodeUpdate, mnodeEntry);

    return oldValueHash != null;
  }

  /**
   * Sets a cache entry
   */
  @Override
  public final boolean remove(HashKey key)
  {
    E entry = getCacheEntry(key);
    MnodeEntry mnodeValue = entry.getMnodeEntry();

    HashKey oldValueHash = mnodeValue != null ? mnodeValue.getValueHashKey() : null;

    long newVersion = getNewVersion(mnodeValue);

    long expireTimeout = mnodeValue != null ? mnodeValue.getExpireTimeout() : -1;
    long idleTimeout = mnodeValue != null ? mnodeValue.getIdleTimeout() : -1;
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

    getClusterBacking().putCluster(key, null, mnodeValue);

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
      long now = Alarm.getCurrentTime();
      
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
  @Override
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
    ResinDeflaterOutputStream gzOut = new ResinDeflaterOutputStream(mOut);

    serializer.serialize(value, gzOut);
    //serializer.serialize(value, mOut);

    //gzOut.finish();
    gzOut.close();
    mOut.close();
    
    byte []hash = mOut.getDigest();
    
    return hash;
  }

  final protected DataItem writeData(HashKey oldValueHash,
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
                                  int flags,
                                  CacheSerializer serializer)
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      return null;

    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      WriteStream out = Vfs.openWrite(os);

      if (! getDataBacking().loadData(valueKey, out)) {
        if (! loadClusterData(valueKey, flags)) {
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
        InflaterInputStream gzIn = new InflaterInputStream(is);

        Object value = serializer.deserialize(gzIn);
        // Object value = serializer.deserialize(is);

        gzIn.close();

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
                                   int flags,
                                   OutputStream os)
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

      if (! loadClusterData(valueKey, flags)) {
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

    long now = Alarm.getCurrentTime();
  
    if (newVersion < now)
      return now;
    else
      return newVersion;
  }

  /**
   * Load the cluster data from the triad.
   */
  protected boolean loadClusterData(HashKey valueKey, int flags)
  {
    // _cacheService.requestData(valueKey, flags);
    return true;
  }

  /**
   * Clears leases on server start/stop
   */
  final public void clearLeases()
  {
    Iterator<E> iter = _entryCache.values();

    while (iter.hasNext()) {
      E entry = iter.next();

      entry.clearLease();
    }
  }

  /**
   * Clears ephemeral data on startup.
   */
  public void clearEphemeralEntries()
  {
  }
  
  @Override
  public void start()
  {
    super.start();
    
    if (_dataBacking == null)
      _dataBacking = createDataBacking();
    
    if (getDataBacking() == null)
      throw new NullPointerException();
    
    if (getClusterBacking() == null)
      throw new NullPointerException();
    
    _dataBacking.start();
  }

  /**
   * Closes the manager.
   */
  @Override
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
}
