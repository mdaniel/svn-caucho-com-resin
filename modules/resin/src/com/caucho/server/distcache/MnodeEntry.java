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

import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;

import java.lang.ref.SoftReference;
import java.sql.Blob;

/**
 * An entry in the cache map
 */
public final class MnodeEntry extends MnodeValue implements ExtCacheEntry {
  public static final MnodeEntry NULL
    = new MnodeEntry(null, 0, 0, null, null, 0, 0, 0, 0, 0, 0, false, true);
  
  public static final HashKey NULL_KEY = new HashKey(new byte[32]);
  public static final HashKey ANY_KEY = createAnyKey(32);
  
  private final long _leaseTimeout;

  private final boolean _isServerVersionValid;

  private final boolean _isImplicitNull;
  
  private final long _lastModifiedTime;
  
  private volatile long _lastAccessTime;
  
  private int _leaseOwner = -1;
  private long _leaseExpireTime;
  
  private long _lastRemoteAccessTime;

  private int _hits = 0;

  private SoftReference<Object> _valueRef;
  private transient Blob _blob;

  public MnodeEntry(HashKey valueHash,
                    long valueLength,
                    long version,
                    Object value,
                    HashKey cacheHash,
                    long flags,
                    long accessedExpireTimeout,
                    long modifiedExpireTimeout,
                    long leaseTimeout,
                    long lastAccessTime,
                    long lastUpdateTime,
                    boolean isServerVersionValid,
                    boolean isImplicitNull)
  {
    super(HashKey.getHash(valueHash), valueLength, version,
          HashKey.getHash(cacheHash),
          flags,
          accessedExpireTimeout, modifiedExpireTimeout);
    
    _leaseTimeout = leaseTimeout;
    _lastRemoteAccessTime = lastAccessTime;
    _lastModifiedTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _isImplicitNull = isImplicitNull;
    _isServerVersionValid = isServerVersionValid;

    if (value != null)
      _valueRef = new SoftReference<Object>(value);
  }

  public MnodeEntry(MnodeValue mnodeValue,
                    Object value,
                    long leaseTimeout,
                    long lastAccessTime,
                    long lastUpdateTime,
                    boolean isServerVersionValid,
                    boolean isImplicitNull)
  {
    super(mnodeValue);
    
    _leaseTimeout = leaseTimeout;
    _lastRemoteAccessTime = lastAccessTime;
    _lastModifiedTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _isImplicitNull = isImplicitNull;
    _isServerVersionValid = isServerVersionValid;

    if (value != null)
      _valueRef = new SoftReference<Object>(value);
  }

  public MnodeEntry(MnodeEntry oldMnodeValue,
                    long accessTimeout,
                    long lastUpdateTime)
  {
    super(oldMnodeValue.getValueHash(),
          oldMnodeValue.getValueLength(),
          oldMnodeValue.getVersion(),
          oldMnodeValue.getCacheHash(),
          oldMnodeValue.getFlags(),
          accessTimeout,
          oldMnodeValue.getModifiedExpireTimeout());
    
    _leaseTimeout = oldMnodeValue.getLeaseTimeout();
    
    _lastRemoteAccessTime = lastUpdateTime;
    _lastModifiedTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _leaseExpireTime = oldMnodeValue._leaseExpireTime;
    _leaseOwner = oldMnodeValue._leaseOwner;

    _isImplicitNull = oldMnodeValue.isImplicitNull();
    _isServerVersionValid = oldMnodeValue.isServerVersionValid();

    Object value = oldMnodeValue.getValue();
    
    if (value != null)
      _valueRef = new SoftReference<Object>(value);
  }

  @Override
  public HashKey getKeyHash()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  /**
   * Returns the last access time.
   */
  public long getLastAccessedTime()
  {
    return _lastAccessTime;
  }

  /**
   * Sets the last access time.
   */
  public void setLastAccessTime(long accessTime)
  {
    _lastAccessTime = accessTime;
  }

  /**
   * Returns the last remote access time.
   */
  public long getLastRemoteAccessTime()
  {
    return _lastRemoteAccessTime;
  }

  /**
   * Sets the last remote access time.
   */
  public void setLastRemoteAccessTime(long accessTime)
  {
    _lastRemoteAccessTime = accessTime;
  }

  /**
   * Returns the last update time.
   */
  @Override
  public long getLastModifiedTime()
  {
    return _lastModifiedTime;
  }

  /**
   * Returns the expiration time
   */
  public final long getExpirationTime()
  {
    return _lastModifiedTime + getModifiedExpireTimeout();
  }

  public final boolean isLocalExpired(int serverIndex, 
                                      long now,
                                      long localExpireTimeout)
  {
    if (! _isServerVersionValid)
      return true;
    else if (now <= _lastAccessTime + localExpireTimeout)
      return false;
    else if (_leaseOwner == serverIndex && now <= _leaseExpireTime)
      return false;
    else
      return true;
  }

