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

import com.caucho.util.CurrentTime;

/**
 * An entry in the cache map
 */
@SuppressWarnings("serial")
public class MnodeUpdate extends MnodeValue {
  public static final MnodeUpdate NULL
    = new MnodeUpdate(0, 0, 0, 0, 0, 0, -1, -1, -1, -1);
  
  private final int _leaseOwner;
  private final long _accessTime;
  private final long _modifiedTime;
  
  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version,
                     long flags,
                     long accessedExpireTime,
                     long modifiedExpireTime,
                     long leaseExpireTime,
                     int leaseOwner,
                     long accessTime,
                     long modifiedTime)
  {
    super(valueHash, valueLength, version,
          flags, 
          accessedExpireTime, modifiedExpireTime, leaseExpireTime);
    
    _leaseOwner = leaseOwner;
    _accessTime = accessTime;
    _modifiedTime = modifiedTime;
  }
  
  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version)
  {
    super(valueHash, valueLength, version);
    
    _leaseOwner = -1;
    _modifiedTime = CurrentTime.getCurrentTime();
    _accessTime = _modifiedTime;
  }
  
  public MnodeUpdate(MnodeUpdate update)
  {
    super(update);
    
    _leaseOwner = update._leaseOwner;
    _modifiedTime = update._modifiedTime;
    _accessTime = update._accessTime;
  }
  
  public MnodeUpdate(MnodeValue mnodeValue)
  {
    super(mnodeValue);
    
    _leaseOwner = -1;
    _modifiedTime = CurrentTime.getCurrentTime();
    _accessTime = _modifiedTime;
  }
  
  public MnodeUpdate(MnodeValue mnodeValue,
                     int leaseOwner,
                     long modifiedTime)
  {
    super(mnodeValue);
    
    _leaseOwner = leaseOwner;
    _modifiedTime = modifiedTime;
    _accessTime = modifiedTime;
  }

  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version,
                     CacheConfig config)
  {
    super(valueHash, valueLength, version, config);
    
    _leaseOwner = -1;
    _modifiedTime = CurrentTime.getCurrentTime();
    _accessTime = _modifiedTime;
  }

  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version,
                     CacheConfig config,
                     int leaseOwner,
                     long leaseTimeout,
                     long modifiedTime)
  {
    super(valueHash, valueLength, version, config);
    
    _leaseOwner = leaseOwner;
    _modifiedTime = modifiedTime;
    _accessTime = modifiedTime;
  }

  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version,
                     MnodeValue oldValue)
  {
    super(valueHash, valueLength, version, oldValue);
    
    _leaseOwner = -1;
    _modifiedTime = CurrentTime.getCurrentTime();
    _accessTime = _modifiedTime;
  }

  public MnodeUpdate(long valueHash,
                     long valueLength,
                     long version,
                     MnodeValue oldValue,
                     int leaseOwner)
  {
    super(valueHash, valueLength, version, oldValue);
    
    _leaseOwner = leaseOwner;
    _modifiedTime = CurrentTime.getCurrentTime();
    _accessTime = _modifiedTime;
  }
  
  public static MnodeUpdate createNull(long version, MnodeValue oldValue)
  {
    return new MnodeUpdate(0, 0, version, oldValue);
  }
  
  public static MnodeUpdate createNull(long version, CacheConfig config)
  {
    return new MnodeUpdate(0, 0, version, config);
  }
  
  /**
   * Create an update that removes the local information for sending to a
   * remote server.
   */
  public MnodeUpdate createRemote()
  {
    return new MnodeUpdate(getValueHash(),
                           getValueLength(),
                           getVersion(),
                           this,
                           getLeaseOwner());
  }
  
  public final int getLeaseOwner()
  {
    return _leaseOwner;
  }
  
  public final long getLastAccessTime()
  {
    return _accessTime;
  }
  
  public final long getLastModifiedTime()
  {
    return _modifiedTime;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
        + "[value=" + Long.toHexString(getValueHash())
        + ",len=" + getValueLength()
        + ",flags=" + Long.toHexString(getFlags())
        + ",version=" + getVersion()
        + ",lease=" + getLeaseOwner()
        + "]");
  }
}
