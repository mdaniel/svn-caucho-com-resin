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

package com.caucho.distcache;

import java.io.IOException;
import java.io.OutputStream;

import javax.cache.Cache;

import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.MnodeUpdate;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;

/**
 * Provides additional information about an entry in a {@link javax.cache.Cache}.
 */
public interface ExtCacheEntry<K,V> extends Cache.Entry<K,V>
{
  /**
   * Returns the key hash for the current entry.
   */
  public HashKey getKeyHash();
  
  /**
   * Returns true for a null entry
   */
  public boolean isValueNull();
  
  /**
   * Returns the item's value
   */
  @Override
  public V getValue();

  /**
   * Returns the value key
   */
  public long getValueHash();
  
  /**
   * Returns the value length
   */
  public long getValueLength();
  
  public StreamSource getValueStream();

  /**
   * Returns the idle timeout
   */
  public long getAccessedExpireTimeout();
  
  /**
   * Returns the expire timeout.
   */
  public long getModifiedExpireTimeout();

  /**
   * Returns the lease timeout
   */
  public long getLeaseExpireTimeout();

  /**
   * @return
   */
  public long getLastAccessedTime();

  /**
   * Returns the last update time.
   */
  public long getLastModifiedTime();
  
  /**
   * Returns true when the entry is expired.
   */
  public boolean isExpired(long now);
  
  /**
   * Update the access time on a read.
   */
  public void updateAccessTime();

  /**
   * Returns the lease owner
   */
  public int getLeaseOwner();

  public boolean isValid();
  
  /**
   * Returns the load count.
   */
  public int getLoadCount();
  
  public int getUserFlags();

  /**
   * @return
   */
  public long getVersion();

  public MnodeUpdate getRemoteUpdate();
  
  /**
   * Loads the data to the output stream.
   */
  public boolean readData(OutputStream os, CacheConfig config)
    throws IOException;
}
