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

import java.lang.ref.SoftReference;
import java.sql.Blob;

import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;

/**
 * An entry in the cache map
 */
public final class MnodeEntry extends MnodeValue {
  private static final L10N L = new L10N(MnodeEntry.class);
  
  public static final MnodeEntry NULL
  = new MnodeEntry(0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, false, true);
  
  public static final long NULL_KEY = 0;
  public static final long ANY_KEY = createAnyKey();
  
  private final long _valueDataId;
  private final long _valueDataTime;
  
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

  public MnodeEntry(long valueHash,
                    long valueLength,
                    long version,
                    long flags,
                    long accessedExpireTimeout,
                    long modifiedExpireTimeout,
                    long leaseExpireTimeout,
                    long valueDataId,
                    long valueDataTime,
                    Object value,
                    long lastAccessTime,
                    long lastModifiedTime,
                    boolean isServerVersionValid,
                    boolean isImplicitNull)
  {
    super(valueHash, valueLength, version,
          flags,
          accessedExpireTimeout, modifiedExpireTimeout, leaseExpireTimeout);
    
    _valueDataId = valueDataId;
    _valueDataTime = valueDataTime;
    
    _lastRemoteAccessTime = lastAccessTime;
    _lastModifiedTime = lastModifiedTime;
    
    // server/0165
    // _lastAccessTime = CurrentTime.getCurrentTime();
    setLastAccessTime(lastAccessTime);

    _isImplicitNull = isImplicitNull;
    _isServerVersionValid = isServerVersionValid;
    
    if (value != null)
      _valueRef = new SoftReference<Object>(value);
    
    if ((valueDataId != 0) != (valueHash != 0)) {
      throw new IllegalStateException(L.l("mismatch dataId {0} and valueHash {1}",
                                          _valueDataId, this));
    }
    
  }

  public MnodeEntry(MnodeValue mnodeValue,
                    long valueDataId,
                    long valueDataTime,
                    Object value,
                    long lastAccessTime,
                    long lastModifiedTime,
                    boolean isServerVersionValid,
                    boolean isImplicitNull,
                    int leaseOwner)
  {
    this(mnodeValue.getValueHash(),
         mnodeValue.getValueLength(),
         mnodeValue.getVersion(),
         mnodeValue.getFlags(),
         mnodeValue.getAccessedExpireTimeout(),
         mnodeValue.getModifiedExpireTimeout(),
         mnodeValue.getLeaseExpireTimeout(),
         valueDataId,
         valueDataTime,
         value,
         lastAccessTime,
         lastModifiedTime,
         isServerVersionValid,
         isImplicitNull);
    
    long now = CurrentTime.getCurrentTime();
    
    setLeaseOwner(leaseOwner, now);
  }

  public MnodeEntry(MnodeEntry oldMnodeValue,
                    long valueDataId,
                    long valueDataTime,
                    long accessTimeout,
                    long lastAccessTime)
  {
    super(oldMnodeValue.getValueHash(),
          oldMnodeValue.getValueLength(),
          oldMnodeValue.getVersion(),
          oldMnodeValue.getFlags(),
          accessTimeout,
          oldMnodeValue.getModifiedExpireTimeout(),
          oldMnodeValue.getLeaseExpireTimeout());
    
    _valueDataId = valueDataId;
    _valueDataTime = valueDataTime;
    
    _lastRemoteAccessTime = lastAccessTime;
    //_lastAccessTime = CurrentTime.getCurrentTime();
    // server/01o9
    setLastAccessTime(lastAccessTime);
    
    _lastModifiedTime = oldMnodeValue._lastModifiedTime;

    _leaseExpireTime = oldMnodeValue._leaseExpireTime;
    _leaseOwner = oldMnodeValue._leaseOwner;

    _isImplicitNull = oldMnodeValue.isImplicitNull();
    _isServerVersionValid = oldMnodeValue.isServerVersionValid();

    Object value = oldMnodeValue.getValue();
    
    if (value != null)
      _valueRef = new SoftReference<Object>(value);
  }
  
