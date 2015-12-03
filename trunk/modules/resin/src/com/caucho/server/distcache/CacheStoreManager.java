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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.CacheSerializer;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.inject.Module;
import com.caucho.util.HashKey;
import com.caucho.vfs.StreamSource;

/**
 * Manages the distributed cache
 */
@Module
public final class CacheStoreManager implements CacheEntryFactory
{
  private final ResinSystem _resinSystem;
  
  private final CacheEntryManager _cacheEntryManager;
  private final CacheKeyManager _keyManager;
  
  private final CacheRegionManager _regionManager
    = new CacheRegionManager();
  
  private CacheDataBackingImpl _dataBacking;
  
  private final LocalMnodeManager _localMnodeManager;
  private final LocalDataManager _localDataManager;
  private final LocalStoreManager _localStoreManager;
  
  private final ConcurrentHashMap<HashKey,CacheMnodeListener> _cacheListenMap
    = new ConcurrentHashMap<HashKey,CacheMnodeListener>();
  
  private final CacheConfig _defaultCacheConfig = new CacheConfig();
  
  private boolean _isCacheListen;
  
  private boolean _isClosed;
  
  private CacheEngine _cacheEngine = new AbstractCacheEngine();
  
  private AdminCacheStore _admin = new AdminCacheStore(this);
  
  public CacheStoreManager(ResinSystem resinSystem)
  {
    _resinSystem = resinSystem;
    // new AdminPersistentStore(this);
    
    _cacheEntryManager = new CacheEntryManager(this);
    _keyManager = new CacheKeyManager(_cacheEntryManager);
    
    _localMnodeManager = new LocalMnodeManager(this);
    _localDataManager = new LocalDataManager(this);
    
    _localStoreManager = new LocalStoreManager(this);
  }
  
  public final CacheEntryManager getCacheEntryManager()
  {
    return _cacheEntryManager;
  }

  public void setCacheEngine(CacheEngine cacheEngine)
  {
    if (cacheEngine == null)
      throw new NullPointerException();
    
    _cacheEngine = cacheEngine;
  }
  
  public CacheEngine getCacheEngine()
  {
    return _cacheEngine;
  }
  
  public CacheDataBacking getDataBacking()
  {
    return _dataBacking;
  }
  
  public LocalMnodeManager getLocalMnodeManager()
  {
    return _localMnodeManager;
  }
  
  public LocalDataManager getLocalDataManager()
  {
    return _localDataManager;
  }
  
  public LocalStoreManager getLocalStoreManager()
  {
    return _localStoreManager;
  }
  
  public CacheKeyManager getKeyManager()
  {
    return _keyManager;
  }
  
  public final CacheConfig getDefaultCacheConfig()
  {
    return _defaultCacheConfig;
  }
  
  public void addCacheListener(HashKey cacheKey, CacheMnodeListener listener)
  {
    _cacheListenMap.put(cacheKey, listener);
    _isCacheListen = true;
  }

  /**
   * Returns the key entry.
   */
  public final DistCacheEntry getCacheEntry(Object key, CacheConfig config)
  {
    HashKey hashKey = _keyManager.createHashKey(key, config);

    DistCacheEntry entry
      = _cacheEntryManager.createCacheEntry(hashKey, config.getCache());
    
    if (key != null) {
      entry.setKey(key);
    }
    
    return entry;
  }

  /**
   * Returns the key entry.
   */
  final public DistCacheEntry getCacheEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();
    
