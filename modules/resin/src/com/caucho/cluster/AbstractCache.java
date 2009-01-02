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

package com.caucho.cluster;

import com.caucho.config.ConfigException;
import com.caucho.loader.Environment;
import com.caucho.server.cluster.Server;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.distcache.HashKey;
import com.caucho.server.distcache.HashManager;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import javax.annotation.PostConstruct;

/**
 * Implements the distributed cache
 */
abstract public class AbstractCache implements Cache, ByteStreamCache
{
  private static final L10N L = new L10N(AbstractCache.class);

  private String _contextId;
  
  private String _name = "";

  private String _id;

  private CacheConfig _config = new CacheConfig();

  private LruCache<Object,HashKey> _keyHashCache
    = new LruCache<Object,HashKey>(512);

  private boolean _isInit;

  private DistributedCacheManager _distributedCacheManager;

  /**
   * Assign the name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Assign the serializer
   */
  public void setSerializer(CacheSerializer serializer)
  {
    _config.setValueSerializer(serializer);
  }

  /**
   * Sets the backup mode.
   */
  public void setBackup(boolean isBackup)
  {
    if (isBackup)
      _config.setFlags(_config.getFlags() | CacheConfig.FLAG_BACKUP);
    else
      _config.setFlags(_config.getFlags() & ~CacheConfig.FLAG_BACKUP);
  }

  /**
   * Sets the triplicate backup mode.
   */
  public void setTriplicate(boolean isTriplicate)
  {
    if (isTriplicate)
      _config.setFlags(_config.getFlags() | CacheConfig.FLAG_TRIPLICATE);
    else
      _config.setFlags(_config.getFlags() & ~CacheConfig.FLAG_TRIPLICATE);
  }

  /**
   * Initialize the cache
   */
  @PostConstruct
  public void init()
  {
    synchronized (this) {
      if (_isInit)
	return;
      _isInit = true;
    
      _contextId = Environment.getEnvironmentName();

      _id = _contextId + ":" + _name;

      _config.init();

      Server server = Server.getCurrent();

      if (server == null)
	throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a clustered environment",
				      getClass().getSimpleName()));

      _distributedCacheManager = server.getDistributedCacheManager();
    }
  }
  
  /**
   * Returns the object with the given key.
   */
  public Object get(Object key)
  {
    HashKey hashKey = getHashKey(key);

    return _distributedCacheManager.get(hashKey, _config);
  }
  
  /**
   * Fills an output stream with the value for a key.
   */
  public boolean get(Object key, OutputStream os)
    throws IOException
  {
    HashKey hashKey = getHashKey(key);

    return _distributedCacheManager.get(hashKey, os, _config);
  }
  
  /**
   * Returns the cache entry for the object with the given key.
   */
  public CacheEntry<Object> getEntry(Object key)
  {
    HashKey hashKey = getHashKey(key);
    
    return _distributedCacheManager.getEntry(hashKey, _config);
  }
  
  /**
   * Puts a new item in the cache.
   *
   * @param key the key of the item to put
   * @param value the value of the item to put
   */
  public void put(Object key, Object value)
  {
    HashKey hashKey = getHashKey(key);

    _distributedCacheManager.put(hashKey, value, _config);
  }
  
  /**
   * Puts a new item in the cache.
   *
   * @param key the key of the item to put
   * @param value the value of the item to put
   */
  public CacheEntry put(Object key,
			InputStream is,
			long idleTimeout)
    throws IOException
  {
    HashKey hashKey = getHashKey(key);

    return _distributedCacheManager.put(hashKey, is, idleTimeout, _config);
  }
  
  /**
   * Updates the cache if the old value hash matches the current value.
   * A null value for the old value hash only adds the entry if it's new
   *
   * @param key the key to compare
   * @param oldValueHash the hash of the old value, returned by getEntry
   * @param value the new value
   *
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
			       Object value,
			       byte[] oldValueHash)
  {
    put(key, value);
    
    return true;
  }
  
  /**
   * Updates the cache if the old value hash matches the current value.
   * A null value for the old value hash only adds the entry if it's new
   *
   * @param key the key to compare
   * @param oldValueHash the hash of the old value, returned by getEntry
   * @param value the new value
   *
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
			       InputStream is,
			       byte[] oldValueHash)
    throws IOException
  {
    put(key, is);
    
    return true;
  }

  /**
   * Removes the entry from the cache
   *
   * @return true if the object existed
   */
  public boolean remove(Object key)
  {
    HashKey hashKey = getHashKey(key);
    
    return _distributedCacheManager.remove(hashKey);
  }

  /**
   * Removes the entry from the cache if the current entry matches the hash
   */
  public boolean compareAndRemove(Object key, byte[] oldValueHash)
  {
    HashKey hashKey = getHashKey(key);
    
    return false;
  }

  /**
   * Returns the key hash
   */
  protected HashKey getHashKey(Object key)
  {
    HashKey hashKey = _keyHashCache.get(key);

    if (hashKey != null)
      return hashKey;

    try {
      MessageDigest digest
	= MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      NullDigestOutputStream dOut = new NullDigestOutputStream(digest);

      Object []fullKey = new Object[] { _id, key };

      _config.getKeySerializer().serialize(fullKey, dOut);

      hashKey = new HashKey(dOut.digest());

      _keyHashCache.put(key, hashKey);

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
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
