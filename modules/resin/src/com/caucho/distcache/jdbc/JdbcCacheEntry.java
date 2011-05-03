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

package com.caucho.distcache.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.DistCacheEntry;
import com.caucho.server.distcache.MnodeValue;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;

/**
 * An entry in the cache map
 */
public final class JdbcCacheEntry extends DistCacheEntry {
  private final JdbcCacheManager _manager;
  
  public JdbcCacheEntry(Object key,
                        HashKey keyHash,
                        TriadOwner owner,
                        JdbcCacheManager manager)
  {
    super(key, keyHash, owner);

    _manager = manager;
  }

  /**
   * Peeks the current value without checking any backing store
   */
  @Override
  public Object peek()
  {
    MnodeValue entryValue = getMnodeValue();

    if (entryValue != null)
      return entryValue.getValue();
    else
      return null;
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  @Override
  public Object get(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    return _manager.get(this, config, now);
  }

  /**
   * Returns the object for the given key, checking the backing if necessary.
   * If it is not found, the optional cacheLoader is invoked, if present.
   */
  /*
  @Override
  public Object getLazy(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    return _manager.getLazy(this, config, now);
  }
  */

  /**
   * Returns the object for the given key, checking the backing if necessary
   */
  @Override
  public MnodeValue getMnodeValue(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    return _manager.getMnodeValue(this, config, now); // , false);
  }

  /**
   * Fills the value with a stream
   */
  @Override
  public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _manager.getStream(this, os, config);
  }

  /**
   * Sets the current value
   */
  @Override
  public Object put(Object value, CacheConfig config)
  {
    return _manager.put(this, value, config);
  }

  /**
   * Sets the value by an input stream
   */
  @Override
  public ExtCacheEntry put(InputStream is,
                           CacheConfig config,
                           long idleTimeout)
    throws IOException
  {
    return _manager.putStream(this, is, config, idleTimeout);
  }

  /**
   * Sets the current value
   */
  @Override
  public boolean compareAndPut(long version, HashKey value, CacheConfig config)
  {
    return _manager.compareAndPut(this, version, value, config);
  }

  /**
   * Remove the value
   */
  @Override
  public boolean remove(CacheConfig config)
  {
    return _manager.remove(this, config);
  }
}