  public static MnodeEntry createInitialNull(CacheConfig config)
  {
    long accessedExpireTimeout = 0;
    long modifiedExpireTimeout = 0;
    long leaseExpireTimeout = 0;
    
    long now = 0;//CurrentTime.getCurrentTime();
    
    if (config != null) {
      accessedExpireTimeout = config.getAccessedExpireTimeout();
      modifiedExpireTimeout = config.getModifiedExpireTimeout();
      leaseExpireTimeout = config.getLeaseExpireTimeout();
    }
    
    return new MnodeEntry(0, 0, 0, 
                          0, 
                          accessedExpireTimeout, 
                          modifiedExpireTimeout,
                          leaseExpireTimeout,
                          0, 0, null,
                          now, now, false, true);
  }
  
  public MnodeEntry updateModifiedTime(long now)
  {
    return new MnodeEntry(getValueHash(),
                          getValueLength(),
                          getVersion(),
                          getFlags(),
                          getAccessedExpireTimeout(),
                          getModifiedExpireTimeout(),
                          getLeaseExpireTimeout(),
                          getValueDataId(),
                          getValueDataTime(),
                          getValue(),
                          now,
                          now,
                          isServerVersionValid(),
                          isImplicitNull());
  }
  
  public long getValueDataId()
  {
    return _valueDataId;
  }

  /**
   * @return
   */
  public long getValueDataTime()
  {
    return _valueDataTime;
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

  /**
   * Returns true if the local (unchecked) expire time.
   */
  public final boolean isLocalExpired(int serverIndex, 
                                      long now, 
                                      CacheConfig config)
  {
    return isLocalExpired(serverIndex, now, config.getLocalExpireTimeout());
  }

  public final boolean isLocalExpired(int serverIndex, 
                                      long now,
                                      long localExpireTimeout)
  {
    if (! _isServerVersionValid) {
      return true;
    }
    else if (isExpired(now)) {
      return true;
    }
    else if (now <= _lastAccessTime + localExpireTimeout) {
      return false;
    }
    else if ((serverIndex <= 2 
              && (localExpireTimeout > 0 || getLeaseExpireTimeout() > 0))
             || _leaseOwner == serverIndex && now <= _leaseExpireTime) {
      return false;
    }
    else {
      return true;
    }
  }

  public final boolean isLeaseExpired(long now)
  {
    return _leaseOwner <= 0 || _leaseExpireTime <= now;
  }
  
  /**
   * Returns true is the entry has expired for being idle or having
   * expired.
   */
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
  public final int getLeaseOwner()
  {
    return _leaseOwner;
  }

  /**
   * Sets the owner
   */
  public final void setLeaseOwner(int leaseOwner, long now)
  {
    if (leaseOwner > 2 && getLeaseExpireTimeout() > 0) {
      _leaseOwner = leaseOwner;

      _leaseExpireTime = now + getLeaseExpireTimeout();
    }
    else {
      _leaseOwner = -1;

      _leaseExpireTime = 0;
      
      // server/0b10
      // _lastAccessTime = 0;
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
   * Sets the owner
   */
  public final void clearLease(int oldLeaseOwner)
  {
    if (_leaseOwner == oldLeaseOwner) {
      _leaseOwner = -1;

      _leaseExpireTime = 0;
    }
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
  public boolean isValueNull()
  {
    return getValueHash() == 0;
  }

  /**
   * Returns the deserialized value for the entry.
   */
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
  
  /**
   * Creates an update with local data removed for remote update.
   */
  public MnodeUpdate getRemoteUpdate()
  {
    return new MnodeUpdate(getValueHash(), getValueLength(), getVersion(),
                           this,
                           getLeaseOwner());
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
    else if (getValueHash() == 0)
      return -1;
    else
      return (int) (getValueHash() - mnode.getValueHash());
  }

  //
  // jcache stubs
  //


  public boolean isValid()
  {
    return (! isExpired(CurrentTime.getCurrentTime()));
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
  
  private static long createAnyKey()
  {
    return -1;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[value=" + Long.toHexString(getValueHash())
            + ",flags=0x" + Long.toHexString(getFlags())
            + ",version=" + getVersion()
            + ",lease=" + _leaseOwner
            + "]");
  }
}
