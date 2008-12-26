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
import com.caucho.cluster.CacheSerializer;
import com.caucho.cluster.HessianSerializer;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.util.LruCache;

/**
 * Manages the distributed cache
 */
public class CacheConfig
{
  private long _localReadTimeout = 10L; // 10ms default timeout
  
  private long _idleTimeout = Long.MAX_VALUE / 2;

  private CacheSerializer _keySerializer;
  private CacheSerializer _valueSerializer;

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public long getLocalReadTimeout()
  {
    return _localReadTimeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public void setLocalReadTimeout(long timeout)
  {
    _localReadTimeout = timeout;
  }

  /**
   * Returns the maximum idle time in the database.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  /**
   * Returns the key serializer
   */
  public CacheSerializer getKeySerializer()
  {
    return _keySerializer;
  }

  /**
   * Returns the value serializer
   */
  public CacheSerializer getValueSerializer()
  {
    return _valueSerializer;
  }

  /**
   * Sets the value serializer
   */
  public void setValueSerializer(CacheSerializer serializer)
  {
    _valueSerializer = serializer;
  }

  public void init()
  {
    if (_keySerializer == null)
      _keySerializer = new HessianSerializer();
    
    if (_valueSerializer == null)
      _valueSerializer = new HessianSerializer();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
