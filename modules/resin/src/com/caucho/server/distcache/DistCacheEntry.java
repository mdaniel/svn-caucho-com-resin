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
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;
import com.caucho.util.IoUtil;
import com.caucho.vfs.StreamSource;

/**
 * An entry in the cache map
 */
public class DistCacheEntry implements ExtCacheEntry {
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
                 TriadOwner owner)
  {
    _cacheService = engine;
    _keyHash = keyHash;
    
    _owner = TriadOwner.getHashOwner(keyHash.getHash());
  }

  /**
   * Returns the key for this entry in the Cache.
   */
  @Override
  public final Object getKey()
  {
    return _key;
  }
  
  public final void setKey(Object key)
  {
    if (_key == null)
      _key = key;
  }

  /**
   * Returns the keyHash
   */
  @Override
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
   * Returns the value of the cache entry.
   */
  @Override
  public Object getValue()
  {
    return getMnodeEntry().getValue();
  }

  /**
   * Returns true if the value is null.
   */
  @Override
  public boolean isValueNull()
  {
    MnodeEntry entry = getMnodeEntry();
    
    return entry == null || entry.isValueNull();
  }

  /**
   * Returns the cacheHash
   */
  public final HashKey getCacheHash()
  {
    return getMnodeEntry().getCacheHashKey();
  }
  
  public final int getUserFlags()
  {
    return getMnodeEntry().getUserFlags();
  }

  /**
   * Peeks the current value without checking the backing store.
   */
  public Object peek()
  {
    MnodeEntry entry = getMnodeEntry();
    
    if (entry != null)
      return entry.getValue();
    else
      return null;
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  public Object get(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    return get(config, now, true);
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  public Object getExact(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    return get(config, now, true);
  }

  /**
   * Returns the value of the cache entry.
   */
  public StreamSource getValueStream()
  {
    try {
      return _cacheService.getValueStream(this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the object for the given key, checking the backing if necessary
   */
  public MnodeEntry loadMnodeValue(CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    return loadMnodeValue(config, now); // , false);
  }

  /**
   * Fills the value with a stream
   */
  public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _cacheService.getStream(this, os, config);
  }

  public long getValueHash(Object value, CacheConfig config)
  {
    if (value == null)
      return 0;

    return _cacheService.calculateValueHash(value, config);
  }
 
 /**
   * Sets a cache entry
   */
  final public void put(Object value,
                        CacheConfig config)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = loadMnodeValue(config, now); // , false);

    put(value, config, now, mnodeValue);
  }

  /**
   * Sets the value by an input stream
   */
  public ExtCacheEntry put(InputStream is,
                           CacheConfig config,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout)
    throws IOException
  {
    return _cacheService.putStream(this, is, config, 
                                   accessedExpireTimeout,
                                   modifiedExpireTimeout, 
                                   0);
  }

  /**
   * Sets the value by an input stream
   */
  public ExtCacheEntry put(InputStream is,
                           CacheConfig config,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout,
                           int flags)
    throws IOException
  {
    return _cacheService.putStream(this, is, config, 
                                   accessedExpireTimeout, 
                                   modifiedExpireTimeout,
                                   flags);
  }

  /**
   * Sets the value by an input stream
   */
  public boolean putIfNew(MnodeUpdate update,
                          InputStream is)
    throws IOException
  {
    return _cacheService.putIfNew(this, update, is);
  }

  /**
   * Sets the current value
   */
  public Object getAndPut(Object value, CacheConfig config)
  {
    return _cacheService.getAndPut(this, value, config);
  }

  /**
   * Sets the current value
   */
  public boolean compareAndPut(long testValue, 
                               Object value, 
                               CacheConfig config)
  {
    return _cacheService.compareAndPut(this, testValue, value, config);
  }

  /**
   * Sets the current value
   */
  public Object getAndReplace(long testValue, 
                              Object value, 
                              CacheConfig config)
  {
    return _cacheService.getAndReplace(this, testValue, value, config);
  }

  /**
   * Remove the value
   */
  public boolean remove(CacheConfig config)
  {
    return _cacheService.remove(this, config);
  }

  /**
   * Remove the value
   */
  public Object getAndRemove(CacheConfig config)
  {
    return _cacheService.getAndRemove(this, config);
  }

  /**
   * Sets the current value.
   */
  public final boolean compareAndSet(MnodeEntry oldMnodeValue,
                                     MnodeEntry mnodeValue)
  {
    if (mnodeValue == null)
      throw new NullPointerException();
    
    return _mnodeEntry.compareAndSet(oldMnodeValue, mnodeValue);
  }

  @Override
  public long getValueHash()
  {
    return getMnodeEntry().getValueHash();
  }

  @Override
  public long getValueLength()
  {
    return getMnodeEntry().getValueLength();
  }
  
  /**
   * Writes the data to a stream.
   */
  @Override
  public boolean readData(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _cacheService.readData(getKeyHash(), getMnodeEntry(), os, config);
  }

  @Override
  public long getAccessedExpireTimeout()
  {
    return getMnodeEntry().getAccessedExpireTimeout();
  }

  @Override
  public long getModifiedExpireTimeout()
  {
    return getMnodeEntry().getModifiedExpireTimeout();
  }
  
  @Override
  public boolean isExpired(long now)
  {
    MnodeEntry entry = getMnodeEntry();
    
    if (entry != null)
      return entry.isExpired(now);
    else
      return false;
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

  @Override
  public long getLeaseTimeout()
  {
    return getMnodeEntry().getLeaseTimeout();
  }

  @Override
  public int getLeaseOwner()
  {
    return getMnodeEntry().getLeaseOwner();
  }

  public void clearLease()
  {
    MnodeEntry mnodeValue = getMnodeEntry();

    if (mnodeValue != null)
      mnodeValue.clearLease();
  }

  public long getCost()
  {
    return 0;
  }

  public long getCreationTime()
  {
    return getMnodeEntry().getCreationTime();
  }

  public long getExpirationTime()
  {
    return getMnodeEntry().getExpirationTime();
  }

  public int getHits()
  {
    return getMnodeEntry().getHits();
  }

  public long getLastAccessedTime()
  {
    return getMnodeEntry().getLastAccessedTime();
  }

  public long getLastModifiedTime()
  {
    return getMnodeEntry().getLastModifiedTime();
  }

  @Override
  public long getVersion()
  {
    return getMnodeEntry().getVersion();
  }

  @Override
  public MnodeUpdate getRemoteUpdate()
  {
    return getMnodeEntry().getRemoteUpdate();
  }

  public boolean isValid()
  {
    return getMnodeEntry().isValid();
  }


  public Object setValue(Object value)
  {
    return getMnodeEntry().setValue(value);
  }
  
  //
  // operations
  //

  private Object get(CacheConfig config,
                     long now,
                     boolean isForceLoad)
  {
    MnodeEntry mnodeValue = loadMnodeValue(config, now, isForceLoad);

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

    updateAccessTime(mnodeValue, now);

    value = _cacheService.getLocalDataManager().readData(getKeyHash(),
                                                         valueHash,
                                                         mnodeValue.getValueDataId(),
                                                         config.getValueSerializer(),
                                                         config);
    
    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data in get for " 
                  + mnodeValue + " " + this);
      _cacheService.remove(this, config);
    }

    mnodeValue.setObjectValue(value);

    return value;
  }

  final public MnodeEntry loadMnodeValue(CacheConfig config,
                                         long now)
  {
    return loadMnodeValue(config, now, false);
  }

  final public MnodeEntry loadMnodeValue(CacheConfig config,
                                        long now,
                                        boolean isForceLoad)
  {
    MnodeEntry mnodeValue = loadLocalMnodeValue();

    if (mnodeValue == null
        || isForceLoad
        || isLocalExpired(config, getKeyHash(), mnodeValue, now)) {
      reloadValue(config, now);
    }

    // server/016q
    updateAccessTime();

    mnodeValue = getMnodeEntry();

    return mnodeValue;
  }

  protected boolean isLocalExpired(CacheConfig config,
                                   HashKey key,
                                   MnodeEntry mnodeValue,
                                   long now)
  {
    return config.getEngine().isLocalExpired(config, key, mnodeValue, now);
  }

  private void reloadValue(CacheConfig config,
                           long now)
  {
    // only one thread may update the expired data
    if (startReadUpdate()) {
      try {
        loadExpiredValue(config, now);
      } finally {
        finishReadUpdate();
      }
    }
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

  private void loadExpiredValue(CacheConfig config,
                                long now)
  {
    MnodeEntry mnodeEntry = getMnodeEntry();
    
    addLoadCount();
    
    CacheEngine engine = config.getEngine();
    
    engine.get(this, config);
    
    mnodeEntry = getMnodeEntry();

    if (mnodeEntry != null && ! mnodeEntry.isExpired(now)) {
      mnodeEntry.setLastAccessTime(now);
    }
    else if (loadFromCacheLoader(config, now)) {
      mnodeEntry.setLastAccessTime(now);
    }
    else {
      MnodeEntry nullMnodeValue = new MnodeEntry(0, 0, 0, 0, null, null,
                                                 0,
                                                 config.getAccessedExpireTimeout(),
                                                 config.getModifiedExpireTimeout(),
                                                 config.getLeaseExpireTimeout(),
                                                 now, now,
                                                 true, true);

      compareAndSet(mnodeEntry, nullMnodeValue);
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
      compareAndSet(mnodeValue, newMnodeValue);
    }

    return getMnodeEntry();
  }

  public MnodeUpdate localUpdate(MnodeUpdate update,
                                 InputStream is)
  {
    MnodeEntry oldEntryValue = getMnodeEntry();
    
    long oldEntryHash = oldEntryValue.getValueHash();
    
    if (update.getValueHash() == 0) {
    }
    else if (oldEntryValue == null
             || (oldEntryValue.getVersion() <= update.getVersion()
                 && update.getValueHash() != oldEntryHash)) {
      try {
        if (is != null) {
          update = _cacheService.getLocalDataManager().writeData(update,
                                                                 update.getVersion(),
                                                                 is);
        }
      } finally {
        IoUtil.close(is);
      }
    }
    
    putLocalValue(update, null);
    
    return getMnodeEntry().getRemoteUpdate();
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry putLocalValue(MnodeUpdate mnodeUpdate,
                                 Object value)
  {
    HashKey key = getKeyHash();
    
    long valueHash = mnodeUpdate.getValueHash();
    long version = mnodeUpdate.getVersion();
    
    MnodeEntry oldEntryValue;
    MnodeEntry mnodeValue;

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
        // lease ownership updates even if value doesn't
        if (oldEntryValue != null) {
          oldEntryValue.setLeaseOwner(mnodeUpdate.getLeaseOwner(), now);

          // XXX: access time?
          oldEntryValue.setLastAccessTime(now);
        }

        return oldEntryValue;
      }

      long accessTime = now;
      long updateTime = accessTime;
      long leaseTimeout = mnodeUpdate.getLeaseTimeout();

      mnodeValue = new MnodeEntry(mnodeUpdate,
                                  value,
                                  leaseTimeout,
                                  accessTime,
                                  updateTime,
                                  true,
                                  false);
    } while (! compareAndSet(oldEntryValue, mnodeValue));

    //MnodeValue newValue
    _cacheService.getDataBacking().putLocalValue(mnodeValue, key,  
                                                 oldEntryValue,
                                                 mnodeUpdate);
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  protected final void put(Object value,
                           CacheConfig config,
                           long now,
                           MnodeEntry mnodeValue)
  {
    // long idleTimeout = config.getIdleTimeout() * 5L / 4;
    HashKey key = getKeyHash();

    MnodeUpdate update
      = _cacheService.getLocalDataManager().writeValue(mnodeValue, value, config);
    
    mnodeValue = putLocalValue(update, value);

    if (mnodeValue == null)
      return;

    config.getEngine().put(key, update);
    
    CacheWriter writer = config.getCacheWriter();
    
    if (writer != null && config.isWriteThrough()) {
      writer.write(this);
    }

    return;
  }

  /**
   * Sets a cache entry
   */
  final MnodeEntry putLocalValue(MnodeEntry mnodeValue)
  {
    MnodeEntry oldEntryValue = getMnodeEntry();

    if (oldEntryValue != null && mnodeValue.compareTo(oldEntryValue) <= 0) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()
    if (! compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue update failed due to timing conflict"
        + " (key=" + getKeyHash() + ")");

      return getMnodeEntry();
    }

    _cacheService.getDataBacking().insertLocalValue(getKeyHash(), mnodeValue,
                                                    oldEntryValue);
    
    return getMnodeEntry();
  }
  
  public boolean compareAndPutLocal(long testValueHash,
                                    MnodeUpdate update,
                                    Object value)
  {
    MnodeEntry mnodeValue = loadLocalMnodeValue();

    long oldValueHash = mnodeValue.getValueHash();
    
    if (oldValueHash != testValueHash) {
      return false;
    }
    
    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;

    mnodeValue = putLocalValue(update, value);
    
    return (mnodeValue != null);
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
      compareAndSet(mnodeValue, newMnodeValue);

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

    if (newEntryValue.getVersion() != mnodeValue.getVersion())
      return newEntryValue;

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
    
    if (oldEntryValue != null
        && mnodeValue.getLastAccessedTime() == oldEntryValue.getLastAccessedTime()
        && mnodeValue.getLastModifiedTime() == oldEntryValue.getLastModifiedTime()) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue updateTime failed due to timing conflict"
               + " (key=" + getKeyHash() + ")");

      return getMnodeEntry();
    }
    
    _cacheService.getDataBacking().saveLocalUpdateTime(getKeyHash(),
                                                       mnodeValue,
                                                       oldEntryValue);

    return getMnodeEntry();
  }

  //
  // statistics
  //
  
  public void addLoadCount()
  {
    _loadCount.incrementAndGet();
  }
  
  @Override
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
