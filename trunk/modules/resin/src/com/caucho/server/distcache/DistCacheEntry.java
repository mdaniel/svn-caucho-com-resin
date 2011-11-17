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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;

/**
 * An entry in the cache map
 */
public class DistCacheEntry implements ExtCacheEntry {
  private final CacheStoreManager _cacheService;
  private final HashKey _keyHash;

  private final TriadOwner _owner;

  private Object _key;

  private final AtomicBoolean _isReadUpdate = new AtomicBoolean();

  private final AtomicReference<MnodeEntry> _mnodeEntry
    = new AtomicReference<MnodeEntry>(MnodeEntry.NULL);
  
  private final AtomicInteger _loadCount = new AtomicInteger();

  public DistCacheEntry(CacheStoreManager engine,
                        Object key,
                        HashKey keyHash,
                        TriadOwner owner)
  {
    _cacheService = engine;
    _key = key;
    _keyHash = keyHash;
    _owner = owner;
  }

  public DistCacheEntry(CacheStoreManager engine,
                        Object key,
                        HashKey keyHash,
                        TriadOwner owner,
                        CacheConfig config)
  {
    _cacheService = engine;
    _key = key;
    _keyHash = keyHash;
    _owner = owner;
  }

  /**
   * Returns the key for this entry in the Cache.
   */
  @Override
  public final Object getKey()
  {
    return _key;
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
    return getMnodeEntry().isValueNull();
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
    long now = Alarm.getCurrentTime();

    return _cacheService.get(this, config, now);
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  public Object getExact(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    return _cacheService.getExact(this, config, now);
  }

  /**
   * Returns the object for the given key, checking the backing if necessary
   */
  public MnodeEntry getMnodeValue(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    return _cacheService.getMnodeValue(this, config, now); // , false);
  }

  /**
   * Fills the value with a stream
   */
  public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _cacheService.getStream(this, os, config);
  }

  public HashKey getValueHash(Object value, CacheConfig config)
  {
    if (value == null)
      return null;
    else
      return _cacheService.getValueHash(value, config).getValue();
  }
 
  /**
   * Sets the current value
   */
  public void put(Object value, CacheConfig config)
  {
    _cacheService.put(this, value, config);
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
   * Sets the current value
   */
  public Object getAndPut(Object value, CacheConfig config)
  {
    return _cacheService.getAndPut(this, value, config);
  }

  /**
   * Sets the current value
   */
  public HashKey compareAndPut(HashKey testValue, 
                               Object value, 
                               CacheConfig config)
  {
    return _cacheService.compareAndPut(this, testValue, value, config);
  }

  /**
   * Sets the current value
   */
  public boolean compareAndPut(long version,
                               HashKey value,
                               long valueLength,
                               CacheConfig config)
  {
    return _cacheService.compareAndPut(this, version, value, valueLength, config);
  }

  /**
   * Remove the value
   */
  public boolean remove(CacheConfig config)
  {
    return _cacheService.remove(this, config);
  }

  /**
   * Conditionally starts an update of a cache item, allowing only a
   * single thread to update the data.
   *
   * @return true if the thread is allowed to update
   */
  public final boolean startReadUpdate()
  {
    return _isReadUpdate.compareAndSet(false, true);
  }

  /**
   * Completes an update of a cache item.
   */
  public final void finishReadUpdate()
  {
    _isReadUpdate.set(false);
  }

  /**
   * Sets the current value.
   */
  public final boolean compareAndSet(MnodeEntry oldMnodeValue,
                                     MnodeEntry mnodeValue)
  {
    return _mnodeEntry.compareAndSet(oldMnodeValue, mnodeValue);
  }

  @Override
  public HashKey getValueHashKey()
  {
    MnodeEntry entry = getMnodeEntry();
    
    if (entry != null)
      return entry.getValueHashKey();
    else
      return null;
  }

  public byte []getValueHashArray()
  {
    return getMnodeEntry().getValueHash();
  }

  @Override
  public long getValueLength()
  {
    return getMnodeEntry().getValueLength();
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

  public long getVersion()
  {
    MnodeEntry entry = getMnodeEntry();
    
    if (entry != null)
      return entry.getVersion();
    else
      return 0;
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
