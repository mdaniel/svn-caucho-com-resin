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
import com.caucho.util.Hex;

/**
 * An entry in the cache map
 */
@SuppressWarnings("serial")
public class MnodeUpdate extends MnodeValue {
  private final byte []_keyHash;
  
  public MnodeUpdate(byte []keyHash,
                     byte []valueHash,
                     long valueLength,
                     long version,
                     byte []cacheHash,
                     long flags,
                     long accessedExpireTime,
                     long modifiedExpireTime)
  {
    super(valueHash, valueLength, version,
          cacheHash, 
          flags, 
          accessedExpireTime, modifiedExpireTime);
    
    _keyHash = keyHash;
  }
  
  public MnodeUpdate(byte []keyHash,
                     byte []valueHash,
                     long valueLength,
                     long version)
  {
    super(valueHash, valueLength, version);
    
    _keyHash = keyHash;
  }
  
  public MnodeUpdate(MnodeUpdate mnodeUpdate)
  {
    super(mnodeUpdate);
    
    _keyHash = mnodeUpdate._keyHash;
  }
  
  public MnodeUpdate(byte []keyHash,
                     MnodeValue mnodeValue)
  {
    super(mnodeValue);
    
    _keyHash = keyHash;
  }

  public MnodeUpdate(byte []keyHash,
                     byte []valueHash,
                     long valueLength,
                     long version,
                     CacheConfig config)
  {
    super(valueHash, valueLength, version, config);
    
    _keyHash = keyHash;
  }

  public MnodeUpdate(byte []keyHash,
                     byte []valueHash,
                     long valueLength,
                     long version,
                     MnodeValue oldValue)
  {
    super(valueHash, valueLength, version, oldValue);
    
    _keyHash = keyHash;
  }
  
  public MnodeUpdate(HashKey keyHash,
                     HashKey valueHash,
                     long valueLength,
                     long version,
                     CacheConfig config)
  {
    this(HashKey.getHash(keyHash),
         HashKey.getHash(valueHash),
         valueLength,
         version,
         config);
  }
  
  public final byte []getKeyHash()
  {
    return _keyHash;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
        + "[key=" + Hex.toHex(getKeyHash(), 0, 4)
        + ",value=" + Hex.toHex(getValueHash(), 0, 4)
        + ",len=" + getValueLength()
        + ",flags=" + Long.toHexString(getFlags())
        + ",version=" + getVersion()
        + "]");
  }
}
