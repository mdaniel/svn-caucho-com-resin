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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.server.distcache.LocalDataManager.DataItem;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamSource;

/**
 * An entry in the cache map
 */
public class DistCacheEntry {
  private static final L10N L = new L10N(DistCacheEntry.class);
  
  private static final Logger log
    = Logger.getLogger(DistCacheEntry.class.getName());
  
  private final CacheStoreManager _cacheService;
  private final HashKey _keyHash;
  private final TriadOwner _owner;

  private Object _key;

  private final AtomicBoolean _isReadUpdate = new AtomicBoolean();

  private final AtomicReference<MnodeEntry> _mnodeEntry
    = new AtomicReference<MnodeEntry>(MnodeEntry.NULL);
  
  private final AtomicInteger _loadCount = new AtomicInteger();

  DistCacheEntry(CacheStoreManager engine,
                 HashKey keyHash,
                 TriadOwner owner,
                 CacheConfig config)
  {
    _cacheService = engine;
    _keyHash = keyHash;
    
    _owner = TriadOwner.getHashOwner(keyHash.getHash());
    
    _mnodeEntry.set(MnodeEntry.createInitialNull(config));
  }

  /**
   * Returns the key for this entry in the Cache.
   */
  public final Object getKey()
  {
    return _key;
  }
  
  public final void setKey(Object key)
  {
    if (_key == null)
      _key = key;
  }
  
  private LocalDataManager getLocalDataManager()
  {
    return _cacheService.getLocalDataManager();
  }

  /**
   * Returns the keyHash
   */
  public final HashKey getKeyHash()
  {
    return _keyHash;
  }

  /**
   * Returns the owner
   */
  public final TriadOwner getOwner()
  {
    return _owner;
  }

