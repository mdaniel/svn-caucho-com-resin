/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.cluster.ExtCacheEntry;
import com.caucho.util.Alarm;

import java.lang.ref.SoftReference;

/**
 * An entry in the cache map
 */
public final class CacheMapEntry implements ExtCacheEntry {
  public static final CacheMapEntry NULL
    = new CacheMapEntry(null, null, 0, 0, 0, 0, 0, 0, 0, 0, false);
  
  private final HashKey _valueHash;
  private final int _flags;
  private final long _version;
  
  private final long _expireTimeout;
  private final long _idleTimeout;
  private final long _leaseTimeout;
  private final long _localReadTimeout;

  private final long _lastUpdateTime;

  private final boolean _isServerVersionValid;
  
  private volatile long _lastAccessTime;
  
  private int _leaseOwner = -1;
  private long _leaseExpireTime;
  
  private long _lastRemoteAccessTime;
  
  private SoftReference _valueRef;

  public CacheMapEntry(HashKey valueHash,
		       Object value,
		       int flags,
		       long version,
		       long expireTimeout,
		       long idleTimeout,
		       long leaseTimeout,
		       long localReadTimeout,
		       long lastAccessTime,
		       long lastUpdateTime,
		       boolean isServerVersionValid)
  {
    _valueHash = valueHash;
    _flags = flags;
    _version = version;
    
    _expireTimeout = expireTimeout;
    _idleTimeout = idleTimeout;
    _leaseTimeout = leaseTimeout;
    _localReadTimeout = localReadTimeout;
    
    _lastRemoteAccessTime = lastAccessTime;
    _lastUpdateTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _isServerVersionValid = isServerVersionValid;

    if (value != null)
      _valueRef = new SoftReference(value);
  }

  public CacheMapEntry(CacheMapEntry oldEntry,
		       long idleTimeout,
		       long lastUpdateTime)
  {
    _valueHash = oldEntry.getValueHashKey();
    _flags = oldEntry.getFlags();
    _version = oldEntry.getVersion();
    
    _expireTimeout = oldEntry.getExpireTimeout();
    _idleTimeout = idleTimeout;
    _leaseTimeout = oldEntry.getLeaseTimeout();
    _localReadTimeout = oldEntry.getLocalReadTimeout();
    
    _lastRemoteAccessTime = lastUpdateTime;
    _lastUpdateTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _leaseExpireTime = oldEntry._leaseExpireTime;
    _leaseOwner = oldEntry._leaseOwner;

    _isServerVersionValid = oldEntry.isServerVersionValid();

    Object value = oldEntry.getValue();
    
    if (value != null)
      _valueRef = new SoftReference(value);
  }

  /**
   * Returns the last access time.
   */
  public long getLastAccessTime()
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
  public long getLastUpdateTime()
  {
    return _lastUpdateTime;
  }

  /**
   * Returns the expiration time
   */
  public final long getExpirationTime()
  {
    return _lastUpdateTime + _expireTimeout;
  }

  public final boolean isLocalReadValid(int serverIndex, long now)
  {
    if (! _isServerVersionValid)
      return false;
    else if (now <= _lastAccessTime + _localReadTimeout)
      return true;
    else if (_leaseOwner == serverIndex && now <= _leaseExpireTime)
      return true;
    else
      return false;
  }

  public final boolean isExpired(long now)
  {
    return (_lastUpdateTime + _expireTimeout < now);
  }

  public final boolean isLeaseExpired(long now)
  {
    return (_leaseExpireTime <= now);
  }

  /**
   * Returns the lease owner
   */
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
    }
  }

  public int getFlags()
  {
    return _flags;
  }

  /**
   * Returns the expire timeout for this entry.
   */
  public long getExpireTimeout()
  {
    return _expireTimeout;
  }

  /**
   * Returns the idle timeout for this entry.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  /**
   * Returns the idle window to avoid too many updates
   */
  public long getIdleWindow()
  {
    long window = _idleTimeout / 4;
    long windowMax = 15 * 60 * 1000L;

    if (window < windowMax)
      return window;
    else
      return windowMax;
  }

  /**
   * Returns the read timeout for a local cached entry
   */
  public long getLocalReadTimeout()
  {
    return _localReadTimeout;
  }

  /**
   * Returns the timeout for a lease of the cache entry
   */
  public long getLeaseTimeout()
  {
    return _leaseTimeout;
  }

  public long getVersion()
  {
    return _version;
  }

  /**
   * Sets the deserialized value for the entry.
   */
  public final void setObjectValue(Object value)
  {
    if (value != null && (_valueRef == null || _valueRef.get() == null))
      _valueRef = new SoftReference(value);
  }

  /**
   * Returns true if the value is null
   */
  public boolean isValueNull()
  {
    return _valueHash == null;
  }

  /**
   * Returns the deserialized value for the entry.
   */
  public final Object getValue()
  {
    SoftReference valueRef = _valueRef;

    if (valueRef != null)
      return valueRef.get();
    else
      return null;
  }

  public byte []getValueHash()
  {
    if (_valueHash != null)
      return _valueHash.getHash();
    else
      return null;
  }

  public HashKey getValueHashKey()
  {
    return _valueHash;
  }

  /**
   * Returns true if the server version (startup count) matches
   * the database.
   */
  public boolean isServerVersionValid()
  {
    return _isServerVersionValid;
  }

  //
  // jcache stubs
  //

  public Object getKey()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public Object setValue(Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getCreationTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isValid()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getCost()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public int getHits()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[value=" + _valueHash
	    + ",flags=0x" + Integer.toHexString(_flags)
	    + ",version=" + _version
	    + ",lease=" + _leaseOwner
	    + "]");
  }
}
