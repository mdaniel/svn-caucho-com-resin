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

import com.caucho.cluster.CacheEntry;
import com.caucho.util.Alarm;

import java.lang.ref.SoftReference;

/**
 * An entry in the cache map
 */
public final class CacheMapEntry implements CacheEntry {
  public static final CacheMapEntry NULL
    = new CacheMapEntry(null, null, 0, 0, 0, false);
  
  private final HashKey _valueHash;
  private final int _flags;
  private final long _version;
  private final long _localExpireTime;

  private final boolean _isServerVersionValid;
  
  private SoftReference _valueRef;

  public CacheMapEntry(HashKey valueHash,
		       Object value,
		       int flags,
		       long version,
		       long localExpireTime,
		       boolean isServerVersionValid)
  {
    _valueHash = valueHash;
    _flags = flags;
    _version = version;
    
    _localExpireTime = localExpireTime;
    _isServerVersionValid = isServerVersionValid;

    if (value != null)
      _valueRef = new SoftReference(value);
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

  public int getFlags()
  {
    return _flags;
  }

  public long getVersion()
  {
    return _version;
  }

  public void setValue(Object value)
  {
    if (value != null && (_valueRef == null || _valueRef.get() == null))
      _valueRef = new SoftReference(value);
  }

  public Object getValue()
  {
    SoftReference valueRef = _valueRef;

    if (valueRef != null)
      return valueRef.get();
    else
      return null;
  }

  /**
   * Returns true if the server version (startup count) matches
   * the database.
   */
  public boolean isServerVersionValid()
  {
    return _isServerVersionValid;
  }

  public boolean isExpired()
  {
    return (_localExpireTime < Alarm.getExactTime()
	    || ! _isServerVersionValid);
  }

  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[value=" + _valueHash
	    + ",flags=" + Integer.toHexString(_flags)
	    + ",version=" + _version
	    + "]");
  }
}