  /**
   * Returns the value section of the entry.
   */
  public final MnodeEntry getMnodeEntry()
  {
    return _mnodeEntry.get();
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  public Object get(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    return get(config, now);
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  /*
  public Object getExact(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    return get(config, now, true);
  }
  */

  /**
   * Returns the object for the given key, checking the backing if necessary
   */
  public MnodeEntry loadMnodeValue(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/01o9
    return loadMnodeValue(config, now, false); // , false);
  }

  /**
   * Gets a cache entry as a stream
   */
  final public StreamSource getValueStream()
  {
    MnodeEntry mnodeValue = getMnodeEntry();

    // server/01o9
    // updateAccessTime();

    return getLocalDataManager().createDataSource(mnodeValue.getValueDataId());
  }

  public long getValueHash(Object value, CacheConfig config)
  {
    if (value == null)
      return 0;

    return _cacheService.calculateValueHash(value, config);
  }
  
  public CacheUpdateWithSource loadCacheStream(long requestVersion,
                                               boolean isValueStream)
  {
    MnodeEntry mnodeEntry = getMnodeEntry();
    
    if (mnodeEntry.getVersion() <= requestVersion) {
      return new CacheUpdateWithSource(mnodeEntry, null, 
                                       mnodeEntry.getLeaseOwner());
    }
    else if (mnodeEntry.isImplicitNull()) {
      return new CacheUpdateWithSource(mnodeEntry, null, 
                                       mnodeEntry.getLeaseOwner());
    }

    StreamSource source = null;
      
    if (isValueStream) {
      long valueDataId = mnodeEntry.getValueDataId();
      
      DataStreamSource dataSource
          = getLocalDataManager().createDataSource(valueDataId);

      if (dataSource != null) {
        source = new StreamSource(dataSource);
      }

      // XXX: updateLease(entryKey, mnodeEntry, leaseOwner);
    }

    return new CacheUpdateWithSource(mnodeEntry, source, mnodeEntry.getLeaseOwner());
  }

 /**
   * Sets a cache entry
   */
  final public void put(Object value,
                        CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = loadMnodeValue(config, now, true); // , false);

    put(value, config, now, mnodeValue);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  CacheConfig config,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    putStream(is, config, 
              accessedExpireTimeout,
              modifiedExpireTimeout, 
              0,
              lastAccessTime,
              lastModifiedTime);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  CacheConfig config,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout,
                  long lastAccessTime,
                  long lastModifiedTime)
    throws IOException
  {
    putStream(is, config, 
              accessedExpireTimeout,
              modifiedExpireTimeout, 
              0,
              lastAccessTime,
              lastModifiedTime);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  CacheConfig config,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout,
                  int flags)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    putStream(is, config, 
              accessedExpireTimeout, 
              modifiedExpireTimeout,
              flags,
              lastAccessTime,
              lastModifiedTime);
  }

  private final void putStream(InputStream is,
                               CacheConfig config,
                               long accessedExpireTime,
                               long modifiedExpireTime,
                               int userFlags,
                               long lastAccessTime,
                               long lastModifiedTime)
    throws IOException
  {
    loadLocalMnodeValue();

    DataItem valueItem = getLocalDataManager().writeData(is);
    
    long valueHash = valueItem.getValueHash();
    long valueDataId = valueItem.getValueDataId();
    
    long valueLength = valueItem.getLength();
    long newVersion = getNewVersion(getMnodeEntry());
    
    long flags = config.getFlags() | ((long) userFlags) << 32;
    
    if (accessedExpireTime < 0)
      accessedExpireTime = config.getAccessedExpireTimeout();
    
    if (modifiedExpireTime < 0)
      modifiedExpireTime = config.getModifiedExpireTimeout();
    
    if (valueHash == getMnodeEntry().getValueHash()
        && flags == getMnodeEntry().getFlags()) {
    }
    
    int leaseOwner = getMnodeEntry().getLeaseOwner();
    long leaseExpireTimeout = config.getLeaseExpireTimeout();
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(valueHash,
                                              valueLength,
                                              newVersion,
                                              HashKey.getHash(config.getCacheKey()),
                                              flags,
                                              accessedExpireTime,
                                              modifiedExpireTime,
                                              leaseExpireTimeout,
                                              leaseOwner,
                                              lastAccessTime,
                                              lastModifiedTime);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;
    
    putLocalValue(mnodeUpdate, valueDataId, null);
    
    config.getEngine().put(getKeyHash(), mnodeUpdate, valueDataId);
  }

  /**
   * Sets a cache entry
   */
  public final boolean remove(CacheConfig config)
  {
    HashKey key = getKeyHash();

    MnodeEntry mnodeEntry = loadLocalMnodeValue();
    long oldValueHash = mnodeEntry.getValueHash();

    long newVersion = getNewVersion(mnodeEntry);

    /*
    long leaseTimeout = (mnodeEntry != null
                         ? mnodeEntry.getLeaseTimeout()
                         : config.getLeaseExpireTimeout());
    int leaseOwner = (mnodeEntry != null ? mnodeEntry.getLeaseOwner() : -1);
    */
    
    MnodeUpdate mnodeUpdate = MnodeUpdate.createNull(newVersion, config);

    /*
    if (mnodeEntry != null)
      mnodeUpdate = MnodeUpdate.createNull(newVersion, mnodeEntry);
    else
      mnodeUpdate = MnodeUpdate.createNull(newVersion, config);
      */

    putLocalValueImpl(mnodeUpdate, 0, null);
    
    config.getEngine().remove(key, mnodeUpdate);
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      writer.delete(getKey());
    }

    return oldValueHash != 0;
  }
  
  //
  // atomic operations
  //

  /**
   * Sets the value by an input stream
   */
  public boolean putIfNew(MnodeUpdate update,
                          InputStream is,
                          CacheConfig config)
    throws IOException
  {
    MnodeEntry entry = getMnodeEntry();
    
    if (update.getVersion() < entry.getVersion()) {
      return false;
    }
    else if (update.getVersion() == entry.getVersion()
             && update.getValueHash() == entry.getValueHash()) {
      return false;
    }
    
    MnodeValue newValue = putLocalValue(update, is);
    
    entry = getMnodeEntry();
    
    if (newValue.getValueHash() == update.getValueHash()) {
      config.getEngine().put(getKeyHash(), update, entry.getValueDataId());
    }

    return newValue.getValueHash() == update.getValueHash();
  }
  

  //
  // compare and put
  //
  