  public final boolean isLeaseExpired(long now)
  {
    return _leaseExpireTime <= now;
  }

  /**
   * Returns true if the local (unchecked) expire time.
   */
  public final boolean isLocalExpired(long now, CacheConfig config)
  {
    if (isExpired(now))
      return true;
    else if (_lastAccessTime + config.getLocalExpireTimeout() <= now)
      return true;
    else
      return false;
  }
  
  /**
   * Returns true is the entry has expired for being idle or having
   * expired.
   */
  @Override
  public final boolean isExpired(long now)
  {
    return isIdleExpired(now) || isValueExpired(now);
  }

   /**
   * Returns true if the value of the entry has expired.
   */
  public final boolean isValueExpired(long now)
  {
    return _lastModifiedTime + getModifiedExpireTimeout() < now;
  }

  /**
   * Returns true is the entry has remained idle  too long.
   */
  public final boolean isIdleExpired(long now)
  {
    return _lastAccessTime + getAccessedExpireTimeout() < now;
  }

  /**
   * Returns the lease owner
   */
  @Override
  public final int getLeaseOwner()
  {
    return _leaseOwner;
  }

  /**
   * Sets the owner
   */
  public final void setLeaseOwner(int leaseOwner, long now)
  {
    if (leaseOwner > 2) {
      _leaseOwner = leaseOwner;

      _leaseExpireTime = now + _leaseTimeout;
    }
    else {
      _leaseOwner = -1;

      _leaseExpireTime = 0;
      
      // server/0b10
      _lastAccessTime = 0;
    }
  }

  /**
   * Sets the owner
   */
  public final void clearLease()
  {
    _leaseOwner = -1;

    _leaseExpireTime = 0;
  }

  /**
   * Returns the idle window to avoid too many updates
   */
  public long getAccessExpireTimeoutWindow()
  {
    long window = getAccessedExpireTimeout() / 4;
    long windowMax = 15 * 60 * 1000L;

    if (window < windowMax)
      return window;
    else
      return windowMax;
  }

  /**
   * Returns the timeout for a lease of the cache entry
   */
  @Override
  public long getLeaseTimeout()
  {
    return _leaseTimeout;
  }

  /**
   * Sets the deserialized value for the entry.
   */
  public final void setObjectValue(Object value)
  {
    if (value != null && (_valueRef == null || _valueRef.get() == null))
      _valueRef = new SoftReference<Object>(value);
  }

  /**
   * Returns true if the value is null
   */
  @Override
  public boolean isValueNull()
  {
    return getValueHash() == null;
  }

  /**
   * Returns the deserialized value for the entry.
   */
  @Override
  public final Object getValue()
  {
    SoftReference<Object> valueRef = _valueRef;

    if (valueRef != null) {
      _hits++;
      return valueRef.get();
    }
    else
      return null;
  }

  public Blob getBlob()
  {
    return _blob;
  }
  
  public void setBlob(Blob blob)
  {
    _blob = blob;
  }

  @Override
  public HashKey getValueHashKey()
  {
    return HashKey.create(getValueHash());
  }

  public HashKey getCacheHashKey()
  {
    return HashKey.create(getCacheHash());
  }

  /**
   * Returns true if the server version (startup count) matches
   * the database.
   */
  public boolean isServerVersionValid()
  {
    return _isServerVersionValid;
  }

  /**
   * If the null value is due to a missing item in the database.
   */
  public boolean isImplicitNull()
  {
    return _isImplicitNull;
  }
  
  public boolean isUnloadedValue()
  {
    return this == NULL;
  }

  /**
   * Compares values
   */
  public int compareTo(MnodeEntry mnode)
  {
    if (getVersion() < mnode.getVersion())
      return -1;
    else if (mnode.getVersion() < getVersion())
      return 1;
    else if (getValueHashKey() == null)
      return -1;
    else
      return getValueHashKey().compareTo(mnode.getValueHashKey());
  }
  
  //
  // statistics
  //
  
  @Override
  public int getLoadCount()
  {
    return 0;
  }

  //
  // jcache stubs
  //

  /**
   * Implements a method required by the interface that should never be
   * called>
   */
  @Override
  public Object getKey()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
   /**
   * Implements a method required by the interface that should never be
   * called>
   */
  public Object setValue(Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getCreationTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public boolean isValid()
  {
    return (! isExpired(Alarm.getCurrentTime()));
  }

  /*
  @Override
  public long getCost()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */
  
  public int getHits()
  {
    return _hits;
  }
  
  private static HashKey createAnyKey(int len)
  {
    byte []value = new byte[len];
    
    for (int i = 0; i < len; i++) {
      value[i] = (byte) 0xff;
    }
    
    return new HashKey(value); 
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[value=" + Hex.toHex(getValueHash(), 0, 4)
            + ",flags=0x" + Long.toHexString(getFlags())
            + ",version=" + getVersion()
            + ",lease=" + _leaseOwner
            + "]");
  }
}