    return _cacheEntryManager.getCacheEntry(key);
  }

  /**
   * Returns the key entry.
   */
  public final DistCacheEntry getCacheEntry(HashKey hashKey,
                                            HashKey cacheKey,
                                            Object oKey)
  {
    DistCacheEntry entry = getCacheEntry(hashKey, cacheKey);
    
    if (oKey != null) {
      entry.setKey(oKey);
    }
    
    return entry;
  }

  /**
   * Returns the key entry.
   */
  final public DistCacheEntry getCacheEntry(HashKey key,
                                            HashKey cacheKey)
  {
    if (key == null)
      throw new NullPointerException();
    
    DistCacheEntry entry = _cacheEntryManager.getCacheEntry(key);
    
    if (entry != null) {
      return entry;
    }
    
    CacheConfig defaultConfig = getDefaultCacheConfig();
    
    CacheHandle cache = _regionManager.createCache(cacheKey, defaultConfig);
    
    return _cacheEntryManager.createCacheEntry(key, cache);
  }

  /**
   * Returns the key entry.
   */
  final public DistCacheEntry getCacheEntry(HashKey key, CacheHandle cache)
  {
    if (key == null)
      throw new NullPointerException();
    
    return _cacheEntryManager.createCacheEntry(key, cache);
  }

  /**
   * Returns the key entry.
   */
  @Override
  public DistCacheEntry createCacheEntry(HashKey hashKey,
                                         CacheHandle cache)
  {
    TriadOwner owner = TriadOwner.getHashOwner(hashKey.getHash());

    return new DistCacheEntry(this, hashKey, cache, owner);
  }

  /**
   * Returns the CacheHandle.
   */
  public final CacheHandle getCache(String guid, CacheSerializer keySerializer)
  {
    HashKey cacheHash = getKeyManager().createSelfHashKey(guid, keySerializer);

    return getCache(cacheHash);
  }
  
  public final CacheHandle getCache(HashKey cacheHash)
  {
    return _regionManager.createCache(cacheHash, getDefaultCacheConfig());
  }

  public final DistCacheEntry loadLocalEntry(HashKey key, CacheConfig config)
  {
    if (key == null)
      throw new NullPointerException();

    DistCacheEntry entry = getCacheEntry(key, config);

    entry.loadLocalEntry();

    return entry;
  }

  public final DistCacheEntry loadLocalEntry(HashKey key, HashKey cacheKey)
  {
    if (key == null)
      throw new NullPointerException();

    DistCacheEntry entry = getCacheEntry(key, cacheKey);

    entry.loadLocalEntry();

    return entry;
  }

  /**
   * Sets a cache entry
   */
  public final void saveLocalUpdateTime(HashKey key,
                                        long version,
                                        long accessTimeout,
                                        long lastAccessTime)
  {
    DistCacheEntry entry = _cacheEntryManager.getCacheEntry(key);

    if (entry == null)
      return;

    MnodeEntry oldEntryValue = entry.getMnodeEntry();

    if (oldEntryValue == null || version != oldEntryValue.getVersion())
      return;

    MnodeEntry mnodeValue
      = new MnodeEntry(oldEntryValue,
                       oldEntryValue.getValueDataId(),
                       accessTimeout, 
                       lastAccessTime);

    entry.saveLocalUpdateTime(mnodeValue);
  }

  /**
   * localPut updates the local copy based on a CachePut message
   */
  public MnodeUpdate localPut(byte []keyHash,
                              MnodeUpdate update,
                              StreamSource source)
  {
    try {
      HashKey key = new HashKey(keyHash);

      CacheConfig config = null;
      DistCacheEntry entry = loadLocalEntry(key, config);
    
      return entry.localUpdate(update, source.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void notifyPutListeners(HashKey key,
                          HashKey cacheKey,
                          MnodeUpdate update,
                          MnodeValue mnodeValue)
  {
    if (mnodeValue != null
        && mnodeValue.getValueHash() == update.getValueHash()
        && _isCacheListen) {
      CacheMnodeListener listener = _cacheListenMap.get(cacheKey);

      if (listener != null) {
        listener.onPut(key, cacheKey, mnodeValue);
      }
    }
  }

  /**
   * Clears leases on server start/stop
   */
  final public void clearLeases()
  {
    _cacheEntryManager.clearLeases();
  }

  /**
   * Clears ephemeral data on startup.
   */
  public void clearEphemeralEntries()
  {
  }
  
  public Iterator<HashKey> getEntries(HashKey cacheKey)
  {
    return _dataBacking.getEntries(cacheKey);
    
    // return _cacheEntryManager.getEntries();
  }
  
  public Iterator<DistCacheEntry> getEntries()
  {
    return _cacheEntryManager.getEntries();
  }
  
  public void start()
  {
    if (_dataBacking == null) {
      _dataBacking = new CacheDataBackingImpl(this);
    }
    
    if (getDataBacking() == null) {
      throw new NullPointerException();
    }
    
    _dataBacking.start();
    
    _cacheEngine.start();
  }
  
  public void stop()
  {
    _admin.unregister();
  }

  public void closeCache(String guid, HashKey cacheKey)
  {
    _keyManager.closeCache(guid);
    
    _cacheListenMap.remove(cacheKey);
  }

  /**
   * Called when a cache initializes.
   */
  public void initCache(CacheImpl<?,?> cache)
  {
    CacheConfig config = cache.getConfig();
    
    CacheHandle cacheHandle = config.getCache();
    
    cacheHandle.setConfig(config);
    // _regionManager.putCacheConfig(config.getCacheKey(), config);
    // XXX: engine.initCache
  }

  /**
   * Called when a cache is removed.
   */
  public void destroyCache(CacheImpl<?,?> cache)
  {
    // clear config?
  }
  
  public void addCacheConfig(byte []cacheHash, CacheConfig config)
  {
    HashKey cacheKey = HashKey.create(cacheHash);
    
    CacheHandle cacheHandle = _regionManager.createCache(cacheKey, config);
  }

  /**
   * @param cacheHash
   * @return
   */
  public CacheConfig getCacheConfig(byte[] cacheHash)
  {
    if (cacheHash == null) {
      return null;
    }
    
    HashKey cacheKey = HashKey.create(cacheHash);
    
    CacheConfig defaultConfig = getDefaultCacheConfig();
   
    return _regionManager.createCache(cacheKey, defaultConfig).getConfig();
  }

  /**
   * @param task
   */
  void schedule(Runnable task)
  {
    ThreadPool.getCurrent().schedule(task);
  }

  /**
   * Closes the manager.
   */
  public void close()
  {
    _isClosed = true;

    if (getDataBacking() != null) {
      getDataBacking().close();
    }
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  //
  // QA
  //

  public long calculateValueHash(Object value, CacheConfig config)
  {
    return _localDataManager.calculateValueHash(value, config);
  }
  
  public MnodeStore getMnodeStore()
  {
    return _dataBacking.getMnodeStore();
  }
  
  public DataStore getDataStore()
  {
    return _dataBacking.getDataStore();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _resinSystem.getId() + "]";
  }
}