  public boolean compareAndPut(long testValue,
                               Object value, 
                               CacheConfig config)
  {
    MnodeEntry oldMnodeEntry = getMnodeEntry();
    
    DataItem dataItem
      = getLocalDataManager().writeValue(getMnodeEntry(), value, config);
    long valueDataId = dataItem.getValueDataId();
    long newVersion = getNewVersion(oldMnodeEntry);
    
    long oldValueDataId = oldMnodeEntry.getValueDataId();
    
    
    try {
      MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                           dataItem.getLength(),
                                           newVersion,
                                           config);
//                                           oldMnodeEntry);
      
      return config.getEngine().compareAndPut(this, testValue,
                                              update, 
                                              valueDataId);
    } finally {
      MnodeValue newMnodeValue = getMnodeEntry();
      
      if (newMnodeValue.getValueHash() != dataItem.getValueHash()) {
        getLocalDataManager().removeData(valueDataId);
      }
      /*
      else if (oldValueDataId != 0) {
        getLocalDataManager().removeData(oldValueDataId);
      }
      */
    }
  }
  
  public final boolean compareAndPutLocal(long testValue,
                                          MnodeUpdate update,
                                          StreamSource source)
  {
    long prevDataItem = getMnodeEntry().getValueDataId();

    // long version = getNewVersion(getMnodeEntry());
    long version = update.getVersion();
    

    // cloud/60r0
    DataItem dataItem = getLocalDataManager().writeData(update, version, source);
    
    long valueDataId = dataItem.getValueDataId();
    
    Object value = null;
    
    try {
      return compareAndPutLocal(testValue, update, valueDataId, value);
    } finally {
      /*
      MnodeEntry newMnodeValue = getMnodeEntry();
      
      if (newMnodeValue.getValueDataId() != valueDataId) {
        if (valueDataId != 0) {
          getLocalDataManager().removeData(valueDataId);
        }
      }
      else if (prevDataItem != 0) {
        // XXX:
        getLocalDataManager().removeData(prevDataItem);
      }
      */
    }
  }
  
  public boolean compareAndPutLocal(long testValueHash,
                                    MnodeUpdate update,
                                    long valueDataId,
                                    Object value)
  {
    MnodeEntry mnodeValue = loadLocalMnodeValue();

    long oldValueHash = mnodeValue.getValueHash();

    if (testValueHash == oldValueHash) {
    }
    else if (testValueHash == MnodeEntry.ANY_KEY && oldValueHash != 0) {
    }
    else {
      return false;
    }
    
    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;

    mnodeValue = putLocalValueImpl(update, valueDataId, value);
    
    return (mnodeValue != null);
  }

  protected boolean compareAndPut(DistCacheEntry entry,
                                  long testValue,
                                  MnodeUpdate mnodeUpdate,
                                  long valueDataId,
                                  Object value,
                                  CacheConfig config)
  {
    CacheEngine engine = config.getEngine();

    return engine.compareAndPut(entry, testValue, mnodeUpdate, valueDataId);
  }
  
  //
  // get and put
  //

  /**
   * Remove the value
   */
  public Object getAndRemove(CacheConfig config)
  {
    return getAndPut(null, config);
  }

  public Object getAndReplace(long testValue,
                              Object value, 
                              CacheConfig config)
  {
    long prevDataId = getMnodeEntry().getValueDataId();
    
    if (compareAndPut(testValue, value, config)) {
      long result = -1;
      
      return getLocalDataManager().readData(getKeyHash(),
                                            result,
                                            prevDataId,
                                            config.getValueSerializer(),
                                            config);
    }
    else {
      return null;
    }
  }

  /**
   * Sets the current value
   */
  public Object getAndPut(Object value, CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = loadMnodeValue(config, now, true); // , false);

    return getAndPut(value, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final Object getAndPut(Object value,
                                   CacheConfig config,
                                   long now,
                                   MnodeEntry mnodeValue)
  {
    DataItem dataItem
      = getLocalDataManager().writeValue(mnodeValue, value, config);
    
    long version = getNewVersion(getMnodeEntry());
    
    MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                         dataItem.getLength(),
                                         version,
                                         config);
    // int leaseOwner = mnodeValue.getLeaseOwner();

    InputStream is = getAndPut(update,
                               dataItem.getValueDataId(),
                               config);

    if (is == null)
      return null;
    
    Object oldValue = getLocalDataManager().decodeValue(is, config.getValueSerializer());

    return oldValue;
  }
  
  public long getAndPutLocal(MnodeUpdate mnodeUpdate,
                             StreamSource source)
  {
    long oldValueDataId = getMnodeEntry().getValueDataId();
    
    DataItem dataItem = getLocalDataManager().writeData(source);
    
    long valueDataId = dataItem.getValueDataId();
    
    Object value = null;

    putLocalValue(mnodeUpdate, valueDataId, value);

    return oldValueDataId;
  }
  
  public long getAndPutLocal(DistCacheEntry entry,
                             MnodeUpdate mnodeUpdate,
                             long valueDataId,
                             Object value)
  {
    long oldValueHash = entry.getMnodeEntry().getValueHash();

    entry.putLocalValue(mnodeUpdate, valueDataId, value);

    return oldValueHash;
  }

  /**
   * Sets a cache entry
   */
  private InputStream getAndPut(MnodeUpdate mnodeValue,
                                long valueDataId,
                                CacheConfig config)
  {
    return config.getEngine().getAndPut(this, mnodeValue, valueDataId);
  }
  
  //
  // utility
  //

  /**
   * Sets the current value.
   */
  public final boolean compareAndSetEntry(MnodeEntry oldMnodeValue,
                                          MnodeEntry mnodeValue)
  {
    if (mnodeValue == null)
      throw new NullPointerException();
    
    return _mnodeEntry.compareAndSet(oldMnodeValue, mnodeValue);
  }
  
  /**
   * Writes the data to a stream.
   */
  public boolean readData(OutputStream os, CacheConfig config)
    throws IOException
  {
    return getLocalDataManager().readData(getKeyHash(), getMnodeEntry(),
                                          os, config);
  }

  public boolean isModified(MnodeValue newValue)
  {
    MnodeEntry oldValue = getMnodeEntry();
    
    if (oldValue.getVersion() < newValue.getVersion()) {
      return true;
    }
    else if (newValue.getVersion() < oldValue.getVersion()) {
      return false;
    }
    else {
      // XXX: need to check hash.
      return true;
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

  public void clearLease(int oldLeaseOwner)
  {
    getMnodeEntry().clearLease(oldLeaseOwner);
  }

  public void clearLease()
  {
    getMnodeEntry().clearLease();
  }
  
  public boolean isLeaseExpired()
  {
    return getMnodeEntry().isLeaseExpired(CurrentTime.getCurrentTime());
  }
  
  public void updateLease(int leaseOwner)
  {
    if (leaseOwner <= 2) {
      return;
    }
    
    long now = CurrentTime.getCurrentTime();

    MnodeEntry entry = getMnodeEntry();
    
    if (isLeaseExpired() || entry.getLeaseOwner() == leaseOwner) {
      entry.setLeaseOwner(leaseOwner, now);
    }
  }

  public long getCost()
  {
    return 0;
  }
  
  //
  // get/load operations
  //

  private Object get(CacheConfig config,
                     long now)
  {
    MnodeEntry mnodeEntry = loadMnodeValue(config, now, true);

    if (mnodeEntry == null) {
      return null;
    }

    Object value = mnodeEntry.getValue();

    if (value != null) {
      return value;
    }

    long valueHash = mnodeEntry.getValueHash();

    if (valueHash == 0) {
      return null;
    }
    
    updateAccessTime(mnodeEntry, now);

    value = _cacheService.getLocalDataManager().readData(getKeyHash(),
                                                         valueHash,
                                                         mnodeEntry.getValueDataId(),
                                                         config.getValueSerializer(),
                                                         config);
    
    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data in get for " 
                  + mnodeEntry + " " + this);
      remove(config);
    }

    mnodeEntry.setObjectValue(value);

    return value;
  }

  final private MnodeEntry loadMnodeValue(CacheConfig config, 
                                          long now,
                                          boolean isUpdateAccessTime)
  {
    MnodeEntry mnodeEntry = loadLocalMnodeValue();
    
    int server = config.getEngine().getServerIndex();
    
    if (mnodeEntry == null || mnodeEntry.isLocalExpired(server, now, config)) {
      reloadValue(config, now, isUpdateAccessTime);
    }
    
    // server/016q
    if (isUpdateAccessTime) {
      updateAccessTime();
    }

    mnodeEntry = getMnodeEntry();

    return mnodeEntry;
  }

  private boolean isLocalExpired(CacheConfig config,
                                 HashKey key,
                                 MnodeEntry mnodeEntry,
                                 long now)
  {
    return config.getEngine().isLocalExpired(config, key, mnodeEntry, now);
  }

  private void reloadValue(CacheConfig config,
                           long now,
                           boolean isUpdateAccessTime)
  {
    // only one thread may update the expired data
    if (startReadUpdate()) {
      try {
        loadExpiredValue(config, now, isUpdateAccessTime);
      } finally {
        finishReadUpdate();
      }
    }
  }
  
  private void loadExpiredValue(CacheConfig config,
                                long now,
                                boolean isUpdateAccessTime)
  {
    MnodeEntry mnodeEntry = getMnodeEntry();
    
    _loadCount.incrementAndGet();

    CacheEngine engine = config.getEngine();
    
    engine.get(this, config);
    
    mnodeEntry = getMnodeEntry();

    if (! mnodeEntry.isExpired(now)) {
      if (isUpdateAccessTime) {
        mnodeEntry.setLastAccessTime(now);
      }
    }
    else if (loadFromCacheLoader(config, now)) {
      mnodeEntry.setLastAccessTime(now);
    }
    else {
      MnodeEntry nullMnodeValue = new MnodeEntry(0, 0, 0, null,
                                                 0,
                                                 config.getAccessedExpireTimeout(),
                                                 config.getModifiedExpireTimeout(),
                                                 config.getLeaseExpireTimeout(),
                                                 0,
                                                 null,
                                                 now, now,
                                                 true, true);

      compareAndSetEntry(mnodeEntry, nullMnodeValue);
    }
  }
  
  private boolean loadFromCacheLoader(CacheConfig config, long now)
  {
    CacheLoader loader = config.getCacheLoader();

    if (loader != null && config.isReadThrough() && getKey() != null) {
      Object arg = null;
      
      Cache.Entry loaderEntry = loader.load(getKey());
      
      MnodeEntry mnodeEntry = getMnodeEntry();

      if (loaderEntry != null) {
        put(loaderEntry.getValue(), config);

        return true;
      }
    }
    
    return false;
  }

  final void loadLocalEntry()
  {
    long now = CurrentTime.getCurrentTime();

    if (getMnodeEntry().isExpired(now)) {
      forceLoadMnodeValue();
    }
  }

  /**
   * Gets a cache entry
   */
  private MnodeEntry forceLoadMnodeValue()
  {
    HashKey key = getKeyHash();
    MnodeEntry mnodeValue = getMnodeEntry();

    MnodeEntry newMnodeValue
      = _cacheService.getDataBacking().loadLocalEntryValue(key);
    
    if (newMnodeValue != null) {
      compareAndSetEntry(mnodeValue, newMnodeValue);
    }

    return getMnodeEntry();
  }

  /**
   * Gets a cache entry as a stream
   */
  final public boolean getStream(OutputStream os,
                                 CacheConfig config)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();

    MnodeEntry mnodeValue = loadMnodeValue(config); // , false);

    if (mnodeValue == null)
      return false;

    updateAccessTime(mnodeValue, now);

    long valueHash = mnodeValue.getValueHash();

    if (valueHash == 0) {
      return false;
    }

    getLocalDataManager().readData(getKeyHash(), mnodeValue, os, config);
    
    return true;
  }
  
  //
  // put methods
  //

  public MnodeUpdate localUpdate(MnodeUpdate update,
                                 InputStream is)
  {
    MnodeEntry oldEntryValue = getMnodeEntry();
    
    long oldEntryHash = oldEntryValue.getValueHash();
    long oldValueDataId = oldEntryValue.getValueDataId();
    DataItem data = null;
    
    if (update.getValueHash() == 0) {
    }
    else if (oldEntryValue == null
             || oldEntryValue.getVersion() < update.getVersion()
             || (oldEntryValue.getVersion() == update.getVersion()
                 && update.getValueHash() < oldEntryHash)) {
      try {
        if (is != null) {
          data = _cacheService.getLocalDataManager().writeData(update,
                                                               update.getVersion(),
                                                               is);
        }
      } finally {
        IoUtil.close(is);
      }
    }
    
    // XXX: data?
    if (data != null) {
      putLocalValueImpl(update, data.getValueDataId(), null);
    }
    else {
      // XXX: avoid update if no change?
      putLocalValueImpl(update, oldValueDataId, null);
    }
    
    return getMnodeEntry().getRemoteUpdate();
  }

  /**
   * Sets a cache entry
   */
  public final MnodeValue putLocalValue(MnodeUpdate mnodeUpdate,
                                        InputStream is)
  {
    MnodeValue mnodeValue = localUpdate(mnodeUpdate, is);

    _cacheService.notifyPutListeners(getKeyHash(), mnodeUpdate, mnodeValue);
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(MnodeUpdate mnodeUpdate,
                                        long valueDataId,
                                        Object value)
  {
    // long valueHash = mnodeUpdate.getValueHash();
    // long version = mnodeUpdate.getVersion();
    
    MnodeEntry prevMnodeValue = getMnodeEntry();

    MnodeEntry mnodeValue = putLocalValueImpl(mnodeUpdate, valueDataId, value);
    
    if (mnodeValue.getValueHash() != prevMnodeValue.getValueHash()) {
      _cacheService.notifyPutListeners(getKeyHash(), mnodeUpdate, mnodeValue);
    }

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  private final MnodeEntry putLocalValueImpl(MnodeUpdate mnodeUpdate,
                                             long valueDataId,
                                             Object value)
  {
    HashKey key = getKeyHash();
    
    long valueHash = mnodeUpdate.getValueHash();
    long version = mnodeUpdate.getVersion();

    MnodeEntry oldEntryValue = getMnodeEntry();
    MnodeEntry mnodeValue;
    
    int oldLeaseOwner = oldEntryValue.getLeaseOwner();

    do {
      oldEntryValue = loadLocalMnodeValue();
    
      long oldValueHash
        = oldEntryValue != null ? oldEntryValue.getValueHash() : 0;

      long oldVersion = oldEntryValue != null ? oldEntryValue.getVersion() : 0;
      long now = CurrentTime.getCurrentTime();
      
      if (version < oldVersion
          || (version == oldVersion
              && valueHash != 0
              && valueHash <= oldValueHash)) {
        // server/01ns
        // lease ownership handled externally. Only lease owner updates.
        /*
        // lease ownership updates even if value doesn't
        if (oldEntryValue.isLeaseExpired(now)) {
          oldEntryValue.setLeaseOwner(mnodeUpdate.getLeaseOwner(), now);

          // XXX: access time?
          oldEntryValue.setLastAccessTime(now);
        }
        */

        return oldEntryValue;
      }

      // long accessTime = now;
      long accessTime = mnodeUpdate.getLastAccessTime();
      long updateTime = mnodeUpdate.getLastModifiedTime();
      
      int leaseOwner = oldLeaseOwner;
      
      // server/01ns
      /*
      if (oldEntryValue.isLeaseExpired(now))
        leaseOwner = mnodeUpdate.getLeaseOwner();
      else
        leaseOwner = oldLeaseOwner;
        */

      mnodeValue = new MnodeEntry(mnodeUpdate,
                                  valueDataId,
                                  value,
                                  accessTime,
                                  updateTime,
                                  true,
                                  false,
                                  leaseOwner);
    } while (! compareAndSetEntry(oldEntryValue, mnodeValue));

    //MnodeValue newValue
    _cacheService.getDataBacking().putLocalValue(mnodeValue, key,  
                                                 oldEntryValue,
                                                 mnodeUpdate);
    
    if (oldLeaseOwner != mnodeUpdate.getLeaseOwner()) {
      clearLease(oldLeaseOwner);
      _cacheService.getCacheEngine().notifyLease(key, oldLeaseOwner);
    }
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  protected final void put(Object value,
                           CacheConfig config,
                           long now,
                           MnodeEntry mnodeEntry)
  {
    // long idleTimeout = config.getIdleTimeout() * 5L / 4;
    HashKey key = getKeyHash();

    DataItem dataItem
      = _cacheService.getLocalDataManager().writeValue(mnodeEntry, value, config);
    
    long version = getNewVersion(mnodeEntry);

    MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                         dataItem.getLength(),
                                         version,
                                         config);
    
    mnodeEntry = putLocalValueImpl(update,
                                   dataItem.getValueDataId(), value);

    if (mnodeEntry == null) {
      return;
    }
    
    if ((update.getValueHash() != 0) != (dataItem.getValueDataId() != 0)) {
      throw new IllegalStateException(L.l("{0}: update: {1} dataItem: {2}",
                                          this, update, dataItem));
    }

    config.getEngine().put(key, update, dataItem.getValueDataId());
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      // XXX: save facade?
      writer.write(new ExtCacheEntryFacade(this));
    }

    return;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(MnodeEntry mnodeValue)
  {
    MnodeEntry oldEntryValue = getMnodeEntry();

    if (oldEntryValue != null && mnodeValue.compareTo(oldEntryValue) <= 0) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()
    if (! compareAndSetEntry(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue update failed due to timing conflict"
        + " (key=" + getKeyHash() + ")");

      return getMnodeEntry();
    }

    _cacheService.getDataBacking().insertLocalValue(getKeyHash(), mnodeValue,
                                                    oldEntryValue);
    
    return getMnodeEntry();
  }

  /**
   * Loads the value from the local store.
   */
  final MnodeEntry loadLocalMnodeValue()
  {
    HashKey key = getKeyHash();
    MnodeEntry mnodeValue = getMnodeEntry();

    if (mnodeValue.isImplicitNull()) {
      // MnodeEntry newMnodeValue = _cacheSystem.getDataBacking().loadLocalEntryValue(key);
      MnodeEntry newMnodeValue
        = _cacheService.getDataBacking().loadLocalEntryValue(key);

      if (newMnodeValue == null) {
        newMnodeValue = MnodeEntry.NULL;
      }
      
      // cloud/6811
      compareAndSetEntry(mnodeValue, newMnodeValue);

      mnodeValue = getMnodeEntry();
    }

    return mnodeValue;
  }

  void updateAccessTime(MnodeEntry mnodeValue,
                        long now)
  {
    if (mnodeValue != null) {
      long idleTimeout = mnodeValue.getAccessedExpireTimeout();
      long updateTime = mnodeValue.getLastModifiedTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
          && updateTime + mnodeValue.getAccessExpireTimeoutWindow() < now) {
        // XXX:
        mnodeValue.setLastAccessTime(now);

        saveUpdateTime(mnodeValue);
      }
    }
  }

  final protected void updateAccessTime()
  {
    MnodeEntry mnodeValue = getMnodeEntry();
    
    long accessedExpireTimeout = mnodeValue.getAccessedExpireTimeout();
    long accessedTime = mnodeValue.getLastAccessedTime();

    long now = CurrentTime.getCurrentTime();
                       
    if (accessedExpireTimeout < CacheConfig.TIME_INFINITY
        && accessedTime + mnodeValue.getAccessExpireTimeoutWindow() < now) {
      mnodeValue.setLastAccessTime(now);

      saveUpdateTime(mnodeValue);
    }
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry saveUpdateTime(MnodeEntry mnodeValue)
  {
    MnodeEntry newEntryValue = saveLocalUpdateTime(mnodeValue);

    if (newEntryValue.getVersion() != mnodeValue.getVersion()) {
      return newEntryValue;
    }

    _cacheService.getCacheEngine().updateTime(getKeyHash(), mnodeValue);

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry saveLocalUpdateTime(MnodeEntry mnodeValue)
  {
    MnodeEntry oldEntryValue = getMnodeEntry();

    if (oldEntryValue != null
        && mnodeValue.getVersion() < oldEntryValue.getVersion()) {
      return oldEntryValue;
    }
    
    // cloud/60e3
    if (oldEntryValue != null
        && mnodeValue != oldEntryValue
        && mnodeValue.getLastAccessedTime() == oldEntryValue.getLastAccessedTime()
        && mnodeValue.getLastModifiedTime() == oldEntryValue.getLastModifiedTime()) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! compareAndSetEntry(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue updateTime failed due to timing conflict"
               + " (key=" + getKeyHash() + ")");

      return getMnodeEntry();
    }
    
    _cacheService.getDataBacking().saveLocalUpdateTime(getKeyHash(),
                                                       mnodeValue,
                                                       oldEntryValue);

    return getMnodeEntry();
  }
  
  /**
   * Invalidates the entry
   */
  public void clear()
  {
    _mnodeEntry.set(MnodeEntry.NULL);
  }

  /**
   * Conditionally starts an update of a cache item, allowing only a
   * single thread to update the data.
   *
   * @return true if the thread is allowed to update
   */
  private final boolean startReadUpdate()
  {
    return _isReadUpdate.compareAndSet(false, true);
  }

  /**
   * Completes an update of a cache item.
   */
  private final void finishReadUpdate()
  {
    _isReadUpdate.set(false);
  }
  
  private long getNewVersion(MnodeEntry mnodeValue)
  {
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;
    
    return getNewVersion(version);
  }

  //
  // statistics
  //
  

  public int getLoadCount()
  {
    return _loadCount.get();
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[key=" + _key
            + ",keyHash=" + Hex.toHex(_keyHash.getHash(), 0, 4)
            + ",owner=" + _owner
            + "]");
  }
}
