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

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.server.distcache.DataStore.DataItem;
import com.caucho.server.distcache.LocalDataManager.DataItemLocal;
import com.caucho.server.util.CauchoSystem;
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
  private final CacheHandle _cache;
  private final TriadOwner _owner;

  private Object _key;

  private final AtomicBoolean _isReadUpdate = new AtomicBoolean();

  private final AtomicReference<MnodeEntry> _mnodeEntry
    = new AtomicReference<MnodeEntry>(MnodeEntry.NULL);
  
  private final AtomicInteger _loadCount = new AtomicInteger();

  private long _lastLoaderTime;

  DistCacheEntry(CacheStoreManager cacheService,
                 HashKey keyHash,
                 CacheHandle cache,
                 TriadOwner owner)
  {
    _cacheService = cacheService;
    _keyHash = keyHash;
    _cache = cache;
    
    _owner = TriadOwner.getHashOwner(keyHash.getHash());
    
    _mnodeEntry.set(MnodeEntry.createInitialNull(cache.getConfig()));
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
  
  public HashKey getCacheKey()
  {
    return _cache.getCacheKey();
  }
  
  public byte []getCacheKeyHash()
  {
    return _cache.getCacheKeyHash();
  }
  
  public CacheHandle getCache()
  {
    return _cache;
  }
  
  public CacheConfig getConfig()
  {
    return _cache.getConfig();
  }
  
  public CacheEngine getEngine()
  {
    return getConfig().getEngine();
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
  public Object get()
  {
    long now = CurrentTime.getCurrentTime();

    return get(now);
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
  public MnodeEntry loadMnodeValue()
  {
    long now = CurrentTime.getCurrentTime();

    // server/01o9
    return loadMnodeValue(now, false); // , false);
  }

  /**
   * Gets a cache entry as a stream
   */
  final public StreamSource getValueStream()
  {
    MnodeEntry mnodeValue = getMnodeEntry();

    // server/01o9
    // updateAccessTime();

    return getLocalDataManager().createDataSource(mnodeValue.getValueDataId(),
                                                  mnodeValue.getValueDataTime());
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
      long valueDataTime = mnodeEntry.getValueDataTime();
      
      DataStreamSource dataSource
          = getLocalDataManager().createDataSource(valueDataId, valueDataTime);

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
   final public void put(Object value)
   {
     long now = CurrentTime.getCurrentTime();
     
     // server/60a0 - on server '4', need to read update from triad
     // MnodeEntry mnodeValue = loadMnodeValue(now, true); // , false);
     
     MnodeEntry mnodeValue = loadLocalMnodeValue();

     put(value, now, mnodeValue, true);
   }

   /**
     * Sets a cache entry
     */
    final public void putInternal(Object value)
    {
      long now = CurrentTime.getCurrentTime();

      // server/60a0 - on server '4', need to read update from triad
      // MnodeEntry mnodeValue = loadMnodeValue(now, true); // , false);
      
      MnodeEntry mnodeValue = loadLocalMnodeValue();

      put(value, now, mnodeValue, false);
    }

    /**
     * Sets the value by an input stream
      */
    public void put(InputStream is)
      throws IOException
    {
      long now = CurrentTime.getCurrentTime();
      long lastAccessTime = now;
      long lastModifiedTime = now;
      
      CacheConfig config = getConfig();
      
      putStream(is,
                config.getAccessedExpireTimeout(),
                config.getModifiedExpireTimeout(), 
                0,
                lastAccessTime,
                lastModifiedTime,
                0,
                false);
    }

  /**
   * Sets the value by an input stream
    */
  public void putLocal(InputStream is)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    CacheConfig config = getConfig();
    
    putStream(is, 
              config.getAccessedExpireTimeout(),
              config.getModifiedExpireTimeout(), 
              0,
              lastAccessTime,
              lastModifiedTime,
              0,
              true);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    putStream(is,
              accessedExpireTimeout,
              modifiedExpireTimeout, 
              0,
              lastAccessTime,
              lastModifiedTime,
              0,
              false);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout,
                  long lastAccessTime,
                  long lastModifiedTime)
    throws IOException
  {
    putStream(is,
              accessedExpireTimeout,
              modifiedExpireTimeout, 
              0,
              lastAccessTime,
              lastModifiedTime,
              0,
              false);
  }

  /**
   * Sets the value by an input stream
   */
  public void put(InputStream is,
                  long accessedExpireTimeout,
                  long modifiedExpireTimeout,
                  int flags)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    putStream(is,
              accessedExpireTimeout, 
              modifiedExpireTimeout,
              flags,
              lastAccessTime,
              lastModifiedTime,
              0,
              false);
  }

  /**
   * Sets the value by an input stream
    */
  public void putIfNewer(long version, InputStream is)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    long lastAccessTime = now;
    long lastModifiedTime = now;
    
    CacheConfig config = getConfig();
    
    putStream(is,
              config.getAccessedExpireTimeout(),
              config.getModifiedExpireTimeout(), 
              0,
              lastAccessTime,
              lastModifiedTime,
              version,
              false);
  }

  private final void putStream(InputStream is,
                               long accessedExpireTime,
                               long modifiedExpireTime,
                               int userFlags,
                               long lastAccessTime,
                               long lastModifiedTime,
                               long newVersion,
                               boolean isLocal)
    throws IOException
  {
    loadLocalMnodeValue();

    DataItemLocal valueItem = getLocalDataManager().writeData(is);
    
    long valueHash = valueItem.getValueHash();
    long valueDataId = valueItem.getValueDataId();
    long valueDataTime = valueItem.getValueDataTime();
    
    long valueLength = valueItem.getLength();
    
    MnodeEntry mnodeEntry = getMnodeEntry();
    
    if (newVersion <= 0) {
      newVersion = getNewVersion(getMnodeEntry());
    }
    else if (newVersion < mnodeEntry.getVersion()) {
      log.finer(this + " put with obsolete version"
          + " current=0x" + Long.toHexString(mnodeEntry.getVersion())
          + " new=0x" + Long.toHexString(newVersion));
    }
    
    CacheConfig config = getConfig();
    
    long flags = config.getFlags() | ((long) userFlags) << 32;
    
    if (accessedExpireTime < 0)
      accessedExpireTime = config.getAccessedExpireTimeout();
    
    if (modifiedExpireTime < 0)
      modifiedExpireTime = config.getModifiedExpireTimeout();
    
    long now = CurrentTime.getCurrentTime();
    long delta = now - mnodeEntry.getLastAccessedTime();
    
    if (valueHash == mnodeEntry.getValueHash()
        && flags == mnodeEntry.getFlags()
        && delta < modifiedExpireTime
        && delta < accessedExpireTime) {
      // server/01nx
      
      return;
    }
    
    int leaseOwner = getMnodeEntry().getLeaseOwner();
    long leaseExpireTimeout = config.getLeaseExpireTimeout();
    
    MnodeUpdate mnodeUpdate = new MnodeUpdate(valueHash,
                                              valueLength,
                                              newVersion,
                                              flags,
                                              accessedExpireTime,
                                              modifiedExpireTime,
                                              leaseExpireTimeout,
                                              leaseOwner,
                                              lastAccessTime,
                                              lastModifiedTime);

    // add 25% window for update efficiency
    // idleTimeout = idleTimeout * 5L / 4;
    
    putLocalValue(mnodeUpdate, valueDataId, valueDataTime, null);
    config.getEngine().put(getKeyHash(), getCacheKey(), 
                           mnodeUpdate, 
                           valueDataId, valueDataTime);
    
    CacheWriterExt writer = config.getCacheWriterExt();

    if (! isLocal && writer != null && config.isWriteThrough()) {
      // loadValue(config);
      
      writer.write(this);
    }
  }

  /**
   * Sets a cache entry
   */
  public final boolean remove()
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
    
    CacheConfig config = getConfig();
    
    MnodeUpdate mnodeUpdate = MnodeUpdate.createNull(newVersion, config);

    /*
    if (mnodeEntry != null)
      mnodeUpdate = MnodeUpdate.createNull(newVersion, mnodeEntry);
    else
      mnodeUpdate = MnodeUpdate.createNull(newVersion, config);
      */

    putLocalValueImpl(mnodeUpdate, 0, 0, null);
    
    config.getEngine().remove(key, getCacheKey(), mnodeUpdate);
    
    CacheWriterExt writer = config.getCacheWriterExt();
    
    if (writer != null && config.isWriteThrough()) {
      writer.delete(this);
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
                          InputStream is)
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
      getEngine().put(getKeyHash(), getCacheKey(),
                      update, 
                      entry.getValueDataId(),
                      entry.getValueDataTime());
    }

    return newValue.getValueHash() == update.getValueHash();
  }
  

  //
  // compare and put
  //
  
  public boolean compareAndPut(long testValue,
                               Object value)
  {
    MnodeEntry oldMnodeEntry = getMnodeEntry();
    
    CacheConfig config = getConfig();
    
    DataItemLocal dataItem
      = getLocalDataManager().writeValue(getMnodeEntry(), value, config);
    long valueDataId = dataItem.getValueDataId();
    long valueDataTime = dataItem.getValueDataTime();
    long newVersion = getNewVersion(oldMnodeEntry);
    
    // long oldValueDataId = oldMnodeEntry.getValueDataId();
    
    
    try {
      MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                           dataItem.getLength(),
                                           newVersion,
                                           config);
//                                           oldMnodeEntry);
      
      return config.getEngine().compareAndPut(this, testValue,
                                              update, 
                                              valueDataId,
                                              valueDataTime);
    } finally {
      MnodeValue newMnodeValue = getMnodeEntry();
      
      if (newMnodeValue.getValueHash() != dataItem.getValueHash()) {
        getLocalDataManager().removeData(valueDataId, valueDataTime);
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
    // long version = getNewVersion(getMnodeEntry());
    long version = update.getVersion();
    

    // cloud/60r0
    DataItemLocal dataItem = getLocalDataManager().writeData(update, version, source);
    
    long valueDataId = dataItem.getValueDataId();
    long valueDataTime = dataItem.getValueDataTime();
    
    Object value = null;
    
    return compareAndPutLocal(testValue, update, valueDataId, valueDataTime, value);
  }
  
  public boolean compareAndPutLocal(long testValueHash,
                                    MnodeUpdate update,
                                    long valueDataId,
                                    long valueDataTime,
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

    mnodeValue = putLocalValueImpl(update, valueDataId, valueDataTime, value);
    
    return (mnodeValue != null);
  }

  protected boolean compareAndPut(DistCacheEntry entry,
                                  long testValue,
                                  MnodeUpdate mnodeUpdate,
                                  long valueDataId,
                                  long valueDataTime,
                                  Object value,
                                  CacheConfig config)
  {
    CacheEngine engine = config.getEngine();

    return engine.compareAndPut(entry, testValue, mnodeUpdate, valueDataId, valueDataTime);
  }
  
  //
  // get and put
  //

  /**
   * Remove the value
   */
  public Object getAndRemove()
  {
    return getAndPut(null);
  }

  public Object getAndReplace(long testValue,
                              Object value)
  {
    long prevDataId = getMnodeEntry().getValueDataId();
    long prevDataTime = getMnodeEntry().getValueDataTime();
    
    if (compareAndPut(testValue, value)) {
      long result = -1;
      
      CacheConfig config = getConfig();
      
      return getLocalDataManager().readData(getKeyHash(),
                                            result,
                                            prevDataId,
                                            prevDataTime,
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
  public Object getAndPut(Object value)
  {
    long now = CurrentTime.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeEntry mnodeValue = loadMnodeValue(now, true); // , false);

    return getAndPut(value, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected final Object getAndPut(Object value,
                                   long now,
                                   MnodeEntry mnodeValue)
  {
    CacheConfig config = getConfig();
    
    DataItemLocal dataItem
      = getLocalDataManager().writeValue(mnodeValue, value, config);
    
    long version = getNewVersion(getMnodeEntry());
    
    MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                         dataItem.getLength(),
                                         version,
                                         config);
    // int leaseOwner = mnodeValue.getLeaseOwner();

    InputStream is = getAndPut(update,
                               dataItem.getValueDataId(),
                               dataItem.getValueDataTime());

    if (is == null)
      return null;
    
    Object oldValue = getLocalDataManager().decodeValue(is, config.getValueSerializer());

    return oldValue;
  }
  
  public DataItem getAndPutLocal(MnodeUpdate mnodeUpdate,
                             StreamSource source)
  {
    long oldValueDataId = getMnodeEntry().getValueDataId();
    long oldValueDataTime = getMnodeEntry().getValueDataTime();
    
    DataItemLocal dataItem = getLocalDataManager().writeData(source);
    
    long valueDataId = dataItem.getValueDataId();
    long valueDataTime = dataItem.getValueDataTime();
    
    Object value = null;

    putLocalValue(mnodeUpdate, valueDataId, valueDataTime, value);

    return new DataItem(oldValueDataId, oldValueDataTime);
  }
  
  public long getAndPutLocal(DistCacheEntry entry,
                             MnodeUpdate mnodeUpdate,
                             long valueDataId,
                             long valueDataTime,
                             Object value)
  {
    long oldValueHash = entry.getMnodeEntry().getValueHash();

    entry.putLocalValue(mnodeUpdate, valueDataId, valueDataTime, value);

    return oldValueHash;
  }

  /**
   * Sets a cache entry
   */
  private InputStream getAndPut(MnodeUpdate mnodeValue,
                                long valueDataId,
                                long valueDataTime)
  {
    return getEngine().getAndPut(this, mnodeValue, valueDataId, valueDataTime);
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
  
  public void load()
  {
    long now = CurrentTime.getCurrentTime();
    
    loadMnodeValue(now, true);
  }
  
  public void load(DistCacheLoadListener listener)
  {
    loadMnodeValue(listener);
  }

  private Object get(long now)
  {
    MnodeEntry mnodeEntry = loadMnodeValue(now, true);

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
    // asdf
    
    CacheConfig config = getConfig();

    value = _cacheService.getLocalDataManager().readData(getKeyHash(),
                                                         valueHash,
                                                         mnodeEntry.getValueDataId(),
                                                         mnodeEntry.getValueDataTime(),
                                                         config.getValueSerializer(),
                                                         config);
    
    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data in get for " 
                  + mnodeEntry + " " + this);
      remove();
    }

    if (! config.isStoreByValue() || isImmutable(value)) {
      mnodeEntry.setObjectValue(value);
    }

    return value;
  }
  
  private boolean isImmutable(Object value)
  {
    return value instanceof String;
  }
  
  public long getValueHash()
  {
    MnodeEntry entry = getMnodeEntry();

    return entry.getValueHash();
  }
  
  public long getVersion()
  {
    MnodeEntry entry = getMnodeEntry();

    return entry.getVersion();
  }

  public Object getValue()
  {
    long now = CurrentTime.getCurrentTime();
    
    MnodeEntry entry = getMnodeEntry();
    
    return getValue(entry, now);
  }
  
  private Object getValue(MnodeEntry mnodeEntry,
                          long now)
    {
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
    
    CacheConfig config = getConfig();

    value = _cacheService.getLocalDataManager().readData(getKeyHash(),
                                                         valueHash,
                                                         mnodeEntry.getValueDataId(),
                                                         mnodeEntry.getValueDataTime(),
                                                         config.getValueSerializer(),
                                                         config);
    
    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data in get for " 
                  + mnodeEntry + " " + this);
      remove();
    }

    mnodeEntry.setObjectValue(value);

    return value;
  }

  final private MnodeEntry loadMnodeValue(long now,
                                          boolean isUpdateAccessTime)
  {
    MnodeEntry mnodeEntry = loadLocalMnodeValue();
    
    CacheConfig config = getConfig();
    int server = config.getServerIndex();

    if (mnodeEntry == null 
        || mnodeEntry.isLocalExpired(server, now, config)
        || ! isReadThroughLocalValid(now)) {
      reloadValue(now, isUpdateAccessTime);
    }
    
    // server/016q
    if (isUpdateAccessTime) {
      updateAccessTime();
    }

    mnodeEntry = getMnodeEntry();

    return mnodeEntry;
  }

  final private void loadMnodeValue(DistCacheLoadListener listener)
  {
    MnodeEntry mnodeEntry = loadLocalMnodeValue();
    
    CacheConfig config = getConfig();
    
    int server = config.getServerIndex();
    
    long now = CurrentTime.getCurrentTime();
    
    if (mnodeEntry == null 
        || mnodeEntry.isLocalExpired(server, now, config)) {
      DistCacheLoadTask task = new DistCacheLoadTask(this, listener);
      
      _cacheService.schedule(task);
    }
    else {
      updateAccessTime();
      
      listener.onLoad(this);
    }
  }

  void reloadValue(long now,
                   boolean isUpdateAccessTime)
  {
    // only one thread may update the expired data
    if (startReadUpdate()) {
      try {
        loadExpiredValue(now, isUpdateAccessTime);
      } finally {
        finishReadUpdate();
      }
    }
  }
  
  private void loadExpiredValue(long now,
                                boolean isUpdateAccessTime)
  {
    MnodeEntry mnodeEntry = getMnodeEntry();
    
    _loadCount.incrementAndGet();
    
    CacheConfig config = getConfig();

    CacheEngine engine = config.getEngine();
    
    engine.get(this);
    
    mnodeEntry = getMnodeEntry();

    if (! mnodeEntry.isExpired(now) && isReadThroughLocalValid(now)) {
      if (isUpdateAccessTime) {
        mnodeEntry.setLastAccessTime(now);
      }
    }
    else if (loadFromCacheLoader(now)) {
      _lastLoaderTime = now; 
      mnodeEntry.setLastAccessTime(now);
    }
    else {
      MnodeEntry nullMnodeValue = new MnodeEntry(0, 0, mnodeEntry.getVersion(),
                                                 0,
                                                 config.getAccessedExpireTimeout(),
                                                 config.getModifiedExpireTimeout(),
                                                 config.getLeaseExpireTimeout(),
                                                 0, 0, null,
                                                 now, now,
                                                 true, true);

      compareAndSetEntry(mnodeEntry, nullMnodeValue);
    }
  }
  
  private boolean isReadThroughLocalValid(long now)
  {
    CacheConfig config = getConfig();
    
    if (! config.isReadThrough()) {
      return true;
    }
    else  {
      return (now - _lastLoaderTime < config.getReadThroughExpireTimeout());
    }
  }
  
  private boolean loadFromCacheLoader(long now)
  {
    CacheConfig config = getConfig();
    CacheLoaderExt loader = config.getCacheLoaderExt();

    if (loader != null && config.isReadThrough() && getKey() != null) {
      DistCacheEntryLoadCallback cb = new DistCacheEntryLoadCallback();

      loader.load(this, cb);

      return cb.get();
    }
    
    return false;
  }

  final void loadLocalEntry()
  {
    long now = CurrentTime.getCurrentTime();

    if (getMnodeEntry().isExpired(now) || ! isReadThroughLocalValid(now)) {
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
  final public boolean getStream(OutputStream os)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    
    MnodeEntry mnodeValue = loadMnodeValue(); // , false);

    if (mnodeValue == null) {
      return false;
    }

    updateAccessTime(mnodeValue, now);

    long valueHash = mnodeValue.getValueHash();

    if (valueHash == 0) {
      return false;
    }

    CacheConfig config = getConfig();

    getLocalDataManager().readData(getKeyHash(), mnodeValue, os, config);
    
    return true;
  }

  /**
   * Gets a cache entry as a stream
   */
  final public boolean getLocalStream(OutputStream os)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();

    MnodeEntry mnodeValue = getMnodeEntry();

    if (mnodeValue == null)
      return false;

    long valueHash = mnodeValue.getValueHash();

    if (valueHash == 0) {
      return false;
    }
    
    CacheConfig config = getConfig();

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
    long oldValueDataTime = oldEntryValue.getValueDataTime();
    DataItemLocal data = null;
    
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
    
    if (data != null) {
      putLocalValueImpl(update, data.getValueDataId(), data.getValueDataTime(), null);
    }
    else {
    //  XXX: avoid update if no change?
      putLocalValueImpl(update, oldValueDataId, oldValueDataTime, null);
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

    _cacheService.notifyPutListeners(getKeyHash(), getCacheKey(),
                                     mnodeUpdate, mnodeValue);
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(MnodeUpdate mnodeUpdate,
                                        DataItem valueData,
                                        Object value)
  {
    long valueDataId = 0;
    long valueDataTime = 0;
    
    if (valueData != null) {
      valueDataId = valueData.getId();
      valueDataTime = valueData.getTime();
    }

    return putLocalValue(mnodeUpdate, valueDataId, valueDataTime, value);
  }

  /**
   * Sets a cache entry
   */
  public final MnodeEntry putLocalValue(MnodeUpdate mnodeUpdate,
                                        long valueDataId,
                                        long valueDataTime,
                                        Object value)
  {
    // long valueHash = mnodeUpdate.getValueHash();
    // long version = mnodeUpdate.getVersion();
    
    MnodeEntry prevMnodeValue = getMnodeEntry();

    MnodeEntry mnodeValue = putLocalValueImpl(mnodeUpdate, 
                                              valueDataId,
                                              valueDataTime,
                                              value);
    if (mnodeValue.getValueHash() != prevMnodeValue.getValueHash()) {
      _cacheService.notifyPutListeners(getKeyHash(), getCacheKey(),
                                       mnodeUpdate, mnodeValue);
    }

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  private final MnodeEntry putLocalValueImpl(MnodeUpdate mnodeUpdate,
                                             long valueDataId,
                                             long valueDataTime,
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
      
      Object saveValue = null;
      if (! getConfig().isStoreByValue() || isImmutable(value)) {
        saveValue = value;
      }


      mnodeValue = new MnodeEntry(mnodeUpdate,
                                  valueDataId,
                                  valueDataTime,
                                  saveValue,
                                  accessTime,
                                  updateTime,
                                  true,
                                  false,
                                  leaseOwner);
    } while (! compareAndSetEntry(oldEntryValue, mnodeValue));

    //MnodeValue newValue
    _cacheService.getDataBacking().putLocalValue(mnodeValue, key,
                                                 getCacheKey(),
                                                 oldEntryValue,
                                                 mnodeUpdate);
    
    if (oldLeaseOwner != mnodeUpdate.getLeaseOwner()) {
      clearLease(oldLeaseOwner);
      _cacheService.getCacheEngine().notifyLease(key, 
                                                 getCacheKey(),
                                                 oldLeaseOwner);
    }
    
    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  protected final void put(Object value,
                           long now,
                           MnodeEntry mnodeEntry,
                           boolean isWriteThrough)
  {
    // long idleTimeout = config.getIdleTimeout() * 5L / 4;
    HashKey key = getKeyHash();
    
    CacheConfig config = getConfig();

    DataItemLocal dataItem
      = _cacheService.getLocalDataManager().writeValue(mnodeEntry, value, config);
    
    long version = getNewVersion(mnodeEntry);

    MnodeUpdate update = new MnodeUpdate(dataItem.getValueHash(),
                                         dataItem.getLength(),
                                         version,
                                         config);
    
    mnodeEntry = putLocalValueImpl(update,
                                   dataItem.getValueDataId(), 
                                   dataItem.getValueDataTime(), 
                                   value);

    if (mnodeEntry == null) {
      return;
    }
    
    if ((update.getValueHash() != 0) != (dataItem.getValueDataId() != 0)) {
      throw new IllegalStateException(L.l("{0}: update: {1} dataItem: {2}",
                                          this, update, dataItem));
    }

    config.getEngine().put(key, getCacheKey(),
                           update, 
                           dataItem.getValueDataId(),
                           dataItem.getValueDataTime());
    
    CacheWriterExt writer = config.getCacheWriterExt();
    
    if (isWriteThrough && writer != null && config.isWriteThrough()) {
      // XXX: save facade?
      // writer.write(new ExtCacheEntryFacade(this));
      
      writer.write(this);
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

    _cacheService.getDataBacking().insertLocalValue(getKeyHash(),
                                                    getCacheKey(),
                                                    mnodeValue,
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
  
  public void updateModifiedTime()
  {
    MnodeEntry mnodeValue = getMnodeEntry();
    long now = CurrentTime.getCurrentTime();
    
    MnodeEntry newMnodeValue = mnodeValue.updateModifiedTime(now);
    
    compareAndSetEntry(mnodeValue, newMnodeValue);
  }

  void updateAccessTime(MnodeEntry mnodeValue,
                        long now)
  {
    if (mnodeValue != null) {
      long idleTimeout = mnodeValue.getAccessedExpireTimeout();
      long updateTime = mnodeValue.getLastModifiedTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
          && updateTime + mnodeValue.getAccessExpireTimeoutWindow() < now) {
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

    _cacheService.getCacheEngine().updateTime(getKeyHash(), 
                                              getCacheKey(),
                                              mnodeValue);
    
    CacheWriterExt writer = getConfig().getCacheWriterExt();

    if (writer != null && getConfig().isWriteThrough()) {
      writer.updateTime(this);
    }

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
