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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheMXBean;
import javax.cache.CacheStatistics;
import javax.cache.CacheWriter;
import javax.cache.Configuration;
import javax.cache.Status;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.db.block.BlockManager;
import com.caucho.distcache.jcache.CacheManagerFacade;
import com.caucho.loader.Environment;
import com.caucho.server.distcache.CacheBacking;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.CacheEngine;
import com.caucho.server.distcache.CacheImpl;
import com.caucho.server.distcache.CacheManagerImpl;
import com.caucho.server.distcache.DataStore;
import com.caucho.server.distcache.DataStore.DataItem;
import com.caucho.server.distcache.DistCacheSystem;
import com.caucho.server.distcache.MnodeStore;
import com.caucho.server.distcache.MnodeUpdate;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * Implements the distributed cache
 */

public class AbstractCache
  implements ObjectCache, ByteStreamCache, ResinCacheBuilder, Closeable
{
  private static final L10N L = new L10N(AbstractCache.class);

  private String _name;
  
  private String _managerName;
  private CacheManagerFacade _cacheManager;

  private String _guid;

  private CacheConfig _config = new CacheConfig();

  private boolean _isClosed;
  
  private CacheImpl _delegate;
  
  private long _memorySizeMin;

  public AbstractCache()
  {
    DistCacheSystem cacheService = DistCacheSystem.getCurrent();

    if (cacheService == null)
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a Resin environment",
                                    getClass().getSimpleName()));
    
    _config.setEngine(cacheService.getDistCacheManager().getCacheEngine());
  }

  /**
   * Returns the name of the cache.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Assigns the name of the cache.
   * A name is mandatory and must be unique among open caches.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }
  
  @Configurable
  public void setManagerName(String managerName)
  {
    _managerName = managerName;
  }
  
  @Override
  public CacheManagerFacade getCacheManager()
  {
    return _cacheManager;
  }
  
  public void setCacheManager(CacheManagerFacade cacheManager)
  {
    _cacheManager = cacheManager;
  }
  
  /**
   * Sets the CacheLoader that the Cache can then use to populate
   * cache misses from a reference store (database).
   */
  @Configurable
  public void setCacheLoader(CacheLoader loader)
  {
    _config.setCacheLoader(loader);
  }
  
  @Configurable
  public void setReadThrough(boolean isReadThrough)
  {
    _config.setReadThrough(isReadThrough);
  }
  
  @Configurable
  public void setReadThroughExpireTimeout(Period timeout)
  {
    _config.setReadThroughExpireTimeout(timeout.getPeriod());
  }
  
  public void setReadThroughExpireTimeoutMillis(long timeout)
  {
    _config.setReadThroughExpireTimeout(timeout);
  }
  
  /**
   * Sets the CacheWrite that the Cache can then use to save
   * cache misses from a reference store (database).
   */
  @Configurable
  public void setCacheWriter(CacheWriter writer)
  {
    _config.setCacheWriter(writer);
  }
  
  @Configurable
  public void setWriteThrough(boolean isWriteThrough)
  {
    _config.setWriteThrough(isWriteThrough);
  }
  
  /**
   * Sets the CacheLoader and CacheWriter which the Cache can then use 
   * to populate cache misses from a reference store (database).
   */
  @Configurable
  public void setCacheReaderWriter(CacheLoader loader)
  {
    if (! (loader instanceof CacheWriter)) {
      throw new ConfigException(L.l("cache-reader-writer '{0}' must implements both CacheLoader and CacheWriter.",
                                    loader));
    }
    
    _config.setCacheLoader(loader);
    _config.setReadThrough(true);
    _config.setCacheWriter((CacheWriter) loader);
    _config.setWriteThrough(true);
  }

  /**
   * Assign the serializer used on values.
   *
   * @Note: This setting should not be changed after
   * a cache is created.
   */
  @Configurable
  public void setSerializer(CacheSerializer serializer)
  {
    _config.setValueSerializer(serializer);
  }
  
  public void setEngine(CacheEngine engine)
  {
    _config.setEngine(engine);
  }

  /**
   * The maximum idle time for an item, which is typically used for
   * temporary data like sessions.  For example, session
   * data might be removed if idle over 30 minutes.
   * <p/>
   * Cached data would have infinite idle time because
   * it doesn't depend on how often it's accessed.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setAccessedExpireTimeout(Period period)
  {
    setAccessedExpireTimeoutMillis(period.getPeriod());
  }
  
  @Configurable
  public void setIdleTimeout(Period period)
  {
    setAccessedExpireTimeout(period);
  }

  /**
   * The maximum idle time for an item, which is typically used for
   * temporary data like sessions.  For example, session
   * data might be removed if idle over 30 minutes.
   * <p/>
   * Cached data would have infinite idle time because
   * it doesn't depend on how often it's accessed.
   * <p/>
   * Default is infinite.
   */
  public long getAccessedExpireTimeout()
  {
    return _config.getAccessedExpireTimeout();
  }

  /**
   * Sets the idle timeout in milliseconds
   */
  @Configurable
  public void setAccessedExpireTimeoutMillis(long timeout)
  {
    _config.setAccessedExpireTimeout(timeout);
  }

  /**
   * Returns the idle check window, used to minimize traffic when
   * updating access times.
   */
  public long getAccessedExpireTimeoutWindow()
  {
    return _config.getAccessedExpireTimeoutWindow();
  }

  /**
   * Sets the idle timeout windows
   */
  public void setAccessedExpireTimeoutWindow(Period period)
  {
    _config.setAccessedExpireTimeoutWindow(period.getPeriod());
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and z null value
   * will be returned for a get.
   * <p/>
   * Default is infinite.
   */
  public long getModifiedExpireTimeout()
  {
    return _config.getModifiedExpireTimeout();
  }

  /**
   * The maximum valid time for a cached item before it expires.
   * Items stored in the cache for longer than the expire time are
   * no longer valid and will return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setModifiedExpireTimeout(Period expireTimeout)
  {
    setModifiedExpireTimeoutMillis(expireTimeout.getPeriod());
  }
  
  /**
   * Backwards compat.
   */
  @Configurable
  public void setExpireTimeout(Period expireTimeout)
  {
    setModifiedExpireTimeout(expireTimeout);
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setModifiedExpireTimeoutMillis(long expireTimeout)
  {
    _config.setModifiedExpireTimeout(expireTimeout);
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseExpireTimeout()
  {
    return _config.getLeaseExpireTimeout();
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseExpireTimeout(Period period)
  {
    setLeaseExpireTimeoutMillis(period.getPeriod());
  }
  
  @Configurable
  public void setLeaseTimeout(Period period)
  {
    setLeaseExpireTimeout(period);
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseExpireTimeoutMillis(long timeout)
  {
    _config.setLeaseExpireTimeout(timeout);
  }

  /**
   * The local read timeout is how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quickly changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  public long getLocalExpireTimeout()
  {
    return _config.getLocalExpireTimeout();
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalExpireTimeout(Period period)
  {
    setLocalExpireTimeoutMillis(period.getPeriod());
  }
  
  /**
   * Backwards compat.
   */
  @Configurable
  public void setLocalReadTimeout(Period period)
  {
    setLocalExpireTimeout(period);
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalExpireTimeoutMillis(long period)
  {
    _config.setLocalExpireTimeout(period);
  }

  public void setScopeMode(Scope scope)
  {
    _config.setScopeMode(scope);
  }

  public boolean isBackup()
  {
    return _config.isBackup();
  }

  public boolean isTriplicate()
  {
    return _config.isTriplicate();
  }

  /**
   * @param backing
   */
  public void setBacking(CacheBacking<?, ?> backing)
  {
    _config.setCacheLoader(backing);
    _config.setReadThrough(true);
    _config.setCacheWriter(backing);
    _config.setWriteThrough(true);
  }
  
  public void setPersistenceMode(Persistence persistence)
  {
    
  }
  
  /**
   * Sets the minimum memory size for the internal byte buffer cache.
   * Since the buffer cache is shared across all caches, the minimum
   * memory size is for all caches, not the sum of each of the memory sizes.
   */
  
  @Configurable
  public void setMemorySizeMin(Bytes size)
  {
    _memorySizeMin = size.getBytes();
  }
  
  //
  // caching API facade
  //

  /**
   * Returns the object with the given key without checking the backing store.
   */
  public Object peek(Object key)
  {
    return _delegate.peek(key);
  }
  
  /**
   * Returns the hash of the given key
   */
  public HashKey getKeyHash(Object key)
  {
    return _delegate.getKeyHash(key);
  }

  /**
   * Returns the object with the given key, checking the backing
   * store if necessary.
   */
  @Override
  public Object get(Object key)
  {
    return _delegate.get(key);
  }

  /**
   * Returns the object with the given key, checking the backing
   * store if necessary.
   */
  /*
  @Override
  public Object getExact(Object key)
  {
    return _delegate.getExact(key);
  }
  */

  /**
   * Fills an output stream with the value for a key.
   */
  @Override
  public boolean get(Object key, OutputStream os)
    throws IOException
  {
    return _delegate.get(key, os);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  @Override
  public ExtCacheEntry getExtCacheEntry(Object key)
  {
    return _delegate.getExtCacheEntry(key);
  }
  
  public ExtCacheEntry getExtCacheEntry(HashKey key)
  {
    return _delegate.getExtCacheEntry(key);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  @Override
  public ExtCacheEntry peekExtCacheEntry(Object key)
  {
    return _delegate.peekExtCacheEntry(key);
  }
  
  public ExtCacheEntry getStatCacheEntry(Object key)
  {
    return _delegate.getStatCacheEntry(key);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public Cache.Entry getCacheEntry(Object key)
  {
    return _delegate.getExtCacheEntry(key);
  }
  
  public ExtCacheEntry getLiveCacheEntry(Object key)
  {
    return _delegate.getLiveCacheEntry(key);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  @Override
  public void put(Object key, Object value)
  {
    _delegate.put(key, value);
  }

  @Override
  public boolean putIfAbsent(Object key, Object value) throws CacheException
  {
    return _delegate.putIfAbsent(key, value);
  }

  @Override
  public boolean replace(Object key, Object oldValue, Object value)
    throws CacheException
  {
    return _delegate.replace(key, oldValue, value);
  }

  @Override
  public boolean replace(Object key, Object value) throws CacheException
  {
    return _delegate.replace(key, value);
  }

  @Override
  public Object getAndReplace(Object key, Object value) throws CacheException
  {
    return _delegate.getAndReplace(key, value);
  }

  /**
   * Removes the entry from the cache.
   *
   * @return true if the object existed
   */
  @Override
  public boolean remove(Object key)
  {
    return _delegate.remove(key);
  }

  /**
   * Removes the entry from the cache.
   *
   * @return true if the object existed
   */
  @Override
  public boolean remove(Object key, Object oldValue)
  {
    return _delegate.remove(key, oldValue);
  }

  @Override
  public Object getAndRemove(Object key) throws CacheException
  {
    return _delegate.getAndRemove(key);
  }

  /**
   * Removes the entry from the cache if the current entry matches the version.
   */
  @Override
  public boolean compareAndRemove(Object key, long version)
  {
    return _delegate.compareAndRemove(key, version);
  }

  @Override
  public Future load(Object key)
      throws CacheException
  {
    return _delegate.load(key);
  }

  @Override
  public Future loadAll(Set keys)
      throws CacheException
  {
    return _delegate.loadAll(keys);
  }

  @Override
  public void removeAll() throws CacheException
  {
    _delegate.removeAll();
  }

  @Override
  public Iterator iterator()
  {
    return _delegate.iterator();
  }

  @Override
  public Status getStatus()
  {
    return _delegate.getStatus();
  }

  @Override
  public void start() throws CacheException
  {
  }

  @Override
  public void stop() throws CacheException
  {
  }

  @Override
  public Object unwrap(Class cl)
  {
    return _delegate.unwrap(cl);
  }
  
  //
  // Resin ObjectCache facade
  //

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   * @param flags       the flags value (for memcache)
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout,
                           int userFlags)
    throws IOException
  {
    return _delegate.put(key, is, 
                         accessedExpireTimeout, 
                         modifiedExpireTimeout,
                         userFlags);
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout)
    throws IOException
  {
    return _delegate.put(key, is, 
                         accessedExpireTimeout, 
                         modifiedExpireTimeout);
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout,
                           long lastAccessTime,
                           long lastModifiedTime)
    throws IOException
  {
    return _delegate.put(key, is, 
                         accessedExpireTimeout, 
                         modifiedExpireTimeout,
                         lastAccessTime,
                         lastModifiedTime);
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  @Override
  public boolean putIfNew(Object key,
                          MnodeUpdate update,
                          InputStream is)
    throws IOException
  {
    return _delegate.putIfNew(key, update, is);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  @Override
  public Object getAndPut(Object key, Object value)
  {
    return _delegate.getAndPut(key, value);
  }

  /**
   * Updates the cache if the old version matches the current version.
   * A zero value for the old value hash only adds the entry if it's new.
   *
   * @param key     the key to compare
   * @param version the version of the old value, returned by getEntry
   * @param value   the new value
   * @return true if the update succeeds, false if it fails
   */
  @Override
  public boolean compareVersionAndPut(Object key,
                                      long version,
                                      Object value)
  {
    return _delegate.compareVersionAndPut(key, version, value);
  }

  /**
   * Updates the cache if the old version matches the current value.
   * A zero value for the old version only adds the entry if it's new.
   *
   * @param key         the key to compare
   * @param version     the hash of the old version, returned by getEntry
   * @param inputStream the new value
   * @return true if the update succeeds, false if it fails
   */
  /*
  @Override
  public boolean compareVersionAndPut(Object key,
                                      long version,
                                      InputStream inputStream)
    throws IOException
  {
    return _delegate.compareVersionAndPut(key, version, inputStream);
  }
  */
  
  /**
   * Returns the entry for the given key, returning the live item.
   */
  /*
  public ExtCacheEntry getLiveCacheEntry(Object key)
  {
    return getDistCacheEntry(key);
  }
  */

  /**
   * Returns the CacheKeyEntry for the given key.
   */
  /*
  protected DistCacheEntry getDistCacheEntry(Object key)
  {
    DistCacheEntry cacheEntry = null;
    
    // cacheEntry = _entryCache.get(key);

    if (cacheEntry == null) {
      cacheEntry = _manager.getCacheEntry(key, _config);

      // _entryCache.put(key, cacheEntry);
    }

    return cacheEntry;
  }
  */

  /**
   * Returns the CacheKeyEntry for the given key.
   */
  /*
  protected DistCacheEntry getDistCacheEntry(HashKey key)
  {
    return _manager.getCacheEntry(key, _config);
  }
  */

  /**
   * Returns a new map of the items found in the central cache.
   *
   * @note If a cacheLoader is configured if an item is not found in the cache.
   */
  public Map getAll(Collection keys)
  {
    Map result = new HashMap();

    for (Object key : keys) {
      Object value = get(key);

      if (value != null) {
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * Loads an item into the cache if not already there and was returned from  in the optional cache loader.
   *
   * @param key
   */
  /*
  @Override
  public void load(Object key)
  {
    if (containsKey(key) || get(key) != null)
      return;

    Object loaderValue = cacheLoader(key);

    if (loaderValue != null)
      put(key, loaderValue);

    notifyLoad(key);
  }
  */

  /**
   * Implements the loadAll method for a collection of keys.
   */
  /*
  public void loadAll(Collection keys)
  {
    Map<Object, Object> entries = null;
    CacheLoader loader = _config.getCacheLoader();

    if (loader == null || keys == null || keys.size() == 0)
      return;

    entries = loader.loadAll(keys);

    if (entries.isEmpty())
      return;

    for (Entry loaderEntry : entries.entrySet()) {
      Object loaderKey = loaderEntry.getKey();
      if (!containsKey(loaderKey) && (get(loaderKey) != null)) {
        put(loaderKey, loaderEntry.getValue());
        notifyLoad(loaderKey);
      }
    }
  }
  */

  /**
   * Removes a listener from the cache.
   */
  @Override
  public boolean unregisterCacheEntryListener(CacheEntryListener listener)
  {
    return _delegate.unregisterCacheEntryListener(listener);
  }

  /**
   * Returns the CacheStatistics for this cache.
   */
  @Override
  public CacheStatistics getStatistics()
  {
    return _delegate.getStatistics();
  }

  /**
   * Puts each item in the map into the cache.
   */
  @Override
  public void putAll(Map map)
  {
    if (map == null || map.size() == 0)
      return;
    Set entries = map.entrySet();

    for (Object item : entries) {
      Map.Entry entry = (Map.Entry) item;
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns true if an entry for the item is found in  the cache.
   *
   * @param key
   * @return
   */
  @Override
  public boolean containsKey(Object key)
  {
    return _delegate.containsKey(key);
  }
  
  public HashKey getCacheKey()
  {
    return _config.getCacheKey();
  }

  /**
   * Places an item in the cache from the loader unless the item is in cache already.
   */
  protected Object cacheLoader(Object key)
  {
    Object value = get(key);

    if (value != null)
      return value;

    CacheLoader loader = _config.getCacheLoader();
    
    Object arg = null;

    value = (loader != null) ? loader.load(key) : null;

    if (value != null)
      put(key, value);
    notifyLoad(key);

    return value;
  }

  protected void notifyLoad(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onLoad(key);
    }
    */
  }

  protected void notifyEvict(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onEvict(key);
    }
    */
  }

  protected void notifyClear(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onClear();
    }
    */
  }

  protected void notifyPut(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onPut(key);
    }
    */
  }

  protected void notifyRemove(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onRemove(key);
    }
    */
  }

  @Override
  public boolean isClosed()
  {
    return _isClosed;
  }

  @Override
  public void close()
  {
  }

  private void initName(String name)
    throws ConfigException
  {
    if (_name == null || _name.length() == 0)
      throw new ConfigException(L.l("Each Cache must have a name."));

    String contextId = Environment.getEnvironmentName();
    
    if (_cacheManager != null)
      contextId = _cacheManager.getGuid();
      

    if (_guid == null)
      _guid = contextId + ":" + _name;

    _config.setGuid(_guid);
  }
  
  public boolean loadData(long valueIndex,
                          long valueDataTime,
                          WriteStream os)
    throws IOException
  {
    return _delegate.loadData(valueIndex, valueDataTime, os);
  }

  public DataItem saveData(StreamSource source, int length)
    throws IOException
  {
    return _delegate.saveData(source, length);
  }

  public boolean isDataAvailable(long valueDataId, long valueDataTime)
  {
    return _delegate.isDataAvailable(valueDataId, valueDataTime);
  }

  //
  // QA
  //
  
  public byte []getKeyHash(String name)
  {
    return _delegate.getKeyHash(name);
  }
  
  public long getValueHash(Object value)
  {
    return _delegate.getValueHash(value);
  }
  
  public MnodeStore getMnodeStore()
  {
    return _delegate.getMnodeStore();
  }
  
  public DataStore getDataStore()
  {
    return _delegate.getDataStore();
  }
  
  public CacheImpl createIfAbsent()
  {
    init();
    
    return _delegate;
  }

  /**
   * Initialize the cache.
   */
  @PostConstruct
  public void init()
  {
    if (_delegate != null)
      return;

    _config.init();

    initName(_name);
    
    DistCacheSystem cacheService = DistCacheSystem.getCurrent();

    if (cacheService == null)
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a Resin environment",
                                    getClass().getSimpleName()));
    
    String managerName = _managerName;
    
    if (managerName == null && _cacheManager != null)
      managerName = _cacheManager.getName();

    CacheManagerImpl manager;
    
    if (managerName != null)
      manager = cacheService.getCacheManager(managerName);
    else
      manager = cacheService.getCacheManager();
    
    if (_config.getEngine() == null) {
      _config.setEngine(cacheService.getDistCacheManager().getCacheEngine());
    }
    
    if (_memorySizeMin > 0) {
      BlockManager.create().ensureMemoryCapacity(_memorySizeMin);
    }
    
    _delegate = manager.createIfAbsent(_name, _config);
  }

  @Override
  public Configuration getConfiguration()
  {
    return _delegate.getConfiguration();
  }
  
  public CacheConfig getConfig()
  {
    return _config;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getAll(java.util.Set)
   */
  @Override
  public Map getAll(Set keys)
  {
    return _delegate.getAll(keys);
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#invokeEntryProcessor(java.lang.Object, javax.cache.Cache.EntryProcessor)
   */
  @Override
  public Object invokeEntryProcessor(Object key, EntryProcessor entryProcessor)
  {
    return _delegate.invokeEntryProcessor(key, entryProcessor);
  }

  @Override
  public void removeAll(Set keys)
  {
    _delegate.removeAll(keys);
  }

  @Override
  public CacheMXBean getMBean()
  {
    return _delegate.getMBean();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#registerCacheEntryListener(javax.cache.event.CacheEntryListener, boolean, javax.cache.event.CacheEntryEventFilter, boolean)
   */
  @Override
  public boolean registerCacheEntryListener(CacheEntryListener listener,
                                            boolean requireOldValue,
                                            CacheEntryEventFilter filter,
                                            boolean synchronous)
  {
    // TODO Auto-generated method stub
    return false;
  }
}
