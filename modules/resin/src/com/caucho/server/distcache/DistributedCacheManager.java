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

package com.caucho.server.distcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import com.caucho.distcache.AbstractCache;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.util.FreeList;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;
import com.caucho.util.LruCache;

/**
 * Manages the distributed cache
 */
abstract public class DistributedCacheManager
{
  private static final Object NULL_OBJECT = new Object();
  
  private FreeList<KeyHashStream> _keyStreamFreeList
    = new FreeList<KeyHashStream>(32);
  
  private LruCache<CacheKey,HashKey> _keyCache;
  
  /**
   * Starts the service
   */
  public void start()
  {
    _keyCache = new LruCache<CacheKey,HashKey>(64 * 1024);
  }

  /**
   * Called when a cache initializes.
   */
  public void initCache(AbstractCache abstractCache)
  {
  }

  /**
   * Called when a cache is removed.
   */
  public void destroyCache(AbstractCache abstractCache)
  {
  }


  /**
   * Gets a cache key entry
   */
  abstract public DistCacheEntry getCacheEntry(Object key, CacheConfig config);

  /**
   * Gets a cache key entry
   */
  abstract public DistCacheEntry getCacheEntry(HashKey key, CacheConfig config);

  /**
   * Sets a cache entry
   */
  public void put(HashKey hashKey,
                  Object value,
                  CacheConfig config)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes a cache entry
   */
  abstract public boolean remove(HashKey hashKey);

  abstract public CacheDataBacking getDataBacking();
  
  /**
   * For QA
   */
  public byte[] calculateValueHash(Object value, CacheConfig config)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Closes the manager
   */
  public void close()
  {
  }

  public void closeCache(String guid)
  {
    _keyCache.clear();
  }

  protected HashKey createHashKey(Object key, CacheConfig config)
  {
    CacheKey cacheKey = new CacheKey(config.getGuid(), key);
    
    HashKey hashKey = _keyCache.get(cacheKey);
    
    if (hashKey == null) {
      hashKey = createHashKeyImpl(key, config);
      
      _keyCache.put(cacheKey, hashKey);
    }
    
    return hashKey;
  }
  
  /**
   * Returns the key hash
   */
  protected HashKey createHashKeyImpl(Object key, CacheConfig config)
  {
    try {
      KeyHashStream dOut = _keyStreamFreeList.allocate();
      
      if (dOut == null) {
        MessageDigest digest
          = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);
      
        dOut = new KeyHashStream(digest);
      }
      
      dOut.init();

      CacheSerializer keySerializer = config.getKeySerializer();
      
      keySerializer.serialize(config.getGuid(), dOut);
      keySerializer.serialize(key, dOut);

      HashKey hashKey = new HashKey(dOut.digest());
      
      _keyStreamFreeList.free(dOut);

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the key hash
   */
  public HashKey createSelfHashKey(Object key, CacheSerializer keySerializer)
  {
    try {
      MessageDigest digest
        = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      KeyHashStream dOut = new KeyHashStream(digest);

      keySerializer.serialize(key, dOut);

      HashKey hashKey = new HashKey(dOut.digest());

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class KeyHashStream extends OutputStream {
    private MessageDigest _digest;

    KeyHashStream(MessageDigest digest)
    {
      _digest = digest;
    }
    
    void init()
    {
      _digest.reset();
    }

    @Override
    public void write(int value)
    {
      _digest.update((byte) value);
    }

    @Override
    public void write(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    public byte []digest()
    {
      byte []digest = _digest.digest();
      
      return digest;
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close()
    {
    }
  }
  
  static class CacheKey {
    private String _guid;
    private Object _key;
    
    CacheKey(String guid, Object key)
    {
      init(guid, key);
    }
    
    void init(String guid, Object key)
    {
      _guid = guid;
      
      if (key == null)
        key = NULL_OBJECT;
      
      _key = key;
    }
    
    @Override
    public int hashCode()
    {
      int hash = 17;
      
      hash += _guid.hashCode();
      
      hash = 65521 * hash + _key.hashCode();
      
      return hash;
    }
    
    @Override
    public boolean equals(Object o)
    {
      CacheKey key = (CacheKey) o;
      
      if (! key._key.equals(_key))
        return false;
      
      return key._guid.equals(_guid);
    }
  }
}
