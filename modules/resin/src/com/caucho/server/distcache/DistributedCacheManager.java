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
import com.caucho.cluster.CacheSerializer;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterTriad;
import com.caucho.server.cluster.Server;
import com.caucho.util.LruCache;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * Manages the distributed cache
 */
abstract public class DistributedCacheManager
{
  private final Server _server;

  protected DistributedCacheManager(Server server)
  {
    _server = server;
  }

  /**
   * Returns the owning server
   */
  protected Server getServer()
  {
    return _server;
  }

  /**
   * Returns the owning cluster.
   */
  protected Cluster getCluster()
  {
    return _server.getCluster();
  }

  /**
   * Returns the owning triad.
   */
  protected ClusterTriad getTriad()
  {
    return _server.getTriad();
  }

  /**
   * Gets a cache key entry
   */
  abstract public CacheKeyEntry getKey(Object key, CacheConfig config);

  /**
   * Gets a cache entry
   */
  // abstract public CacheEntry getEntry(HashKey hashKey, CacheConfig config);

  /**
   * Gets a cache entry
   */
  //abstract public Object get(HashKey hashKey, CacheConfig config);

  /**
   * Gets a cache entry
   */
  /*
  abstract public boolean get(HashKey hashKey,
			      OutputStream os,
			      CacheConfig config)
    throws IOException;
  */

  /**
   * Gets a cache entry
   */
  //  abstract public Object peek(HashKey hashKey, CacheConfig config);

  /**
   * Sets a cache entry
   */
  public void put(HashKey hashKey,
			   Object value,
			   CacheConfig config)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(HashKey hashKey,
			   InputStream is,
			   CacheConfig config,
			   long idleTimeout)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a cache entry
   */
  abstract public boolean remove(HashKey hashKey);

  /**
   * Closes the manager
   */
  public void close()
  {
  }

  /**
   * Returns the key hash
   */
  protected HashKey createHashKey(Object key, CacheConfig config)
  {
    try {
      MessageDigest digest
	= MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      NullDigestOutputStream dOut = new NullDigestOutputStream(digest);

      Object []fullKey = new Object[] { config.getGuid(), key };

      config.getKeySerializer().serialize(fullKey, dOut);

      HashKey hashKey = new HashKey(dOut.digest());

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server.getServerId() + "]";
  }

  static class NullDigestOutputStream extends OutputStream {
    private MessageDigest _digest;

    NullDigestOutputStream(MessageDigest digest)
    {
      _digest = digest;
    }

    public void write(int value)
    {
      _digest.update((byte) value);
    }

    public void write(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    public byte []digest()
    {
      return _digest.digest();
    }

    public void flush()
    {
    }

    public void close()
    {
    }
  }
}
