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

import com.caucho.util.HashKey;

/**
 * Full data from the data map
 */
@SuppressWarnings("serial")
public final class CacheData extends MnodeValue {
  private final HashKey _key;
  private final HashKey _cacheKey;

  private final long _valueDataId;
  private final long _valueDataTime;
  
  private final long _accessTime;
  private final long _modifiedTime;

  public CacheData(HashKey key,
                   HashKey cacheKey,
                   long valueHash,
                   long valueDataId,
                   long valueDataTime,
                   long valueLength,
                   long version,
                   long flags,
                   long accessedTimeout,
                   long modifiedTimeout,
                   long leaseTimeout,
                   long accessTime,
                   long modifiedTime)
  {
    super(valueHash, valueLength, version,
          flags,
          accessedTimeout, modifiedTimeout, leaseTimeout);
    
    _key = key;
    _cacheKey = cacheKey;
    
    _valueDataId = valueDataId;
    _valueDataTime = valueDataTime;
    _accessTime = accessTime;
    _modifiedTime = modifiedTime;
  }

  public HashKey getKey()
  {
    return _key;
  }

  public HashKey getCacheKey()
  {
    return _cacheKey;
  }

  public long getAccessTime()
  {
    return _accessTime;
  }

  public long getModifiedTime()
  {
    return _modifiedTime;
  }
  
  public long getValueDataId()
  {
    return _valueDataId;
  }
}
