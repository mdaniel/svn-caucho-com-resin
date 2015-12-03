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
public final class ExtCacheEntryFacade implements ExtCacheEntry {
  private static final Logger log
    = Logger.getLogger(ExtCacheEntryFacade.class.getName());
  
  private final DistCacheEntry _entry;
  private final MnodeEntry _mnodeEntry;

  ExtCacheEntryFacade(DistCacheEntry entry)
  {
    _entry = entry;
    _mnodeEntry = entry.getMnodeEntry();
  }

  /**
   * Returns the key for this entry in the Cache.
   */
  @Override
  public final Object getKey()
  {
    return _entry.getKey();
  }

  /**
   * Returns the keyHash
   */
  @Override
  public final HashKey getKeyHash()
  {
    return _entry.getKeyHash();
  }

  /**
   * Returns the value of the cache entry.
   */
  @Override
  public Object getValue()
  {
    return _mnodeEntry.getValue();
  }

  /**
   * Returns true if the value is null.
   */
  @Override
  public boolean isValueNull()
  {
    return _mnodeEntry.isValueNull();
  }

  @Override
  public final int getUserFlags()
  {
    return _mnodeEntry.getUserFlags();
  }

  /**
   * Returns the value of the cache entry.
   */
  @Override
  public StreamSource getValueStream()
  {
    return _entry.getValueStream();
  }

  @Override
  public long getValueHash()
  {
    return _mnodeEntry.getValueHash();
  }

  @Override
  public long getValueLength()
  {
    return _mnodeEntry.getValueLength();
  }
  
  /**
   * Writes the data to a stream.
   */
  @Override
  public boolean readData(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _entry.readData(os, config);
  }

  @Override
  public long getAccessedExpireTimeout()
  {
    return _mnodeEntry.getAccessedExpireTimeout();
  }

  @Override
  public long getModifiedExpireTimeout()
  {
    return _mnodeEntry.getModifiedExpireTimeout();
  }
  
  @Override
  public boolean isExpired(long now)
  {
    return _mnodeEntry.isExpired(now);
  }
  
  @Override
  public void updateAccessTime()
  {
    _entry.updateAccessTime();
  }

  @Override
  public long getLeaseExpireTimeout()
  {
    return _mnodeEntry.getLeaseExpireTimeout();
  }

  @Override
  public int getLeaseOwner()
  {
    if (! _mnodeEntry.isLeaseExpired(CurrentTime.getCurrentTime())) {
      return _mnodeEntry.getLeaseOwner();
    }
    else {
      return -1;
    }
  }

  @Override
  public long getLastAccessedTime()
  {
    return _mnodeEntry.getLastAccessedTime();
  }

  @Override
  public long getLastModifiedTime()
  {
    return _mnodeEntry.getLastModifiedTime();
  }

  @Override
  public long getVersion()
  {
    return _mnodeEntry.getVersion();
  }

  @Override
  public MnodeUpdate getRemoteUpdate()
  {
    return _mnodeEntry.getRemoteUpdate();
  }

  @Override
  public boolean isValid()
  {
    return _mnodeEntry.isValid();
  }

  //
  // statistics
  //
  
  @Override
  public int getLoadCount()
  {
    return _entry.getLoadCount();
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _mnodeEntry + "]");
  }
}
